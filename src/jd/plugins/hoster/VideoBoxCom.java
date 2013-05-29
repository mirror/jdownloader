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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Cookie;
import jd.http.Cookies;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "videobox.com" }, urls = { "http://(www\\.)?videobox\\.com/(movie\\-details\\?contentId=|flashPage/)\\d+" }, flags = { 2 })
public class VideoBoxCom extends PluginForHost {

    public VideoBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.videobox.com/tour/home/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.videobox.com/public/terms-of-service";
    }

    private String DLLINK = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null) {
            link.getLinkStatus().setStatusText("Only checkable with enabled premium account");
            return AvailableStatus.UNCHECKABLE;
        }
        login(aa, false);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title> \\-  \\- VideoBox</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("\"upgrade\":true")) {
            link.getLinkStatus().setStatusText("Account upgrade needed to download this");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String sessionID = br.getCookie("http://videobox.com/", "JSESSIONID");
        if (sessionID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.videobox.com/content/details/generate/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0) + "/content-column.json?x-user-name=" + aa.getUser() + "&x-session-key=" + sessionID + "&callback=metai.buildMovieDetails");
        final String filename = getJson("name", br.toString());
        String firstSceneID = new Regex(link.getDownloadURL(), "videobox\\.com/flashPage/(\\d+)").getMatch(0);
        if (firstSceneID == null) firstSceneID = br.getRegex("\"firstSceneId\" : (\\d+)").getMatch(0);
        if (firstSceneID == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.videobox.com/content/download/options/" + firstSceneID + ".json?x-user-name=" + aa.getUser() + "&x-session-key=" + sessionID + "&callback=metai.buildDownloadLinks");
        final String dlSource = getDlInfo();
        String filesize = getJson("size", dlSource);
        DLLINK = getJson("url", dlSource);
        if (filesize == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + DLLINK.substring(DLLINK.lastIndexOf(".")));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    private String getDlInfo() {
        final String[] qualities = { "DVD", "H264_640", "HIGH", "H264_IPOD" };
        String qualityInfo = null;
        for (final String quality : qualities) {
            qualityInfo = br.getRegex("(\"res\" : \"" + quality + "\".*?)\\}").getMatch(0);
            if (qualityInfo != null) break;
        }
        return qualityInfo;
    }

    private String getJson(final String parameter, final String source) {
        String result = new Regex(source, "\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = new Regex(source, "\"" + parameter + "\"([ ]+)?:([ ]+)?\"([^<>\"]*?)\"").getMatch(2);
        return result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
    }

    private static final String MAINPAGE = "http://videobox.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("https://www.videobox.com/j_spring_security_check", "_spring_security_remember_me=true&login-page=login-page&j_username=" + Encoding.urlEncode(account.getUser()) + "&j_password=" + Encoding.urlEncode(account.getPass()) + "&x=" + new Random().nextInt(100) + "&y=" + new Random().nextInt(100));
                if (br.getCookie(MAINPAGE, "SPRING_SECURITY_REMEMBER_ME_COOKIE") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("https://www.videobox.com/billing/account");
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex(">next statement</div>[\t\n\r ]+<div class=\"value\">([^<>\"]*?)</div>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "MM/dd/yyyy", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}