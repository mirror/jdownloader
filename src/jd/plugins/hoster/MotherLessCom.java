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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "motherless.com" }, urls = { "http://(www\\.)?(members\\.)?(motherless\\.com/(movies|thumbs).*|(premium)?motherlesspictures(media)?\\.com/[a-zA-Z0-9/\\.]+|motherlessvideos\\.com/[a-zA-Z0-9/\\.]+)" }, flags = { 2 })
public class MotherLessCom extends PluginForHost {

    public static final String subscribedFailed       = "Failed to subscribe to the owner of the video";
    public static final String contentRegistered      = "This link is only downloadable for registered users.";
    public static final String contentSubscriberOnly  = "The upload is subscriber only. You can subscribe to the member from their";
    public static final String contentSubscriberVideo = "Here's another video instead\\.";
    public static final String contentSubscriberImage = "Here's another image instead\\.";
    public static final String contentFriendsOnly     = ">\\s*The content you are trying to view is for friends only\\.\\s*<";
    // offline can contain text which is displayed in contentScriber pages
    public static final String OFFLINE                = "Violated Site Terms of Use|The page you're looking for cannot be found|You will be redirected to";
    public static final String notOnlineYet           = "(This video is being processed and will be available shortly|This video will be available in (less than a minute|[0-9]+ minutes))";
    public static final String ua                     = RandomUserAgent.generate();
    private String             DLLINK                 = null;

