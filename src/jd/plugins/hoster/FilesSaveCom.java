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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files-save.com" }, urls = { "http://(www\\.)?files\\-save\\.com/(fr|en)/download\\-[a-z0-9]{32}\\.html" }, flags = { 2 })
public class FilesSaveCom extends PluginForHost {

    public FilesSaveCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.files-save.com/en/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.files-save.com/fr/faq/";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("http://www.files-save.com/en/download-" + new Regex(link.getDownloadURL(), "download\\-([a-z0-9]{32}\\.html)$").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("The requested file does not exist<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Filename :</td><td class=\"td\\-haut\\-gd\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null) filename = br.getRegex("class=\"title_top\"><h3>Download file : ([^<>\"]*?)</h3>").getMatch(0);
        final String extension = br.getRegex(">File Type :</td><td class=\"td\\-haut\\-gd1\">([^<>\"/]*?)</td>").getMatch(0);
        String filesize = br.getRegex(">Filesize :</td><td class=\"td\\-haut\\-gd1\">([^<>\"]*?)</td>").getMatch(0);
        if (filename == null || filesize == null || extension == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()) + "." + extension);
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("Mo", "Mb")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String reconnectWait = br.getRegex("count=(\\d+);").getMatch(0);
        if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(reconnectWait) * 1001l);
        if (!br.containsHTML("/img_key_protect\\.png")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final String code = getCaptchaCode("http://www.files-save.com/img_key_protect.png", downloadLink);
        if (code.length() != 6) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        br.postPage(br.getURL(), "verif_code=" + code);
        if (br.containsHTML("/img_key_protect\\.png")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        final String dllink = getDllink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("id=\"downloadlink\"><a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://ftp\\d+\\.files\\-save\\.com/files/[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        return dllink;
    }

    private void prepBrowser() {
        br.setCookie(MAINPAGE, "lang", "en");
    }

    private static final String MAINPAGE = "http://files-save.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                prepBrowser();
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
                br.setFollowRedirects(true);
                // br.getPage("");
                br.postPage("http://www.files-save.com/login.html", "souvenir=auto&Submit=Connexion&pseudolog=" + Encoding.urlEncode(account.getUser()) + "&mdp=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "mdp") == null || br.getCookie(MAINPAGE, "idlog") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                if (!br.containsHTML("<th>Account type:</th><td class=\"td\"><b>Premium</b>")) {
                    logger.info("Unknown/Unsupported accounttype");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
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
            return ai;
        }
        final String points = br.getRegex("<th>Current points:</th><td class=\"td\"><b id=\"money_all\">(\\d+)</b>").getMatch(0);
        if (points != null) ai.setPremiumPoints(points);
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex("<th>Expire Premium:</th><td class=\"td\"><b>([^<>\"]*?)</b>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM/dd/yyyy", Locale.ENGLISH));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final String dllink = getDllink();
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
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
        // Only possible when starting all at the same time, maybe even more possible but didn't have/find any more working example links
        return 3;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}