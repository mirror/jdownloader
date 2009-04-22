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

package jd.utils;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;

public class JDFileReg {

    public static String createSetKey(String key, String valueName, String value) {
        StringBuilder sb = new StringBuilder();

        sb.append("\r\n[HKEY_CLASSES_ROOT\\" + key + "]");

        if (valueName != null && valueName.trim().length() > 0) {
            sb.append("\r\n\"" + valueName + "\"=\"" + value + "\"");
        } else {
            sb.append("\r\n@=\"" + value + "\"");
        }

        return sb.toString();
    }

    public static void registerFileExts() {

        if (OSDetector.isWindows() && SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME).getBooleanProperty("FILE_REGISTER", true)) {
            StringBuilder sb = new StringBuilder();
            sb.append(createRegisterWinFileExt("jd"));
            sb.append(createRegisterWinFileExt("dlc"));
            sb.append(createRegisterWinFileExt("ccf"));
            sb.append(createRegisterWinFileExt("rsdf"));
            sb.append(createRegisterWinProtocol("jd"));
            sb.append(createRegisterWinProtocol("jdlist"));
            sb.append(createRegisterWinProtocol("dlc"));
            sb.append(createRegisterWinProtocol("ccf"));
            sb.append(createRegisterWinProtocol("rsdf"));
            JDIO.writeLocalFile(JDUtilities.getResourceFile("tmp/installcnl.reg"), "Windows Registry Editor Version 5.00\r\n\r\n\r\n\r\n" + sb.toString());

            JDUtilities.runCommand("regedit", new String[] { "/e", "test.reg", "HKEY_CLASSES_ROOT\\.dlc" }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
            if (!JDUtilities.getResourceFile("tmp/test.reg").exists()) {

                JDUtilities.runCommand("regedit", new String[] { "/s", "installcnl.reg" }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
                JDUtilities.runCommand("regedit", new String[] { "/e", "test.reg", "HKEY_CLASSES_ROOT\\.dlc" }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
                if (JDUtilities.getResourceFile("tmp/test.reg").exists()) {
                    JDLogger.getLogger().info("Installed Click'n'Load and associated .*dlc,.*ccf,.*rsdf and .*jd with JDownloader. Uninstall with " + JDUtilities.getResourceFile("tools/windows/uninstall.reg"));
                } else {
                    JDLogger.getLogger().severe("Installation of CLick'n'Load failed. Try to execute " + JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath() + " manually");
                }

            }
            JDUtilities.getResourceFile("tmp/test.reg").delete();

        }

    }

    private static String createRegisterWinFileExt(String ext) {

        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\") + " \\\"%1\\\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\r\n;Register fileextension ." + ext);
        sb.append(createSetKey("." + ext, "", "JDownloader " + ext + " file"));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\shell", "", "open"));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\")));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\shell\\open\\command", "", command));
        return sb.toString();
    }

    private static String createRegisterWinProtocol(String p) {
        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\") + " --add-link \\\"%1\\\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\r\n;Register Protocol " + p + "://jdownloader.org/sample." + p);
        sb.append(createSetKey(p, "", "JDownloader " + p));
        sb.append(createSetKey(p + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\")));
        sb.append(createSetKey(p + "\\shell", "", "open"));
        sb.append(createSetKey(p, "Url Protocol", ""));
        sb.append(createSetKey(p + "\\shell\\open\\command", "", command));
        return sb.toString();
    }
}
