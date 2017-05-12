package org.jdownloader.extensions.shutdown;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.utils.JDUtilities;

import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.updatev2.ForcedShutdown;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class WindowsShutdownInterface extends ShutdownInterface {
    private final LogSource logger;

    public WindowsShutdownInterface(ShutdownExtension shutdownExtension) {
        logger = shutdownExtension.getLogger();
    }

    @Override
    public Mode[] getSupportedModes() {
        return new Mode[] { Mode.SHUTDOWN, Mode.HIBERNATE, Mode.STANDBY, Mode.LOGOFF, Mode.CLOSE };
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
            stopActivity();
            if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_NT)) {
                final String[] cmdLine;
                if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_VISTA)) {
                    if (force) {
                        cmdLine = new String[] { "/s", "/f", "/t", "01" };
                    } else {
                        cmdLine = new String[] { "/s", "/t", "01" };
                    }
                } else {
                    if (force) {
                        cmdLine = new String[] { "-s", "-f", "-t", "01" };
                    } else {
                        cmdLine = new String[] { "-s", "-t", "01" };
                    }
                }
                try {
                    JDUtilities.runCommand("shutdown.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            } else {
                try {
                    JDUtilities.runCommand("rundll32.exe", new String[] { "User,ExitWindows" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("rundll32.exe", new String[] { "Shell32,SHExitWindowsEx", "1" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            if (CrossSystem.OS == OperatingSystem.WINDOWS_2000 || CrossSystem.OS == OperatingSystem.WINDOWS_NT) {
                /* also try extra methods for windows2000 and nt */
                try {
                    final File f = Application.getTempResource("shutdown.vbs");
                    try {
                        IO.writeStringToFile(f, "set WshShell = CreateObject(\"WScript.Shell\")\r\nWshShell.SendKeys \"^{ESC}^{ESC}^{ESC}{UP}{ENTER}{ENTER}\"\r\n");
                        JDUtilities.runCommand("cmd", new String[] { "/c", "start", "/min", "cscript", f.getAbsolutePath() }, null, 0);
                    } finally {
                        if (!f.delete()) {
                            f.deleteOnExit();
                        }
                    }
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            RestartController.getInstance().exitAsynch(new ForcedShutdown());
            break;
        case HIBERNATE:
            if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_NT)) {
                stopActivity();
                final String cmdLine[] = new String[] { "hibernate on" };
                try {
                    JDUtilities.runCommand("powercfg.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                final String runDll[] = new String[] { "powrprof.dll,SetSuspendState", "1,1,0" };
                try {
                    JDUtilities.runCommand("rundll32.exe", runDll, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\rundll32.exe", runDll, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            break;
        case STANDBY:
            if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_NT)) {
                stopActivity();
                final String cmdLine[] = new String[] { "hibernate off" };
                try {
                    JDUtilities.runCommand("powercfg.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                final String runDll[] = new String[] { "powrprof.dll,SetSuspendState", "0,1,0" };
                try {
                    JDUtilities.runCommand("rundll32.exe", runDll, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\rundll32.exe", runDll, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            break;
        case LOGOFF:
            stopActivity();
            if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_NT)) {
                // https://www.computerhope.com/shutdown.htm
                final String[] cmdLine;
                if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_VISTA)) {
                    if (force) {
                        cmdLine = new String[] { "/l", "/f", "/t", "01" };
                    } else {
                        cmdLine = new String[] { "/l", "/t", "01" };
                    }
                } else {
                    if (force) {
                        cmdLine = new String[] { "-l", "-f", "-t", "01" };
                    } else {
                        cmdLine = new String[] { "-l", "-t", "01" };
                    }
                }
                try {
                    JDUtilities.runCommand("shutdown.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", cmdLine, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            } else {
                try {
                    // https://arstechnica.com/civis/viewtopic.php?t=960898
                    JDUtilities.runCommand("rundll32.exe", new String[] { "User,ExitWindows", "0,0" }, null, 0);
                    JDUtilities.runCommand("rundll32.exe", new String[] { "User,ExitWindows", "0,0" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("rundll32.exe", new String[] { "Shell32,SHExitWindowsEx", "0" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            RestartController.getInstance().exitAsynch(new ForcedShutdown());
            break;
        case CLOSE:
            stopActivity();
            RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
            break;
        default:
            break;
        }
    }

    private Response execute(String[] command) throws IOException, UnsupportedEncodingException, InterruptedException {
        final ProcessBuilder probuilder = ProcessBuilderFactory.create(command);
        final Process process = probuilder.start();
        final Response ret = new Response();
        ret.setStd(IO.readInputStreamToString(process.getInputStream()));
        ret.setErr(IO.readInputStreamToString(process.getErrorStream()));
        ret.setExit(process.waitFor());
        return ret;
    }

    private final void checkStandbyHibernateSettings(Mode newValue) {
        try {
            final String path = CrossSystem.is64BitOperatingSystem() ? Application.getResource("tools\\Windows\\elevate\\Elevate64.exe").getAbsolutePath() : Application.getResource("tools\\Windows\\elevate\\Elevate32.exe").getAbsolutePath();
            switch (newValue) {
            case HIBERNATE:
                if (!isHibernateActivated()) {
                    Dialog.getInstance().showMessageDialog(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, org.jdownloader.extensions.shutdown.translate.T.T.show_admin());
                    try {
                        logger.info(execute(new String[] { path, "powercfg", "-hibernate", "on" }).toString());
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
                break;
            case STANDBY:
                if (isHibernateActivated()) {
                    Dialog.getInstance().showMessageDialog(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, org.jdownloader.extensions.shutdown.translate.T.T.show_admin());
                    try {
                        logger.info(execute(new String[] { path, "powercfg", "-hibernate", "off" }).toString());
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
                break;
            default:
                break;
            }
        } catch (Throwable e) {
            logger.log(e);
        }
    }

    private boolean isHibernateActivated() throws UnsupportedEncodingException, IOException, InterruptedException {
        final Response status = execute(new String[] { "powercfg", "-a" });
        logger.info(status.toString());
        if (status.getStd() != null) {
            final String std = status.getStd();
            if (std.contains("Ruhezustand wurde nicht aktiviert")) {
                return false;
            }
            if (std.contains("Hibernation has not been enabled")) {
                return false;
            }
            if (std.contains("La mise en veille") && std.contains("n'a pas")) {
                return false;
            }
            if (std.contains("Hibernation")) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void prepareMode(Mode mode) {
        checkStandbyHibernateSettings(mode);
    }
}
