package org.jdownloader.gui.swt.widgets;

import java.io.File;

import jd.gui.swing.jdgui.JDGui;

import org.appwork.utils.logging2.LogSource;
import org.jdownloader.logging.LogController;

import chrriis.dj.nativeswing.swtimpl.NativeInterface;
import chrriis.dj.nativeswing.swtimpl.components.JDirectoryDialog;

public class NativeUtils {

    public static LogSource LOGGER = LogController.getInstance().getLogger(NativeUtils.class.getName());
    private static boolean  INIT   = false;

    public static File open(final File path, final boolean packager, final String title) {

        JDirectoryDialog directoryDialog = new JDirectoryDialog();
        directoryDialog.setTitle(title);
        directoryDialog.setSelectedDirectory(path.getAbsolutePath());

        directoryDialog.show(JDGui.getInstance().getMainFrame());
        String ret = directoryDialog.getSelectedDirectory();

        return ret == null ? null : new File(ret);
    }

    public synchronized static void init() {
        if (INIT) {
            return;
        }
        long start = System.currentTimeMillis();
        try {

            NativeInterface.open();
            NativeInterface.runEventPump();

        } catch (Throwable e) {
            LOGGER.log(e);

        } finally {
            LOGGER.info("Native Init took: " + (System.currentTimeMillis() - start) + " ms");
            INIT = true;
        }
    }

}
