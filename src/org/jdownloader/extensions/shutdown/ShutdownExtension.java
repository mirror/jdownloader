//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package org.jdownloader.extensions.shutdown;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import jd.SecondLevelLaunch;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.controlling.linkcollector.LinkCollector;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.controlling.StateMachine;
import org.appwork.shutdown.BasicShutdownRequest;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.controlling.contextmenu.ActionData;
import org.jdownloader.controlling.contextmenu.ContextMenuManager;
import org.jdownloader.controlling.contextmenu.MenuContainerRoot;
import org.jdownloader.controlling.contextmenu.MenuExtenderHandler;
import org.jdownloader.controlling.contextmenu.MenuItemData;
import org.jdownloader.controlling.contextmenu.MenuLink;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.shutdown.actions.ShutdownToggleAction;
import org.jdownloader.extensions.shutdown.translate.ShutdownTranslation;
import org.jdownloader.gui.mainmenu.MenuManagerMainmenu;
import org.jdownloader.gui.mainmenu.container.ExtensionsMenuContainer;
import org.jdownloader.gui.toolbar.MenuManagerMainToolbar;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.ForcedShutdown;
import org.jdownloader.updatev2.RestartController;
import org.jdownloader.updatev2.SmartRlyExitRequest;

public class ShutdownExtension extends AbstractExtension<ShutdownConfig, ShutdownTranslation> implements StateEventListener, MenuExtenderHandler {

    private Thread              shutdown = null;

    private ShutdownConfigPanel configPanel;

    public ShutdownConfigPanel getConfigPanel() {
        return configPanel;
    }

