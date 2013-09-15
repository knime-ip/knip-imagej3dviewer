package org.knime.knip.ij3dviewer;

import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.HashMap;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import net.imglib2.meta.AxisType;
import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataValue;
import org.knime.core.node.NodeLogger;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.imagej2.core.util.ImgToIJ;

import view4d.Timeline;
import view4d.TimelineGUI;

public class IJ3DTableCellView<T extends RealType<T>> implements TableCellView {

	private NodeLogger logger = NodeLogger.getLogger(IJ3DTableCellView.class);

	// Default rendering Type
	private final int renderType = ContentConstants.VOLUME; //TODO

	// 4D stuff
	private Timeline timeline;
	private TimelineGUI timelineGUI;
	private JPanel panel4D = new JPanel();

	// rendering Universe
	private Image3DUniverse universe;

	// Container for the converted picture,
	private ImagePlus ijImagePlus;

	private JPanel rootPanel;

	private DataValue dataValue;

	public DataValue getDataValue() {
		return dataValue;
	}

	// Container for picture
	private Content c;

	// Creates a viewer which will be updated on "updateComponent"
	@Override
	public Component getViewComponent() {
		// Fixes Canvas covering menu
		JPopupMenu.setDefaultLightWeightPopupEnabled(false);
		ToolTipManager.sharedInstance().setLightWeightPopupEnabled(false);

		universe = new Image3DUniverse();
		timeline = new Timeline(universe);

		// Container for the viewComponent
		rootPanel = new JPanel(new BorderLayout());

		// Menubar
		IJ3DMenubar<T> ij3dbar = new IJ3DMenubar<T>(universe, this);
		// Image3DMenubar ij3dbar = new Image3DMenubar(universe);

		rootPanel.add(ij3dbar, BorderLayout.NORTH);
		rootPanel.add(universe.getCanvas(0), BorderLayout.CENTER);

		return rootPanel;
	}

	/**
	 * updates the Component, called whenever a new picture is selected, or view
	 * is reset
	 */

	@Override
	public void updateComponent(DataValue valueToView) {
		// New image arrives
		universe.resetView();
		universe.removeAllContents(); // cleanup universe

		dataValue = valueToView;
		// convert to IJ
		@SuppressWarnings("unchecked")
		ImgPlus<T> imgPlus = ((ImgPlusValue<T>) valueToView).getImgPlus();
		
		if(imgPlus.numDimensions() > 5){
			logger.warn("Error: only immages with up to 5 Dimensions are supported by the 3D viewer");
			return;
		}
		ImgToIJ imgToIJ = new ImgToIJ();

//		if (!imgToIJ.validateMapping(imgPlus)) {
//			logger.warn("Warning: couldn't match dimensions of input picture, using Standard dimension settings as best guess");
//			HashMap<AxisType, Integer> newMapping = new HashMap<AxisType, Integer>();
//
//			for (int d = 0; d < imgPlus.numDimensions(); d++) {
//				newMapping.put(imgPlus.axis(0).type(), d);
//			}
//
//			imgToIJ.setMapping(newMapping);
//		}
//		
		ijImagePlus = Operations.compute(imgToIJ, imgPlus);

		new StackConverter(ijImagePlus).convertToGray8();

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
		default: // TODO remove Evil Haxx :D
			throw new RuntimeException(
					"no RenderMode selected, This should not have happend!!!");
		}
		universe.updateTimeline();

		if (imgPlus.numDimensions() > 3) {
			timelineGUI = new TimelineGUI(timeline);
			panel4D = timelineGUI.getPanel();
			panel4D.setVisible(true);
			rootPanel.add(panel4D, BorderLayout.SOUTH);
		} else {
			panel4D.setVisible(false);
		}

	}

	@Override
	public void onClose() {
		// Called on close (Cleanup memory etc) TODO:?
	}

	@Override
	public String getName() {
		return "Java3D Viewer";
	}

	@Override
	public String getDescription() {
		return " 3D viewer";
	};

	@Override
	public void loadConfigurationFrom(ConfigRO config) {
		// Bene: Nothing to do here
	}

	@Override
	public void saveConfigurationTo(ConfigWO config) {
		// Bene: Nothing to do here
	}

	@Override
	public void onReset() {
		universe.removeAllContents(); // TODO needed?
		// Called on reset of the TableCellViewer
	}

}
