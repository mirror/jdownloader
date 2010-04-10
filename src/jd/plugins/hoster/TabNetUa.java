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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tab.net.ua" }, urls = { "http://[\\w\\.]*?tab\\.net\\.ua/sites/files/site_name\\..*?/id\\.\\d+/" }, flags = { 2 })
public class TabNetUa extends PluginForHost {

    public TabNetUa(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://tab.net.ua/registration/");
    }

    @Override
    public String getAGBLink() {
        return "http://tab.net.ua/";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        String fid = new Regex(link.getDownloadURL(), "tab\\.net\\.ua/sites/files/site_name\\..*?/id\\.(\\d+)/").getMatch(0);
        if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("name=\"password\\[files_")) {
            link.getLinkStatus().setStatusText("This file is password protected");
            if (link.getStringProperty("pass", null) != null) {
                handlePassword(link.getStringProperty("pass", null), link);
            } else {
                link.setName(fid);
                return AvailableStatus.TRUE;
            }
        } else if (br.containsHTML(">Тільки зареєстровані користувачі можуть скачати цей файл<")) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.TabNetUa.only4resteredusers", "This file is only downloadable for registered users!"));
            link.setName(fid);
            return AvailableStatus.TRUE;
        }
        String filename = br.getRegex("<span style=\"font-size:21px; color:#091E35; font-family:'Trebuchet MS';\">(.*?)</span><br>").getMatch(0);
        if (filename == null) filename = br.getRegex("<input type=\"text\" id=\"re\" value=\"Re:(.*?)\"").getMatch(0);
        String filesize = br.getRegex(">Розмір файлу:(.*?), завантажено").getMatch(0);
        if (filename == null || filename.equals("") || (filesize != null && filesize.equals(" 0 байт"))) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename.trim());
        if (filesize != null) {
            if (filesize.contains("Gб") || filesize.contains("Гб"))
                filesize = filesize.replaceAll("(Gб|Гб)", "GB");
            else if (filesize.contains("")) filesize = filesize.replace("Мб", "MB");
            link.setDownloadSize(Regex.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML("name=\"password\\[files_")) {
            handlePassword(downloadLink.getStringProperty("pass", null), downloadLink);
        } else if (br.containsHTML(">Тільки зареєстровані користувачі можуть скачати цей файл<")) { throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.TabNetUa.only4resteredusers", "This file is only downloadable for registered users!")); }
        for (int i = 0; i <= 3; i++) {
            String captchaUrl = br.getRegex("Введіть число, вказане на картинці:<br>.*?<img src=\"(http://tab\\.net\\.ua/.*?)\"").getMatch(0);
            if (captchaUrl == null) captchaUrl = br.getRegex("\"(http://tab\\.net\\.ua/tools/antispam\\.php\\?id=[a-z0-9]+\\&r=\\d+)\"").getMatch(0);
            if (captchaUrl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchaUrl, downloadLink);
            br.postPage(br.getURL(), "antispam=" + code);
            if (br.containsHTML("(>Не співпадає з числом на картинці<|antispam.php?)")) continue;
            break;
        }
        if (br.containsHTML("Вы ввели неправильный код проверки")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("; background-repeat:no-repeat;\">.*?<a href=\"(http://.*?)\" class=\"file_download\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://d\\d+\\.tab\\.net\\.ua:\\d+/\\d+/.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("<title>404 Not Found</title>")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setFinalFileName(getFileNameFromHeader(dl.getConnection()));
        dl.startDownload();
    }

    public void handlePassword(String passCode, DownloadLink downloadLink) throws Exception, PluginException {
        String fid = new Regex(downloadLink.getDownloadURL(), "tab\\.net\\.ua/sites/files/site_name\\..*?/id\\.(\\d+)/").getMatch(0);
        if (fid == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        for (int i = 0; i <= 3; i++) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Please enter the password file with the ID: " + fid, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            br.postPage(downloadLink.getDownloadURL(), "password%5Bfiles_" + fid + "%5D=" + passCode);
            logger.info("File is password protected, password = " + passCode);
            if (br.containsHTML("name=\"password\\[files_")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                downloadLink.setProperty("pass", null);
                continue;
            }
            break;
        }
        if (br.containsHTML("name=\"password\\[files_")) {
            logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
            downloadLink.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
    }

    public void login(Account account) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.postPage("http://tab.net.ua/", "r%5Blog%5D=" + Encoding.urlEncode(account.getUser()) + "&r%5Bpass%5D=" + Encoding.urlEncode(account.getPass()));
        if (br.getCookie("http://tab.net.ua", "sid") == null || br.getCookie("http://tab.net.ua", "password") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        ai.setStatus("Registered (Free) User");
        account.setValid(true);

        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        requestFileInformation(parameter);
        login(account);
        br.getPage(parameter.getDownloadURL());
        doFree(parameter);
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