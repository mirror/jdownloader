//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import java.util.ArrayList;
import java.util.LinkedHashMap;

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "docdroid.net" }, urls = { "https?://(?:www\\.)?(?:docdroid\\.net|docdro\\.id)/([A-Za-z0-9\\-]+)(?:/[^/]+)?" })
public class DocdroidNet extends PluginForHost {
    public DocdroidNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://www.docdroid.net/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private LinkedHashMap<String, Object> entries = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        /* Website hosts documents ONLY! */
        link.setMimeHint(CompiledFiletypeFilter.DocumentExtensions.PDF);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String linkid = this.getLinkID(link);
        /*
         * Also oembed API existant but this does not return that much information:
         * https://www.docdroid.net/api/oembed?url=https%3A%2F%2Fwww.docdroid.net%2FC<linkid>%2F<url_filename>
         */
        br.getPage("https://www." + this.getHost() + "/api/document/" + linkid);
        String filename = null;
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* {"message": "Oops... Document not found"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /* Probably password protected - we cannot use the API then but we can get the filename via HTML */
            br.getPage(link.getPluginPatternMatcher());
            /* Double-check offline */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (isPasswordProtected()) {
                logger.info("Document is password protected");
            } else {
                /* Strange case, this should never happen - continue anyways! */
            }
            filename = br.getRegex("<h1 class=\"text\\-center\">([^<>\"]+)</h1>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("title>([^<>\"]+) \\- DocDroid</title>").getMatch(0);
            }
        } else {
            try {
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
                entries = (LinkedHashMap<String, Object>) entries.get("data");
            } catch (final Throwable e) {
            }
            filename = (String) entries.get("filename");
        }
        if (filename == null) {
            /* Fallback */
            filename = linkid;
            /* Do not set final filename as we're missing the extension! */
            link.setName(filename);
        } else {
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        /* If previous request was made via API --> Check for boolean, if not, assume that a download is possible. */
        final boolean download_allowed = entries == null ? true : ((Boolean) entries.get("allow_download")).booleanValue();
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (StringUtils.isEmpty(dllink)) {
            if (isPasswordProtected()) {
                logger.info("Handling password protected");
                /* Password protected */
                /* Get password */
                String passCode = downloadLink.getDownloadPassword();
                if (passCode == null) {
                    passCode = getUserInput("Password?", downloadLink);
                }
                /* Get data for required headers */
                final String csrfToken = PluginJSonUtils.getJson(br, "csrfToken");
                final String xxsrftoken = br.getCookie(br.getURL(), "XSRF-TOKEN");
                if (StringUtils.isEmpty(csrfToken) || StringUtils.isEmpty(xxsrftoken)) {
                    logger.warning("Failed to find data for required headers");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                /*
                 * Correct password will also change cookie 'laravel_session' but there is no need to store it as we store the final
                 * downloadurl which will usually be valid for the next download try as well
                 */
                final PostRequest downloadReq = br.createJSonPostRequest(br.getURL(), "{\"password\":\"" + passCode + "\",\"errors\":{\"errors\":{}},\"busy\":true,\"successful\":false}");
                downloadReq.getHeaders().put("content-type", "application/json;charset=UTF-8");
                downloadReq.getHeaders().put("x-requested-with", "XMLHttpRequest");
                downloadReq.getHeaders().put("x-csrf-token", csrfToken);
                downloadReq.getHeaders().put("x-xsrf-token", xxsrftoken);
                downloadReq.getHeaders().put("origin", "https://www." + br.getHost());
                br.openRequestConnection(downloadReq);
                br.loadConnection(null);
                final String error = PluginJSonUtils.getJson(br, "error");
                if (!StringUtils.isEmpty(error) || isPasswordProtected()) {
                    /* Wrong password */
                    downloadLink.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
                /* Save correct password */
                downloadLink.setDownloadPassword(passCode);
                /*  */
            } else {
                /* NOT password protected --> First try via API but this does usually not return a downloadlink (2019-01-30) */
                logger.info("Trying to find downloadlink via API");
                dllink = getDllink();
            }
            if (StringUtils.isEmpty(dllink)) {
                /* Try via website html + json */
                logger.info("Trying to find downloadlink via website");
                /* Only access website if we're not already on it (e.g. link pass PW protected) */
                if (br.getURL().contains("/api/")) {
                    br.getPage(downloadLink.getPluginPatternMatcher());
                }
                final String json = br.getRegex("json=\\'(\\{.*?\\})\\'").getMatch(0);
                entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(json);
                dllink = getDllink();
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            logger.warning("Failed to find downloadlink");
            if (!download_allowed) {
                /* 2019-01-30: Did not find any testlinks for this case */
                throw new PluginException(LinkStatus.ERROR_FATAL, "This document is not downloadable");
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private boolean isPasswordProtected() {
        return br.getURL().contains("/password");
    }

    private String getDllink() {
        String dllink = null;
        try {
            ArrayList<Object> linklist = (ArrayList<Object>) entries.get("links");
            for (final Object linkO : linklist) {
                entries = (LinkedHashMap<String, Object>) linkO;
                final String type = (String) entries.get("rel");
                final String url = (String) entries.get("uri");
                if ("download".equalsIgnoreCase(type) && !StringUtils.isEmpty(url)) {
                    dllink = url;
                    break;
                }
            }
        } catch (final Throwable e) {
        }
        if (StringUtils.isEmpty(dllink)) {
            /* Last chance - try via RegEx */
            dllink = br.getRegex("(https?://[^/\"]+/file/download/[^\"\\']+)").getMatch(0);
        }
        return dllink;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
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
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}