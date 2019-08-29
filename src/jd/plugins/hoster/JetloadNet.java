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

import org.appwork.utils.StringUtils;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jetload.net" }, urls = { "https?://(?:www\\.)?jetload\\.net/(?:#\\!/d|e|#\\!/v)/([A-Za-z0-9]+)" })
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
     * Generated 2019-05-08 via jetload.com account psp[AT]jdownloader[DOT]org Documentation: https://jetload.net/u/#!/api_docs
     */
    private static final String  API_KEY                      = "b9yEWYHSNVZq1a2y";
    private static final boolean prefer_linkcheck_via_API     = true;
    private boolean              api_used                     = true;

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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        api_used = false;
        this.setBrowserExclusive();
        AvailableStatus status = AvailableStatus.UNCHECKABLE;
        if (prefer_linkcheck_via_API) {
            status = this.requestFileInformationAPI(link);
        }
        if (status == AvailableStatus.UNCHECKABLE) {
            /* Fallback to website(or API usage was disabled) - this should never happen! */
            status = requestFileInformationWebsite(link);
            api_used = false;
        }
        return status;
    }

    public AvailableStatus requestFileInformationAPI(final DownloadLink link) throws IOException, PluginException {
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
        final String encoding_status = PluginJSonUtils.getJson(br, "encoding_status");
        if (StringUtils.equalsIgnoreCase("pending", encoding_status) || StringUtils.equalsIgnoreCase("started", encoding_status)) {
            // downloading hls stream
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video is still encoding!", 60 * 60 * 1000l);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    public AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws IOException, PluginException {
        /* 2019-05-08: Very similar to their API but not exactly the same */
        // br.getHeaders().put("X-XSRF-TOKEN", ""); /* 2019-05-08: We don't need this */
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

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (this.api_used) {
                /* We need to access it via the website here if this has not happened before. */
                requestFileInformationWebsite(link);
            }
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            /*
             * Attention! This is NOT the filename we use - it is only required to get a working downloadlink (wrong value = downloadlink
             * leads to 404)
             */
            // final String encoding_status = (String) JavaScriptEngineFactory.walkJson(entries, "file/encoding_status");
            final String filename_internal = (String) JavaScriptEngineFactory.walkJson(entries, "file/file_name");
            final String ext = (String) JavaScriptEngineFactory.walkJson(entries, "file/file_ext");
            final boolean archive = "1".equals(JavaScriptEngineFactory.walkJson(entries, "file/archive"));
            final boolean low = "1".equals(JavaScriptEngineFactory.walkJson(entries, "file/low"));
            final boolean med = "1".equals(JavaScriptEngineFactory.walkJson(entries, "file/med"));
            if (StringUtils.isEmpty(filename_internal)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (StringUtils.isEmpty(ext)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* 2019-07-17: official http download via API fails with response 404 --> Prefer stream download */
            final boolean prefer_stream_download = true;
            if (prefer_stream_download) {
                final String hostname = (String) JavaScriptEngineFactory.walkJson(entries, "server/hostname");
                if (StringUtils.isEmpty(hostname)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // br.getPage("/api/get_direct_video/" + this.getFID(link));
                // entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                // ng-visitor/js/play.js
                final String m3u8;
                if (low && med) {
                    m3u8 = "master.m3u8";
                } else if (!low || med) {
                    m3u8 = "med.m3u8";
                } else {
                    m3u8 = "low.m3u8";
                }
                if (archive) {
                    dllink = String.format("%s/v2/schema/archive/%s/" + m3u8, hostname, filename_internal);
                } else {
                    dllink = String.format("%s/v2/schema/%s/" + m3u8, hostname, filename_internal);
                }
            } else {
                /* Official download via API */
                String serverID = (String) JavaScriptEngineFactory.walkJson(entries, "server/id");
                if (StringUtils.isEmpty(serverID)) {
                    // server/id available for encoding_status: "completed"
                    serverID = (String) JavaScriptEngineFactory.walkJson(entries, "file/srv_id");
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
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (dllink.contains(".m3u8")) {
            /* HLS download */
            br.getPage(dllink);
            checkFFmpeg(link, "Download a HLS Stream");
            final HlsContainer hlsbest = HlsContainer.findBestVideoByBandwidth(HlsContainer.getHlsQualities(this.br));
            if (hlsbest == null && br.containsHTML("#EXT-X-ENDLIST")) {
                /* 2019-08-29: New */
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
                if (!con.isOK() || con.getContentType().contains("html") || con.getLongContentLength() == -1) {
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