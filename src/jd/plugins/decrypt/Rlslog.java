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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class Rlslog extends PluginForDecrypt {
    static private final String host = "rlslog.net";
    private Pattern patternSupported = Pattern.compile("(http://[\\w\\.]*?rlslog\\.net(/.+/.+/#comments|/.+/#comments|/.+/))", Pattern.CASE_INSENSITIVE);

    public Rlslog(String cfgName){
        super(cfgName);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        String parameter = param.toString();
        String followcomments = "";

        if (parameter.contains("/comment-page")) {
            followcomments = parameter.substring(0, parameter.indexOf("/comment-page"));
        }
        if (!parameter.contains("#comments")) {
            parameter += "#comments";
            followcomments = parameter.substring(0, parameter.indexOf("/#comments"));
        } else {
            followcomments = parameter.substring(0, parameter.indexOf("/#comments"));
        }
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String[] Links = HTMLParser.getHttpLinks(reqinfo.getHtmlCode(), null);
            Vector<String> passs = HTMLParser.findPasswords(reqinfo.getHtmlCode());
            for (String element : Links) {
                if (element.contains(followcomments) == true) {
                    /* weitere comment pages abrufen */
                    URL url2 = new URL(element);
                    RequestInfo reqinfo2 = HTTP.getRequest(url2);
                    String[] Links2 = HTMLParser.getHttpLinks(reqinfo2.getHtmlCode(), null);
                    Vector<String> passs2 = HTMLParser.findPasswords(reqinfo2.getHtmlCode());
                    for (String element2 : Links2) {
                        DownloadLink l = createDownloadlink(element2);
                        decryptedLinks.add(l);
                        l.addSourcePluginPasswords(passs2);
                    }
                } else {
                    DownloadLink l = createDownloadlink(element);
                    decryptedLinks.add(l);
                    l.addSourcePluginPasswords(passs);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
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
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}