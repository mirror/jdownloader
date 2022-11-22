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
package jd.plugins.hoster;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.config.OdyseeComConfig;
import org.jdownloader.plugins.components.config.OdyseeComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.linkcrawler.LinkCrawlerDeepInspector;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OdyseeCom extends PluginForHost {
    public OdyseeCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    private static final String PROPERTY_DIRECTURL             = "free_directlink";
    private static final String PROPERTY_QUALITY               = "preferredQuality";
    private static final String PROPERTY_EXPECTED_CONTENT_TYPE = "expected_content_type";

    @Override
    public String getAGBLink() {
        return "https://lbry.com/termsofservice";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "odysee.com", "lbry.tv" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/((@[A-Za-z0-9\\-\\.]+):[a-z0-9]+/([^/:]+:[a-z0-9\\-]+)|[^/:]+:[a-z0-9\\-]+|\\$/embed/[^/]+/[a-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = 20;

    // private static final boolean ACCOUNT_FREE_RESUME = true;
    // private static final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private static final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private static final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private static final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private static final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String fid = new Regex(link.getPluginPatternMatcher(), "embed/([^/]+/[a-z0-9\\-]+)").getMatch(0);
        if (fid != null) {
            // https://odysee.com/$/embed/xxxxx-xxxxxx/id
            return fid.replace("/", "#");
        }
        fid = new Regex(link.getPluginPatternMatcher(), "/(@[^:]+:[^/]+/[^/:]+:[a-z0-9\\-]+)").getMatch(0);
        if (fid != null) {
            // https://odysee.com/@xxxx:yyyy/xxx-xxxx:z
            return fid.replace(":", "#");
        }
        fid = new Regex(link.getPluginPatternMatcher(), "/([^/:]+:[a-z0-9\\-]+)").getMatch(0);
        if (fid != null) {
            // https://odysee.com/xxxxx-xxxxxx/id
            return fid.replace(":", "#");
        }
        fid = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
        return fid;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        String urlpart = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(urlpart);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("Content-Type", "application/json-rpc");
        if (Encoding.isUrlCoded(urlpart)) {
            urlpart = Encoding.htmlDecode(urlpart);
        }
        String resolveString = "lbry://" + urlpart.replace(":", "#");
        br.postPageRaw("https://api.na-backend.odysee.com/api/v1/proxy?m=resolve", "{\"jsonrpc\":\"2.0\",\"method\":\"resolve\",\"params\":{\"urls\":[\"" + resolveString + "\"],\"include_purchase_receipt\":true,\"include_is_my_output\":true}}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        entries = (Map<String, Object>) entries.get("result");
        entries = (Map<String, Object>) entries.get(resolveString);
        if (entries.containsKey("error")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final long uploadTimestamp = ((Number) entries.get("timestamp")).longValue();
        final String claimID = (String) entries.get("claim_id");
        final String slug = (String) entries.get("name");
        // final Map<String, Object> channel = (Map<String, Object>) entries.get("signing_channel");
        final String username = slug;// new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        final Map<String, Object> videoInfo = (Map<String, Object>) entries.get("value");
        String filename = (String) videoInfo.get("title");
        final String stream_type = (String) videoInfo.get("stream_type");
        if (stream_type == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final boolean directDownload = "document".equals(stream_type) || "image".equals(stream_type);
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(filename)) {
            final String dateFormatted = new SimpleDateFormat("yyyy-MM-dd").format(new Date(uploadTimestamp * 1000l));
            final String ext;
            if ("document".equals(stream_type)) {
                ext = ".txt";
            } else if ("image".equals(stream_type)) {
                ext = ".jpg";
            } else {
                /* Assume we have a video. */
                ext = ".mp4";
            }
            link.setFinalFileName(dateFormatted + "_" + username + " - " + filename + ext);
        }
        final String description = (String) videoInfo.get("description");
        if (!StringUtils.isEmpty(description)) {
            link.setComment(description);
        }
        final Map<String, Object> downloadInfo = (Map<String, Object>) videoInfo.get("source");
        if (downloadInfo != null) {
            final String sdhash = (String) downloadInfo.get("sd_hash");
            if (!StringUtils.isEmpty(claimID) && !StringUtils.isEmpty(sdhash)) {
                final String dllink = "https://cdn.lbryplayer.xyz/api/v4/streams/free/" + slug + "/" + claimID + "/" + sdhash.substring(0, 6);
                final PreferredStreamQuality quality = getPreferredQuality(link);
                final int userPreferredQualityHeight = this.getPreferredQualityHeight(link, quality);
                if (userPreferredQualityHeight == -1 || directDownload) {
                    final long filesize = JavaScriptEngineFactory.toLong(downloadInfo.get("size"), -1);
                    if (filesize > 0) {
                        link.setVerifiedFileSize(filesize);
                    }
                    link.setProperty(PROPERTY_DIRECTURL, dllink + "?download=true");
                    link.setProperty(PROPERTY_QUALITY, PreferredStreamQuality.BEST.name());
                } else {
                    link.setProperty(PROPERTY_DIRECTURL, dllink);
                    link.setProperty(PROPERTY_QUALITY, quality.name());
                }
            }
            /* E.g. "text/markdown", "video/mp4" */
            link.setProperty(PROPERTY_EXPECTED_CONTENT_TYPE, downloadInfo.get("media_type").toString());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, PROPERTY_DIRECTURL);
    }

    protected boolean looksLikeDownloadableContent(final URLConnectionAdapter urlConnection, final DownloadLink link) {
        /* First check if content-type matches the one we expect. */
        if (StringUtils.equalsIgnoreCase(urlConnection.getContentType(), link.getStringProperty(PROPERTY_EXPECTED_CONTENT_TYPE))) {
            return true;
        } else {
            return super.looksLikeDownloadableContent(urlConnection) && !LinkCrawlerDeepInspector.looksLikeMpegURL(urlConnection);
        }
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (!attemptStoredDownloadurlDownload(link, directlinkproperty, resumable, maxchunks)) {
            requestFileInformation(link);
            String dllink = link.getStringProperty(directlinkproperty);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Stream unavailable");
            }
            final Browser brc = br.cloneBrowser();
            brc.setFollowRedirects(true);
            final URLConnectionAdapter con = brc.openGetConnection(dllink);
            if (!looksLikeDownloadableContent(con, link)) {
                brc.followConnection();
                if (LinkCrawlerDeepInspector.looksLikeMpegURL(con)) {
                    final List<HlsContainer> hls = HlsContainer.getHlsQualities(brc);
                    final HlsContainer best = HlsContainer.findBestVideoByBandwidth(hls);
                    if (best == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final HlsContainer chosenQuality;
                    final PreferredStreamQuality quality = getPreferredQuality(link);
                    final int userPreferredQualityHeight = this.getPreferredQualityHeight(link, quality);
                    if (userPreferredQualityHeight == -1) {
                        chosenQuality = best;
                    } else {
                        HlsContainer userPreferred = null;
                        for (final HlsContainer hlsTmp : hls) {
                            if (hlsTmp.getHeight() == userPreferredQualityHeight) {
                                userPreferred = hlsTmp;
                                break;
                            }
                        }
                        if (userPreferred != null) {
                            logger.info("Using user selected quality: " + userPreferredQualityHeight + "p");
                            chosenQuality = userPreferred;
                        } else {
                            logger.info("Failed to find user preferred quality -> Using BEST instead");
                            chosenQuality = best;
                        }
                    }
                    link.setVerifiedFileSize(-1);
                    checkFFmpeg(link, "Download a HLS Stream");
                    dl = new HLSDownloader(link, br, chosenQuality.getStreamURL());
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, con.getRequest(), resumable, maxchunks);
                if (!this.looksLikeDownloadableContent(dl.getConnection(), link)) {
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    if (dl.getConnection().getResponseCode() == 403) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                    } else if (dl.getConnection().getResponseCode() == 404) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection(), link)) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(directlinkproperty);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private PreferredStreamQuality getPreferredQuality(final DownloadLink downloadLink) {
        final String preferredQuality = downloadLink.getStringProperty(PROPERTY_QUALITY, null);
        PreferredStreamQuality quality = PluginJsonConfig.get(OdyseeComConfig.class).getPreferredStreamQuality();
        if (preferredQuality != null) {
            try {
                quality = PreferredStreamQuality.valueOf(preferredQuality);
            } catch (IllegalArgumentException ignore) {
                logger.log(ignore);
            }
        }
        if (quality == null) {
            return PreferredStreamQuality.BEST;
        } else {
            return quality;
        }
    }

    private int getPreferredQualityHeight(final DownloadLink downloadLink, PreferredStreamQuality quality) {
        switch (quality) {
        case BEST:
            return -1;
        case Q144P:
            return 144;
        case Q360P:
            return 360;
        case Q720P:
            return 720;
        case Q1080P:
            return 1080;
        default:
            return -1;
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return OdyseeComConfig.class;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        link.removeProperty(PROPERTY_DIRECTURL);
        link.removeProperty(PROPERTY_QUALITY);
    }
}