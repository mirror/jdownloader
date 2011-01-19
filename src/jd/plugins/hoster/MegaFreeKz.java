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

import java.io.IOException;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megafree.kz" }, urls = { "http://[\\w\\.]*?megafree\\.kz/file\\d+" }, flags = { 2 })
public class MegaFreeKz extends PluginForHost {

    public MegaFreeKz(PluginWrapper wrapper) {
        super(wrapper);
        // At the moment this plugin only accepts free accounts
        this.enablePremium();
    }

    @Override
    public String getAGBLink() {
        return "http://megafree.kz/";
    }

    private static final String CAPTCHATEXT = "confirm\\.php";
    private static final String AREA2       = "megafree.kz/delayfile";
    private static final String MAINPAGE    = "http://megafree.kz/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains(AREA2)) {
                br.getPage(br.getRedirectLocation());
                return AvailableStatus.TRUE;
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            if (br.containsHTML("(>Запрашиваемый Вами файл не найден\\.<|Файл мог быть удален на основании претензии правообладателей)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            Regex nameAndSize = br.getRegex("<title>(.*?) \\[(.*?)\\] с MegaFree.KZ</title>");
            String filename = nameAndSize.getMatch(0);
            if (filename == null) filename = br.getRegex("\\(\\'http://megafree\\.kz/file\\d+\\', \\'(.*?) на upload\\.com\\.ua\\'\\);\"").getMatch(0);
            String filesize = br.getRegex("class=\"srchSize\">(.*?)</span>").getMatch(0);
            if (filesize == null) nameAndSize.getMatch(1);
            if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            link.setName(filename.trim());
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (!br.getURL().contains(AREA2)) {
            if (!br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.setFollowRedirects(true);
            boolean failed = true;
            for (int i = 0; i <= 3; i++) {
                br.postPage(downloadLink.getDownloadURL(), "submit=1&capcha_code=" + getCaptchaCode("http://megafree.kz/confirm.php?rnd=1", downloadLink));
                if (br.containsHTML(CAPTCHATEXT)) continue;
                failed = false;
                break;
            }
            if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String dllink = br.getRegex("new Array\\((.*?)\\);").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.trim().replaceAll("(\"|\"| |\r|\n|,)", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        // br.getPage(MAINPAGE);
        br.postPage(MAINPAGE, "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&submit=%D0%92%D0%BE%D0%B9%D1%82%D0%B8");
        String registered = br.getCookie(MAINPAGE, "registered");
        if (registered == null || !registered.equals("1")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) User");
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 2;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}