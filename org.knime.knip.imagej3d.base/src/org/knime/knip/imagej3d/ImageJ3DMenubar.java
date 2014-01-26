package org.knime.knip.imagej3d;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij3d.Content;
import ij3d.Executer;
import ij3d.Image3DUniverse;
import ij3d.ImageCanvas3D;
import ij3d.UniverseListener;

import java.awt.Checkbox;
import java.awt.Cursor;
import java.awt.Scrollbar;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.media.j3d.Background;
import javax.media.j3d.View;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import javax.vecmath.Color3f;

import net.imglib2.type.numeric.RealType;

@SuppressWarnings("serial")
public class ImageJ3DMenubar<T extends RealType<T>> extends JMenuBar implements
		ActionListener, ItemListener, UniverseListener {

	private interface ColorListener {
		public void colorChanged(Color3f color);
	}

	private final Executer executer;
	private final ImageJ3DTableCellView<T> tableCellview;
	private final Image3DUniverse universe;

	private JCheckBoxMenuItem boundingBox;
	private JCheckBoxMenuItem coordinateSystem;
	private JCheckBoxMenuItem lock;
	private JCheckBoxMenuItem saturated;
	private JCheckBoxMenuItem shaded;
	private JCheckBoxMenuItem show;
	private JMenuItem animationOptions;
	private JMenuItem applyTransform;
	private JMenuItem bgColor;
	private JMenuItem centerOrigin;
	private JMenuItem centerUniverse;
	private JMenuItem channels;
	private JMenuItem close;
	private JMenuItem color;
	private JMenuItem colorSurface;
	private JMenuItem displayAsMultiOrtho;
	private JMenuItem displayAsOrtho;
	private JMenuItem displayAsSurface;
	private JMenuItem displayAsSurfacePlot;
	private JMenuItem displayAsVolume;
	private JMenuItem fitViewToContent;
	private JMenuItem fitViewToUniverse;
	private JMenuItem j3dproperties;
	private JMenuItem luts;
	private JMenuItem properties;
	private JMenuItem resetTransform;
	private JMenuItem resetView;
	private JMenuItem reloadImage;
	private JMenuItem saveTransform;
	private JMenuItem scalebar;
	private JMenuItem setTransform;
	private JMenuItem slices;
	private JMenuItem snapshot;
	private JMenuItem startAnimation;
	private JMenuItem stopAnimation;
	private JMenuItem threshold;
	private JMenuItem transparency;
	private JMenuItem viewPreferences;
	private JMenuItem viewposXY, viewposXZ, viewposYZ, viewnegXY, viewnegXZ,
			viewnegYZ;

	private final JMenu editMenu;
	private final JMenu helpMenu;
	private final JMenu transformMenu;
	private final JMenu viewMenu;

	public ImageJ3DMenubar(final Image3DUniverse univ,
			final ImageJ3DTableCellView<T> ij3dTableCellView) {
		super();
		this.universe = univ;
		this.executer = universe.getExecuter();
		this.tableCellview = ij3dTableCellView;
		universe.addUniverseListener(this);

		editMenu = createEditMenu();
		this.add(editMenu);

		viewMenu = createViewMenu();
		this.add(viewMenu);

		transformMenu = createTransformMenu();
		this.add(transformMenu);

		helpMenu = createHelpMenu();
		this.add(helpMenu);

		contentSelected(null);
	}

	private JMenu createEditMenu() {
		JMenu edit = new JMenu("Edit");

		edit.add(createDisplayAsSubMenu());
		slices = new JMenuItem("Adjust slices");
		slices.addActionListener(this);
		edit.add(slices);

		edit.addSeparator();

		luts = new JMenuItem("Transfer function");
		luts.addActionListener(this);
		edit.add(luts);

		channels = new JMenuItem("Change channels");
		channels.addActionListener(this);
		edit.add(channels);

		color = new JMenuItem("Change color");
		color.addActionListener(this);
		edit.add(color);

		transparency = new JMenuItem("Change transparency");
		transparency.addActionListener(this);
		edit.add(transparency);

		threshold = new JMenuItem("Adjust threshold");
		threshold.addActionListener(this);
		edit.add(threshold);

		edit.addSeparator();

		show = new JCheckBoxMenuItem("Show content");
		show.setState(true);
		show.addItemListener(this);
		edit.add(show);

		coordinateSystem = new JCheckBoxMenuItem("Show coordinate system", true);
		coordinateSystem.addItemListener(this);
		edit.add(coordinateSystem);

		boundingBox = new JCheckBoxMenuItem("Show bounding box", false);
		boundingBox.addItemListener(this);
		edit.add(boundingBox);

		return edit;
	}

	private JMenu createTransformMenu() {
		JMenu transform = new JMenu("Transformation");

		lock = new JCheckBoxMenuItem("Lock");
		lock.addItemListener(this);
		transform.add(lock);

		setTransform = new JMenuItem("Set Transform");
		setTransform.addActionListener(this);
		transform.add(setTransform);

		resetTransform = new JMenuItem("Reset Transform");
		resetTransform.addActionListener(this);
		transform.add(resetTransform);

		applyTransform = new JMenuItem("Apply Transform");
		applyTransform.addActionListener(this);
		transform.add(applyTransform);

		saveTransform = new JMenuItem("Save Transform");
		saveTransform.addActionListener(this);
		transform.add(saveTransform);

		return transform;
	}

	private JMenu createViewMenu() {
		JMenu view = new JMenu("View");

		resetView = new JMenuItem("Reset view");
		resetView.addActionListener(this);
		view.add(resetView);

		reloadImage = new JMenuItem("Reload Image");
		reloadImage.addActionListener(this);
		view.add(reloadImage);

		// center submenu
		JMenu menu = new JMenu("Center");

		centerOrigin = new JMenuItem("Origin");
		centerOrigin.addActionListener(this);
		menu.add(centerOrigin);

		centerUniverse = new JMenuItem("Universe");
		centerUniverse.addActionListener(this);
		menu.add(centerUniverse);
		view.add(menu);

		// set View submenu
		menu = new JMenu("Set view");
		viewposXY = new JMenuItem("+ XY");
		viewposXY.addActionListener(this);
		menu.add(viewposXY);
		viewposXZ = new JMenuItem("+ XZ");
		viewposXZ.addActionListener(this);
		menu.add(viewposXZ);
		viewposYZ = new JMenuItem("+ YZ");
		viewposYZ.addActionListener(this);
		menu.add(viewposYZ);
		viewnegXY = new JMenuItem("- XY");
		viewnegXY.addActionListener(this);
		menu.add(viewnegXY);
		viewnegXZ = new JMenuItem("- XZ");
		viewnegXZ.addActionListener(this);
		menu.add(viewnegXZ);
		viewnegYZ = new JMenuItem("- YZ");
		viewnegYZ.addActionListener(this);
		menu.add(viewnegYZ);
		view.add(menu);

		view.addSeparator();

		snapshot = new JMenuItem("Take snapshot");
		snapshot.addActionListener(this);
		view.add(snapshot);

		view.addSeparator();

		startAnimation = new JMenuItem("Start animation");
		startAnimation.addActionListener(this);
		view.add(startAnimation);

		stopAnimation = new JMenuItem("Stop animation");
		stopAnimation.addActionListener(this);
		view.add(stopAnimation);

		animationOptions = new JMenuItem("Change animation options");
		animationOptions.addActionListener(this);
		view.add(animationOptions);

		view.addSeparator();

		scalebar = new JMenuItem("Edit Scalebar");
		scalebar.addActionListener(this);
		view.add(scalebar);

		view.addSeparator();

		bgColor = new JMenuItem("Change background color");
		bgColor.addActionListener(this);
		view.add(bgColor);

		viewPreferences = new JMenuItem("View Preferences");
		viewPreferences.addActionListener(this);
		view.add(viewPreferences);

		return view;
	}

	private JMenu createHelpMenu() {
		JMenu help = new JMenu("Help");
		j3dproperties = new JMenuItem("Java 3D Properties");
		j3dproperties.addActionListener(this);
		help.add(j3dproperties);

		properties = new JMenuItem("Object Properties");
		properties.addActionListener(this);
		help.add(properties);

		return help;
	}

	@SuppressWarnings("unused")
	private JMenu createAttributesSubMenu() {
		JMenu attributes = new JMenu("Attributes");

		luts = new JMenuItem("Transfer function");
		luts.addActionListener(this);
		attributes.add(luts);

		channels = new JMenuItem("Change channels");
		channels.addActionListener(this);
		attributes.add(channels);

		color = new JMenuItem("Change color");
		color.addActionListener(this);
		attributes.add(color);

		transparency = new JMenuItem("Change transparency");
		transparency.addActionListener(this);
		attributes.add(transparency);

		threshold = new JMenuItem("Adjust threshold");
		threshold.addActionListener(this);
		attributes.add(threshold);

		shaded = new JCheckBoxMenuItem("Shade surface");
		shaded.setState(true);
		shaded.addItemListener(this);
		attributes.add(shaded);

		saturated = new JCheckBoxMenuItem("Saturated volume rendering");
		saturated.setState(false);
		saturated.addItemListener(this);
		attributes.add(saturated);

		colorSurface = new JMenuItem("Surface color");
		colorSurface.addActionListener(this);
		attributes.add(colorSurface);
		return attributes;
	}

	private JMenu createDisplayAsSubMenu() {
		JMenu display = new JMenu("Display as");

		displayAsVolume = new JMenuItem("Volume");
		displayAsVolume.addActionListener(this);
		display.add(displayAsVolume);

		displayAsOrtho = new JMenuItem("Orthoslice");
		displayAsOrtho.addActionListener(this);
		display.add(displayAsOrtho);

		displayAsMultiOrtho = new JMenuItem("Multi-orthoslice");
		displayAsMultiOrtho.addActionListener(this);
		display.add(displayAsMultiOrtho);

		displayAsSurface = new JMenuItem("Surface");
		displayAsSurface.addActionListener(this);
		display.add(displayAsSurface);

		displayAsSurfacePlot = new JMenuItem("Surface Plot 2D");
		displayAsSurfacePlot.addActionListener(this);
		display.add(displayAsSurfacePlot);

		return display;
	}

	private void changeBackgroundColor() {
		final Background background = ((ImageCanvas3D) universe.getCanvas())
				.getBG();
		final Color3f oldC = new Color3f();
		background.getColor(oldC);

		final ColorListener colorListener = new ColorListener() {
			@Override
			public void colorChanged(final Color3f color) {
				background.setColor(color);
				((ImageCanvas3D) universe.getCanvas()).render();
			}
		};
		showColorDialog("Adjust background color ...", oldC, colorListener,
				false, false);
	}

	private void showColorDialog(final String title, final Color3f oldC,
			final ColorListener colorListener,
			final boolean showDefaultCheckbox,
			final boolean showTimepointsCheckbox) {
		final GenericDialog gd = new GenericDialog(title, universe.getWindow());

		if (showDefaultCheckbox) {
			gd.addCheckbox("Use default color", oldC == null);
		}
		gd.addSlider("Red", 0, 255, oldC == null ? 255 : oldC.x * 255);
		gd.addSlider("Green", 0, 255, oldC == null ? 0 : oldC.y * 255);
		gd.addSlider("Blue", 0, 255, oldC == null ? 0 : oldC.z * 255);

		if (showTimepointsCheckbox) {
			gd.addCheckbox("Apply to all timepoints", true);
		}

		final Scrollbar rSlider = (Scrollbar) gd.getSliders().get(0);
		final Scrollbar gSlider = (Scrollbar) gd.getSliders().get(1);
		final Scrollbar bSlider = (Scrollbar) gd.getSliders().get(2);

		rSlider.setEnabled(oldC != null);
		gSlider.setEnabled(oldC != null);
		bSlider.setEnabled(oldC != null);

		if (showDefaultCheckbox) {
			final Checkbox cBox = (Checkbox) gd.getCheckboxes().get(0);
			cBox.addItemListener(new ItemListener() {
				@Override
				public void itemStateChanged(final ItemEvent e) {
					gd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
					rSlider.setEnabled(!cBox.getState());
					gSlider.setEnabled(!cBox.getState());
					bSlider.setEnabled(!cBox.getState());
					colorListener.colorChanged(new Color3f(
							rSlider.getValue() / 255f,
							gSlider.getValue() / 255f,
							bSlider.getValue() / 255f));
					gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}

		AdjustmentListener listener = new AdjustmentListener() {
			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				colorListener.colorChanged(new Color3f(
						rSlider.getValue() / 255f, gSlider.getValue() / 255f,
						bSlider.getValue() / 255f));
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosed(final WindowEvent e) {
				if (gd.wasCanceled()) {
					colorListener.colorChanged(oldC);
				} else {
					gd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
					gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
		gd.showDialog();
	}

	@Override
	public final void actionPerformed(final ActionEvent e) {
		Object src = e.getSource();

		if (src == color) {
			executer.changeColor(getSelected());
		} else if (src == bgColor) {
			changeBackgroundColor();
		} else if (src == scalebar) {
			executer.editScalebar();
		} else if (src == luts) {
			executer.adjustLUTs(getSelected());
		} else if (src == channels) {
			executer.changeChannels(getSelected());
		} else if (src == transparency) {
			executer.changeTransparency(getSelected());
		} else if (src == resetView) {
			executer.resetView();
		} else if (src == reloadImage) {
			tableCellview.fullReload(tableCellview.getDataValue());
		} else if (src == centerOrigin) {
			executer.centerOrigin();
		} else if (src == centerUniverse) {
			executer.centerUniverse();
		} else if (src == fitViewToUniverse) {
			executer.fitViewToUniverse();
		} else if (src == fitViewToContent) {
			executer.fitViewToContent(getSelected());
		} else if (src == snapshot) {
			executer.snapshot();
		} else if (src == startAnimation) {
			executer.startAnimation();
		} else if (src == stopAnimation) {
			executer.stopAnimation();
		} else if (src == animationOptions) {
			executer.changeAnimationOptions();
		} else if (src == threshold) {
			executer.changeThreshold(getSelected());
		} else if (src == displayAsVolume) {
			executer.displayAs(getSelected(), Content.VOLUME);
			updateMenus();
		} else if (src == displayAsOrtho) {
			executer.displayAs(getSelected(), Content.ORTHO);
			updateMenus();
		} else if (src == displayAsMultiOrtho) {
			executer.displayAs(getSelected(), Content.MULTIORTHO);
			updateMenus();
		} else if (src == displayAsSurface) {
			executer.displayAs(getSelected(), Content.SURFACE);
			updateMenus();
		} else if (src == displayAsSurfacePlot) {
			executer.displayAs(getSelected(), Content.SURFACE_PLOT2D);
			updateMenus();
		} else if (src == slices) {
			executer.changeSlices(getSelected());
		} else if (src == close) {
			executer.close();
		} else if (src == resetTransform) {
			executer.resetTransform(getSelected());
		} else if (src == setTransform) {
			executer.setTransform(getSelected());
		} else if (src == properties) {
			executer.contentProperties(getSelected());
		} else if (src == applyTransform) {
			executer.applyTransform(getSelected());
		} else if (src == saveTransform) {
			executer.saveTransform(getSelected());
		} else if (src == viewPreferences) {
			executer.viewPreferences();
		} else if (src == j3dproperties) {
			executer.j3dproperties();
		} else if (viewposXY == src) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					// Fixes the rotation bug. Waiting after reseting the view
					// results in a corect rotation.
					executer.resetView();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					universe.rotateToPositiveXY();
				}
			});
		} else if (viewposXZ == src) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					executer.resetView();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					universe.rotateToPositiveXZ();
				}
			});
		} else if (viewposYZ == src) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					executer.resetView();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					universe.rotateToPositiveYZ();
				}
			});
		} else if (viewnegXY == src) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					executer.resetView();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					universe.rotateToNegativeXY();
				}
			});
		} else if (viewnegXZ == src) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					executer.resetView();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					universe.rotateToNegativeXZ();
				}
			});
		} else if (viewnegYZ == src) {
			executer.execute(new Runnable() {
				@Override
				public void run() {
					executer.resetView();
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					universe.rotateToNegativeYZ();
				}
			});
		}
	}

	@Override
	public final void itemStateChanged(final ItemEvent e) {
		Object src = e.getSource();
		Content c = getSelected();
		if (src == coordinateSystem) {
			executer.showCoordinateSystem(c, coordinateSystem.getState());
		} else if (src == boundingBox) {
			executer.showBoundingBox(c, boundingBox.getState());
		} else if (src == show) {
			executer.showContent(c, show.getState());
		} else if (src == lock) {
			executer.setLocked(c, lock.getState());
		} else if (src == shaded) {
			executer.setShaded(c, shaded.getState());
		} else if (src == saturated) {
			executer.setSaturatedVolumeRendering(c, saturated.getState());
		}
	}

	private Content getSelected() {
		Content c = universe.getSelected();
		if (c != null) {
			return c;
		}
		if (universe.getContents().size() == 1) {
			return (Content) universe.contents().next();
		}
		return null;
	}

	// Universe Listener interface
	@Override
	public void transformationStarted(final View view) {
	}

	@Override
	public void transformationFinished(final View view) {
	}

	@Override
	public void canvasResized() {
	}

	@Override
	public void transformationUpdated(final View view) {
	}

	@Override
	public void universeClosed() {
	}

	@Override
	public final void contentAdded(final Content c) {
		updateMenus();
	}

	@Override
	public void contentRemoved(final Content c) {
	}

	@Override
	public final void contentSelected(final Content c) {
		updateMenus();
	}

	public final void updateMenus() {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				doUpdateMenus();
			}
		});
	}

	private void doUpdateMenus() {

		Content c = getSelected();

		displayAsVolume.setEnabled(c != null);
		displayAsSurface.setEnabled(c != null);
		displayAsSurfacePlot.setEnabled(c != null);
		displayAsOrtho.setEnabled(c != null);
		properties.setEnabled(c != null);

		color.setEnabled(c != null);
		transparency.setEnabled(c != null);
		threshold.setEnabled(c != null);
		channels.setEnabled(c != null);

		show.setEnabled(c != null);
		coordinateSystem.setEnabled(c != null);

		lock.setEnabled(c != null);
		setTransform.setEnabled(c != null);
		applyTransform.setEnabled(c != null);
		resetTransform.setEnabled(c != null);
		saveTransform.setEnabled(c != null);

		if (c == null) {
			return;
		}

		int t = c.getType();

		slices.setEnabled(t == Content.ORTHO || t == Content.MULTIORTHO);

		coordinateSystem.setState(c.hasCoord());
		lock.setState(c.isLocked());
		show.setState(c.isVisible());

		ImagePlus i = c.getImage();
		displayAsVolume.setEnabled(t != Content.VOLUME && i != null);
		displayAsOrtho.setEnabled(t != Content.ORTHO && i != null);
		displayAsSurface.setEnabled(t != Content.SURFACE && i != null);
		displayAsSurfacePlot.setEnabled(t != Content.SURFACE_PLOT2D
				&& i != null);
		displayAsMultiOrtho.setEnabled(t != Content.MULTIORTHO && i != null);
	}

	@Override
	public void contentChanged(final Content c) {
	}
}
