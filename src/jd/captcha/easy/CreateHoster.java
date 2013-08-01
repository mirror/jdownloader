//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.captcha.easy;

import java.io.File;

import jd.nutils.io.JDIO;

import org.appwork.exceptions.WTFException;
import org.jdownloader.controlling.FileCreationManager;

public class CreateHoster {
    public static void createJacinfoXml(EasyMethodFile destination, String user, int lettersize, boolean showLoadDialog) {
        String jacInfoXml = getjacInfoXmlString(destination, user, lettersize, showLoadDialog);
        destination.file = new File(destination.file.getParentFile(), destination.getName().replaceAll("[^a-zA-Z0-9]", "").replaceAll("[AEIOUaeiou]", ""));
        FileCreationManager.getInstance().mkdir(destination.file);

        File ji = destination.getJacinfoXml();
        if (ji.exists()) ji.renameTo(new File(destination.file, "jacinfo_bak_" + System.currentTimeMillis() + ".xml"));
        JDIO.writeLocalFile(ji, jacInfoXml, false);
    }

    private static String getjacInfoXmlString(EasyMethodFile destination, String user, int lettersize, boolean showLoadDialog) {
        String type = destination.getCaptchaType(showLoadDialog);
        return "<jDownloader>\r\n" + "<method name=\"" + destination + "\" author=\"" + user + "\"  services=\"" + destination + "\" /> \r\n" + "<format type=\"" + type + "\" letterNum=\"" + lettersize + "\" />\r\n" + "</jDownloader>";

    }

    public static void setImageType(EasyMethodFile destination) {
        String type = destination.getCaptchaType(true);
        String info = JDIO.readFileToString(destination.getJacinfoXml());
        if (!info.contains("type=\"" + type + "\"")) {
            info = info.replaceFirst("type=\"[^\"]*\"", "type=\"" + type + "\"");
            JDIO.writeLocalFile(destination.getJacinfoXml(), info, false);
        }
    }

    public static void copyScriptJas(EasyMethodFile ParentHost, EasyMethodFile destination) {
        File in = new File(ParentHost.file, "script.jas");
        File out = new File(destination.file, "script.jas");
        if (out.exists()) out.renameTo(new File(destination.file, "script_bak_" + System.currentTimeMillis() + ".jas"));
        if (!in.exists()) { throw new WTFException(in + " must exist"); }
        JDIO.copyFile(in, out);

    }

    public static void create(EasyMethodFile ParentHost, EasyMethodFile destination, String user, int lettersize) {
        createJacinfoXml(destination, user, lettersize, false);
        copyScriptJas(ParentHost, destination);
    }

}
