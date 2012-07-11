//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenuItem;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.config.Property;
import jd.controlling.downloadcontroller.DownloadWatchDog;
import jd.gui.UserIO;
import jd.nutils.Executer;
import jd.nutils.JDFlags;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;

import org.appwork.controlling.StateEvent;
import org.appwork.controlling.StateEventListener;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.actions.AppAction;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.shutdown.translate.ShutdownTranslation;
import org.jdownloader.logging.LogController;

public class ShutdownExtension extends AbstractExtension<ShutdownConfig, ShutdownTranslation> implements StateEventListener {

    private final int                               count                 = 60;
    private static final String                     CONFIG_ENABLEDONSTART = "ENABLEDONSTART";
    private static final String                     CONFIG_MODE           = "CONFIG_MODE";
    private static final String                     CONFIG_FORCESHUTDOWN  = "FORCE";
    private Thread                                  shutdown              = null;
    private boolean                                 shutdownEnabled;
    private AppAction                               menuAction            = null;
    private String[]                                MODES_AVAIL           = null;
    private ExtensionConfigPanel<ShutdownExtension> configPanel;

    public ExtensionConfigPanel<ShutdownExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public ShutdownExtension() throws StartException {
        setTitle(_.jd_plugins_optional_jdshutdown());
        MODES_AVAIL = new String[] { _.gui_config_jdshutdown_shutdown(), _.gui_config_jdshutdown_standby(), _.gui_config_jdshutdown_hibernate(), _.gui_config_jdshutdown_close() };
        shutdownEnabled = getPropertyConfig().getBooleanProperty(CONFIG_ENABLEDONSTART, false);
    }

    private void closejd() {
        LogController.CL().info("close jd");
        org.jdownloader.controlling.JDRestartController.getInstance().exit(true);
    }

