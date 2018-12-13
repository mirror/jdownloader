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

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mio.to" }, urls = { "https?://(?:www\\.)?mio\\.to/album/.+" })
public class MioTo extends PluginForDecrypt {
    public MioTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(this.createOfflinelink(parameter));
            return decryptedLinks;
        }
        // final Regex urlInfo = new Regex(parameter, "/album/([^/]+)/([^/]+)");
        // final String name_artist = urlInfo.getMatch(0);
        // final String name_album = urlInfo.getMatch(1);
        /* Thx: https://vishnudevtj.github.io/notes/mio */
        final Regex thumbnailInfo = br.getRegex("media-images.mio.to/(.*?)/([^/]+)/Art\\-\\d+\\.jpg");
        final String linkpart_1 = thumbnailInfo.getMatch(0);
        final String linkpart_2 = thumbnailInfo.getMatch(1);
        String fpName = new Regex(parameter, "/album/(.+)").getMatch(0);
        final String[] htmls = br.getRegex("<tr album_id=.*?</td></tr>").getColumn(-1);
        if (htmls == null || htmls.length == 0 || linkpart_1 == null || linkpart_1 == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String html : htmls) {
            final String album_id = new Regex(html, "album_id=\"([^\"]+)\"").getMatch(0);
            final String disc_number = new Regex(html, "disc_number=\"([^\"]+)\"").getMatch(0);
            final String track_number = new Regex(html, "track_number=\"([^\"]+)\"").getMatch(0);
            final String track_name = new Regex(html, "track_name=\"([^\"]+)\"").getMatch(0);
            final String track_name_s = new Regex(html, "track_name_s=\"([^\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(album_id) || StringUtils.isEmpty(disc_number) || StringUtils.isEmpty(track_number) || StringUtils.isEmpty(track_name)) {
                return null;
            }
            final String real_track_name;
            if (track_name_s != null) {
                real_track_name = track_name_s;
            } else {
                real_track_name = track_name;
            }
            final String final_filename = track_number + "." + track_name + album_id + " - " + real_track_name + ".mp3";
            // final String firstletter_of_album_id = album_id.substring(0, 1);
            if (track_number.equals("18")) {
                logger.info("");
            }
            String directurl = String.format("https://media-audio.mio.to/%s/%s/%s_%s%%20-%%20%s-vbr-V5.mp3", linkpart_1, linkpart_2, disc_number, track_number, URLEncode.encodeURIComponent(real_track_name));
            // if (directurl.contains("(")) {
            // directurl = directurl.replaceAll("\\(", "%28");
            // }
            // if (directurl.contains(")")) {
            // directurl = directurl.replaceAll("\\)", "%29");
            // }
            final DownloadLink dl = createDownloadlink(directurl);
            dl.setFinalFileName(final_filename);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }
}
