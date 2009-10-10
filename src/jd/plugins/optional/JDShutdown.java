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

package jd.plugins.optional;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.nutils.JDFlags;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "shutdown", interfaceversion = 5)
public class JDShutdown extends PluginOptional {

    private static final int count = 60;
    private static final String CONFIG_STANDBY = "STANDBY";
    private static final String CONFIG_HIBERNATE = "HIBERNATE";
    private static final String CONFIG_FORCESHUTDOWN = "FORCE";
    private static Thread shutdown = null;
    private static boolean shutdownenabled = false;
    private static MenuAction menuAction = null;

    public JDShutdown(PluginWrapper wrapper) {
        super(wrapper);
        initConfig();
    }

    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        if (shutdownenabled) {
            if (event.getID() == ControlEvent.CONTROL_INTERACTION_CALL) {
                if ((InteractionTrigger) event.getSource() == Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS) {
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
    }

    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        menu.add(menuAction);
        return menu;
    }

    public boolean initAddon() {
        if (menuAction == null) menuAction = new MenuAction("gui.jdshutdown.toggle", "gui.images.logout") {
            private static final long serialVersionUID = 4359802245569811800L;

            @Override
            public void initDefaults() {
                setPriority(800);
                this.setToolTipText(JDL.L("gui.jdshutdown.toggle.tooltip", "Enable/Disable Shutdown after Downloads"));
                this.setEnabled(true);
                setType(ToolBarAction.Types.TOGGLE);
                this.setSelected(false);
                this.setIcon("gui.images.logout");
                this.addPropertyChangeListener(new PropertyChangeListener() {
                    public void propertyChange(PropertyChangeEvent evt) {
                        if (evt.getPropertyName() == SELECTED_KEY) {
                            shutdownenabled = isSelected();
                            if (shutdownenabled) {
                                UserIO.getInstance().requestMessageDialog(JDL.L("addons.jdshutdown.statusmessage.enabled", "Das System wird nach dem Download heruntergefahren."));
                            } else {
                                UserIO.getInstance().requestMessageDialog(JDL.L("addons.jdshutdown.statusmessage.disabled", "Das System wird nach dem Download NICHT heruntergefahren."));
                            }
                        }
                    }
                });
            }

        };
        menuAction.setSelected(false);
        JDUtilities.getController().addControlListener(this);
        logger.info("Shutdown OK");
        return true;
    }

    public void onExit() {
        JDUtilities.getController().removeControlListener(this);
    }

    private void shutDownWin() {
        if (!getPluginConfig().getBooleanProperty(CONFIG_STANDBY, false)) {
            if (getPluginConfig().getBooleanProperty(CONFIG_FORCESHUTDOWN, false)) {
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-f", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
            } else {
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
                try {
                    JDUtilities.runCommand("%windir%\\system32\\shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
            }
        } else {
            if (getPluginConfig().getBooleanProperty(CONFIG_HIBERNATE, false)) {
                try {
                    JDUtilities.runCommand("powercfg.exe", new String[] { "hibernate on" }, null, 0);
                } catch (Exception e) {
                    try {
                        JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", new String[] { "hibernate on" }, null, 0);
                    } catch (Exception ex) {
                    }
                }
            } else {
                try {
                    JDUtilities.runCommand("powercfg.exe", new String[] { "hibernate off" }, null, 0);
                } catch (Exception e) {
                    try {
                        JDUtilities.runCommand("%windir%\\system32\\powercfg.exe", new String[] { "hibernate off" }, null, 0);
                    } catch (Exception ex) {
                    }
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
        }
    }

    class ShutDown extends Thread {

        /*
         * Wait for JD-Unrar
         */
        public void run() {
            OptionalPluginWrapper addon = JDUtilities.getOptionalPlugin("unrar");
            if (addon != null && addon.isEnabled()) {
                while (true) {
                    Object obj = addon.getPlugin().interact("isWorking", null);
                    if (obj == null || (obj instanceof Boolean && obj.equals(false))) break;
                    logger.info("JD-Unrar is working - wait before shutting down");
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        JDLogger.exception(e);
                    }
                }
            }
            logger.info("Shutting down now");
            String message = JDL.L("interaction.shutdown.dialog.msg", "<h2><font color=\"red\">Achtung ihr Betriebssystem wird heruntergefahren!</font></h2>");
            UserIO.setCountdownTime(count);
            int ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("interaction.shutdown.dialog.title", "Shutdown"), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
            UserIO.setCountdownTime(-1);
            logger.info("Return code: " + ret);
            if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                logger.info("Prepare Shutdown");
                JDUtilities.getController().prepareShutdown(false);
                switch (OSDetector.getOSID()) {
                case OSDetector.OS_WINDOWS_2003:
                case OSDetector.OS_WINDOWS_VISTA:
                case OSDetector.OS_WINDOWS_XP:
                case OSDetector.OS_WINDOWS_7:
                    shutDownWin();
                    break;
                case OSDetector.OS_WINDOWS_2000:
                case OSDetector.OS_WINDOWS_NT:
                    shutDownWin();
                    try {
                        FileWriter fw = null;
                        BufferedWriter bw = null;
                        try {
                            fw = new FileWriter(JDUtilities.getResourceFile("jd/shutdown.vbs"));
                            bw = new BufferedWriter(fw);

                            bw.write("set WshShell = CreateObject(\"WScript.Shell\")\r\nWshShell.SendKeys \"^{ESC}^{ESC}^{ESC}{UP}{ENTER}{ENTER}\"\r\n");

                            bw.flush();
                            bw.close();

                            JDUtilities.runCommand("cmd", new String[] { "/c", "start", "/min", "cscript", JDUtilities.getResourceFile("jd/shutdown.vbs").getAbsolutePath() }, null, 0);

                        } catch (IOException e) {
                        }
                    } catch (Exception e) {
                    }
                    break;
                case OSDetector.OS_WINDOWS_OTHER:
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
                    try {
                        if (getPluginConfig().getBooleanProperty(CONFIG_HIBERNATE, false)) {
                            JDUtilities.runCommand("/usr/bin/osascript", new String[] { JDUtilities.getResourceFile("jd/osx/osxhibernate.scpt").getAbsolutePath() }, null, 0);
                        } else {
                            JDUtilities.runCommand("/usr/bin/osascript", new String[] { JDUtilities.getResourceFile("jd/osx/osxshutdown.scpt").getAbsolutePath() }, null, 0);
                        }
                    } catch (Exception e) {
                    }
                default:
                    if (getPluginConfig().getBooleanProperty(CONFIG_HIBERNATE, false)) {
                        try {
                            dbusPowerState("Hibernate");
                        } catch (Exception e) {
                        }
                    } else if (getPluginConfig().getBooleanProperty(CONFIG_STANDBY, false)) {
                        try {
                            dbusPowerState("Suspend");
                        } catch (Exception e) {
                        }
                    } else {
                        try {
                            dbusPowerState("Shutdown");
                        } catch (Exception e) {
                        }
                        try {
                            JDUtilities.runCommand("dcop", new String[] { "--all-sessions", "--all-users", "ksmserver", "ksmserver", "logout", "0", "2", "0" }, null, 0);
                        } catch (Exception e) {
                        }
                        try {
                            JDUtilities.runCommand("sudo", new String[] { "shutdown", "-h", "now" }, null, 0);
                        } catch (Exception e) {
                        }
                    }
                    break;
                }
            }
        }
    }

    private void dbusPowerState(String command) {
        JDUtilities.runCommand("dbus-send", new String[] { "--session", "--dest=org.freedesktop.PowerManagement", "--type=method_call", "--print-reply", "--reply-timeout=2000", "/org/freedesktop/PowerManagement", "org.freedesktop.PowerManagement." + command }, null, 0);
    }

    public String getIconKey() {
        return "gui.images.logout";
    }

    public void initConfig() {
        SubConfiguration subConfig = getPluginConfig();
        ConfigEntry ce;
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_STANDBY, JDL.L("gui.config.jdshutdown.standby", "Standby (Nur einige OS)")));
        ce.setDefaultValue(false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_HIBERNATE, JDL.L("gui.config.jdshutdown.hibernate", "Ruhezustand/Hibernate (Nur einige OS)")));
        ce.setDefaultValue(false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_FORCESHUTDOWN, JDL.L("gui.config.jdshutdown.forceshutdown", "Herunterfahren erzwingen (Nur einige OS)")));
        ce.setDefaultValue(false);
    }
}