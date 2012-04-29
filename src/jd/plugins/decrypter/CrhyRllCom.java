package jd.plugins.decrypter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import jd.controlling.JDLogger;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision: 15878 $", interfaceVersion = 2, names = { "crunchyroll.com" }, urls = { "http://(www\\.)?crunchyroll.com/[a-z0-9-]+/[a-z0-9-]+-[0-9]+" }, flags = { 0 })
public class CrhyRllCom extends PluginForDecrypt {

    public static enum DestinationQuality {
        VIDEO360P("360p", new String[] { "60", "10" }),
        VIDEO480P("480p", new String[] { "61", "20" }),
        VIDEO720P("720p", new String[] { "62" }),
        VIDEO1080P("1080p", new String[] { "80" });

        private String   text;
        private String[] val;

        DestinationQuality(final String text, final String[] val) {
            this.text = text;
            this.val = val;
        }

        public String[] getValues() {
            return this.val;
        }

        public String getFirstValue() {
            return this.val[0];
        }

        public String getText() {
            return this.text;
        }

        @Override
        public String toString() {
            return this.text;
        }

    }

    static public final Pattern CONFIG_URL   = Pattern.compile("(http://www.crunchyroll.com/xml/\\?req=RpcApiVideoPlayer_GetStandardConfig&media_id=([0-9]+).*video_quality=)([0-9]*)(.*)", Pattern.CASE_INSENSITIVE);
    static public final Pattern SWF_URL      = Pattern.compile("((http://static.ak.crunchyroll.com/flash/[a-z0-9.]+/)StandardVideoPlayer.swf)", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_HOST    = Pattern.compile("<host>(rtmp.*)</host>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_FILE    = Pattern.compile("<file>(.*?)</file>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_QUAL    = Pattern.compile("<video_encode_quality>(.*?)</video_encode_quality>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_SWF     = Pattern.compile("<default:chromelessPlayerUrl>(ChromelessPlayerApp.swf.*)</default:chromelessPlayerUrl>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_SERIES  = Pattern.compile("<series_title>(.*?)</series_title>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_EPISODE = Pattern.compile("<episode_number>(.*?)</episode_number>", Pattern.CASE_INSENSITIVE);
    static public final Pattern RTMP_TITLE   = Pattern.compile("<episode_title>(.*?)</episode_title>", Pattern.CASE_INSENSITIVE);

    private static final Logger LOG          = JDLogger.getLogger();

    public ArrayList<DownloadLink> decryptIt(final CryptedLink cryptedLink, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        try {
            this.br.setFollowRedirects(true);
            this.br.getPage(cryptedLink.getCryptedUrl());
            if (br.containsHTML("(<title>Crunchyroll \\- Page Not Found</title>|<p>But we were unable to find the page you were looking for\\. Sorry\\.</p>)")) { throw new DecrypterException("This video is not avaible (404)"); }

            String seperator = " ";
            String title = this.br.getRegex("<title>Watch (.+)</title>").getMatch(0);
            if (title == null) {
                Regex urlReg = new Regex(cryptedLink.getCryptedUrl(), "crunchyroll.com/([a-z0-9-]+)/([a-z0-9-]+)-([0-9]+)");
                seperator = "-";
                title = urlReg.getMatch(0) + "-" + urlReg.getMatch(1);
            }
            if (title == null) { throw new DecrypterException("Unable to find episode title"); }

            String configUrlSearch = this.br.getRegex("\"config_url\":\"(.+?)\"").getMatch(0);
            String configUrlDecode = Encoding.htmlDecode(configUrlSearch);
            Regex configUrl = new Regex(configUrlDecode, CONFIG_URL);

            String swfUrlSearch = this.br.getRegex("swfobject.embedSWF\\(\"(.*?)\"").getMatch(0).replaceAll("\\\\/", "/");
            String swfUrlDecode = Encoding.htmlDecode(swfUrlSearch);
            Regex swfUrl = new Regex(swfUrlDecode, SWF_URL);

            String[] qualities = this.br.getRegex("token=\"showmedia.([0-9]+?p)\"").getColumn(0);
            if (qualities.length == 0) { throw new DecrypterException("No qualities found"); }

            for (String quality : qualities) {
                DestinationQuality qualityValue = DestinationQuality.valueOf("VIDEO" + quality.toUpperCase());

                String xmlUrl = configUrl.getMatch(0) + qualityValue.getFirstValue() + configUrl.getMatch(3);
                String filename = title + seperator + quality + ".mp4";

                final DownloadLink thisLink = this.createDownloadlink(xmlUrl);
                thisLink.setBrowserUrl(cryptedLink.getCryptedUrl());
                thisLink.setFinalFileName(filename);

                thisLink.setProperty("filename", filename);
                thisLink.setProperty("seperator", seperator);
                thisLink.setProperty("quality", qualityValue.getFirstValue());
                thisLink.setProperty("swfdir", swfUrl.getMatch(1));
                thisLink.setProperty("valid", true);

                decryptedLinks.add(thisLink);
            }
        } catch (final IOException e) {
            this.br.getHttpConnection().disconnect();
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            return null;
        }
        return decryptedLinks;
    }

    public boolean setRTMP(final DownloadLink downloadLink, Browser br) throws IOException, PluginException {
        if (br == null) {
            br = this.br;
        }

        Regex configUrl = new Regex(downloadLink.getDownloadURL(), CONFIG_URL);
        String qualityStr = configUrl.getMatch(2);
        if (qualityStr == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "Invalid URL (could not find quality)"); }

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
        if (qualityObj == null) { throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown quality"); }

        downloadLink.setName("Crunchyroll " + qualityObj.toString());

        for (String quality : qualityObj.getValues()) {
            String url = configUrl.getMatch(0) + quality + configUrl.getMatch(3);

            br.setFollowRedirects(true);
            br.getPage(url);

            if (downloadLink.getFinalFileName() == null) {
                downloadLink.setFinalFileName("crunchyroll." + configUrl.getMatch(1) + ".mp4");
            }

            if (br.containsHTML("<msg>(Media not found)</msg>")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "File does not exist"); }

            String filename = downloadLink.getStringProperty("filename");
            if (filename == null) {
                final String series = Encoding.htmlDecode(br.getRegex(RTMP_SERIES).getMatch(0));
                final String episode = Encoding.htmlDecode(br.getRegex(RTMP_EPISODE).getMatch(0));
                final String title = Encoding.htmlDecode(br.getRegex(RTMP_TITLE).getMatch(0));

                filename = series + " Episode " + episode + " - " + title + " " + qualityObj.toString() + ".mp4";
                filename = filename.replaceAll("  ", " ").trim();

                downloadLink.setFinalFileName(filename);
                downloadLink.setProperty("filename", filename);
                downloadLink.setProperty("seperator", " ");
            }

            if (br.containsHTML("<upsell>1</upsell>")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, "Quality not available (try using premium)"); }

            final String host = Encoding.htmlDecode(br.getRegex(RTMP_HOST).getMatch(0));
            final String file = Encoding.htmlDecode(br.getRegex(RTMP_FILE).getMatch(0));
            final String swf = br.getRegex(RTMP_SWF).getMatch(0).replaceAll("&amp;", "&");

            final String qual = Encoding.htmlDecode(br.getRegex(RTMP_QUAL).getMatch(0));
            if (!qual.equals(quality)) {
                continue;
            }

            downloadLink.setProperty("rtmphost", host);
            downloadLink.setProperty("rtmpfile", file);
            downloadLink.setProperty("rtmpswf", swf);
            downloadLink.setProperty("qualityname", qual);
            return true;
        }
        return false;
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 100);
    }
}