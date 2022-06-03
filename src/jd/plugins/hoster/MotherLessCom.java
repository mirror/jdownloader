//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.decrypter.MotherLessComCrawler;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { MotherLessComCrawler.class })
public class MotherLessCom extends PluginForHost {
    public static final String  html_subscribedFailed       = "Failed to subscribe to the owner of the video";
    public static final String  html_contentSubscriberVideo = "Here's another video instead\\.";
    public static final String  html_contentSubscriberImage = "Here's another image instead\\.";
    public static final String  html_contentFriendsOnly     = ">\\s*The content you are trying to view is for friends only\\.\\s*<";
    // offline can contain text which is displayed in contentScriber pages
    public static final String  html_notOnlineYet           = "(This video is being processed and will be available shortly|This video will be available in (less than a minute|[0-9]+ minutes))";
    public static final String  ua                          = RandomUserAgent.generate();
    private final static String PROPERTY_DIRECTURL          = "PROPERTY_DIRECTURL";
    public static final String  PROPERTY_TYPE               = "dltype";
    public static final String  PROPERTY_ONLYREGISTERED     = "onlyregistered";
    public static String        PROPERTY_TITLE              = "title";

    @SuppressWarnings("deprecation")
    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
        this.enablePremium("https://motherless.com/register");
    }

    public static List<String[]> getPluginDomains() {
        return MotherLessComCrawler.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Z0-9]+)$");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getMirrorID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getMirrorID(link);
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return getFID(link.getPluginPatternMatcher());
    }

    private static String getFID(final String url) {
        return new Regex(url, "https?://[^/]+/(.+)").getMatch(0);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setWeakFilename(link);
        if (this.checkDirectLink(link) != null) {
            logger.info("Linkcheck done via directurl");
            return AvailableStatus.TRUE;
        }
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        if ("offline".equals(link.getStringProperty(PROPERTY_TYPE))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*The member has deleted the upload\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final AvailableStatus status = parseFileInfoAndSetFilename(link);
        if (status == AvailableStatus.FALSE) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser brc = br.cloneBrowser();
                brc.getHeaders().put("Accept-Encoding", "identity");
                con = brc.openHeadConnection(dllink);
                if (!this.looksLikeDownloadableContent(con)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
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

    public void setWeakFilename(final DownloadLink link) {
        if (!link.isNameSet()) {
            /* Set fallback name */
            final String ext = getAssumedFileExtension(link);
            if (ext != null) {
                link.setName(this.getFID(link) + ext);
            } else {
                link.setName(this.getFID(link));
            }
        }
    }

    public static String getAssumedFileExtension(final DownloadLink link) {
        if (StringUtils.equals(link.getStringProperty(PROPERTY_TYPE), "video")) {
            return ".mp4";
        } else if (StringUtils.equals(link.getStringProperty(PROPERTY_TYPE), "image")) {
            return ".jpg";
        } else {
            return null;
        }
    }

    public AvailableStatus parseFileInfoAndSetFilename(final DownloadLink link) {
        setWeakFilename(link);
        String dllink = null;
        if ("video".equals(link.getStringProperty(PROPERTY_TYPE)) || MotherLessComCrawler.isVideo(this.br)) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                return AvailableStatus.FALSE;
            }
            if (br.containsHTML(html_notOnlineYet)) {
                return AvailableStatus.UNCHECKABLE;
            } else if (isWatchSubscriberPremiumOnly(br)) {
                // requires account!
                logger.info("The upload is subscriber only.");
                return AvailableStatus.TRUE;
            } else if (br.containsHTML(html_contentFriendsOnly)) {
                logger.info("The content you are trying to view is for friends only.");
                return AvailableStatus.UNCHECKABLE;
            } else if (isOffline(br)) {
                // should be last
                // links can go offline between the time of adding && download, also decrypter doesn't check found content, will happen
                // here..
                return AvailableStatus.FALSE;
            }
            dllink = getVideoLink(br);
        } else if ("image".equals(link.getStringProperty(PROPERTY_TYPE))) {
            // links can go offline between the time of adding && download, also decrypter doesn't check found content, will happen here..
            if (isOffline(br)) {
                return AvailableStatus.FALSE;
            } else if (br.containsHTML(html_contentFriendsOnly)) {
                logger.info("The content you are trying to view is for friends only.");
                return AvailableStatus.UNCHECKABLE;
            }
            dllink = getPictureLink(br);
            logger.info("dllink: " + dllink);
            if (dllink == null) {
                dllink = br.getRegex("fileurl = \'(http://.*?)\'").getMatch(0);
            }
            // No link there but link to the full picture -> Offline
            if (dllink == null && br.containsHTML("<div id=\"media-media\">\\s*<div>[\t\n\r ]+<a href=\"/[A-Z0-9]+\\?full\"")) {
                return AvailableStatus.FALSE;
            }
        } else {
            /* Old/deprecated/directurls */
            dllink = link.getPluginPatternMatcher();
        }
        link.setProperty(PROPERTY_DIRECTURL, dllink);
        final String title = br.getRegex("class=\"media-meta-title\">\\s*<h1>([^<]+)</h1>").getMatch(0);
        if (title != null) {
            link.setProperty(PROPERTY_TITLE, Encoding.htmlDecode(title).trim());
        }
        setFilename(link);
        return AvailableStatus.TRUE;
    }

    public static boolean isOffline(final Browser br) {
        if (br.getHttpConnection().getResponseCode() == 404) {
            return true;
        } else if (br.containsHTML("(?i)Violated Site Terms of Use|The page you're looking for cannot be found|You will be redirected to")) {
            return true;
        } else if (br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404")) {
            return true;
        } else {
            return false;
        }
    }

    public static void setFilename(final DownloadLink link) {
        final String fid = getFID(link.getPluginPatternMatcher());
        final String title = link.getStringProperty(PROPERTY_TITLE);
        String ext = Plugin.getFileNameExtensionFromString(link.getStringProperty(PROPERTY_DIRECTURL));
        if (ext == null) {
            ext = getAssumedFileExtension(link);
        }
        if (title != null && !title.equalsIgnoreCase(fid)) {
            link.setFinalFileName(Encoding.htmlDecode(title).trim() + "_ " + fid + ext);
        } else {
            link.setFinalFileName(fid + ext);
        }
    }

    public static boolean isDownloadPremiumOnly(final Browser br) {
        return br.containsHTML("This link is only downloadable for registered users\\.");
    }

    public static boolean isWatchSubscriberPremiumOnly(final Browser br) {
        return br.containsHTML("The upload is subscriber only\\. You can subscribe to the member from their");
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        final int maxchunks = 1;
        if (!attemptStoredDownloadurlDownload(link, maxchunks)) {
            requestFileInformation(link);
            if (br.containsHTML(html_contentFriendsOnly)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Content is for friends only!");
            }
            final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("Accept-Encoding", "identity");
            if ("video".equals(link.getStringProperty(PROPERTY_TYPE))) {
                br.getHeaders().put("Referer", "http://motherless.com/scripts/jwplayer.flash.swf");
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, isResumeable(link, account), maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken file(?)");
            }
            if (link.getFinalFileName() == null) {
                final String plugin_filename = link.getName();
                String server_filename = getFileNameFromHeader(dl.getConnection());
                if (server_filename != null) {
                    server_filename = Encoding.htmlDecode(server_filename).trim();
                }
                if (server_filename != null && plugin_filename.length() > server_filename.length()) {
                    /* Actually it should always use the plugin_filename. */
                    link.setFinalFileName(plugin_filename);
                } else {
                    link.setFinalFileName(server_filename);
                }
            }
        }
        dl.startDownload();
    }

    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (link != null) {
            if ("video".equals(link.getStringProperty(PROPERTY_TYPE))) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            prepHeadersDownload(link, brc);
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, allowResume(link), 1);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private String checkDirectLink(final DownloadLink link) {
        String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                prepHeadersDownload(link, br2);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private void prepHeadersDownload(final DownloadLink link, final Browser brc) {
        if ("video".equals(link.getStringProperty(PROPERTY_TYPE))) {
            brc.getHeaders().put("Referer", "http://motherless.com/scripts/jwplayer.flash.swf");
        }
    }

    private boolean allowResume(final DownloadLink link) {
        return !"image".equals(link.getStringProperty(PROPERTY_TYPE));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        login(account, true);
        account.setType(AccountType.FREE);
        return ai;
    }

    private void login(final Account account, final boolean validateCookies) throws Exception {
        /* TODO: Add cookie handling -> Re-load cookies and check validity */
        br.setFollowRedirects(true);
        /* 2021-08-26: Don#t use random User-Agent anymore when logging in. */
        // br.getHeaders().put("User-Agent", ua);
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                if (!validateCookies) {
                    logger.info("Trust cookies without checking");
                    return;
                } else {
                    /* TODO */
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedIN(br)) {
                        logger.info("Successfully loggedin via cookies");
                        /* Update cookies */
                        account.saveCookies(this.br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/login");
            final Form loginform = br.getFormbyProperty("id", "motherless-login-modal-form");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            if (loginform.hasInputFieldByName("botcheck")) {
                loginform.put("botcheck", "no+bots%21");
            }
            loginform.put("remember_me", "1");
            loginform.put("__remember_me", "0");
            br.submitForm(loginform);
            // br.postPage("https://" + this.getHost() + "/login", "remember_me=1&__remember_me=0&botcheck=no+bots%21&username=" +
            // Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (!isLoggedIN(this.br)) {
                account.clearCookies("");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.saveCookies(this.br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedIN(final Browser br) {
        return br.containsHTML("") && br.getCookie("http://motherless.com/", "_auth", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public String getAGBLink() {
        return "https://motherless.com/tou";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    private String getPictureLink(final Browser br) {
        String directlink = br.getRegex("\"(https?://members\\.motherless\\.com/img/.*?)\"").getMatch(0);
        if (directlink == null) {
            directlink = br.getRegex("full_sized\\.jpg\" (.*?)\"(https?://s\\d+\\.motherless\\.com/dev\\d+/\\d+/\\d+/\\d+/\\d+.*?)\"").getMatch(1);
            if (directlink == null) {
                directlink = br.getRegex("<div style=\"clear: left;\"></div>[\t\r\n ]+<img src=\"(https?://.*?)\"").getMatch(0);
                if (directlink == null) {
                    directlink = br.getRegex("\\?full\">[\n\t\r ]+<img src=\"(?!https?://motherless\\.com/images/full_sized\\.jpg)(http://.*?)\"").getMatch(0);
                    if (directlink == null) {
                        directlink = br.getRegex("\"(https?://[\\w\\-\\.]*images\\.motherlessmedia\\.com/images/[a-zA-Z0-9]+\\..{3,4}(?:\\?fs=opencloud)?)\"").getMatch(0);
                        if (directlink == null) {
                            directlink = br.getRegex("\"(https?://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\..{3,4})\"").getMatch(0);
                        }
                    }
                }
            }
        }
        return directlink;
    }

    private String getVideoLink(final Browser br) {
        String directlink = br.getRegex("addVariable\\(\\'file\\', \\'(https?://.*?\\.(flv|mp4))\\'\\)").getMatch(0);
        if (directlink == null) {
            directlink = br.getRegex("(https?://[^/]+\\.motherlessmedia\\.com/[^<>\"]*?-720p\\.(flv|mp4))\"").getMatch(0);
            if (directlink == null) {
                directlink = br.getRegex("(https?://[^/]+\\.motherlessmedia\\.com/[^<>\"]*?\\.(flv|mp4))\"").getMatch(0);
            }
        }
        if (directlink == null) {
            directlink = PluginJSonUtils.getJsonValue(br, "file");
        }
        if (directlink == null) {
            directlink = br.getRegex("__fileurl\\s*=\\s*'(https?://[^<>\"\\']+)").getMatch(0);
        }
        if (directlink != null && !directlink.contains("?start=0")) {
            // dllink += "?start=0";
        }
        return directlink;
    }

    public void handleFree(final DownloadLink link) throws Exception {
        if (link.hasProperty("onlyregistered")) {
            throw new AccountRequiredException();
        }
        handleDownload(link, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        if (br.containsHTML("(?i)Subscribers Only")) {
            logger.info("Content is subscriber only --> Auto-subscribing to content-owner");
            String profileToSubscribe = br.getRegex("You can subscribe to the member from\\s*their <a href=(\"|')(https?://motherless\\.com)?/m/(.*?)\\1").getMatch(2);
            if (profileToSubscribe == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.getPage("/subscribe/" + profileToSubscribe);
            final String token = br.getRegex("name=\"_token\" value=\"(.*?)\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.postPage("/subscribe/" + profileToSubscribe, "_token=" + token);
            if (!br.containsHTML("(?i)(>\\s*You are already subscribed to|>\\s*You are now subscribed to)")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.getPage(link.getPluginPatternMatcher());
        }
        handleDownload(link, account);
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}