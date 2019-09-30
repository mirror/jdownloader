//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.requests.PostRequest;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com", "boards.wetransfer.com" }, urls = { "https?://wetransferdecrypted/[a-f0-9]{46}/[a-f0-9]{4,12}/[a-f0-9]{46}", "https?://boards\\.wetransfer\\.com/board/[a-z0-9]+" })
public class WeTransferCom extends PluginForHost {
    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://wetransfer.info/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static Browser prepBRWebsite(final Browser br) {
        br.addAllowedResponseCodes(new int[] { 410, 503 });
        br.setCookie("wetransfer.com", "wt_tandc", "20170208");
        return br;
    }

    private Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "okhttp/3.12.0");
        br.setAllowedResponseCodes(new int[] { 401 });
        return br;
    }

    private String              downloadurl     = null;
    private boolean             isSingleZIP     = false;
    public static final Object  LOCK            = new Object();
    /* 2019-09-30: https://play.google.com/store/apps/details?id=com.wetransfer.app.live */
    private static final String API_BASE_AUTH   = "https://api.wetransfermobile.com/v1";
    private static final String API_BASE_NORMAL = "https://api.wetransfermobile.com/v2";

    private boolean isTypeDownload(final DownloadLink dl) {
        return dl.getPluginPatternMatcher().matches("https?://wetransferdecrypted/[a-f0-9]{46}/[a-f0-9]{4,12}/[a-f0-9]{46}");
    }

    private String getFID(final DownloadLink link) {
        if (isTypeDownload(link)) {
            return link.getPluginPatternMatcher();
        } else {
            /* boards.wetransfer.com URL */
            return new Regex(link.getPluginPatternMatcher(), "([a-z0-9]+)$").getMatch(0);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        if (isTypeDownload(link)) {
            br = prepBRWebsite(new Browser());
            final String[] dlinfo = link.getPluginPatternMatcher().replace("http://wetransferdecrypted/", "").split("/");
            final String id_main = dlinfo[0];
            final String security_hash = dlinfo[1];
            final String id_single = dlinfo[2];
            if (security_hash == null || id_main == null || id_single == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String referer = link.getStringProperty("referer");
            if (referer == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(referer);
            final String[] recipient_id = referer.replaceFirst("https?://[^/]+/+", "").split("/");
            if (recipient_id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String csrfToken = br.getRegex("name\\s*=\\s*\"csrf-token\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
            final Map<String, Object> map = new HashMap<String, Object>();
            map.put("security_hash", security_hash);
            map.put("file_ids", Arrays.asList(new String[] { id_single }));
            if (recipient_id.length == 4) {
                map.put("recipient_id", recipient_id[2]);
            }
            final PostRequest post = new PostRequest(br.getURL(("/api/v4/transfers/" + id_main + "/download")));
            post.getHeaders().put("Accept", "application/json");
            post.getHeaders().put("Content-Type", "application/json");
            if (csrfToken != null) {
                post.getHeaders().put("X-CSRF-Token", csrfToken);
            }
            post.setPostDataString(JSonStorage.toString(map));
            br.getPage(post);
            if ("invalid_transfer".equals(PluginJSonUtils.getJsonValue(br, "error"))) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            downloadurl = PluginJSonUtils.getJsonValue(br, "direct_link");
            if (downloadurl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else {
            /* Boards - so far we only allow our users to download all contents of the added board as .zip. */
            final String fid = this.getFID(link);
            String token = this.getPluginConfig().getStringProperty("api_token", null);
            int counter = 0;
            boolean authed = false;
            do {
                /* Clear old headers and cookies each loop */
                br = prepBRAPI(new Browser());
                try {
                    if (token == null) {
                        /* Only generate new token if needed */
                        synchronized (LOCK) {
                            /*
                             * 2019-09-30: E.g. {"device_token":
                             * "wt-android-<hash-length-8>-<hash-length-4>-<hash-length-4>-<hash-length-4>-<hash-length-12>"}
                             */
                            br.postPageRaw(API_BASE_AUTH + "/authorize", "{\"device_token\":\"wt-android-\"}");
                            token = PluginJSonUtils.getJson(br, "token");
                            if (StringUtils.isEmpty(token)) {
                                logger.warning("Failed to authorize");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                            /* Save token for to eventually re-use it later */
                            this.getPluginConfig().setProperty("api_token", token);
                        }
                    }
                    br.getHeaders().put("Authorization", "Bearer " + token);
                    br.getPage(API_BASE_NORMAL + "/mobile/collections/" + fid);
                    if (br.getHttpConnection().getResponseCode() == 401) {
                        /* 2019-09-30 {"error":"Invalid JWT token : Signature Verification Error"} */
                        token = null;
                        continue;
                    }
                    authed = true;
                    break;
                } finally {
                    counter++;
                }
            } while (counter <= 1);
            if (!authed) {
                logger.warning("Authorization error");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else if (br.getHttpConnection().getResponseCode() == 404) {
                /* 2019-09-30 e.g. {"success":false,"message":"This collection does not exist","error_key":"board_deleted"} */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            String filename = (String) entries.get("name");
            final String filesize = PluginJSonUtils.getJson(br, "size");
            if (StringUtils.isEmpty(filename)) {
                /* Fallback */
                filename = fid + ".zip";
                isSingleZIP = true;
            }
            if (!StringUtils.isEmpty(filesize) && filesize.matches("\\d+")) {
                link.setDownloadSize(Long.parseLong(filesize));
            }
            link.setFinalFileName(filename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        boolean resume = true;
        int maxChunks = -2;
        if (!isTypeDownload(link) && StringUtils.isEmpty(this.downloadurl)) {
            /* Key over which we can select the version of the file we want - usually the laters. Available in browser as key "latest" */
            final String operation_version = PluginJSonUtils.getJson(br, "operation_version");
            if (StringUtils.isEmpty(operation_version)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Use website from now on */
            br = prepBRWebsite(new Browser());
            br.getHeaders().put("x-requested-with", "XMLHttpRequest");
            final String fid = this.getFID(link);
            final String action = String.format("https://boards.wetransfer.com/api/boards/%s/%s/download", fid, operation_version);
            br.postPageRaw(action, "{}");
            this.downloadurl = PluginJSonUtils.getJson(br, "download_url");
        }
        if (isSingleZIP) {
            resume = false;
            maxChunks = 1;
        }
        if (StringUtils.isEmpty(this.downloadurl)) {
            logger.warning("Failed to find final downloadurl");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, downloadurl, resume, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>Error while downloading your file")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}