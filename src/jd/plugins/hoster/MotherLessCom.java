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

    private static final String SUBSCRIBEFAILED     = "Failed to subscribe to the owner of the video";
    private static final String ONLY4REGISTEREDTEXT = "This link is only downloadable for registered users.";
    private String              DLLINK              = null;
    public final static String  ua                  = RandomUserAgent.generate();

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
        if ("video".equals(link.getStringProperty("dltype"))) {
            getVideoLink(link);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        } else if ("image".equals(link.getStringProperty("dltype"))) {
            getPictureLink(link);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, false, 1);
        } else {
            logger.warning("Unnknown case for link: " + link.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (link.getFinalFileName() == null) link.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
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

    private void getPictureLink(DownloadLink parameter) throws IOException, PluginException {
        br.getPage(parameter.getDownloadURL());
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
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void getVideoLink(DownloadLink parameter) throws IOException, PluginException {
        br.getPage(parameter.getDownloadURL());
        DLLINK = br.getRegex("addVariable\\(\\'file\\', \\'(http://.*?\\.flv)\\'\\)").getMatch(0);
        if (DLLINK == null) {
            DLLINK = br.getRegex("(http://s\\d+\\.motherlessmedia\\.com/dev[0-9/]+\\.flv/[a-z0-9]+/[A-Z0-9]+\\.flv)").getMatch(0);
        }
        if (DLLINK != null && !DLLINK.contains("?start=0")) DLLINK += "?start=0";
        if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    public void handleFree(DownloadLink link) throws Exception {
        if (link.getStringProperty("onlyregistered") != null) {
            logger.info(ONLY4REGISTEREDTEXT);
            throw new PluginException(LinkStatus.ERROR_FATAL, ONLY4REGISTEREDTEXT);
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
            String profileToSubscribe = br.getRegex("You can subscribe to the member from[\t\n\r ]+their <a href=\"http://motherless\\.com/m/([^\\'\"]+)\"").getMatch(0);
            if (profileToSubscribe == null) throw new PluginException(LinkStatus.ERROR_FATAL, SUBSCRIBEFAILED);
            br.getPage("http://motherless.com/subscribe/" + profileToSubscribe);
            String token = br.getRegex("name=\"_token\" value=\"(.*?)\"").getMatch(0);
            if (token == null) throw new PluginException(LinkStatus.ERROR_FATAL, SUBSCRIBEFAILED);
            br.postPage("http://motherless.com/subscribe/" + profileToSubscribe, "_token=" + token);
            if (!br.containsHTML("(>You are already subscribed to|>You are now subscribed to)")) throw new PluginException(LinkStatus.ERROR_FATAL, SUBSCRIBEFAILED);
            br.getPage(link.getDownloadURL());
        }
        doFree(link);

    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.postPage("http://motherless.com/auth/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember_me=1&__remember_me=0");
        if (br.getCookie("http://motherless.com/", "motherless_auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws IOException, PluginException {
        if (parameter.getStringProperty("onlyregistered") != null) {
            logger.info(ONLY4REGISTEREDTEXT);
            parameter.getLinkStatus().setStatusText("This " + parameter.getStringProperty("dltype") + " link can only be downloaded by registered users");
            return AvailableStatus.UNCHECKABLE;
        }
        this.setBrowserExclusive();
        br.getHeaders().put("User-Agent", ua);
        br.setFollowRedirects(true);
        String betterName = null;
        if ("video".equals(parameter.getStringProperty("dltype"))) {
            getVideoLink(parameter);
            betterName = new Regex(parameter.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
            if (betterName != null) betterName += ".flv";
        } else if ("image".equals(parameter.getStringProperty("dltype"))) {
            getPictureLink(parameter);
        }
        if (DLLINK == null) DLLINK = parameter.getDownloadURL();
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (con.getContentType().contains("html")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String name = getFileNameFromHeader(con);
            if (betterName == null) betterName = new Regex(name, "/([^/].*?\\.flv)").getMatch(0);
            if (betterName != null) name = betterName;
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

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}