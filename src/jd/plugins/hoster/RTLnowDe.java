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
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import org.jdownloader.downloader.hls.HLSDownloader;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "nowtv.de" }, urls = { "https?://(www\\.)?nowtv\\.de/(?:rtl|vox|rtl2|rtlnitro|superrtl|ntv)/[a-z0-9\\-]+/[a-z0-9\\-]+" }, flags = { 3 })
public class RTLnowDe extends PluginForHost {

    public RTLnowDe(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    /* Settings */
    private final String                  DEFAULTTIMEOUT       = "DEFAULTTIMEOUT";
    private final String                  MINIMUM_FILESIZE     = "MINIMUM_FILESIZE";

    /* Tags: rtl-interactive.de, RTL, rtlnow, rtl-now */
    /* General information: The "filmID" is a number which is usually in the html as "film_id" or in the XML 'generate' URL as "para1" */
    /* https?://(www\\.)?<host>/hds/videos/<filmID>/manifest\\-hds\\.f4m */
    private final String                  HDSTYPE_NEW_MANIFEST = "https?://(www\\.)?[a-z0-0\\-\\.]+/hds/videos/\\d+/manifest\\-hds\\.f4m";
    /*
     * http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/abr/videos/<seriesID (same for app episodes of one series>/<videoIUD(same for all
     * qualities/versions of a video)>/V_\\d+[A-Za-z0-9\\-_]+abr\\-<bitrate - usually 550, 1000 or 1500)>_[a-f0-9]{30}\\.mp4\\.f4m\\?cb=\\d+
     */
    private final String                  HDSTYPE_NEW_DETAILED = "http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/abr/videos/\\d+/\\d+/V_\\d+[A-Za-z0-9\\-_]+_abr\\-\\d+_[a-f0-9]{30,}\\.mp4\\.f4m\\?cb=\\d+";
    /*
     * http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/[^/]+/videos/<seriesID (same for app episodes of one
     * series>/V_\\d+_[A-Z0-9]+_E\\d+_\\d+_h264-mq_<[a-f0-9] usually {30,}>\\.f4v\\.f4m\\?ts=\\d+
     */
    private final String                  HDSTYPE_OLD          = "http://hds\\.fra\\.[^/]+/hds\\-vod\\-enc/[^/]+/videos/\\d+/V_\\d+_[A-Z0-9]+_E\\d+_\\d+_h264-mq_[a-f0-9]+\\.f4v\\.f4m\\?ts=\\d+";
    private Document                      doc;
    private static final boolean          ALLOW_HLS            = true;
    private static final boolean          ALLOW_RTMP           = true;
    private Account                       currAcc              = null;
    private DownloadLink                  currDownloadLink     = null;
    private LinkedHashMap<String, Object> entries              = null;
    private LinkedHashMap<String, Object> format               = null;

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

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setConstants(null, downloadLink);
        setBrowserExclusive();
        String filename = null;
        final String addedlink = downloadLink.getDownloadURL();
        final String urlpart = getURLPart(downloadLink);
        /* urlpart is the same throughout different TV stations so it is a reliable way to detect duplicate urls. */
        downloadLink.setLinkID(urlpart);
        final String apiurl = "https://api.nowtv.de/v3/movies/" + urlpart + "?fields=*,format,files,breakpoints,paymentPaytypes,trailers,pictures,isDrm";
        br.getPage(apiurl);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
        format = (LinkedHashMap<String, Object>) entries.get("format");
        if (br.containsHTML("<\\!\\-\\- Payment\\-Teaser \\-\\->")) {
            downloadLink.getLinkStatus().setStatusText("Download nicht möglich (muss gekauft werden)");
            return AvailableStatus.TRUE;
        }
        final String episode_str = new Regex(addedlink, "folge\\-(\\d+)").getMatch(0);
        final long season = DummyScriptEnginePlugin.toLong(entries.get("season"), -1);
        long episode = DummyScriptEnginePlugin.toLong(entries.get("episode"), -1);
        if (episode == -1 && episode_str != null) {
            episode = Long.parseLong(episode_str);
        }
        final String description = (String) entries.get("articleLong");
        String title = (String) entries.get("title");
        String title_series = (String) format.get("title");
        if (title == null || title_series == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = encodeUnicode(title);
        title_series = encodeUnicode(title_series);

        filename = title_series;
        if (season != -1 && episode != -1) {
            final DecimalFormat df = new DecimalFormat("00");
            filename += "_S" + df.format(season) + "E" + df.format(episode);
        }
        filename += "_" + title;
        filename += ".mp4";

        try {
            if (FilePackage.isDefaultFilePackage(downloadLink.getFilePackage())) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(title_series);
                fp.add(downloadLink);
            }
            if (description != null && downloadLink.getComment() == null) {
                downloadLink.setComment(description);
            }
        } catch (final Throwable e) {
        }
        downloadLink.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    /* Last revision with old handling: BEFORE 30393 */
    @SuppressWarnings({ "unchecked", "rawtypes", "deprecation" })
    private void download(final DownloadLink downloadLink) throws Exception {
        final String[] duration_str = ((String) entries.get("duration")).split(":");
        long duration = Long.parseLong(duration_str[0]) * 60 * 60;
        duration += Long.parseLong(duration_str[1]) * 60;
        duration += Long.parseLong(duration_str[2]);
        final boolean isFree = ((Boolean) entries.get("free")).booleanValue();
        final boolean isDRM = ((Boolean) entries.get("isDrm")).booleanValue();
        String url_hds = null;
        String url_hls = null;
        String url_rtmp = null;
        boolean isHDS = (DummyScriptEnginePlugin.toLong(format.get("flashHds"), -1) == 1);
        long bitrate_max = 0;
        long bitrate_temp = 0;
        final String movieID = Long.toString(DummyScriptEnginePlugin.toLong(entries.get("id"), -1));
        if (movieID.equals("-1")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isDRM) {
            /* There really is no way to download these videos and if, you will get encrypted trash data so let's just stop here. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type");
        }
        br.getPage("http://rtl-now.rtl.de/hds/videos/" + movieID + "/manifest-hds.f4m?&ts=" + System.currentTimeMillis());
        final String[] hdsurls = br.getRegex("<media (.*?)/>").getColumn(0);
        if (hdsurls == null || hdsurls.length == 0) {
            isHDS = false;
        }
        if (isHDS) {
            /* hds/hls */
            /* Get the highest quality available */
            for (final String hdssource : hdsurls) {
                final String url = new Regex(hdssource, "href=\"(http[^<>\"]*?)\"").getMatch(0);
                final String bitrate_str = new Regex(hdssource, "bitrate=\"(\\d+)\"").getMatch(0);
                if (url == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                bitrate_temp = Long.parseLong(bitrate_str);
                if (bitrate_temp > bitrate_max) {
                    url_hds = url;
                    bitrate_max = bitrate_temp;
                }
            }
            if (url_hds == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* check if rtmp is possible */
            final String apiurl = "https://api.nowtv.de/v3/movies/" + getURLPart(downloadLink) + "?fields=files";
            br.getPage(apiurl);
            entries = (LinkedHashMap<String, Object>) DummyScriptEnginePlugin.jsonToJavaObject(br.toString());
            final ArrayList<Object> ressourcelist = (ArrayList) DummyScriptEnginePlugin.walkJson(entries, "files/items");
            if (ressourcelist == null || ressourcelist.size() == 0) {
                if (!isFree) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Download nicht möglich (muss gekauft werden)");
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final Object quality_o : ressourcelist) {
                entries = (LinkedHashMap<String, Object>) quality_o;
                bitrate_temp = DummyScriptEnginePlugin.toLong(entries.get("bitrate"), -1);
                if (bitrate_temp > bitrate_max) {
                    url_rtmp = (String) entries.get("path");
                    bitrate_max = bitrate_temp;
                }
            }
            if (url_rtmp == null) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type");
        }

        if (ALLOW_HLS && url_hds != null && url_hds.matches(this.HDSTYPE_NEW_DETAILED)) {
            url_hls = url_hds.replace("hds", "hls");
            url_hls = url_hls.replace(".f4m", ".m3u8");
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            dl = new HLSDownloader(downloadLink, br, url_hls);
            dl.startDownload();
        } else if (url_rtmp != null && ALLOW_RTMP) {
            final long calculated_filesize = ((bitrate_max * 1000) / 8) * duration;
            final long calculated_filesize_minimum = (long) (calculated_filesize * 0.9);
            logger.info("Calculated filesize: " + calculated_filesize);
            logger.info("Minimum filesize: " + calculated_filesize_minimum);

            final Regex urlregex = new Regex(url_rtmp, "/([^/]+)/(\\d+/.+)");
            /*
             * Either we already got rtmp urls or we can try to build them via the playpath-part of our HDS manifest url (see code BEFORE
             * rev 30393)
             */
            String app = urlregex.getMatch(0);
            if (app == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            app = convertAppToRealApp(app);
            /* We don't need the exact url of the video, especially because we do not even always have it. An url of the mainpage is enough! */
            final String pageURL = convertAppToMainpage(app);
            final String rtmp_playpath = "mp4:" + urlregex.getMatch(1);
            /* Either use fms-fra[1-32].rtl.de or just fms.rtl.de */
            final String rtmpurl = "rtmpe://fms.rtl.de/" + app + "/";

            downloadLink.setProperty("FLVFIXER", true);
            dl = new RTMPDownload(this, downloadLink, rtmpurl);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(rtmp_playpath);
            rtmp.setPageUrl(pageURL);
            rtmp.setSwfVfy("http://cdn.static-fra.de/now/vodplayer.swf");
            rtmp.setFlashVer("WIN 14,0,0,145");
            rtmp.setApp(app);
            rtmp.setUrl(rtmpurl);
            rtmp.setResume(true);
            rtmp.setRealTime();
            if (!getPluginConfig().getBooleanProperty(DEFAULTTIMEOUT, false)) {
                rtmp.setTimeOut(-1);
            }

            final boolean downloadSuccessful = ((RTMPDownload) dl).startDownload();

            if (this.getPluginConfig().getBooleanProperty(MINIMUM_FILESIZE, false) && downloadLink.getDownloadCurrent() < calculated_filesize_minimum && downloadSuccessful) {
                /*
                 * Maybe the downloaded file is too small - some rtmp servers tend to just stop after small files even though the download
                 * is by far not complete --> Reset progress and try again later!
                 */
                try {
                    downloadLink.setChunksProgress(null);
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Downloaded file is too small", 5 * 60 * 1000l);
            }

        } else {
            /* TODO */
            if (true) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type");
            }
            if (url_hds.matches(this.HDSTYPE_NEW_DETAILED)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            }
            // if (dllink.matches(this.HDSTYPE_NEW_MANIFEST)) {
            // logger.info("2nd attempt to get final hds url");
            // /* TODO */
            // if (true) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            // }
            // final XPath xPath = xmlParser(dllink);
            // final NodeList nl = (NodeList) xPath.evaluate("/manifest/media", doc, XPathConstants.NODESET);
            // final Node n = nl.item(0);
            // dllink = n.getAttributes().getNamedItem("href").getTextContent();
            // }
            // br.getPage(dllink);
            // final String hds = parseManifest();

            dl = new HDSDownloader(downloadLink, br, url_hds);
            dl.startDownload();

        }
    }

    @SuppressWarnings("deprecation")
    private String getURLPart(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/([a-z0-9\\-]+/[a-z0-9\\-]+)$").getMatch(0);
    }

    /** Corrects the rtmpdump app-parameter for some rare cases */
    private String convertAppToRealApp(final String input) {
        final String output;
        if (input.equals("rtlnitronow")) {
            output = "nitronow";
        } else if (input.equals("n-tvnow")) {
            output = "ntvnow";
        } else {
            output = input;
        }
        return output;
    }

    /** Returns the main URL that fits the given rtmpdump-app */
    private String convertAppToMainpage(final String input) {
        final String output;
        if (input.equals("nitronow")) {
            output = "http://www.rtlnitronow.de/";
        } else if (input.equals("ntvnow")) {
            output = "http://www.n-tvnow.de/";
        } else {
            output = "http://www." + input + ".de/";
        }
        return output;
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
        /* TODO: Fix this! */
        // final String ageCheck = br.getRegex("(Aus Jugendschutzgründen nur zwischen \\d+ und \\d+ Uhr abrufbar\\!)").getMatch(0);
        // if (ageCheck != null) {
        // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, ageCheck, 10 * 60 * 60 * 1000l);
        // }
        download(downloadLink);
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
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), DEFAULTTIMEOUT, JDL.L("plugins.hoster.rtlnowde.enabledeafulttimeout", "Enable default timeout?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), MINIMUM_FILESIZE, JDL.L("plugins.hoster.rtlnowde.enable_minimum_filesize", "Enable minimum filesize to try to prevent too small/crippled files?")).setDefaultValue(false));
    }

}