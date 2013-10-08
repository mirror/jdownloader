package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "crunchyroll.com" }, urls = { "http://(www\\.)?crunchyroll\\.com/(?!forumtopic)[\\w\\_\\-]+/[\\w\\_\\-]+\\-[0-9]+" }, flags = { 2 })
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

    static private final Pattern CONFIG_SUBS    = Pattern.compile("<subtitle id='([0-9]+?)' link='(http://www\\.crunchyroll\\.com/xml/\\?req=RpcApiSubtitle_GetXml&amp;subtitle_script_id=[0-9]+?)' title='(.+?)'.*?/>", Pattern.CASE_INSENSITIVE);
    static private final Pattern CONFIG_URL     = Pattern.compile("(http://www\\.crunchyroll\\.com/xml/\\?req=RpcApiVideoPlayer_GetStandardConfig&media_id=([0-9]+).*video_quality=)([0-9]*)(.*)", Pattern.CASE_INSENSITIVE);
    static private final Pattern ANDROID_URL    = Pattern.compile("http://www\\.crunchyroll\\.com/android_rpc/\\?req=RpcApiAndroid_GetVideoWithAcl&media_id=([0-9]+).*", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_FILE      = Pattern.compile("<file>(.*?)</file>", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_HOST      = Pattern.compile("<host>(rtmp.*)</host>", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_QUAL      = Pattern.compile("<video_encode_quality>(.*?)</video_encode_quality>", Pattern.CASE_INSENSITIVE);
    static private final Pattern RTMP_SWF       = Pattern.compile("<default:chromelessPlayerUrl>(ChromelessPlayerApp\\.swf.*)</default:chromelessPlayerUrl>", Pattern.CASE_INSENSITIVE);
    static private final Pattern SWF_URL        = Pattern.compile("((http://static\\.ak\\.crunchyroll\\.com/flash/[a-f0-9\\.]+/)StandardVideoPlayer\\.swf)", Pattern.CASE_INSENSITIVE);
    static private final String  SWF_DIR        = "http://static.ak.crunchyroll.com/flash/20120424185935.0acb0eac20ff1d5f75c78ac39a889d03/";
    private final int            EPISODE_PAD    = 3;
    private final char           SEPARATOR      = '-';
    private final String         SUFFIX_RAW     = ".raw";
    private final String         SUFFIX_ANDROID = ".android.english";
    private final String         EXT_UNKNOWN    = ".unk";
    private final String         EXT_SUBS       = ".ass";

    @SuppressWarnings("deprecation")
    public CrhyRllCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        try {
            // Attempt to login
            this.setBrowserExclusive();
            final PluginForHost plugin = JDUtilities.getPluginForHost("crunchyroll.com");
            if (plugin != null) {
                final Account account = AccountController.getInstance().getValidAccount(plugin);
                if (account != null) {
                    try {
                        ((jd.plugins.hoster.CrunchyRollCom) plugin).login(account, this.br, false, true);
                    } catch (final Throwable e) {
                    }
                }
            }
            // set utf-8
            this.br.setCustomCharset("utf-8");
            // Load the linked page
            this.br.setFollowRedirects(true);
            this.br.getPage(cryptedLink.getCryptedUrl());
            if (br.getURL().equals("http://www.crunchyroll.com/") || br.containsHTML("Sorry, this video is not available in your region due to licensing restrictions")) {
                logger.info("Link offline: " + cryptedLink.getCryptedUrl());
                return decryptedLinks;
            }
            if (br.containsHTML("\"Note: This video requires a")) {
                logger.info("Video only available for premium users (in the current region): " + cryptedLink.getCryptedUrl());
                return decryptedLinks;
            }

            // Determine if the video exists
            if (this.br.containsHTML("(<title>Crunchyroll \\- Page Not Found</title>|<p>But we were unable to find the page you were looking for\\. Sorry\\.</p>)")) {
                // not available == offline, no need to show error message
                return decryptedLinks;
            }
            if (br.getURL().contains("crunchyroll.com/showmedia_wall?next=")) {
                logger.info("Link can only be decrypted if you own and add a crunchyroll.com account: " + cryptedLink.getCryptedUrl());
                return decryptedLinks;
            }
            if (br.containsHTML("This video has not been released yet")) {
                logger.info("Video is not released yet -> Cannot decrypt link: " + cryptedLink.getCryptedUrl());
                return decryptedLinks;
            }

            // Get the episode name
            String title = this.nameFromVideoUrl(cryptedLink.getCryptedUrl());
            if (title == null) title = new Regex(cryptedLink.getCryptedUrl(), "/([^<>\"/]+)$").getMatch(0);
            if (title == null) { throw new DecrypterException("Invalid video URL"); }

            // Get the link to the XML file
            final Regex configUrlSearch = this.br.getRegex("\"config_url\":\"(.+?)\"");
            if (!configUrlSearch.matches()) { throw new DecrypterException("Failed to get config url"); }

            final String configUrlDecode = Encoding.htmlDecode(configUrlSearch.getMatch(0));
            final Regex configUrl = new Regex(configUrlDecode, CrhyRllCom.CONFIG_URL);
            if (!configUrl.matches()) { throw new DecrypterException("Invalid config url"); }

            // Get the link to the SWF file
            final Regex swfUrlSearch = this.br.getRegex("swfobject.embedSWF\\(\"(.*?)\"");
            if (!swfUrlSearch.matches()) { throw new DecrypterException("Failed to get SWF url"); }

            final String swfUrlDecode = Encoding.htmlDecode(swfUrlSearch.getMatch(0).replaceAll("\\\\/", "/"));
            final Regex swfUrl = new Regex(swfUrlDecode, CrhyRllCom.SWF_URL);
            if (!swfUrl.matches()) { throw new DecrypterException("Invalid SWF url"); }

            // Find the available qualities by looking for the buttons
            String[] qualities = this.br.getRegex("\\?p([0-9]+)=1").getColumn(0);
            if (qualities == null || qualities.length == 0) {
                qualities = br.getRegex("token=\"showmedia\\.(\\d+)p\"").getColumn(0);
                if (qualities == null || qualities.length == 0) throw new DecrypterException("No qualities found");
            }

            final FilePackage filePackage = FilePackage.getInstance();
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

                thisLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                thisLink.setFinalFileName(filename + EXT_UNKNOWN);
                thisLink.setProperty("quality", qualityValue.getFirstValue());
                thisLink.setProperty("filename", filename);
                thisLink.setProperty("swfdir", swfUrl.getMatch(1));
                thisLink.setProperty("valid", true);

                filePackage.add(thisLink);
                decryptedLinks.add(thisLink);
            }

            // Get subtitles
            br.postPage(configUrlDecode, "current_page=" + cryptedLink.getCryptedUrl());
            final String mediaId = br.getRegex("<media_id>(\\d+)</media_id>").getMatch(0);
            br.postPage("http://www.crunchyroll.com/xml/", "req=RpcApiSubtitle%5FGetListing&media%5Fid=" + mediaId);
            final String[][] subtitles = this.br.getRegex(CrhyRllCom.CONFIG_SUBS).getMatches();

            // Loop through each subtitles xml found
            for (final String[] subtitle : subtitles) {
                final String subUrl = Encoding.htmlDecode(subtitle[1]);
                final String subTitle = subtitle[2];
                String subName = new Regex(subTitle, "\\[[0-9\\s]+\\]\\s*(.+)").getMatch(0);
                if (subName == null) subName = subTitle;

                subName = subName.replace(' ', SEPARATOR).toLowerCase();
                subName = subName.replaceAll("[\\[\\]\\(\\)]+", "");

                final String subFile = title + "." + subName;
                final DownloadLink thisLink = this.createDownloadlink(subUrl);

                thisLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                thisLink.setFinalFileName(subFile + EXT_SUBS);
                thisLink.setProperty("filename", subFile);
                thisLink.setProperty("valid", true);

                filePackage.add(thisLink);
                decryptedLinks.add(thisLink);
            }

            // Add the Android video file (low-res, embedded subtitles)
            final String androidFile = title + SUFFIX_ANDROID;

            final DownloadLink androidLink = this.createDownloadlink("http://www.crunchyroll.com/android_rpc/?req=RpcApiAndroid_GetVideoWithAcl&media_id=" + mediaId);

            androidLink.setBrowserUrl(cryptedLink.getCryptedUrl());
            androidLink.setFinalFileName(androidFile + EXT_UNKNOWN);
            androidLink.setProperty("filename", androidFile);

            filePackage.add(androidLink);
            decryptedLinks.add(androidLink);

        } catch (final IOException e) {
            this.logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
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
        if (!new Regex(videoId, "^[0-9]+$").matches()) { return null; }

        String name = null;
        try {
            // Use a feature where you are redirected to the full-url if you go
            // to a shortened version
            br.setFollowRedirects(false);
            br.getPage("http://www.crunchyroll.com/a/a-" + videoId);
            name = this.nameFromVideoUrl(br.getRedirectLocation());
        } catch (final Throwable e) {
        }

        return name;
    }

    private String nameFromVideoUrl(final String videoUrl) {
        // Get the episode name 'show-episode-1-name'
        final Regex urlReg = new Regex(videoUrl, "crunchyroll\\.com/([\\w\\_\\-]+)/episode\\-([0-9]+)([\\w\\_\\-]*)\\-([0-9]+)");
        if (!urlReg.matches()) { return null; }

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
     * Try and find the Android details for the given link. If the details are successfully found, then set the properties of the link.
     * 
     * @param downloadLink
     *            The DownloadLink file to check
     * @param br
     *            The browser to use to load the XML file with. If null, uses different browser
     */
    public void setAndroid(final DownloadLink downloadLink, Browser br) throws IOException, PluginException {
        if (br == null) {
            br = this.br;
        }

        // Extract the quality code from the url
        final Regex androidUrl = new Regex(downloadLink.getDownloadURL(), CrhyRllCom.ANDROID_URL);
        final String mediaId = androidUrl.getMatch(0);
        if (mediaId == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid URL (could not find media id)"); }

        // If the download does not yet have a filename, set a temporary
        // filename
        String filename = "CrunchyRoll." + mediaId + SUFFIX_ANDROID;
        if (downloadLink.getFinalFileName() == null) {
            downloadLink.setFinalFileName(filename + EXT_UNKNOWN);
        }

        // Load the xml using the spoofed Android headers
        // TODO Randomise UID?
        final Browser androidBr = br.cloneBrowser();
        androidBr.setFollowRedirects(true);
        androidBr.setHeader("X-Device-Uniqueidentifier", "ffffffff-ffff-ffff-ffff-ffffffffffff");
        androidBr.setHeader("X-Device-Manufacturer", "HTC");
        androidBr.setHeader("X-Device-Model", "HTC Desire HD");
        androidBr.setHeader("X-Application-Name", "com.crunchyroll.crunchyroid");
        androidBr.setHeader("X-Device-Product", "htc_ace");
        androidBr.setHeader("X-Device-Is-GoogleTV", "0");
        androidBr.getPage(downloadLink.getDownloadURL());

        // Check if we can actually get the video
        if (androidBr.containsHTML("Video not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Permission denied!"); }
        if (androidBr.containsHTML("Media not found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File does not exist!"); }
        if (!androidBr.containsHTML("\"exception_error_code\":null")) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown XML error!"); }

        // Get the filetype from the JSON
        String filetype = androidBr.getRegex("video(\\.\\w+)").getMatch(0);
        if (filetype == null) {
            filetype = EXT_UNKNOWN;
        }

        // Get the filename, and generate a new one if it doesn't exist
        String oldFilename = downloadLink.getStringProperty("filename");
        if (oldFilename == null) {
            oldFilename = this.nameFromVideoId(mediaId, br);

            if (oldFilename != null) {
                filename = oldFilename + SUFFIX_ANDROID;
                downloadLink.setProperty("filename", filename);
            }
        } else {
            filename = oldFilename;
        }
        downloadLink.setFinalFileName(filename + filetype);

        String videoUrl = androidBr.getRegex("\"video_url\":\"(.+?)\"").getMatch(0);
        if (videoUrl == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to get video URL"); }
        videoUrl = Encoding.htmlDecode(videoUrl.replaceAll("\\\\/", "/"));

        downloadLink.setProperty("videourl", videoUrl);

        // Get the HTTP response headers of the video file to check for
        // validity
        URLConnectionAdapter conn = null;
        try {
            conn = br.openGetConnection(videoUrl);
            final long respCode = conn.getResponseCode();
            final long length = conn.getLongContentLength();
            final String contType = conn.getContentType();
            if (respCode == 200 && contType.startsWith("video")) {
                // File valid, set details
                downloadLink.setDownloadSize(length);
                downloadLink.setProperty("valid", true);
            }
        } finally {
            try {
                conn.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    /**
     * Try and find the RTMP details for the given link. If the details are successfully found, then set the properties of the link.
     * rtmphost = TcUrl. rtmpfile = playpath. rtmpswf = swfVfy (without full path). filename = output filename without extension.
     * qualityname = text definition of the quality found ("360p", "480p", etc).
     * 
     * @param downloadLink
     *            The DownloadLink file to check
     * @param br
     *            The browser to use to load the XML file with. If null, uses different browser
     */
    public void setRTMP(final DownloadLink downloadLink, Browser br) throws IOException, PluginException {
        if (br == null) {
            br = this.br;
        }

        if (downloadLink.getStringProperty("swfdir") == null) {
            downloadLink.setProperty("swfdir", CrhyRllCom.SWF_DIR);
        }

        // Extract the quality code from the url
        final Regex configUrl = new Regex(downloadLink.getDownloadURL(), CrhyRllCom.CONFIG_URL);
        final String qualityStr = configUrl.getMatch(2);
        if (qualityStr == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid URL (could not find quality)"); }

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
        if (qualityObj == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown quality"); }

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
            br.postPage(url, "current_page=" + downloadLink.getDownloadURL());

            // Does the file actually exist?
            if (br.containsHTML("<msg>Media not found</msg>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File does not exist"); }

            // Check if a premium account is needed (and we aren't using one)
            if (br.containsHTML("<upsell>1</upsell>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Premium account required for this quality"); }

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

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}