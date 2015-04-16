//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.io.InputStream;
import java.net.URL;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.jdownloader.downloader.hds.HDSDownloader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rtlnow.rtl.de", "rtlnitronow.de", "voxnow.de", "superrtlnow.de", "rtl2now.rtl2.de", "n-tvnow.de" }, urls = { "http://(www\\.)?rtl\\-now\\.rtl\\.de/([\\w-]+/)?[\\w-]+\\.php\\?(container_id|player|film_id)=.+", "http://(www\\.)?rtlnitronow\\.de/([\\w-]+/)?[\\w-]+\\.php\\?(container_id|player|film_id)=.+", "http://(www\\.)?voxnow\\.de//?([\\w-]+/)?[\\w-]+\\.php\\?(container_id|player|film_id)=.+", "http://(www\\.)?superrtlnow\\.de/([\\w-]+/)?[\\w-]+\\.php\\?(container_id|player|film_id)=.+", "http://(www\\.)?rtl2now\\.rtl2\\.de/([\\w-]+/)?[\\w-]+\\.php\\?(container_id|player|film_id)=.+", "http://(www\\.)?n\\-tvnow\\.de/([\\w-]+/)?[\\w-]+\\.php\\?(container_id|player|film_id)=.+" }, flags = { 32, 32, 32, 32, 32, 32 })
public class RTLnowDe extends PluginForHost {

