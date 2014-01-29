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
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

import javax.swing.JComponent;
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
import org.knime.knip.imagej3d.libs.WaitIndicator;

import view4d.Timeline;
import view4d.TimelineGUI;

/**
 * class that handles loading the image in the background
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */

/**
 * Helper class for the IJ3D viewer, provides the TableCellView.
 *
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ImageJ3DTableCellView<T extends RealType<T>> implements
		TableCellView {

	private NodeLogger m_logger = NodeLogger
			.getLogger(ImageJ3DTableCellView.class);

	// Default rendering Type
	private final int m_renderType = ContentConstants.VOLUME;

	// 4D stuff
	private Timeline m_timeline;
	private TimelineGUI m_timelineGUI;
	private JPanel m_panel4D = new JPanel();

	// rendering Universe
	private Image3DUniverse m_universe;
	private Component m_universePanel;

	// Container for the converted picture,
	private ImagePlus m_ijImagePlus;

	// ui containers
	private JPanel m_rootPanel;
	private JTextArea m_errorPanel;

	// Stores the Image that the viewer displays
	private static DataValue dataValue;

	/**
	 *
	 * @return the immage the viewer is displaying
	 */

	public final DataValue getDataValue() {
		return dataValue;
	}

	// Container for picture
	private Content m_c;

	// Creates a viewer which will be updated on "updateComponent"
	@Override
	public final Component getViewComponent() {
		// Fixes Canvas covering menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		// universe for rendering the image
		m_universe = new Image3DUniverse();
		m_timeline = m_universe.getTimeline();

		// Container for the viewComponent
		m_rootPanel = new JPanel(new BorderLayout());

		// Panel for error messages
		m_errorPanel = new JTextArea();
		m_errorPanel.setVisible(false);

		// Menubar
		ImageJ3DMenubar<T> ij3dbar = new ImageJ3DMenubar<T>(m_universe, this);

		// add menubar and 3Duniverse to the panel
		m_rootPanel.add(ij3dbar, BorderLayout.NORTH);
		// rootPanel.add(universe.getCanvas(0), BorderLayout.CENTER);

		return m_rootPanel;
	}

	/**
	 * flushes the cache and updates the Component.
	 *
	 * @param valueToView
	 *            TheImgPlus that is to be displayed by the viewer.
	 */
	protected final void fullReload(final DataValue valueToView) {
		dataValue = null;
		m_rootPanel.remove(m_universePanel);
		updateComponent(valueToView);
	}

	/**
	 * updates the Component, called whenever a new picture is selected,
	 * or the view is reset.
	 *
	 * @param valueToView
	 * The ImgPlus that is to be displayed by the viewer.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final void updateComponent(final DataValue valueToView) {

		if (dataValue == null || !(dataValue.equals(valueToView))) {
			// New image arrives
			m_universe.resetView();
			m_universe.removeAllContents(); // cleanup universe

			dataValue = valueToView;

			showError(m_rootPanel, null, false);
			setWaiting(m_rootPanel, true);

			SwingWorker<ImgPlus<T>, Integer> worker = new SwingWorker<ImgPlus<T>, Integer>() {

				@Override
				protected ImgPlus<T> doInBackground() throws Exception {

					ImgPlus<T> in = ((ImgPlusValue<T>) valueToView)
							.getImgPlus();

					// abort if input image has to few dimensions.
					if (in.numDimensions() < 3) {
						showError(m_rootPanel, new String[] {
								"Only immages with a minimum of 3 Dimensions",
								"are supported by the 3D viewer" }, true);
						return null;
					}

					// abort if input image has to many dimensions.
					if (in.numDimensions() > 5) {
						showError(m_rootPanel, new String[] {
								"Only immages with up to 5 Dimensions",
								"are supported by the 3D viewer" }, true);
						return null;
					}

					ImgPlus<T> imgPlus = null;
					final T firstElement = in.firstElement();

					// Convert to ByteType if needed.
					imgPlus = (ImgPlus<T>) in;

					// abort if unsuported type
					if (firstElement instanceof DoubleType) {
						// TODO Add normalisation
						showError(m_rootPanel, new String[] {
								"DoubleType images are not supported!",
								"convert to any different Type eg. ByteType" },
								true);
						return null;
					}

					// initalize ImgToIJ converter.
					ImgToIJ imgToIJ = new ImgToIJ();

					// validate if mapping can be inferred automatically
					if (!imgToIJ.validateMapping(imgPlus)) {
						if (!imgToIJ.inferMapping(imgPlus)) {
							showError(
									m_rootPanel,
									new String[] {"Warning: couldn't match dimensions of input picture" },
									true);
							return null;
						}
					}
					// convert to ijImagePlus.
					try {
						m_ijImagePlus = Operations.compute(imgToIJ, imgPlus);
					} catch (UntransformableIJTypeException f) {
						try {
							// convert to ByteType if imgToIJ fails to convert,
							// fixes most untransformable IJType errors.
							ImgPlus<ByteType> imgPlusConverted = null;
							ConvertedRandomAccessibleInterval<T, ByteType> converted = new ConvertedRandomAccessibleInterval<T, ByteType>(
									in, new Convert<T, ByteType>(firstElement,
											new ByteType(),
											TypeConversionTypes.SCALE),
									new ByteType());

							imgPlusConverted = new ImgPlus<ByteType>(
									new ImgView<ByteType>(converted, in
											.factory().imgFactory(
													new ByteType())), in);
							// second attempt at imgToIJ conversion.
							m_ijImagePlus = Operations.compute(imgToIJ,
									imgPlusConverted);
						} catch (IncompatibleTypeException f1) {

							showError(
									m_rootPanel,
									new String[] {" Can't convert the picture to ijImagePlus" },
									true);
							return null;
						}
					}

					// convert into 8-Bit gray values image.
					try {
						new StackConverter(m_ijImagePlus).convertToGray8();
					} catch (java.lang.IllegalArgumentException e) {
						showError(
								m_rootPanel,
								new String[] {"Can't convert the picture to ijImagePlus" },
								true);
						return null;
					}

					// select the rendertype
					switch (m_renderType) {
					case ContentConstants.ORTHO:
						m_c = m_universe.addOrthoslice(m_ijImagePlus);
						break;
					case ContentConstants.MULTIORTHO:
						m_c = m_universe.addOrthoslice(m_ijImagePlus);
						m_c.displayAs(ContentConstants.MULTIORTHO);
					case ContentConstants.VOLUME:
						m_c = m_universe.addVoltex(m_ijImagePlus);
						break;
					case ContentConstants.SURFACE:
						m_c = m_universe.addMesh(m_ijImagePlus);
						break;
					case ContentConstants.SURFACE_PLOT2D:
						m_c = m_universe.addSurfacePlot(m_ijImagePlus);
						break;
					default:
						break;
					}
					m_universe.updateTimeline();
					return imgPlus;
				}

				@Override
				protected void done() {

					ImgPlus<T> imgPlus = null;
					try {
						imgPlus = get();
					} catch (Exception e) {
						e.printStackTrace();
						return;
					}

					// Error happend during rendering
					if (imgPlus == null) {
						return;
					}

					//
					m_universePanel = m_universe.getCanvas(0);
					m_rootPanel.add(m_universePanel, BorderLayout.CENTER);

					// enables the timeline gui if picture has 4 or 5 Dimensions
					if (m_ijImagePlus.getNFrames() > 1) {
						m_timelineGUI = new TimelineGUI(m_timeline);
						m_panel4D = m_timelineGUI.getPanel();
						m_universe.setTimelineGui(m_timelineGUI);

						m_panel4D.setVisible(true);
						m_rootPanel.add(m_panel4D, BorderLayout.SOUTH);
					} else {
						m_panel4D.setVisible(false);
					}
					setWaiting(m_rootPanel, false);
				}

			};

			worker.execute();
		}
	}

	private class ImageJ3DWaitIndicator extends WaitIndicator {
		public ImageJ3DWaitIndicator(final JComponent target) {
			super(target);
		}

		@Override
		public void paint(Graphics g) {
			Rectangle r = getDecorationBounds();
			g = g.create();
			g.setColor(new Color(103, 117, 219, 255));
			g.fillRect(r.x, r.y, r.width, r.height);
			g.setColor(new Color(255, 255, 255));
			g.setFont(new Font("Helvetica", Font.BOLD, 50));
			((Graphics2D) g).setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g.drawString("loading....", 10, 200);
			repaint();
			g.dispose();

		}
	}

	private class ImageJ3DErrorIndicator extends WaitIndicator {

		private String[] m_errorText = {"Error"};

		public ImageJ3DErrorIndicator(final JComponent target, final String[] message) {
			super(target);
			getPainter().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			this.m_errorText = message;
		}

		@Override
		public void paint(Graphics g) {
			Rectangle r = getDecorationBounds();
			g = g.create();
			g.setColor(new Color(200, 20, 30, 255));
			g.fillRect(r.x, r.y, r.width, r.height);
			if (m_errorText == null) {
				m_errorText = new String[] {"unknown Error!"};
			}

			((Graphics2D) g).setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			// Title Warning
			g.setColor(new Color(255, 255, 255));
			g.setFont(new Font("TimesRoman", Font.BOLD, 50));
			g.drawString("ERROR", 10, 130);

			// Error message
			g.setFont(new Font("TimesRoman", Font.BOLD, 14));
			int newline = g.getFontMetrics().getHeight() + 5;
			int y = 200;
			for (int i = 0; i < m_errorText.length; i++) {
				g.drawString(m_errorText[i], 10, y += newline);
			}
			g.dispose();

		}
	}

	@SuppressWarnings("unchecked")
	private void setWaiting(final JComponent jc, final boolean on) {
		ImageJ3DWaitIndicator w = (ImageJ3DWaitIndicator) jc
				.getClientProperty("waiter");
		if (w == null) {
			if (on) {
				w = new ImageJ3DWaitIndicator(jc);
			}
		} else if (!on) {
			w.dispose();
			w = null;
		}
		jc.putClientProperty("waiter", w);
	}

	@SuppressWarnings("unchecked")
	private void showError(final JComponent jc, final String[] message, final boolean on) {
		ImageJ3DErrorIndicator w = (ImageJ3DErrorIndicator) jc
				.getClientProperty("error");
		if (w == null) {
			if (on) {
				m_logger.warn(message[0]);
				w = new ImageJ3DErrorIndicator(jc, message);
			}
		} else if (!on) {
			w.dispose();
			w = null;
		}
		jc.putClientProperty("error", w);
	}

	@Override
	public final void onClose() {
		dataValue = null;
		m_universe.removeAllContents();
	}

	@Override
	public final String getName() {
		return "ImageJ 3D Viewer";
	}

	@Override
	public final String getDescription() {
		return "ImageJ 3D viewer";
	}

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