    private void shutdown() {
        LogController.CL().info("shutdown");
        int id = 0;
        switch (id = CrossSystem.getID()) {
        case CrossSystem.OS_WINDOWS_2003:
        case CrossSystem.OS_WINDOWS_VISTA:
        case CrossSystem.OS_WINDOWS_XP:
        case CrossSystem.OS_WINDOWS_7:
            /* modern windows versions */
        case CrossSystem.OS_WINDOWS_2000:
        case CrossSystem.OS_WINDOWS_NT:
        case CrossSystem.OS_WINDOWS_SERVER_2008:
            /* not so modern windows versions */
            if (getPropertyConfig().getBooleanProperty(CONFIG_FORCESHUTDOWN, false)) {
                /* force shutdown */
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
            } else {
                /* normal shutdown */
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
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
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "Shell32,SHExitWindowsEx", "1" }, null, 0);
            } catch (Exception e) {
            }
            break;
        case CrossSystem.OS_MAC_OTHER:
            /* mac os */
            if (getPropertyConfig().getBooleanProperty(CONFIG_FORCESHUTDOWN, false)) {
                /* force shutdown */
                try {
                    JDUtilities.runCommand("sudo", new String[] { "shutdown", "-p", "now" }, null, 0);
                } catch (Exception e) {
                }
            } else {
                /* normal shutdown */
                try {
                    JDUtilities.runCommand("/usr/bin/osascript", new String[] { "-e", "tell application \"Finder\" to shut down" }, null, 0);
                } catch (Exception e) {
                }
            }
            break;
        default:
            /* linux and others */
            try {
                dbusPowerState("Shutdown");
            } catch (Exception e) {
            }
            try {
                JDUtilities.runCommand("dcop", new String[] { "--all-sessions", "--all-users", "ksmserver", "ksmserver", "logout", "0", "2", "0" }, null, 0);
            } catch (Exception e) {
            }
            try {
                JDUtilities.runCommand("poweroff", new String[] {}, null, 0);
            } catch (Exception e) {
            }
            try {
                JDUtilities.runCommand("sudo", new String[] { "shutdown", "-p", "now" }, null, 0);
            } catch (Exception e) {
            }
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
        }
        System.exit(0);
    }

    private void prepareHibernateOrStandby() {
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

    private void standby() {
        switch (CrossSystem.getID()) {
        case CrossSystem.OS_WINDOWS_2003:
        case CrossSystem.OS_WINDOWS_VISTA:
        case CrossSystem.OS_WINDOWS_XP:
        case CrossSystem.OS_WINDOWS_7:
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
            if (shutdownEnabled == false) return;
            /* check for running jdunrar and wait */
            // OptionalPluginWrapper addon =
            // JDUtilities.getOptionalPlugin("unrar");
            // if (addon != null && addon.isEnabled()) {
            // while (true) {
            // Object obj = addon.getPlugin().interact("isWorking", null);
            // if (obj == null || (obj instanceof Boolean &&
            // obj.equals(false))) break;
            // logger.info("JD-Unrar is working - wait before shutting down");
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            // JDLogger.exception(e);
            // }
            // }
            // }

            // addon = JDUtilities.getOptionalPlugin("extraction");
            // if (addon != null && addon.isEnabled()) {
            // while (true) {
            // Object obj = addon.getPlugin().interact("isWorking", null);
            // if (obj == null || (obj instanceof Boolean &&
            // obj.equals(false))) break;
            // logger.info("JD-Unrar is working - wait before shutting down");
            // try {
            // Thread.sleep(1000);
            // } catch (InterruptedException e) {
            // JDLogger.exception(e);
            // }
            // }
            // }

            int ret = getPropertyConfig().getIntegerProperty(CONFIG_MODE, 0);
            String message;
            int ret2;
            switch (ret) {
            case 0:
                /* try to shutdown */
                LogController.CL().info("ask user about shutdown");
                message = _.interaction_shutdown_dialog_msg_shutdown();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, _.interaction_shutdown_dialog_title_shutdown(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                LogController.CL().info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                    shutdown();
                }
                break;
            case 1:
                /* try to standby */
                LogController.CL().info("ask user about standby");
                message = _.interaction_shutdown_dialog_msg_standby();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, _.interaction_shutdown_dialog_title_standby(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                LogController.CL().info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                    standby();
                }
                break;
            case 2:
                /* try to hibernate */
                LogController.CL().info("ask user about hibernate");
                message = _.interaction_shutdown_dialog_msg_hibernate();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, _.interaction_shutdown_dialog_title_hibernate(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                LogController.CL().info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                    hibernate();
                }
                break;
            case 3:
                /* try to close */
                LogController.CL().info("ask user about closing");
                message = _.interaction_shutdown_dialog_msg_closejd();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, _.interaction_shutdown_dialog_title_closejd(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                LogController.CL().info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
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
    }

    @Override
    protected void start() throws StartException {
        if (menuAction == null) menuAction = new AppAction() {
            {
                setName("Shutdown");
                setIconKey("logout");

                this.setEnabled(true);
                setSelected(false);

                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            shutdownEnabled = isSelected();
                        }
                    }
                });
            }

            public void actionPerformed(ActionEvent e) {
                if (shutdownEnabled) {
                    UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN, _.addons_jdshutdown_statusmessage_enabled());
                } else {
                    UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN, _.addons_jdshutdown_statusmessage_disabled());
                }
            }

        };
        menuAction.setSelected(shutdownEnabled);
        DownloadWatchDog.getInstance().getStateMachine().addListener(this);
        LogController.CL().info("Shutdown OK");
    }

    protected void initSettings(ConfigContainer config) {
        Property subConfig = getPropertyConfig();

        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_ENABLEDONSTART, _.gui_config_jdshutdown_enabledOnStart()).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, CONFIG_MODE, MODES_AVAIL, _.gui_config_jdshutdown_mode()).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_FORCESHUTDOWN, _.gui_config_jdshutdown_forceshutdown()).setDefaultValue(false));

        /* enable force shutdown for Mac OSX */
        if (CrossSystem.isMac()) {
            config.addEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Executer exec = new Executer("/usr/bin/osascript");
                    File tmp = Application.getResource("tmp/osxnopasswordforshutdown.scpt");
                    tmp.delete();
                    try {
                        IO.writeToFile(tmp, IO.readURL(Application.getRessourceURL(Application.getPackagePath(ShutdownExtension.class) + "osxnopasswordforshutdown.scpt")));

                        exec.addParameter(tmp.getAbsolutePath());
                        exec.setWaitTimeout(0);
                        exec.start();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    } finally {
                        tmp.delete();
                        tmp.deleteOnExit();
                    }
                }

            }, _.gui_config_jdshutdown_osx_force_short(), _.gui_config_jdshutdown_osx_force_long(), null));
        }
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
    public java.util.ArrayList<JMenuItem> getMenuAction() {
        ArrayList<JMenuItem> menu = new ArrayList<JMenuItem>();
        menu.add(new JCheckBoxMenuItem(menuAction));
        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }

    public void onStateChange(StateEvent event) {
        if (shutdownEnabled == false) return;
        if (DownloadWatchDog.IDLE_STATE == event.getNewState() || DownloadWatchDog.STOPPED_STATE == event.getNewState()) {
            if (DownloadWatchDog.getInstance().getDownloadssincelastStart() > 0) {
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
        }
    }

    public void onStateUpdate(StateEvent event) {
    }
}