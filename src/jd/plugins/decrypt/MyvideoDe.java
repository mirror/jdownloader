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

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.PluginForDecrypt;
import jd.plugins.RequestInfo;

public class MyvideoDe extends PluginForDecrypt {

    static private String host = "myvideo.de";
    static private final Pattern FILENAME = Pattern.compile("GetThis\\('(.*?)',", Pattern.CASE_INSENSITIVE);
    static public final Pattern DOWNLOADURL = Pattern.compile("SWFObject\\('http://myvideo.*?/player/.*?swf\\?(http://[\\w\\.\\-0-9]*//*.*?flv)&amp;ID=[0-9]+', 'video_player_swf'", Pattern.CASE_INSENSITIVE);
    private Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?myvideo\\.de/watch/[0-9]+/", Pattern.CASE_INSENSITIVE);

    public MyvideoDe() {
        super();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(String parameter) throws Exception {
        Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            URL url = new URL(parameter);
            RequestInfo reqinfo = HTTP.getRequest(url, null, null, true);

            String link = new Regex(reqinfo.getHtmlCode(), DOWNLOADURL).getMatch(0);
            String name = Encoding.UTF8Decode(new Regex(reqinfo.getHtmlCode(), FILENAME).getMatch(0).trim());
            possibleconverts.add(ConversionMode.AUDIOMP3);
            possibleconverts.add(ConversionMode.VIDEOFLV);
            possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);

            ConversionMode ConvertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray(), name);

            DownloadLink thislink = createDownloadlink(link);
            thislink.setBrowserUrl(parameter);
            thislink.setStaticFileName(name + ".tmp");
            thislink.setSourcePluginComment("Convert to " + ConvertTo.GetText());
            thislink.setProperty("convertto", ConvertTo.name());
            decryptedLinks.add(thislink);
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