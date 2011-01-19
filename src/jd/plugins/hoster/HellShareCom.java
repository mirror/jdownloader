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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hellshare.com" }, urls = { "http://[\\w\\.]*?(download\\.)?(sk|cz|en)?hellshare\\.(com|sk|hu|de)/((.+/[0-9]+)|(/[0-9]+/.+/.+))" }, flags = { 2 })
public class HellShareCom extends PluginForHost {

    public HellShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.en.hellshare.com/register");
    }

    @Override
    public String getAGBLink() {
        return "http://www.en.hellshare.com/terms";
    }

    private static final String LIMITREACHED = "(You have exceeded today´s free download limit|<strong>Dnešní limit free downloadů jsi vyčerpal\\.</strong>)";

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        String numbers = new Regex(link.getDownloadURL(), "hellshare\\.com/(\\d+)").getMatch(0);
        if (numbers == null) link.setUrlDownload(link.getDownloadURL().replaceAll("http.*?//.*?/", "http://download.en.hellshare.com/"));
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        /* to prefer english page */
        br.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        br.setFollowRedirects(false);
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.postPage("http://www.hellshare.com/login?do=loginForm-submit", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&login=odeslat&DownloadRedirect=");
        /*
         * this will change account language to eng,needed because language is
         * saved in profile
         */
        String changetoeng = br.getRegex("\"(http://www\\.en\\.hellshare\\.com/--.*?profile.*?)\"").getMatch(0);
        if (changetoeng == null) {
            // Do NOT throw an exeption here as this part isn't that important
            // but it's bad that the plugin breaks just because of this regex
            logger.warning("Language couldn't be changed. This will probably cause trouble...");
        } else {
            br.getPage(changetoeng);
        }
        if (!br.containsHTML("credit for downloads") || br.containsHTML("Špatně zadaný login nebo heslo uživatele")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        String hostedFiles = br.getRegex(">Number of your files:</label></th>.*?<td id=\"info_files_counter\"><strong>(\\d+)</strong></td>").getMatch(0);
        if (hostedFiles != null) ai.setFilesNum(Long.parseLong(hostedFiles));
        String trafficleft = br.getRegex("id=\"info_credit\" class=\"va-middle\">[\n\t\r ]+<strong>(.*?)</strong>").getMatch(0);
        if (trafficleft != null) {
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleft));
        }
        ai.setStatus("Premium User");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        String dllink = br.getRegex("launchFullDownload\\('.*?(http:.*?)(&|\"|')").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(http://data.*?\\.helldata\\.com.*?)(&|\"|')").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("dllink (premium) is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replaceAll("\\\\", "");
        /*
         * set max chunks to 1 because each range request counts as download,
         * reduces traffic very fast ;)
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.containsHTML("<h1>File not found</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("The server is under the maximum load")) throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server is under maximum load", 10 * 60 * 1000l);
            if (br.containsHTML("Incorrectly copied code from the image")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("You are exceeding the limitations on this download")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        /* to prefer english page UPDATE: English does not work anymore */
        br.setCustomCharset("utf-8");
        br.getHeaders().put("Accept-Language", "en-gb;q=0.9, en;q=0.8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<h1>File not found</h1>") || br.containsHTML("<h1>Soubor nenalezen</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("FileSize_master\">(.*?)</strong>").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("\"The content.*?with a size of (.*?) has been uploaded").getMatch(0);
        }
        String filename = br.getRegex("\"FileName_master\">(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("\"The content (.*?) with a size").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) – Download").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("hidden\">Downloading file (.*?)</h1>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("keywords\" content=\"HellShare, (.*?)\"").getMatch(0);
                    }
                }
            }
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace("&nbsp;", "")));
        link.setUrlDownload(br.getURL());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(LIMITREACHED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        String changetocz = br.getRegex("lang=\"cz\" xml:lang=\"cz\" href=\"(http://download\\.cz\\.hellshare\\.com/.*?/\\d+)\"").getMatch(0);
        if (changetocz == null) {
            // Do NOT throw an exeption here as this part isn't that important
            // but it's bad that the plugin breaks just because of this regex
            logger.warning("Language couldn't be changed. This will probably cause trouble...");
        } else {
            br.getPage(changetocz);
            if (br.containsHTML("No htmlCode read")) br.getPage(downloadLink.getDownloadURL());
        }
        br.setDebug(true);
        if (br.containsHTML("Current load 100%")) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.CurrentLoadIs100Percent", "The current serverload is 100%"), 15 * 60 * 1000l); }
        if (br.containsHTML(LIMITREACHED)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        String freePage = br.getURL().replace("hellshare.com/serialy/", "hellshare.com/").replace("/pop/", "/").replace("filmy/", "") + "/free";
        br.getPage(freePage);
        if (br.containsHTML("The server is under the maximum load")) {
            logger.info(JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"));
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"), 10 * 60 * 1000l);
        }
        if (br.containsHTML("You are exceeding the limitations on this download")) {
            logger.info("You are exceeding the limitations on this download");
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        }
        if (br.containsHTML("<h1>File not found</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String fileId = new Regex(downloadLink.getDownloadURL(), "/(\\d+)$").getMatch(0);
        Form form = br.getForm(0);
        if (form == null || fileId == null || !br.containsHTML("antispam\\.php\\?sv=FreeDown:")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String captcha = "http://www.en.hellshare.com/antispam.php?sv=FreeDown:" + fileId;
        String code = getCaptchaCode(captcha, downloadLink);
        form.put("captcha", Encoding.urlEncode(code));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            if (br.getURL().contains("errno=404")) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.HellShareCom.error.404", "404 Server error. File might not be available for your country!"));
            if (br.containsHTML("<h1>File not found</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (br.containsHTML("The server is under the maximum load")) {
                logger.info(JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"));
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.HellShareCom.error.ServerUnterMaximumLoad", "Server is under maximum load"), 10 * 60 * 1000l);
            }
            if (br.containsHTML("(Incorrectly copied code from the image|Opište barevný kód z obrázku)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            if (br.containsHTML("You are exceeding the limitations on this download")) {
                logger.info("You are exceeding the limitations on this download");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
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
