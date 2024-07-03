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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.downloader.hls.HLSDownloader;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "beeg.com" }, urls = { "https?://(?:www\\.)?beeg\\.com/-?\\d+(?:\\?t=\\d+-\\d+)?|https?://beta\\.beeg\\.com/-\\d+(?:\\?t=\\d+-\\d+)?" })
public class BeegCom extends PluginForHost {
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
        final String extDefault = ".mp4";
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + extDefault);
        }
        server_issue = false;
        final String videoidOriginal = getFID(link);
        if (videoidOriginal == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String title = null;
        boolean isHLS = false;
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
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        /* 2021-07-26: Seems like all objects in this array lead to the same video. */
        final List<Map<String, Object>> vids = (List<Map<String, Object>>) entries.get("fc_facts");
        final Map<String, Object> file = (Map<String, Object>) entries.get("file");
        final List<Map<String, Object>> data = (List<Map<String, Object>>) file.get("data");
        final Map<String, Object> stuff = (Map<String, Object>) file.get("stuff");
        if (stuff != null) {
            /* 2024-05-06: The "stuff" map si not always given. */
            title = (String) stuff.get("sf_name");
        }
        if (StringUtils.isEmpty(title) && data != null && data.size() > 0) {
            title = (String) data.get(0).get("cd_value");
        }
        Map<String, String> qualities_http = null;
        Map<String, String> qualities_hls = null;
        for (final Map<String, Object> vid : vids) {
            qualities_http = (Map<String, String>) vid.get("resources");
            qualities_hls = (Map<String, String>) vid.get("hls_resources");
            if (qualities_http != null || qualities_hls != null) {
                break;
            }
        }
        if (qualities_http == null || qualities_http.size() == 0) {
            /* E.g. videos without "t" parameter inside URL. */
            qualities_http = (Map<String, String>) file.get("resources");
        }
        if (qualities_hls == null || qualities_hls.size() == 0) {
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
        if (!StringUtils.isEmpty(title)) {
            title = title.trim();
            link.setFinalFileName(this.applyFilenameExtension(title, extDefault));
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
                try {
                    basicLinkCheck(br.cloneBrowser(), br.createGetRequest(dllink), link, link.getFinalFileName(), extDefault);
                } catch (Exception e) {
                    logger.log(e);
                    server_issue = true;
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