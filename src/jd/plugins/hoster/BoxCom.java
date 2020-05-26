//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.com" }, urls = { "https?://(?:\\w+\\.)*box\\.(?:com|net)/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})/file/\\d+" })
public class BoxCom extends antiDDoSForHost {
    private static final String TOS_LINK           = "https://www.box.net/static/html/terms.html";
    private static final String fileLink           = "https?://(?:\\w+\\.)*box\\.com/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})/file/\\d+";
    private String              dllink             = null;
    private boolean             is_stream_download = false;

    public BoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return TOS_LINK;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("box.net/", "box.com/"));
    }

    @Override
    public String rewriteHost(String host) {
        if ("box.net".equals(getHost())) {
            if (host == null || "box.net".equals(host)) {
                return "box.com";
            }
        }
        return super.rewriteHost(host);
    }

    public static boolean isOffline(Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<title>Box \\| 404 Page Not Found</title>") || br.containsHTML("error_message_not_found")) {
            return true;
        }
        return false;
    }

    private boolean isPasswordProtected(final Browser br) {
        return (br.containsHTML("passwordRequired") || br.containsHTML("incorrectPassword")) && br.containsHTML("\"status\"\\s*:\\s*403");
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // our default is german, this returns german!!
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        correctDownloadLink(link);
        if (!link.getPluginPatternMatcher().matches(fileLink)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex dlIds = new Regex(link.getPluginPatternMatcher(), "box\\.com/s/([a-z0-9]+)/file/(\\d+)");
        final String sharedname = dlIds.getMatch(0);
        final String fileid = dlIds.getMatch(1);
        if (sharedname == null || fileid == null) {
            /* This should never happen! */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String rootFolder = new Regex(link.getPluginPatternMatcher(), "(.+)/file/\\d+").getMatch(0);
        final String passCode;
        if (link.hasProperty("passCode")) {
            passCode = link.getStringProperty("passCode", null);
        } else {
            passCode = link.getDownloadPassword();
        }
        if (passCode != null) {
            br.postPage(rootFolder, "password=" + Encoding.urlEncode(passCode));
        } else {
            br.getPage(rootFolder);
        }
        if (isPasswordProtected(br)) {
            // direct link that is password protected?
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String requestToken = br.getRegex("Box\\.config\\.requestToken\\s*=\\s*'(.*?)'").getMatch(0);
        if (StringUtils.isEmpty(requestToken)) {
            requestToken = br.getRegex("requestToken\"\\s*:\\s*\"(.*?)\"").getMatch(0);
            if (StringUtils.isEmpty(requestToken)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        br.getPage(link.getPluginPatternMatcher());
        if (isOffline(br)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // final String results = br.getRegex("<script type=\"text/x-config\">\\s*.*?\"fileName\".*?</script>").getMatch(-1);
        final String results = br.getRegex("\"items\":(.*?\\}\\])").getMatch(0);
        if (results == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String filename = PluginJSonUtils.getJson(results, "name");
        // final String filesize = br.getRegex("Size:\\s*([\\d\\.]+\\s*[KMGT]{0,1}B)").getMatch(0);
        final String filesize = PluginJSonUtils.getJson(results, "itemSize");
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        link.setLinkID("box.com://file/" + fileid);
        final PostRequest tokens = br.createJSonPostRequest("https://app.box.com/app-api/enduserapp/elements/tokens", "{\"fileIDs\":[\"file_" + fileid + "\"]}");
        tokens.getHeaders().put("Request-Token", requestToken);
        tokens.getHeaders().put("X-Request-Token", requestToken);
        tokens.getHeaders().put("X-Box-EndUser-API", "sharedName=" + sharedname);
        tokens.getHeaders().put("X-Box-Client-Name", "enduserapp");
        tokens.getHeaders().put("X-Box-Client-Version", "0.86.0");
        Browser brc = br.cloneBrowser();
        brc.getPage(tokens);
        Map<String, Object> map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        final String token_read = (String) ((Map<String, Object>) map.get("file_" + fileid)).get("read");
        if (StringUtils.isEmpty(token_read)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final GetRequest pre_download_req = br.createGetRequest("https://api.box.com/2.0/files/" + fileid + "?fields=permissions,shared_link,sha1,file_version,name,size,extension,representations,watermark_info,authenticated_download_url,is_download_available");
        pre_download_req.getHeaders().put("Authorization", "Bearer " + token_read);
        pre_download_req.getHeaders().put("BoxApi", "shared_link=https://app.box.com/s/" + sharedname);
        pre_download_req.getHeaders().put("X-Box-Client-Name", "ContentPreview");
        pre_download_req.getHeaders().put("Origin", "https://app.box.com");
        brc = br.cloneBrowser();
        brc.getPage(pre_download_req);
        map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
        final Map<String, Object> permissions = (Map<String, Object>) map.get("permissions");
        final Map<String, Object> file_version = (Map<String, Object>) map.get("file_version");
        /*
         * 2020-05-26: is_download_available always seems to be TRUE while can_download is the interesting one as it will be FALSE for e.g.
         * videostreams which
         */
        // final boolean is_download_available = ((Boolean) map.get("is_download_available")).booleanValue();
        /*
         * 2020-05-26: This is the hash of the original file. We cannot use this if we e.g. download the stream instead so only set it if we
         * are really able to download the original file!
         */
        final String sha1 = (String) map.get("sha1");
        final boolean can_download = ((Boolean) permissions.get("can_download")).booleanValue();
        if (can_download) {
            final GetRequest download_req = br.createGetRequest("https://api.box.com/2.0/files/" + fileid + "?fields=download_url");
            download_req.getHeaders().put("Authorization", "Bearer " + token_read);
            download_req.getHeaders().put("BoxApi", "shared_link=https://app.box.com/s/" + sharedname);
            download_req.getHeaders().put("X-Box-Client-Name", "box-content-preview");
            download_req.getHeaders().put("X-Box-Client-Version", "1.54.0");
            download_req.getHeaders().put("Origin", "https://app.box.com");
            brc = br.cloneBrowser();
            brc.getPage(download_req);
            /*
             * Double-check - trying to request the field 'download_url' for undownloadable content, we will get 403 insufficient
             * permissions!
             */
            if (brc.getHttpConnection().getResponseCode() == 403) {
                /* 2020-05-26: E.g. download is officially not allowed, item can only be watched/streamed */
                throw new AccountRequiredException();
            }
            map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            dllink = (String) map.get("download_url");
            if (!StringUtils.isEmpty(sha1)) {
                link.setSha1Hash(sha1);
            }
        } else {
            /* Try to download files without download button (so far supported: mp4, pdf) */
            logger.info("Download impossible (?) --> Trying to download stream/view if possible");
            final String file_version_id = (String) file_version.get("id");
            /* 2020-05-26: This is NOT a hash we can use for CRC checking! */
            // final String file_version_sha1 = (String) file_version.get("sha1");
            if (StringUtils.isEmpty(file_version_id)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Check and see if a file-stream is available --> Download :) */
            /* Example video (download mp4 instead of DASH stream) */
            /*
             * Instead of: this:
             * https://public.boxcloud.com/api/2.0/internal_files/<file_id>/versions/<version_file_id>/representations/dash/content/
             */
            /*
             * ... we will use this:
             * https://public.boxcloud.com/api/2.0/internal_files/<file_id>/versions/<version_file_id>/representations/mp4/content/
             */
            final UrlQuery query = new UrlQuery();
            query.add("access_token", Encoding.urlEncode(token_read));
            query.add("shared_link", Encoding.urlEncode("https://app.box.com/s/" + sharedname));
            String template_url = null;
            final ArrayList<Object> ressourcelist = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(map, "representations/entries");
            String newFileExtension = null;
            for (final Object fileO : ressourcelist) {
                map = (Map<String, Object>) fileO;
                final String representation = (String) map.get("representation");
                template_url = (String) JavaScriptEngineFactory.walkJson(map, "content/url_template");
                if ("dash".equalsIgnoreCase(representation)) {
                    if (!StringUtils.isEmpty(template_url) && template_url.contains("/dash/")) {
                        dllink = template_url.replace("/dash/", "/mp4/") + "?" + query.toString();
                    } else {
                        /* Fallback to at least try to download */
                        dllink = "https://public.boxcloud.com/api/2.0/internal_files/" + fileid + "/versions/" + file_version_id + "/representations/mp4/content/" + "?" + query.toString();
                    }
                    newFileExtension = "mp4";
                    break;
                } else if ("pdf".equalsIgnoreCase(representation)) {
                    if (!StringUtils.isEmpty(template_url) && template_url.contains("/pdf/")) {
                        dllink += "?" + query.toString();
                    } else {
                        /* Fallback to at least try to download */
                        dllink = "https://public.boxcloud.com/api/2.0/internal_files/" + fileid + "/versions/" + file_version_id + "/representations/pdf/content/" + "?" + query.toString();
                    }
                    newFileExtension = "pdf";
                    break;
                } else {
                    logger.info("Skipping representation type: " + representation);
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                logger.info("This content is not downloadable/viewable at all (?)");
                throw new AccountRequiredException();
            }
            is_stream_download = true;
            /*
             * Because we are downloading not the original content, file-extension can be different from the original which is still in our
             * filename --> Correct that
             */
            String finalname_old = link.getFinalFileName();
            if (finalname_old == null) {
                finalname_old = link.getName();
            }
            if (finalname_old != null && !finalname_old.endsWith("." + newFileExtension) && finalname_old.contains(".")) {
                final String finalname_new = finalname_old.substring(0, finalname_old.lastIndexOf(".")) + "." + newFileExtension;
                logger.info("Old filename = " + finalname_old + " | New filename = " + finalname_new);
                link.setFinalFileName(finalname_new);
            }
            // if (!StringUtils.isEmpty(file_version_sha1)) {
            // link.setSha1Hash(file_version_sha1);
            // }
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        // final boolean isContent = !(dl.getConnection().isContentDisposition() ||
        // StringUtils.equalsIgnoreCase(dl.getConnection().getContentType(), "application/octet-stream") ||
        // StringUtils.equalsIgnoreCase(dl.getConnection().getContentType(), "video/mp4"));
        if (!dl.getConnection().isOK() || (!is_stream_download && !dl.getConnection().isContentDisposition())) {
            logger.info("The final downloadlink seems not to be a file");
            try {
                br.getHttpConnection().setAllowedResponseCodes(new int[] { br.getHttpConnection().getResponseCode() });
                br.followConnection();
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 500) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            if (br.containsHTML("error_message_bandwidth")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The uploader of this file doesn't have enough bandwidth left!", 3 * 60 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}