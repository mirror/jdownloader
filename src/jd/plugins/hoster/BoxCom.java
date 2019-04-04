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
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "box.com" }, urls = { "https?://(?:\\w+\\.)*box\\.(?:com|net)/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})/file/\\d+" })
public class BoxCom extends antiDDoSForHost {
    private static final String TOS_LINK = "https://www.box.net/static/html/terms.html";
    private static final String fileLink = "https?://(?:\\w+\\.)*box\\.com/s(?:hared)?/(?:[a-z0-9]{32}|[a-z0-9]{20})/file/\\d+";
    private String              dllink   = null;

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
    public AvailableStatus requestFileInformation(final DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // our default is german, this returns german!!
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        correctDownloadLink(parameter);
        if (parameter.getPluginPatternMatcher().matches(fileLink)) {
            final Regex dlIds = new Regex(parameter.getPluginPatternMatcher(), "box\\.com/s/([a-z0-9]+)/file/(\\d+)");
            final String sharedname = dlIds.getMatch(0);
            final String fileid = dlIds.getMatch(1);
            final String rootFolder = new Regex(parameter.getPluginPatternMatcher(), "(.+)/file/\\d+").getMatch(0);
            final String passCode = parameter.getStringProperty("passCode", null);
            if (passCode != null) {
                br.postPage(rootFolder, "password=" + Encoding.urlEncode(passCode));
            } else {
                br.getPage(rootFolder);
            }
            if (isPasswordProtected(br)) {
                // direct link that is password protected?
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String requestToken = br.getRegex("Box\\.config\\.requestToken\\s*=\\s*'(.*?)'").getMatch(0);
            if (StringUtils.isEmpty(requestToken)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(parameter.getPluginPatternMatcher());
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
            parameter.setName(filename);
            if (filesize != null) {
                parameter.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            parameter.setLinkID("box.com://file/" + fileid);
            final PostRequest tokens = br.createJSonPostRequest("https://app.box.com/app-api/enduserapp/elements/tokens", "{\"fileIDs\":[\"file_" + fileid + "\"]}");
            tokens.getHeaders().put("Request-Token", requestToken);
            tokens.getHeaders().put("X-Request-Token", requestToken);
            tokens.getHeaders().put("X-Box-EndUser-API", "sharedName=" + sharedname);
            tokens.getHeaders().put("X-Box-Client-Name", "enduserapp");
            tokens.getHeaders().put("X-Box-Client-Version", "0.86.0");
            Browser brc = br.cloneBrowser();
            brc.getPage(tokens);
            Map<String, Object> map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            final String read = (String) ((Map<String, Object>) map.get("file_" + fileid)).get("read");
            if (StringUtils.isEmpty(read)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final GetRequest download_url = br.createGetRequest("https://api.box.com/2.0/files/" + fileid + "?fields=download_url");
            download_url.getHeaders().put("Authorization", "Bearer " + read);
            download_url.getHeaders().put("boxapi", "shared_link=https://app.box.com/s/" + sharedname);
            download_url.getHeaders().put("X-Box-Client-Name", "box-content-preview");
            download_url.getHeaders().put("X-Box-Client-Version", "1.54.0");
            download_url.getHeaders().put("Origin", "https://app.box.com");
            brc = br.cloneBrowser();
            brc.getPage(download_url);
            map = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            dllink = (String) map.get("download_url");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                return AvailableStatus.TRUE;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
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