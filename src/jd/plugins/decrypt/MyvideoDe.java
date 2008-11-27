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

import java.util.ArrayList;
import java.util.Vector;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.ConvertDialog;
import jd.gui.skins.simple.ConvertDialog.ConversionMode;
import jd.http.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class MyvideoDe extends PluginForDecrypt {

    public MyvideoDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        Vector<ConversionMode> possibleconverts = new Vector<ConversionMode>();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(true);
        br.getPage(parameter);

        // String server =
        // br.getRegex(Pattern.compile("p\\.addVariable\\('SERVER','(.*?)'\\)",
        // Pattern.CASE_INSENSITIVE)).getMatch(0);
        String videoid = br.getRegex(Pattern.compile("p\\.addVariable\\('_videoid','(.*?)'\\)", Pattern.CASE_INSENSITIVE)).getMatch(0);
        String serverpath = br.getRegex(Pattern.compile("<link rel='image_src'.*?href='(.*?)thumbs/.*?'.*?/><link", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (videoid == null || serverpath == null) return null;
        String link = serverpath + videoid + ".flv";
        String name = Encoding.htmlDecode(br.getRegex(Pattern.compile("<td class='globalHd'>(.*?)</td>", Pattern.CASE_INSENSITIVE)).getMatch(0).trim());
        possibleconverts.add(ConversionMode.AUDIOMP3);
        possibleconverts.add(ConversionMode.VIDEOFLV);
        possibleconverts.add(ConversionMode.AUDIOMP3_AND_VIDEOFLV);

        ConversionMode ConvertTo = ConvertDialog.DisplayDialog(possibleconverts.toArray(), name);
        if (ConvertTo == null) return decryptedLinks;
        DownloadLink thislink = createDownloadlink(link);
        thislink.setBrowserUrl(parameter);
        thislink.setFinalFileName(name + ".tmp");
        thislink.setSourcePluginComment("Convert to " + ConvertTo.GetText());
        thislink.setProperty("convertto", ConvertTo.name());
        decryptedLinks.add(thislink);

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}