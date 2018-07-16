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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.RandomUserAgent;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "motherless.com" }, urls = { "https?://(?:www\\.)?(?:members\\.)?(?:motherless\\.com/(?:movies|thumbs).*|(?:premium)?motherlesspictures(?:media)?\\.com/[a-zA-Z0-9/\\.]+|motherlessvideos\\.com/[a-zA-Z0-9/\\.]+)" })
public class MotherLessCom extends PluginForHost {
    public static final String html_subscribedFailed       = "Failed to subscribe to the owner of the video";
    public static final String html_contentRegistered      = "This link is only downloadable for registered users.";
    public static final String html_contentSubscriberOnly  = "The upload is subscriber only. You can subscribe to the member from their";
    public static final String html_contentSubscriberVideo = "Here's another video instead\\.";
    public static final String html_contentSubscriberImage = "Here's another image instead\\.";
    public static final String html_contentFriendsOnly     = ">\\s*The content you are trying to view is for friends only\\.\\s*<";
    // offline can contain text which is displayed in contentScriber pages
    public static final String html_OFFLINE                = "Violated Site Terms of Use|The page you're looking for cannot be found|You will be redirected to";
    public static final String html_notOnlineYet           = "(This video is being processed and will be available shortly|This video will be available in (less than a minute|[0-9]+ minutes))";
    public static final String ua                          = RandomUserAgent.generate();
    private String             dllink                      = null;

    @SuppressWarnings("deprecation")
    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
        this.enablePremium("http://motherless.com/register");
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replace("premium", "").replaceAll("(motherlesspictures|motherlessvideos)", "motherless");
        link.setUrlDownload(theLink);
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        // reset comment/message
        if ("video".equals(parameter.getStringProperty("dltype", null))) {
            notOnlineYet(parameter, true, true);
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        String title = null;
        String betterName = null;
        if ("offline".equals(parameter.getStringProperty("dltype", null))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML(">The member has deleted the upload<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if ("video".equals(parameter.getStringProperty("dltype", null))) {
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(html_notOnlineYet)) {
                notOnlineYet(parameter, false, true);
                return AvailableStatus.FALSE;
            } else if (br.containsHTML(jd.plugins.hoster.MotherLessCom.html_contentSubscriberOnly)) {
                // requires account!
                logger.info("The upload is subscriber only.");
                return AvailableStatus.UNCHECKABLE;
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
            betterName = new Regex(parameter.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            if (betterName != null) {
                String ext = new Regex(dllink, "\\.(flv|mp4)").getMatch(-1);
                if (ext != null) {
                    betterName += ext;
                }
            }
        } else if ("image".equals(parameter.getStringProperty("dltype", null))) {
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
            dllink = parameter.getDownloadURL();
        }
        URLConnectionAdapter con = null;
        try {
            final Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept-Encoding", "identity");
            con = openConnection(brc, dllink);
            if (con.getContentType().contains("html")) {
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
            parameter.setName(name);
            parameter.setDownloadSize(con.getLongContentLength());
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    private String getUploadTitle() {
        return br.getRegex("id=\"view\\-upload\\-title\">([^<>\"]*?)</h1>").getMatch(0);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link) throws Exception {
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
        if ("video".equals(link.getStringProperty("dltype"))) {
            br.getHeaders().put("Referer", "http://motherless.com/scripts/jwplayer.flash.swf");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 1);
        } else if ("image".equals(link.getStringProperty("dltype"))) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        } else {
            logger.warning("Unnknown case for link: " + link.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getFinalFileName() == null) {
            final String plugin_filename = link.getName();
            final String server_filename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
            final String final_filename;
            if (server_filename == null || server_filename.length() < plugin_filename.length()) {
                /* Actually it should always use the plugin_filename. */
                final_filename = plugin_filename;
            } else {
                final_filename = server_filename;
            }
            link.setFinalFileName(final_filename);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        account.setValid(true);
        ai.setStatus("Registered (free) User");
        return ai;
    }

    private void login(final Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", ua);
        br.postPage("https://motherless.com/login", "remember_me=1&__remember_me=0&botcheck=no+bots%21&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://motherless.com/", "_auth") == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public String getAGBLink() {
        return "http://motherless.com/tou";
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
            dllink = br.getRegex("(https?://s\\d+\\.motherlessmedia\\.com/dev[0-9]+/[^<>\"]*?\\.(flv|mp4))\"").getMatch(0);
            if (dllink == null) {
                dllink = PluginJSonUtils.getJsonValue(br, "file");
            }
        }
        if (dllink != null && !dllink.contains("?start=0")) {
            // dllink += "?start=0";
        }
    }

    public void handleFree(final DownloadLink link) throws Exception {
        if (link.getStringProperty("onlyregistered", null) != null) {
            logger.info(html_contentRegistered);
            throw new PluginException(LinkStatus.ERROR_FATAL, html_contentRegistered);
        }
        doFree(link);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Subscribers Only")) {
            String profileToSubscribe = br.getRegex("You can subscribe to the member from[\t\n\r ]+their <a href=(\"|')(https?://motherless\\.com)?/m/(.*?)\\1").getMatch(2);
            if (profileToSubscribe == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.getPage("https://motherless.com/subscribe/" + profileToSubscribe);
            String token = br.getRegex("name=\"_token\" value=\"(.*?)\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.postPage("https://motherless.com/subscribe/" + profileToSubscribe, "_token=" + token);
            if (!br.containsHTML("(>You are already subscribed to|>You are now subscribed to)")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, html_subscribedFailed);
            }
            br.getPage(link.getDownloadURL());
        }
        doFree(link);
    }

    private URLConnectionAdapter openConnection(final Browser br, final String directlink) throws IOException {
        URLConnectionAdapter con;
        if (true) {
            con = br.openGetConnection(directlink);
        } else {
            con = br.openHeadConnection(directlink);
        }
        return con;
    }

    public static DownloadLink notOnlineYet(final DownloadLink downloadLink, final boolean reset, final boolean hostPlugin) {
        String msg = null;
        if (!reset) {
            msg = "Not online yet... check again later";
        }
        if (hostPlugin) {
            downloadLink.getLinkStatus().setStatusText(msg);
        }
        downloadLink.setComment(msg);
        return downloadLink;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}