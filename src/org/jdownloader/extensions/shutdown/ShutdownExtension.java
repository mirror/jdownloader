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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.swing.jdgui.components.toolbar.ToolbarManager;
import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarAction;
import jd.gui.swing.jdgui.components.toolbar.actions.AbstractToolbarToggleAction;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;

import org.appwork.controlling.State;
import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.controlling.StateMachine;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownVetoException;
import org.appwork.uio.MessageDialogImpl;
import org.appwork.uio.MessageDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.processes.ProcessBuilderFactory;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.extraction.ExtractionExtension;
import org.jdownloader.extensions.shutdown.translate.ShutdownTranslation;
import org.jdownloader.extensions.shutdown.translate.T;
import org.jdownloader.gui.shortcuts.ShortcutController;
import org.jdownloader.logging.LogController;
import org.jdownloader.updatev2.RestartController;

public class ShutdownExtension extends AbstractExtension<ShutdownConfig, ShutdownTranslation> implements StateEventListener {

    private Thread                shutdown   = null;

    private AbstractToolbarAction menuAction = null;

    private ShutdownConfigPanel   configPanel;

    public ShutdownConfigPanel getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public ShutdownExtension() throws StartException {
        setTitle(_.jd_plugins_optional_jdshutdown());

    }

    private void closejd() {
        LogController.CL().info("close jd");
        RestartController.getInstance().exitAsynch();
    }

