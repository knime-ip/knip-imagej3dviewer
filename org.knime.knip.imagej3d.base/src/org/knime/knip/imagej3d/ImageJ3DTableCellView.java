/*
 * ------------------------------------------------------------------------
 *
 *  Copyright (C) 2003 - 2014
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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.util.List;

import javax.media.j3d.Canvas3D;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingWorker;
import javax.swing.ToolTipManager;

import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.cellviewer.interfaces.CellView;
import org.knime.knip.core.util.waitingindicator.WaitingIndicatorUtils;
import org.knime.knip.core.util.waitingindicator.libs.WaitIndicator;
import org.knime.knip.imagej2.core.util.ImageProcessorFactory;
import org.knime.knip.imagej2.core.util.ImgToIJ;

import ij.ImagePlus;
import ij.process.ByteProcessor;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import ij3d.ImageWindow3D;
import net.imagej.ImgPlus;
import net.imglib2.converter.Converter;
import net.imglib2.ops.operation.Operations;
import net.imglib2.ops.operation.iterableinterval.unary.MinMax;
import net.imglib2.ops.operation.real.unary.Normalize;
import net.imglib2.type.Type;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.real.DoubleType;
import net.imglib2.type.numeric.real.FloatType;
import net.imglib2.util.ValuePair;
import view4d.Timeline;
import view4d.TimelineGUI;

/**
 * Helper class for the ImageJ 3D Viewer, which provides the TableCellView.
 * 
 * @author <a href="mailto:gabriel.einsdorf@uni.kn">Gabriel Einsdorf</a>
 */
