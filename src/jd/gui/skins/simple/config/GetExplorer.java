//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui.skins.simple.config;

import java.io.File;

import jd.config.Configuration;
import jd.utils.JDUtilities;

public class GetExplorer {
    Object[] explorer = (Object[]) JDUtilities.getConfiguration().getProperty(Configuration.PARAM_FILE_BROWSER, null);

    /**
     * Versucht den Programmpfad zum Explorer zu finden
     * 
     * @return
     */
    private static Object[] autoGetExplorerCommand() {
        String OS = System.getProperty("os.name").toLowerCase();
        if ((OS.indexOf("nt") > -1) || (OS.indexOf("windows") > -1)) {
            return new Object[] { "Explorer", "explorer", new String[] { "%%path%%" } };
        }
        else if (OS.indexOf("mac") >= 0) {
            return new Object[] { "Open", "open", new String[] { "'%%path%%'" } };
        }
        else {
            Object[][] programms = new Object[][] { { "nautilus", new String[] { "--browser", "--no-desktop", "%%path%%" } }, { "konqueror", new String[] { "%%path%%" } } };
            try {
                String[] charset = System.getenv("PATH").split(":");
                for (int i = 0; i < charset.length; i++) {
                    for (int j = 0; j < programms.length; j++) {
                        File fi = new File(charset[i], (String) programms[j][0]);
                        if (fi.isFile()) return new Object[] { (String) programms[j][0], fi.getAbsolutePath(), programms[j][1] };
                    }
                }
            }
            catch (Throwable e) {
            }
        }
        return null;
    }

    /**
     * Object[0] = Browsername Object[1] = Befehl zum Browser Object[2] =
     * String[] Parameter
     * 
     * @return
     */
    public Object[] getExplorerCommand() {
        if (explorer == null) {
            explorer = autoGetExplorerCommand();
            if (explorer == null) {
                JDUtilities.getLogger().severe("Can't find explorer command");
            }
            else {
                JDUtilities.getConfiguration().setProperty(Configuration.PARAM_FILE_BROWSER, explorer);
            }
            return explorer;
        }
        else {
            return explorer;
        }

    }

    public boolean openExplorer(File path) {
        getExplorerCommand();
  
        if (path.isDirectory() && explorer != null) {
          
            String spath = path.getAbsolutePath();
            String[] paramsArray = ((String[]) explorer[2]);
            String[] finalParams = new String[paramsArray.length];
            
            for (int i = 0; i < paramsArray.length; i++) {
            
                finalParams[i] = paramsArray[i].replace("%%path%%", spath);
              

            }
          
            JDUtilities.runCommand((String) explorer[1], finalParams, null, 0);
            
            return true;
        }
        return false;
    }

}
