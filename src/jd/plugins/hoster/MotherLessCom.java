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

import org.appwork.utils.StringUtils;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "motherless.com" }, urls = { "https?://(?:www\\.|members\\.)?(?:motherless\\.com/(?:movies|thumbs).*|(?:premium)?motherlesspictures(?:media)?\\.com/[a-zA-Z0-9/\\.]+|motherlessvideos\\.com/[a-zA-Z0-9/\\.]+)" })
public class MotherLessCom extends PluginForHost {
    public static final String html_subscribedFailed       = "Failed to subscribe to the owner of the video";
    public static final String html_contentSubscriberVideo = "Here's another video instead\\.";
    public static final String html_contentSubscriberImage = "Here's another image instead\\.";
    public static final String html_contentFriendsOnly     = ">\\s*The content you are trying to view is for friends only\\.\\s*<";
    // offline can contain text which is displayed in contentScriber pages
    public static final String html_OFFLINE                = "Violated Site Terms of Use|The page you're looking for cannot be found|You will be redirected to";
    public static final String html_notOnlineYet           = "(This video is being processed and will be available shortly|This video will be available in (less than a minute|[0-9]+ minutes))";
    public static final String ua                          = RandomUserAgent.generate();
    private final String       PROPERTY_DIRECTURL          = "PROPERTY_DIRECTURL";
    public static final String PROPERTY_TYPE               = "dltype";
    private String             dllink                      = null;

