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
import java.util.Vector;
import java.util.regex.Pattern;

import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class YouTubeCom extends PluginForDecrypt {

    static private String host = "youtube.com";
    static private final Pattern patternswfArgs = Pattern.compile("(.*?swfArgs.*)", Pattern.CASE_INSENSITIVE);
    private static final String PLAYER = "get_video";
    private static final String T = "\"t\"";
    private static final String VIDEO_ID = "video_id";
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?youtube\\.com/watch\\?v=[a-z-_A-Z0-9]+", Pattern.CASE_INSENSITIVE);

    static public final Pattern YT_FILENAME = Pattern.compile("<meta name=\"title\" content=\"(.*?)\">", Pattern.CASE_INSENSITIVE);

    public YouTubeCom() {
        super();
    }

    private String clean(String s) {
        s = s.replaceAll("\"", "");
        s = s.replaceAll("YouTube -", "");
        s = s.replaceAll("YouTube", "");
        s = s.trim();
        return s;
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) {
        Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url);
            String video_id = "";
            String t = "";
            String match = new Regex(reqinfo.getHtmlCode(), patternswfArgs).getFirstMatch();
            if (match == null) { return null; }

            /* DownloadUrl holen */
            String[] lineSub = match.split(",|:");
            for (int i = 0; i < lineSub.length; i++) {
                String s = lineSub[i];
                if (s.indexOf(VIDEO_ID) > -1) {
                    video_id = clean(lineSub[i + 1]);
                }
                if (s.indexOf(T) > -1) {
                    t = clean(lineSub[i + 1]);
                }
            }
            String link = "http://" + host + "/" + PLAYER + "?" + VIDEO_ID + "=" + video_id + "&" + "t=" + t;

            /* Konvertierungsmöglichkeiten adden */
            if (HTTP.getRequestWithoutHtmlCode(new URL(link + "&fmt=18"), null, null, true).getResponseCode() == 200) {
                possibleconverts.add(ConversionMode.VIDEOMP4);
            }
            if (HTTP.getRequestWithoutHtmlCode(new URL(link + "&fmt=13"), null, null, true).getResponseCode() == 200) {
                possibleconverts.add(ConversionMode.VIDEO3GP);
            }
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.VIDEOFLV);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);

            ConversionMode ConvertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray());
            if (ConvertTo != null) {
                if (ConvertTo == ConvertDialog.ConversionMode.VIDEOMP4) {
                    link += "&fmt=18";
                } else if (ConvertTo == ConvertDialog.ConversionMode.VIDEO3GP) {
                    link += "&fmt=13";
                }
                String name = new Regex(reqinfo.getHtmlCode(), YT_FILENAME).getFirstMatch().trim();
                DownloadLink thislink = createDownloadlink(link);
                thislink.setBrowserUrl(parameter);
                thislink.setStaticFileName(name + ".tmp");
                thislink.setSourcePluginComment("Convert to " + ConvertTo.GetText());
                thislink.setProperty("convertto", ConvertTo.name());
                decryptedLinks.add(thislink);
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