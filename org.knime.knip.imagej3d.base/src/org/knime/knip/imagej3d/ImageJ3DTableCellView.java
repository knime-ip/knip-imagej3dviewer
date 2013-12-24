/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2013
 *  University of Konstanz, Germany and
 *  KNIME GmbH, Konstanz, Germany
 *  Website: http://www.knime.org; Email: contact@knime.org
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License, Version 3, as
 *  published by the Free Software Foundation.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses>.
 *
 *  Additional permission under GNU GPL version 3 section 7:
 *
 *  KNIME interoperates with ECLIPSE solely via ECLIPSE's plug-in APIs.
 *  Hence, KNIME and ECLIPSE are both independent programs and are not
 *  derived from each other. Should, however, the interpretation of the
 *  GNU GPL Version 3 ("License") under any applicable laws result in
 *  KNIME and ECLIPSE being a combined program, KNIME GMBH herewith grants
 *  you the additional permission to use and propagate KNIME together with
 *  ECLIPSE with only the license terms in place for ECLIPSE applying to
 *  ECLIPSE and the GNU GPL Version 3 applying for KNIME, provided the
 *  license terms of ECLIPSE themselves allow for the respective use and
 *  propagation of ECLIPSE together with KNIME.
 *
 *  Additional permission relating to nodes for KNIME that extend the Node
 *  Extension (and in particular that are based on subclasses of NodeModel,
 *  NodeDialog, and NodeView) and that only interoperate with KNIME through
 *  standard APIs ("Nodes"):
 *  Nodes are deemed to be separate and independent programs and to not be
 *  covered works.  Notwithstanding anything to the contrary in the
 *  License, the License does not apply to Nodes, you are not required to
 *  license Nodes under the License, and you are granted a license to
 *  prepare and propagate Nodes, in each case even if such Nodes are
 *  propagated with or for interoperation with KNIME.  The owner of a Node
 *  may freely choose the license terms applicable to such Node, including
 *  when such Node is propagated with or for interoperation with KNIME.
 * --------------------------------------------------------------------- *
 *
 */

package org.knime.knip.imagej3d;

import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.concurrent.ExecutionException;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;

import net.imglib2.converter.read.ConvertedRandomAccessibleInterval;
import net.imglib2.exception.IncompatibleTypeException;
import net.imglib2.img.ImgView;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.real.unary.Convert;
import net.imglib2.ops.operation.real.unary.Convert.TypeConversionTypes;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.real.DoubleType;

import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.imagej2.core.util.ImgToIJ;
import org.knime.knip.imagej2.core.util.UntransformableIJTypeException;

import view4d.Timeline;
import view4d.TimelineGUI;

/**
 * class that handles loading the image in the background
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */

