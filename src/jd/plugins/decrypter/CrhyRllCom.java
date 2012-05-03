package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

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

@DecrypterPlugin(revision = "$Revision: 16456 $", interfaceVersion = 2, names = { "crunchyroll.com" }, urls = { "http://(www\\.)?crunchyroll.com/[a-z0-9-]+/[a-z0-9-]+-[0-9]+" }, flags = { 2 })
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

    static public final Pattern CONFIG_SUBS  = Pattern.compile("<subtitles><media_id>([0-9]+?)</media_id><subtitle id='([0-9]+?)' link='(http://www.crunchyroll.com/xml/\\?req=RpcApiSubtitle_GetXml&amp;subtitle_script_id=[0-9]+?)' title='(.+?)'.*/>.*</subtitles>", Pattern.CASE_INSENSITIVE);
    static public final Pattern CONFIG_URL   = Pattern.compile("(http://www.crunchyroll.com/xml/\\?req=RpcApiVideoPlayer_GetStandardConfig&media_id=([0-9]+).*video_quality=)([0-9]*)(.*)", Pattern.CASE_INSENSITIVE);
    static public final int     EPISODE_PAD  = 3;
    static public final Pattern RTMP_EPISODE = Pattern.compile("<episode_number>(.*?)</episode_number>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_FILE    = Pattern.compile("<file>(.*?)</file>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_HOST    = Pattern.compile("<host>(rtmp.*)</host>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_QUAL    = Pattern.compile("<video_encode_quality>(.*?)</video_encode_quality>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_SERIES  = Pattern.compile("<series_title>(.*?)</series_title>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_SWF     = Pattern.compile("<default:chromelessPlayerUrl>(ChromelessPlayerApp.swf.*)</default:chromelessPlayerUrl>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_TITLE   = Pattern.compile("<episode_title>(.*?)</episode_title>", Pattern.CASE_INSENSITIVE);
    static public final String  SWF_DIR      = "http://static.ak.crunchyroll.com/flash/20120315193834.0fa282dfa08cb851004372906bfd7e14/";
    static public final Pattern SWF_URL      = Pattern.compile("((http://static.ak.crunchyroll.com/flash/[a-z0-9.]+/)StandardVideoPlayer.swf)", Pattern.CASE_INSENSITIVE);

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
            if (br.getCookies("crunchyroll.com").isEmpty()) {
                final PluginForHost plugin = JDUtilities.getPluginForHost("crunchyroll.com");
                if (plugin != null) {
                    Account account = AccountController.getInstance().getValidAccount(plugin);
                    if (account != null) {
                        try {
                            ((jd.plugins.hoster.CrunchyRollCom) plugin).login(account, this.br, false, true);
                        } catch (final Exception e) {
                        }
                    }
                }
            }

            // Load the linked page
            this.br.setFollowRedirects(true);
            this.br.getPage(cryptedLink.getCryptedUrl());

            // Determine if the video exists
            if (br.containsHTML("(<title>Crunchyroll \\- Page Not Found</title>|<p>But we were unable to find the page you were looking for\\. Sorry\\.</p>)")) { throw new DecrypterException("This video is not avaible (404)"); }

            // Get the episode name 'show-episode-1-name'
            Regex urlReg = new Regex(cryptedLink.getCryptedUrl(), "crunchyroll.com/([a-z0-9-]+)/episode-([0-9]+)([a-z0-9-]*)-([0-9]+)");
            if (!urlReg.matches()) { throw new DecrypterException("Unable to find episode title"); }
            String separator = "-";
            String vidSeries = urlReg.getMatch(0);
            String vidEpisode = urlReg.getMatch(1);
            String vidTitle = urlReg.getMatch(2);

            while (vidEpisode.length() < EPISODE_PAD) {
                vidEpisode = "0" + vidEpisode;
            }
            String title = vidSeries + "-" + vidEpisode + vidTitle;

            // Get the link to the XML file
            String configUrlSearch = this.br.getRegex("\"config_url\":\"(.+?)\"").getMatch(0);
            String configUrlDecode = Encoding.htmlDecode(configUrlSearch);
            Regex configUrl = new Regex(configUrlDecode, CONFIG_URL);

            // Get the link to the SWF file
            String swfUrlSearch = this.br.getRegex("swfobject.embedSWF\\(\"(.*?)\"").getMatch(0).replaceAll("\\\\/", "/");
            String swfUrlDecode = Encoding.htmlDecode(swfUrlSearch);
            Regex swfUrl = new Regex(swfUrlDecode, SWF_URL);

            // Find the available qualities by looking for the buttons
            String[] qualities = this.br.getRegex("\\?p([0-9]+)=1").getColumn(0);
            if (qualities.length == 0) { throw new DecrypterException("No qualities found"); }

            final FilePackage filePackage = FilePackage.getInstance();
            filePackage.setProperty("ALLOW_MERGE", true);
            filePackage.setName(title);
            for (String quality : qualities) {
                quality += "p"; // '360' => '360p'

                // Try and find the RTMP quality codes
                jd.plugins.decrypter.CrhyRllCom.DestinationQuality qualityValue = null;
                if (quality.equals(jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO360P.toString())) {
                    qualityValue = jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO360P;
                } else if (quality.equals(jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO480P.toString())) {
                    qualityValue = jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO480P;
                } else if (quality.equals(jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO720P.toString())) {
                    qualityValue = jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO720P;
                } else if (quality.equals(jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO1080P.toString())) {
                    qualityValue = jd.plugins.decrypter.CrhyRllCom.DestinationQuality.VIDEO1080P;
                }
                if (qualityValue == null) {
                    continue;
                }

                String xmlUrl = configUrl.getMatch(0) + qualityValue.getFirstValue() + configUrl.getMatch(3);
                String filename = title + separator + quality;

                final DownloadLink thisLink = this.createDownloadlink(xmlUrl);

                thisLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                thisLink.setFinalFileName(filename + ".tmp");
                thisLink.setProperty("quality", qualityValue.getFirstValue());
                thisLink.setProperty("filename", filename);
                thisLink.setProperty("separator", separator);
                thisLink.setProperty("swfdir", swfUrl.getMatch(1));
                thisLink.setProperty("valid", true);

                filePackage.add(thisLink);
                decryptedLinks.add(thisLink);
            }

            // Get subtitles
            this.br.getPage(configUrlDecode);
            String[][] subtitles = this.br.getRegex(CONFIG_SUBS).getMatches();

            // Loop through each subtitles xml found
            for (String[] subtitle : subtitles) {
                String mediaId = subtitle[0];
                String subId = subtitle[1];
                String subUrl = Encoding.htmlDecode(subtitle[2]);
                String subTitle = subtitle[3];
                String subName = new Regex(subTitle, "\\[[0-9 ]+\\] (.+)").getMatch(0);

                subName = subName.replaceAll("[ ]", separator).toLowerCase();
                subName = subName.replaceAll("[^a-z0-9-]", "");

                String subFile = title + separator + subName;

                final DownloadLink thisLink = this.createDownloadlink(subUrl);

                thisLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                thisLink.setFinalFileName(subFile + ".ass");
                thisLink.setProperty("filename", subFile);
                thisLink.setProperty("mediaid", mediaId);
                thisLink.setProperty("id", subId);
                thisLink.setProperty("title", subTitle);
                thisLink.setProperty("valid", true);

                filePackage.add(thisLink);
                decryptedLinks.add(thisLink);
            }
        } catch (final IOException e) {
            this.br.getHttpConnection().disconnect();
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            return null;
        }
        return decryptedLinks;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }

    /**
     * Try and find the RTMP details for the given link. If the details are
     * successfully found, then set the properties of the link. rtmphost =
     * TcUrl. rtmpfile = playpath. rtmpswf = swfVfy (without full path).
     * filename = output filename without extension. separator = character
     * separator for the filename. qualityname = text definition of the quality
     * found ("360p", "480p", etc).
     * 
     * @param downloadLink
     *            The DownloadLink file to check
     * @param br
     *            The browser to use to load the XML file with. If null, uses
     *            different browser
     */
    public void setRTMP(final DownloadLink downloadLink, Browser br) throws IOException, PluginException {
        if (br == null) {
            br = this.br;
        }

        if (downloadLink.getStringProperty("swfdir") == null) {
            downloadLink.setProperty("swfdir", SWF_DIR);
        }

        // Extract the quality code from the url
        Regex configUrl = new Regex(downloadLink.getDownloadURL(), CONFIG_URL);
        String qualityStr = configUrl.getMatch(2);
        if (qualityStr == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Invalid URL (could not find quality)"); }

        // Try and find that quality code in the known codes
        DestinationQuality qualityObj = null;
        for (DestinationQuality quality : DestinationQuality.values()) {
            for (String value : quality.getValues()) {
                if (qualityStr.equals(value)) {
                    qualityObj = quality;
                    break;
                }
            }
            if (qualityObj != null) break;
        }
        if (qualityObj == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown quality"); }

        // Loop through all of the quality codes for the given quality
        for (String quality : qualityObj.getValues()) {
            // Get the XML file for the given quality code
            String url = configUrl.getMatch(0) + quality + configUrl.getMatch(3);
            br.setFollowRedirects(true);
            br.getPage(url);

            // If the download does not yet have a filename, set a temporary
            // filename
            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName("CrunchyRoll." + configUrl.getMatch(1));
            }

            // Does the file actually exist?
            if (br.containsHTML("<msg>(Media not found)</msg>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File does not exist"); }

            // Get the filename, and generate a new one if it doesn't exist
            String filename = downloadLink.getStringProperty("filename");
            if (filename == null) {
                final String series = Encoding.htmlDecode(br.getRegex(RTMP_SERIES).getMatch(0));
                final String title = Encoding.htmlDecode(br.getRegex(RTMP_TITLE).getMatch(0));
                String episode = Encoding.htmlDecode(br.getRegex(RTMP_EPISODE).getMatch(0));

                while (episode.length() < EPISODE_PAD) {
                    episode = "0" + episode;
                }

                // 'show-001-name-360p'
                filename = series + " " + episode + " " + title + " " + qualityObj.toString();
                filename = filename.trim().replace(' ', '-').toLowerCase().replaceAll("[^a-z0-9-]", "");

                downloadLink.setFinalFileName(filename + ".unk");
                downloadLink.setProperty("filename", filename);
                downloadLink.setProperty("separator", "-");
            }

            // Check if a premium account is needed (and we aren't using one)
            if (br.containsHTML("<upsell>1</upsell>")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Quality not available (try using premium)"); }

            // Check if the quality found is actually the one we wanted
            final String qual = Encoding.htmlDecode(br.getRegex(RTMP_QUAL).getMatch(0));
            if (!qual.equals(quality)) {
                continue;
            }

            // Get the needed RTMP details
            final String host = Encoding.htmlDecode(br.getRegex(RTMP_HOST).getMatch(0));
            final String file = Encoding.htmlDecode(br.getRegex(RTMP_FILE).getMatch(0));
            final String swf = br.getRegex(RTMP_SWF).getMatch(0).replaceAll("&amp;", "&");

            String filetype = new Regex(file, "^(.+):.*").getMatch(0);
            if (filetype == null) {
                filetype = "unk";
            }

            downloadLink.setFinalFileName(filename + "." + filetype);

            downloadLink.setProperty("rtmphost", host);
            downloadLink.setProperty("rtmpfile", file);
            downloadLink.setProperty("rtmpswf", swf);
            downloadLink.setProperty("qualityname", qual);
            return;
        }
        downloadLink.setAvailable(false);
        throw new PluginException(LinkStatus.ERROR_PREMIUM, "Quality not available (try using premium)");
    }
}
