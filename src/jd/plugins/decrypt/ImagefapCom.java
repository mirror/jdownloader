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

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class ImagefapCom extends PluginForDecrypt {
    static private final String host = "imagefap.com";
    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?imagefap\\.com/(gallery\\.php\\?gid=.+|gallery/.+)", Pattern.CASE_INSENSITIVE);

    private URL url;

    // private String version = "1.0.0.0";

    public ImagefapCom() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            parameter = parameter.replaceAll("view\\=[0-9]+", "view=2");
            if (!parameter.contains("view=2")) {
                parameter += "&view=2";
            }
            url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String links[][] = new Regex(reqinfo.getHtmlCode(), Pattern.compile("image\\.php\\?id=(.*?)\">", Pattern.CASE_INSENSITIVE)).getMatches();
            progress.setRange(links.length);
            for (String[] element : links) {
                DownloadLink link = createDownloadlink("http://imagefap.com/image.php?id=" + element[0]);
                decryptedLinks.add(link);
                progress.increase(1);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
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