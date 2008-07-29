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
import java.util.HashMap;
import java.util.regex.Pattern;
import jd.parser.HTMLParser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;
import jd.utils.JDUtilities;

public class RsHoerbuchin extends PluginForDecrypt {
    static private final String host = "rs.hoerbuch.in";

    private String version = "1.0.0.2";
    private static final Pattern patternLink_RS = Pattern.compile("http://rs\\.hoerbuch\\.in/com-[\\w]{11}/.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_DE = Pattern.compile("http://rs\\.hoerbuch\\.in/de-[\\w]{11}/.*", Pattern.CASE_INSENSITIVE);
    private static final Pattern patternLink_UP = Pattern.compile("http://rs\\.hoerbuch\\.in/u[\\w]{6}.html", Pattern.CASE_INSENSITIVE);
    static private final Pattern patternSupported = Pattern.compile(patternLink_RS.pattern() + "|" + patternLink_DE.pattern() + "|" + patternLink_UP.pattern(), patternLink_RS.flags() | patternLink_DE.flags() | patternLink_UP.flags());

    public RsHoerbuchin() {
        super();
    }

    @Override
    public String getCoder() {
        return "JD-Team";
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
    public String getHost() {
        return host;
    }

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public String getPluginID() {
        return host + "-" + version;
    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo requestInfo = HTTP.getRequest(url);
            if (parameter.matches(patternLink_RS.pattern())) {
                HashMap<String, String> fields = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode(), "postit", "starten");
                String newURL = "http://rapidshare.com" + JDUtilities.htmlDecode(fields.get("uri"));
                decryptedLinks.add(this.createDownloadlink(newURL));
            } else if (parameter.matches(patternLink_DE.pattern())) {
                HashMap<String, String> fields = HTMLParser.getInputHiddenFields(requestInfo.getHtmlCode(), "postit", "starten");
                String newURL = "http://rapidshare.de" + JDUtilities.htmlDecode(fields.get("uri"));
                decryptedLinks.add(this.createDownloadlink(newURL));
            } else if (parameter.matches(patternLink_UP.pattern())) {
                String links[][] = new Regex(requestInfo, Pattern.compile("<form action=\"(.*?)\" method=\"post\" id=\"postit\"", Pattern.CASE_INSENSITIVE)).getMatches();
                for (int i = 0; i < links.length; i++)
                    decryptedLinks.add(this.createDownloadlink(links[i][0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return decryptedLinks;
    }
}
