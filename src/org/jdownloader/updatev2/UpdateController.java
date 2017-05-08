package org.jdownloader.updatev2;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JFrame;

import jd.controlling.proxy.ProxyController;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.locator.RememberRelativeLocator;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class UpdateController implements UpdateCallbackInterface {
    private static final UpdateController INSTANCE = new UpdateController();

    /**
     * get the only existing instance of UpdateController. This is a singleton
     *
     * @return
     */
    public static UpdateController getInstance() {
        return UpdateController.INSTANCE;
    }

    private UpdateProgress icon;
    private LogSource      logger;

    public LogSource getLogger() {
        return logger;
    }

    private UpdateSettings settings;

    /**
     * Create a new instance of UpdateController. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    private UpdateController() {
        confirmedThreads = new HashSet<Thread>();
        eventSender = new UpdaterEventSender();
        logger = LogController.getInstance().getLogger(UpdateController.class.getName());
        settings = JsonConfig.create(UpdateSettings.class);
        installedRevisionJDU = readRevision("update/versioninfo/JDU/rev");
        installedRevisionJD = readRevision("update/versioninfo/JD/rev");
    }

    public int getInstalledRevisionJDU() {
        return installedRevisionJDU;
    }

    public int getInstalledRevisionJD() {
        return installedRevisionJD;
    }

    private UpdateHandler      handler;
    private boolean            running;
    private HashSet<Thread>    confirmedThreads;
    private String             appid;
    private String             updaterid;
    private UpdaterEventSender eventSender;
    private Icon               statusIcon;
    private String             statusLabel;
    private double             statusProgress      = -1;
    private volatile boolean   hasPendingUpdates   = false;
    private int                installedRevisionJDU;
    private int                installedRevisionJD;
    public static final int    DEBUG_SELFTEST_PORT = System.getProperty("DEBUG_SELFTEST") == null ? -1 : Integer.parseInt(System.getProperty("DEBUG_SELFTEST"));

    public UpdateHandler getHandler() {
        return handler;
    }

    public void setHandler(UpdateHandler handler, ConfigInterface updaterSetup, String appid, String updaterid) {
        this.handler = handler;
        LogSource newLogger = handler.getLogger();
        if (newLogger != null) {
            if (logger != null) {
                logger.close();
            }
            logger = newLogger;
        }
        this.appid = appid;
        this.updaterid = updaterid;
        hasPendingUpdates = handler.hasPendingUpdates();
        handler.startIntervalChecker();
        try {
            jd.SecondLevelLaunch.UPDATE_HANDLER_SET.setReached();
        } catch (Throwable e) {
        }
        // UpdateAction.getInstance().setEnabled(true);
    }

    private synchronized boolean isThreadConfirmed() {
        return confirmedThreads.contains(Thread.currentThread());
    }

    private synchronized void setUpdateConfirmed(boolean b) {
        if (b) {
            confirmedThreads.add(Thread.currentThread());
        } else {
            confirmedThreads.remove(Thread.currentThread());
        }
        // cleanup
        for (Iterator<Thread> it = confirmedThreads.iterator(); it.hasNext();) {
            Thread th = it.next();
            if (!th.isAlive()) {
                it.remove();
            }
        }
    }

    @Override
    public void updateGuiIcon(ImageIcon icon) {
        this.statusIcon = icon;
        eventSender.fireEvent(new UpdateStatusUpdateEvent(this, statusLabel, statusIcon, statusProgress));
    }

    @Override
    public void updateGuiText(String text) {
        if (!org.appwork.utils.Application.isHeadless()) {
            lazyGetIcon().setTitle(text);
        }
        this.statusLabel = text;
        eventSender.fireEvent(new UpdateStatusUpdateEvent(this, statusLabel, statusIcon, statusProgress));
    }

    @Override
    public void updateGuiProgress(double progress) {
        this.statusProgress = progress;
        if (!org.appwork.utils.Application.isHeadless()) {
            lazyGetIcon().setIndeterminate(progress < 0);
            lazyGetIcon().setValue((int) progress);
        }
        eventSender.fireEvent(new UpdateStatusUpdateEvent(this, statusLabel, statusIcon, statusProgress));
    }

    public String getAppID() {
        final UpdateHandler lhandler = handler;
        if (lhandler == null) {
            return "NotConnected";
        }
        return lhandler.getAppID();
    }

    public void runUpdateChecker(boolean manually) {
        final UpdateHandler lhandler = handler;
        if (lhandler == null) {
            return;
        }
        lhandler.runUpdateCheck(manually);
    }

    private int readRevision(String rev) {
        try {
            final File revisionFile = Application.getResource(rev);
            if (revisionFile.exists()) {
                final String string = IO.readFileToTrimmedString(revisionFile);
                if (string != null && string.matches("^\\d+$")) {
                    return Integer.parseInt(string);
                }
                final Map<String, Object> ret = JSonStorage.restoreFromString(string, TypeRef.HASHMAP);
                if (ret != null && ret.containsKey("id")) {
                    return ((Number) ret.get("id")).intValue();
                }
            }
        } catch (Throwable e) {
            LoggerFactory.getDefaultLogger().log(e);
        }
        return -1;
    }

    @Override
    public void setRunning(boolean b) {
        this.running = b;
        if (!b) {
            installedRevisionJDU = readRevision("update/versioninfo/JDU/rev");
            installedRevisionJD = readRevision("update/versioninfo/JD/rev");
        }
        if (!org.appwork.utils.Application.isHeadless()) {
            new EDTRunner() {
                @Override
                protected void runInEDT() {
                    if (running) {
                        if (icon != null && lazyGetIcon().getParent() != null) {
                            //
                            return;
                        }
                        lazyGetIcon().setIndeterminate(true);
                        lazyGetIcon().setTitle(_GUI.T.JDUpdater_JDUpdater_object_icon());
                        lazyGetIcon().setDescription(null);
                        JDGui.getInstance().getStatusBar().addProcessIndicator(icon);
                    } else {
                        lazyGetIcon().setIndeterminate(false);
                        JDGui.getInstance().getStatusBar().removeProcessIndicator(icon);
                    }
                }
            };
        }
    }

    protected IconedProcessIndicator lazyGetIcon() {
        if (icon != null) {
            return icon;
        }
        icon = new EDTHelper<UpdateProgress>() {
            @Override
            public UpdateProgress edtRun() {
                if (icon != null) {
                    return icon;
                }
                UpdateProgress icon = new UpdateProgress();
                ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setBackground(Color.LIGHT_GRAY);
                ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setForeground(Color.GREEN);
                icon.setTitle(_GUI.T.JDUpdater_JDUpdater_object_icon());
                icon.setEnabled(true);
                icon.addMouseListener(new MouseListener() {
                    @Override
                    public void mouseReleased(MouseEvent e) {
                    }

                    @Override
                    public void mousePressed(MouseEvent e) {
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                    }

                    @Override
                    public void mouseEntered(MouseEvent e) {
                    }

                    @Override
                    public void mouseClicked(MouseEvent e) {
                        // JDUpdater.getInstance().startUpdate(false);
                    }
                });
                return icon;
            }
        }.getReturnValue();
        return icon;
    }

    public boolean isRunning() {
        return running;
    }

    public void setGuiVisible(boolean b) {
        final UpdateHandler lhandler = handler;
        if (lhandler != null) {
            lhandler.setGuiVisible(b, true);
        }
    }

    @Override
    public boolean handleException(Exception e) {
        return false;
    }

    public void setGuiToFront(JFrame mainFrame) {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                final UpdateHandler lhandler = handler;
                if (lhandler != null && lhandler.isGuiVisible()) {
                    lhandler.setGuiVisible(true, true);
                }
            }
        };
    }

    @Override
    public void onGuiVisibilityChanged(final Window window, boolean oldValue, boolean newValue) {
        // if (!oldValue && newValue && window != null) {
        // new EDTRunner() {
        //
        // @Override
        // protected void runInEDT() {
        // if (JDGui.getInstance().getMainFrame().isVisible()) {
        // Point ret = SwingUtils.getCenter(JDGui.getInstance().getMainFrame(), window);
        // window.setLocation(ret);
        // }
        // }
        // };
        // }
    }

    @Override
    public org.appwork.utils.swing.locator.Locator getGuiLocator() {
        if (JDGui.getInstance().getMainFrame() != null) {
            //
            return new RememberRelativeLocator("Updater", JDGui.getInstance().getMainFrame());
        }
        return null;
    }

    @Override
    public boolean doContinueLoopStarted() {
        return true;
    }

    @Override
    public boolean doContinueUpdateAvailable(boolean app, boolean updater, long appDownloadSize, long updaterDownloadSize, int appRevision, int updaterRevision, int appDestRevision, int updaterDestRevision) {
        if (!settings.isDoAskBeforeDownloadingAnUpdate()) {
            return true;
        }
        if (isThreadConfirmed()) {
            return true;
        }
        try {
            if (app && appDownloadSize < 0 || updater && updaterDownloadSize < 0) {
                confirm(0, _UPDATE.T.confirmdialog_new_update_available_frametitle(), _UPDATE.T.confirmdialog_new_update_available_message(), _UPDATE.T.confirmdialog_new_update_available_answer_now(), _UPDATE.T.confirmdialog_new_update_available_answer_later());
            } else {
                long download = 0;
                if (app) {
                    download += appDownloadSize;
                }
                if (updater) {
                    download += updaterDownloadSize;
                }
                confirm(0, _UPDATE.T.confirmdialog_new_update_available_frametitle(), _UPDATE.T.confirmdialog_new_update_available_message_sized(SizeFormatter.formatBytes(download)), _UPDATE.T.confirmdialog_new_update_available_answer_now(), _UPDATE.T.confirmdialog_new_update_available_answer_later());
            }
            // setUpdateConfirmed(true);
            return true;
        } catch (DialogClosedException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        } catch (DialogCanceledException e) {
            org.appwork.utils.logging2.extmanager.LoggerFactory.getDefaultLogger().log(e);
        }
        return false;
    }

    @Override
    public boolean doContinuePackageAvailable(boolean app, boolean updater, long appDownloadSize, long updaterDownloadSize, int appRevision, int updaterRevision, int appDestRevision, int updaterDestRevision) {
        return true;
    }

    @Override
    public boolean doContinueReadyForExtracting(boolean app, boolean updater, File fileclient, File fileself) {
        return true;
    }

    @Override
    public void onResults(boolean app, boolean updater, int clientRevision, int clientDestRevision, int selfRevision, int selfDestRevision, File awfFileclient, File awfFileSelf, File selfWOrkingDir, boolean jdlaunched) throws InterruptedException, IOException {
        try {
            logger.info("onResult");
            if (handler.hasPendingSelfupdate()) {
                fireUpdatesAvailable(false, handler.createAWFInstallLog());
                if (!isThreadConfirmed()) {
                    if (handler.isGuiVisible() || settings.isDoAskMeBeforeInstallingAnUpdateEnabled()) {
                        logger.info("ASK for installing selfupdate");
                        confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE.T.confirmdialog_new_update_available_frametitle(), _UPDATE.T.confirmdialog_new_update_available_for_install_message(), _UPDATE.T.confirmdialog_new_update_available_answer_now_install(), _UPDATE.T.confirmdialog_new_update_available_answer_later_install());
                        setUpdateConfirmed(true);
                        handler.setGuiVisible(true, true);
                    } else {
                        return;
                    }
                }
                logger.info("Run Installing Updates");
                UpdateController.getInstance().installUpdates(null);
                return;
            }
            // no need to do this if we have a selfupdate pending
            InstallLog awfoverview = handler.createAWFInstallLog();
            logger.info(JSonStorage.toString(awfoverview));
            if (awfoverview.getSourcePackages().size() == 0) {
                logger.info("Nothing to install " + handler.isGuiVisible());
                // Thread.sleep(1000);
                handler.setGuiFinished(null);
                if (settings.isAutohideGuiIfThereAreNoUpdatesEnabled()) {
                    handler.setGuiVisible(false, false);
                }
                fireUpdatesAvailable(false, null);
                return;
            }
            if (awfoverview.getModifiedFiles().size() == 0) {
                // empty package
                logger.info("Nothing to install2");
                UpdateController.getInstance().installUpdates(awfoverview);
                handler.setGuiFinished(null);
                if (settings.isAutohideGuiIfThereAreNoUpdatesEnabled()) {
                    handler.setGuiVisible(false, false);
                }
                fireUpdatesAvailable(false, null);
                return;
            }
            if (awfoverview.getModifiedRestartRequiredFiles().size() == 0) {
                logger.info("Only directs");
                // can install direct
                if (!settings.isInstallUpdatesSilentlyIfPossibleEnabled()) {
                    logger.info("ask to install plugins");
                    confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE.T.confirmdialog_new_update_available_frametitle(), _UPDATE.T.confirmdialog_new_update_available_for_install_message_plugin(), _UPDATE.T.confirmdialog_new_update_available_answer_now_install(), _UPDATE.T.confirmdialog_new_update_available_answer_later_install());
                }
                logger.info("run install");
                UpdateController.getInstance().installUpdates(awfoverview);
                logger.info("start scanner");
                new Thread("PluginScanner") {
                    public void run() {
                        HostPluginController.getInstance().invalidateCache();
                        CrawlerPluginController.invalidateCache();
                        HostPluginController.getInstance().ensureLoaded();
                        CrawlerPluginController.getInstance().ensureLoaded();
                    }
                }.start();
                logger.info("set gui finished");
                handler.setGuiFinished(_UPDATE.T.updatedplugins());
                if (settings.isAutohideGuiIfSilentUpdatesWereInstalledEnabled()) {
                    handler.setGuiVisible(false, false);
                }
                fireUpdatesAvailable(false, null);
                return;
            }
            fireUpdatesAvailable(false, awfoverview);
            // we need at least one restart
            if (isThreadConfirmed()) {
                installUpdates(awfoverview);
                fireUpdatesAvailable(false, null);
            } else {
                if (handler.isGuiVisible() || settings.isDoAskMeBeforeInstallingAnUpdateEnabled()) {
                    List<String> rInstalls = handler.getRequestedInstalls();
                    List<String> ruInstalls = handler.getRequestedUnInstalls();
                    if (rInstalls.size() > 0 || ruInstalls.size() > 0) {
                        confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE.T.confirmdialog_new_update_available_frametitle_extensions(), _UPDATE.T.confirmdialog_new_update_available_for_install_message_extensions(rInstalls.size(), ruInstalls.size()), _UPDATE.T.confirmdialog_new_update_available_answer_now_install(), _UPDATE.T.confirmdialog_new_update_available_answer_later_install());
                    } else {
                        confirm(UIOManager.LOGIC_COUNTDOWN, _UPDATE.T.confirmdialog_new_update_available_frametitle(), _UPDATE.T.confirmdialog_new_update_available_for_install_message(), _UPDATE.T.confirmdialog_new_update_available_answer_now_install(), _UPDATE.T.confirmdialog_new_update_available_answer_later_install());
                    }
                    setUpdateConfirmed(true);
                    handler.setGuiVisible(true, true);
                    UpdateController.getInstance().installUpdates(awfoverview);
                    fireUpdatesAvailable(false, null);
                } else {
                    return;
                }
            }
        } catch (DialogNoAnswerException e) {
            logger.log(e);
            handler.setGuiVisible(false, false);
        } finally {
        }
    }

    // public static final String UPDATE = "update";
    // public static final String SELFTEST = "selftest";
    // public static final String SELFUPDATE_ERROR = "selfupdateerror";
    // public static final String AFTER_SELF_UPDATE = "afterupdate";
    // public static final String OK = "OK";
    private void fireUpdatesAvailable(boolean self, InstallLog installLog) {
        hasPendingUpdates = handler.hasPendingUpdates();
        eventSender.fireEvent(new UpdaterEvent(this, UpdaterEvent.Type.UPDATES_AVAILABLE, self, installLog));
    }

    public UpdaterEventSender getEventSender() {
        return eventSender;
    }

    private void confirm(int flags, String title, String message, String ok, String no) throws DialogCanceledException, DialogClosedException {
        final UpdateHandler lhandler = handler;
        final ConfirmUpdateDialog cd = new ConfirmUpdateDialog(flags, title, message, null, ok, no) {
            @Override
            protected Window getDesiredRootFrame() {
                if (lhandler == null) {
                    return null;
                }
                return lhandler.getGuiFrame();
            }
        };
        UIOManager.I().show(ConfirmDialogInterface.class, cd).throwCloseExceptions();
        if (cd.isClosedBySkipUntilNextRestart()) {
            if (lhandler != null) {
                lhandler.stopIntervalChecker();
            }
            throw new DialogCanceledException(0);
        }
    }

    public boolean hasPendingUpdates() {
        return hasPendingUpdates;
    }

    public void installUpdates(InstallLog log) {
        handler.installPendingUpdates(log);
        handler.clearInstallLogs();
    }

    @Override
    public Process runExeAsynch(List<String> call, File root) throws IOException {
        if (DEBUG_SELFTEST_PORT > 0) {
            // -Xdebug -Xrunjdwp:transport=dt_socket,server=y,address=8000,suspend=y
            call.addAll(RestartController.getInstance().getFilteredRestartParameters());
            call.add(1, "-Xdebug");
            call.add(2, "-Xrunjdwp:transport=dt_socket,server=y,address=" + DEBUG_SELFTEST_PORT + ",suspend=y");
            logger.info("Call: " + call + " in " + root);
            if (CrossSystem.isWindows()) {
                StringBuilder sb = new StringBuilder();
                sb.append("@echo SelfTest for Windows").append("\r\n");
                long time = System.currentTimeMillis();
                sb.append("@echo The Selftest will start now and write all outputs in this window and to " + new File(root, "self_log_err/std" + time + ".txt")).append("\r\n");
                for (String c : call) {
                    if (sb.length() > 0) {
                        sb.append(" ");
                    }
                    sb.append("\"").append(c).append("\"");
                }
                File tmp = Application.getTempResource("selftestLaunch.bat");
                tmp.delete();
                sb.append(" >self_log_std" + time + ".txt  2>self_log_err" + time + ".txt\r\ntype self_log_std" + time + ".txt\r\ntype self_log_err" + time + ".txt");
                sb.append("\r\n");
                sb.append("@echo Please close this window now.");
                IO.writeStringToFile(tmp, sb.toString());
                ArrayList<String> newList = new ArrayList<String>();
                newList.add("cmd");
                newList.add("/C");
                newList.add("start");
                newList.add("/wait");
                newList.add(tmp.getAbsolutePath());
                call = newList;
            }
            final ProcessBuilder pb = ProcessBuilderFactory.create(call);
            pb.redirectErrorStream(true);
            pb.directory(root);
            final Process process = pb.start();
            if (process != null) {
                // logger.logAsynch(process.getErrorStream());
                logger.logAsynch(process.getInputStream());
            }
            return process;
        } else {
            call.addAll(RestartController.getInstance().getFilteredRestartParameters());
            logger.info("Start Process: " + call);
            final ProcessBuilder pb = ProcessBuilderFactory.create(call);
            pb.directory(root);
            Process process = pb.start();
            logger.logAsynch(process.getErrorStream());
            logger.logAsynch(process.getInputStream());
            return process;
        }
    }

    public boolean isExtensionInstalled(String id) {
        return handler != null && handler.isExtensionInstalled(id);
    }

    public boolean isHandlerSet() {
        return handler != null;
    }

    public void runExtensionUnInstallation(String id) throws InterruptedException {
        handler.uninstallExtension(id);
    }

    public void runExtensionInstallation(String id) throws InterruptedException {
        handler.installExtension(id);
    }

    public void waitForUpdate() throws InterruptedException {
        handler.waitForUpdate();
    }

    public String[] listExtensionIds() throws IOException {
        return handler.getOptionalsList();
    }

    @Override
    public HTTPProxy updateProxyAuth(int retries, HTTPProxy usedProxy, List<String> proxyAuths, URL url) {
        return ProxyController.getInstance().updateProxyAuthForUpdater(retries, usedProxy, proxyAuths, url);
    }

    @Override
    public List<HTTPProxy> selectProxy(URL url) {
        ArrayList<HTTPProxy> ret = new ArrayList<HTTPProxy>();
        List<HTTPProxy> lst = ProxyController.getInstance().getProxiesForUpdater(url);
        for (HTTPProxy p : lst) {
            ret.add(new ProxyClone(p));
        }
        return ret;
    }

    /**
     * forces the updatesystem to reinstall the existions.
     *
     * @param list
     */
    public void runExtensionsFullUpdate(ArrayList<String> list) {
        if (handler == null || !Application.isJared(null)) {
            return;
        }
        handler.requestFullExtensionUpdate(list.toArray(new String[] {}));
        runUpdateChecker(false);
    }
}
