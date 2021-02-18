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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "jamendo.com" }, urls = { "https?://[\\w\\.\\-]*?jamendo\\.com(?:/[a-z]{2})?/(album/\\d+|artist/.+|list/a\\d+)" })
public class JamendoCom extends PluginForDecrypt {
    public JamendoCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_ALBUM = ".*/(album/\\d+|list/a\\d+)";

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setLoadLimit(4194304);
        String url = parameter.toString();
        final SubConfiguration cfg = SubConfiguration.getConfig("jamendo.com");
        if (url.matches(TYPE_ALBUM)) {
            final String albumID = new Regex(url, "(\\d+)$").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage("https://www." + this.getHost() + "/album/" + albumID);
            if (!url.contains("/album") && !url.contains("list/a")) {
                decryptedLinks.add(this.createOfflinelink(url));
                return decryptedLinks;
            }
            String album = br.getRegex("og:title\" content=\"(.*?)\"").getMatch(0);
            String artist = br.getRegex("og:description\" content=\"Album by (.*?)\"").getMatch(0);
            String tracks[] = br.getRegex("\\{[^\\}]+position[^\\}]+id\\&quot;:(\\d+),\\&quot;").getColumn(0);
            FilePackage fp = FilePackage.getInstance();
            String packageName = "";
            if (album != null) {
                packageName = packageName + album;
            }
            if (artist != null) {
                if (packageName.length() > 0) {
                    packageName = " - " + packageName;
                }
                packageName = artist + packageName;
            }
            fp.setName(packageName);
            /* 2021-02-18: Not supported anymore */
            final boolean allowAlbumZipDownload = false;
            if (cfg.getBooleanProperty("PREFER_WHOLEALBUM", false) && allowAlbumZipDownload) {
                DownloadLink link = createDownloadlink("http://storage-new.newjamendo.com/download/a/" + albumID);
                link.setName(packageName);
                link.setAvailable(true);
                fp.add(link);
                decryptedLinks.add(link);
            } else {
                for (final String trackID : tracks) {
                    final DownloadLink link = createDownloadlink("https://www.jamendo.com/track/" + trackID);
                    link.setName(trackID + ".mp3");
                    link.setAvailable(true);
                    fp.add(link);
                    decryptedLinks.add(link);
                }
            }
        } else {
            String artistID = new Regex(parameter.toString(), "artist/(.+)").getMatch(0);
            br.setFollowRedirects(true);
            br.getPage("https://www.jamendo.com/en/artist/" + artistID);
            String artist = br.getRegex("([^\"]+)\" property=\"og:title").getMatch(0);
            String albums[] = br.getRegex("<h2>\\s+<a href='/en/list/a(\\d+)/\\w+' >").getColumn(0);
            DownloadLink link;
            for (String album : albums) {
                if (cfg.getBooleanProperty("PREFER_WHOLEALBUM", false)) {
                    link = createDownloadlink("http://storage-new.newjamendo.com/en/download/a" + album);
                    link.setName(artist + " - " + album + ".zip");
                } else {
                    link = createDownloadlink("https://www.jamendo.com/en/album/" + album);
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