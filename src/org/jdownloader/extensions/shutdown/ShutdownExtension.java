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

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.ConfigGroup;
import jd.controlling.DownloadWatchDog;
import jd.controlling.JDController;
import jd.controlling.JSonWrapper;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.Executer;
import jd.nutils.JDFlags;
import jd.nutils.OSDetector;
import jd.plugins.AddonPanel;
import jd.utils.JDUtilities;

import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;
import org.jdownloader.extensions.shutdown.translate.T;

public class ShutdownExtension extends AbstractExtension<ShutdownConfig> implements ControlListener {

    private static final int                        count                 = 60;
    private static final String                     CONFIG_ENABLEDONSTART = "ENABLEDONSTART";
    private static final String                     CONFIG_MODE           = "CONFIG_MODE";
    private static final String                     CONFIG_FORCESHUTDOWN  = "FORCE";
    private static Thread                           shutdown              = null;
    private static boolean                          shutdownEnabled;
    private static MenuAction                       menuAction            = null;
    private static String[]                         MODES_AVAIL           = null;
    private ExtensionConfigPanel<ShutdownExtension> configPanel;

    public ExtensionConfigPanel<ShutdownExtension> getConfigPanel() {
        return configPanel;
    }

    public boolean hasConfigPanel() {
        return true;
    }

    public ShutdownExtension() throws StartException {
        super(T._.jd_plugins_optional_jdshutdown());
        MODES_AVAIL = new String[] { T._.gui_config_jdshutdown_shutdown(), T._.gui_config_jdshutdown_standby(), T._.gui_config_jdshutdown_hibernate(), T._.gui_config_jdshutdown_close() };
        shutdownEnabled = getPluginConfig().getBooleanProperty(CONFIG_ENABLEDONSTART, false);
    }

