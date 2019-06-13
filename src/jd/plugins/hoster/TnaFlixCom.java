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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class TnaFlixCom extends PluginForHost {
    public TnaFlixCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    private static String[]      domains                         = new String[] { "tnaflix.com", "empflix.com" };
    private static final String  ALLOW_MULTIHOST_USAGE           = "ALLOW_MULTIHOST_USAGE";
    private static final boolean default_allow_multihoster_usage = false;
    private static final String  TYPE_NORMAL                     = "https?://[^/]+/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)";
    private static final String  TYPE_embed                      = "https?://[^/]*?player\\.[^/]+/video/(\\d+)";
    private static final String  TYPE_embedding_player           = ".+/embedding_player/embedding_feed\\.php\\?viewkey=([a-z0-9]+)";

    @Override
    public String getLinkID(final DownloadLink link) {
        String linkid = getViewkey(link.getPluginPatternMatcher());
        if (linkid == null) {
            linkid = getVideoID(link.getPluginPatternMatcher());
        }
        if (linkid != null) {
            return link.getHost() + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getViewkey(final String url) {
        return new Regex(url, "viewkey=([a-z0-9]+)").getMatch(0);
    }

    private String getVideoID(final String url) {
        String videoid = new Regex(url, "video(\\d+)$").getMatch(0);
        if (videoid == null) {
            videoid = new Regex(url, TYPE_embed).getMatch(0);
        }
        return videoid;
    }

    private void setConfigElements() {
        String user_text;
        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
            user_text = "Erlaube den Download von Links dieses Anbieters über Multihoster (nicht empfohlen)?\r\n<html><b>Kann die Anonymität erhöhen, aber auch die Fehleranfälligkeit!</b>\r\nAktualisiere deine(n) Multihoster Account(s) nach dem Aktivieren dieser Einstellung um diesen Hoster in der Liste der unterstützten Hoster deines/r Multihoster Accounts zu sehen (sofern diese/r ihn unterstützen).</html>";
        } else {
            user_text = "Allow links of this host to be downloaded via multihosters (not recommended)?\r\n<html><b>This might improve anonymity but perhaps also increase error susceptibility!</b>\r\nRefresh your multihoster account(s) after activating this setting to see this host in the list of the supported hosts of your multihost account(s) (in case this host is supported by your used multihost(s)).</html>";
        }
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MULTIHOST_USAGE, JDL.L("plugins.hoster." + this.getClass().getName() + ".ALLOW_MULTIHOST_USAGE", user_text)).setDefaultValue(default_allow_multihoster_usage));
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        if (this.getPluginConfig().getBooleanProperty(ALLOW_MULTIHOST_USAGE, default_allow_multihoster_usage)) {
            return true;
        } else {
            return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.tnaflix.com/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        final String urlpart = new Regex(link.getDownloadURL(), "https?://(?:www\\.)?(.+)").getMatch(0);
        link.setUrlDownload("https://www." + urlpart);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("https://" + link.getHost(), "content_filter2", "type%3Dstraight%26filter%3Dcams");
        br.setCookie("https://" + link.getHost(), "content_filter3", "type%3Dstraight%2Ctranny%2Cgay%26filter%3Dcams");
        if (link.getDownloadURL().matches(TYPE_embedding_player)) {
            /* Convert embed urls --> Original urls */
            link.setUrlDownload(link.getDownloadURL().replace("http://", "https://").replace("embedding_player/embedding_feed", "view_video"));
            link.setContentUrl(link.getDownloadURL());
        }
        String filename = null;
        final String videoid_type_2 = this.getVideoID(link.getPluginPatternMatcher());
        if (videoid_type_2 != null) {
            /* 2019-06-11: New: Ajax-linkcheck, old handling should not get used anymore! */
            br.getPage("https://dyn." + Browser.getHost(link.getPluginPatternMatcher(), false) + "/ajax/info.php?action=video&vid=" + videoid_type_2);
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            filename = (String) entries.get("title");
            // final boolean mp4download = ((Boolean) entries.get("mp4download")).booleanValue();
            // if (mp4download) {
            // }
        } else {
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("class=\"errorPage page404\"|> This video is set to private") || this.br.getHttpConnection().getResponseCode() == 404 || this.br.getURL().length() < 30) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                if (redirect.contains("errormsg=true")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (redirect.contains("video")) {
                    link.setUrlDownload(br.getRedirectLocation());
                }
                br.getPage(redirect);
            }
            filename = br.getRegex("<title>([^<>]*?) \\- TNAFlix Porn Videos</title>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<meta property=\"og:title\" content=\"([^<>]*?)\"").getMatch(0);
            }
        }
        if (StringUtils.isEmpty(filename)) {
            /* Fallback */
            filename = getLinkID(link) + ".mp4";
            link.setName(filename);
        } else {
            filename = Encoding.htmlDecode(filename).trim();
            filename = encodeUnicode(filename);
            link.setFinalFileName(filename + ".mp4");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        String vkey = this.getViewkey(link.getPluginPatternMatcher());
        final String videoid = this.getVideoID(link.getPluginPatternMatcher());
        String ajax_old_flv_downloadurl = null;
        if (videoid != null) {
            /*
             * 2019-06-11: Ajax handling - we need to find- and access the original URL else we will not be able to get all required
             * information.
             */
            if (vkey == null) {
                vkey = PluginJSonUtils.getJson(br, "vkey");
            }
            ajax_old_flv_downloadurl = PluginJSonUtils.getJsonValue(this.br, "flv");
            final String original_url = PluginJSonUtils.getJson(br, "link");
            if (original_url != null && videoid != null && original_url.contains(videoid)) {
                br.getPage(original_url);
            }
        }
        if (vkey == null) {
            vkey = this.br.getRegex("id=\"vkey\" type=\"hidden\" value=\"([A-Za-z0-9]+)\"").getMatch(0);
        }
        final String currenthost = br.getHost();
        final String nkey = this.br.getRegex("id=\"nkey\" type=\"hidden\" value=\"([^<>\"]+)\"").getMatch(0);
        /* This link doesn't have quality choice: https://www.tnaflix.com/view_video.php?viewkey=b5a6fcf68b48e6dd6734 */
        /* This may sometimes return 403 - avoid it if possible! */
        String dllink1 = br.getRegex("itemprop=\"contentUrl\" content=\"([^\"<>]+)\"").getMatch(0);
        /* This may sometimes return 403 - avoid it if possible! */
        String download = br.getRegex("<div class=\"playlist_listing\" data-loaded=\"true\">(.*?)</div>").getMatch(0);
        if (download == null) {
            /* This may sometimes return 403 - avoid it if possible! */
            download = br.getRegex("download href=\"((https?:)?//[^<>\"]+)\"").getMatch(0);
        }
        String configLink = br.getRegex("addVariable\\(\\'config\\', \\'(http.*?)\\'").getMatch(0);
        if (configLink == null) {
            configLink = br.getRegex("flashvars.config.*?escape\\(.*?(cdn.*?)\"").getMatch(0);
        }
        if (configLink == null && vkey != null && videoid != null && nkey != null) {
            /* 2019-06-13: Used for tnaflix AND empflix! */
            configLink = "https://cdn-fck.tnaflix.com/tnaflix/" + vkey + ".fid?key=" + nkey + "&VID=" + videoid + "&nomp4=1&catID=0&rollover=1&startThumb=30&embed=0&utm_source=0&multiview=0&premium=1&country=0user=0&vip=1&cd=0&ref=0&alpha";
        }
        if (configLink == null && download == null && dllink1 == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String dllink = null;
        if (configLink != null) {
            if (!configLink.startsWith("http")) {
                configLink = Encoding.htmlDecode("http://" + configLink);
            }
            br.getPage(configLink);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error: 'Config file is not correct'", 30 * 60 * 1000l);
            }
            final String[] qualities = { "720p", "480p", "360p", "240p", "144p" };
            for (final String quality : qualities) {
                dllink = br.getRegex("" + quality + "</res>\\s*<videolink>(http://.*?)</videoLink>").getMatch(0);
                if (dllink != null) {
                    break;
                }
            }
            if (dllink == null) {
                /* Fallback for videos with only one quality */
                dllink = this.br.getRegex("<videoLink>(?:<\\!\\[CDATA\\[)?(http[^<>\"]+)(?:\\]\\]>)?</videoLink>").getMatch(0);
            }
        } else if (download != null) {
            /* Official download */
            if (download.startsWith("http") || download.startsWith("//")) {
                dllink = "http:" + download;
            } else {
                final String[] qualities = { "720", "480", "360", "240", "144" };
                for (final String quality : qualities) {
                    dllink = new Regex(download, "href=(\"|')((?:https?:)?//.*?)\\1>Download in " + quality).getMatch(1);
                    if (dllink != null) {
                        break;
                    }
                }
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("<file>(http://.*?)</file>").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("<videolink>(http://.*?)</videoLink>").getMatch(0);
        }
        if (dllink == null) {
            /* 2019-06-11 */
            dllink = br.getRegex("<videoLinkDownload><\\!\\[CDATA\\[([^<>\"\\[\\]]+)\\]\\]></videoLinkDownload>").getMatch(0);
        }
        if (dllink == null && ajax_old_flv_downloadurl != null) {
            logger.info("Fallback to ajax method");
            dllink = ajax_old_flv_downloadurl;
            if (dllink != null && dllink.startsWith("//")) {
                dllink = "https:" + dllink;
            }
        }
        if (dllink == null) {
            /* This may sometimes return 403 - avoid it if possible! */
            dllink = dllink1;
        }
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.htmlDecode(dllink);
        Browser brc = br.cloneBrowser();
        brc.getHeaders().put("Range", "bytes=0-");
        final URLConnectionAdapter con = brc.openHeadConnection(dllink);
        final long fileSize = con.getCompleteContentLength();
        con.disconnect();
        link.setVerifiedFileSize(fileSize);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (dl.getConnection().getResponseCode() == 416) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 30 * 60 * 1000l);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            /* 403 error usually means we've tried to download an official downloadurl which may only be available for loggedin users! */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        dl.startDownload();
    }

    public static String[] getAnnotationNames() {
        return domains;
    }

    @Override
    public String[] siteSupportedNames() {
        return domains;
    }

    public static String[] getAnnotationUrls() {
        /*
         * 2019-06-12: Special: The owner of this host mograded from another script to XFS which is why we accept other URLs than only
         * default XFS.
         */
        final List<String> ret = new ArrayList<String>();
        for (int i = 0; i < domains.length; i++) {
            if (i == 0) {
                /* Match all URLs on first (=current) domain */
                ret.add("https?://(?:[a-z0-9]+\\.)?" + getHostsPatternPart() + "/(view_video\\.php\\?viewkey=[a-z0-9]+|video/\\d+|.*?video\\d+)|https?://(?:www\\.)?" + getHostsPatternPart() + "/embedding_player/embedding_feed\\.php\\?viewkey=[a-z0-9]+");
            } else {
                ret.add("");
            }
        }
        return ret.toArray(new String[0]);
    }

    /** Returns '(?:domain1|domain2)' */
    public static String getHostsPatternPart() {
        final StringBuilder pattern = new StringBuilder();
        pattern.append("(?:");
        for (final String name : domains) {
            pattern.append((pattern.length() > 0 ? "|" : "") + Pattern.quote(name));
        }
        pattern.append(")");
        return pattern.toString();
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