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
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

//Nearly the same code as plugin UploadHereCom
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadking.com" }, urls = { "http://(www\\.)?uploadking\\.com/[A-Z0-9]+" }, flags = { 2 })
public class UploadKingCom extends PluginForHost {

    public UploadKingCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.uploadking.com/premium");
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadking.com/terms";
    }

    private static final String TEMPORARYUNAVAILABLE         = "(>Unfortunately, this file is temporarily unavailable|> \\- The server the file is residing on is currently down for maintenance)";
    private static final String TEMPORARYUNAVAILABLEUSERTEXT = "This file is temporary unavailable!";
    private static final Object LOCK                         = new Object();
    private static final String MAINPAGE                     = "http://www.uploadking.com/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.uploadkingcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT));
            return AvailableStatus.TRUE;
        }
        if (br.containsHTML("(>Unfortunately, this file is unavailable|> \\- Invalid link|> \\- The file has been deleted)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex infoWhileLimitReached = br.getRegex(">You are currently downloading (.*?) \\((\\d+.*?)\\)\\. Please ");
        String filename = br.getRegex("\">File: <b>(.*?)</b>").getMatch(0);
        if (filename == null) filename = infoWhileLimitReached.getMatch(0);
        String filesize = br.getRegex("\">Size: <b>(.*?)</b>").getMatch(0);
        if (filesize == null) filesize = infoWhileLimitReached.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(TEMPORARYUNAVAILABLE)) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.uploadkingcom.temporaryunavailable", TEMPORARYUNAVAILABLEUSERTEXT), 60 * 60 * 1000l);
        if (br.containsHTML("(>You are currently downloading|this download, before starting another\\.</font>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 5 * 60 * 1000l);
        String dllink = br.getRegex("\" id=\"dlbutton\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.uploadking\\.com:\\d+/files/[A-Z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // More connections possible but doesn't work for all links
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, -11);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(false);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
            br.postPage("http://www.uploadking.com/myaccount", "do=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(MAINPAGE, "u") == null || !br.containsHTML(">Account type:</TD><TD><b>Premium")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex(">Total storage space used:</TD><TD><b>(.*?)</b></TD>").getMatch(0);
        if (space != null) ai.setUsedSpace(space.trim());
        ai.setUnlimitedTraffic();
        String filesNum = br.getRegex(">Total files uploaded:</TD><TD><b>(\\d+)</b>").getMatch(0);
        if (filesNum != null) ai.setFilesNum(Integer.parseInt(filesNum));
        // Expire date not set because its not very precise
        // String expire =
        // br.getRegex("<b>Premium</b> \\(expires in (\\d+) weeks").getMatch(0);
        // if (expire == null) {
        // account.setValid(false);
        // return ai;
        // } else {
        // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire,
        // "dd MMMM yyyy", null));
        // }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<div style=\"position:absolute; left:0px; top:0px;\"><a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://www\\d+\\.uploadking\\.com:\\d+/files/[A-Z0-9]+/.*?)\"").getMatch(0);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}