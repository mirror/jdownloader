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
import java.text.DecimalFormat;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.config.SubConfiguration;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "drss.tv" }, urls = { "http://(www\\.)?drss\\.tv/(sendung/\\d{2}\\-\\d{2}\\-\\d{4}|video/[a-z0-9\\-]+|profil/[a-z0-9\\-]+|[a-z0-9\\-]+)/" }, flags = { 0 })
public class DrssTvDecrypter extends PluginForDecrypt {

    @SuppressWarnings("deprecation")
    public DrssTvDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings stuff */
    private static final String     ALLOW_TRAILER         = "ALLOW_TRAILER";
    private static final String     ALLOW_TEASER_PIC      = "ALLOW_TEASER_PIC";
    private static final String     ALLOW_GALLERY         = "ALLOW_GALLERY";
    private static final String     ALLOW_OTHERS          = "ALLOW_OTHERS";

    private static final String     invalidlinks          = "http://(www\\.)?drss\\.tv/(gaeste/?|sendungen/?|team/?|club/?|impressum/?|feedback/?|index/?)";
    private static final String     type_video            = "http://(www\\.)?drss\\.tv/video/[a-z0-9\\-]+/";
    private static final String     type_normal_episode   = "http://(www\\.)?drss\\.tv/sendung/\\d{2}\\-\\d{2}\\-\\d{4}/";
    private static final String     type_profile          = "http://(www\\.)?drss\\.tv/profil/[a-z0-9\\-]+/";

    private static final String     type_directlink_vimeo = "https?://player\\.vimeo\\.com/external/\\d+\\.(?:hd|sd)\\.mp4.+";

    private String                  parameter             = null;
    private ArrayList<DownloadLink> decryptedlinks        = new ArrayList<DownloadLink>();
    private String[]                allvideos             = null;
    private String                  title                 = null;
    private String                  description           = null;

    private boolean                 force_trailer         = false;
    private boolean                 force_other_content   = false;
    private short                   counter_real          = 1;

