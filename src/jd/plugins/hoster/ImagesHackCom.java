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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.appwork.utils.StringUtils;
import org.appwork.utils.parser.UrlQuery;
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
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.ImagesHackComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { ImagesHackComCrawler.class })
public class ImagesHackCom extends PluginForHost {
    private static final String  TYPE_DOWNLOAD    = "(?i)https?://[^/]+/f/\\d+/[^<>\"/]+";
    private static final String  TYPE_IMAGE       = "(?i)https?://[^/]+/i/[A-Za-z0-9]+";
    private static final boolean enable_api_image = true;
    // private static final String TYPE_DIRECT =
    // "https?://imagizer\\.imageshack\\.(?:com|us)/(?:a/img\\d+/\\d+/|v2/\\d+x\\d+q\\d+/\\d+/)([A-Za-z0-9]+)\\.[A-Za-z]{3,5}";
    private String               DLLINK           = null;
    private String               fid              = null;

    private static List<String[]> getPluginDomains() {
        return ImagesHackComCrawler.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:i/[A-Za-z0-9]+|f/\\d+/[^<>\"/]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ImagesHackCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_HOST };
    }

    @Override
    public String getAGBLink() {
        return "https://imageshack.com/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private String getContentURL(final DownloadLink link) {
        return link.getPluginPatternMatcher().replace("imageshack.us/", "imageshack.com/").replaceFirst("(?i)http://", "https://");
    }

    /** Using API: https://api.imageshack.com/ */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        DLLINK = null;
        this.setBrowserExclusive();
        /* Set password-cookies if needed */
        final String passCode = link.getDownloadPassword();
        final String pwcookie = link.getStringProperty("pwcookie");
        if (pwcookie != null) {
            final String[] cookieinfo = pwcookie.split(":");
            this.br.setCookie(this.getHost(), cookieinfo[0], cookieinfo[1]);
        }
        final String contenturl = getContentURL(link);
        if (contenturl.matches(TYPE_DOWNLOAD) || br.containsHTML("class=\"download-block\"")) {
            /* Download */
            br.setFollowRedirects(true);
            br.getPage(contenturl);
            if (br.containsHTML("Looks like the image is no longer here")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = br.getRegex("\"(https?://imageshack\\.us/download/[^<>\"]*?)\"").getMatch(0);
        } else if (enable_api_image) {
            /* Image + usage of API. */
            prepBR_API(this.br);
            this.fid = getFIDFRomURL_image(link);
            final UrlQuery query = new UrlQuery();
            query.add("next_prev_limit", "0");
            query.add("related_images_limit", "0");
            if (passCode != null) {
                query.add("password", Encoding.urlEncode(passCode));
            }
            br.getPage("https://api.imageshack.com/v2/images/" + this.fid + "?" + query.toString());
            if (this.br.getHttpConnection().getResponseCode() == 401) {
                /*
                 * TThis case is nearly impossible as only albums can be password protected --> Correct password should already be available
                 * through decrypter but okay I guess users could also open such folders via browser, then add links to JDownloader - plus
                 * passwords could be changed too.
                 */
                link.setPasswordProtected(true);
                return AvailableStatus.TRUE;
            } else {
                link.setPasswordProtected(false);
            }
            if (this.br.getHttpConnection().getResponseCode() != 200) {
                /* Typically response 500 for offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            Map<String, Object> map = (Map<String, Object>) JavaScriptEngineFactory.jsonToJavaObject(br.getRequest().getHtmlCode());
            map = (Map<String, Object>) map.get("result");
            final AvailableStatus status = apiImageGetAvailablestatus(this, link, map);
            DLLINK = (String) map.get("direct_link");
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (!DLLINK.startsWith("http")) {
                DLLINK = "https://" + DLLINK;
            }
            return status;
        } else {
            /* Image - handling via website. */
            this.fid = getFIDFRomURL_image(link);
            br.setFollowRedirects(false);
            br.getPage(contenturl);
            if (br.getRedirectLocation() != null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            DLLINK = br.getRegex("data\\-width=\"0\" data\\-height=\"0\" alt=\"\" src=\"(//imagizer\\.imageshack[^<>\"]*?)\"").getMatch(0);
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            DLLINK = "http" + DLLINK;
        }
        br.setFollowRedirects(true);
        if (DLLINK != null && !isDownload) {
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(DLLINK);
                handleConnectionErrors(br, con);
                final String filenameFromHeader = getFileNameFromHeader(con);
                if (filenameFromHeader != null) {
                    link.setName(filenameFromHeader);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } catch (final Throwable e) {
                try {
                    con.disconnect();
                } catch (final Throwable e2) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    public static AvailableStatus apiImageGetAvailablestatus(final Plugin plugin, final DownloadLink dl, Map<String, Object> json) {
        final Object error = json.get("error");
        if (error != null) {
            /* Whatever it is - our picture is probably offline! */
            dl.setAvailable(false);
            return AvailableStatus.FALSE;
        }
        final String id = api_json_get_id(json);
        final long filesize = JavaScriptEngineFactory.toLong(json.get("filesize"), -1);
        final String username = api_json_get_username(json);
        final String album = api_json_get_album(json);
        final Object isDeleted_o = json.get("hidden");
        final boolean isDeleted;
        if (isDeleted_o != null) {
            isDeleted = ((Boolean) json.get("hidden")).booleanValue();
        } else {
            isDeleted = false;
        }
        /*
         * Do NOT use 'original_filename' as it can happen that file got converted on the imageshack servers so we'd have a wrong file
         * extension!
         */
        String filename = (String) json.get("filename");
        if (filename == null || filesize == -1 || isDeleted) {
            dl.setAvailable(false);
            return AvailableStatus.FALSE;
        }
        if (!StringUtils.isEmpty(album) && !StringUtils.isEmpty(username)) {
            filename = username + " - " + album + "_" + filename;
        } else {
            if (!StringUtils.isEmpty(username)) {
                /* E.g. username is hidden for private images but images are downloadable/viewable. */
                filename = username + "_" + filename;
            } else if (!StringUtils.isEmpty(album)) {
                /* An image does not necessarily have to be part of an album. */
                filename = album + "_" + filename;
            }
        }
        dl.setFinalFileName(filename);
        if (filesize > 0) {
            /* Happens e.g. when crawling all images of a user - API sometimes randomly returns 0 for filesize. */
            dl.setDownloadSize(filesize * 1024);
        }
        dl.setLinkID(id);
        return AvailableStatus.TRUE;
    }

    public static String api_json_get_album(final Map<String, Object> json) {
        return (String) JavaScriptEngineFactory.walkJson(json, "album/title");
    }

    public static String api_json_get_id(final Map<String, Object> json) {
        return (String) json.get("id");
    }

    public static String api_json_get_username(final Map<String, Object> json) {
        return (String) JavaScriptEngineFactory.walkJson(json, "owner/username");
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        if (link.isPasswordProtected()) {
            // passCode = Plugin.getUserInput("Password?", downloadLink);
            // /* Simply do the availablecheck again - it will use the password. */
            // requestFileInformation(downloadLink);
            // if (this.br.getHttpConnection().getResponseCode() == 401) {
            // downloadLink.setProperty("pass", Property.NULL);
            // throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            // }
            /* Very very very rare case - but until now there is no way to set passwords for single images! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Password protected images are not yet supported.", 3 * 60 * 60 * 1000l);
        }
        // More is possible but 1 chunk is good to prevent errors
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        handleConnectionErrors(br, dl.getConnection());
        // link.setDownloadPassword(this.passCode);
        dl.startDownload();
    }

    private void handleConnectionErrors(final Browser br, final URLConnectionAdapter con) throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404");
            } else if (dl.getConnection().getResponseCode() == 401) {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 401", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 500) {
                /* Should never happen */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Image broken?");
            }
        }
    }

    private String getFIDFRomURL_image(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    public static Browser prepBR_API(final Browser br) {
        /* Password required */
        br.setAllowedResponseCodes(401);
        /* Offline content */
        br.setAllowedResponseCodes(500);
        return br;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}