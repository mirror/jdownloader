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

import jd.config.Configuration;
import jd.controlling.JDLogger;
import jd.nutils.OSDetector;

public class GetExplorer {
    /**
     * Versucht den Programmpfad zum Explorer zu finden
     * 
     * @return
     */
    private static Object[] autoGetExplorerCommand() {
        if (OSDetector.isWindows()) {
            return new Object[] { "Explorer", "explorer", new String[] { "%%path%%" } };
        } else if (OSDetector.isMac()) {
            return new Object[] { "Open", "/usr/bin/open", new String[] { "%%path%%" } };
        } else {
            final Object[][] programms = new Object[][] { { "dolphin", new String[] { "%%path%%" } }, { "konqueror", new String[] { "%%path%%" } }, { "thunar", new String[] { "%%path%%" } }, { "rox", new String[] { "%%path%%" } }, { "pcmanfm", new String[] { "%%path%%" } }, { "nautilus", new String[] { "--browser", "--no-desktop", "%%path%%" } } };
            try {
                final String[] charset = System.getenv("PATH").split(":");
                for (String element : charset) {
                    for (Object[] element2 : programms) {
                        final File fi = new File(element, (String) element2[0]);
                        if (fi.isFile()) { return new Object[] { (String) element2[0], fi.getAbsolutePath(), element2[1] }; }
                    }
                }
            } catch (Throwable e) {
            }
        }
        return null;
    }

    private static Object[] explorer = JDUtilities.getConfiguration().getGenericProperty(Configuration.PARAM_FILE_BROWSER, (Object[]) null);

    /**
     * Object[0] = Browsername Object[1] = Befehl zum Browser Object[2] =
     * String[] Parameter
     * 
     * @return
     */
    public static Object[] getExplorerCommand() {
        if (explorer != null) {
            if (!new File((String) explorer[1]).exists()) {
                explorer = null;
            }
            JDUtilities.getConfiguration().setProperty(Configuration.PARAM_FILE_BROWSER, null);
        }
        if (explorer == null) {
            explorer = GetExplorer.autoGetExplorerCommand();
            if (explorer == null) {
                JDLogger.getLogger().severe("Can't find explorer command");
            } else {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_FILE_BROWSER, explorer);
            }
        }
        return explorer;
    }

    public static boolean openExplorer(File path) {
        if (path != null) {
            getExplorerCommand();
            while (path != null && !path.isDirectory()) {
                path = path.getParentFile();
            }

            if (path != null && explorer != null) {
                final String spath = path.getAbsolutePath();
                final String[] paramsArray = (String[]) explorer[2];
                final int length = paramsArray.length;
                final String[] finalParams = new String[length];

                for (int i = 0; i < length; i++) {
                    finalParams[i] = paramsArray[i].replace("%%path%%", spath);
                }

                JDUtilities.runCommand((String) explorer[1], finalParams, null, 0);
                return true;
            }
        }
        return false;
    }

}
