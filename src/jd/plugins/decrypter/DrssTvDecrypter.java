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

import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drss.tv" }, urls = { "http://(www\\.)?drss\\.tv/(sendung/\\d{2}\\-\\d{2}\\-\\d{4}|video/[a-z0-9\\-]+|profil/[a-z0-9\\-]+|[a-z0-9\\-]+)/" }, flags = { 0 })
public class DrssTvDecrypter extends PluginForDecrypt {

    public DrssTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    private static final String     ALLOW_TRAILER       = "ALLOW_TRAILER";
    private static final String     ALLOW_TEASER_PIC    = "ALLOW_TEASER_PIC";
    private static final String     ALLOW_GALLERY       = "ALLOW_GALLERY";
    private static final String     ALLOW_OTHERS        = "ALLOW_OTHERS";

    private static final String     invalidlinks        = "http://(www\\.)?drss\\.tv/(gaeste/?|sendungen/?|team/?|club/?|impressum/?|feedback/?|index/?)";
    private static final String     type_video          = "http://(www\\.)?drss\\.tv/video/[a-z0-9\\-]+/";
    private static final String     type_normal_episode = "http://(www\\.)?drss\\.tv/sendung/\\d{2}\\-\\d{2}\\-\\d{4}/";
    private static final String     type_profile        = "http://(www\\.)?drss\\.tv/profil/[a-z0-9\\-]+/";

    private DownloadLink            MAIN                = null;
    private String                  PARAMETER           = null;
    private ArrayList<DownloadLink> DECRYPTEDLINKS      = new ArrayList<DownloadLink>();
    private String[]                ALLVIDEOS           = null;
    private String                  TITLE               = null;

