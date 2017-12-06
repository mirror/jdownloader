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

import java.util.LinkedHashMap;

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
import jd.plugins.components.PluginJSonUtils;

import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "puls4.com" }, urls = { "https?://(?:www\\.)?puls4\\.com/.*" })
public class Puls4Com extends PluginForHost {
    public Puls4Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    /* Nice (old 2016) API, usually only available for mobile devices. */
    private static final boolean use_mobile_api    = true;
    /* Connection stuff */
    private static final boolean free_resume       = true;
    private static final int     free_maxchunks    = 0;
    private static final int     free_maxdownloads = -1;
    private String               dllink            = null;
    private boolean              server_issue      = false;

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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        dllink = null;
        server_issue = false;
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String filename = null;
        String date = null;
        final String urlpart = new Regex(downloadLink.getDownloadURL(), "puls4\\.com/(.+)").getMatch(0);
        if (use_mobile_api) {
            prepBR(this.br);
            /* 2016-09-08: Webpage has changed. */
            String mobileID = new Regex(downloadLink.getDownloadURL(), "(\\d{5,})$").getMatch(0);
            if (mobileID == null) {
                this.br.getPage("http://www." + this.getHost() + "/api/json-fe/page/" + urlpart);
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
            br.getPage("http://www.puls4.com/api/video/single/" + mobileID + "?version=v4");
            /* Offline or geo blocked video */
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.toString().length() <= 10) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(this.br.toString());
            date = (String) entries.get("broadcast_date");
            filename = (String) entries.get("title");
            /* Get highest quality downloadlink */
            final LinkedHashMap<String, Object> files = (LinkedHashMap<String, Object>) entries.get("files");
            if (files == null) {
                /* Video should be offline! */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            LinkedHashMap<String, Object> currentQualityMap = null;
            final String[] qualities = { "h3", "h1", "h2", "h4" };
            for (final String quality : qualities) {
                currentQualityMap = (LinkedHashMap<String, Object>) files.get(quality);
                if (currentQualityMap != null) {
                    dllink = (String) currentQualityMap.get("url");
                    break;
                }
            }
        } else {
            br.getPage(downloadLink.getDownloadURL());
            /* Offline|Other error (e.g. geo block) */
            if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"message\\-error\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            filename = PluginJSonUtils.getJsonValue(br, "episodename");
            dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\",\"hd\":true").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"url\":\"(http:[^<>\"]*?)\",\"hd\":false").getMatch(0);
            }
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (date != null && !date.equals("")) {
            filename = date + "_puls4_" + filename;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        if (dllink == null) {
            filename = filename + ".mp4";
            downloadLink.setName(filename);
        } else {
            downloadLink.setFinalFileName(filename);
            dllink = dllink.replace("\\", "");
            String ext = getFileNameExtensionFromString(dllink, ".mp4");
            filename += ext;
            downloadLink.setFinalFileName(filename);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setProperty("directlink", dllink);
                } else {
                    server_issue = true;
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
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (server_issue) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 60 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, free_resume, free_maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 30 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "(\\d+)$").getMatch(0);
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
