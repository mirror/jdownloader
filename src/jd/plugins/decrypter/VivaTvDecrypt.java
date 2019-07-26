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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "mtv.de", "mtviggy.com", "southpark.de", "southpark.cc.com", "vh1.com", "nickmom.com", "nicktoons.nick.com", "teennick.com", "nickatnite.com", "mtv.com.au", "mtv.co.uk", "mtv.com", "logotv.com", "cc.com", "funnyclips.cc", "comedycentral.tv", "nick.de", "nickjr.de", "nicknight.de", "tvland.com", "spike.com", "cmt.com", "thedailyshow.cc.com", "tosh.cc.com", "mtvu.com" }, urls = { "https?://(?:www\\.)?mtv\\.de/.+", "https?://(?:www\\.)?(?:mtviggy|mtvdesi|mtvk)\\.com/.+", "https?://(?:www\\.)?southpark\\.de/.+", "https?://southpark\\.cc\\.com/.+", "https?://(?:www\\.)?vh1\\.com/.+", "https?://(?:www\\.)?nickmom\\.com/.+", "https?://nicktoons\\.nick\\.com/.+", "https?://(?:www\\.)?teennick\\.com/.+", "https?://(?:www\\.)?nickatnite\\.com/.+", "https?://(?:www\\.)?mtv\\.com\\.au/.+",
        "https?://(?:www\\.)?mtv\\.co\\.uk/.+", "https?://(?:www\\.)?mtv\\.com/.+", "https?://(?:www\\.)logotv\\.com/.+", "https?://(?:www\\.)?cc\\.com/.+", "https?://de\\.funnyclips\\.cc/.+", "https?://(?:www\\.)?comedycentral\\.tv/.+", "https?://(?:www\\.)?nick\\.de/.+", "https?://(?:www\\.)?nickjr\\.de/.+", "https?://(?:www\\.)?nicknight\\.de/.+", "https?://(?:www\\.)?tvland\\.com/.+", "https?://(?:www\\.)?spike\\.com/.+", "https?://(?:www\\.)?cmt\\.com/.+", "https?://thedailyshow\\.cc\\.com/.+", "https?://tosh\\.cc\\.com/.+", "https?://(?:www\\.)?mtvu\\.com/.+" })
public class VivaTvDecrypt extends PluginForDecrypt {
    public VivaTvDecrypt(PluginWrapper wrapper) {
        super(wrapper);
    }
    /** Tags: Viacom International Media Networks Northern Europe, mrss, gameone.de */

