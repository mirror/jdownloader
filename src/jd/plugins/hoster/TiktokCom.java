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
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.TiktokConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tiktok.com" }, urls = { "https?://(?:www\\.)?tiktok\\.com/((@[^/]+)/video/|embed/)(\\d+)|https?://m\\.tiktok\\.com/v/(\\d+)\\.html" })
public class TiktokCom extends antiDDoSForHost {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
        try {
            Browser.setRequestIntervalLimitGlobal("tiktok.com", true, 1000);
        } catch (final Throwable e) {
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.tiktok.com/";
    }

    /* Connection stuff */
    private final boolean RESUME    = true;
    /* 2019-07-10: More chunks possible but that would not be such a good idea! */
    private final int     MAXCHUNKS = 1;

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
        return new Regex(link.getPluginPatternMatcher(), "/(?:video|v|embed)/(\\d+)").getMatch(0);
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
        this.setBrowserExclusive();
        String user = null;
        final String fid = getFID(link);
        if (fid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getPluginPatternMatcher().matches(".+/@[^/]+/video/\\d+.*?")) {
            user = new Regex(link.getPluginPatternMatcher(), "/(@[^/]+)/").getMatch(0);
        } else {
            /* 2nd + 3rd linktype which does not contain username --> Find username by finding original URL */
            br.setFollowRedirects(false);
            br.getPage(String.format("https://m.tiktok.com/v/%s.html", fid));
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                user = new Regex(redirect, "/(@[^/]+)/").getMatch(0);
                if (user != null) {
                    /* Set new URL so we do not have to handle that redirect next time. */
                    link.setPluginPatternMatcher(redirect);
                }
            }
        }
        String filename = "";
        if (user != null) {
            filename += user + "_";
        }
        filename += fid + ".mp4";
        if (PluginJsonConfig.get(this.getConfigInterface()).isEnableFastLinkcheck() && !isDownload) {
            br.getPage("https://www." + this.getHost() + "/oembed?url=" + Encoding.urlEncode("https://www.tiktok.com/video/" + fid));
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(this.br.toString());
            final String status_msg = (String) entries.get("status_msg");
            final String type = (String) entries.get("type");
            if (!"video".equalsIgnoreCase(type)) {
                /* This should never happen */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (!StringUtils.isEmpty(status_msg)) {
                /* {"status_msg":"Something went wrong"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String title = (String) entries.get("title");
            if (!StringUtils.isEmpty(title) && StringUtils.isEmpty(link.getComment())) {
                link.setComment(title);
            }
            /* Do not set final filename here! */
            link.setName(filename);
        } else {
            String text_hashtags = null;
            String createDate = null;
            final boolean use_new_way = true;
            if (use_new_way) {
                // br.getPage(link.getPluginPatternMatcher());
                /* Old version: https://www.tiktok.com/embed/<videoID> */
                // br.getPage(String.format("https://www.tiktok.com/embed/%s", fid));
                /* Required headers! */
                br.getHeaders().put("sec-fetch-dest", "iframe");
                br.getHeaders().put("sec-fetch-mode", "navigate");
                // br.getHeaders().put("sec-fetch-site", "cross-site");
                // br.getHeaders().put("upgrade-insecure-requests", "1");
                br.getHeaders().put("Referer", link.getPluginPatternMatcher());
                br.getPage("https://www.tiktok.com/embed/v2/" + fid);
                if (this.br.getHttpConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String videoJson = br.getRegex("crossorigin=\"anonymous\">(.*?)</script>").getMatch(0);
                LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(videoJson);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "props/pageProps/videoData/itemInfos");
                /* 2020-10-12: Hmm reliably checking for offline is complicated so let's try this instead ... */
                if (entries == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                // entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "videoData/itemInfos");
                createDate = Long.toString(JavaScriptEngineFactory.toLong(entries.get("createTime"), 0));
                text_hashtags = (String) entries.get("text");
                /* 2020-10-26: Doesn't work anymore, returns 403 */
                dllink = (String) JavaScriptEngineFactory.walkJson(entries, "video/urls/{0}");
                // {
                // /* 2020-10-26: Test */
                // dllink = br.getRegex("<video src=\"(https?://[^<>\"]+)\"").getMatch(0);
                // if (Encoding.isHtmlEntityCoded(dllink)) {
                // dllink = Encoding.htmlDecode(dllink);
                // }
                // }
                if (isDownload) {
                    /* 2020-10-26: Workaround (??!) */
                    this.dllink = generateDownloadurlOld(link);
                }
            } else {
                /* Rev. 40928 and earlier */
                this.dllink = generateDownloadurlOld(link);
            }
            if (!StringUtils.isEmpty(createDate)) {
                final String dateFormatted = convertDateFormat(createDate);
                filename = dateFormatted + "_" + filename;
            }
            link.setFinalFileName(filename);
            if (!StringUtils.isEmpty(text_hashtags) && StringUtils.isEmpty(link.getComment())) {
                link.setComment(text_hashtags);
            }
            /* 2020-09-16: Directurls can only be used one time! If tried to re-use, this will happen: HTTP/1.1 403 Forbidden */
            br.setFollowRedirects(true);
            if (!StringUtils.isEmpty(dllink) && !isDownload) {
                URLConnectionAdapter con = null;
                try {
                    final Browser brc = br.cloneBrowser();
                    brc.setFollowRedirects(true);
                    con = openAntiDDoSRequestConnection(brc, brc.createHeadRequest(dllink));
                    if (!this.looksLikeDownloadableContent(con)) {
                        server_issues = true;
                        try {
                            brc.followConnection(true);
                        } catch (final IOException e) {
                            logger.log(e);
                        }
                    } else {
                        /*
                         * 2020-05-04: Do not use header anymore as it seems like they've modified all files < December 2019 so their
                         * "Header dates" are all wrong now.
                         */
                        // createDate = con.getHeaderField("Last-Modified");
                        if (con.getCompleteContentLength() > 0) {
                            link.setDownloadSize(con.getCompleteContentLength());
                        }
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    private String generateDownloadurlOld(final DownloadLink link) throws IOException {
        this.br.getPage("https://www.tiktok.com/node/video/playwm?id=" + this.getFID(link));
        return new URL(br.toString()).toString();
    }

    private String convertDateFormat(String sourceDate) {
        if (sourceDate == null) {
            return null;
        }
        String result = null;
        SimpleDateFormat target_format = new SimpleDateFormat("yyyy-MM-dd");
        if (sourceDate.matches("\\d+")) {
            /* Timestamp */
            final Date theDate = new Date(Long.parseLong(sourceDate) * 1000);
            result = target_format.format(theDate);
        } else {
            final String sourceDatePart = new Regex(sourceDate, "^[A-Za-z]+, (\\d{1,2} \\w+ \\d{4})").getMatch(0);
            if (sourceDatePart == null) {
                return sourceDate;
            }
            sourceDate = sourceDatePart;
            final SimpleDateFormat source_format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            try {
                try {
                    final Date date = source_format.parse(sourceDate);
                    result = target_format.format(date);
                } catch (Throwable e) {
                }
            } catch (Throwable e) {
                result = sourceDate;
                return sourceDate;
            }
        }
        return result;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        doFree(link, RESUME, MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.getHeaders().put("Referer", "https://www.tiktok.com/");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return PluginJsonConfig.get(getConfigInterface()).getMaxSimultaneousDownloads();
    }

    @Override
    public Class<TiktokConfig> getConfigInterface() {
        return TiktokConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}