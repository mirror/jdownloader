//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
//along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mikseri.net" }, urls = { "https?://(?:www\\.)?mikseri\\.net/(artists/[^/]+/[^/]+/\\d+/|artists/\\?id=\\d+|artists/[^\"\\']+\\.\\d+)" })
public class MikSeriNet extends PluginForDecrypt {
    public MikSeriNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        String contenturl = param.getCryptedUrl();
        String anID = new Regex(contenturl, "mikseri\\.net/artists/[^\"\\']+\\.(\\d+)").getMatch(0);
        if (anID != null) {
            contenturl = "https://www." + getHost() + "/artists/?id=" + anID;
        }
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getURL().contains("/search.php") || br.containsHTML("class=\"error\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (contenturl.matches(".*?mikseri.net/artists/\\?id=.*?")) {
            if (br.containsHTML("Artistilla ei valitettavasti toistaiseksi ole kappaleita Mikseri\\.netissä")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String fpName = br.getRegex("<meta name=\"og:title\" content=\"(.*?)\"").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
                if (fpName == null) {
                    fpName = br.getRegex("<title>(.*?)</title>").getMatch(0);
                }
            }
            String[] fileIDs = br.getRegex("id=\"sharelinks_(\\d+)\"").getColumn(0);
            if (fileIDs == null || fileIDs.length == 0) {
                fileIDs = br.getRegex("type=\"hidden\" name=\"song_id\" value=\"(\\d+)\"").getColumn(0);
                if (fileIDs == null || fileIDs.length == 0) {
                    fileIDs = br.getRegex("id=\"player\\-(\\d+)\"").getColumn(0);
                    if (fileIDs == null || fileIDs.length == 0) {
                        fileIDs = br.getRegex("/music/play\\.php\\?id=(\\d+)").getColumn(0);
                        if (fileIDs == null || fileIDs.length == 0) {
                            fileIDs = br.getRegex("displaySomething\\(\\'sharelinks_(\\d+)\\'\\)").getColumn(0);
                        }
                    }
                }
            }
            if (fileIDs == null || fileIDs.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            progress.setRange(fileIDs.length);
            for (String id : fileIDs) {
                final DownloadLink song = getSingleLink(id);
                ret.add(song);
                distribute(song);
                progress.increase(1);
                if (this.isAbort()) {
                    throw new InterruptedException();
                }
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        } else {
            final DownloadLink song = getSingleLink(new Regex(contenturl, "/(\\d+)/$").getMatch(0));
            ret.add(song);
        }
        return ret;
    }

    private DownloadLink getSingleLink(String iD) throws IOException, PluginException {
        br.getPage("https://www." + getHost() + "/player/songlist.php?newsession=1&type=1&parameter=" + iD);
        if (br.containsHTML("<Error>No music to play\\!</Error>")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // if (br.getHttpConnection().getResponseCode() == 404) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        String finallink = br.getRegex("<SongUrl>([^<>\"]*?)</SongUrl>").getMatch(0);
        if (finallink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!finallink.startsWith("http")) {
            finallink = "http:" + finallink;
        }
        final DownloadLink song = createDownloadlink(DirectHTTP.createURLForThisPlugin(finallink));
        final String songName = br.getRegex("<SongName><\\!\\[CDATA\\[([^<>\"]*?)\\]\\]>").getMatch(0);
        final String songId = br.getRegex("<SongId>([^<>\"]*?)</SongId>").getMatch(0);
        final String minutesStr = br.getRegex("<Minutes>(\\d+)</Minutes>").getMatch(0);
        final String secondsStr = br.getRegex("<Minutes>(\\d+)</Minutes>").getMatch(0);
        final String bitrateStr = br.getRegex("<BitRate>(\\d+)</BitRate>").getMatch(0);
        if (songName != null && songId != null) {
            song.setFinalFileName(songId + "." + songName + ".mp3");
        }
        if (minutesStr != null && secondsStr != null && bitrateStr != null) {
            /* Set estimated filesize */
            final int seconds = (Integer.parseInt(minutesStr) * 60) + Integer.parseInt(secondsStr);
            song.setDownloadSize((seconds * Integer.parseInt(bitrateStr) * 1024) / 8);
        }
        song.setAvailable(true);
        return song;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}