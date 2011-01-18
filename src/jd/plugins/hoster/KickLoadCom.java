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
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kickload.com" }, urls = { "http://[\\w\\.]*?kickload\\.com/(file/\\d+/.+|get/[A-Za-z0-9]+/.+)" }, flags = { 2 })
public class KickLoadCom extends PluginForHost {
    private static final String MAINPAGE            = "http://kickload.com/";
    private static final String PREMIUMONLYTEXT     = "To download this file you need a premium account";
    private static final String PREMIUMONLYUSERTEXT = "Only available for premium users";
    private static final String POSTPAGEREGEX       = "\"(http://(srv|ftp)\\d+\\.kickload\\.com/download\\.php\\?ticket=.*?)\"";

    public KickLoadCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://kickload.com/premium/");
    }

    @Override
    public String getAGBLink() {
        return "http://kickload.com/tos/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!downloadLink.isAvailable()) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML(PREMIUMONLYTEXT)) throw new PluginException(LinkStatus.ERROR_FATAL, PREMIUMONLYUSERTEXT);
        br.postPage(downloadLink.getDownloadURL(), "free_download=1&free_download1.x=&free_download1.y=&free_download1=1");
        if (br.containsHTML("(ou are already downloading a file\\.|Please, wait until the file has been loaded\\.</h2>)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
        String postPage = br.getRegex(POSTPAGEREGEX).getMatch(0);
        String ticket = getTicket();
        if (ticket == null || postPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, postPage, "ticket=" + ticket + "&x=&y=", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("error=filedontexist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getTicket() {
        String ticket = br.getRegex("\\?ticket=(.*?)\"").getMatch(0);
        if (ticket == null) ticket = br.getRegex("name=\"ticket\" id=\"ticket\" value=\"(.*?)\"").getMatch(0);
        return ticket;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie(MAINPAGE, "lang", "en");
        br.postPage("http://kickload.com/login/", "email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&login=1&x=0&y=0");
        if (br.getCookie(MAINPAGE, "email") == null || br.getCookie(MAINPAGE, "password") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://kickload.com/settings/");
        account.setValid(true);
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("<strong>Valid until</strong></td>[\t\n\r ]+<td width=\"354\">([0-9-]+)</td>").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setStatus("Premium User");
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "MM-dd-yyyy", null));
        }
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        final boolean resumable = true;
        final int maxchunks = 1;
        String dllink = br.getRedirectLocation();
        if (dllink != null) {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        } else {
            String postPage = br.getRegex(POSTPAGEREGEX).getMatch(0);
            String ticket = getTicket();
            if (ticket == null || postPage == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, postPage, "ticket=" + ticket + "&x=&y=", resumable, maxchunks);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getURL().contains("error=filedontexist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            br.setCookiesExclusive(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 5 links at once, get request */
                    if (index == urls.length || links.size() > 5) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("url=");
                int c = 0;
                for (DownloadLink dl : links) {
                    if (c > 0) sb.append(Encoding.urlEncode("|"));
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                /* post seems buggy */
                br.getPage("http://api.kickload.com/linkcheck.php?" + sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("((\\d+)?;?;?(.*?);;(.*?);;(\\d+))|((\\d+);;FILE)")).getMatches();
                for (int i = 0; i < links.size(); i++) {
                    DownloadLink dL = links.get(i);
                    if (infos[i][2] == null) {
                        /* id not in response, so its offline */
                        dL.setAvailable(false);
                    } else {
                        if ("OK".equals(infos[i][2])) {
                            dL.setFinalFileName(infos[i][3].trim());
                            dL.setDownloadSize(SizeFormatter.getSize(infos[i][4]));
                            dL.setAvailable(true);
                        } else {
                            dL.setAvailable(false);
                        }
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
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