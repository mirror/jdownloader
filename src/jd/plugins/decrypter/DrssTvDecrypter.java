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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drss.tv" }, urls = { "http://(www\\.)?drss\\.tv/(sendung/\\d{2}\\-\\d{2}\\-\\d{4}|video/[a-z0-9\\-]+)/" }, flags = { 0 })
public class DrssTvDecrypter extends PluginForDecrypt {

    public DrssTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String type_video   = "http://(www\\.)?drss\\.tv/video/[a-z0-9\\-]+/";
    private static final String type_normal  = "http://(www\\.)?drss\\.tv/sendung/\\d{2}\\-\\d{2}\\-\\d{4}/";
    private static final String type_profile = "http://(www\\.)?drss\\.tv/profil/[a-z0-9\\-]+/";

    /*
     * TODO: Add support for profile links & galleries: http://www.drss.tv/profil/xxx/ , Add plugin settings, download trailer/pictures and
     * other things also, based on user settings.
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        final DownloadLink main = createDownloadlink(parameter.replace("drss.tv/", "drssdecrypted.tv/"));
        br.getPage(parameter);
        if (br.getHttpConnection().getResponseCode() == 404) {
            main.setFinalFileName("drss Sendung vom " + new Regex(parameter, "endung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0) + ".mp4");
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        String fpName = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        String externID = null;
        final String all_videos[] = br.getRegex("data\\-src=\"(https?://(www\\.)?(youtube|dailymotion)\\.com/[^<>\"]*?)\"").getColumn(0);
        if (all_videos == null || all_videos.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (parameter.matches(type_video)) {
            /* This is just a single video. */
            decryptedLinks.add(createDownloadlink(all_videos[0]));
        } else if (parameter.matches(type_normal)) {
            /* Check whether we can get the whole episode or only the trailer(s) and or pre-recorded video(s) and/or picture gallery. */
            if (br.containsHTML("class=\"descr\">Sendung \\- Part \\d+<")) {
                /* Some older videos were split in multiple YouTube video parts... */
                final String[] infos = br.getRegex("class=\"descr\">([^<>\"]*?)<").getColumn(0);
                /* Only get the parts of the episode by default - leave out e.g. bonus material. */
                int counter = 0;
                for (final String info : infos) {
                    if (counter > all_videos.length - 1) {
                        /* Small fail safe. */
                        break;
                    }
                    if (info.matches("Sendung \\- Part \\d+") || info.contains("Youtube Sendung")) {
                        decryptedLinks.add(createDownloadlink(all_videos[counter]));
                    }
                    counter++;
                }
            } else if (br.containsHTML(">Komplette Sendung</h4>")) {
                externID = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<iframe src=\"(https?://player\\.vimeo\\.com/video/\\d+)\"").getMatch(0);
                if (externID != null) {
                    externID = externID + "&forced_referer=" + Encoding.Base64Encode(this.br.getURL());
                    decryptedLinks.add(createDownloadlink(externID));
                    return decryptedLinks;
                }
                /* Now let's assume that the video is hosted on drss.tv and return the link to the host plugin. */
                try {
                    main.setContentUrl(parameter);
                } catch (final Throwable e) {
                    /* Not available ind old 0.9.581 Stable */
                    main.setBrowserUrl(parameter);
                }
                decryptedLinks.add(main);
            } else {
                /* This is most likely just a trailer. */
                decryptedLinks.add(createDownloadlink(all_videos[0]));
            }
        } else {
            /* Profiles */
        }

        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }
}
