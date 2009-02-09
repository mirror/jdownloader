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

import java.io.File;

import jd.gui.skins.simple.SimpleGUI;
import jd.nutils.OSDetector;

import com.ice.jni.registry.RegStringValue;
import com.ice.jni.registry.Registry;
import com.ice.jni.registry.RegistryException;
import com.ice.jni.registry.RegistryKey;
import com.ice.jni.registry.RegistryValue;

public class JDFileReg {

    public static void setKey(String key, String valueName, String value) throws RegistryException {
        RegistryKey topKey = Registry.getTopLevelKey("HKCR");
        RegistryKey localKey = topKey.openSubKey(key);
        String dv = localKey.getDefaultValue();

        if (!dv.equals(value)) {
            JDUtilities.getLogger().info("Created Windows Registry entry:" + key + "=" + value);
            localKey = topKey.createSubKey(key, value, RegistryKey.ACCESS_WRITE);
        }
        RegistryValue v = localKey.getValue(valueName);
        if (!v.equals(value)) {
            RegStringValue val = new RegStringValue(localKey, valueName, value);
            JDUtilities.getLogger().info("Created Windows Registry entry:" + key + "/" + valueName + "=" + value);
            localKey.setValue(val);
            localKey.flushKey();
        }
    }

    public static void registerFileExts() {
        // 5bc4004260d83e0cf69addb8f9262837
        // 6f3ad5e9971f92aa28eb01c2ac11f896
        // f19fbcb71e9682d307e331c04a45fd53
        try {
            if (OSDetector.isWindows() && JDUtilities.getSubConfig(SimpleGUI.GUICONFIGNAME).getBooleanProperty("FILE_REGISTER", true)) {
                registerWinFileExt("jd");
                registerWinFileExt("dlc");
                registerWinFileExt("ccf");
                registerWinFileExt("rsdf");
                registerWinProtocol("jd");
                registerWinProtocol("jdlist");
                registerWinProtocol("dlc");
                registerWinProtocol("ccf");
                registerWinProtocol("rsdf");
            }
        } catch (Throwable e) {
            System.err.println("Run in " + new File("ICE_JNIRegistry.dll").getAbsolutePath());
        }
    }

    private static void registerWinFileExt(String ext) throws RegistryException {
        String name = "JDownloader " + ext + "-Container";
        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath() + " \"%1\"";

        setKey(name, "", "JDownloader " + ext + " file");
        setKey(name + "\\shell", "", "open");
        setKey(name + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath());
        setKey(name + "\\shell\\open\\command", "", command);
    }

    private static void registerWinProtocol(String p) throws RegistryException {
        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath() + " --add-link \"%1\"";

        setKey(p, "", "JDownloader " + p);
        setKey(p + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath());
        setKey(p + "\\shell", "", "open");
        setKey(p, "Url Protocol", "");
        setKey(p + "\\shell\\open\\command", "", command);
    }
}
