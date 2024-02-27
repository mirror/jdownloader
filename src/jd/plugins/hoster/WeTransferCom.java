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
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wetransfer.com" }, urls = { "https?://wetransferdecrypted/[a-f0-9]{46}/[a-f0-9]{4,12}/[a-f0-9]{46}|https?://(boards|collect)\\.wetransfer\\.com/board/[a-z0-9]+" })
public class WeTransferCom extends PluginForHost {
    public WeTransferCom(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://wetransfer.com/de-DE/explore/legal/terms";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public static Browser prepBRWebsite(final Browser br) {
        br.addAllowedResponseCodes(new int[] { 410, 503 });
        br.setCookie("wetransfer.com", "wt_tandc", "20240117%3A1");
        br.setCookie("wetransfer.com", "wt_lang", "en");
        return br;
    }

    public static Browser prepBRAPI(final Browser br) {
        br.getHeaders().put("User-Agent", "okhttp/3.12.0");
        br.setAllowedResponseCodes(new int[] { 401 });
        return br;
    }

    /* 2019-09-30: https://play.google.com/store/apps/details?id=com.wetransfer.app.live */
    public static final String   API_BASE_AUTH        = "https://api.wetransfermobile.com/v1";
    public static final String   API_BASE_NORMAL      = "https://api.wetransfermobile.com/v2";
    private static final Pattern TYPE_DOWNLOAD        = Pattern.compile("https?://wetransferdecrypted/([a-f0-9]{46})/([a-f0-9]{4,12})/([a-f0-9]{46})");
    public static final String   PROPERTY_DIRECT_LINK = "direct_link";
    public static final String   PROPERTY_SINGLE_ZIP  = "single_zip";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        prepBRWebsite(br);
        final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), TYPE_DOWNLOAD);
        final String folder_id = urlinfo.getMatch(0);
        final String security_hash = urlinfo.getMatch(1);
        final String file_id = urlinfo.getMatch(2);
        if (security_hash == null || folder_id == null || file_id == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "One or multiple plugin properties are missing");
        }
        final String refererurl = link.getReferrerUrl();
        if (refererurl == null) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Referer property is missing");
        }
        br.getPage(refererurl);
        final String[] recipient_id = refererurl.replaceFirst("https?://[^/]+/+", "").split("/");
        if (recipient_id == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String domain_user_id = br.getRegex("user\\s*:\\s*\\{\\s*\"key\"\\s*:\\s*\"(.*?)\"").getMatch(0);
        final String csrfToken = br.getRegex("name\\s*=\\s*\"csrf-token\"\\s*content\\s*=\\s*\"(.*?)\"").getMatch(0);
        final Map<String, Object> postdata = new HashMap<String, Object>();
        postdata.put("security_hash", security_hash);
        if (this.isSingleZip(link)) {
            postdata.put("intent", "entire_transfer");
        } else {
            postdata.put("intent", "single_file");
            postdata.put("file_ids", Arrays.asList(new String[] { file_id }));
        }
        if (recipient_id.length == 4) {
            postdata.put("recipient_id", recipient_id[2]);
        }
        if (domain_user_id != null) {
            postdata.put("domain_user_id", domain_user_id);
        }
        final PostRequest post = new PostRequest(br.getURL(("/api/v4/transfers/" + folder_id + "/download")));
        post.getHeaders().put("Accept", "application/json");
        post.getHeaders().put("Content-Type", "application/json");
        post.getHeaders().put("Origin", "https://" + br.getHost());
        post.getHeaders().put("X-Requested-With", " XMLHttpRequest");
        if (csrfToken != null) {
            post.getHeaders().put("X-CSRF-Token", csrfToken);
        }
        post.setPostDataString(JSonStorage.serializeToJson(postdata));
        br.getPage(post);
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        final String error = (String) entries.get("error");
        if (error != null) {
            if (error.equalsIgnoreCase("invalid_transfer")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_FATAL, error);
            }
        }
        link.setProperty(PROPERTY_DIRECT_LINK, entries.get("direct_link"));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECT_LINK);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Trying to re-use stored directurl: " + storedDirecturl);
        } else {
            requestFileInformation(link);
            dllink = link.getStringProperty(PROPERTY_DIRECT_LINK);
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find final downloadurl");
            }
        }
        boolean resume = true;
        int maxChunks = -2;
        final boolean isSingleZIP = this.isSingleZip(link);
        if (isSingleZIP) {
            resume = false;
            maxChunks = 1;
        }
        try {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume, maxChunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECT_LINK);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        dl.startDownload();
    }

    private boolean isSingleZip(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_SINGLE_ZIP, false);
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return WetransferConfig.class;
    }

    public static interface WetransferConfig extends PluginConfigInterface {
        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        public static class TRANSLATION {
            public String getCrawlMode_label() {
                return "Crawl mode";
            }
        }

        public static enum CrawlMode implements LabelInterface {
            ZIP {
                @Override
                public String getLabel() {
                    return "Add .zip only";
                }
            },
            FILES_FOLDERS {
                @Override
                public String getLabel() {
                    return "Add loose files & folders";
                }
            },
            ALL {
                @Override
                public String getLabel() {
                    return "Add loose files & folders AND .zip with all items";
                }
            };
        }

        @AboutConfig
        @DefaultEnumValue("ZIP")
        @Order(10)
        @DescriptionForConfigEntry("Single .zip download is recommended. Loose file download may lave you with corrupted files or wrongly named .zip files.")
        @DefaultOnNull
        CrawlMode getCrawlMode();

        void setCrawlMode(final CrawlMode mode);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}