    /** Additional thanks goes to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/mtv.py */
    /* Additional information/methods can be found in the VivaTv host plugin */
    /** TODO: mtvplay.tv */
    private static final String     type_viva                    = "https?://(?:www\\.)?viva\\.tv/.+";
    private static final String     type_mtv_de                  = "https?://(?:www\\.)?mtv\\.de/.+";
    private static final String     type_southpark_de_episode    = "http://www\\.southpark\\.de/alle\\-episoden/.+";
    private static final String     type_southpark_cc_episode    = "http://southpark\\.cc\\.com/full\\-episodes/.+";
    private static final String     type_nickmom_com             = "https?://(?:www\\.)?nickmom\\.com/.+";
    private static final String     type_mtv_com                 = "https?://(?:www\\.)?mtv\\.com/.+";
    private static final String     type_logotv_com              = "http://www\\.logotv\\.com/.+";
    private static final String     hosterplugin_url_viacom_mgid = "http://viacommgid/";
    private static final String     PATTERN_MGID                 = "mgid:[A-Za-z]+:[A-Za-z0-9_\\-]+:[A-Za-z0-9\\.\\-]+:[A-Za-z0-9_\\-]+";
    private ArrayList<DownloadLink> decryptedLinks               = new ArrayList<DownloadLink>();
    private String                  default_ext                  = null;
    private String                  parameter                    = null;
    private String                  mgid                         = null;
    private String                  fpName                       = null;

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        /* we first have to load the plugin, before we can reference it */
        JDUtilities.getPluginForHost("viva.tv");
        default_ext = jd.plugins.hoster.VivaTv.default_ext;
        parameter = param.toString();
        jd.plugins.hoster.VivaTv.prepBR(this.br);
        if (parameter.matches(type_southpark_de_episode)) {
            decryptSouthparkDe();
        } else if (parameter.matches(type_southpark_cc_episode)) {
            decryptSouthparkCc();
        } else if (parameter.matches(type_nickmom_com)) {
            decryptNickmomCom();
        } else if (parameter.matches(type_mtv_com)) {
            decryptMtvCom();
        } else if (parameter.matches(type_logotv_com)) {
            decrypLogoTvCom();
        } else {
            /* Universal viacom crawler */
            this.br.getPage(parameter);
            vivaUniversalCrawler();
        }
        return decryptedLinks;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void decryptMtvGermanyPlaylists() {
        fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
        if (fpName == null) {
            /* Fallback to url-packagename */
            fpName = new Regex(this.parameter, "https?://[^/]+/(.+)").getMatch(0);
        }
        ArrayList<Object> ressourcelist = null;
        LinkedHashMap<String, Object> entries = null;
        try {
            final String json = this.br.getRegex("window\\.pagePlaylist = (\\[\\{.*?\\}\\])").getMatch(0);
            ressourcelist = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(json);
            for (final Object object : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) object;
                final String path = (String) entries.get("path");
                final String url_mrss = (String) entries.get("mrss");
                final String title = (String) entries.get("title");
                final String subtitle = (String) entries.get("subtitle");
                final String video_token = (String) entries.get("video_token");
                final String mgid = jd.plugins.hoster.VivaTv.getMGIDOutOfURL(url_mrss);
                if (url_mrss == null || title == null || video_token == null || mgid == null) {
                    throw new DecrypterException("Decrypter broken for link: " + parameter);
                }
                final String contenturl;
                if (path != null) {
                    contenturl = "http://" + this.br.getHost() + path;
                } else {
                    contenturl = this.parameter;
                }
                String temp_filename = title;
                if (subtitle != null) {
                    temp_filename += " - " + subtitle;
                }
                temp_filename += jd.plugins.hoster.VivaTv.default_ext;
                final DownloadLink dl = mgidSingleVideoGetDownloadLink(mgid);
                dl.setLinkID(video_token);
                dl.setName(temp_filename);
                dl.setAvailable(true);
                dl.setContentUrl(contenturl);
                this.decryptedLinks.add(dl);
            }
        } catch (final Throwable e) {
            return;
        }
    }

    private void decryptMtvComPlaylists() {
        fpName = this.br.getRegex("<h3 class=\"h\\-sub3 h\\-feed\">[\t\n\r ]*?<span>([^<>\"]*?)</span>[\t\n\r ]*?</h3>").getMatch(0);
        final String[] entries = this.br.getRegex("(id=\"vid\\d+\">.*?<p class=\"usage\"/>)").getColumn(0);
        if (entries != null && entries.length != 0) {
            for (final String entry : entries) {
                String title = new Regex(entry, "<span class=\"title_container trim_container\">[\t\n\r ]+<span>([^<>]*?)</span>").getMatch(0);
                if (title == null) {
                    title = new Regex(entry, "class=\"song\">([^<>\"]*?)</span>").getMatch(0);
                }
                String url_content = new Regex(entry, "itemprop=\"url\"[\t\n\r ]*?href=\"(/[^<>\"]*?)\"").getMatch(0);
                final String mgid = new Regex(entry, "/uri/(mgid:[^<>\"\\?]+)").getMatch(0);
                if (title == null || mgid == null) {
                    /* This shouldn't happen ... */
                    continue;
                }
                if (url_content != null) {
                    url_content = this.parameter;
                } else {
                    url_content = "http://" + this.br.getHost() + url_content;
                }
                final String url_final = getViacomHostUrl(mgid);
                final DownloadLink dl = this.createDownloadlink(url_final);
                if (title != null) {
                    /* Should cover 99% of all cases */
                    title = this.doFilenameEncoding(title);
                    dl.setName(title + default_ext);
                    dl.setAvailable(true);
                }
                dl.setContentUrl(url_content);
                dl.setLinkID(mgid);
                this.decryptedLinks.add(dl);
            }
        }
    }

    private void decryptSouthparkDe() throws IOException, DecrypterException {
        br.getPage(parameter);
        this.mgid = br.getRegex("media\\.mtvnservices\\.com/(mgid[^<>\"]+)\"").getMatch(0);
        if (this.mgid == null) {
            /* New 2016-10-19 */
            this.mgid = br.getRegex("data\\-mgid=\"(mgid:[^<>\"]+)\"").getMatch(0);
        }
        if (this.mgid == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String feedURL = String.format(getFEEDURL("southpark.de"), this.mgid);
        br.getPage(feedURL);
        fpName = getXML("title");
        if (fpName == null) {
            this.decryptedLinks = null;
            return;
        }
        fpName = new Regex(parameter, "episoden/(s\\d{2}e\\d{2})").getMatch(0) + " - " + fpName;
        fpName = Encoding.htmlDecode(fpName.trim());
        decryptFeed();
    }

    private void decryptSouthparkCc() throws IOException, DecrypterException {
        br.getPage(parameter);
        this.mgid = br.getRegex("data\\-mgid=\"(mgid[^<>\"]*?)\"").getMatch(0);
        if (this.mgid == null) {
            throw new DecrypterException("Decrypter broken for link: " + parameter);
        }
        final String feedURL = String.format(getFEEDURL("southpark.cc.com"), this.mgid);
        br.getPage(feedURL);
        fpName = getFEEDtitle(br.toString());
        if (fpName == null) {
            this.decryptedLinks = null;
            return;
        }
        fpName = new Regex(parameter, "episodes/(s\\d{2}e\\d{2})").getMatch(0) + " - " + fpName;
        fpName = Encoding.htmlDecode(fpName.trim());
        decryptFeed();
    }

    private void decryptNickmomCom() throws DecrypterException, IOException {
        br.getPage(parameter);
        String extern_ID = br.getRegex("\"(https?://(www\\.)?youtube\\.com/embed/[^<>\"]*?)\"").getMatch(0);
        if (extern_ID != null) {
            logger.info("Current link is an extern link");
            decryptedLinks.add(createDownloadlink(extern_ID));
            return;
        }
        vivaUniversalCrawler();
    }

    private void decryptMtvCom() throws Exception {
        // final String feedURL_plain = this.getFEEDURL("mtv.com");
        br.getPage(parameter);
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            this.decryptedLinks.add(this.createOfflinelink(this.parameter));
            return;
        }
        if (decryptVevo()) {
            return;
        }
        logger.info("Current link is NO VEVO link");
        String playlist_id = new Regex(this.parameter, "^.+#id=(\\d+)$").getMatch(0);
        if (playlist_id == null) {
            playlist_id = this.br.getRegex("\\.jhtml#id=(\\d+)\"").getMatch(0);
        }
        if (playlist_id != null) {
            /* Playlist */
            this.br.getPage("http://www.mtv.com/global/music/videos/ajax/playlist.jhtml?id=" + playlist_id);
            decryptMtvComPlaylists();
        } else {
            vivaUniversalCrawler();
        }
    }

    private void decrypLogoTvCom() throws Exception {
        boolean isPlaylist = false;
        final String playlist_id = new Regex(this.parameter, "^.+#id=(\\d+)$").getMatch(0);
        String url_name = new Regex(this.parameter, "/([A-Za-z0-9\\-_]+)\\.j?html").getMatch(0);
        if (playlist_id != null) {
            /* Playlist */
            this.br.getPage("http://www.logotv.com/global/music/videos/ajax/playlist.jhtml?id=" + playlist_id);
            decryptMtvComPlaylists();
            isPlaylist = this.decryptedLinks.size() > 0;
        }
        if (!isPlaylist) {
            /* Feed */
            /* We have no feed-url so let's use this */
            br.getPage(parameter);
            if (decryptVevo()) {
                return;
            }
            logger.info("Current link is NO VEVO link");
            if (!br.containsHTML("\"video\":")) {
                decryptedLinks.add(this.createOfflinelink(parameter));
                return;
            }
            this.mgid = br.getRegex("media\\.mtvnservices\\.com/(mgid[^<>\"]*?)\"").getMatch(0);
            if (this.mgid == null) {
                this.decryptedLinks = null;
                return;
            }
            final String feedURL = "http://www.logotv.com/player/includes/rss.jhtml?uri=" + this.mgid;
            br.getPage(feedURL);
            decryptFeed();
        }
        if (fpName == null) {
            fpName = url_name;
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fpName = Encoding.htmlDecode(fpName.trim());
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
    }

    /** This function is able to crawl content of most viacom/mtv websites. */
    private void vivaUniversalCrawler() throws IOException, DecrypterException {
        if (decryptVevo()) {
            return;
        }
        crawlDrupal();
        crawlMgids();
        crawlTriforceManifestFeed();
        decryptMtvGermanyPlaylists();
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fpName = Encoding.htmlDecode(fpName.trim());
            fp.setName(fpName);
            fp.addLinks(decryptedLinks);
        }
    }

    /** In case a video is not hosted by mtv but by vimeo, this function should find- and return that vimeo url. */
    private boolean decryptVevo() {
        final String vevo_ID = br.getRegex("MTVN\\.Player\\.vevoVideoId = \"([A-Za-z0-9]+)\";").getMatch(0);
        if (vevo_ID != null) {
            logger.info("Current link is a VEVO link");
            decryptedLinks.add(createDownloadlink("http://www.vevo.com/watch/" + vevo_ID));
            return true;
        }
        return false;
    }

    private void crawlMgids() throws IOException, DecrypterException {
        final String[] mgids = this.br.getRegex("(" + PATTERN_MGID + ")").getColumn(0);
        if (mgids != null) {
            for (final String mgid : mgids) {
                addMgid(mgid);
            }
        }
    }

    /** Finds mgids inside drupal json */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private void crawlDrupal() {
        final String playlist_id = br.getRegex("name=\"vimn:entity_uuid\" content=\"([a-z0-9\\-:]*?)\"").getMatch(0);
        final String js = br.getRegex("jQuery\\.extend\\(Drupal\\.settings, (\\{.*?)\\);.*?</script>").getMatch(0);
        if (js == null || playlist_id == null) {
            return;
        }
        try {
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(js);
            final LinkedHashMap<String, Object> vimn_video = (LinkedHashMap<String, Object>) entries.get("vimn_video");
            ArrayList<Object> resources = null;
            final Object playlist_o = vimn_video.get("playlists");
            final Object embedded_videos_o = vimn_video.get("embedded_videos");
            if (!(playlist_o instanceof ArrayList)) {
                /* Playlist */
                entries = (LinkedHashMap<String, Object>) playlist_o;
                entries = (LinkedHashMap<String, Object>) entries.get(playlist_id);
                resources = (ArrayList) entries.get("items");
                final Object title_object = entries.get("title");
                if (title_object instanceof String) {
                    fpName = (String) entries.get("title");
                }
                for (final Object pt : resources) {
                    final LinkedHashMap<String, Object> playlistentry = (LinkedHashMap<String, Object>) pt;
                    final String mgid = (String) playlistentry.get("guid");
                    final String partname = (String) playlistentry.get("title");
                    if (mgid == null || partname == null) {
                        continue;
                    }
                    if (fpName == null) {
                        fpName = doFilenameEncoding(partname);
                    }
                    final String final_filename = this.doFilenameEncoding(partname) + this.default_ext;
                    final DownloadLink fina = mgidSingleVideoGetDownloadLink(mgid);
                    fina.setFinalFileName(final_filename);
                    fina.setProperty("decryptedfilename", final_filename);
                    fina.setAvailable(true);
                    fina.setContentUrl(this.parameter);
                    decryptedLinks.add(fina);
                }
            }
            if (embedded_videos_o instanceof ArrayList) {
                resources = (ArrayList) embedded_videos_o;
                for (final Object pt : resources) {
                    final LinkedHashMap<String, Object> playlistentry = (LinkedHashMap<String, Object>) pt;
                    final String mgid = (String) playlistentry.get("video_id");
                    final String partname = (String) playlistentry.get("video_title");
                    final String url = (String) playlistentry.get("video_url");
                    if (mgid == null || partname == null || url == null) {
                        continue;
                    }
                    final String final_filename = this.doFilenameEncoding(partname) + this.default_ext;
                    final DownloadLink fina = mgidSingleVideoGetDownloadLink(mgid);
                    fina.setFinalFileName(final_filename);
                    fina.setProperty("decryptedfilename", final_filename);
                    fina.setAvailable(true);
                    fina.setContentUrl("http://" + this.br.getHost() + url);
                    decryptedLinks.add(fina);
                }
            }
        } catch (final Throwable e) {
        }
    }

    /* 2017-02-22: New - finds- and adds feed URLs --> These will then go back into the decrypter! */
    private void crawlTriforceManifestFeed() {
        try {
            final String json_source = this.br.getRegex("var triforceManifestFeed\\s*?=\\s*?(\\{.*?\\})\\s+").getMatch(0);
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
            LinkedHashMap<String, Object> entries2 = null;
            entries = (LinkedHashMap<String, Object>) entries.get("manifest");
            entries = (LinkedHashMap<String, Object>) entries.get("zones");
            final Iterator<Entry<String, Object>> it = entries.entrySet().iterator();
            while (it.hasNext()) {
                final Entry<String, Object> entry = it.next();
                entries2 = (LinkedHashMap<String, Object>) entry.getValue();
                final String url_feed = (String) entries2.get("feed");
                if (url_feed != null) {
                    this.decryptedLinks.add(this.createDownloadlink(url_feed));
                }
            }
        } catch (final Throwable e) {
        }
    }

    /** Validates- and adds mgids. */
    private void addMgid(String mgid) throws IOException, DecrypterException {
        if (mgid == null) {
            return;
        }
        mgid = cleanMgid(mgid);
        /* Skip image-mgids - we don't need them! */
        if (!isValidMgid(mgid)) {
            return;
        }
        if (mgidIsPlaylist(mgid)) {
            /* Episode (maybe with multiple segments) */
            final String feed_url = jd.plugins.hoster.VivaTv.mgidGetFeedurlForMgid(mgid);
            if (feed_url == null) {
                return;
            }
            this.br.getPage(feed_url);
            decryptFeed();
        } else {
            final DownloadLink dl = mgidSingleVideoGetDownloadLink(mgid);
            if (dl != null) {
                dl.setContentUrl(this.parameter);
                this.decryptedLinks.add(dl);
            }
        }
    }

    /** Used to make downloadlink objects out of mgids of which we know they are single videos. */
    private DownloadLink mgidSingleVideoGetDownloadLink(String mgid) {
        /* Additional errorhandling - make sure we only accept valid video content! */
        if (!isValidMgid(mgid) || !mgidIsSingleVideo(mgid)) {
            return null;
        }
        mgid = cleanMgid(mgid);
        final String url_hosterplugin = getViacomHostUrl(mgid);
        final DownloadLink dl = this.createDownloadlink(url_hosterplugin);
        return dl;
    }

    private boolean mgidIsSingleVideo(final String mgid) {
        final String type = jd.plugins.hoster.VivaTv.mgidGetType(mgid);
        final boolean isvideo = type.equals("video");
        return isvideo;
    }

    private String cleanMgid(String mgid) {
        if (mgid == null) {
            return null;
        }
        mgid = Encoding.htmlDecode(mgid);
        mgid = mgid.replace(" ", "");
        return mgid;
    }

    /** Validates mgids */
    private boolean isValidMgid(final String mgid) {
        if (mgid == null) {
            return false;
        }
        boolean isValidMgid = mgid.matches(PATTERN_MGID);
        if (isValidMgid && mgid.contains(":image:") || mgid.contains(":drupal:")) {
            isValidMgid = false;
        }
        return isValidMgid;
    }

    private boolean mgidIsPlaylist(final String mgid) {
        if (mgid.contains("episode")) {
            return true;
        }
        return false;
    }

    /** General function to decrypt viacom RSS feeds, especially with multiple segments of a single video no matter what their source is. */
    private void decryptFeed() throws DecrypterException {
        final String[] items = br.getRegex("<item>(.*?)</item>").getColumn(0);
        if (fpName == null) {
            fpName = getMainFEEDTitle();
        }
        if (items == null || items.length == 0 || fpName == null) {
            return;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);
        int counter = -1;
        for (final String item : items) {
            counter++;
            String title = getFEEDtitle(item);
            String item_mgid = new Regex(item, "uri=(mgid:[A-Za-z0-9:\\-\\.]+)").getMatch(0);
            if (item_mgid == null) {
                item_mgid = new Regex(item, "(mgid:[A-Za-z0-9:\\-\\.]+)").getMatch(0);
            }
            if (title == null || item_mgid == null) {
                throw new DecrypterException("Decrypter broken for link: " + parameter);
            }
            /* We don't need the intro - it's always the same! */
            if (counter == 0 && items.length > 1 && title.contains("Intro")) {
                continue;
            }
            title = doFilenameEncoding(title);
            title = title + this.default_ext;
            // final DownloadLink dl = mgidSingleVideoGetDownloadLink(item_mgid);
            final String url_hosterplugin = getViacomHostUrl(item_mgid);
            final DownloadLink dl = this.createDownloadlink(url_hosterplugin);
            dl.setProperty("decryptedfilename", title);
            dl.setProperty("mainlink", this.parameter);
            dl.setName(title);
            dl.setAvailable(true);
            dl.setContentUrl(this.parameter);
            decryptedLinks.add(dl);
        }
    }

    private String getMainFEEDTitle() {
        String title = getXML("title");
        if (title != null) {
            title = doEncoding(title);
        }
        return title;
    }

    private String getXML(final String parameter) {
        return getXML(this.br.toString(), parameter);
    }

    private String getXML(final String source, final String parameter) {
        return new Regex(source, "<" + parameter + "[^<]*?>([^<>]*?)</" + parameter + ">").getMatch(0);
    }

    private String getFEEDURL(final String domain) {
        return jd.plugins.hoster.VivaTv.feedURLs.get(domain);
    }

    private String getEMBEDURL(final String domain) {
        return jd.plugins.hoster.VivaTv.embedURLs.get(domain);
    }

    private String getFEEDtitle(final String source) {
        return jd.plugins.hoster.VivaTv.feedGetTitle(source);
    }

    private String doEncoding(final String data) {
        return jd.plugins.hoster.VivaTv.doEncoding(data);
    }

    private String doFilenameEncoding(final String filename) {
        return jd.plugins.hoster.VivaTv.doFilenameEncoding(this, filename);
    }

    private String getViacomHostUrl(final String mgid) {
        return hosterplugin_url_viacom_mgid + mgid;
    }
}
