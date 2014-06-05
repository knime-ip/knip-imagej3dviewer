package org.knime.knip.imagej3d;

import org.knime.knip.base.activators.LinuxSystemLibraryConfig;
import org.knime.knip.base.activators.MacOSXSystemLibraryConfig;
import org.knime.knip.base.activators.NativeLibBundleActivator;
import org.knime.knip.base.activators.WindowsSystemLibraryConfig;

public class ImageJ3DActivator extends NativeLibBundleActivator {

	public ImageJ3DActivator() {
		super("org.knime.knip.imagej3d");

		addConfig(new LinuxSystemLibraryConfig(new String[] { "newt", "openal",
				"joal", "jocl", "gluegen-rt", "jogl_desktop", "jawt",
				"nativewindow_awt", "nativewindow_x11", }));

		addConfig(new WindowsSystemLibraryConfig(new String[] { "newt", "joal",
				"jocl", "gluegen-rt", "jogl_desktop", "jawt",
				"nativewindow_awt", "nativewindow_win32", "soft_oal" }));

		addConfig(new MacOSXSystemLibraryConfig(new String[] { "newt", "joal",
				"jocl", "gluegen-rt", "jogl_desktop", "jawt",
				"nativewindow_awt", "nativewindow_macosx", }));
	}

	@Override
	protected void load(final String s) {
		System.loadLibrary(s);
	}
}