    public RTLnowDe(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /* Tags: rtl-interactive.de, RTL */
    /* General information: The "filmID" is a number which is usually in the html as "film_id" or in the XML 'generate' URL as "para1" */
    /* https?://(www\\.)?<host>/hds/videos/<filmID>/manifest\\-hds\\.f4m */
    private String               HDSTYPE_NEW          = "https?://(www\\.)?[a-z0-0\\-\\.]+/hds/videos/\\d+/manifest\\-hds\\.f4m";
    /*
     * http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/abr/videos/<seriesID (same for app episodes of one series>/<videoIUD(same for all
     * qualities/versions of a video)>/V_\\d+_[A-Z0-9]+_16-\\d+_\\d+_abr\\-<bitrate - usually 550, 1000 or
     * 1500)>_[a-f0-9]{30}\\.mp4\\.f4m\\?cb=\\d+
     */
    private String               HDSTYPE_NEW_DETAILED = "http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/abr/videos/\\d+/\\d+/V_\\d+_[A-Z0-9]+_16-\\d+_\\d+_abr\\-\\d+_[a-f0-9]{30}\\.mp4\\.f4m\\?cb=\\d+";
    /*
     * http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/[^/]+/videos/<seriesID (same for app episodes of one
     * series>/V_\\d+_[A-Z0-9]+_E\\d+_\\d+_h264-mq_<[a-f0-9] usually {30,}>\\.f4v\\.f4m\\?ts=\\d+
     */
    // http://hds.fra.rtlnow.de/hds-vod-enc/rtlnow/videos/7947/V_680273_CWRW_E68173_116557_h264-mq_433c5d3b9df8489a7e62bb68ff11eff.f4v.f4m?ts=1429140440
    private String               HDSTYPE_OLD          = "http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/[^/]+/videos/\\d+/V_\\d+_[A-Z0-9]+_E\\d+_\\d+_h264-mq_[a-f0-9]+\\.f4v\\.f4m\\?ts=\\d+";
    private Document             doc;
    private static final boolean ALLOW_RTMP           = true;
    private Account              currAcc              = null;
    private DownloadLink         currDownloadLink     = null;

    /* Thx https://github.com/bromix/plugin.video.rtl-now.de/blob/master/resources/lib/rtlinteractive/client.py */
    // private String apiUrl = null;
    // private String apiSaltPhone = null;
    // private String apiSaltTablet = null;
    // private String apiKeyPhone = null;
    // private String apiKeyTablet = null;
    // private String apiID = null;
    //
    // private void initAPI() throws PluginException {
    // final String currHost = this.currDownloadLink.getHost();
    // if (currHost.equals("rtlnow.rtl.de")) {
    // apiUrl = "https://rtl-now.rtl.de/";
    // apiSaltPhone = "ba647945-6989-477b-9767-870790fcf552";
    // apiSaltTablet = "ba647945-6989-477b-9767-870790fcf552";
    // apiKeyPhone = "46f63897-89aa-44f9-8f70-f0052050fe59";
    // apiKeyTablet = "56f63897-89aa-44f9-8f70-f0052050fe59";
    // apiID = "9";
    //
    // br.getHeaders().put("X-App-Name", "RTL NOW App");
    // br.getHeaders().put("X-Device-Type", "rtlnow_android");
    // br.getHeaders().put("X-App-Version", "1.3.1");
    // } else if (currHost.equals("voxnow.de")) {
    // apiUrl = "https://www.voxnow.de/";
    // apiSaltPhone = "9fb130b5-447e-4bbc-a44a-406f2d10d963";
    // apiSaltTablet = "0df2738e-6fce-4c44-adaf-9981902de81b";
    // apiKeyPhone = "b11f23ac-10f1-4335-acb8-ebaaabdb8cde";
    // apiKeyTablet = "2e99d88e-088e-4108-a319-c94ba825fe29";
    // apiID = "41";
    //
    // br.getHeaders().put("X-App-Name", "VOX NOW App");
    // br.getHeaders().put("X-Device-Type", "voxnow_android");
    // br.getHeaders().put("X-App-Version", "1.3.1");
    // } else if (currHost.equals("rtl2now.rtl2.de")) {
    // apiUrl = "https://rtl2now.rtl2.de/";
    // apiSaltPhone = "9be405a6-2d5c-4e62-8ba0-ba2b5f11072d";
    // apiSaltTablet = "4bfab4aa-705a-4e8c-b1a7-b551b1b2613f";
    // apiKeyPhone = "26c0d1ac-e6a0-4df9-9f79-e07727f33380";
    // apiKeyTablet = "83bbc955-c96e-4b50-b263-bc7bcbcdf8c8";
    // apiID = "37";
    //
    // br.getHeaders().put("X-App-Name", "RTL II NOW App");
    // br.getHeaders().put("X-Device-Type", "rtl2now_android");
    // br.getHeaders().put("X-App-Version", "1.3.1");
    // } else if (currHost.equals("n-tvnow.de")) {
    // apiUrl = "https://www.n-tvnow.de/";
    // apiSaltPhone = "ba647945-6989-477b-9767-870790fcf552";
    // apiSaltTablet = "ba647945-6989-477b-9767-870790fcf552";
    // apiKeyPhone = "46f63897-89aa-44f9-8f70-f0052050fe59";
    // apiKeyTablet = "56f63897-89aa-44f9-8f70-f0052050fe59";
    // apiID = "49";
    //
    // br.getHeaders().put("X-App-Name", "N-TV NOW App");
    // br.getHeaders().put("X-Device-Type", "ntvnow_android");
    // br.getHeaders().put("X-App-Version", "1.3.1");
    // } else {
    // /* Unsupported host */
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // br.getHeaders().put("User-Agent",
    // "Mozilla/5.0 (Linux; Android 4.4.2; GT-I9505 Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36");
    // }
    //
    // private void apiperformrequest(final String path, String params) {
    // final String requestUrl = this.apiUrl + path;
    // }
    //
    // private String apiCalculateToken(final long timestamp, final String params) throws NoSuchAlgorithmException {
    // final StringBuilder sb = new StringBuilder();
    // sb.append(this.apiKeyTablet);
    // sb.append(";");
    // sb.append(this.apiSaltTablet);
    // sb.append(";");
    // sb.append(Long.toString(timestamp));
    //
    // final String[] paramslist = params.split("&");
    // for (final String parampair : paramslist) {
    // final String[] parPAIR = parampair.split("=");
    // sb.append(";");
    // sb.append(parPAIR[1]);
    // }
    //
    // if (params.length() == 0) {
    // sb.append(";");
    // }
    //
    // final MessageDigest md = MessageDigest.getInstance("md5");
    // md.update(sb.toString().getBytes());
    // /* TODO */
    //
    // String token = "";
    // try {
    // } catch (final Throwable e) {
    // token = "";
    // }
    //
    // return token;
    // }
    //
    // private void apiGet_film_details(final String filmID) {
    // final String params = "filmid=" + filmID;
    // apiperformrequest("/api/query/json/content.film_details", params);
    // }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currAcc = acc;
        this.currDownloadLink = dl;
    }