    @Override
    public boolean isHeadlessRunnable() {
        return true;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public ShutdownExtension() throws StartException {
        setTitle(T.jd_plugins_optional_jdshutdown());
    }

    private void shutdown() {
        logger.info("shutdown");
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
        switch (CrossSystem.OS) {
        case WINDOWS_2003:
        case WINDOWS_VISTA:
        case WINDOWS_XP:
        case WINDOWS_7:
        case WINDOWS_8:
        case WINDOWS_SERVER_2012:
            /* modern windows versions */
        case WINDOWS_2000:
        case WINDOWS_NT:
        case WINDOWS_SERVER_2008:
        case WINDOWS_10:
            /* not so modern windows versions */
            if (getSettings().isForceShutdownEnabled()) {
                /* force shutdown */
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            } else {
                /* normal shutdown */
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            if (CrossSystem.OS == OperatingSystem.WINDOWS_2000 || CrossSystem.OS == OperatingSystem.WINDOWS_NT) {
                /* also try extra methods for windows2000 and nt */
                try {
                    final File f = Application.getTempResource("shutdown.vbs");
                    f.deleteOnExit();
                    IO.writeStringToFile(f, "set WshShell = CreateObject(\"WScript.Shell\")\r\nWshShell.SendKeys \"^{ESC}^{ESC}^{ESC}{UP}{ENTER}{ENTER}\"\r\n");
                    try {
                        JDUtilities.runCommand("cmd", new String[] { "/c", "start", "/min", "cscript", f.getAbsolutePath() }, null, 0);
                    } finally {
                        FileCreationManager.getInstance().delete(f, null);
                    }
                } catch (Throwable e) {
                }
            }
            break;
        case WINDOWS_OTHERS:
            /* older windows versions */
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "user,ExitWindows" }, null, 0);
            } catch (Throwable e) {
                logger.log(e);
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "Shell32,SHExitWindowsEx", "1" }, null, 0);
            } catch (Throwable e) {
                logger.log(e);
            }
            break;
        case MAC:
            /* mac os */
            if (getSettings().isForceShutdownEnabled()) {
                /* force shutdown */
                try {
                    System.out.println(JDUtilities.runCommand("sudo", new String[] { "shutdown", "-p", "now" }, null, 0));
                } catch (Throwable e) {
                    logger.log(e);
                }
                try {
                    System.out.println(JDUtilities.runCommand("sudo", new String[] { "shutdown", "-h", "now" }, null, 0));
                } catch (Throwable e) {
                    logger.log(e);
                }
            } else {
                /* normal shutdown */
                try {
                    JDUtilities.runCommand("/usr/bin/osascript", new String[] { "-e", "tell application \"Finder\" to shut down" }, null, 0);
                } catch (Throwable e) {
                    logger.log(e);
                }
            }
            break;
        default:
            ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
                @Override
                public int getHookPriority() {
                    return Integer.MIN_VALUE;
                }

                @Override
                public void onShutdown(ShutdownRequest shutdownRequest) {
                    final File file = SecondLevelLaunch.FILE;
                    if (file != null) {
                        file.delete();
                    }
                    /* linux and others */
                    try {
                        dbusPowerState("Shutdown");
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                    try {
                        JDUtilities.runCommand("dcop", new String[] { "--all-sessions", "--all-users", "ksmserver", "ksmserver", "logout", "0", "2", "0" }, null, 0);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                    try {
                        JDUtilities.runCommand("poweroff", new String[] {}, null, 0);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                    try {
                        JDUtilities.runCommand("sudo", new String[] { "shutdown", "-P", "now" }, null, 0);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                    try {
                        // shutdown: -H and -P flags can only be used along with -h flag.
                        JDUtilities.runCommand("sudo", new String[] { "shutdown", "-Ph", "now" }, null, 0);
                    } catch (Throwable e) {
                        logger.log(e);
                    }
                }
            });
            break;
        }
        RestartController.getInstance().exitAsynch(new ForcedShutdown());
    }

    private void prepareHibernateOrStandby() {
        checkStandbyHibernateSettings(getSettings().getShutdownMode());
        logger.info("Stop all running downloads");
        DownloadWatchDog.getInstance().stopDownloads();
        LinkCollector.getInstance().abort();
        // /* reset enabled flag */
        // menuAction.setSelected(false);
    }

    private void hibernate() {
        switch (CrossSystem.OS) {
        case WINDOWS_2003:
        case WINDOWS_VISTA:
        case WINDOWS_XP:
        case WINDOWS_7:
        case WINDOWS_8:
            /* modern windows versions */
        case WINDOWS_2000:
        case WINDOWS_NT:
        case WINDOWS_10:
            /* not so modern windows versions */
            prepareHibernateOrStandby();
            try {
                JDUtilities.runCommand("powercfg.exe", new String[] { "hibernate on" }, null, 0);
            } catch (Throwable e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", new String[] { "hibernate on" }, null, 0);
                } catch (Throwable ex) {
                }
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
            } catch (Throwable e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
                } catch (Throwable ex) {
                }
            }
            break;
        case WINDOWS_OTHERS:
            /* older windows versions */
            logger.info("no hibernate support, use shutdown");
            shutdown();
            break;
        case MAC:
            /* mac os */
            prepareHibernateOrStandby();
            logger.info("no hibernate support, use shutdown");
            shutdown();
            break;
        default:
            /* linux and other */
            prepareHibernateOrStandby();
            try {
                dbusPowerState("Hibernate");
            } catch (Throwable e) {
            }
            break;
        }
    }

    public static Response execute(String[] command) throws IOException, UnsupportedEncodingException, InterruptedException {
        final ProcessBuilder probuilder = ProcessBuilderFactory.create(command);
        final Process process = probuilder.start();
        System.out.println(Arrays.toString(command));
        final Response ret = new Response();
        ret.setStd(IO.readInputStreamToString(process.getInputStream()));
        ret.setErr(IO.readInputStreamToString(process.getErrorStream()));
        ret.setExit(process.waitFor());
        return ret;
    }

    private void standby() {
        switch (CrossSystem.OS) {
        case WINDOWS_2003:
        case WINDOWS_VISTA:
        case WINDOWS_XP:
        case WINDOWS_7:
        case WINDOWS_8:
            /* modern windows versions */
        case WINDOWS_2000:
        case WINDOWS_NT:
        case WINDOWS_10:
            /* not so modern windows versions */
            prepareHibernateOrStandby();
            try {
                JDUtilities.runCommand("powercfg.exe", new String[] { "hibernate off" }, null, 0);
            } catch (Throwable e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", new String[] { "hibernate off" }, null, 0);
                } catch (Throwable ex) {
                }
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
            } catch (Throwable e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
                } catch (Throwable ex) {
                }
            }
            break;
        case WINDOWS_OTHERS:
            /* older windows versions */
            logger.info("no standby support, use shutdown");
            shutdown();
            break;
        case MAC:
            /* mac os */
            prepareHibernateOrStandby();
            try {
                JDUtilities.runCommand("/usr/bin/osascript", new String[] { "-e", "tell application \"Finder\" to sleep" }, null, 0);
            } catch (Throwable e) {
            }
            break;
        default:
            /* linux and other */
            prepareHibernateOrStandby();
            try {
                dbusPowerState("Suspend");
            } catch (Throwable e) {
            }
            break;
        }
    }

    private class ShutDown extends Thread {
        @Override
        public void run() {
            if (getSettings().isShutdownActive() == false) {
                return;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            ExtractionExtension extractor = (ExtractionExtension) ExtensionController.getInstance().getExtension(ExtractionExtension.class)._getExtension();
            if (extractor != null) {
                while (!extractor.getJobQueue().isEmpty()) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            final boolean showDialog = getSettings().isShowWarningDialog();
            switch (getSettings().getShutdownMode()) {
            case SHUTDOWN:
                /** try to shutdown */
                if (showDialog) {
                    logger.info("ask user about shutdown");
                    final String message = T.interaction_shutdown_dialog_msg_shutdown();
                    final WarningDialog d = new WarningDialog(ShutdownExtension.this, T.interaction_shutdown_dialog_title_shutdown(), message);
                    final WarningDialogInterface io = UIOManager.I().show(WarningDialogInterface.class, d);
                    switch (io.getCloseReason()) {
                    case OK:
                    case TIMEOUT:
                        shutdown();
                        return;
                    default:
                        break;
                    }
                } else {
                    logger.info("don't ask user about shutdown");
                    shutdown();
                    return;
                }
                break;
            case STANDBY:
                /* try to standby */
                if (showDialog) {
                    logger.info("ask user about standby");
                    final String message = T.interaction_shutdown_dialog_msg_standby();
                    final WarningDialog d = new WarningDialog(ShutdownExtension.this, T.interaction_shutdown_dialog_title_standby(), message);
                    final WarningDialogInterface io = UIOManager.I().show(WarningDialogInterface.class, d);
                    switch (io.getCloseReason()) {
                    case OK:
                    case TIMEOUT:
                        standby();
                        return;
                    default:
                        break;
                    }
                } else {
                    logger.info("don't ask user about standby");
                    standby();
                    return;
                }
                break;
            case HIBERNATE:
                /* try to hibernate */
                if (showDialog) {
                    logger.info("ask user about hibernate");
                    final String message = T.interaction_shutdown_dialog_msg_hibernate();
                    final WarningDialog d = new WarningDialog(ShutdownExtension.this, T.interaction_shutdown_dialog_title_hibernate(), message);
                    final WarningDialogInterface io = UIOManager.I().show(WarningDialogInterface.class, d);
                    switch (io.getCloseReason()) {
                    case OK:
                    case TIMEOUT:
                        hibernate();
                        return;
                    default:
                        break;
                    }
                } else {
                    logger.info("dont' ask user about hibernate");
                    hibernate();
                    return;
                }
                break;
            case CLOSE:
                /* try to close */
                if (showDialog) {
                    logger.info("ask user about closing");
                    final String message = T.interaction_shutdown_dialog_msg_closejd();
                    final WarningDialog d = new WarningDialog(ShutdownExtension.this, T.interaction_shutdown_dialog_title_closejd(), message);
                    final WarningDialogInterface io = UIOManager.I().show(WarningDialogInterface.class, d);
                    switch (io.getCloseReason()) {
                    case OK:
                    case TIMEOUT:
                        RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
                        return;
                    default:
                        break;
                    }
                } else {
                    logger.info("don't ask user about closing");
                    RestartController.getInstance().exitAsynch(new SmartRlyExitRequest(true));
                    return;
                }
                break;
            default:
                break;
            }
        }
    }

    private void dbusPowerState(String command) {
        JDUtilities.runCommand("dbus-send", new String[] { "--session", "--dest=org.freedesktop.PowerManagement", "--type=method_call", "--print-reply", "--reply-timeout=2000", "/org/freedesktop/PowerManagement", "org.freedesktop.PowerManagement." + command }, null, 0);
    }

    @Override
    public String getIconKey() {
        return "logout";
    }

    @Override
    protected void stop() throws StopException {
        DownloadWatchDog.getInstance().getStateMachine().removeListener(this);
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().unregisterExtender(this);
            MenuManagerMainmenu.getInstance().unregisterExtender(this);
        }
    }

    @Override
    protected void start() throws StartException {
        if (!Application.isHeadless()) {
            MenuManagerMainToolbar.getInstance().registerExtender(this);
            MenuManagerMainmenu.getInstance().registerExtender(this);
        }
        if (!getSettings().isShutdownActiveByDefaultEnabled()) {
            CFG_SHUTDOWN.SHUTDOWN_ACTIVE.setValue(false);
        } else {
            CFG_SHUTDOWN.SHUTDOWN_ACTIVE.setValue(true);
        }
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        logger.info("Shutdown OK");
    }

    @Override
    public String getDescription() {
        return T.jd_plugins_optional_jdshutdown_description();
    }

    @Override
    public AddonPanel<ShutdownExtension> getGUI() {
        return null;
    }

    @Override
    protected void initExtension() throws StartException {
        if (!Application.isHeadless()) {
            configPanel = new ShutdownConfigPanel(this);
        }
    }

    public void onStateChange(StateEvent event) {
        if (event.getNewState() == DownloadWatchDog.STOPPED_STATE) {
            final boolean active = getSettings().isShutdownActive();
            if (!active) {
                logger.info("ShutdownExtension is not active!");
            } else {
                logger.info("ShutdownExtension is active!");
                if (DownloadWatchDog.getInstance().getSession().getDownloadsStarted() > 0) {
                    final ShutdownRequest request = ShutdownController.getInstance().collectVetos(new BasicShutdownRequest(true));
                    if (request.hasVetos()) {
                        logger.info("Vetos: " + request.getVetos().size() + " Wait until there is no veto");
                        for (ShutdownVetoException e : request.getVetos()) {
                            logger.log(e);
                            logger.info(e.getSource() + "");
                        }
                        new Thread("Wait to Shutdown") {
                            public void run() {
                                while (true) {
                                    final StateMachine sm = DownloadWatchDog.getInstance().getStateMachine();
                                    if (sm.isState(DownloadWatchDog.PAUSE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE)) {
                                        logger.info("Cancel Shutdown.");
                                        return;
                                    }
                                    final ShutdownRequest request = ShutdownController.getInstance().collectVetos(new BasicShutdownRequest(true));
                                    if (!request.hasVetos()) {
                                        logger.info("No Vetos");
                                        if (sm.isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.STOPPED_STATE)) {
                                            doShutdown();
                                            return;
                                        }
                                    }
                                    logger.info("Vetos: " + request.getVetos().size() + " Wait until there is no veto");
                                    for (ShutdownVetoException e : request.getVetos()) {
                                        logger.log(e);
                                        logger.info(e.getSource() + "");
                                    }
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        return;
                                    }
                                }
                            }
                        }.start();
                    } else {
                        doShutdown();
                    }
                } else {
                    logger.info("No downloads have been started in last session! ByPass ShutdownExtension!");
                }
            }
        }
    }

    protected void doShutdown() {
        if (shutdown == null || !shutdown.isAlive()) {
            shutdown = new ShutDown();
            shutdown.start();
        }
    }

    public void onStateUpdate(StateEvent event) {
    }

    public static void checkStandbyHibernateSettings(Mode newValue) {
        try {
            switch (newValue) {
            case HIBERNATE:
                if (isHibernateActivated()) {
                    return;
                }
                Dialog.getInstance().showMessageDialog(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, org.jdownloader.extensions.shutdown.translate.T.T.show_admin());
                String path = CrossSystem.is64BitOperatingSystem() ? Application.getResource("tools\\Windows\\elevate\\Elevate64.exe").getAbsolutePath() : Application.getResource("tools\\Windows\\elevate\\Elevate32.exe").getAbsolutePath();
                try {
                    LogController.CL().info(ShutdownExtension.execute(new String[] { path, "powercfg", "-hibernate", "on" }).toString());
                } catch (Throwable e) {
                    LogController.CL().log(e);
                }
                break;
            case STANDBY:
                if (!isHibernateActivated()) {
                    return;
                }
                Dialog.getInstance().showMessageDialog(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, org.jdownloader.extensions.shutdown.translate.T.T.show_admin());
                path = CrossSystem.is64BitOperatingSystem() ? Application.getResource("tools\\Windows\\elevate\\Elevate64.exe").getAbsolutePath() : Application.getResource("tools\\Windows\\elevate\\Elevate32.exe").getAbsolutePath();
                try {
                    LogController.CL().info(ShutdownExtension.execute(new String[] { path, "powercfg", "-hibernate", "off" }).toString());

                } catch (Throwable e) {
                    LogController.CL().log(e);
                }
                break;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isQuickToggleEnabled() {
        return false;
    }

    private static boolean isHibernateActivated() throws UnsupportedEncodingException, IOException, InterruptedException {
        final Response status = ShutdownExtension.execute(new String[] { "powercfg", "-a" });
        LogController.CL().info(status.toString());
        // we should add the return for other languages
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
    public MenuItemData updateMenuModel(ContextMenuManager manager, MenuContainerRoot mr) {
        if (manager instanceof MenuManagerMainmenu) {
            ExtensionsMenuContainer container = new ExtensionsMenuContainer();
            container.add(org.jdownloader.extensions.shutdown.actions.ShutdownToggleAction.class);
            return container;
        } else if (manager instanceof MenuManagerMainToolbar) {
            // try to search a toggle action and queue it after it.
            for (int i = mr.getItems().size() - 1; i >= 0; i--) {
                MenuItemData mid = mr.getItems().get(i);
                if (mid.getActionData() == null || !mid.getActionData()._isValidDataForCreatingAnAction() || mid instanceof MenuLink) {
                    continue;
                }
                boolean val = mid._isValidated();
                try {
                    mid._setValidated(true);
                    if (mid.createAction().isToggle()) {

                        mr.getItems().add(i + 1, new MenuItemData(new ActionData(ShutdownToggleAction.class)));
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    mid._setValidated(val);
                }
            }
            // no toggle action found. append action at the end.
            mr.add(ShutdownToggleAction.class);
        }
        return null;
    }
}