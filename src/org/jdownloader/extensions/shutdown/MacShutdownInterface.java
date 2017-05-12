package org.jdownloader.extensions.shutdown;

import java.io.File;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.nutils.Executer;
import jd.utils.JDUtilities;

import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.extensions.shutdown.translate.T;
import org.jdownloader.updatev2.ForcedShutdown;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class MacShutdownInterface extends ShutdownInterface {
    private final File           osascript = new File("/usr/bin/osascript");
    private final LogSource      logger;
    private final ShutdownConfig settings;

    public MacShutdownInterface(ShutdownExtension shutdownExtension) {
        logger = shutdownExtension.getLogger();
        settings = shutdownExtension.getSettings();
    }

    @Override
    public Mode[] getSupportedModes() {
        if (osascript.isFile() && osascript.canExecute()) {
            return new Mode[] { Mode.SHUTDOWN, Mode.STANDBY, Mode.CLOSE };
        } else {
            return new Mode[] { Mode.SHUTDOWN, Mode.CLOSE };
        }
    }

    private void stopActivity() {
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
        StorageHandler.flushWrites();
    }

    @Override
    public void requestMode(Mode mode, boolean force) {
        switch (mode) {
        case SHUTDOWN:
            if (force) {
                try {
                    JDUtilities.runCommand("sudo", new String[] { "shutdown", "-p", "now" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("sudo", new String[] { "shutdown", "-h", "now" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            } else {
                if (osascript.isFile() && osascript.canExecute()) {
                    try {
                        JDUtilities.runCommand(osascript.getAbsolutePath(), new String[] { "-e", "tell application \"Finder\" to shut down" }, null, 0);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                } else {
                    return;
                }
            }
            stopActivity();
            RestartController.getInstance().exitAsynch(new ForcedShutdown());
            break;
        case STANDBY:
            stopActivity();
            try {
                JDUtilities.runCommand(osascript.getAbsolutePath(), new String[] { "-e", "tell application \"Finder\" to sleep" }, null, 0);
            } catch (Throwable e) {
                logger.log(e);
            }
            break;
        case CLOSE:
            stopActivity();
            RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
            break;
        default:
            break;
        }
    }

    @Override
    public void prepareMode(Mode mode) {
        if (osascript.isFile() && osascript.canExecute() && !Application.isHeadless() && !settings.isForceForMacInstalled()) {
            try {
                Dialog.getInstance().showConfirmDialog(0, T.T.install_title(), T.T.install_msg());
                final Executer exec = new Executer("/usr/bin/osascript");
                final File tmp = Application.getTempResource("osxnopasswordforshutdown.scpt");
                FileCreationManager.getInstance().delete(tmp, null);
                try {
                    IO.writeToFile(tmp, IO.readURL(getClass().getResource("osxnopasswordforshutdown.scpt")));
                    exec.addParameter(tmp.getAbsolutePath());
                    exec.setWaitTimeout(0);
                    exec.start();
                } finally {
                    if (!tmp.delete()) {
                        tmp.deleteOnExit();
                    }
                }
                settings.setForceForMacInstalled(true);
            } catch (final Throwable e) {
                logger.log(e);
                settings.setForceForMacInstalled(false);
            }
        }
    }
}