    /*
     * TODO: Add support for profile links & galleries: http://www.drss.tv/profil/xxx/ , Add plugin settings, download trailer/pictures and
     * other things also, based on user settings.
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        PARAMETER = param.toString();
        if (PARAMETER.matches(invalidlinks)) {
            MAIN = createDownloadlink("directhttp://" + PARAMETER);
            MAIN.setFinalFileName(new Regex(PARAMETER, "drss\\.tv/(.+)/?").getMatch(0));
            MAIN.setAvailable(false);
            MAIN.setProperty("offline", true);
            DECRYPTEDLINKS.add(MAIN);
            return DECRYPTEDLINKS;
        }
        MAIN = createDownloadlink(PARAMETER.replace("drss.tv/", "drssdecrypted.tv/"));
        try {
            MAIN.setContentUrl(PARAMETER);
        } catch (final Throwable e) {
            /* Not available ind old 0.9.581 Stable */
            MAIN.setBrowserUrl(PARAMETER);
        }
        br.getPage(PARAMETER);
        if (br.getHttpConnection().getResponseCode() == 404) {
            MAIN.setFinalFileName("drss Sendung vom " + new Regex(PARAMETER, "endung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0) + ".mp4");
            MAIN.setAvailable(false);
            MAIN.setProperty("offline", true);
            DECRYPTEDLINKS.add(MAIN);
            return DECRYPTEDLINKS;
        }
        ALLVIDEOS = br.getRegex("data\\-src=\"(https?://(www\\.)?(youtube|dailymotion)\\.com/[^<>\"]*?)\"").getColumn(0);
        if (PARAMETER.matches(type_video)) {
            /* This is just a single video. */
            if (ALLVIDEOS == null || ALLVIDEOS.length == 0) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            DECRYPTEDLINKS.add(createDownloadlink(ALLVIDEOS[0]));
        } else if (PARAMETER.matches(type_normal_episode)) {
            decryptEpisode();
        } else if (PARAMETER.matches(type_profile)) {
            /* Profiles */
            decryptProfile();
        } else {
            /* Handles all undefined linktypes - should be videolinks though. */
            decryptOther();
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(TITLE);
        fp.addLinks(DECRYPTEDLINKS);

        return DECRYPTEDLINKS;
    }

    @SuppressWarnings("deprecation")
    private void decryptEpisode() throws DecrypterException {
        final SubConfiguration cfg = SubConfiguration.getConfig("drss.tv");
        final boolean allow_gallery = cfg.getBooleanProperty(ALLOW_GALLERY, false);

        TITLE = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (TITLE == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        String date = br.getRegex("class=\"subline\">Sendung vom (\\d{2}\\.\\d{2}\\.\\d{4})</span>").getMatch(0);
        if (date == null) {
            /* Only get date from url when site fails as url date is sometimes wrong/not accurate! */
            date = new Regex(PARAMETER, "sendung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0).replace("-", ".");
        }
        TITLE = date + "_" + Encoding.htmlDecode(TITLE).trim();
        String externID = null;
        /* Check whether we can get the whole episode or only the trailer(s) and or pre-recorded video(s) and/or picture gallery. */
        if (br.containsHTML("class=\"descr\">Sendung \\- Part \\d+<|>Komplette Sendung</h4>")) {
            /* We have a full single episode - let's check what type it is... */
            if (br.containsHTML("class=\"descr\">Sendung \\- Part \\d+<")) {
                if (ALLVIDEOS == null || ALLVIDEOS.length == 0) {
                    logger.warning("Decrypter broken for link: " + PARAMETER);
                    throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
                }
                /* Full episode split in multiple YouTube parts. */
                final String[] infos = br.getRegex("class=\"descr\">([^<>\"]*?)<").getColumn(0);
                /* Only get the parts of the episode by default - leave out e.g. bonus material. */
                int counter = 0;
                for (final String info : infos) {
                    if (counter > ALLVIDEOS.length - 1) {
                        /* Small fail safe. */
                        break;
                    }
                    if (info.matches("Sendung \\- Part \\d+") || info.contains("Youtube Sendung")) {
                        DECRYPTEDLINKS.add(createDownloadlink(ALLVIDEOS[counter]));
                    }
                    counter++;
                }
            } else {
                /* Full episode either in one part hosted by drss or extern. */
                externID = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<iframe src=\"(https?://player\\.vimeo\\.com/video/\\d+)\"").getMatch(0);
                if (externID == null) {
                    /* New vimeo embed */
                    externID = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<div[^>]+data\\-url=\"(http://player\\.vimeo.com/external/\\d+)").getMatch(0);
                }
                /* Add special quality if available */
                final String specialVimeoFULL_HDLink = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<div[^>]+data\\-url=\"(http://player\\.vimeo.com/external/\\d+\\.hd\\.mp4\\?s=[a-z0-9]+)\"").getMatch(0);
                if (specialVimeoFULL_HDLink != null) {
                    /*
                     * In theory this is a bad workaround but it's the best solution here as we do not easily get a http link to this
                     * quality via the vimeo "API" so it's easier to just grab this one as it's plain in the html code and download it via
                     * hostplugin.
                     */
                    MAIN.setProperty("special_vimeo", true);
                    DECRYPTEDLINKS.add(MAIN);
                } else if (externID != null) {
                    externID = externID + "&forced_referer=" + Encoding.Base64Encode(this.br.getURL());
                    DECRYPTEDLINKS.add(createDownloadlink(externID));
                } else {
                    /* Now let's assume that the video is hosted on drss.tv and return the link to the host plugin. */
                    DECRYPTEDLINKS.add(MAIN);
                }
            }
            decryptVideolistStuff(false);
        } else {
            if (ALLVIDEOS == null || ALLVIDEOS.length == 0) {
                logger.warning("Decrypter broken for link: " + PARAMETER);
                throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
            }
            /* This is most likely just a trailer. */
            DECRYPTEDLINKS.add(createDownloadlink(ALLVIDEOS[0]));
        }
        /* Check if the user also wants to have the photo gallery... */
        final String[] pics = br.getRegex("href=\"(/images/data/edition/\\d+/[a-z0-9\\-]+\\.jpg)\"").getColumn(0);
        if (allow_gallery && pics != null) {
            final DecimalFormat df = new DecimalFormat("00");
            int counter = 1;
            for (String piclink : pics) {
                piclink = "directhttp://http://www.drss.tv" + piclink;
                final DownloadLink pic = createDownloadlink(piclink);
                pic.setFinalFileName(TITLE + "_" + df.format(counter) + ".jpg");
                pic.setAvailable(true);
                DECRYPTEDLINKS.add(pic);
                counter++;
            }
        }
    }

    private void decryptProfile() throws DecrypterException {
        TITLE = br.getRegex("class=\"profile\\-title\\-block\" style=\"margin:0px -15px\">([^<>\"]*?)<").getMatch(0);
        final String[] episodes = br.getRegex("\"(/sendung/[0-9\\-]+/)\"").getColumn(0);
        if (episodes == null || episodes.length == 0 || TITLE == null) {
            /* Every profile has at least participated in one episode and of course title should exist! */
            logger.warning("Failed to decrypt profile");
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        for (String episodelink : episodes) {
            episodelink = "http://www.drss.tv" + episodelink;
            DECRYPTEDLINKS.add(createDownloadlink(episodelink));
        }
        final String[] pics = br.getRegex("href=\"(/images/data/girls/\\d+/[a-z0-9\\-_]+\\.jpg)\"").getColumn(0);
        if (pics != null) {
            final DecimalFormat df = new DecimalFormat("00");
            int counter = 1;
            for (String piclink : pics) {
                piclink = "directhttp://http://www.drss.tv" + piclink;
                final DownloadLink pic = createDownloadlink(piclink);
                pic.setFinalFileName(TITLE + "_" + df.format(counter) + ".jpg");
                pic.setAvailable(true);
                DECRYPTEDLINKS.add(pic);
                counter++;
            }
        }
    }

    private void decryptOther() throws DecrypterException {
        TITLE = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (TITLE == null) {
            logger.warning("Decrypter broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        decryptVideolistStuff(true);
    }

    /**
     * Decrypts all the things in the videolist on the right side of the page.
     *
     * @throws DecrypterException
     * @param force_others
     *            : Force the decryption of uncategorized (video) material. If true, this overrides the users' setting. Only important for
     *            uncategorized linktypes.
     */
    @SuppressWarnings("deprecation")
    private void decryptVideolistStuff(final boolean force_others) throws DecrypterException {
        final SubConfiguration cfg = SubConfiguration.getConfig("drss.tv");
        final boolean allow_teaser_pic = cfg.getBooleanProperty(ALLOW_TEASER_PIC, false);
        final boolean allow_trailer = cfg.getBooleanProperty(ALLOW_TRAILER, false);
        boolean allow_others = cfg.getBooleanProperty(ALLOW_OTHERS, false);
        if (force_others) {
            allow_others = true;
        }
        final String[] bigtitles = br.getRegex("<h4 class=\"text\\-cutted\">([^<>\"]*?)</h4>").getColumn(0);
        final String[] subtitles = br.getRegex("<p class=\"descr\">([^<>\"]*?)</p>").getColumn(0);
        if (subtitles == null || subtitles.length == 0 || bigtitles == null || bigtitles.length == 0) {
            logger.warning("Decrypter method decryptVideolistStuff broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }

        final int bigtitles_length = bigtitles.length;
        final int subtitles_length = subtitles.length;
        if (bigtitles_length != subtitles_length) {
            logger.warning("Decrypter method decryptVideolistStuff broken for link: " + PARAMETER);
            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
        }
        int real_counter = 1;
        for (int counter = 0; counter <= bigtitles_length - 1; counter++) {
            final String bigtitle = bigtitles[counter];
            final String subtitle = subtitles[counter];
            try {
                /* First some errorhandling. */
                if (bigtitle.equals("Komplette Sendung")) {
                    continue;
                }
                if (subtitle.equals("Titelbild")) {
                    if (allow_teaser_pic) {
                        String teaser_picture = br.getRegex("property=\"og:image\" content=\"(/images/[^<>\"]*?\\.jpg)\"").getMatch(0);
                        if (teaser_picture == null) {
                            teaser_picture = br.getRegex("player\\-" + real_counter + "\">[\t\n\r ]+<img height=\"\\d+\" class=\"img\\-responsive\" src=\"(/images/[^<>\"]*?\\.jpg)\"").getMatch(0);
                        }
                        if (teaser_picture == null) {
                            logger.warning("Failed to find teaser picture!");
                            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
                        }
                        teaser_picture = "directhttp://http://www.drss.tv" + teaser_picture;
                        final DownloadLink pic = createDownloadlink(teaser_picture);
                        pic.setFinalFileName(TITLE + "_Titelbild_.jpg");
                        pic.setAvailable(true);
                        DECRYPTEDLINKS.add(pic);
                    }
                } else if (subtitle.contains("Trailer")) {
                    /*
                     * Trailer exists and user wants to download it --> Find it. Trailers are usually hosted externally.
                     */
                    if (allow_trailer) {
                        final String trailer_externlink = br.getRegex("player\\-" + real_counter + "\">[\t\n\r ]+<iframe[^<>]+(?:data\\-)?src=\"(http[^<>\"]*?)\"").getMatch(0);
                        if (trailer_externlink == null) {
                            logger.warning("Failed to find trailer!");
                            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
                        }
                        DECRYPTEDLINKS.add(createDownloadlink(trailer_externlink));
                    }
                } else {
                    /*
                     * Now we should have a video which is neither trailer nor any other undefined video type.
                     */
                    if (allow_others) {
                        final String video_externlink = br.getRegex("player\\-" + real_counter + "\">[\t\n\r ]+<iframe[^<>]+(?:data\\-)?src=\"(http[^<>\"]*?)\"").getMatch(0);
                        if (video_externlink == null) {
                            logger.warning("Failed to find trailer!");
                            throw new DecrypterException("Decrypter broken for link: " + PARAMETER);
                        }
                        DECRYPTEDLINKS.add(createDownloadlink(video_externlink));
                        logger.info("Decrypted unknown bigtitle: " + bigtitle);
                        logger.info("Decrypted unknown subtitle: " + subtitle);
                    }
                }
            } finally {
                real_counter++;
            }
        }
    }

}