public class ImageJ3DTableCellView<T extends RealType<T>> implements
		CellView {

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

	private Canvas3D m_universePanel;

	// Container for the converted picture,
	private ImagePlus m_ijImagePlus;

	// ui containers
	private JPanel m_rootPanel;

	// Stores the Image that the viewer displays
	private DataValue m_dataValue;

	/**
	 * 
	 * @return the immage the viewer is displaying
	 */

	public final DataValue getDataValue() {
		return m_dataValue;
	}

	// Container for picture
	private Content m_c;

	// Creates a viewer which will be updated on "updateComponent"
	@Override
	public final JPanel getViewComponent() {
		// Fixes Canvas covering menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		// Container for the viewComponent
		m_rootPanel = new JPanel(new BorderLayout());
		m_rootPanel.setVisible(true);

		return m_rootPanel;
	}

	/**
	 * flushes the cache and updates the Component.
	 * 
	 * @param valueToView
	 *            TheImgPlus that is to be displayed by the viewer.
	 */
	protected final void fullReload(final List<DataValue> valueToView) {
		m_dataValue = null;
		m_rootPanel.remove(m_universePanel);
		updateComponent(valueToView);
	}

	/**
	 * updates the Component, called whenever a new picture is selected, or the
	 * view is reset.
	 * 
	 * @param valueToView
	 *            The ImgPlus that is to be displayed by the viewer.
	 */
	@SuppressWarnings("unchecked")
	@Override
	public final void updateComponent(final List<DataValue> valueToView) {

		if (m_dataValue == null || !(m_dataValue.equals(valueToView.get(0)))) {

			// reference to the current object so it can be reached in the
			// worker.
			final ImageJ3DTableCellView<T> context = this;

			showError(m_rootPanel, null, false);
			WaitingIndicatorUtils.setWaiting(m_rootPanel, true);

			final SwingWorker<ImgPlus<T>, Integer> worker = new SwingWorker<ImgPlus<T>, Integer>() {

				@Override
				protected ImgPlus<T> doInBackground() throws Exception {

					// universe for rendering the image
					m_universe = new Image3DUniverse();
					m_timeline = m_universe.getTimeline();

					// Menubar
					final ImageJ3DMenubar<T> ij3dbar = new ImageJ3DMenubar<T>(
							m_universe, context);

					// add menubar and 3Duniverse to the panel
					m_rootPanel.add(ij3dbar, BorderLayout.NORTH);

					// New image arrives
					m_universe.resetView();
					m_universe.removeAllContents(); // cleanup universe

					m_dataValue = valueToView.get(0);
					final ImgPlus<T> in = ((ImgPlusValue<T>) valueToView.get(0))
							.getImgPlus();

					// abort if input image has to few dimensions.
					if (in.numDimensions() < 3) {
						showError(
								m_rootPanel,
								new String[] {
										"Only images with a minimum of three dimensions",
										" are supported by the ImageJ 3D Viewer." },
								true);
						return null;
					}

					// abort if input image has to many dimensions.
					if (in.numDimensions() > 5) {
						showError(m_rootPanel, new String[] {
								"Only images with up to five dimensions",
								"are supported by the 3D Viewer." }, true);
						return null;
					}

					// abort if unsuported type
					if (in.firstElement() instanceof DoubleType) {
						showError(
								m_rootPanel,
								new String[] {
										"DoubleType images are not supported!",
										" You have to convert your image to e.g. ByteType using the converter." },
								true);
						return null;
					}

					// validate if mapping can be inferred automatically
					if (!ImgToIJ.validateMapping(in)) {
						showError(
								m_rootPanel,
								new String[] { "Warning: The input image contains unknown dimensions. Currently we only support 'X','Y','Channel,'Z' and 'Time'!" },
								true);
						return null;
					}

					// here we create an converted ImagePlus
					m_ijImagePlus = createImagePlus(in);

					try {
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
					} catch (final Exception e) {
						WaitingIndicatorUtils.setWaiting(m_rootPanel, false);
						showError(m_rootPanel, new String[] {
								"error adding picture to universe:",
								e.getClass().getSimpleName() }, true);
						return null;
					}

					m_universe.updateTimeline();
					return in;
				}

				/**
				 * @param firstElement
				 * @param imgPlus
				 * @param factor
				 */
				private ImagePlus createImagePlus(final ImgPlus<T> in) {

					final double minValue = in.firstElement().getMinValue();
					final double maxValue = in.firstElement().getMaxValue();

					final ValuePair<T, T> oldMinMax = Operations.compute(
							new MinMax<T>(), in);

					final double factor = (((maxValue - minValue) / 255))
							/ Normalize.normalizationFactor(
									oldMinMax.a.getRealDouble(),
									oldMinMax.b.getRealDouble(), minValue,
									maxValue);

					return ImgToIJ.wrap(in, new ImageProcessorFactory() {

						@Override
						public <V extends Type<V>> ImageProcessor createProcessor(
								final int width, final int height, final V type) {
							return new ByteProcessor(width, height);
						}
					}, new Converter<T, FloatType>() {

						@Override
						public void convert(final T input,
								final FloatType output) {
							output.setReal(((input.getRealDouble() - minValue) / factor));
						}
					});
				}

				@Override
				protected void done() {

					ImgPlus<T> imgPlus = null;
					try {
						imgPlus = get();
					} catch (final Exception e) {
						e.printStackTrace();
						return;
					}

					// Error happend during rendering
					if (imgPlus == null) {
						return;
					}

					//
					
					m_universe.init(new ImageWindow3D("abc", m_universe));
					m_universePanel = m_universe.getCanvas(0);
					try {
						m_rootPanel.add(m_universePanel, BorderLayout.CENTER);
					} catch (final IllegalArgumentException e) {
						// TEMPORARY error handling: openen the 3D view
						// on different monitors doesn't work so far, at
						// least with linux
						if (e.getLocalizedMessage()
								.equals("adding a container to a container on a different GraphicsDevice")) {
							m_rootPanel
									.add(new JLabel(
											"Opening the ImageJ 3D Viewer on different monitors doesn't work so far, sorry. We are working on it ...!"));
						} else {
							throw e;
						}
					}


					WaitingIndicatorUtils.setWaiting(m_rootPanel, false);

					// enables the timeline gui if picture has 4 or 5
					// Dimensions

					if (m_ijImagePlus.getNFrames() > 1) {
						m_timelineGUI = new TimelineGUI(m_timeline);
						m_panel4D = m_timelineGUI.getPanel();
						m_universe.setTimelineGui(m_timelineGUI);

						m_panel4D.setVisible(true);
						m_rootPanel.add(m_panel4D, BorderLayout.SOUTH);
					} else {
						m_panel4D.setVisible(false);
					}
					
					m_rootPanel.updateUI();
				}
			};

			worker.execute();
		}
	}

	private class ImageJ3DErrorIndicator extends WaitIndicator {

		private String[] m_errorText = { "Error" };

		public ImageJ3DErrorIndicator(final JComponent target,
				final String[] message) {
			super(target);
			getPainter().setCursor(
					Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			this.m_errorText = message;
		}

		@Override
		public void paint(Graphics g) {
			final Rectangle r = getDecorationBounds();
			g = g.create();
			g.setColor(new Color(211, 211, 211, 255));
			g.fillRect(r.x, r.y, r.width, r.height);
			if (m_errorText == null) {
				m_errorText = new String[] { "unknown Error!" };
			}

			((Graphics2D) g).setRenderingHint(
					RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

			// Title Warning
			g.setColor(new Color(0, 0, 0));
			g.setFont(new Font("Helvetica", Font.BOLD, 50));
			g.drawString("ERROR", 10, 130);

			// Error message
			g.setFont(new Font("TimesRoman", Font.BOLD, 14));
			final int newline = g.getFontMetrics().getHeight() + 5;
			int y = 200;
			for (final String s : m_errorText) {
				g.drawString(s, 10, y += newline);
			}
			g.dispose();

		}
	}

	@SuppressWarnings("unchecked")
	private void showError(final JComponent jc, final String[] message,
			final boolean on) {
		ImageJ3DErrorIndicator w = (ImageJ3DErrorIndicator) jc
				.getClientProperty("error");
		if (w == null) {
			if (on) {
				String loggerMessage = "";
				for (final String s : message) {
					loggerMessage += " " + s;
				}
				m_logger.warn(loggerMessage);
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

		if (m_universe != null)
			m_universe.cleanup();

		m_dataValue = null;
		m_ijImagePlus = null;
		m_c = null;
		m_panel4D = null;
		m_universe = null;
		m_timeline = null;
		m_timelineGUI = null;
		m_logger = null;
	}

	@Override
	public void onReset() {
	}

}
