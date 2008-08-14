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

public class SaveRaidrushWs extends PluginForDecrypt {

    static private final String host = "save.raidrush.ws";

    private Pattern patternCount = Pattern.compile("\',\'FREE\',\'");
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?save\\.raidrush\\.ws/\\?id\\=[a-zA-Z0-9]+", Pattern.CASE_INSENSITIVE);

    public SaveRaidrushWs() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            RequestInfo reqinfo = HTTP.getRequest(new URL(parameter));
            progress.setRange(SimpleMatches.countOccurences(reqinfo.getHtmlCode(), patternCount));
            ArrayList<ArrayList<String>> links = SimpleMatches.getAllSimpleMatches(reqinfo.getHtmlCode(), "get('°','FREE','°');");
            for (int i = 0; i < links.size(); i++) {
                ArrayList<String> help = links.get(i);
                reqinfo = HTTP.getRequest(new URL("http://save.raidrush.ws/404.php.php?id=" + help.get(0) + "&key=" + help.get(1)));
                progress.increase(1);
                decryptedLinks.add(createDownloadlink("http://" + reqinfo.getHtmlCode().trim()));
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