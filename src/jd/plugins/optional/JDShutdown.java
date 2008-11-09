//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDShutdown extends PluginOptional {

    private static final int count = 60;
    private static final String CONFIG_STANDBY = "STANDBY";
    private static final String CONFIG_FORCESHUTDOWN = "FORCE";

    public static int getAddonInterfaceVersion() {
        return 2;
    }

    private MenuItem menuItem;

    public JDShutdown(PluginWrapper wrapper) {
        super(wrapper);
        initConfig();
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == menuItem) {
            menuItem.setSelected(!menuItem.isSelected());
            if (menuItem.isSelected()) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("addons.jdshutdown.statusmessage.enabled", "Das System wird nach dem Download heruntergefahren."));
            } else {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("addons.jdshutdown.statusmessage.disabled", "Das System wird nach dem Download NICHT heruntergefahren."));
            }
        }
    }

    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        if (menuItem != null && menuItem.isSelected()) {
            if (event.getID() == ControlEvent.CONTROL_INTERACTION_CALL) {
                if ((InteractionTrigger) event.getSource() == Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS) {
                    shutDown();
                }
            }
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();

        if (menuItem == null) menuItem = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdshutdown.menu", "System nach dem Downloaden herunterfahren"), 0).setActionListener(this);
        menu.add(menuItem);

        return menu;
    }

    @Override
    public String getHost() {
        return JDLocale.L("plugins.optional.jdshutdown.name", "JDShutdown");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public boolean initAddon() {

        JDUtilities.getController().addControlListener(this);
        logger.info("Shutdown OK");
        return true;
    }

    @Override
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
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
            } catch (Exception e) {
            }
            try {
                JDUtilities.runCommand("%windir%\\system32\\RUNDLL32.EXE", new String[] { "powrprof.dll,SetSuspendState" }, null, 0);
            } catch (Exception e) {
            }
        }
    }

    public void shutDown() {

        CountdownConfirmDialog shutDownMessage = new CountdownConfirmDialog(((SimpleGUI) JDUtilities.getGUI()).getFrame(), JDLocale.L("interaction.shutdown.dialog.msg", "<h2><font color=\"red\">Achtung ihr Betriebssystem wird heruntergefahren!</font></h2>"), count, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
        if (shutDownMessage.result) {
            JDUtilities.getController().prepareShutdown();
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.indexOf("windows xp") > -1 || OS.indexOf("windows vista") > -1 || OS.indexOf("windows 2003") > -1) {
                shutDownWin();
            } else if (OS.indexOf("windows 2000") > -1 || OS.indexOf("nt") > -1) {
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
            } else if (OS.indexOf("windows") > -1) {
                try {
                    JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "user,ExitWindows" }, null, 0);
                } catch (Exception e) {
                }
                try {
                    JDUtilities.runCommand("RUNDLL32.EXE", new String[] { "Shell32,SHExitWindowsEx", "1" }, null, 0);
                } catch (Exception e) {
                }
            } else if (OS.indexOf("mac") >= 0) {
                JDUtilities.runCommand("/usr/bin/osascript", new String[] { JDUtilities.getResourceFile("jd/osx/osxshutdown.scpt").getAbsolutePath() }, null, 0);
            } else {
                try {
                    JDUtilities.runCommand("dbus-send", new String[] { "--session", "--dest=org.freedesktop.PowerManagement", "--type=method_call", "--print-reply", "--reply-timeout=2000", "/org/freedesktop/PowerManagement", "org.freedesktop.PowerManagement.Shutdown" }, null, 0);
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
        }
    }

    public void initConfig() {

        SubConfiguration subConfig = getPluginConfig();
        ConfigEntry ce;
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_STANDBY, JDLocale.L("gui.config.jdshutdown.standby", "Standby (Nur einige OS)")));
        ce.setDefaultValue(false);
        config.addEntry(ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, subConfig, CONFIG_FORCESHUTDOWN, JDLocale.L("gui.config.jdshutdown.forceshutdown", "Shutdown erzwingen (Nur einige OS)")));
        ce.setDefaultValue(false);
    }
}