    private void shutdown() {
        LogController.CL().info("shutdown");
        int id = 0;
        switch (id = CrossSystem.getID()) {
        case CrossSystem.OS_WINDOWS_2003:
        case CrossSystem.OS_WINDOWS_VISTA:
        case CrossSystem.OS_WINDOWS_XP:
        case CrossSystem.OS_WINDOWS_7:
        case CrossSystem.OS_WINDOWS_8:
            /* modern windows versions */
        case CrossSystem.OS_WINDOWS_2000:
        case CrossSystem.OS_WINDOWS_NT:
        case CrossSystem.OS_WINDOWS_SERVER_2008:
            /* not so modern windows versions */
            if (getSettings().isForceShutdownEnabled()) {
                /* force shutdown */
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Exception e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Exception e) {
                    logger.log(e);
                }
            } else {
                /* normal shutdown */
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                    logger.log(e);
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                    logger.log(e);
                }
            }
            if (id == CrossSystem.OS_WINDOWS_2000 || id == CrossSystem.OS_WINDOWS_NT) {
                /* also try extra methods for windows2000 and nt */
                try {

                    File f = JDUtilities.getResourceFile("tmp/shutdown.vbs");
                    f.deleteOnExit();
                    IO.writeStringToFile(f, "set WshShell = CreateObject(\"WScript.Shell\")\r\nWshShell.SendKeys \"^{ESC}^{ESC}^{ESC}{UP}{ENTER}{ENTER}\"\r\n");

                    try {
                        JDUtilities.runCommand("cmd", new String[] { "/c", "start", "/min", "cscript", f.getAbsolutePath() }, null, 0);

                    } finally {
                        f.delete();
                    }
                } catch (Exception e) {
                }
            }
            break;
        case CrossSystem.OS_WINDOWS_OTHER:
            /* older windows versions */
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "user,ExitWindows" }, null, 0);
            } catch (Exception e) {
                logger.log(e);
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "Shell32,SHExitWindowsEx", "1" }, null, 0);
            } catch (Exception e) {
                logger.log(e);
            }
            break;
        case CrossSystem.OS_MAC_OTHER:
            /* mac os */
            if (getSettings().isForceShutdownEnabled()) {
                /* force shutdown */
                try {
                    System.out.println(JDUtilities.runCommand("sudo", new String[] { "shutdown", "-p", "now" }, null, 0));
                } catch (Exception e) {

                    logger.log(e);

                }
                try {
                    System.out.println(JDUtilities.runCommand("sudo", new String[] { "shutdown", "-h", "now" }, null, 0));
                } catch (Exception e) {

                    logger.log(e);

                }
            } else {
                /* normal shutdown */
                try {
                    JDUtilities.runCommand("/usr/bin/osascript", new String[] { "-e", "tell application \"Finder\" to shut down" }, null, 0);
                } catch (Exception e) {
                    logger.log(e);
                }
            }
            break;
        default:
            /* linux and others */
            try {
                dbusPowerState("Shutdown");
            } catch (Exception e) {
                logger.log(e);
            }
            try {
                JDUtilities.runCommand("dcop", new String[] { "--all-sessions", "--all-users", "ksmserver", "ksmserver", "logout", "0", "2", "0" }, null, 0);
            } catch (Exception e) {
                logger.log(e);
            }
            try {
                JDUtilities.runCommand("poweroff", new String[] {}, null, 0);
            } catch (Exception e) {
                logger.log(e);
            }
            try {
                JDUtilities.runCommand("sudo", new String[] { "shutdown", "-P", "now" }, null, 0);
            } catch (Exception e) {
                logger.log(e);
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    private void prepareHibernateOrStandby() {

        checkStandbyHibernateSettings(getSettings().getShutdownMode());
        LogController.CL().info("Stop all running downloads");
        DownloadWatchDog.getInstance().stopDownloads();
        /* reset enabled flag */
        menuAction.setSelected(false);
    }

    private void hibernate() {
        switch (CrossSystem.getID()) {
        case CrossSystem.OS_WINDOWS_2003:
        case CrossSystem.OS_WINDOWS_VISTA:
        case CrossSystem.OS_WINDOWS_XP:
        case CrossSystem.OS_WINDOWS_7:
        case CrossSystem.OS_WINDOWS_8:
            /* modern windows versions */
        case CrossSystem.OS_WINDOWS_2000:
        case CrossSystem.OS_WINDOWS_NT:
            /* not so modern windows versions */
            prepareHibernateOrStandby();
            try {
                JDUtilities.runCommand("powercfg.exe", new String[] { "hibernate on" }, null, 0);
            } catch (Exception e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", new String[] { "hibernate on" }, null, 0);
                } catch (Exception ex) {
                }
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
            } catch (Exception e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
                } catch (Exception ex) {
                }
            }
            break;
        case CrossSystem.OS_WINDOWS_OTHER:
            /* older windows versions */
            LogController.CL().info("no hibernate support, use shutdown");
            shutdown();
            break;
        case CrossSystem.OS_MAC_OTHER:
            /* mac os */
            prepareHibernateOrStandby();
            LogController.CL().info("no hibernate support, use shutdown");
            shutdown();
            break;
        default:
            /* linux and other */
            prepareHibernateOrStandby();
            try {
                dbusPowerState("Hibernate");
            } catch (Exception e) {
            }
            break;
        }
    }

    public static Response execute(String[] command) throws IOException, UnsupportedEncodingException, InterruptedException {
        ProcessBuilder probuilder = ProcessBuilderFactory.create(command);

        Process process = probuilder.start();
        System.out.println(Arrays.toString(command));
        Response ret = new Response();
        ret.setStd(IO.readInputStreamToString(process.getInputStream()));
        ret.setErr(IO.readInputStreamToString(process.getErrorStream()));
        ret.setExit(process.waitFor());
        return ret;
    }

    private void standby() {
        switch (CrossSystem.getID()) {
        case CrossSystem.OS_WINDOWS_2003:
        case CrossSystem.OS_WINDOWS_VISTA:
        case CrossSystem.OS_WINDOWS_XP:
        case CrossSystem.OS_WINDOWS_7:
        case CrossSystem.OS_WINDOWS_8:
            /* modern windows versions */
        case CrossSystem.OS_WINDOWS_2000:
        case CrossSystem.OS_WINDOWS_NT:
            /* not so modern windows versions */
            prepareHibernateOrStandby();
            try {
                JDUtilities.runCommand("powercfg.exe", new String[] { "hibernate off" }, null, 0);
            } catch (Exception e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", new String[] { "hibernate off" }, null, 0);
                } catch (Exception ex) {
                }
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
            } catch (Exception e) {
                try {
                    JDUtilities.runCommand("%windir%\\system32\\RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
                } catch (Exception ex) {
                }
            }
            break;
        case CrossSystem.OS_WINDOWS_OTHER:
            /* older windows versions */
            LogController.CL().info("no standby support, use shutdown");
            shutdown();
            break;
        case CrossSystem.OS_MAC_OTHER:
            /* mac os */
            prepareHibernateOrStandby();
            try {
                JDUtilities.runCommand("/usr/bin/osascript", new String[] { "-e", "tell application \"Finder\" to sleep" }, null, 0);
            } catch (Exception e) {
            }
            break;
        default:
            /* linux and other */
            prepareHibernateOrStandby();
            try {
                dbusPowerState("Suspend");
            } catch (Exception e) {
            }
            break;
        }
    }

    private class ShutDown extends Thread {
        @Override
        public void run() {
            if (getSettings().isShutdownActive() == false) return;
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
            String message;
            int ret2;
            switch (getSettings().getShutdownMode()) {
            case SHUTDOWN:
                /* try to shutdown */
                LogController.CL().info("ask user about shutdown");
                message = _.interaction_shutdown_dialog_msg_shutdown();

                WarningDialog d = new WarningDialog(ShutdownExtension.this, _.interaction_shutdown_dialog_title_shutdown(), message);

                UIOManager.I().show(WarningDialogInterface.class, d);

                if ((d.getReturnmask() & Dialog.RETURN_OK) == Dialog.RETURN_OK || (d.getReturnmask() & Dialog.RETURN_TIMEOUT) == Dialog.RETURN_TIMEOUT) {
                    shutdown();
                }
                break;
            case STANDBY:
                /* try to standby */
                LogController.CL().info("ask user about standby");

                message = _.interaction_shutdown_dialog_msg_standby();
                d = new WarningDialog(ShutdownExtension.this, _.interaction_shutdown_dialog_title_standby(), message);
                try {
                    UIOManager.I().show(WarningDialogInterface.class, d);
                } catch (Throwable e) {

                }
                if ((d.getReturnmask() & Dialog.RETURN_OK) == Dialog.RETURN_OK || (d.getReturnmask() & Dialog.RETURN_TIMEOUT) == Dialog.RETURN_TIMEOUT) {
                    standby();
                }
                break;
            case HIBERNATE:
                /* try to hibernate */
                LogController.CL().info("ask user about hibernate");

                message = _.interaction_shutdown_dialog_msg_hibernate();
                d = new WarningDialog(ShutdownExtension.this, _.interaction_shutdown_dialog_title_hibernate(), message);
                try {
                    UIOManager.I().show(WarningDialogInterface.class, d);
                } catch (Throwable e) {

                }
                if ((d.getReturnmask() & Dialog.RETURN_OK) == Dialog.RETURN_OK || (d.getReturnmask() & Dialog.RETURN_TIMEOUT) == Dialog.RETURN_TIMEOUT) {
                    hibernate();
                }
                break;
            case CLOSE:
                /* try to close */
                LogController.CL().info("ask user about closing");
                message = _.interaction_shutdown_dialog_msg_closejd();

                d = new WarningDialog(ShutdownExtension.this, _.interaction_shutdown_dialog_title_closejd(), message);
                try {
                    UIOManager.I().show(WarningDialogInterface.class, d);
                } catch (Throwable e) {

                }
                if ((d.getReturnmask() & Dialog.RETURN_OK) == Dialog.RETURN_OK || (d.getReturnmask() & Dialog.RETURN_TIMEOUT) == Dialog.RETURN_TIMEOUT) {
                    closejd();
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

    // @Override
    // public Object interact(String command, Object parameter) {
    // if (command == null) return null;
    // if (command.equals("shutdown")) this.shutdown();
    // if (command.equals("hibernate")) this.hibernate();
    // if (command.equals("standby")) this.standby();
    // return null;
    // }

    @Override
    protected void stop() throws StopException {
        DownloadWatchDog.getInstance().getStateMachine().removeListener(this);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (menuAction != null) {

                    ToolbarManager.getInstance().remove(menuAction);
                }
            }
        };

    }

    @Override
    protected void start() throws StartException {

        if (!getSettings().isShutdownActiveByDefaultEnabled()) {
            CFG_SHUTDOWN.SHUTDOWN_ACTIVE.setValue(false);
        }
        if (menuAction == null) {
            menuAction = new AbstractToolbarToggleAction(CFG_SHUTDOWN.SHUTDOWN_ACTIVE) {

                {
                    setName(_.lit_shutdownn());

                    this.setEnabled(true);
                    setSelected(false);

                }

                public void actionPerformed(ActionEvent e) {
                    super.actionPerformed(e);

                    if (isSelected()) {

                        UIOManager.I().show(MessageDialogInterface.class, new MessageDialogImpl(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _.addons_jdshutdown_statusmessage_enabled()));

                    } else {
                        UIOManager.I().show(MessageDialogInterface.class, new MessageDialogImpl(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _.addons_jdshutdown_statusmessage_disabled()));

                    }

                }

                @Override
                public String createIconKey() {
                    return "logout";
                }

                @Override
                protected String createAccelerator() {
                    return ShortcutController._.getShutdownExtensionToggleShutdownAction();
                }

                @Override
                protected String createTooltip() {
                    return _.action_tooltip();
                }

            };
        }

        if (getSettings().isToolbarButtonEnabled()) {
            ToolbarManager.getInstance().add(menuAction);
        }
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        LogController.CL().info("Shutdown OK");
    }

    @Override
    public String getDescription() {
        return _.jd_plugins_optional_jdshutdown_description();
    }

    @Override
    public AddonPanel<ShutdownExtension> getGUI() {
        return null;
    }

    @Override
    public List<JMenuItem> getMenuAction() {
        java.util.List<JMenuItem> menu = new ArrayList<JMenuItem>();
        menu.add(new JCheckBoxMenuItem(menuAction));
        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
        // ConfigContainer cc = new ConfigContainer(getName());
        // initSettings(cc);
        configPanel = new ShutdownConfigPanel(this);
    }

    public void onStateChange(StateEvent event) {
        final StateMachine sm = DownloadWatchDog.getInstance().getStateMachine();
        if (!getSettings().isShutdownActive()) return;
        State state = sm.getState();

        if (event.getNewState() == DownloadWatchDog.STOPPED_STATE) {
            if (DownloadWatchDog.getInstance().getDownloadssincelastStart() > 0) {
                List<ShutdownVetoException> vetos = ShutdownController.getInstance().collectVetos(true);
                if (vetos.size() > 0) {
                    logger.info("Vetos: " + vetos + " Wait until there is no veto");
                    new Thread("Wait to Shutdown") {
                        public void run() {

                            while (true) {

                                if (sm.isState(DownloadWatchDog.PAUSE_STATE, DownloadWatchDog.RUNNING_STATE, DownloadWatchDog.STOPPING_STATE)) {
                                    logger.info("Cancel Shutdown.");
                                    return;
                                }
                                List<ShutdownVetoException> vetos = ShutdownController.getInstance().collectVetos(true);

                                if (vetos.size() == 0) {
                                    logger.info("No Vetos");
                                    if (sm.isState(DownloadWatchDog.IDLE_STATE, DownloadWatchDog.STOPPED_STATE)) {

                                        doShutdown();
                                        return;
                                    }
                                }
                                logger.info("Vetos: " + vetos + " Wait until there is no veto");
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
            }

        }
    }

    protected void doShutdown() {
        if (shutdown != null) {
            if (!shutdown.isAlive()) {
                shutdown = new ShutDown();
                shutdown.start();
            }
        } else {
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

                if (isHibernateActivated()) return;

                Dialog.getInstance().showMessageDialog(T._.show_admin());
                String path = CrossSystem.is64BitOperatingSystem() ? Application.getResource("tools\\Windows\\elevate\\Elevate64.exe").getAbsolutePath() : Application.getResource("tools\\Windows\\elevate\\Elevate32.exe").getAbsolutePath();
                try {
                    LogController.CL().info(ShutdownExtension.execute(new String[] { path, "powercfg", "-hibernate", "on" }).toString());

                } catch (Throwable e) {
                    LogController.CL().log(e);
                }
                break;
            case STANDBY:
                if (!isHibernateActivated()) return;

                Dialog.getInstance().showMessageDialog(T._.show_admin());
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

    private static boolean isHibernateActivated() throws UnsupportedEncodingException, IOException, InterruptedException {
        Response status = ShutdownExtension.execute(new String[] { "powercfg", "-a" });
        LogController.CL().info(status.toString());
        // we should add the return for other languages
        if (status.getStd() != null) {
            if (status.getStd().contains("Ruhezustand wurde nicht aktiviert")) return false;
            if (status.getStd().contains("Hibernation has not been enabled")) return false;
            if (status.getStd().contains("Hibernation")) return false;
        }

        return true;
    }
}