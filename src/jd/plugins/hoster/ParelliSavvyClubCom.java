//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.JDUtilities;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "parellisavvyclub.com" }, urls = { "https?://(www\\.)?parellisavvyclub\\.com/(watchMedia\\.faces\\?id=\\d+|video\\?sckey=[^\"\\']+(\\&pl=\\d+)?)" }, flags = { 2 })
public class ParelliSavvyClubCom extends PluginForHost {

    private static final String MAINPAGE = "http://www.parellisavvyclub.com";

    private static final Object LOCK     = new Object();

    public ParelliSavvyClubCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://shop.parellinaturalhorsetraining.com/savvySignupStep1.jsf");
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("https://", "http://"));
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.parellisavvyclub.com/termsofservice.faces";
    }

    private String getDllink() {
        String dllink = br.getRegex("(http://down\\d+\\.parelli\\.com/[^\"\\']+)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("flashvars=\"file=(.*?)\"").getMatch(0);
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadalbe for premium members");
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.getPage(link.getDownloadURL());
        final String dllink = getDllink();
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        final String[] urlValues = new Regex(dllink, "(.*?)\\&streamer=(.*?)\\&autostart.+").getRow(0);
        if (urlValues == null || urlValues.length != 2) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }

        if (urlValues[1].startsWith("rtmp")) {
            if (isStableEnviroment()) { throw new PluginException(LinkStatus.ERROR_FATAL, "Developer Version of JD needed!"); }

            dl = new RTMPDownload(this, link, urlValues[1] + "/" + urlValues[0]);
            final jd.network.rtmp.url.RtmpUrlConnection rtmp = ((RTMPDownload) dl).getRtmpConnection();

            rtmp.setPlayPath(Encoding.htmlDecode("mp4:" + urlValues[0]));
            rtmp.setApp(new Regex(urlValues[1], ".+://[\\w\\.]+/(.*?)$").getMatch(0));
            rtmp.setUrl(urlValues[1]);
            rtmp.setResume(true);
            rtmp.setSwfVfy("http://parconassets.s3.amazonaws.com/1308985315/flash/player-licensed.swf");

            ((RTMPDownload) dl).startDownload();

        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private boolean isStableEnviroment() {
        String prev = JDUtilities.getRevision();
        if (prev == null || prev.length() < 3) {
            prev = "0";
        } else {
            prev = prev.replaceAll(",|\\.", "");
        }
        final int rev = Integer.parseInt(prev);
        if (rev < 10000) { return true; }
        return false;
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) {
                acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            }
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("userName") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(false);
            br.getPage("https://www.parellisavvyclub.com/login.faces");
            final String viewState = br.getRegex("id=\"javax\\.faces\\.ViewState\" value=\"(j_id\\d+)\"").getMatch(0);
            if (viewState == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            br.postPage("https://www.parellisavvyclub.com/login.faces", "funid=funid&funid%3AusernameInput=" + Encoding.urlEncode(account.getUser()) + "&funid%3ApasswordInput=" + Encoding.urlEncode(account.getPass()) + "&funid%3AloginBut.x=0&funid%3AloginBut.y=0&javax.faces.ViewState=" + viewState);
            if (br.getCookie(MAINPAGE, "userName") == null || br.getCookie(MAINPAGE, "email") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            account.setValid(true);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Link only checkable if you have an account!");
            return AvailableStatus.UNCHECKABLE;
        }
        login(aa, false);
        br.getPage(link.getDownloadURL());
        final String dllink = getDllink();
        if (dllink == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String filename = new Regex(dllink, ".*?parelli\\.com/.{1,10}/(.*?)\\?Policy=").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"plWatchVideo playing\" rel=\"nofollow\" href=\"[^\"\\']+\">(.*?)<br>").getMatch(0);
        }
        if (filename == null) {
            filename = br.getRegex("class=\"playing\">(.*?)<").getMatch(0);
        }
        if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        filename = filename.trim();
        String ext = filename.substring(filename.lastIndexOf("."));
        if (ext == null) {
            ext = ".mov";
        }
        if (!filename.contains(ext)) {
            filename += ext;
        }
        if (dllink.matches(".+&streamer=rtmp.+")) {
            filename = filename.replaceAll("\\.mov$", ".flv");
        } else {
            link.setFinalFileName(filename);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}