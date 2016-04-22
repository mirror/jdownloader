//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.ByteArrayInputStream;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.jdownloader.downloader.hds.HDSDownloader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tf1.fr" }, urls = { "https?://(?:www\\.)?(wat\\.tv/video/.*?|tf1\\.fr/.+/videos/[A-Za-z0-9\\-_]+)\\.html" }, flags = { 0 })
public class Tf1Fr extends PluginForHost {

    public Tf1Fr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    // private String getTS(long ts) {
    // int t = (int) Math.floor(ts / 1000);
    // return Integer.toString(t, 36);
    // }

    private final boolean enable_hds_workaround_android     = false;
    private final boolean enable_hds_workaround_embedplayer = true;
    private final boolean enable_oauth_api                  = false;

    private String        video_id                          = null;
    private String        uvid                              = null;

    @Override
    public String getAGBLink() {
        return "http://www.wat.tv/cgu";
    }

    @Override
    public String rewriteHost(String host) {
        if ("wat.tv".equals(getHost())) {
            if (host == null || "freakshare.net".equals(host)) {
                return "tf1.fr";
            }
        }
        return super.rewriteHost(host);
    }

    /* 2016-04-22: Changed domain from wat.tv to tf1.fr - everything else mostly stays the same */
    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        video_id = null;
        uvid = null;
        String filename = null;
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        if (enable_oauth_api) {
            /* thx to: https://github.com/olamedia/medialink/blob/master/mediaDrivers/watTvMediaLinkDriver.php */
            /* xml also possible */
            br.getPage("http://www.wat.tv/interface/oembed/json?url=http%3A%2F%2Fwww.wat.tv%2Fvideo%2F" + downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/")) + "&oembedtype=wattv");
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            filename = (String) entries.get("title");
            final String html = (String) entries.get("html");
            video_id = new Regex(html, "id=\"wat_(\\d{8})\"").getMatch(0);
        } else {
            br.getPage(downloadLink.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404 || br.getHttpConnection().getResponseCode() == 410) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = br.getRegex("<meta name=\"name\" content=\"(.*?)\"").getMatch(0);
            if (filename == null || filename.equals("")) {
                filename = br.getRegex("\\'premium:([^<>\"\\']*?)\\'").getMatch(0);
            }
            if (filename == null || filename.equals("")) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"").getMatch(0);
            }
            video_id = br.getRegex("<meta property=\"og:video(:secure_url)?\" content=\"[^\"]+(\\d{6,8})\">").getMatch(1);
            if (video_id == null) {
                video_id = br.getRegex("xtpage = \"[^;]+video\\-(\\d{6,8})\";").getMatch(0);
            }
        }
        if (filename == null || filename.equals("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        filename = encodeUnicode(filename);
        if (filename.endsWith(" - ")) {
            filename = filename.replaceFirst(" \\- $", "");
        }
        filename = Encoding.htmlDecode(filename.trim());
        downloadLink.setName(filename + ".mp4");
        return AvailableStatus.TRUE;
    }

    public String getFinalLink() throws Exception {
        String videolink = null;
        final Browser br2 = br.cloneBrowser();
        if (enable_hds_workaround_android) {
            /* Thanks to: https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/wat.py */
            /* Android devices get http urls. This can also be used to avoid GEO-blocks! */
            videolink = getAndroidURL(video_id);
        } else {
            String getpage = null;
            /*
             * Contentv4 is used on the website but it returns information that we don't need --> 2016-04-22: contentv3 no longer works,
             * returns 400
             */
            br2.getPage("http://www.wat.tv/interface/contentv3/" + uvid);
            String quality = "/web/", country = "DE";
            if (br2.containsHTML("\"hasHD\":true")) {
                quality = "/webhd/";
            }
            final String token = computeToken(quality, uvid);
            if (br2.containsHTML("\"geolock\":true")) {
                country = "FR";
            }
            if (enable_hds_workaround_embedplayer) {
                /* Their embed players get http urls for some reason. */
                /* Referer header not needed */
                br2.getHeaders().put("Referer", "http://www.wat.tv/images/v40/LoaderExportV3.swf?revision=4.1.243&baseUrl=www.wat.tv&v40=1&videoId=" + this.video_id + "&playerType=watPlayer&browser=firefox&context=swf2&referer=undefined&ts=nnznqq&oasTag=WAT%2Fnone%2Fp%2Fle-comte-de-bouderbala&embedMode=direct&isEndAd=1");
                getpage = "http://www.wat.tv/get" + quality + uvid + "?token=" + token + "&domain=www.tf1.fr&domain2=null&refererURL=wat.tv&revision=04.00.829%0A&synd=0&helios=1&context=swf2&pub=1&country=" + country + "&sitepage=nt1.tv&lieu=wat&playerContext=CONTEXT_WAT&getURL=1&version=WIN%2021,0,0,213";
            } else {
                /* We'll just leave this out: ?videoId=73rcb */
                /* Referer header not needed */
                br2.getHeaders().put("Referer", "http://www.wat.tv/images/v70/PlayerLite.swf");
                getpage = "http://www.wat.tv/get" + quality + uvid + "?token=" + token + "&domain=www.tf1.fr&refererURL=wat.tv&revision=04.00.829%0A&synd=0&helios=1&context=playerWat&pub=1&country=" + country + "&sitepage=nt1.tv&lieu=wat&playerContext=CONTEXT_WAT&getURL=1&version=WIN%2021,0,0,213";
            }
            br2.getPage(getpage);
            if (br2.toString().length() < 25) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Video not available in your country!");
            }
            videolink = br2.toString();
            if (videolink == null || (!videolink.startsWith("http") && !videolink.startsWith("rtmp"))) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            videolink = Encoding.htmlDecode(videolink.trim());
            if (videolink.startsWith("http")) {
                final URLConnectionAdapter con = br2.openGetConnection(videolink);
                if (con.getResponseCode() == 404 || con.getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.WatTv.CountryBlocked", "This video isn't available in your country!"));
                }
            }
        }
        return videolink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (this.video_id == null) {
            /* 2016-04-22: Domainchange from wat.tv to tf1.fr so now the way to get the video_id is slightly different */
            final String embedframe_id = this.br.getRegex("/embedframe/([^<>\"\\'/]+)").getMatch(0);
            if (embedframe_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage("http://www.wat.tv/embedframe/" + embedframe_id);
            video_id = br.getRegex("videoId=([A-Za-z0-9]+)").getMatch(0);
            uvid = this.br.getRegex("iphoneId[\t\n\r ]*?:[\t\n\r ]*?\"([^\"]+)\"").getMatch(0);
            if (uvid == null) {
                uvid = this.br.getRegex("UVID=([^<>\"\\&]+)").getMatch(0);
            }
        }
        if (video_id == null || uvid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String finallink = getFinalLink();
        if (finallink.startsWith("rtmp")) {
            /* Old */
            if (System.getProperty("jd.revision.jdownloaderrevision") == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "JD2 BETA needed!");
            }
            /**
             * NOT WORKING IN RTMPDUMP
             */
            final String nw = "rtmpdump";
            if (nw.equals("rtmpdump")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Not supported yet!");
            }

            finallink = finallink.replaceAll("^.*?:", "rtmpe:");
            finallink = finallink.replaceAll("watestreaming/", "watestreaming/#");

            dl = new RTMPDownload(this, downloadLink, finallink);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setUrl(finallink.substring(0, finallink.indexOf("#")));
            rtmp.setPlayPath(finallink.substring(finallink.indexOf("#") + 1));
            rtmp.setApp(new Regex(finallink.substring(0, finallink.indexOf("#")), "[a-zA-Z]+://.*?/(.*?)$").getMatch(0));
            rtmp.setSwfVfy("http://www.wat.tv/images/v40/PlayerWat.swf");
            rtmp.setResume(true);

            ((RTMPDownload) dl).startDownload();
        } else if (finallink.contains(".f4m?")) {
            // HDS
            br.getPage(finallink);
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
            NodeList mediaUrls = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);
            Node media;
            for (int j = 0; j < mediaUrls.getLength(); j++) {
                media = mediaUrls.item(j);

                String temp = getAttByNamedItem(media, "url");
                if (temp != null) {
                    // finallink = Request.getLocation(temp, br.getRequest());
                    finallink = temp;
                    break;
                }
            }
            dl = new HDSDownloader(downloadLink, br, finallink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getAndroidURL(final String videoID) {
        return "http://wat.tv/get/android5/" + videoID + ".mp4";
    }

    private String computeToken(String quality, final String videoID) {
        quality += videoID;
        // String salt = String.valueOf(Integer.toHexString(Integer.parseInt(ts, 36)));
        // while (salt.length() < 8) {
        // salt = "0" + salt;
        // }
        long timestamp = System.currentTimeMillis();
        try {
            final Browser br2 = br.cloneBrowser();
            br2.getPage("http://www.wat.tv/servertime?" + videoID);
            timestamp = Long.parseLong(br2.toString().split("\\|")[0]);
        } catch (final Throwable e) {
            logger.warning("Failed to get server timestamp");
        }
        final String timestamp_hex = Long.toHexString(timestamp);
        final String key = "9b673b13fa4682ed14c3cfa5af5310274b514c4133e9b3a81e6e3aba009l2564";
        return JDHash.getMD5(key + quality + timestamp_hex) + "/" + timestamp_hex;
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /**
     * lets try and prevent possible NPE from killing the progress.
     *
     * @author raztoki
     * @param n
     * @param item
     * @return
     */
    private String getAttByNamedItem(final Node n, final String item) {
        final String t = n.getAttributes().getNamedItem(item).getTextContent();
        return (t != null ? t.trim() : null);
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}