    @SuppressWarnings("deprecation")
    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
        this.enablePremium("https://motherless.com/register");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replace("premium", "").replaceAll("(motherlesspictures|motherlessvideos)", "motherless");
        link.setUrlDownload(theLink);
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (this.checkDirectLink(link) != null) {
            logger.info("Linkcheck done via directurl");
            return AvailableStatus.TRUE;
        }
        // reset comment/message
        if ("video".equals(link.getStringProperty(PROPERTY_TYPE))) {
            notOnlineYet(link, true, true);
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        String title = null;
        String betterName = null;
        if ("offline".equals(link.getStringProperty(PROPERTY_TYPE))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(link.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*The member has deleted the upload\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if ("video".equals(link.getStringProperty(PROPERTY_TYPE)) || jd.plugins.decrypter.MotherLessCom.isVideo(this.br)) {
            link.setMimeHint(CompiledFiletypeFilter.VideoExtensions.MP4);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(html_notOnlineYet)) {
                notOnlineYet(link, false, true);
                return AvailableStatus.FALSE;
            } else if (isWatchSubscriberPremiumOnly(br)) {
                // requires account!
                logger.info("The upload is subscriber only.");
                return AvailableStatus.TRUE;
            } else if (br.containsHTML(html_contentFriendsOnly)) {
                logger.info("The content you are trying to view is for friends only.");
                return AvailableStatus.UNCHECKABLE;
            } else if (br.containsHTML(html_OFFLINE) || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404")) {
                // should be last
                // links can go offline between the time of adding && download, also decrypter doesn't check found content, will happen
                // here..
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            title = getUploadTitle();
            getVideoLink();
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            betterName = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            if (betterName != null) {
                String ext = new Regex(dllink, "\\.(flv|mp4)").getMatch(-1);
                if (ext != null) {
                    betterName += ext;
                }
            }
        } else if ("image".equals(link.getStringProperty(PROPERTY_TYPE))) {
            link.setMimeHint(CompiledFiletypeFilter.ImageExtensions.JPG);
            // links can go offline between the time of adding && download, also decrypter doesn't check found content, will happen here..
            if (br.containsHTML(html_OFFLINE) || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(html_contentFriendsOnly)) {
                logger.info("The content you are trying to view is for friends only.");
                return AvailableStatus.UNCHECKABLE;
            }
            title = getUploadTitle();
            getPictureLink();
            logger.info("dllink: " + dllink);
            if (dllink == null) {
                dllink = br.getRegex("fileurl = \'(http://.*?)\'").getMatch(0);
            }
            // No link there but link to the full picture -> Offline
            if (dllink == null && br.containsHTML("<div id=\"media-media\">[\t\n\r ]+<div>[\t\n\r ]+<a href=\"/[A-Z0-9]+\\?full\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (dllink == null) {
            dllink = link.getDownloadURL();
        }
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept-Encoding", "identity");
            con = brc.openHeadConnection(this.dllink);
            if (!this.looksLikeDownloadableContent(con)) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String name = getFileNameFromHeader(con);
            if (betterName == null) {
                betterName = new Regex(name, "/([^/].*?\\.(flv|mp4))").getMatch(0);
            }
            if (betterName != null) {
                name = betterName;
            }
            if (title != null) {
                /*
                 * Check if we found the site-title (upload-name). NEVER EVER set the upload name only as a lot of them are just the same
                 * --> Mirror handling will go insane!
                 */
                title = Encoding.htmlDecode(title).trim();
                name = title + "_" + name;
            }
            link.setName(name);
            if (con.getCompleteContentLength() > 0) {
                link.setVerifiedFileSize(con.getCompleteContentLength());
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        return AvailableStatus.TRUE;
    }

    public static boolean isDownloadPremiumOnly(final Browser br) {
        return br.containsHTML("This link is only downloadable for registered users\\.");
    }

    public static boolean isWatchSubscriberPremiumOnly(final Browser br) {
        return br.containsHTML("The upload is subscriber only\\. You can subscribe to the member from their");
    }

    private String getUploadTitle() {
        return br.getRegex("class=\"media-meta-title\">\\s*<h1>([^<>\"]*?)(?:\\.mp4)?\\s*<").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link) throws Exception {
        final int maxchunks = 1;
        if (!attemptStoredDownloadurlDownload(link, maxchunks)) {
            if (!link.getDownloadURL().contains("/img/") && !link.getDownloadURL().contains("/dev")) {
                requestFileInformation(link);
            } else {
                // Access the page first to make the finallink valid
                String fileid = new Regex(link.getDownloadURL(), "/img/([A-Z0-9]+)").getMatch(0);
                if (fileid != null) {
                    br.getPage("http://motherless.com/" + fileid);
                } else {
                    br.getPage(link.getPluginPatternMatcher());
                }
            }
            if (br.containsHTML(html_contentFriendsOnly)) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Content is for friends only!");
            }
            br.getHeaders().put("Accept-Encoding", "identity");
            if ("video".equals(link.getStringProperty(PROPERTY_TYPE))) {
                br.getHeaders().put("Referer", "http://motherless.com/scripts/jwplayer.flash.swf");
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
            } else if ("image".equals(link.getStringProperty(PROPERTY_TYPE))) {
                dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
            } else {
                logger.warning("Unknown case for link: " + link.getDownloadURL());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                final String server_filename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
                final String final_filename;
                if (server_filename != null && plugin_filename.length() > server_filename.length()) {
                    /* Actually it should always use the plugin_filename. */
                    final_filename = plugin_filename;
                } else {
                    final_filename = server_filename;
                }
                link.setFinalFileName(final_filename);
            }
        }
        dl.startDownload();
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

    private void getPictureLink() {
        dllink = br.getRegex("\"(https?://members\\.motherless\\.com/img/.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("full_sized\\.jpg\" (.*?)\"(https?://s\\d+\\.motherless\\.com/dev\\d+/\\d+/\\d+/\\d+/\\d+.*?)\"").getMatch(1);
            if (dllink == null) {
                dllink = br.getRegex("<div style=\"clear: left;\"></div>[\t\r\n ]+<img src=\"(https?://.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\\?full\">[\n\t\r ]+<img src=\"(?!https?://motherless\\.com/images/full_sized\\.jpg)(http://.*?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("\"(https?://[\\w\\-\\.]*images\\.motherlessmedia\\.com/images/[a-zA-Z0-9]+\\..{3,4}(?:\\?fs=opencloud)?)\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("\"(https?://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\..{3,4})\"").getMatch(0);
                        }
                    }
                }
            }
        }
    }

    private void getVideoLink() {
        dllink = br.getRegex("addVariable\\(\\'file\\', \\'(https?://.*?\\.(flv|mp4))\\'\\)").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(https?://[^/]+\\.motherlessmedia\\.com/[^<>\"]*?-720p\\.(flv|mp4))\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https?://[^/]+\\.motherlessmedia\\.com/[^<>\"]*?\\.(flv|mp4))\"").getMatch(0);
            }
        }
        if (dllink == null) {
            dllink = PluginJSonUtils.getJsonValue(br, "file");
        }
        if (dllink == null) {
            dllink = br.getRegex("__fileurl\\s*=\\s*'(https?://[^<>\"\\']+)").getMatch(0);
        }
        if (dllink != null && !dllink.contains("?start=0")) {
            // dllink += "?start=0";
        }
    }

    public void handleFree(final DownloadLink link) throws Exception {
        if (link.hasProperty("onlyregistered")) {
            throw new AccountRequiredException();
        }
        doFree(link);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Subscribers Only")) {
            String profileToSubscribe = br.getRegex("You can subscribe to the member from\\s*their <a href=(\"|')(https?://motherless\\.com)?/m/(.*?)\\1").getMatch(2);
            if (profileToSubscribe == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.getPage("/subscribe/" + profileToSubscribe);
            String token = br.getRegex("name=\"_token\" value=\"(.*?)\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.postPage("/subscribe/" + profileToSubscribe, "_token=" + token);
            if (!br.containsHTML("(>You are already subscribed to|>You are now subscribed to)")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.getPage(link.getDownloadURL());
        }
        doFree(link);
    }

    public static DownloadLink notOnlineYet(final DownloadLink link, final boolean reset, final boolean hostPlugin) {
        String msg = null;
        if (!reset) {
            msg = "Not online yet... check again later";
        }
        if (hostPlugin) {
            link.getLinkStatus().setStatusText(msg);
        }
        link.setComment(msg);
        return link;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}