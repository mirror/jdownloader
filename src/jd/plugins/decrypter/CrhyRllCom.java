package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.regex.Pattern;

import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "crunchyroll.com" }, urls = { "http://(?:www.)?crunchyroll.com(?:.br)?/(?!forumtopic)(?:comics_read(?:/(?:manga|comipo|artistalley))?\\?(?:volume_id|series_id)=[0-9]+&chapter_num=[0-9]+\\.[0-9]+|[\\w_\\-]+/[\\w\\_\\-]+\\-[0-9]+)" })
public class CrhyRllCom extends PluginForDecrypt {
    // Define the video quality codes used for RTMP
    public static enum DestinationQuality {
        VIDEO1080P("1080p", new String[] { "80" }),
        VIDEO360P("360p", new String[] { "60", "10" }),
        VIDEO480P("480p", new String[] { "61", "20" }),
        VIDEO720P("720p", new String[] { "62", "30" });
        private String   text;
        private String[] val;

        DestinationQuality(final String text, final String[] val) {
            this.text = text;
            this.val = val;
        }

        public String getFirstValue() {
            return this.val[0];
        }

        public String getText() {
            return this.text;
        }

        public String[] getValues() {
            return this.val;
        }

        @Override
        public String toString() {
            return this.text;
        }
    }