    /*
     * TODO: Add support for profile links & galleries: http://www.drss.tv/profil/xxx/ , Add plugin settings, download trailer/pictures and
     * other things also, based on user settings.
     */
    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final SubConfiguration cfg = SubConfiguration.getConfig("drss.tv");
        final boolean allow_gallery = cfg.getBooleanProperty(ALLOW_GALLERY, false);
        parameter = param.toString();
        DownloadLink main = createDownloadlink(parameter.replace("drss.tv/", "drssdecrypted.tv/") + "?video=1");
        if (parameter.matches(invalidlinks)) {
            main = createDownloadlink("directhttp://" + parameter);
            main.setFinalFileName(new Regex(parameter, "drss\\.tv/(.+)/?").getMatch(0));
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedlinks.add(main);
            return decryptedlinks;
        }
        try {
            main.setContentUrl(parameter);
        } catch (final Throwable e) {
            /* Not available ind old 0.9.581 Stable */
            main.setBrowserUrl(parameter);
        }
        this.br.setFollowRedirects(true);
        this.br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            main.setFinalFileName("drss Sendung vom " + new Regex(parameter, "endung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0) + ".mp4");
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedlinks.add(main);
            return decryptedlinks;
        }
        description = this.br.getRegex("<div class=\"row profile-container-text margin-bottom\">(.*?)<div class=\"row\">").getMatch(0);
        allvideos = this.br.getRegex("data\\-src=\"(https?://(?:www\\.)?(?:youtube|dailymotion)\\.com/[^<>\"]*?)\"").getColumn(0);

        try {
            if (description != null) {
                main.setComment(description);
            }
        } catch (final Throwable e) {
            /* Not available in old 0.9.581 Stable */
        }

        if (parameter.matches(type_video)) {
            /* This is just a single video. */
            if (allvideos == null || allvideos.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            decryptedlinks.add(createDownloadlink(allvideos[0]));
        } else if (parameter.matches(type_profile)) {
            /* Profiles */
            decryptProfile();
        } else {
            /* Handles all other linktypes */
            decryptVideolistStuff();
        }

        /* Check if the user also wants to have the photo gallery... */
        final String[] pics = br.getRegex("href=\"(/images/data/edition/\\d+/[a-z0-9\\-]+\\.jpg)\"").getColumn(0);
        if (allow_gallery && pics != null) {
            final DecimalFormat df = new DecimalFormat("00");
            int counter = 1;
            for (String piclink : pics) {
                piclink = "directhttp://http://www.drss.tv" + piclink;
                final DownloadLink pic = createDownloadlink(piclink);
                pic.setFinalFileName(title + "_" + df.format(counter) + ".jpg");
                pic.setAvailable(true);
                decryptedlinks.add(pic);
                counter++;
            }
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(decryptedlinks);

        return decryptedlinks;
    }

    private void decryptEpisode() throws DecrypterException, IOException {
        final String url_content = this.parameter + "?video=" + this.counter_real;
        final DownloadLink main = createDownloadlink(url_content.replace("drss.tv/", "drssdecrypted.tv/"));
        main.setContentUrl(url_content);
        title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        String date = br.getRegex("class=\"subline\">Sendung vom (\\d{2}\\.\\d{2}\\.\\d{4})</span>").getMatch(0);
        if (date == null) {
            /* Only get date from url when site fails as url date is sometimes wrong/not accurate! */
            date = new Regex(parameter, "sendung/(\\d{2}\\-\\d{2}\\-\\d{4})/").getMatch(0).replace("-", ".");
        }
        title = date + "_" + Encoding.htmlDecode(title).trim();
        String externID = null;
        /* Check whether we can get the whole episode or only the trailer(s) and or pre-recorded video(s) and/or picture gallery. */
        if (br.containsHTML("class=\"descr\">Sendung \\- Part \\d+<|>Komplette Sendung</h4>")) {
            /* We have a full single episode - let's check what type it is... */
            if (br.containsHTML("class=\"descr\">Sendung \\- Part \\d+<")) {
                if (allvideos == null || allvideos.length == 0) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    throw new DecrypterException("Decrypter broken for link: " + parameter);
                }
                /* Full episode split in multiple YouTube parts. */
                final String[] infos = br.getRegex("class=\"descr\">([^<>\"]*?)<").getColumn(0);
                /* Only get the parts of the episode by default - leave out e.g. bonus material. */
                int counter = 0;
                for (final String info : infos) {
                    if (counter > allvideos.length - 1) {
                        /* Small fail safe. */
                        break;
                    }
                    if (info.matches("Sendung \\- Part \\d+") || info.contains("Youtube Sendung")) {
                        decryptedlinks.add(createDownloadlink(allvideos[counter]));
                    }
                    counter++;
                }
            } else {
                /* Full episode either in one part hosted by drss or extern. */
                externID = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<iframe src=\"(https?://player\\.vimeo\\.com/video/\\d+)\"").getMatch(0);
                if (externID == null) {
                    /* New vimeo embed */
                    externID = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<div[^>]+data\\-url=\"(https?://player\\.vimeo.com/external/\\d+)").getMatch(0);
                }
                /* Add special quality if available */
                final String specialVimeoFULL_HDLink = br.getRegex("<div class=\"player active current player\\-1\">[\t\n\r ]+<div[^>]+data\\-url=\"(https?://player\\.vimeo.com/external/\\d+\\.hd\\.mp4[^<>\"]*?)\"").getMatch(0);
                if (specialVimeoFULL_HDLink != null) {
                    /*
                     * In theory this is a bad workaround but it's the best solution here as we do not easily get a http link to this
                     * quality via the vimeo "API" so it's easier to just grab this one as it's plain in the html code and download it via
                     * hostplugin.
                     */
                    main.setProperty("special_vimeo", true);
                    decryptedlinks.add(main);
                } else if (externID != null) {
                    externID = externID + "&forced_referer=" + Encoding.Base64Encode(this.br.getURL());
                    decryptedlinks.add(createDownloadlink(externID));
                } else {
                    /* Now let's assume that the video is hosted on drss.tv and return the link to the host plugin. */
                    decryptedlinks.add(main);
                }
            }
        } else {
            if (allvideos != null && allvideos.length > 0) {
                /* This is most likely just a trailer. */
                decryptedlinks.add(createDownloadlink(allvideos[0]));
            } else {
                /*
                 * 2015-10-22
                 *
                 * Trust our code - in very very rare cases the video is officially not there but chances are high that the trailer == the
                 * full episode e.g.:
                 *
                 * http://www.drss.tv/sendung/27-08-2015/
                 *
                 * --> Force trailer download
                 */
                /* Loop continues - force trailer download */
                this.force_trailer = true;
            }
        }
    }

    private void decryptProfile() throws DecrypterException {
        title = br.getRegex("class=\"profile\\-title\\-block\" style=\"margin:0px -15px\">([^<>\"]*?)<").getMatch(0);
        final String[] episodes = br.getRegex("\"(/sendung/[0-9\\-]+/)\"").getColumn(0);
        if (episodes == null || episodes.length == 0 || title == null) {
            /* Every profile has at least participated in one episode and of course title should exist! */
            logger.warning("Failed to decrypt profile");
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (String episodelink : episodes) {
            episodelink = "http://www.drss.tv" + episodelink;
            decryptedlinks.add(createDownloadlink(episodelink));
        }
        final String[] pics = br.getRegex("href=\"(/images/data/girls/\\d+/[a-z0-9\\-_]+\\.jpg)\"").getColumn(0);
        if (pics != null) {
            final DecimalFormat df = new DecimalFormat("00");
            int counter = 1;
            for (String piclink : pics) {
                piclink = "directhttp://http://www.drss.tv" + piclink;
                final DownloadLink pic = createDownloadlink(piclink);
                pic.setFinalFileName(title + "_" + df.format(counter) + ".jpg");
                pic.setAvailable(true);
                decryptedlinks.add(pic);
                counter++;
            }
        }
    }

    /**
     * Decrypts all the things in the videolist on the right side of the page.
     *
     * @throws DecrypterException
     * @param force_other_content
     *            : Force the decryption of uncategorized (video) material. If true, this overrides the users' setting. Only important for
     *            uncategorized linktypes.
     * @throws IOException
     */
    @SuppressWarnings("deprecation")
    private void decryptVideolistStuff() throws DecrypterException, IOException {
        final SubConfiguration cfg = SubConfiguration.getConfig("drss.tv");
        final boolean allow_teaser_pic = cfg.getBooleanProperty(ALLOW_TEASER_PIC, false);
        boolean allow_trailer = cfg.getBooleanProperty(ALLOW_TRAILER, false);
        boolean allow_others = cfg.getBooleanProperty(ALLOW_OTHERS, false);
        if (!this.parameter.matches(type_video)) {
            /* Force decryption of non video content in this case */
            allow_others = true;
        }
        if (force_other_content) {
            allow_others = true;
        }
        if (force_trailer) {
            allow_trailer = true;
        }
        title = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        if (title == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String[] bigtitles = getBigtitles();
        final String[] subtitles = getSubtitles();
        if (subtitles == null || subtitles.length == 0 || bigtitles == null || bigtitles.length == 0) {
            logger.warning("Decrypter method decryptVideolistStuff broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }

        final int bigtitles_length = bigtitles.length;
        final int subtitles_length = subtitles.length;
        if (bigtitles_length != subtitles_length) {
            logger.warning("Decrypter method decryptVideolistStuff broken for link: " + parameter);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        for (int counter = 0; counter <= bigtitles_length - 1; counter++) {
            final String bigtitle = bigtitles[counter];
            final String subtitle = subtitles[counter];
            try {
                /* First some errorhandling. */
                if (bigtitle.equals("Komplette Sendung") || (bigtitle.equals("Video") && subtitle.matches("Teil \\d+"))) {
                    decryptEpisode();
                    continue;
                }
                if (this.counter_real == 1 && this.parameter.matches(type_normal_episode)) {
                    /*
                     * No main video available but we have a video url? Well then they added it as a trailer by mistake --> Force trailer
                     * download!
                     */
                    force_trailer = true;
                    allow_trailer = true;
                }
                if (subtitle.equals("Titelbild")) {
                    if (allow_teaser_pic) {
                        br.getPage(parameter + "?video=" + counter_real);
                        String teaser_picture = br.getRegex("property=\"og:image\" content=\"(/images/[^<>\"]*?\\.jpg)\"").getMatch(0);
                        if (teaser_picture == null) {
                            teaser_picture = br.getRegex("player\\-" + counter_real + "\">[\t\n\r ]+<img [^>]+ src=\"(/images/[^<>\"]*?\\.jpg)\"").getMatch(0);
                        }
                        if (teaser_picture == null) {
                            logger.warning("Failed to find teaser picture!");
                            throw new DecrypterException("Decrypter broken for link: " + parameter);
                        }
                        teaser_picture = "directhttp://http://www.drss.tv" + teaser_picture;
                        final DownloadLink pic = createDownloadlink(teaser_picture);
                        pic.setFinalFileName(title + "_Titelbild_.jpg");
                        pic.setAvailable(true);
                        decryptedlinks.add(pic);
                    }
                } else if (subtitle.contains("Trailer")) {
                    /*
                     * Trailer exists and user wants to download it --> Find it. Trailers are usually hosted externally.
                     */
                    if (allow_trailer) {
                        br.getPage(parameter + "?video=" + counter_real);
                        String trailer_externlink = br.getRegex("player\\-" + counter_real + "\">[\t\n\r ]+<iframe[^<>]+(?:data\\-)?src=\"(http[^<>\"]*?)\"").getMatch(0);
                        if (trailer_externlink == null) {
                            trailer_externlink = br.getRegex("player\\-" + counter_real + "\">[\t\n\r ]+<div class=\"jp\\-video jp\\-video\\-wide\"[^<>]+data\\-url=\"(http[^<>\"]*?)\"").getMatch(0);
                        }
                        if (trailer_externlink == null) {
                            logger.warning("Failed to find trailer!");
                            throw new DecrypterException("Decrypter broken for link: " + parameter);
                        }
                        trailer_externlink += "&forced_referer=" + Encoding.Base64Encode(this.br.getURL());
                        decryptedlinks.add(createDownloadlink(trailer_externlink));
                    }
                } else {
                    /*
                     * Now we should have a video which is neither trailer nor any other undefined video type.
                     */
                    if (allow_others) {
                        br.getPage(parameter + "?video=" + counter_real);
                        String videolink = br.getRegex("player\\-" + counter_real + "\">[\t\n\r ]+<iframe[^<>]+(?:data\\-)?src=\"(http[^<>\"]*?)\"").getMatch(0);
                        if (videolink == null) {
                            videolink = br.getRegex("\"(\\?video=" + counter_real + ")\"").getMatch(0);
                        }
                        if (videolink == null) {
                            logger.warning("Failed to find trailer!");
                            throw new DecrypterException("Decrypter broken for link: " + parameter);
                        }
                        if (videolink.matches("\\?video=\\d+")) {
                            final DownloadLink segmentdl = crawlSegment(videolink);
                            segmentdl.setFinalFileName(title + "_" + bigtitle + "_" + subtitle + ".mp4");
                            decryptedlinks.add(segmentdl);
                        } else {
                            decryptedlinks.add(createDownloadlink(videolink));
                        }
                        logger.info("Decrypted unknown bigtitle: " + bigtitle);
                        logger.info("Decrypted unknown subtitle: " + subtitle);
                    }
                }
            } finally {
                counter_real++;
            }
        }
    }

    private String[] getBigtitles() {
        return br.getRegex("<h4 class=\"text\\-cutted\">([^<>\"]*?)</h4>").getColumn(0);
    }

    private String[] getSubtitles() {
        return br.getRegex("<p class=\"descr\">([^<>\"]*?)</p>").getColumn(0);
    }

    private DownloadLink crawlSegment(final String input) throws DecrypterException, IOException {
        final Browser segmentBR = this.br.cloneBrowser();
        segmentBR.getPage(parameter + input);
        String finallink = segmentBR.getRegex("data\\-url=\"(http[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            logger.warning("Failed to crawl segment: " + input);
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        if (finallink.matches(type_directlink_vimeo)) {
            finallink = "directhttp://" + finallink;
        }
        final DownloadLink fina = this.createDownloadlink(finallink);
        return fina;
    }

}
