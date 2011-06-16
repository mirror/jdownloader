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

import jd.PluginWrapper;
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

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "i-filez.com" }, urls = { "http://(www\\.)?i-filez\\.com/downloads/i/\\d+/f/.+" }, flags = { 2 })
public class IFilezCom extends PluginForHost {

    public IFilezCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "premium");
        // Would be needed if multiple free-downloads were possible
        // this.setStartIntervall(11 * 1000l);
    }

    @Override
    public String getAGBLink() {
        return "http://i-filez.com/terms";
    }

    private static final String CAPTCHATEXT = "includes/vvc\\.php\\?vvcid=";
    private static final String MAINPAGE    = "http://i-filez.com/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        // Set English language
        br.setCookie(MAINPAGE, "sdlanguageid", "2");
        br.setCustomCharset("utf-8");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Файл не найден в базе i-filez\\.com\\. Возможно Вы неправильно указали ссылку\\.<|>File was not found in the i-filez\\.com database|It is possible that you provided wrong link\\.</p>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = new Regex(link.getDownloadURL(), "i-filez\\.com/downloads/i/\\d+/f/(.+)").getMatch(0);
        String filesize = br.getRegex("<th>Size:</th>[\r\t\n ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim().replace(".html", ""));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        String md5 = br.getRegex("<th>MD5:</th>[\r\t\n ]+<td>([a-z0-9]{32})</td>").getMatch(0);
        if (md5 != null) link.setMD5Hash(md5);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String verifycode = br.getRegex("name=\"vvcid\" value=\"(\\d+)\"").getMatch(0);
        if (verifycode == null) verifycode = br.getRegex("\\?vvcid=(\\d+)\"").getMatch(0);
        if (!br.containsHTML(CAPTCHATEXT) || verifycode == null) {
            logger.warning("Captchatext not found or verifycode null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String code = getCaptchaCode("http://i-filez.com/includes/vvc.php?vvcid=" + verifycode, downloadLink);
        br.postPage(downloadLink.getDownloadURL(), "vvcid=" + verifycode + "&verifycode=" + code + "&FREE=Download+for+free");
        String additionalWaittime = br.getRegex("was recently downloaded from your IP address. No less than (\\d+) min").getMatch(0);
        if (additionalWaittime != null) {
            /* wait 1 minute more to be sure */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(additionalWaittime) + 1) * 60 * 1001l);
        }
        additionalWaittime = br.getRegex("was recently downloaded from your IP address. No less than (\\d+) sec").getMatch(0);
        if (additionalWaittime != null) {
            /* wait 15 secs more to be sure */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(additionalWaittime) + 15) * 1001l);
        }
        if (br.containsHTML(CAPTCHATEXT) || br.containsHTML(">The image code you entered is incorrect\\!<")) { throw new PluginException(LinkStatus.ERROR_CAPTCHA); }
        br.setFollowRedirects(false);
        String dllink = br.getRegex("document\\.getElementById\\(\"wait_input\"\\)\\.value= unescape\\(\\'(.*?)\\'\\);").getMatch(0);
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = Encoding.deepHtmlDecode(dllink).trim();
        int wait = 60;
        String regexedWaittime = br.getRegex("var sec=(\\d+);").getMatch(0);
        if (regexedWaittime != null) wait = Integer.parseInt(regexedWaittime);
        sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // Set English language
        br.setCookie(MAINPAGE, "sdlanguageid", "2");
        br.setCustomCharset("utf-8");
        br.postPage(MAINPAGE, "login=login&loginusername=" + Encoding.urlEncode(account.getUser()) + "&loginpassword=" + Encoding.urlEncode(account.getPass()) + "&submit=login&rememberme=on");
        if (br.getCookie(MAINPAGE, "sduserid") == null || br.getCookie(MAINPAGE, "sdpassword") == null || !br.containsHTML(">Premium account till")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("href=\\'/myspace/space/premium\\'>(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2})</a></div").getMatch(0);
        if (expire == null) {
            ai.setExpired(true);
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm:ss", null));
        }
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("<th>A link for 24 hours:</th>[\t\n\r ]+<td><input type=\"text\" readonly=\"readonly\" class=\"text_field width100\" onclick=\"this\\.select\\(\\);\" value=\"(http://.*?)\"").getMatch(0);
        dllink = null;
        if (dllink == null) dllink = br.getRegex("(\"|\\')(http://[a-z0-9]+\\.i\\-filez\\.com/premdw/\\d+/[a-z0-9]+/.*?)(\"|\\')").getMatch(1);
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, -2);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        // Max 2 connections at all, more only possible with luck
        return 1;
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