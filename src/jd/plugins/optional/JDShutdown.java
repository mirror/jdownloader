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

import jd.config.MenuItem;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.event.ControlEvent;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.parser.Regex;
import jd.plugins.PluginOptional;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class JDShutdown extends PluginOptional {
    private SubConfiguration subConfig = JDUtilities.getSubConfig("ADDONS_JDSHUTDOWN");

    private static final int count = 60;

    private static final String PROPERTY_ENABLED = "PROPERTY_ENABLED";

    public static int getAddonInterfaceVersion() {
        return 0;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuItem mi = (MenuItem) e.getSource();
        if (mi.getActionID() == 0) {
            subConfig.setProperty(PROPERTY_ENABLED, true);
            subConfig.save();
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("addons.jdshutdown.statusmessage.enabled", "JDownloader wird das System nach dem Download herunterfahren."));
        } else {
            subConfig.setProperty(PROPERTY_ENABLED, false);
            subConfig.save();
            JDUtilities.getGUI().showMessageDialog(JDLocale.L("addons.jdshutdown.statusmessage.disabled", "Das System wird von JDownloader NICHT heruntergefahren."));
        }
    }

    @Override
    public void controlEvent(ControlEvent event) {
        super.controlEvent(event);
        if (subConfig.getBooleanProperty(PROPERTY_ENABLED, false)) {
            if (event.getID() == ControlEvent.CONTROL_INTERACTION_CALL) {
                if ((InteractionTrigger) event.getSource() == Interaction.INTERACTION_AFTER_DOWNLOAD_AND_INTERACTIONS) {
                    subConfig.setProperty(PROPERTY_ENABLED, false);
                    subConfig.save();
                    shutDown();
                }
            }
        }
    }

    @Override
    public ArrayList<MenuItem> createMenuitems() {
        ArrayList<MenuItem> menu = new ArrayList<MenuItem>();
        MenuItem m;
        if (!subConfig.getBooleanProperty(PROPERTY_ENABLED, false)) {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdshutdown.menu.enable", "Herunterfahren aktivieren"), 0).setActionListener(this));
            m.setSelected(false);
        } else {
            menu.add(m = new MenuItem(MenuItem.TOGGLE, JDLocale.L("addons.jdshutdown.menu.disable", "Herunterfahren deaktivieren"), 1).setActionListener(this));
            m.setSelected(true);
        }
        return menu;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getPluginName() {
        return JDLocale.L("plugins.optional.jdshutdown.name", "JDShutdown");
    }

    @Override
    public String getRequirements() {
        return "JRE 1.5+";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public boolean initAddon() {

        JDUtilities.getController().addControlListener(this);
        logger.info("Shutdown OK");
        return true;

    }

    @Override
    public void onExit() {
//        Wollen wir, dass der Status auch beim "normalen" Beenden resettet wird?
//        subConfig.setProperty(PROPERTY_ENABLED, false);
//        subConfig.save();
    }

    public void shutDown() {

        CountdownConfirmDialog shutDownMessage = new CountdownConfirmDialog(((SimpleGUI) JDUtilities.getGUI()).getFrame(), JDLocale.L("interaction.shutdown.dialog.msg", "<h2><font color=\"red\">Achtung ihr Betriebssystem wird heruntergefahren!</font></h2>"), count, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
        if (shutDownMessage.result) {
            String OS = System.getProperty("os.name").toLowerCase();
            if (OS.indexOf("windows xp") > -1 || OS.indexOf("windows vista") > -1) {
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
            } else if (OS.indexOf("windows 2000") > -1 || OS.indexOf("nt") > -1) {
                try {
                    JDUtilities.runCommand("shutdown.exe", new String[] { "-s", "-t", "01" }, null, 0);
                } catch (Exception e) {
                }
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

}
