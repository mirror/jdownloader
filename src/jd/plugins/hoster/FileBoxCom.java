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

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

//filebox by pspzockerscene
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filebox.com" }, urls = { "http://[\\w\\.]*?filebox\\.com/(.*?/[0-9a-z]{12}|[0-9a-z]{12})" }, flags = { 2 })
public class FileBoxCom extends PluginForHost {

    private static int simultanpremium = 1;

    public FileBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.filebox.com/premium.html");
    }

    @Override
    public String getAGBLink() {
        return "http://www.filebox.com/tos.html";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.filebox.com", "lang", "english");
        br.getPage("http://www.filebox.com/");
        br.postPage("http://www.filebox.com/member/account/login", "op=login&redirect=&data%5Baccount%5D%5Busr_login%5D=" + Encoding.urlEncode(account.getUser()) + "&data%5Baccount%5D%5Busr_password%5D=" + Encoding.urlEncode(account.getPass()) + "&x=20&y=11");
        br.getPage("http://www.filebox.com/?op=my_account");
        if (!br.containsHTML("Registered User") && !br.containsHTML("Premium User")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://www.filebox.com/", "login") == null || br.getCookie("http://www.filebox.com/", "xfss") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        String space = br.getRegex(Pattern.compile("Used space:</TD><TD><b>(.*?) of", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (space != null) ai.setUsedSpace(space + " Mb");
        String points = br.getRegex(Pattern.compile("You have collected:</TD><TD><b>(\\d+) premium ", Pattern.DOTALL | Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (points != null) ai.setPremiumPoints(Long.parseLong(points));
        account.setValid(true);
        ai.setUnlimitedTraffic();
        if (br.containsHTML("Registered User")) {
            ai.setStatus("Registered User");
        } else {
            ai.setStatus("Premium User");
            String balance = br.getRegex("Balance</td>.*?<b>.(.*?)</b></td>").getMatch(0);
            if (balance != null) {
                ai.setAccountBalance(balance);
            }
            String expire = br.getRegex("Premium-Account expire:</TD><TD><b>(.*?)</b>").getMatch(0);
            if (expire == null) {
                ai.setExpired(true);
                account.setValid(false);
                return ai;
            } else {
                ai.setValidUntil(Regex.getMilliSeconds(expire, "dd MMMM yyyy", null));
            }
        }
        return ai;
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.filebox.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<td nowrap>(.*?)</b></td>").getMatch(0));
        String filesize = br.getRegex("Size:.*?<small>\\((.*?)\\)</small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        String passCode = null;
        boolean premium = br.containsHTML("Premium User");
        if (!premium) {
            simultanpremium = 1;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.getPage(link.getDownloadURL());
        br.setFollowRedirects(true);
        // Form um auf "Datei herunterladen" zu klicken
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null && br.getRedirectLocation() != null && premium) {
            br.setFollowRedirects(true);
            dl = BrowserAdapter.openDownload(br, link, br.getRedirectLocation(), true, 0);
        } else {
            if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            if (br.containsHTML("valign=top><b>Password:</b></td>")) {
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                DLForm.put("password", passCode);
            }
            br.setFollowRedirects(true);
            if (premium) {
                dl = BrowserAdapter.openDownload(br, link, DLForm, true, 0);
            } else {
                dl = BrowserAdapter.openDownload(br, link, DLForm, false, 1);
            }
        }
        if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (premium) {
                String url = br.getRegex("direct link.*?href=\"(http:.*?)\"").getMatch(0);
                if (url == null) throw new PluginException(LinkStatus.ERROR_FATAL);
                dl = BrowserAdapter.openDownload(br, link, url, true, 0);
                if (dl.getConnection() != null && dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                }
            } else
                throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        // Form um auf "Datei herunterladen" zu klicken
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String passCode = null;
        if (br.containsHTML("valign=top><b>Password:</b></td>")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLForm.put("password", passCode);
        }
        // waittime
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001, downloadLink);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLForm, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("Wrong password")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw new PluginException(LinkStatus.ERROR_FATAL);
        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}