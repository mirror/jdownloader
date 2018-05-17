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
import java.util.LinkedHashMap;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.config.MediathekProperties;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MediathekHelper;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tvnow.de" }, urls = { "https?://(?:www\\.)?(?:nowtv|tvnow)\\.(?:de|ch)/[a-z0-9\\-]+/[a-z0-9\\-]+/.+" })
public class TvnowDe extends PluginForHost {
    public TvnowDe(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Settings */
    /* Tags: rtl-interactive.de, RTL, rtlnow, rtl-now */
    private static final String           TYPE_GENERAL_ALRIGHT = "https?://(?:www\\.)?(?:nowtv|tvnow)\\.(?:de|ch)/[^/]+/[a-z0-9\\-]+/[^/\\?]+";
    public static final String            API_BASE             = "https://api.tvnow.de/v3";
    public static final String            CURRENT_DOMAIN       = "tvnow.de";
    private LinkedHashMap<String, Object> entries              = null;

    public static Browser prepBR(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/plain, */*");
        /* 400-bad request for invalid API requests */
        br.setAllowedResponseCodes(new int[] { 400 });
        return br;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        /* First lets get our source url and remove the unneeded '/player' part which is usually at the end of our url. */
        final String url_source = link.getPluginPatternMatcher();
        String urlNew;
        if (link.getPluginPatternMatcher().matches(TYPE_GENERAL_ALRIGHT)) {
            final String url_part = new Regex(url_source, "https?://[^/]+/(.+)").getMatch(0);
            urlNew = "https://www." + CURRENT_DOMAIN + "/" + url_part;
        } else {
            /* We have no supported url --> Fix eventually existing issues */
            /* First let's remove rubbish we don't need ... */
            String rubbish = new Regex(url_source, "(/(?:preview|player)(?:.+)?)").getMatch(0);
            if (rubbish != null) {
                urlNew = url_source.replace(rubbish, "");
            } else {
                urlNew = url_source;
            }
            /* Now remove any leftover parameters as we do not need them! */
            rubbish = new Regex(urlNew, "(\\?.+)").getMatch(0);
            if (rubbish != null) {
                urlNew = url_source.replace(rubbish, "");
            }
            final Regex sourceregex = new Regex(urlNew, "https?://[^/]+/([^/]+)/([a-z0-9\\-]+)");
            final String name_tvstation = sourceregex.getMatch(0);
            final String name_series = sourceregex.getMatch(1);
            /* Find the name of the series which is usually at the end of our URL. */
            final String name_episode = new Regex(urlNew, "/([^/]+)$").getMatch(0);
            urlNew = "https://www." + CURRENT_DOMAIN + "/" + name_tvstation + "/" + name_series + "/" + name_episode;
        }
        link.setUrlDownload(urlNew);
    }

    /**
     * ~2015-05-01 Available HLS AND HDS streams are DRM protected <br />
     * ~2015-07-01: HLS streams were turned off <br />
     * ~2016-01-01: RTMP(E) streams were turned off / all of them are DRM protected/crypted now<br />
     * ~2016-02-24: Summary: There is absolutely NO WAY to download from this website <br />
     * ~2016-03-15: Domainchange from nowtv.de to tvnow.de<br />
     * .2018-04-17: Big code cleanup and HLS streams were re-introduced<br />
     */
    @SuppressWarnings({ "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        /* Fix old urls */
        correctDownloadLink(downloadLink);
        prepBR(this.br);
        final String urlpart = getURLPart(downloadLink);
        /* urlpart is the same throughout different TV stations so it is a reliable way to detect duplicate urls. */
        downloadLink.setLinkID(urlpart);
        // ?fields=*,format,files,manifest,breakpoints,paymentPaytypes,trailers,packages,isDrm
        /*
         * Explanation of possible but left-out parameters: "breakpoints" = timecodes when ads are delivered, "paymentPaytypes" = how can
         * this item be purchased and how much does it cost, "trailers" = trailers, "files" = old rtlnow URLs, see plugin revision 38232 and
         * earlier
         */
        br.getPage(API_BASE + "/movies/" + urlpart + "?fields=" + getFields());
        if (br.getHttpConnection().getResponseCode() != 200) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        final LinkedHashMap<String, Object> format = (LinkedHashMap<String, Object>) entries.get("format");
        final String tv_station = (String) format.get("station");
        final String formatTitle = (String) format.get("title");
        parseInformation(downloadLink, entries, tv_station, formatTitle);
        return AvailableStatus.TRUE;
    }

    /** Returns parameters for API 'fields=' key. */
    public static String getFields() {
        return "*,format,packages,isDrm";
    }

    public static void parseInformation(final DownloadLink downloadLink, final LinkedHashMap<String, Object> entries, final String tv_station, final String formatTitle) {
        final MediathekProperties data = downloadLink.bindData(MediathekProperties.class);
        final String date = (String) entries.get("broadcastStartDate");
        final String episode_str = new Regex(downloadLink.getPluginPatternMatcher(), "folge\\-(\\d+)").getMatch(0);
        final int season = (int) JavaScriptEngineFactory.toLong(entries.get("season"), -1);
        int episode = (int) JavaScriptEngineFactory.toLong(entries.get("episode"), -1);
        if (episode == -1 && episode_str != null) {
            /* Fallback which should usually not be required */
            episode = (int) Long.parseLong(episode_str);
        }
        final String description = (String) entries.get("articleLong");
        /* Title or subtitle of a current series-episode */
        String title = (String) entries.get("title");
        if (title == null || formatTitle == null || tv_station == null || date == null) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        data.setShow(formatTitle);
        data.setChannel(tv_station);
        data.setReleaseDate(getDateMilliseconds(date));
        if (season != -1 && episode != -1) {
            data.setSeasonNumber(season);
            data.setEpisodeNumber(episode);
            if (title.matches("Folge \\d+")) {
                /* We do not need the episode information twice! */
                title = null;
            }
        }
        if (!StringUtils.isEmpty(title)) {
            data.setTitle(title);
        }
        final String filename = MediathekHelper.getMediathekFilename(downloadLink, data, false, false);
        try {
            if (FilePackage.isDefaultFilePackage(downloadLink.getFilePackage())) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(formatTitle);
                fp.add(downloadLink);
            }
            if (!StringUtils.isEmpty(description) && downloadLink.getComment() == null) {
                downloadLink.setComment(description);
            }
        } catch (final Throwable e) {
        }
        downloadLink.setFinalFileName(filename);
    }

    /* Last revision with old handling: BEFORE 38232 (30393) */
    private void download(final DownloadLink downloadLink) throws Exception {
        final boolean isFree = ((Boolean) entries.get("free")).booleanValue();
        final boolean isDRM = ((Boolean) entries.get("isDrm")).booleanValue();
        final String movieID = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), -1));
        if (movieID.equals("-1")) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isDRM) {
            /* There really is no way to download these videos and if, you will get encrypted trash data so let's just stop here. */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [DRM]");
        }
        final String urlpart = getURLPart(downloadLink);
        br.getPage(API_BASE + "/movies/" + urlpart + "?fields=manifest");
        entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("manifest");
        /* 2018-04-18: So far I haven't seen a single http stream! */
        // final String urlHTTP = (String) entries.get("hbbtv");
        final String hdsMaster = (String) entries.get("hds");
        String hlsMaster = (String) entries.get("hlsclear");
        if (StringUtils.isEmpty(hlsMaster)) {
            hlsMaster = (String) entries.get("hlsfairplay");
            /* 2018-05-04: Only "hls" == Always DRM */
            // if (StringUtils.isEmpty(hlsMaster)) {
            // hlsMaster = (String) entries.get("hls");
            // }
        }
        if (!StringUtils.isEmpty(hlsMaster)) {
            br.getPage(hlsMaster);
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null) {
                /* No content available --> Probably DRM protected */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [DRM]");
            }
            checkFFmpeg(downloadLink, "Download a HLS Stream");
            try {
                dl = new HLSDownloader(downloadLink, br, hlsbest.getDownloadurl());
            } catch (final Throwable e) {
                /*
                 * 2017-11-15: They've changed these URLs to redirect to image content (a pixel). Most likely we have a broken HLS url -->
                 * Download not possible, only crypted HDS available.
                 */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [DRM]");
            }
            dl.startDownload();
        } else {
            /* hds */
            if (!isFree) {
                /*
                 * We found no downloadurls plus the video is not viewable for free --> Paid content. TODO: Maybe check if it is
                 * downloadable once a user bought it --> Probably not as chances are high that it will be DRM protected!
                 */
                throw new PluginException(LinkStatus.ERROR_FATAL, "Download nicht möglich (muss gekauft werden)");
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "HDS streaming is not (yet) supported");
            // /* Now we're sure that our .mp4 availablecheck-filename is correct */
            // downloadLink.setFinalFileName(downloadLink.getName());
            // /* TODO */
            // if (true) {
            // // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type [HDS]");
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            // }
            // if (url_hds.matches(this.HDSTYPE_NEW_DETAILED)) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, "Unsupported streaming type / encrypted HDS");
            // }
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
            // dl = new HDSDownloader(downloadLink, br, url_hds);
            // dl.startDownload();
        }
    }

    @SuppressWarnings("deprecation")
    private String getURLPart(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "/([a-z0-9\\-]+/[a-z0-9\\-]+)$").getMatch(0);
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
        } catch (Throwable e) {
            e.printStackTrace();
        } finally {
        }
        return null;
    }

    @Override
    public String getAGBLink() {
        return "http://rtl-now.rtl.de/nutzungsbedingungen";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        final TvnowConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.TvnowDe.TvnowConfigInterface.class);
        if (cfg.isEnableUnlimitedSimultaneousDownloads()) {
            return -1;
        } else {
            return 1;
        }
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
        if (link != null) {
            link.removeProperty(HDSDownloader.RESUME_FRAGMENT);
        }
    }

    @Override
    public void resetPluginGlobals() {
    }

    // private XPath xmlParser(final String linkurl) throws Exception {
    // URLConnectionAdapter con = null;
    // try {
    // con = new Browser().openGetConnection(linkurl);
    // final DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    // final XPath xPath = XPathFactory.newInstance().newXPath();
    // try {
    // doc = parser.parse(con.getInputStream());
    // return xPath;
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // } catch (final Throwable e2) {
    // return null;
    // }
    // }
    /** Formats the existing date to the 'general' date used for german TV online services: yyyy-MM-dd */
    public static long getDateMilliseconds(final String input) {
        if (input == null) {
            return -1;
        }
        return TimeFormatter.getMilliSeconds(input, "yyyy-MM-dd HH:mm:ss", Locale.GERMAN);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return TvnowConfigInterface.class;
    }

    public static interface TvnowConfigInterface extends PluginConfigInterface {
        public static class TRANSLATION {
            public String getEnableUnlimitedSimultaneousDownloads_label() {
                /* Translation not required for this */
                return "Enable unlimited simultaneous downloads? [Warning this may cause issues]";
            }
        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(false)
        @Order(10)
        boolean isEnableUnlimitedSimultaneousDownloads();

        void setEnableUnlimitedSimultaneousDownloads(boolean b);
    }
}