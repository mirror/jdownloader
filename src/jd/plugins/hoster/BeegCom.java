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

import java.util.List;
import java.util.Map;

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
import jd.plugins.PluginForHost;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "beeg.com" }, urls = { "https?://(?:www\\.)?beeg\\.com/-?\\d+(?:\\?t=\\d+-\\d+)?|https?://beta\\.beeg\\.com/-\\d+(?:\\?t=\\d+-\\d+)?" })
public class BeegCom extends PluginForHost {
    /* DEV NOTES */
    /* Porn_plugin */
    private String dllink = null;

    public BeegCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://beeg.com/contacts/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String TYPE_BETA       = "https?://beta\\.beeg\\.com/-(\\d+)(?:\\?t=(\\d+-\\d+))?";
    private static final String TYPE_NORMAL     = "https?://beeg\\.com/-?(\\d+)(?:\\?t=(\\d+-\\d+))?";
    private boolean             server_issue    = false;
    private static final String PROPERTY_IS_HLS = "is_hls";

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
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else if (link.getPluginPatternMatcher().matches(TYPE_BETA)) {
            return new Regex(link.getPluginPatternMatcher(), TYPE_BETA).getMatch(0);
        } else {
            return new Regex(link.getPluginPatternMatcher(), TYPE_NORMAL).getMatch(0);
        }
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        String fid = null;
        if (link != null && StringUtils.equals(getHost(), link.getHost()) && (fid = getFID(link)) != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        server_issue = false;
        final String videoidOriginal = getFID(link);
        if (videoidOriginal == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        /* 2021-08-09: Seems like they've moved 100% to the new version of their website. */
        final boolean enforceNewWebsiteHandling = true;
        boolean isHLS = false;
        if (link.getPluginPatternMatcher().matches(TYPE_BETA) || enforceNewWebsiteHandling) {
            String extraParams = "";
            final String timeParams = UrlQuery.parse(link.getPluginPatternMatcher()).get("t");
            final Regex timeParamsRegex = new Regex(timeParams, "(\\d+)-(\\d+)");
            if (timeParams != null && timeParamsRegex.matches()) {
                extraParams = "?fc_start=" + timeParamsRegex.getMatch(0) + "&fc_end=" + timeParamsRegex.getMatch(1);
            }
            br.getPage("https://store.externulls.com/facts/file/" + videoidOriginal + extraParams);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.toString().length() < 100) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            /* 2021-07-26: Seems like all objects in this array lead to the same video. */
            final List<Map<String, Object>> vids = (List<Map<String, Object>>) entries.get("fc_facts");
            final Map<String, Object> file = (Map<String, Object>) entries.get("file");
            final Map<String, Object> stuff = (Map<String, Object>) file.get("stuff");
            filename = (String) stuff.get("sf_name");
            Map<String, String> qualities_http = null;
            Map<String, String> qualities_hls = null;
            for (final Map<String, Object> vid : vids) {
                qualities_http = (Map<String, String>) vid.get("resources");
                qualities_hls = (Map<String, String>) vid.get("hls_resources");
                if (qualities_http != null || qualities_hls != null) {
                    break;
                }
            }
            if (qualities_http == null) {
                /* E.g. videos without "t" parameter inside URL. */
                qualities_http = (Map<String, String>) file.get("resources");
            }
            if (qualities_hls == null) {
                qualities_hls = (Map<String, String>) file.get("hls_resources");
            }
            Map<String, String> chosenQualities = null;
            if (qualities_http != null) {
                chosenQualities = qualities_http;
            } else if (qualities_hls != null) {
                chosenQualities = qualities_hls;
                isHLS = true;
            }
            if (chosenQualities != null) {
                /* Pick best quality */
                final String[] qualityStrings = { "2160", "1080", "720", "480", "360", "240" };
                for (final String qualityStr : qualityStrings) {
                    final String qualityKey = "fl_cdn_" + qualityStr;
                    if (chosenQualities.containsKey(qualityKey)) {
                        dllink = chosenQualities.get(qualityKey);
                        if (!StringUtils.isEmpty(dllink)) {
                            break;
                        }
                    }
                }
                if (!StringUtils.isEmpty(dllink) && !dllink.startsWith("http")) {
                    dllink = "https://video.beeg.com/" + dllink;
                }
            }
        } else {
            String videoid = videoidOriginal;
            br.getPage(link.getPluginPatternMatcher());
            final String videoidInsideCurrentURL = new Regex(br.getURL(), "https?://[^/]+/(\\d+)").getMatch(0);
            if (videoidInsideCurrentURL != null && !videoidInsideCurrentURL.equals(videoid)) {
                /* 2020-03-16: Redirect to other videoid can happen */
                logger.info(String.format("Continuing with videoid %s instead of %s", videoidInsideCurrentURL, videoid));
                videoid = videoidInsideCurrentURL;
            }
            String[] match = br.getRegex("script src=\"([^\"]+/(\\d+)\\.js)").getRow(0);
            String jsurl = null;
            String beegVersion = null;
            if (match != null) {
                jsurl = match[0];
                beegVersion = match[1];
            }
            /* 2019-01-16: Salt is not always given/required */
            String salt = null;
            beegVersion = br.getRegex("var beeg_version\\s*=\\s*(\\d+);").getMatch(0);
            if (beegVersion == null) {
                /* 2020-07-07 */
                String jsURL = br.getRegex("(/js/app\\.[a-z0-9]+\\.js)").getMatch(0);
                if (jsURL == null) {
                    /* 2020-07-07: Static fallback */
                    jsURL = "/js/app.ade4860b.js";
                }
                final Browser brc = br.cloneBrowser();
                brc.getPage(jsURL);
                beegVersion = brc.getRegex("service-worker\\.js\\?version=\"\\)\\.concat\\(\"(\\d+)\"\\),").getMatch(0);
            }
            if (beegVersion == null) {
                /* 2020-07-07: Static fallback */
                beegVersion = "1593627308202";
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (jsurl != null) {
                final Browser cbr = br.cloneBrowser();
                cbr.getPage(jsurl);
                salt = cbr.getRegex("beeg_salt=\"([^\"]+)").getMatch(0);
                if (salt == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            String v_startStr = null;
            String v_endStr = null;
            /* 2019-09-16: Sometimes these two values are given inside our URL --> Use them! */
            String v_times_information = new Regex(link.getPluginPatternMatcher(), "\\?t=(.+)").getMatch(0);
            if (v_times_information == null) {
                /* 2020-03-16: URLs without this information may redirect to an URL containing it */
                v_times_information = new Regex(br.getURL(), "\\?t=(.+)").getMatch(0);
            }
            if (v_times_information != null) {
                final String[] v_times_informationList = v_times_information.split("\\-");
                if (v_times_informationList.length == 2) {
                    v_startStr = v_times_informationList[0];
                    v_endStr = v_times_informationList[1];
                }
            }
            Map<String, Object> entries = null;
            boolean useApiV1 = false;
            if (v_startStr == null && v_endStr == null) {
                useApiV1 = true;
                /* 2019-07-16: This basically loads the whole website - we then need to find the element the user wants to download. */
                br.getPage("//beeg.com/api/v6/" + beegVersion + "/index/main/0/pc");
                entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                final List<Object> ressourcelist = (List<Object>) entries.get("videos");
                for (final Object videoO : ressourcelist) {
                    entries = (Map<String, Object>) videoO;
                    final String videoidTemp = Long.toString(JavaScriptEngineFactory.toLong(entries.get("svid"), -1));
                    final String videoidTemp2 = Long.toString(JavaScriptEngineFactory.toLong(entries.get("id"), -1));
                    if ((videoidTemp != null && videoid.equals(videoidTemp)) || (videoidTemp2 != null && videoid.equals(videoidTemp2))) {
                        useApiV1 = false;
                        /* Example v2video: 1059800872 */
                        // if (!entries.containsKey("start") && !entries.containsKey("end")) {
                        // /* 2019-08-14: They can be null but they should be present in their json! */
                        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        // }
                        final long v_start = JavaScriptEngineFactory.toLong(entries.get("start"), -1);
                        final long v_end = JavaScriptEngineFactory.toLong(entries.get("end"), -1);
                        if (v_start > -1 && v_end > -1) {
                            /* 2019-07-16: These values are required to call the API to access single objects! */
                            v_startStr = Long.toString(v_start);
                            v_endStr = Long.toString(v_end);
                        }
                        break;
                    }
                }
            }
            if (!useApiV1) {
                String get_parameters = "?v=2";
                if (v_startStr != null && v_endStr != null) {
                    /* 2019-07-16: These values are required to call the API to access single objects! */
                    get_parameters += "&s=" + v_startStr + "&e=" + v_endStr;
                }
                br.getPage("/api/v6/" + beegVersion + "/video/" + videoid + get_parameters);
                if (br.getHttpConnection().getResponseCode() == 404) {
                    /* 2019-08-20: Ultimate fallback */
                    logger.info("Video is offline according to APIv2 --> Trying APIv1");
                    useApiV1 = true;
                } else {
                    entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
                }
            }
            if (useApiV1) {
                /* Example v1video: 6471530 */
                logger.info("Failed to find extra data for desired content --> Falling back to apiv1");
                br.getPage("/api/v6/" + beegVersion + "/video/" + videoid + "?v=2");
                entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
            }
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = (String) entries.get("title");
            if (StringUtils.isEmpty(filename)) {
                filename = videoidOriginal;
            }
            final String[] qualities = { "2160", "1080", "720", "480", "360", "240" };
            for (final String quality : qualities) {
                dllink = (String) entries.get(quality + "p");
                if (!StringUtils.isEmpty(dllink)) {
                    break;
                }
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (dllink.startsWith("//")) {
                dllink = "https:" + dllink;
            }
            dllink = dllink.replace("{DATA_MARKERS}", "data=pc.XX");
            final String key = new Regex(this.dllink, "/key=([^<>\"=]+)%2Cend=").getMatch(0);
            if (key != null && salt != null) {
                String deckey = decryptKey(key, salt);
                dllink = dllink.replace(key, deckey).replace("%2C", ",");
            }
        }
        if (!StringUtils.isEmpty(filename)) {
            filename = filename.trim();
            if (filename.endsWith(".")) {
                filename = filename.substring(0, filename.length() - 1);
            }
            filename += ".mp4";
            link.setFinalFileName(filename);
        }
        br.setFollowRedirects(true);
        br.getHeaders().put("Referer", link.getPluginPatternMatcher());
        if (dllink != null) {
            if (isHLS) {
                link.setProperty(PROPERTY_IS_HLS, true);
            } else {
                link.removeProperty(PROPERTY_IS_HLS);
            }
            if (!isDownload && !isHLS) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openGetConnection(dllink);
                    if (this.looksLikeDownloadableContent(con)) {
                        if (con.getCompleteContentLength() > 0) {
                            link.setVerifiedFileSize(con.getCompleteContentLength());
                        }
                    } else {
                        server_issue = true;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        if (server_issue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server issue", 30 * 60 * 1000l);
        } else if (StringUtils.isEmpty(this.dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (isHLS(link)) {
            checkFFmpeg(link, "Download a HLS Stream");
            dl = new HLSDownloader(link, br, this.dllink);
            dl.startDownload();
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private boolean isHLS(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_IS_HLS)) {
            return true;
        } else {
            return false;
        }
    }

    private String decryptKey(final String key, final String salt) {
        String decodeKey = Encoding.htmlDecode(key);
        int s = salt.length();
        StringBuffer t = new StringBuffer();
        for (int o = 0; o < decodeKey.length(); o++) {
            char l = decodeKey.charAt(o);
            int n = o % s;
            int i = salt.charAt(n) % 21;
            t.append(String.valueOf(Character.toChars(l - i)));
        }
        String result = t.toString();
        result = strSplitReverse(result, 3, true);
        return result;
    }

    private String strSplitReverse(final String key, final int e, final boolean t) {
        String n = key;
        StringBuffer r = new StringBuffer();
        if (t) {
            int a = n.length() % e;
            if (a > 0) {
                r.append(new StringBuffer(n.substring(0, a)).reverse());
                n = n.substring(a);
            }
        }
        for (; n.length() > e;) {
            r.append(new StringBuffer(n.substring(0, e)).reverse());
            n = n.substring(e);
        }
        r.append(new StringBuffer(n).reverse());
        return r.reverse().toString();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}