/*
 * Helper class for the IJ3D viewer, provides the TableCellView
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ImageJ3DTableCellView<T extends RealType<T>> implements
		TableCellView {

	private NodeLogger logger = NodeLogger
			.getLogger(ImageJ3DTableCellView.class);

	// Default rendering Type
	private final int renderType = ContentConstants.VOLUME;

	// 4D stuff
	private Timeline timeline;
	private TimelineGUI timelineGUI;
	private JPanel panel4D = new JPanel();

	// rendering Universe
	private Image3DUniverse universe;

	// Container for the converted picture,
	private ImagePlus ijImagePlus;

	// ui containers
	private JPanel rootPanel;
	private JTextArea errorPanel;

	// Stores the Image that the viewer displays
	private DataValue dataValue;

	/**
	 *
	 * @return the immage the viewer is displaying
	 */

	public final DataValue getDataValue() {
		return dataValue;
	}

	// Container for picture
	private Content c;

	// Creates a viewer which will be updated on "updateComponent"
	@Override
	public final Component getViewComponent() {
		// Fixes Canvas covering menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		// universe for rendering the image
		universe = new Image3DUniverse();
		timeline = universe.getTimeline();

		// Container for the viewComponent
		rootPanel = new JPanel(new BorderLayout());

		// Panel for error messages
		errorPanel = new JTextArea("");
		errorPanel.setVisible(false);

		// Menubar
		ImageJ3DMenubar<T> ij3dbar = new ImageJ3DMenubar<T>(universe, this);
		// add menubar and 3Duniverse to the panel

		rootPanel.add(errorPanel);
		rootPanel.add(ij3dbar, BorderLayout.NORTH);
		rootPanel.add(universe.getCanvas(0), BorderLayout.CENTER);

		return rootPanel;
	}

	/**
	 * updates the Component, called whenever a new picture is selected, or view
	 * is reset.
	 *
	 * @param The
	 *            ImgPlus that is to be displayed by the viewer.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final void updateComponent(final DataValue valueToView) {
		// New image arrives
		universe.resetView();
		universe.removeAllContents(); // cleanup universe

		dataValue = valueToView;

		SwingWorker<ImgPlus<T>, Integer> worker = new SwingWorker<ImgPlus<T>, Integer>() {

			@Override
			protected ImgPlus<T> doInBackground() throws Exception {
				// TODO Auto-generated method stub

				// make sure everything is visible
				for (Component t : rootPanel.getComponents()) {
					t.setVisible(true);
				}
				// in case it was added before
				rootPanel.remove(errorPanel);

				ImgPlus<T> in = ((ImgPlusValue<T>) valueToView).getImgPlus();

				// abort if input image has to few dimensions.
				if (in.numDimensions() < 3) {
					showErrorPane(
							"Only immages with a minimum of 3 Dimensions \n are supported by the 3D viewer",
							in.getName());
					return null;
				}

				// abort if input image has to many dimensions.
				if (in.numDimensions() > 5) {
					showErrorPane(
							"Only immages with up to 5 Dimensions \n are supported by the 3D viewer",
							in.getName());
					return null;
				}

				ImgPlus<T> imgPlus = null;
				final T firstElement = in.firstElement();

				// Convert to ByteType if needed.
				imgPlus = (ImgPlus<T>) in;

				// abort if unsuported type
				if (firstElement instanceof DoubleType) {
					// TODO Add normalisation
					showErrorPane(
							"DoubleType images are not supported! \n convert to any different Type eg. ByteType",
							imgPlus.getName());
					return null;
				}

				// initalize ImgToIJ converter.
				ImgToIJ imgToIJ = new ImgToIJ(imgPlus.numDimensions());

				// validate if mapping can be inferred automatically
				if (!imgToIJ.validateMapping(imgPlus)) {
					if (!imgToIJ.inferMapping(imgPlus)) {
						showErrorPane(
								"Warning: couldn't match dimensions of input picture",
								imgPlus.getName());
						return null;
					}
				}
				// convert to ijImagePlus.
				try {
					ijImagePlus = Operations.compute(imgToIJ, imgPlus);
				} catch (UntransformableIJTypeException f) {
					try {
						// convert to ByteType if imgToIJ fails to convert,
						// fixes most
						// untransformable IJType errors.
						ImgPlus<ByteType> imgPlusConverted = null;
						ConvertedRandomAccessibleInterval<T, ByteType> converted = new ConvertedRandomAccessibleInterval<T, ByteType>(
								in, new Convert<T, ByteType>(firstElement,
										new ByteType(),
										TypeConversionTypes.SCALE),
								new ByteType());

						imgPlusConverted = new ImgPlus<ByteType>(
								new ImgView<ByteType>(converted, in.factory()
										.imgFactory(new ByteType())), in);
						// second attempt at imgToIJ conversion.
						ijImagePlus = Operations.compute(imgToIJ,
								imgPlusConverted);
					} catch (IncompatibleTypeException f1) {
						// failed to convert, showing error.
						showErrorPane(firstElement.toString(), in.getName());
						// hide UI
						for (Component t : rootPanel.getComponents()) {
							t.setVisible(false);
						}
						showErrorPane(
								" Can't convert the picture to ijImagePlus",
								imgPlus.getName());
						return null;
					}
				}

				// convert into 8-Bit gray values image.
				try {
					new StackConverter(ijImagePlus).convertToGray8();
				} catch (java.lang.IllegalArgumentException e) {
					for (Component t : rootPanel.getComponents()) {
						t.setVisible(false);
					}
					errorPanel.setVisible(true);

					errorPanel.setText("Can't convert picture!");
					logger.warn("Can't convert the picture to Gray8: "
							+ imgPlus.getName());
					return null;
				}

				// select the rendertype
				switch (renderType) {
				case ContentConstants.ORTHO:
					c = universe.addOrthoslice(ijImagePlus);
					break;
				case ContentConstants.MULTIORTHO:
					c = universe.addOrthoslice(ijImagePlus);
					c.displayAs(ContentConstants.MULTIORTHO);
				case ContentConstants.VOLUME:
					c = universe.addVoltex(ijImagePlus);
					break;
				case ContentConstants.SURFACE:
					c = universe.addMesh(ijImagePlus);
					break;
				case ContentConstants.SURFACE_PLOT2D:
					c = universe.addSurfacePlot(ijImagePlus);
					break;
				}
				universe.updateTimeline();
				return imgPlus;
			}

			@Override
			protected void done() {
				ImgPlus<T> imgPlus = null;
				try {
					imgPlus = get();
				} catch (Exception e) {
					e.printStackTrace();
				}

				// enables the timeline gui if picture has 4 or 5 Dimensions
				if (imgPlus.numDimensions() > 3) { // FIXME
					timelineGUI = new TimelineGUI(timeline);
					panel4D = timelineGUI.getPanel();
					universe.setTimelineGui(timelineGUI);

					panel4D.setVisible(true);
					rootPanel.add(panel4D, BorderLayout.SOUTH);
				} else {
					panel4D.setVisible(false);
				}
			}

		};

		// TODO: until here in background task
		worker.execute();
	}

	/**
	 * outputs an error message in the log and in the root panel
	 *
	 * @param type
	 *            and name of image
	 */
	private void showErrorPane(String message, String name) {
		for (Component t : rootPanel.getComponents()) {
			t.setVisible(false);
		}
		rootPanel.add(errorPanel);
		errorPanel.setVisible(true);

		errorPanel.setText(message);
		errorPanel.setVisible(true);
		if (name != null) {
			logger.warn("Error: " + message + "in Picture " + name);
		}
	}

	@Override
	public void onClose() {
		universe.removeAllContents();
	}

	@Override
	public final String getName() {
		return "ImageJ 3D Viewer";
	}

	@Override
	public final String getDescription() {
		return "ImageJ 3D viewer";
	};

	@Override
	public void loadConfigurationFrom(final ConfigRO config) {
	}

	@Override
	public void saveConfigurationTo(final ConfigWO config) {
	}

	@Override
	public void onReset() {
	}
}
