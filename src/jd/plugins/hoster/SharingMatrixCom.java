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

import java.io.File;

import jd.PluginWrapper;
import jd.http.Browser;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharingmatrix.com" }, urls = { "http://[\\w\\.]*?sharingmatrix\\.com/file/[0-9]+" }, flags = { 2 })
public class SharingMatrixCom extends PluginForHost {

    public SharingMatrixCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://sharingmatrix.com/premium");
    }

    public String getAGBLink() {
        return "http://sharingmatrix.com/contact";
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://sharingmatrix.com", "lang", "en");
        br.getPage("http://sharingmatrix.com/login");
        br.getPage("http://sharingmatrix.com/ajax_scripts/login.php?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        String validornot = br.toString();
        validornot = validornot.replaceAll("(\n|\r)", "");
        if (!validornot.equals("1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("http://sharingmatrix.com/ajax_scripts/personal.php?query=homepage");
        String expiredate = br.getRegex("([0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2})").getMatch(0);
        String daysleft = br.getRegex(",.*?([0-9]{1,3}).*?day\\(s\\) left").getMatch(0);
        if (expiredate != null) {
            ai.setValidUntil(Regex.getMilliSeconds(expiredate, "yyyy-MM-dd HH:mm:ss", null));
            account.setValid(true);
            return ai;
        }
        if (daysleft != null) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(daysleft) * 24 * 60 * 60 * 1000));
            account.setValid(true);
            return ai;
        }
        if (expiredate == null && daysleft == null) {
            account.setValid(false);
            return ai;
        }
        account.setValid(false);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        String dllink = downloadLink.getDownloadURL();
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        dl.startDownload();
    }

    // The hoster allows only 10 simultan connections so eighter 10 chunks or 10
    // simultan downloads. If the controller is extented one time we could make
    // this dynamically but right now i just set it to 10 max downloads because
    // many people might now know what chunks are and then they wonder that only
    // 1 download starts^^
    public int getMaxSimultanPremiumDownloadNum() {
        return 10;
    }

    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://sharingmatrix.com", "lang", "en");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:.*?<td>(.*?)</td>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"fname\">(.*?)</div>").getMatch(0);
        }
        Regex reg = br.getRegex("<th>Size:.*?<td>(.*?)&nbsp;(.*?)</td>");
        String filesize = reg.getMatch(0) + reg.getMatch(1);
        if (filesize.contains("null")) {
            reg = br.getRegex("\"fsize_div\">(.*?)&nbsp;(.*?)</div>");
            filesize = reg.getMatch(0) + reg.getMatch(1);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // System.out.print(br.toString());
        // if
        // (br.containsHTML("You have got max allowed bandwidth size per hour"))
        // { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED); }
        if (br.containsHTML("no available free download slots left")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available for this file");
        String linkid = br.getRegex("link_id = '(\\d+)';").getMatch(0);
        if (linkid == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
        String freepage = "http://sharingmatrix.com/ajax_scripts/download.php?type_membership=free&link_id=" + linkid;
        br.getPage(freepage);
        String link_name = br.getRegex("link_name = '([^']+')").getMatch(0);
        // System.out.print(br.toString());
        // long time = System.currentTimeMillis();
        String linkurl = br.getRegex("<input\\.*document\\.location=\"(.*?)\";").getMatch(0);
        // String captchalink =
        // "http://sharingmatrix.com/include/crypt/cryptographp.php?cfg=0&";
        // br.cloneBrowser().getPage(captchalink);
        String captchalink = "http://sharingmatrix.com/include/crypt/cryptographp.inc.php?cfg=0&sn=PHPSESSID&";

        File captcha = getLocalCaptchaFile();

        Browser brc = br.cloneBrowser();
        brc.setCookiesExclusive(true);
        brc.setCookie("sharingmatrix.com", "cryptcookietest", "1");
        brc.getDownload(captcha, captchalink);
        // br.cloneBrowser().getPage("http://sharingmatrix.com/js/jquery-impromptu.1.5.js?_="
        // + System.currentTimeMillis());
        String code = getCaptchaCode(captcha, downloadLink);
        Browser br2 = br.cloneBrowser();
        // looks like wait time is js only (can be skipped)
        // long delay = 60000 - (System.currentTimeMillis() - time);
        // try {
        // delay =
        // (Long.parseLong(br.getRegex("current_time = '([\\d]+)';").getMatch(0))
        // * 1000) - (System.currentTimeMillis() - time);
        // } catch (Exception e) {
        // }
        // sleep(delay, downloadLink);
        br2.postPage("http://sharingmatrix.com/ajax_scripts/verifier.php", "?&code=" + code);
        if (Integer.parseInt(br2.toString().trim()) != 1) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dl_id = br2.getPage("http://sharingmatrix.com/ajax_scripts/dl.php").trim();
        // br2.getPage("http://sharingmatrix.com/ajax_scripts/update_dl.php?id="
        // + dl_id);
        br2.getPage("http://sharingmatrix.com/ajax_scripts/_get.php?link_id=" + linkid + "&link_name=" + link_name + "&dl_id=" + dl_id + "&password=");
        linkurl = br2.getRegex("serv:\"([^\"]+)\"").getMatch(0) + "/download/" + br2.getRegex("hash:\"([^\"]+)\"").getMatch(0) + "/" + dl_id.trim() + "/";
        // br2.getPage("http://sharingmatrix.com/ajax_scripts/update_dl.php?id="
        // + dl_id);
        // System.out.println(br2.getCookies("http://sharingmatrix.com"));
        // System.out.print(br2.toString());
        if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br2, downloadLink, linkurl, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            br2.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public void resetPluginGlobals() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }
}