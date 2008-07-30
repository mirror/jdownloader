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
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class GappingOrg extends PluginForDecrypt {
    final static String host = "gapping.org";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?gapping\\.org/(index\\.php\\?folderid=[0-9]+|file\\.php\\?id=.+|f/.+)", Pattern.CASE_INSENSITIVE);

    // private String version = "0.1.0";

    public GappingOrg() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            if (parameter.indexOf("index.php") != -1) {

                RequestInfo request = HTTP.getRequest(new URL(parameter));
                String ids[][] = new Regex(request.getHtmlCode(), Pattern.compile("href=\"http://gapping\\.org/file\\.php\\?id=(.*?)\" >", Pattern.CASE_INSENSITIVE)).getMatches();

                progress.setRange(ids.length);
                for (String[] element : ids) {
                    request = HTTP.getRequest(new URL("http://gapping.org/decry.php?fileid=" + element[0]));
                    String link = new Regex(request.getHtmlCode(), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                    decryptedLinks.add(createDownloadlink(link));
                    progress.increase(1);
                }

            } else if (parameter.indexOf("file.php") != -1) {

                parameter = parameter.replace("file.php?id=", "decry.php?fileid=");
                RequestInfo request = HTTP.getRequest(new URL(parameter));
                String link = new Regex(request.getHtmlCode(), Pattern.compile("src=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getFirstMatch();
                progress.setRange(1);
                decryptedLinks.add(createDownloadlink(link));
                progress.increase(1);

            } else {
                RequestInfo request = HTTP.getRequest(new URL(parameter));

                ArrayList<String> links = SimpleMatches.getAllSimpleMatches(request, "<a target=\"_blank\" onclick=\"image°.src='http://www.gapping.org/img/°';\" href=\"°http://gapping.org/d/°\" >", 4);

                for (String link : links) {
                    RequestInfo ri = HTTP.getRequest(new URL("http://gapping.org/d/" + link));
                    String url = SimpleMatches.getSimpleMatch(ri, "<iframe height=° width=°  name=° src=\"°\" frameborder=\"0\"   />", 3);
                    decryptedLinks.add(createDownloadlink(url.trim()));
                }
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
        return "jD-Team";
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