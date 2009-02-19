package jd.plugins.decrypt;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;

public class JamendoCom extends PluginForDecrypt {

    private static String ENABLE_SUBFOLDERS = "ENABLE_SUBFOLDERS";

    public JamendoCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        if (parameter.toString().contains("/album")) {
            /* Einzelnes Album */
            String AlbumID = new Regex(parameter.toString(), "album/(\\d+)").getMatch(0);
            br.getPage("http://www.jamendo.com/en/album/" + AlbumID);
            String Album = br.getRegex("<div class='page_title' style=''>(.*?)</div>").getMatch(0);
            String Artist = br.getRegex("<div class='page_title' style=''>.*?</div>.*?class=\"g_artist_name\" title=\"\" >(.*?)</a").getMatch(0);
            String Tracks[][] = br.getRegex("<a href=\"/en/track/(\\d+)\" title=\"\" >(.*?)</a>").getMatches();
            FilePackage fp = new FilePackage();
            fp.setName(Artist);
            for (String Track[] : Tracks) {
                DownloadLink link = createDownloadlink("http://www.jamendo.com/en/track/" + Track[0]);
                link.setName(Track[1]);
                link.setAvailable(true);
                link.setFilePackage(fp);
                if (getPluginConfig().getBooleanProperty(ENABLE_SUBFOLDERS)) {
                    link.addSubdirectory(Album);
                }
                decryptedLinks.add(link);
            }
        } else {
            /* Übersicht der Alben */
            String ArtistID = new Regex(parameter.toString(), "artist/(.+)").getMatch(0);
            br.getPage("http://www.jamendo.com/en/artist/" + ArtistID);
            String Albums[] = br.getRegex("<a href=\"/en/album/(\\d+)\"").getColumn(0);
            for (String Album : Albums) {
                DownloadLink link = createDownloadlink("http://www.jamendo.com/en/album/" + Album);
                decryptedLinks.add(link);
            }
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    private void setConfigElements() {
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_SUBFOLDERS, JDLocale.L("plugins.decrypt.jamendo", "Create a subfolder for each album")).setDefaultValue(false));
    }

}
