//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jamendo.com" }, urls = { "http://[\\w\\.]*?jamendo\\.com/.?.?/?(album/\\d+|artist/.+)" }, flags = { 0 })
public class MndCm extends PluginForDecrypt {

    private static String ENABLE_SUBFOLDERS = "ENABLE_SUBFOLDERS";
    private static String PREFER_WHOLEALBUM = "PREFER_WHOLEALBUM";

    public MndCm(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (parameter.toString().contains("/album")) {
            /* Einzelnes Album */
            String AlbumID = new Regex(parameter.toString(), "album/(\\d+)").getMatch(0);
            br.getPage("http://www.jamendo.com/en/album/" + AlbumID);
            String Album = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            String Tracks[][] = br.getRegex("<a href=\"/en/track/(\\d+)\" title=\"\" >(.*?)</a>").getMatches();
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Artist);
            for (String Track[] : Tracks) {
                DownloadLink link = createDownloadlink("http://www.jamendo.com/en/track/" + Track[0]);
                link.setName(Track[1]);
                link.setAvailable(true);
                fp.add(link);
                if (getPluginConfig().getBooleanProperty(ENABLE_SUBFOLDERS)) {
                    link.addSubdirectory(Album);
                }
                decryptedLinks.add(link);
            }
        } else {
            /* Übersicht der Alben */
            String ArtistID = new Regex(parameter.toString(), "artist/(.+)").getMatch(0);
            br.getPage("http://www.jamendo.com/en/artist/" + ArtistID);
            String Albums[] = br.getRegex("href=\"/en/album/(\\d+)/share\"").getColumn(0);
            DownloadLink link;
            for (String Album : Albums) {
                if (getPluginConfig().getBooleanProperty(PREFER_WHOLEALBUM, true)) {
                    link = createDownloadlink("http://www.jamendo.com/en/download/album/" + Album);
                } else
                    link = createDownloadlink("http://www.jamendo.com/en/album/" + Album);
                decryptedLinks.add(link);
            }
        }
        return decryptedLinks;
    }

    // @Override

    /**
     * TODO: Umbauen!
     */
    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_SUBFOLDERS, JDL.L("plugins.decrypt.jamendo", "Create a subfolder for each album")).setDefaultValue(false));
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), PREFER_WHOLEALBUM, JDL.L("plugins.decrypt.jamendoalbum", "Prefer whole Album as Zip")).setDefaultValue(true));
    }

}
