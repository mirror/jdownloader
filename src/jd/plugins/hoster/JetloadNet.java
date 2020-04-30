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
import java.util.LinkedHashMap;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.components.hls.HlsContainer;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jetload.net" }, urls = { "https?://(?:www\\.)?jetload\\.net/(?:#\\!/d|e|p|#\\!/v)/([A-Za-z0-9]+)" })
public class JetloadNet extends PluginForHost {
    public JetloadNet(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://jetload.net/#!/Terms";
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                  = true;
    private final int            FREE_MAXCHUNKS               = 0;
    private final int            FREE_MAXDOWNLOADS            = 20;
    private final boolean        ACCOUNT_FREE_RESUME          = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean        ACCOUNT_PREMIUM_RESUME       = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    /*
     * 2019-05-08: Generated via jetload.com account psp[AT]jdownloader[DOT]org Documentation: https://jetload.net/u/#!/api_docs
     */
    private static final String  API_KEY                      = "b9yEWYHSNVZq1a2y";
    private static final boolean prefer_linkcheck_via_API     = true;
    private boolean              api_used                     = true;
    private static final boolean useNewWay2020                = true;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException, InterruptedException {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        api_used = false;
        this.setBrowserExclusive();
        /* 2020-03-10: Disabled - not required anymore */
        final boolean allowWebsiteFallback = false;
        AvailableStatus status = AvailableStatus.UNCHECKABLE;
        if (prefer_linkcheck_via_API) {
            status = this.requestFileInformationAPI(link, false);
        }
        if (status == AvailableStatus.UNCHECKABLE && allowWebsiteFallback) {
            /* Fallback to website(or API usage was disabled) - this should never happen! */
            status = requestFileInformationWebsite(link, false);
            api_used = false;
        }
        return status;
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link, final boolean isDownload) throws IOException, PluginException {
        /*
         * 2019-05-08: It seems like we could use a random String instead of a real API_KEY too - but who knows for how long so we'll use a
         * valid one ...
         */
        /*
         * 2019-05-08: Sidenote: It seems like the database which this API uses is not the most recent one. This means that it can sometimes
         * happen that a file gets displayed as online (linkgrabber) but once it gets checked via website (download-attempt) its' real
         * status will be OFFLINE. This should not be a big issue as it is a rare occurence.
         */
        br.getPage(String.format("https://jetload.net/api/v2/check_file/%s/%s", API_KEY, this.getFID(link)));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String status = PluginJSonUtils.getJson(br, "status");
        if ("400".equals(status)) {
            /* Invalid API Key - this should never happen! */
            return AvailableStatus.UNCHECKABLE;
        } else if ("401".equals(status)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = PluginJSonUtils.getJson(br, "origin_filename");
        final String filesize = PluginJSonUtils.getJson(br, "file_size");
        if (filename != null) {
            link.setFinalFileName(filename);
        } else {
            link.setName(this.getFID(link));
        }
        if (!StringUtils.isEmpty(filesize) && filesize.matches("\\d+")) {
            link.setDownloadSize(Long.parseLong(filesize));
        }
        final String filestatus = PluginJSonUtils.getJson(br, "file_status");
        if (StringUtils.equalsIgnoreCase(filestatus, "Deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String encoding_status = PluginJSonUtils.getJson(br, "encoding_status");
        if (StringUtils.equalsIgnoreCase("pending", encoding_status) || StringUtils.equalsIgnoreCase("started", encoding_status)) {
            if (isDownload) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video is still encoding!", 60 * 60 * 1000l);
            } else {
                return AvailableStatus.TRUE;
            }
        } else {
            return AvailableStatus.TRUE;
        }
    }

    protected CaptchaHelperHostPluginRecaptchaV2 getCaptchaHelperHostPluginRecaptchaV2(PluginForHost plugin, Browser br, final String key) throws PluginException {
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, key) {
            @Override
            public org.jdownloader.captcha.v2.challenge.recaptcha.v2.AbstractRecaptchaV2.TYPE getType() {
                return TYPE.INVISIBLE;
            }
        };
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link, final boolean isDownload) throws IOException, PluginException, InterruptedException {
        /* 2019-05-08: Very similar to their API but not exactly the same */
        if (useNewWay2020) {
            if (!isDownload) {
                /* Do not request captchas during availablecheck */
                return AvailableStatus.UNCHECKABLE;
            }
            final String fid = this.getFID(link);
            br.getPage(link.getPluginPatternMatcher());
            /* 2020-01-22: Hardcoded reCaptchaV2 key */
            final String recaptchaV2Response = getCaptchaHelperHostPluginRecaptchaV2(this, br, "6Lc90MkUAAAAAOrqIJqt4iXY_fkXb7j3zwgRGtUI").getToken();
            final String postData = String.format("{\"token\":\"%s\",\"stream_code\":\"%s\"}", recaptchaV2Response, fid);
            br.postPageRaw("https://" + this.getHost() + "/jet_secure", postData);
            final String errorMsg = PluginJSonUtils.getJson(br, "err");
            if (errorMsg != null) {
                if (errorMsg.equalsIgnoreCase("file_not_found")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                /* Unknown error */
                return AvailableStatus.UNCHECKABLE;
            }
            return AvailableStatus.TRUE;
        } else {
            // br.getHeaders().put("X-XSRF-TOKEN", ""); /* 2019-05-08: We don't need this */
            /* 2020-01-22: This will always return 404 */
            br.getPage(String.format("https://jetload.net/api/get_direct_video/%s", this.getFID(link)));
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML("file can\\'t be found") || !br.containsHTML(this.getFID(link))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String filename = PluginJSonUtils.getJson(br, "origin_filename");
            final String filesize = PluginJSonUtils.getJson(br, "file_size");
            if (filename != null) {
                link.setFinalFileName(filename);
            } else {
                link.setName(this.getFID(link));
            }
            if (!StringUtils.isEmpty(filesize) && filesize.matches("\\d+")) {
                link.setDownloadSize(Long.parseLong(filesize));
            }
            final String encoding_status = PluginJSonUtils.getJson(br, "encoding_status");
            if (StringUtils.equalsIgnoreCase("pending", encoding_status) || StringUtils.equalsIgnoreCase("started", encoding_status)) {
                // downloading hls stream
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video is still encoding!", 60 * 60 * 1000l);
            } else {
                return AvailableStatus.TRUE;
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformationAPI(link, true);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private String getString(Map<String, Object> map, String key) {
        final Object ret = JavaScriptEngineFactory.walkJson(map, key);
        if (ret == null) {
            return null;
        } else if (ret instanceof String) {
            return (String) ret;
        } else {
            return String.valueOf(ret);
        }
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (this.api_used) {
                /* We need to access it via the website here if this has not happened before. */
                requestFileInformationWebsite(link, true);
            }
            LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            if (useNewWay2020) {
                /* 2020-01-22: New */
                entries = (LinkedHashMap<String, Object>) entries.get("src");
                dllink = (String) entries.get("src");
            } else {
                /* Old */
                /*
                 * Attention! This is NOT the filename we use - it is only required to get a working downloadlink (wrong value =
                 * downloadlink leads to 404)
                 */
                /* 2019-10-04: E.g. video: "encoding_status":"completed" */
                final boolean is_video = !"file".equalsIgnoreCase((String) JavaScriptEngineFactory.walkJson(entries, "file/encoding_status"));
                final String filename_internal = (String) JavaScriptEngineFactory.walkJson(entries, "file/file_name");
                final String ext = (String) JavaScriptEngineFactory.walkJson(entries, "file/file_ext");
                final String archiveValue = getString(entries, "file/archive");
                final boolean archive = "1".equals(archiveValue);
                final String lowValue = getString(entries, "file/low");
                final String medValue = getString(entries, "file/med");
                final String highValue = getString(entries, "file/high");
                final boolean low = "1".equals(lowValue);
                final boolean med = "1".equals(medValue);
                if (StringUtils.isEmpty(filename_internal)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (StringUtils.isEmpty(ext)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (is_video) {
                    final String hostname = (String) JavaScriptEngineFactory.walkJson(entries, "server/hostname");
                    if (StringUtils.isEmpty(hostname)) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    // br.getPage("/api/get_direct_video/" + this.getFID(link));
                    // entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                    // ng-visitor/js/play.js
                    Browser brc = null;
                    if (lowValue == null && medValue == null && highValue == null) {
                        brc = br.cloneBrowser();
                        brc.setFollowRedirects(true);
                        brc.getPage(link.getPluginPatternMatcher());
                    }
                    final String hlsPlayerSource = brc != null ? brc.getRegex("<\\s*source\\s*src\\s*=\\s*\"([^\"]*\\.m3u8)\"").getMatch(0) : null;
                    if (hlsPlayerSource != null) {
                        dllink = hlsPlayerSource;
                    } else {
                        final String m3u8;
                        if (low && med) {
                            m3u8 = "master.m3u8";
                        } else if (med) {
                            m3u8 = "med.m3u8";
                        } else {
                            m3u8 = "low.m3u8";
                        }
                        if (archive) {
                            dllink = String.format("%s/v2/schema/archive/%s/" + m3u8, hostname, filename_internal);
                        } else {
                            dllink = String.format("%s/v2/schema/%s/" + m3u8, hostname, filename_internal);
                        }
                    }
                } else {
                    /*
                     * Official download via API. Can also be used to download videos but will often fail for videos (error 404) - we're
                     * only using it to download non-video-files!
                     */
                    String serverID = getString(entries, "server/id");
                    if (StringUtils.isEmpty(serverID)) {
                        // server/id available for encoding_status: "completed"
                        serverID = getString(entries, "file/srv_id");
                        // file/srv_id always available for encoding_status: "pending"
                        if (StringUtils.isEmpty(serverID)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    final PostRequest downloadReq = br.createJSonPostRequest("/api/download", String.format("{\"file_name\":\"%s.%s\",\"srv\":\"%s\"}", filename_internal, ext, serverID));
                    br.openRequestConnection(downloadReq);
                    br.loadConnection(null);
                    dllink = br.toString();
                }
            }
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (dllink.contains(".m3u8")) {
            /* HLS download */
            br.getPage(dllink);
            if (br.getHttpConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            checkFFmpeg(link, "Download a HLS Stream");
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null && br.containsHTML("#EXT-X-ENDLIST")) {
                /* 2019-08-29: url to m3u8 is direct hls stream and not quality overview - this is sometimes the case. */
                dllink = br.getURL();
            } else if (hlsbest == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                dllink = hlsbest.getDownloadurl();
            }
            dl = new HLSDownloader(link, br, dllink);
        } else {
            /* HTTP download */
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (!con.isOK() || con.getContentType().contains("text") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}