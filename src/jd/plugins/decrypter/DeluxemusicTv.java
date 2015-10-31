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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "deluxemusic.tv" }, urls = { "https?://(?:www\\.)?deluxemusic\\.tv/.+" }, flags = { 0 })
public class DeluxemusicTv extends PluginForDecrypt {

    public DeluxemusicTv(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = null;
        final String[] playlists = br.getRegex("playlist_id=(\\d+)\"").getColumn(0);
        if (playlists == null || playlists.length == 0) {
            /* No content to crawl --> Do NOT add offline URLs in this case! */
            logger.info("Found no downloadable content in URL: " + parameter);
            return decryptedLinks;
        }
        for (final String playlist_id : playlists) {
            if (this.isAbort()) {
                logger.info("Decryption aborted by user");
                return decryptedLinks;
            }
            final String playlist_url = "http://deluxemusic.tv.cms.ipercast.net/?playlist_id=" + playlist_id + "&xmldata=true";
            this.br.getPage(playlist_url);
            if (jd.plugins.hoster.DeluxemusicTv.isOffline(this.br)) {
                logger.info("Skip offline playlist: " + playlist_id);
                continue;
            }
            fpName = getXML("info");
            if (fpName == null) {
                /* This should never happen */
                fpName = playlist_id;
            }
            fpName = Encoding.htmlDecode(fpName).trim();
            int counter = 0;
            final String[] track_array = getTrackArray(this.br);
            for (final String track_xml : track_array) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                final DownloadLink dl = createDownloadlink("http://deluxemusic.tvdecrypted/" + playlist_id + "_" + counter);
                dl.setAvailableStatus(jd.plugins.hoster.DeluxemusicTv.parseTrackInfo(dl, this.br.toString(), track_array));
                dl.setProperty("playlist_url", playlist_url);
                dl.setProperty("playlist_id", playlist_id);
                dl.setContentUrl(parameter);
                dl._setFilePackage(fp);
                decryptedLinks.add(dl);
                distribute(dl);
                counter++;
            }
        }

        return decryptedLinks;
    }

    public static final String[] getTrackArray(final Browser br) {
        return br.getRegex("<track>(.*?)</track>").getColumn(0);
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    public static String getXML(final String source, final String parameter) {
        String result = new Regex(source, "<" + parameter + "( type=\"[^<>\"/]*?\")?>([^<>]*?)</" + parameter + ">").getMatch(1);
        if (result == null) {
            result = new Regex(source, "<" + parameter + "><\\!\\[CDATA\\[([^<>]*?)\\]\\]></" + parameter + ">").getMatch(0);
        }
        return result;
    }

}
