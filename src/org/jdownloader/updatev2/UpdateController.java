package org.jdownloader.updatev2;

import java.awt.Color;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.swing.ImageIcon;
import javax.swing.JFrame;

import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;
import jd.gui.swing.jdgui.components.toolbar.actions.UpdateAction;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
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
        logger = LogController.getInstance().getLogger(UpdateController.class.getName());
        settings = JsonConfig.create(UpdateSettings.class);

    }

    private UpdateHandler   handler;
    private boolean         running;
    private HashSet<Thread> confirmedThreads;
    private String          appid;
    private String          updaterid;

    public UpdateHandler getHandler() {
        return handler;
    }

    public void setHandler(UpdateHandler handler, ConfigInterface updaterSetup, String appid, String updaterid) {
        this.handler = handler;
        handler.runChecker();
        this.appid = appid;
        this.updaterid = updaterid;
        UpdateAction.getInstance().setEnabled(true);

    }

    private boolean isThreadConfirmed() {
        return confirmedThreads.contains(Thread.currentThread());
    }

    private void setUpdateConfirmed(boolean b) {
        if (b) {
            confirmedThreads.add(Thread.currentThread());
        } else {
            confirmedThreads.remove(Thread.currentThread());
        }
        // cleanup
        for (Iterator<Thread> it = confirmedThreads.iterator(); it.hasNext();) {
            Thread th = it.next();
            if (!th.isAlive()) it.remove();
        }

    }

    @Override
    public void updateGuiIcon(ImageIcon icon) {
    }

    @Override
    public void updateGuiText(String text) {
        lazyGetIcon().setTitle(text);
    }

    @Override
    public void updateGuiProgress(double progress) {
        lazyGetIcon().setIndeterminate(progress < 0);
        lazyGetIcon().setValue((int) progress);
    }

    public boolean hasWaitingUpdates() {
        String[] list = Application.getResource("tmp/update/data").list();
        return (list != null && list.length > 0);
    }

    public String getAppID() {
        return handler.getAppID();
    }

    public void runUpdateChecker(boolean manually) {

        handler.runUpdateCheck(manually);
    }

    @Override
    public void setRunning(boolean b) {
        this.running = b;
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (running) {
                    if (icon != null && lazyGetIcon().getParent() != null) {
                        //
                        return;
                    }
                    lazyGetIcon().setIndeterminate(true);
                    lazyGetIcon().setTitle(_GUI._.JDUpdater_JDUpdater_object_icon());
                    lazyGetIcon().setDescription(null);
                    JDGui.getInstance().getStatusBar().add(icon);

                } else {
                    lazyGetIcon().setIndeterminate(false);
                    JDGui.getInstance().getStatusBar().remove(icon);
                }
            }
        };

    }

    protected IconedProcessIndicator lazyGetIcon() {
        if (icon != null) return icon;

        icon = new EDTHelper<UpdateProgress>() {

            @Override
            public UpdateProgress edtRun() {
                if (icon != null) return icon;
                UpdateProgress icon = new UpdateProgress();
                ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setBackground(Color.LIGHT_GRAY);
                ((org.appwork.swing.components.circlebar.ImagePainter) icon.getValueClipPainter()).setForeground(Color.GREEN);
                icon.setTitle(_GUI._.JDUpdater_JDUpdater_object_icon());
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
        handler.setGuiVisible(b, true);
    }

    @Override
    public boolean handleException(Exception e) {
        return false;
    }

    public void setGuiToFront(JFrame mainFrame) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {

                if (handler != null && handler.isGuiVisible()) {
                    handler.setGuiVisible(true, true);

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

        if (!settings.isDoAskBeforeDownloadingAnUpdate()) return true;
        if (isThreadConfirmed()) return true;
        try {
            if (app && appDownloadSize < 0 || updater && updaterDownloadSize < 0) {
                Dialog.getInstance().showConfirmDialog(0, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_message(), null, _UPDATE._.confirmdialog_new_update_available_answer_now(), _UPDATE._.confirmdialog_new_update_available_answer_later());

            } else {

                long download = 0;
                if (app) {
                    download += appDownloadSize;

                }
                if (updater) {
                    download += updaterDownloadSize;
                }
                Dialog.getInstance().showConfirmDialog(0, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_message_sized(SizeFormatter.formatBytes(download)), null, _UPDATE._.confirmdialog_new_update_available_answer_now(), _UPDATE._.confirmdialog_new_update_available_answer_later());

            }

            // setUpdateConfirmed(true);
            return true;
        } catch (DialogClosedException e) {
            Log.exception(Level.WARNING, e);

        } catch (DialogCanceledException e) {
            Log.exception(Level.WARNING, e);

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
            if (handler.hasPendingSelfupdate()) {
                if (!isThreadConfirmed()) {
                    if (!handler.isGuiVisible() && settings.isDoNotAskJustInstallOnNextStartupEnabled()) return;
                    Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_for_install_message(), null, _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());

                    setUpdateConfirmed(true);
                    handler.setGuiVisible(true, true);
                }
                UpdateController.getInstance().installUpdates(null);
                return;
            }

            // no need to do this if we have a selfupdate pending
            InstallLog awfoverview = handler.createAWFInstallLog();
            if (awfoverview.getSourcePackages().size() == 0) {
                // Thread.sleep(1000);
                handler.setGuiFinished(null);

                return;
            }
            if (awfoverview.getModifiedFiles().size() == 0) {
                // empty package
                UpdateController.getInstance().installUpdates(awfoverview);
                handler.setGuiFinished(_UPDATE._.installframe_statusmsg_complete());

                return;
            }
            if (awfoverview.getModifiedRestartRequiredFiles().size() == 0) {

                if (settings.isDoNotAskToInstallPlugins()) {
                    // can install direct
                    UpdateController.getInstance().installUpdates(awfoverview);

                    HostPluginController.getInstance().invalidateCache();
                    CrawlerPluginController.invalidateCache();
                    handler.setGuiFinished(_UPDATE._.updatedplugins());
                    return;
                }

            }

            // we need at least one restart
            if (isThreadConfirmed()) {
                installUpdates(awfoverview);
            } else {
                if (!isThreadConfirmed()) {

                    if (!handler.isGuiVisible() && settings.isDoNotAskJustInstallOnNextStartupEnabled()) return;
                    List<String> rInstalls = handler.getRequestedInstalls();
                    List<String> ruInstalls = handler.getRequestedUnInstalls();
                    if (rInstalls.size() > 0 || ruInstalls.size() > 0) {
                        Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle_extensions(), _UPDATE._.confirmdialog_new_update_available_for_install_message(rInstalls.size(), ruInstalls.size()), null, _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());

                    } else {
                        Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_COUNTDOWN, _UPDATE._.confirmdialog_new_update_available_frametitle(), _UPDATE._.confirmdialog_new_update_available_for_install_message(), null, _UPDATE._.confirmdialog_new_update_available_answer_now_install(), _UPDATE._.confirmdialog_new_update_available_answer_later_install());
                    }
                    setUpdateConfirmed(true);
                    handler.setGuiVisible(true, true);
                }

                UpdateController.getInstance().installUpdates(awfoverview);
            }
        } catch (DialogNoAnswerException e) {
            logger.log(e);
            handler.setGuiVisible(false, false);
        }
    }

    // public static final String UPDATE = "update";
    // public static final String SELFTEST = "selftest";
    // public static final String SELFUPDATE_ERROR = "selfupdateerror";
    // public static final String AFTER_SELF_UPDATE = "afterupdate";

    // public static final String OK = "OK";

    public boolean hasPendingUpdates() {
        return handler.hasPendingUpdates();
    }

    public void installUpdates(InstallLog log) {

        handler.installPendingUpdates(log);
    }

    @Override
    public Process runExeAsynch(List<String> call, File root) throws IOException {

        call.addAll(RestartController.getInstance().getFilteredRestartParameters());
        final ProcessBuilder pb = ProcessBuilderFactory.create(call);
        pb.directory(root);
        Process process = pb.start();
        logger.logAsynch(process.getErrorStream());
        logger.logAsynch(process.getInputStream());
        return process;
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

}
