package org.knime.knip.ij3dviewer;

import ij.ImagePlus;
import ij.process.StackConverter;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.ToolTipManager;

import net.imglib2.meta.ImgPlus;
import net.imglib2.ops.operation.Operations;
import net.imglib2.type.numeric.RealType;

import org.knime.core.data.DataValue;
import org.knime.core.node.config.ConfigRO;
import org.knime.core.node.config.ConfigWO;
import org.knime.knip.base.data.img.ImgPlusValue;
import org.knime.knip.base.nodes.view.TableCellView;
import org.knime.knip.base.nodes.view.TableCellViewFactory;
import org.knime.knip.imagej2.core.util.ImgToIJ;

import view4d.Timeline;
import view4d.TimelineGUI;

/**
 * Displays a 3D/4D Picture
 * 
 * @author Gabriel
 * 
 * @param <T>
 */
public class IJ3DViewer<T extends RealType<T>> implements TableCellViewFactory {

	@Override
	public TableCellView[] createTableCellViews() {
		return new TableCellView[] { new IJ3DTableCellView<T>() };
	}

	@Override
	public Class<? extends DataValue> getDataValueClass() {
		return ImgPlusValue.class;
	}

}
