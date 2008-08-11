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

package jd.plugins.decrypt;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class FTI6xto extends PluginForDecrypt {
    static private final String host = "fucktheindustry.ru";

    private static final Pattern patternSupported = Pattern.compile("http://92\\.241\\.164\\.148/(file\\.php\\?id=\\d+|store/file/dlc/forcedl\\.php\\?file=(.*?)\\.dlc)", Pattern.CASE_INSENSITIVE);

    public FTI6xto() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        try {
            if (!parameter.endsWith(".dlc")) {
                URL url = new URL(parameter);
                RequestInfo requestInfo = HTTP.getRequest(url);
                parameter = "http://92.241.164.148/store/file/dlc/forcedl.php?file=" + requestInfo.getFirstMatch("http://92\\.241\\.164\\.148/store/file/dlc/forcedl\\.php\\?file=(.*?)\\.dlc") + ".dlc";
            }
            File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");
            if (Browser.download(container, parameter)) {
                JDUtilities.getController().loadContainerFile(container);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public String getPluginName() {
        return host;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }
}
