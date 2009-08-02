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

import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import jd.OptionalPluginWrapper;
import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuAction;
import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.event.ControlEvent;
import jd.gui.UserIO;
import jd.nutils.JDFlags;
import jd.nutils.OSDetector;
import jd.plugins.OptionalPlugin;
import jd.plugins.PluginOptional;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@OptionalPlugin(rev = "$Revision$", id = "shutdown", interfaceversion = 4)
public class JDShutdown extends PluginOptional {

    private static final int count = 60;
    private static final String CONFIG_STANDBY = "STANDBY";
    private static final String CONFIG_HIBERNATE = "HIBERNATE";
    private static final String CONFIG_FORCESHUTDOWN = "FORCE";
    private static Thread shutdown = null;

    private MenuAction menuItem;

    public JDShutdown(PluginWrapper wrapper) {
        super(wrapper);
        initConfig();
    }

    // @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuItem) {
            menuItem.setSelected(!menuItem.isSelected());
            if (menuItem.isSelected()) {
                UserIO.getInstance().requestMessageDialog(JDL.L("addons.jdshutdown.statusmessage.enabled", "Das System wird nach dem Download heruntergefahren."));
            } else {
                UserIO.getInstance().requestMessageDialog(JDL.L("addons.jdshutdown.statusmessage.disabled", "Das System wird nach dem Download NICHT heruntergefahren."));
            }
        }
    }

    // @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        if (menuItem != null && menuItem.isSelected()) {
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

    // @Override
    public ArrayList<MenuAction> createMenuitems() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();
        if (menuItem == null) menuItem = (MenuAction)new MenuAction(MenuAction.TOGGLE, JDL.L("addons.jdshutdown.menu", "Shutdown after downloads finished"), 0).setActionListener(this);
        menu.add(menuItem);
        return menu;
    }

    // @Override
    public boolean initAddon() {

        JDUtilities.getController().addControlListener(this);
        logger.info("Shutdown OK");
        return true;
    }

    // @Override
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
            for (OptionalPluginWrapper wrapper : OptionalPluginWrapper.getOptionalWrapper()) {
                if (wrapper.isEnabled() && wrapper.getPlugin().getClass().getName().endsWith("JDUnrar")) {
                    boolean logged = false;
                    while (true) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            JDLogger.exception(e);
                        }
                        Object obj = wrapper.getPlugin().interact("isWorking", null);
                        if (obj == null || (obj instanceof Boolean && obj.equals(false))) break;
                        if (!logged) {
                            logger.info("JD-Unrar is working - wait before shutting down");
                            logged = true;
                        }
                    }
                    break;
                }
            }

            logger.info("Shutting down now");
            String message = JDL.L("interaction.shutdown.dialog.msg", "<h2><font color=\"red\">Achtung ihr Betriebssystem wird heruntergefahren!</font></h2>");
            UserIO.setCountdownTime(count);
            int ret = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_HTML, JDL.L("interaction.shutdown.dialog.title", "Shutdown"), message, UserIO.getInstance().getIcon(UserIO.ICON_WARNING), null, null);
            UserIO.setCountdownTime(null);
            logger.info("Return code: " + ret);
            if (JDFlags.hasSomeFlags(ret, UserIO.RETURN_OK, UserIO.RETURN_COUNTDOWN_TIMEOUT)) {
                logger.info("Prepare Shutdown");
                JDUtilities.getController().prepareShutdown();
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