    public MotherLessCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2500l);
        this.enablePremium("http://motherless.com/register");
    }

    public void correctDownloadLink(DownloadLink link) {
        String theLink = link.getDownloadURL();
        theLink = theLink.replace("premium", "").replaceAll("(motherlesspictures|motherlessvideos)", "motherless");
        link.setUrlDownload(theLink);
    }

    public void doFree(DownloadLink link) throws Exception {
        if (!link.getDownloadURL().contains("/img/") && !link.getDownloadURL().contains("/dev")) {
            requestFileInformation(link);
        } else {
            // Access the page first to make the finallink valid
            String fileid = new Regex(link.getDownloadURL(), "/img/([A-Z0-9]+)").getMatch(0);
            if (fileid != null) {
                br.getPage("http://motherless.com/" + fileid);
            } else {
                br.getPage(link.getBrowserUrl());
            }
        }
        if (br.containsHTML(contentFriendsOnly)) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Content is for friends only!");
        }
        br.getHeaders().put("Accept-Encoding", "identity");
        if ("video".equals(link.getStringProperty("dltype"))) {
            br.getHeaders().put("Referer", "http://motherless.com/scripts/jwplayer.flash.swf");
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        } else if ("image".equals(link.getStringProperty("dltype"))) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, false, 1);
        } else {
            logger.warning("Unnknown case for link: " + link.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getFinalFileName() == null) {
            link.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
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
        DLLINK = br.getRegex("\"(http://members\\.motherless\\.com/img/.*?)\"").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("full_sized\\.jpg\" (.*?)\"(http://s\\d+\\.motherless\\.com/dev\\d+/\\d+/\\d+/\\d+/\\d+.*?)\"").getMatch(1);
            if (DLLINK == null) {
                DLLINK = br.getRegex("<div style=\"clear: left;\"></div>[\t\r\n ]+<img src=\"(http://.*?)\"").getMatch(0);
                if (DLLINK == null) {
                    DLLINK = br.getRegex("\\?full\">[\n\t\r ]+<img src=\"(?!http://motherless\\.com/images/full_sized\\.jpg)(http://.*?)\"").getMatch(0);
                    if (DLLINK == null) {
                        DLLINK = br.getRegex("\"(http://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\..{3,4})\"").getMatch(0);
                    }
                }
            }
        }
    }

    private void getVideoLink() {
        DLLINK = br.getRegex("addVariable\\(\\'file\\', \\'(http://.*?\\.(flv|mp4))\\'\\)").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("(http://s\\d+\\.motherlessmedia\\.com/dev[0-9]+/[^<>\"]*?\\.(flv|mp4))\"").getMatch(0);
        }
        if (DLLINK != null && !DLLINK.contains("?start=0")) {
            // DLLINK += "?start=0";
        }
    }

    public void handleFree(DownloadLink link) throws Exception {
        if (link.getStringProperty("onlyregistered", null) != null) {
            logger.info(contentRegistered);
            throw new PluginException(LinkStatus.ERROR_FATAL, contentRegistered);
        }
        doFree(link);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Subscribers Only")) {
            String profileToSubscribe = br.getRegex("You can subscribe to the member from[\t\n\r ]+their <a href=(\"|')(http://motherless\\.com)?/m/(.*?)\\1").getMatch(2);
            if (profileToSubscribe == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, subscribedFailed);
            }
            br.getPage("http://motherless.com/subscribe/" + profileToSubscribe);
            String token = br.getRegex("name=\"_token\" value=\"(.*?)\"").getMatch(0);
            if (token == null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, subscribedFailed);
            }
            br.postPage("http://motherless.com/subscribe/" + profileToSubscribe, "_token=" + token);
            if (!br.containsHTML("(>You are already subscribed to|>You are now subscribed to)")) {
                throw new PluginException(LinkStatus.ERROR_FATAL, subscribedFailed);
            }
            br.getPage(link.getDownloadURL());
        }
        doFree(link);
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.postPage("https://motherless.com/login", "remember_me=1&__remember_me=0&botcheck=no+bots%21&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://motherless.com/", "auth") == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        // reset comment/message
        if ("video".equals(parameter.getStringProperty("dltype", null))) {
            notOnlineYet(parameter, true);
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        String betterName = null;
        if ("offline".equals(parameter.getStringProperty("dltype", null))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if ("video".equals(parameter.getStringProperty("dltype", null))) {
            br.getPage(parameter.getDownloadURL());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (br.containsHTML(notOnlineYet)) {
                notOnlineYet(parameter, false);
                return AvailableStatus.FALSE;
            } else if (br.containsHTML(jd.plugins.hoster.MotherLessCom.contentSubscriberOnly)) {
                // requires account!
                return AvailableStatus.UNCHECKABLE;
            } else if (br.containsHTML(contentFriendsOnly)) {
                return AvailableStatus.UNCHECKABLE;
            } else if (br.containsHTML(OFFLINE) || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404")) {
                // should be last
                // links can go offline between the time of adding && download, also decrypter doesn't check found content, will happen
                // here..
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            getVideoLink();
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            betterName = new Regex(parameter.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            if (betterName != null) {
                String ext = new Regex(DLLINK, "\\.(flv|mp4)").getMatch(-1);
                if (ext != null) {
                    betterName += ext;
                }
            }
        } else if ("image".equals(parameter.getStringProperty("dltype", null))) {
            br.getPage(parameter.getDownloadURL());
            // links can go offline between the time of adding && download, also decrypter doesn't check found content, will happen here..
            if (br.containsHTML(OFFLINE) || br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("<img src=\"/images/icons.*/exclamation\\.png\" style=\"margin-top: -5px;\" />[\t\n\r ]+404")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML(contentFriendsOnly)) {
                return AvailableStatus.UNCHECKABLE;
            }
            getPictureLink();
            // No link there but link to the full picture -> Offline
            if (DLLINK == null && br.containsHTML("<div id=\"media-media\">[\t\n\r ]+<div>[\t\n\r ]+<a href=\"/[A-Z0-9]+\\?full\"")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        if (DLLINK == null) {
            DLLINK = parameter.getDownloadURL();
        }
        URLConnectionAdapter con = null;
        try {
            Browser brc = br.cloneBrowser();
            brc.getHeaders().put("Accept-Encoding", "identity");
            con = brc.openGetConnection(DLLINK);
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
            parameter.setName(betterName);
            parameter.setDownloadSize(con.getLongContentLength());
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public static DownloadLink notOnlineYet(DownloadLink downloadLink, boolean reset) {
        String msg = null;
        if (!reset) {
            msg = "Not online yet... check again later";
        }
        downloadLink.getLinkStatus().setStatusText(msg);
        try {
            downloadLink.setComment(msg);
        } catch (Throwable e) {
        }
        return downloadLink;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}