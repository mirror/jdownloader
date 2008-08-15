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
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.parser.SimpleMatches;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class DreiDlAm extends PluginForDecrypt {

    final static String host = "3dl.am";

    // ohne abschliessendes "/" gehts nicht (auch im Browser)!
    private Pattern patternSupported = Pattern.compile("(http://[\\w\\.]*?3dl\\.am/link/[a-zA-Z0-9]+)" + "|(http://[\\w\\.]*?3dl\\.am/download/start/[0-9]+/)" + "|(http://[\\w\\.]*?3dl\\.am/download/[0-9]+/.+\\.html)", Pattern.CASE_INSENSITIVE);

    public DreiDlAm() {
        super();
    }

    private String decryptFromDownload(String parameter) {
        String link = new String();

        try {
            parameter.replace("&quot;", "\"");

            RequestInfo request = HTTP.getRequest(new URL(parameter));
            String layer = SimpleMatches.getBetween(request.getHtmlCode(), "<form action=\"http://3dl.am/download/start/", "/\"");
            link = "http://3dl.am/download/start/" + layer + "/";

            // passwort auslesen
            if (request.getHtmlCode().indexOf("<b>Passwort:</b></td><td><input type='text' value='") != -1) {

                String password = SimpleMatches.getBetween(request.getHtmlCode(), "<b>Passwort:</b></td><td><input type='text' value='", "'");

                if (!password.contains("kein") && !password.contains("kein P")) {
                    default_password.add(password);
                }

            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return link;
    }

    private String decryptFromLink(String parameter) {
        String link = new String();
        try {
            RequestInfo request = HTTP.getRequest(new URL(parameter));
            String layer = SimpleMatches.getBetween(request.getHtmlCode(), "<frame src=\"", "\" width=\"100%\"");
            link = layer;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return link;
    }

    private ArrayList<String> decryptFromStart(String parameter) {
        ArrayList<ArrayList<String>> links = new ArrayList<ArrayList<String>>();
        ArrayList<String> linksReturn = new ArrayList<String>();
        try {
            RequestInfo request = HTTP.getRequest(new URL(parameter));
            links = SimpleMatches.getAllSimpleMatches(request.getHtmlCode(), "value='http://3dl.am/link/°/'");

            for (int i = 0; i < links.size(); i++) {
                linksReturn.add("http://3dl.am/link/" + links.get(i).get(0) + "/");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return linksReturn;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        if (parameter.indexOf("3dl.am/download/start/") != -1) {
            ArrayList<String> links = decryptFromStart(parameter);
            progress.setRange(links.size());
            String link = new String();

            for (int i = 0; i < links.size(); i++) {
                progress.increase(1);
                link = decryptFromLink(links.get(i));
                decryptedLinks.add(createDownloadlink(link));
            }
        } else if (parameter.indexOf("3dl.am/link/") != -1) {
            progress.setRange(1);
            String link = decryptFromLink(parameter);
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        } else if (parameter.indexOf("3dl.am/download/") != -1) {
            String link1 = decryptFromDownload(parameter);
            ArrayList<String> links = decryptFromStart(link1);
            progress.setRange(links.size());
            String link2 = new String();
            for (int i = 0; i < links.size(); i++) {
                progress.increase(1);
                link2 = decryptFromLink(links.get(i));
                decryptedLinks.add(createDownloadlink(link2));
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team, b0ffed";
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