    @SuppressWarnings("deprecation")
    private void download(final DownloadLink downloadLink) throws Exception {
        String rtmp_playpath = downloadLink.getStringProperty("rlnowrtmpplaypath", null);
        rtmp_playpath = null;
        String contentUrl = br.getRegex("data:\'(.*?)\'").getMatch(0);
        final String ivw = br.getRegex("ivw:\'(.*?)\',").getMatch(0);
        final String client = br.getRegex("id:\'(.*?)\'").getMatch(0);
        final String swfurl = br.getRegex("swfobject\\.embedSWF\\(\"(.*?)\",").getMatch(0);
        String dllink = null;
        if (contentUrl == null || ivw == null || client == null || swfurl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        contentUrl = Encoding.urlDecode(downloadLink.getHost() + contentUrl, true);
        if (contentUrl != null) {
            contentUrl = "http://" + contentUrl;
            downloadLink.setProperty("rlnowcontenturl", contentUrl);

            XPath xPath = xmlParser(contentUrl + "&ts=" + System.currentTimeMillis() / 1000);
            final String query = "/data/playlist/videoinfo";

            dllink = xPath.evaluate(query + "/filename", doc);
            // final String fkcont = xPath.evaluate("/data/fkcontent", doc);
            // final String timetp = xPath.evaluate("/data/timetype", doc);
            // final String season = xPath.evaluate("/data/season", doc);
        }
        if (rtmp_playpath == null && dllink != null && dllink.matches(HDSTYPE_OLD)) {
            rtmp_playpath = new Regex(dllink, "(\\d+/V_[^<>\"/]*?\\.(?:f4v|mp4))").getMatch(0);
            downloadLink.setProperty("rlnowdllink", dllink);
        }
        if ((dllink != null && dllink.startsWith("rtmp")) || rtmp_playpath != null && ALLOW_RTMP) {
            /* Either we already got rtmp urls or we can try to build them via the playpath-part of our HDS manifest url. */
            String rtmpurl = null;
            // rtmp_playpath = rtmp_playpath.replace(".mp4", ".f4v");
            final String host = downloadLink.getHost();
            String app = null;
            if (dllink != null && dllink.startsWith("rtmp")) {
                /* Old rtmpe links, sometimes still existant --> Extract playpath */
                rtmp_playpath = "mp4:" + new Regex(dllink, "rtmpe?://[^/]+/[^/]+/(.+)").getMatch(0);
            } else {
                rtmp_playpath = "mp4:" + rtmp_playpath;
            }
            app = new Regex(host, "([a-z0-9\\-]+now)").getMatch(0);
            if (app == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            app = app.replace("-", "");
            /* Correct app for some rare cases */
            if (app.equals("rtlnitronow")) {
                app = "nitronow";
            }
            /* Either use fms-fra[1-32].rtl.de or just fms.rtl.de */
            rtmpurl = "rtmpe://fms.rtl.de/" + app + "/";

            /* Save the playpath for future usage. */
            downloadLink.setProperty("rlnowrtmpplaypath", rtmp_playpath);
            downloadLink.setProperty("FLVFIXER", true);
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(rtmp_playpath);
            rtmp.setPageUrl(downloadLink.getDownloadURL());
            rtmp.setSwfVfy("http://cdn.static-fra.de/now/vodplayer.swf");
            rtmp.setFlashVer("WIN 14,0,0,145");
            rtmp.setApp(app);
            rtmp.setUrl(rtmpurl);
            rtmp.setResume(true);
            rtmp.setRealTime();
            if (!getPluginConfig().getBooleanProperty("DEFAULTTIMEOUT", false)) {
                rtmp.setTimeOut(-1);
            }
            ((RTMPDownload) dl).startDownload();

        } else {
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (dllink.matches(this.HDSTYPE_NEW)) {
                logger.info("2nd attempt to get final hds url");
                /* TODO */
                if (true) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final XPath xPath = xmlParser(dllink);
                final NodeList nl = (NodeList) xPath.evaluate("/manifest/media", doc, XPathConstants.NODESET);
                final Node n = nl.item(0);
                dllink = n.getAttributes().getNamedItem("href").getTextContent();
            }
            br.getPage(dllink);
            String hds = parseManifest();

            dl = new HDSDownloader(downloadLink, br, hds);
            dl.startDownload();

        }
    }

    private String parseManifest() {
        try {

            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();

            Document d = parser.parse(new ByteArrayInputStream(br.toString().getBytes("UTF-8")));
            NodeList nl = (NodeList) xPath.evaluate("/manifest/media", d, XPathConstants.NODESET);

            for (int i = 0; i < nl.getLength(); i++) {
                Node n = nl.item(i);
                String streamId = null;
                String bootstrapInfoId = null;
                String drmAdditionalHeaderId = null;
                String url = null;
                if (n.getAttributes().getNamedItem("url") != null) {
                    /* Crypted */
                    url = n.getAttributes().getNamedItem("url").getTextContent();
                    streamId = n.getAttributes().getNamedItem("streamId").getTextContent();
                    bootstrapInfoId = n.getAttributes().getNamedItem("bootstrapInfoId").getTextContent();
                    drmAdditionalHeaderId = n.getAttributes().getNamedItem("drmAdditionalHeaderId").getTextContent();
                } else {
                    /* Uncrypted */
                    url = n.getAttributes().getNamedItem("href").getTextContent();
                }

                if (url.startsWith("http")) {
                    return url;
                } else {
                    String base = br.getBaseURL();
                    return base + url;
                }
                // System.out.println(n);
                // String tc = n.getTextContent();
                // String media = xPath.evaluate("metadata", n).trim();
                // byte[] mediaB = Base64.decode(media);
                // media = new String(mediaB, "UTF-8");
                // System.out.println(media);

            }
            System.out.println(1);
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {

            System.out.println(1);
        }
        return null;
    }

    @Override
    public String getAGBLink() {
        return "http://rtl-now.rtl.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        final String redirect = br.getRegex("window\\.location\\.href = \"(/[^<>\"]*?)\"").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        if (br.containsHTML("<\\!\\-\\- Payment\\-Teaser \\-\\->")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Download nicht möglich (muss gekauft werden)");
        }
        final String ageCheck = br.getRegex("(Aus Jugendschutzgründen nur zwischen \\d+ und \\d+ Uhr abrufbar\\!)").getMatch(0);
        if (ageCheck != null) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ageCheck, 10 * 60 * 60 * 1000l);
        }
        download(downloadLink);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setConstants(null, downloadLink);
        setBrowserExclusive();
        final String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (br.containsHTML("<\\!\\-\\- Payment\\-Teaser \\-\\->")) {
            downloadLink.getLinkStatus().setStatusText("Download nicht möglich (muss gekauft werden)");
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\">").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String folge = br.getRegex("Folge: '(.*?)'").getMatch(0);
        if (folge == null) {
            folge = new Regex(dllink, "folge\\-(\\d+)").getMatch(0);
        }
        if (folge != null && filename.contains(folge)) {
            filename = filename.substring(0, filename.lastIndexOf("-")).trim();
        }
        final String season = dllink.contains("season=") ? new Regex(dllink, "season=(\\d+)").getMatch(0) : "0";
        if (!season.equals("0")) {
            filename += " - Staffel " + season;
        }
        filename = filename.trim();

        if (folge == null) {
            return AvailableStatus.FALSE;
        }
        folge = folge.trim();
        if (folge.endsWith(".")) {
            folge = folge.substring(0, folge.length() - 1);
        }
        try {
            if (FilePackage.isDefaultFilePackage(downloadLink.getFilePackage())) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(filename.replaceAll(folge, ""));
                fp.add(downloadLink);
            }
        } catch (final Throwable e) {
        }
        downloadLink.setName(filename + "__" + folge + ".flv");
        return AvailableStatus.TRUE;
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

    private XPath xmlParser(final String linkurl) throws Exception {
        try {
            final URL url = new URL(linkurl);
            final InputStream stream = url.openStream();
            final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            final XPath xPath = XPathFactory.newInstance().newXPath();
            try {
                doc = parser.parse(stream);
                return xPath;
            } finally {
                try {
                    stream.close();
                } catch (final Throwable e) {
                }
            }
        } catch (final Throwable e2) {
            return null;
        }
    }

    // private long crc32Hash(final String wahl) throws UnsupportedEncodingException {
    // String a = Long.toString(System.currentTimeMillis()) + Double.toString(Math.random());
    // if ("session".equals(wahl)) {
    // a = Long.toString(System.currentTimeMillis()) + Double.toString(Math.random()) + Long.toString(Runtime.getRuntime().totalMemory());
    // }
    // final CRC32 c = new CRC32();
    // c.update(a.getBytes("UTF-8"));
    // return c.getValue();
    // }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), "DEFAULTTIMEOUT", JDL.L("plugins.hoster.rtlnowde.enabledeafulttimeout", "Enable default timeout?")).setDefaultValue(false));
    }

}