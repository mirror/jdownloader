//jDownloader - Downloadmanager
//Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://gnu.org/licenses/>.

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

public class FalinksCom extends PluginForDecrypt {

    static private String host = "falinks.com";

    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?falinks\\.com/\\?fa=link&id=\\d+", Pattern.CASE_INSENSITIVE);

    public FalinksCom() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String pw = new Regex(reqinfo.getHtmlCode(), "</form>\npw: (.*?)\n.*?</td>", Pattern.CASE_INSENSITIVE).getFirstMatch();
            String[] links = new Regex(reqinfo.getHtmlCode(), "\\<input type=\"hidden\" name=\"url\" value=\"(.*?)\" \\/\\>").getMatches(1);
            progress.setRange(links.length);
            for (String link : links) {
                DownloadLink dlLink = createDownloadlink(link);
                dlLink.addSourcePluginPassword(pw);
                decryptedLinks.add(dlLink);
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