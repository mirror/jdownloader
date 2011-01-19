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

import jd.gui.UserIO;
import jd.nutils.JDFlags;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

public class CheckJava {

    public static boolean check() {
        String runtimeName = System.getProperty("java.runtime.name");
        String runtimeVersion = System.getProperty("java.version");
        if (runtimeName == null || runtimeVersion == null) return false;
        runtimeName = runtimeName.toLowerCase();
        runtimeVersion = runtimeVersion.toLowerCase();
        if (!new Regex(runtimeVersion, "1\\.(5|6|7)").matches()) {
            String html = JDL.LF("gui.javacheck.html", "<link href='http://jdownloader.org/jdcss.css' rel='stylesheet' type='text/css' /><div style='width:534px;height;200px'><h2>You useses a wrong Java version. Please use a original Sun Java. Start jDownloader anyway?<table width='100%%'><tr><th colspan='2'>Your Java Version:</th></tr><tr><th>Runtime Name</th><td>%s</td></tr><tr><th>Runtime Version</th><td>%s</td></tr></table></div>", runtimeName, runtimeVersion);
            return JDFlags.hasAllFlags(UserIO.getInstance().requestConfirmDialog(UserIO.NO_COUNTDOWN | UserIO.STYLE_HTML, JDL.L("gui.javacheck.title", "Wrong Java Version"), html), UserIO.RETURN_OK);
        }
        return true;
    }
}
