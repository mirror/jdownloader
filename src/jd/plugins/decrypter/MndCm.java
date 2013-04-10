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
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jamendo.com" }, urls = { "http://[\\w\\.\\-]*?jamendo\\.com/.?.?/?(album/\\d+|artist/.+|list/a\\d+)" }, flags = { 0 })
public class MndCm extends PluginForDecrypt {

    public MndCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String url = parameter.toString();
        SubConfiguration cfg = SubConfiguration.getConfig("jamendo.com");
        if (url.contains("/album") || url.contains("list/a")) {
            String AlbumID = new Regex(url, "list/a(\\d+)").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage("http://www.jamendo.com/en/album/" + AlbumID);
            String Album = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String Artist = br.getRegex("og:description\" content=\"Album by (.*?)\"").getMatch(0);
            String Tracks[][] = br.getRegex("<a href='/en/track/(\\d+).*?' >(.*?)</").getMatches();
            FilePackage fp = FilePackage.getInstance();
            String packageName = "";
            if (Album != null) packageName = packageName + Album;
            if (Artist != null) {
                if (packageName.length() > 0) {
                    packageName = " - " + packageName;
                }
                packageName = Artist + packageName;
            }
            fp.setName(packageName);
            if (cfg.getBooleanProperty("PREFER_WHOLEALBUM", true)) {
                DownloadLink link = createDownloadlink("http://storage-new.newjamendo.com/en/download/a" + AlbumID);
                link.setName(packageName);
                link.setAvailable(true);
                fp.add(link);
                decryptedLinks.add(link);
            } else {
                for (String Track[] : Tracks) {
                    DownloadLink link = createDownloadlink("http://www.jamendo.com/en/track/" + Track[0]);
                    link.setName(Artist + " - " + Album + " - " + Track[1] + ".mp3");
                    link.setAvailable(true);
                    fp.add(link);
                    decryptedLinks.add(link);
                }
            }
        } else {
            String ArtistID = new Regex(parameter.toString(), "artist/(.+)").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage("http://www.jamendo.com/en/artist/" + ArtistID);
            String Artist = br.getRegex("([^\"]+)\" property=\"og:title").getMatch(0);
            String Albums[] = br.getRegex("<h2>\\s+<a href='/en/list/a(\\d+)/\\w+' >").getColumn(0);
            DownloadLink link;
            for (String Album : Albums) {
                if (cfg.getBooleanProperty("PREFER_WHOLEALBUM", true)) {
                    link = createDownloadlink("http://storage-new.newjamendo.com/en/download/a" + Album);
                    link.setName(Artist + " - " + Album + ".zip");
                } else {
                    link = createDownloadlink("http://www.jamendo.com/en/album/" + Album);
                }
                decryptedLinks.add(link);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}