    static private final Pattern CONFIG_SUBS = Pattern.compile("<subtitle id='([0-9]+?)' link='(http://www\\.crunchyroll\\.com/xml/\\?req=RpcApiSubtitle_GetXml&amp;subtitle_script_id=[0-9]+?)' title='(.+?)'.*?/>", Pattern.CASE_INSENSITIVE);
    static private final Pattern CONFIG_URL  = Pattern.compile("(http://www\\.crunchyroll\\.com/xml/\\?req=RpcApiVideoPlayer_GetStandardConfig&media_id=([0-9]+).*video_quality=)([0-9]*)(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_FILE   = Pattern.compile("<file>(.*?)</file>", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_HOST   = Pattern.compile("<host>(rtmp.*)</host>", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_QUAL   = Pattern.compile("<video_encode_quality>(.*?)</video_encode_quality>", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_SWF    = Pattern.compile("<default:chromelessPlayerUrl>([^<>\"]+\\.swf.*)</default:chromelessPlayerUrl>", Pattern.CASE_INSENSITIVE);
    /* 2016-04-29: http://static.ak.crunchyroll.com/versioned_assets/StandardVideoPlayer.cc7e8515.swf */
    /* 2016-10-19: http://static.ak.crunchyroll.com/vendor/StandardVideoPlayer-10dff2a.swf */
    static private final Pattern SWF_URL     = Pattern.compile("((http://static\\.ak\\.crunchyroll\\.com/[a-z0-9\\-_]+/(?:[a-f0-9\\.]+/)?)StandardVideoPlayer(?:.+)?\\.swf)", Pattern.CASE_INSENSITIVE);
    static private final String  SWF_DIR     = "http://static.ak.crunchyroll.com/flash/20120424185935.0acb0eac20ff1d5f75c78ac39a889d03/";
    private final int            EPISODE_PAD = 3;
    private final char           SEPARATOR   = '-';
    private final String         SUFFIX_RAW  = ".raw";
    private final String         EXT_UNKNOWN = ".unk";
    private final String         EXT_SUBS    = ".ass";
    private final String         EXT_MANGA   = ".jpg";
    final FilePackage            filePackage = FilePackage.getInstance();

    @SuppressWarnings("deprecation")
    public CrhyRllCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * JD2 CODE: DO NOIT USE OVERRIDE FÒR COMPATIBILITY REASONS!!!!!
     */
    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        try {
            // Attempt to login
            this.setBrowserExclusive();
            loadPlugin();
            final Account account = AccountController.getInstance().getValidAccount(plugin);
            if (account != null) {
                try {
                    ((jd.plugins.hoster.CrunchyRollCom) plugin).login(account, this.br, false);
                } catch (final Throwable e) {
                }
            }
            // set utf-8
            this.br.setCustomCharset("utf-8");
            // Load the linked page
            this.br.setFollowRedirects(true);
            cryptedLink.setCryptedUrl(cryptedLink.getCryptedUrl().replace("crunchyroll.com.br/", "crunchyroll.com/"));
            getPage(cryptedLink.getCryptedUrl());
            // Fog: Check for manga link first before handling videos/subs
            if (br.getURL().contains("comics_read")) {
                final String series_id = br.getRegex("seriesId\\=(\\d+)").getMatch(0);
                final String session_id = br.getRegex("session_id\\=(\\w+)").getMatch(0);
                String auth = br.getRegex("auth\\=([_%a-zA-Z0-9]+)").getMatch(0);
                final String name = br.getRegex("manga%252F([a-zA-Z0-9_\\-]+)").getMatch(0);
                // Fog: This can happen if you are not logged into an account, and is a valid input for certain free titles
                if (auth == null) {
                    auth = "null";
                }
                String chapter_number = br.getRegex("chapterNumber\\=(\\d+(\\.*)(\\d+))").getMatch(0);
                filePackage.setName(name + "-" + chapter_number.replace(".", "-"));
                getPage("http://api-manga.crunchyroll.com/chapters?series_id=" + series_id);
                if (br.toString().contains("\"error\"")) {
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab series information"));
                    return decryptedLinks;
                }
                String json_source = br.toString();
                json_source = Encoding.htmlDecode(json_source);
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
                final ArrayList<Object> chapter_array = (ArrayList<Object>) entries.get("chapters");
                String chapter_id = null;
                for (Object c : chapter_array) {
                    entries = (LinkedHashMap<String, Object>) c;
                    final String number = entries.get("number").toString();
                    if (chapter_number.equals(number)) {
                        chapter_id = entries.get("chapter_id").toString();
                        break;
                    }
                }
                getPage("http://api-manga.crunchyroll.com/list_chapter?session_id=" + session_id + "&chapter_id=" + chapter_id + "&auth=" + auth);
                if (br.toString().contains("\"error\"")) {
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab chapter information"));
                    return decryptedLinks;
                }
                json_source = br.toString();
                json_source = Encoding.htmlDecode(json_source);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json_source);
                final ArrayList<Object> pages_array = (ArrayList<Object>) entries.get("pages");
                for (Object p : pages_array) {
                    entries = (LinkedHashMap<String, Object>) p;
                    // Fog: Store this just in case the composed image is not available.
                    final String temp_image = entries.get("image_url").toString();
                    final String page_number = entries.get("number").toString();
                    entries = (LinkedHashMap<String, Object>) entries.get("locale");
                    entries = (LinkedHashMap<String, Object>) entries.get("enUS");
                    String image = entries.get("encrypted_composed_image_url").toString();
                    // Fog: This can apparently happen somehow, so try to grab the raw uncomposed image.
                    if (image == null && temp_image != null) {
                        image = temp_image;
                    }
                    // Fog: If both are null, then abandon all hope of grabbing a proper image file.
                    else if (image == null && temp_image == null) {
                        decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Unable to grab image information"));
                        return decryptedLinks;
                    }
                    final String filename = name + "-" + chapter_number.replace(".", "-") + "-" + page_number + EXT_MANGA;
                    final DownloadLink dl = this.createDownloadlink(image);
                    dl._setFilePackage(filePackage);
                    dl.setFinalFileName(filename);
                    dl.setContentUrl(cryptedLink.getCryptedUrl());
                    dl.setLinkID(filename);
                    dl.setAvailable(true);
                    decryptedLinks.add(dl);
                }
            }
            // Fog: Manga link not found, assume that it's a video/sub link
            else {
                if (br.getURL().equals("http://www.crunchyroll.com/") || br.containsHTML("Sorry, this video is not available in your region due to licensing restrictions")) {
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Regioned Blocked"));
                    return decryptedLinks;
                }
                if (br.containsHTML("\"Note: This video requires a")) {
                    logger.info("Video only available for premium users (in the current region): " + cryptedLink.getCryptedUrl());
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Only available for Premium Account holders"));
                    return decryptedLinks;
                }
                // Determine if the video exists
                if (this.br.containsHTML("(<title>Crunchyroll \\- Page Not Found</title>|<p>But we were unable to find the page you were looking for\\. Sorry\\.</p>)")) {
                    // not available == offline, no need to show error message
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl()));
                    return decryptedLinks;
                }
                if (br.getURL().contains("maturity_wall")) {
                    logger.info("Link can only be decrypted if you own and add a crunchyroll.com account: " + cryptedLink.getCryptedUrl());
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Only available for Account Holders"));
                    return decryptedLinks;
                }
                if (br.containsHTML("This video has not been released yet")) {
                    logger.info("Video is not released yet -> Cannot decrypt link: " + cryptedLink.getCryptedUrl());
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Content not released yet!"));
                    return decryptedLinks;
                }
                // Get the episode name
                String title = this.nameFromVideoUrl(cryptedLink.getCryptedUrl());
                if (title == null) {
                    title = new Regex(cryptedLink.getCryptedUrl(), "/([^<>\"/]+)$").getMatch(0);
                }
                if (title == null) {
                    decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Plugin Error: Title could not be found."));
                    return decryptedLinks;
                }
                // Get the link to the XML file
                final Regex configUrlSearch = this.br.getRegex("\"config_url\":\"(.+?)\"");
                if (!configUrlSearch.matches()) {
                    throw new DecrypterException("Failed to get config url");
                }
                final String configUrlDecode = Encoding.htmlDecode(configUrlSearch.getMatch(0));
                final Regex configUrl = new Regex(configUrlDecode, CrhyRllCom.CONFIG_URL);
                if (!configUrl.matches()) {
                    if (configUrlDecode.contains("video_format=0") && !configUrlDecode.contains("video_quality")) {
                        /* 2016-10-19: Added some errorhandling for premiumonly content. */
                        final String configErrorHandling = new Regex(configUrlDecode, "pop_out_disable_message=([^&\\?]+)").getMatch(0);
                        logger.info("Link can only be decrypted if you own and add a crunchyroll.com account! Crunchyroll Error Message: " + Encoding.htmlDecode(configErrorHandling.replace("+", " ")) + " :: " + cryptedLink.getCryptedUrl());
                        decryptedLinks.add(createOfflinelink(cryptedLink.getCryptedUrl(), "Only available for Premium Account holders"));
                        return decryptedLinks;
                    }
                    throw new DecrypterException("Invalid config url");
                }
                // Get the link to the SWF file
                final Regex swfUrlSearch = this.br.getRegex("swfobject.embedSWF\\(\"(.*?)\"");
                if (!swfUrlSearch.matches()) {
                    logger.warning("WTF");
                    throw new DecrypterException("Failed to get SWF url");
                }
                final String swfUrlDecode = Encoding.htmlDecode(swfUrlSearch.getMatch(0).replaceAll("\\\\/", "/"));
                final Regex swfUrl = new Regex(swfUrlDecode, CrhyRllCom.SWF_URL);
                if (!swfUrl.matches()) {
                    throw new DecrypterException("Invalid SWF url");
                }
                // Find the available qualities by looking for the buttons
                String[] qualities = this.br.getRegex("\\?p([0-9]+)=1").getColumn(0);
                if (qualities == null || qualities.length == 0) {
                    qualities = br.getRegex("token=\"showmedia\\.(\\d+)p\"").getColumn(0);
                    if (qualities == null || qualities.length == 0) {
                        throw new DecrypterException("No qualities found");
                    }
                }
                filePackage.setProperty("ALLOW_MERGE", true);
                filePackage.setName(title);
                for (String quality : qualities) {
                    quality += "p"; // '360' => '360p'
                    // Try and find the RTMP quality codes
                    CrhyRllCom.DestinationQuality qualityValue = null;
                    if (quality.equals(CrhyRllCom.DestinationQuality.VIDEO360P.toString())) {
                        qualityValue = CrhyRllCom.DestinationQuality.VIDEO360P;
                    } else if (quality.equals(CrhyRllCom.DestinationQuality.VIDEO480P.toString())) {
                        qualityValue = CrhyRllCom.DestinationQuality.VIDEO480P;
                    } else if (quality.equals(CrhyRllCom.DestinationQuality.VIDEO720P.toString())) {
                        qualityValue = CrhyRllCom.DestinationQuality.VIDEO720P;
                    } else if (quality.equals(CrhyRllCom.DestinationQuality.VIDEO1080P.toString())) {
                        qualityValue = DestinationQuality.VIDEO1080P;
                    }
                    if (qualityValue == null) {
                        continue;
                    }
                    final String xmlUrl = configUrl.getMatch(0) + qualityValue.getFirstValue() + configUrl.getMatch(3);
                    final String filename = title + "." + quality + SUFFIX_RAW;
                    final DownloadLink thisLink = this.createDownloadlink(xmlUrl);
                    thisLink.setContentUrl(cryptedLink.getCryptedUrl());
                    thisLink.setFinalFileName(filename + EXT_UNKNOWN);
                    thisLink.setProperty("quality", qualityValue.getFirstValue());
                    thisLink.setProperty("filename", filename);
                    thisLink.setProperty("swfdir", swfUrl.getMatch(1));
                    thisLink.setProperty("valid", true);
                    filePackage.add(thisLink);
                    decryptedLinks.add(thisLink);
                }
                // Get subtitles
                postPage(configUrlDecode, "current_page=" + cryptedLink.getCryptedUrl());
                final String mediaId = br.getRegex("<media_id>(\\d+)</media_id>").getMatch(0);
                postPage("http://www.crunchyroll.com/xml/", "req=RpcApiSubtitle%5FGetListing&media%5Fid=" + mediaId);
                final String[][] subtitles = this.br.getRegex(CrhyRllCom.CONFIG_SUBS).getMatches();
                // Loop through each subtitles xml found
                for (final String[] subtitle : subtitles) {
                    final String subUrl = Encoding.htmlDecode(subtitle[1]);
                    final String subTitle = subtitle[2];
                    String subName = new Regex(subTitle, "\\[[0-9\\s]+\\]\\s*(.+)").getMatch(0);
                    if (subName == null) {
                        subName = subTitle;
                    }
                    subName = subName.replace(' ', SEPARATOR).toLowerCase();
                    subName = subName.replaceAll("[\\[\\]\\(\\)]+", "");
                    final String subFile = title + "." + subName;
                    final DownloadLink thisLink = this.createDownloadlink(subUrl);
                    thisLink.setContentUrl(cryptedLink.getCryptedUrl());
                    thisLink.setFinalFileName(subFile + EXT_SUBS);
                    thisLink.setProperty("filename", subFile);
                    thisLink.setProperty("valid", true);
                    filePackage.add(thisLink);
                    decryptedLinks.add(thisLink);
                }
            }
        } catch (final IOException e) {
            this.logger.log(e);
            return null;
        } finally {
            try {
                this.br.getHttpConnection().disconnect();
            } catch (final Throwable e) {
            }
        }
        return decryptedLinks;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }

    private String nameFromVideoId(final String videoId, Browser br) {
        if (br == null) {
            br = this.br;
        }
        // Make sure that the id is just numbers
        if (!new Regex(videoId, "^[0-9]+$").matches()) {
            return null;
        }
        String name = null;
        try {
            // Use a feature where you are redirected to the full-url if you go to a shortened version
            br.setFollowRedirects(false);
            getPage(br, "http://www.crunchyroll.com/a/a-" + videoId);
            name = this.nameFromVideoUrl(br.getRedirectLocation());
        } catch (final Throwable e) {
        }
        return name;
    }

    private String nameFromVideoUrl(final String videoUrl) {
        // Get the episode name 'show-episode-1-name'
        final Regex urlReg = new Regex(videoUrl, "crunchyroll\\.com/([\\w\\_\\-]+)/episode\\-([0-9]+)([\\w\\_\\-]*)\\-([0-9]+)");
        if (!urlReg.matches()) {
            return null;
        }
        final String series = urlReg.getMatch(0);
        String episode = urlReg.getMatch(1);
        final String title = urlReg.getMatch(2);
        // Pad out the episode number
        while (episode.length() < EPISODE_PAD) {
            episode = "0" + episode;
        }
        return series + SEPARATOR + episode + title;
    }

    /**
     * Try and find the RTMP details for the given link. If the details are successfully found, then set the properties of the link. rtmphost =
     * TcUrl. rtmpfile = playpath. rtmpswf = swfVfy (without full path). filename = output filename without extension. qualityname = text
     * definition of the quality found ("360p", "480p", etc).
     *
     * @param downloadLink
     *            The DownloadLink file to check
     * @param br
     *            The browser to use to load the XML file with. If null, uses different browser
     * @throws Exception
     */
    public void setRTMP(final DownloadLink downloadLink, Browser br) throws Exception {
        if (br == null) {
            br = this.br;
        }
        if (downloadLink.getStringProperty("swfdir") == null) {
            downloadLink.setProperty("swfdir", CrhyRllCom.SWF_DIR);
        }
        // Extract the quality code from the url
        final Regex configUrl = new Regex(downloadLink.getDownloadURL(), CrhyRllCom.CONFIG_URL);
        final String qualityStr = configUrl.getMatch(2);
        if (qualityStr == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid URL (could not find quality)");
        }
        // Try and find that quality code in the known codes
        DestinationQuality qualityObj = null;
        for (final DestinationQuality quality : DestinationQuality.values()) {
            for (final String value : quality.getValues()) {
                if (qualityStr.equals(value)) {
                    qualityObj = quality;
                    break;
                }
            }
            if (qualityObj != null) {
                break;
            }
        }
        if (qualityObj == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown quality");
        }
        final String mediaId = configUrl.getMatch(1);
        // Get the filename, and generate a new one if it doesn't exist
        String filename = downloadLink.getStringProperty("filename");
        if (filename == null) {
            filename = this.nameFromVideoId(mediaId, br);
            if (filename != null) {
                filename += "." + qualityObj.getText() + SUFFIX_RAW;
                downloadLink.setProperty("filename", filename);
            } else {
                // Failed to get an appealing one
                filename = "CrunchyRoll-" + mediaId + qualityObj.getText() + SUFFIX_RAW;
            }
        }
        downloadLink.setFinalFileName(filename + EXT_UNKNOWN);
        // Loop through all of the quality codes for the given quality
        for (final String quality : qualityObj.getValues()) {
            // Get the XML file for the given quality code
            final String url = configUrl.getMatch(0) + quality + configUrl.getMatch(3);
            br.setFollowRedirects(true);
            postPage(br, url, "current_page=" + downloadLink.getDownloadURL());
            // Does the file actually exist?
            if (br.containsHTML("<msg>Media not found</msg>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File does not exist");
            }
            // Check if a premium account is needed (and we aren't using one)
            if (br.containsHTML("<upsell>1</upsell>")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Premium account required for this quality");
            }
            // Check if the quality found is actually the one we wanted
            final String qual = Encoding.htmlDecode(br.getRegex(CrhyRllCom.RTMP_QUAL).getMatch(0));
            if (!qual.equals(quality)) {
                continue;
            }
            // Get the needed RTMP details
            final String host = Encoding.htmlDecode(br.getRegex(CrhyRllCom.RTMP_HOST).getMatch(0));
            final String file = Encoding.htmlDecode(br.getRegex(CrhyRllCom.RTMP_FILE).getMatch(0));
            final String swf = br.getRegex(CrhyRllCom.RTMP_SWF).getMatch(0).replaceAll("&amp;", "&");
            String filetype = new Regex(file, "^(.+):.*").getMatch(0);
            if (filetype == null) {
                filetype = EXT_UNKNOWN;
            } else {
                filetype = "." + filetype;
            }
            downloadLink.setFinalFileName(filename + filetype);
            downloadLink.setProperty("rtmphost", host);
            downloadLink.setProperty("rtmpfile", file);
            downloadLink.setProperty("rtmpswf", swf);
            downloadLink.setProperty("valid", true);
            return;
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Quality not available (try using premium)");
    }

    private PluginForHost plugin = null;

    private void getPage(final String parameter) throws Exception {
        getPage(br, parameter);
    }

    private void getPage(final Browser br, final String parameter) throws Exception {
        loadPlugin();
        ((jd.plugins.hoster.CrunchyRollCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.CrunchyRollCom) plugin).getPage(br, parameter);
    }

    private void postPage(final String page, final String postData) throws Exception {
        postPage(br, page, postData);
    }

    private void postPage(final Browser br, final String page, final String postData) throws Exception {
        loadPlugin();
        ((jd.plugins.hoster.CrunchyRollCom) plugin).setBrowser(br);
        ((jd.plugins.hoster.CrunchyRollCom) plugin).postPage(br, page, postData);
    }

    public void loadPlugin() {
        if (plugin == null) {
            plugin = JDUtilities.getPluginForHost("crunchyroll.com");
            if (plugin == null) {
                throw new IllegalStateException("crunchyroll.com hoster plugin not found!");
            }
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}