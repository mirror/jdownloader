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

package jd.controlling.interaction;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;

import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.CountdownConfirmDialog;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

/**
 * Diese Klasse fürs automatische Entpacken
 * 
 * @author DwD
 */
public class ShutDown extends Interaction implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = 2467582501274722811L;
    private static final int count = 60;

    @Override
    public boolean doInteraction(Object arg) {
        start();
        return true;

    }
    @Override
    public String toString() {
        return getInteractionName();
    }

    @Override
    public String getInteractionName() {
        return JDLocale.L("interaction.shutdown.name", "Shutdown");
    }


    @Override
    public void run() {
        CountdownConfirmDialog shutDownMessage = new CountdownConfirmDialog(((SimpleGUI) JDUtilities.getGUI()).getFrame(), JDLocale.L("interaction.shutdown.dialog.msg", "<h2><font color=\"red\">Achtung ihr Betriebssystem wird heruntergefahren!</font></h2>"), count, true, CountdownConfirmDialog.STYLE_OK | CountdownConfirmDialog.STYLE_CANCEL);
        if(shutDownMessage.result)
        {
        String OS = System.getProperty("os.name").toLowerCase();
        if ((OS.indexOf("windows xp") > -1) || (OS.indexOf("windows vista") > -1)) {
            try {
                JDUtilities.runCommand("shutdown.exe", new String[] {"-s", "-t", "01"}, null, 0);
            } catch (Exception e) {
            }
        }
        else if(OS.indexOf("windows 2000") > -1|| (OS.indexOf("nt") > -1) )
        {
            try {
                JDUtilities.runCommand("shutdown.exe", new String[] {"-s", "-t", "01"}, null, 0);
            } catch (Exception e) {
            }
            try {
                FileWriter fw = null;
                BufferedWriter bw = null;
                try {
                    fw = new FileWriter(JDUtilities.getResourceFile("jd/shutdown.vbs"));
                    bw = new BufferedWriter(fw);

                    bw.write(
                        "set WshShell = CreateObject(\"WScript.Shell\")\r\nWshShell.SendKeys \"^{ESC}^{ESC}^{ESC}{UP}{ENTER}{ENTER}\"\r\n");

                    bw.flush();
                                                                        bw.close();

                    JDUtilities.runCommand("cmd", new String[] {"/c", "start", "/min", "cscript", JDUtilities.getResourceFile("jd/shutdown.vbs").getAbsolutePath()}, null, 0);

                } catch (IOException e) {
                }
            } catch (Exception e) {
            }
        }
        else if (OS.indexOf("windows") > -1) {


            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] {"user,ExitWindows"}, null, 0);
            } catch (Exception e) {
            }
            try {
                JDUtilities.runCommand("RUNDLL32.EXE", new String[] {"Shell32,SHExitWindowsEx", "1"}, null, 0);
            } catch (Exception e) {
            }

        }
        else if (OS.indexOf("mac") >= 0) {
           JDUtilities.runCommand("osascript", new String[] {JDUtilities.getResourceFile("jd/osx/osxshutdown.scpt").getAbsolutePath()}, null, 0);
        }
        else {
        try {
            JDUtilities.runCommand("dbus-send", new String[] {"--session", "--dest=org.freedesktop.PowerManagement", "--type=method_call", "--print-reply", "--reply-timeout=2000", "/org/freedesktop/PowerManagement", "org.freedesktop.PowerManagement.Shutdown"}, null, 0);
        } catch (Exception e) {
        }
        try {
            JDUtilities.runCommand("dcop", new String[] {"--all-sessions", "--all-users", "ksmserver", "ksmserver", "logout", "0", "2", "0"}, null, 0);
        } catch (Exception e) {
        }
        try {
            JDUtilities.runCommand("sudo", new String[] {"shutdown", "-h", "now"}, null, 0);
        } catch (Exception e) {
        }
        }
        }
    }

    @Override
    public void initConfig() {

    }

    @Override
    public void resetInteraction() {
    }
}