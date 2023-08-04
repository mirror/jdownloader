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
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "puls4.com" }, urls = { "https?://(?:www\\.)?puls4\\.com/.+" })
public class Puls4Com extends PluginForHost {
    public Puls4Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.VIDEO_STREAMING };
    }

    /* Nice (old 2016) API, usually only available for mobile devices. */
    private static final boolean use_mobile_api    = true;
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "http://www.puls4.com/cms_content/agb";
    }

    private Browser prepBR(final Browser br) {
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Linux; U; Android 2.2.1; en-us; Nexus One Build/FRG83) AppleWebKit/533.1 (KHTML, like Gecko) Version/4.0 Mobile");
        return br;
    }

    @Override
    public boolean isValidURL(String URL) {
        try {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.setFollowRedirects(true);
            prepBR(br);
            final String urlpart = new Regex(URL, "puls4\\.com/(.+)").getMatch(0);
            String mobileID = new Regex(URL, "(\\d{5,})$").getMatch(0);
            if (mobileID == null) {
                br.getPage("https://www." + this.getHost() + "/api/json-fe/page/" + urlpart);
                br.getRequest().setHtmlCode(br.toString().replace("\\", ""));
                mobileID = this.br.getRegex("/video-grid/(\\d+)").getMatch(0);
                if (mobileID == null) {
                    return br.containsHTML("playerVideo");
                } else {
                    return true;
                }
            }
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("https://www.puls4.com/api/video/single/" + mobileID + "?version=v4");
            if (br.getHttpConnection().getResponseCode() == 404 || "[]".equals(br.toString())) {
                return false;
            } else {
                return true;
            }
        } catch (final Throwable e) {
            logger.log(e);
        }
        return false;
    }

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        dllink = null;
        final String extDefault = ".mp4";
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String title = null;
        String date = null;
        final String urlpart = new Regex(link.getDownloadURL(), "puls4\\.com/(.+)").getMatch(0);
        if (use_mobile_api) {
            prepBR(this.br);
            /* 2016-09-08: Webpage has changed. */
            String mobileID = new Regex(link.getDownloadURL(), "(\\d{5,})$").getMatch(0);
            if (mobileID == null) {
                this.br.getPage("https://www." + this.getHost() + "/api/json-fe/page/" + urlpart);
                this.br.getRequest().setHtmlCode(this.br.toString().replace("\\", ""));
                mobileID = this.br.getRegex("/video-grid/(\\d+)").getMatch(0);
                if (mobileID == null) {
                    /* E.g. "id":"playerVideo" */
                    if (!this.br.containsHTML("playerVideo")) {
                        /* Probably offline! */
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            /* API can even avoid geo blocks! */
            /* 2016-09-09: Changed from "m.puls4.com" to "www.puls4.com" */
            br.getPage("https://www.puls4.com/api/video/single/" + mobileID + "?version=v4");
            /* Offline or geo blocked video */
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() <= 10) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Map<String, Object> entries = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            date = (String) entries.get("broadcast_date");
            title = (String) entries.get("title");
            /* Get highest quality downloadlink */
            final Map<String, Object> files = (Map<String, Object>) entries.get("files");
            if (files == null) {
                /* Video should be offline! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> currentQualityMap = null;
            final String[] qualities = { "h3", "h1", "h2", "h4" };
            for (final String quality : qualities) {
                currentQualityMap = (Map<String, Object>) files.get(quality);
                if (currentQualityMap != null) {
                    dllink = (String) currentQualityMap.get("url");
                    break;
                }
            }
        } else {
            br.getPage(link.getDownloadURL());
            /* Offline|Other error (e.g. geo block) */
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"message\\-error\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            title = PluginJSonUtils.getJsonValue(br, "episodename");
            dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\",\"hd\":true").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\",\"hd\":false").getMatch(0);
            }
        }
        if (title != null) {
            if (date != null && !date.equals("")) {
                title = date + "_puls4_" + title;
            }
            title = Encoding.htmlDecode(title);
            title = title.trim();
            link.setName(this.correctOrApplyFileNameExtension(title, extDefault));
        }
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(this.dllink);
                handleConnectionErrors(br, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
                final String ext = Plugin.getExtensionFromMimeTypeStatic(con.getContentType());
                if (ext != null) {
                    link.setFinalFileName(this.correctOrApplyFileNameExtension(title, "." + ext));
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, free_resume, free_maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Video broken?");
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_maxdownloads;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
