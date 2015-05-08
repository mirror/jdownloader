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
import jd.http.Browser.BrowserException;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wat.tv" }, urls = { "http://(www\\.)?wat\\.tv/video/.*?\\.html" }, flags = { 0 })
public class WatTv extends PluginForHost {

    public WatTv(final PluginWrapper wrapper) {
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

    @Override
    public String getAGBLink() {
        return "http://www.wat.tv/cgu";
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        String filename = null;
        setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        if (enable_oauth_api) {
            /* thx to: https://github.com/olamedia/medialink/blob/master/mediaDrivers/watTvMediaLinkDriver.php */
            /* xml also possible */
            br.getPage("http://www.wat.tv/interface/oembed/json?url=http%3A%2F%2Fwww.wat.tv%2Fvideo%2F" + downloadLink.getDownloadURL().substring(downloadLink.getDownloadURL().lastIndexOf("/")) + "&oembedtype=wattv");
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            filename = (String) entries.get("title");
            final String html = (String) entries.get("html");
            video_id = new Regex(html, "id=\"wat_(\\d{8})\"").getMatch(0);
        } else {
            try {
                br.getPage(downloadLink.getDownloadURL());
            } catch (final BrowserException e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.getURL().equals("http://www.wat.tv/") || br.containsHTML("<title> WAT TV, vidéos replay musique et films, votre média vidéo \\– Wat\\.tv </title>")) {
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
            if (video_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (filename == null || filename.equals("") || video_id == null) {
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
            /* Contentv4 is used on the website but it returns information that we don't need */
            br2.getPage("http://www.wat.tv/interface/contentv3/" + video_id);
            String quality = "/web/", country = "DE";
            if (br2.containsHTML("\"hasHD\":true")) {
                quality = "/webhd/";
            }
            final String token = computeToken(quality, video_id);
            if (br2.containsHTML("\"geolock\":true")) {
                country = "FR";
            }
            if (enable_hds_workaround_embedplayer) {
                /* Their embed players get http urls for some reason. */
                /* Referer header not needed */
                br2.getHeaders().put("Referer", "http://www.wat.tv/images/v40/LoaderExportV3.swf?revision=4.1.243&baseUrl=www.wat.tv&v40=1&videoId=" + this.video_id + "&playerType=watPlayer&browser=firefox&context=swf2&referer=undefined&ts=nnznqq&oasTag=WAT%2Fnone%2Fp%2Fle-comte-de-bouderbala&embedMode=direct&isEndAd=1");
                getpage = "http://www.wat.tv/get" + quality + video_id + "?token=" + token + "&domain=www.wat.tv&domain2=null&refererURL=%2Fimages%2Fv40%2FLoaderExportV3.swf%3Frevision%3D4.1.243%26baseUrl%3Dwww.wat.tv%26v40%3D1%26videoId%3D" + this.video_id + "%26playerType%3DwatPlayer%26browser%3Dfirefox%26context%3Dswf2%26referer%3Dundefined%26ts%3Dnnznqq%26oasTag%3DWAT%252Fnone%252Fp%252Fle-comte-de-bouderbala%26embedMode%3Ddirect%26isEndAd%3D1&revision=4.1.243&synd=0&helios=1&context=swf2&pub=1&country=" + country + "&sitepage=WAT%2Fnone%2Fp%2Fle-comte-de-bouderbala&lieu=wat&playerContext=CONTEXT_WAT&getURL=1&version=WIN%2017,0,0,169";
            } else {
                /* We'll just leave this out: ?videoId=73rcb */
                /* Referer header not needed */
                br2.getHeaders().put("Referer", "http://www.wat.tv/images/v70/PlayerLite.swf");
                getpage = "http://www.wat.tv/get" + quality + video_id + "?token=" + token + "&domain=www.wat.tv&refererURL=wat.tv&revision=04.00.759%0A&synd=0&helios=1&context=playerWat&pub=1&country=" + country + "&sitepage=WAT%2Fhumour%2Fp%2Fle-comte-de-bouderbala&lieu=wat&playerContext=CONTEXT_WAT&getURL=1&version=WIN%2017,0,0,169";
            }
            br2.getPage(getpage);
            if (br2.containsHTML("No htmlCode read")) {
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