    public void controlEvent(ControlEvent event) {
        if (shutdownEnabled) {
            if (event.getEventID() == ControlEvent.CONTROL_DOWNLOAD_STOP && DownloadWatchDog.getInstance().getDownloadssincelastStart() > 0) {
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

    private void closejd() {
        logger.info("close jd");
        JDUtilities.getController().exit();
    }

    private void shutdown() {
        logger.info("shutdown");
        JDController.getInstance().prepareShutdown(false);
        int id = 0;
        switch (id = OSDetector.getID()) {
        case OSDetector.OS_WINDOWS_2003:
        case OSDetector.OS_WINDOWS_VISTA:
        case OSDetector.OS_WINDOWS_XP:
        case OSDetector.OS_WINDOWS_7:
            /* modern windows versions */
        case OSDetector.OS_WINDOWS_2000:
        case OSDetector.OS_WINDOWS_NT:
        case OSDetector.OS_WINDOWS_SERVER_2008:
            /* not so modern windows versions */
            if (getPluginConfig().getBooleanProperty(CONFIG_FORCESHUTDOWN, false)) {
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
            if (id == OSDetector.OS_WINDOWS_2000 || id == OSDetector.OS_WINDOWS_NT) {
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
        case OSDetector.OS_WINDOWS_OTHER:
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
        case OSDetector.OS_MAC_OTHER:
            /* mac os */
            if (getPluginConfig().getBooleanProperty(CONFIG_FORCESHUTDOWN, false)) {
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
        logger.info("Stop all running downloads");
        DownloadWatchDog.getInstance().stopDownloads();
        JDController.getInstance().syncDatabase();
        /* reset enabled flag */
        menuAction.setSelected(false);
    }

    private void hibernate() {
        switch (OSDetector.getID()) {
        case OSDetector.OS_WINDOWS_2003:
        case OSDetector.OS_WINDOWS_VISTA:
        case OSDetector.OS_WINDOWS_XP:
        case OSDetector.OS_WINDOWS_7:
            /* modern windows versions */
        case OSDetector.OS_WINDOWS_2000:
        case OSDetector.OS_WINDOWS_NT:
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
        case OSDetector.OS_WINDOWS_OTHER:
            /* older windows versions */
            logger.info("no hibernate support, use shutdown");
            shutdown();
            break;
        case OSDetector.OS_MAC_OTHER:
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
            } catch (Exception e) {
            }
            break;
        }
    }

    private void standby() {
        switch (OSDetector.getID()) {
        case OSDetector.OS_WINDOWS_2003:
        case OSDetector.OS_WINDOWS_VISTA:
        case OSDetector.OS_WINDOWS_XP:
        case OSDetector.OS_WINDOWS_7:
            /* modern windows versions */
        case OSDetector.OS_WINDOWS_2000:
        case OSDetector.OS_WINDOWS_NT:
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
        case OSDetector.OS_WINDOWS_OTHER:
            /* older windows versions */
            logger.info("no standby support, use shutdown");
            shutdown();
            break;
        case OSDetector.OS_MAC_OTHER:
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

            int ret = getPluginConfig().getIntegerProperty(CONFIG_MODE, 0);
            String message;
            int ret2;
            switch (ret) {
            case 0:
                /* try to shutdown */
                logger.info("ask user about shutdown");
                message = T._.interaction_shutdown_dialog_msg_shutdown();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, T._.interaction_shutdown_dialog_title_shutdown(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                logger.info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                    shutdown();
                }
                break;
            case 1:
                /* try to standby */
                logger.info("ask user about standby");
                message = T._.interaction_shutdown_dialog_msg_standby();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, T._.interaction_shutdown_dialog_title_standby(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                logger.info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                    standby();
                }
                break;
            case 2:
                /* try to hibernate */
                logger.info("ask user about hibernate");
                message = T._.interaction_shutdown_dialog_msg_hibernate();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, T._.interaction_shutdown_dialog_title_hibernate(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                logger.info("Return code: " + ret2);
                if (JDFlags.hasSomeFlags(ret2, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                    hibernate();
                }
                break;
            case 3:
                /* try to close */
                logger.info("ask user about closing");
                message = T._.interaction_shutdown_dialog_msg_closejd();
                UserIO.setCountdownTime(count);
                ret2 = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, T._.interaction_shutdown_dialog_title_closejd(), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
                UserIO.setCountdownTime(-1);
                logger.info("Return code: " + ret2);
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
        JDController.getInstance().removeControlListener(this);
    }

    @Override
    protected void start() throws StartException {
        if (menuAction == null) menuAction = new MenuAction("Shutdown", "gui.jdshutdown.toggle", "logout") {
            private static final long serialVersionUID = 4359802245569811800L;

            @Override
            public void initDefaults() {
                this.setEnabled(true);
                setType(ToolBarAction.Types.TOGGLE);
                this.setIcon("logout");
                this.setActionListener(new ActionListener() {
                    public void actionPerformed(ActionEvent e) {
                        if (shutdownEnabled) {
                            UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN, T._.addons_jdshutdown_statusmessage_enabled());
                        } else {
                            UserIO.getInstance().requestMessageDialog(UserIO.DONT_SHOW_AGAIN, T._.addons_jdshutdown_statusmessage_disabled());
                        }
                    }
                });
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            shutdownEnabled = isSelected();
                        }
                    }
                });
            }

            @Override
            protected String createMnemonic() {
                return null;
            }

            @Override
            protected String createAccelerator() {
                return null;
            }

            @Override
            protected String createTooltip() {
                return null;
            }
        };
        menuAction.setSelected(shutdownEnabled);
        JDController.getInstance().addControlListener(this);
        logger.info("Shutdown OK");
    }

    protected void initSettings(ConfigContainer config) {
        JSonWrapper subConfig = getPluginConfig();

        config.setGroup(new ConfigGroup(getName(), getIconKey()));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_ENABLEDONSTART, T._.gui_config_jdshutdown_enabledOnStart()).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, subConfig, CONFIG_MODE, MODES_AVAIL, T._.gui_config_jdshutdown_mode()).setDefaultValue(0));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_FORCESHUTDOWN, T._.gui_config_jdshutdown_forceshutdown()).setDefaultValue(false));

        /* enable force shutdown for Mac OSX */
        if (OSDetector.isMac()) {
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

            }, T._.gui_config_jdshutdown_osx_force_short(), T._.gui_config_jdshutdown_osx_force_long(), null));
        }
    }

    @Override
    public String getConfigID() {
        return "shutdown";
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return T._.jd_plugins_optional_jdshutdown_description();
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(menuAction);
        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
        ConfigContainer cc = new ConfigContainer(getName());
        initSettings(cc);
        configPanel = createPanelFromContainer(cc);
    }
}