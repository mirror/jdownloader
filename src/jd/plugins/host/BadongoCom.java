//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

package jd.plugins.host;

import java.io.IOException;
import java.util.Date;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class BadongoCom extends PluginForHost {

    public BadongoCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.badongo.com/compare");
    }

    @Override
    public String getAGBLink() {
        return "http://www.badongo.com/toc/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage(downloadLink.getDownloadURL().replaceAll("\\.viajd", ".com"));

        String filesize = br.getRegex(Pattern.compile("<div class=\"ffileinfo\">Ansichten.*?\\| Dateig.*?:(.*?)</div>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<div class=\"finfo\">(.*?)</div>").getMatch(0);

        long bytes = Regex.getSize(filesize);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("single")) {
            downloadLink.setName(filename.trim());
            downloadLink.setDownloadSize(Regex.getSize(filesize.trim()));
        } else {

            String parts = JDUtilities.fillString(downloadLink.getIntegerProperty("part", 1) + "", "0", "", 3);

            downloadLink.setName(filename.trim() + "." + parts);
            if (downloadLink.getIntegerProperty("part", 1) == downloadLink.getIntegerProperty("parts", 1)) {
                downloadLink.setDownloadSize(bytes - (downloadLink.getIntegerProperty("parts", 1) - 1) * 100 * 1024 * 1024);
            } else {
                downloadLink.setDownloadSize(100 * 1024 * 1024);
            }
        }
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        DownloadLink downloadLink = (DownloadLink) parameter;
        getFileInformation(parameter);
        login(account);
        isPremium();
        String link = null;
        br.getPage(downloadLink.getDownloadURL().replaceAll("\\.viajd", ".com"));
        sleep(5000l, parameter);
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("split")) {
            String downloadLinks[] = br.getRegex("doDownload\\(\\'(.*?)\\'\\)").getColumn(0);
            link = downloadLinks[downloadLink.getIntegerProperty("part", 1) - 1];
            sleep(5000l, parameter);
            br.getPage(link + "/ifr?pr=1&zenc=");
            link = link + "/loc?pr=1";
        } else {
            link = br.getRegex("onclick=\"return doDownload\\('(.*?)'\\)").getMatch(0);
        }
        if (link == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = br.openDownload(downloadLink, link, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            String page = br.loadConnection(dl.getConnection());
            br.getRequest().setHtmlCode(page);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }
        dl.startDownload();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        String link = null;
        String realURL = downloadLink.getDownloadURL().replaceAll("\\.viajd", ".com");
        getFileInformation(downloadLink);
        if (downloadLink.getStringProperty("type", "single").equalsIgnoreCase("split")) {
            String downloadLinks[] = br.getRegex("doDownload\\(\\'(.*?)\\'\\)").getColumn(0);
            link = downloadLinks[downloadLink.getIntegerProperty("part", 1) - 1];
            sleep(15000, downloadLink);
            br.getPage(link + "/ifr?pr=1&zenc=");
            handleErrors(br);
            dl = br.openDownload(downloadLink, link + "/loc?pr=1", true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                String page = br.loadConnection(dl.getConnection());
                br.getRequest().setHtmlCode(page);
                handleErrors(br);
            }
            dl.startDownload();
        } else {
            Browser ajax = br.cloneBrowser();
            ajax.getPage(realURL + "?rs=refreshImage&rst=&rsrnd=" + new Date().getTime());
            String cid = ajax.getRegex("cid\\=(\\d+)").getMatch(0);
            String code = this.getCaptchaCode("http://www.badongo.com/ccaptcha.php?cid=" + cid, downloadLink);
            Form captchaForm = ajax.getForm(0);
            captchaForm.remove(null);
            captchaForm.put("user_code", code);
            ajax.setFollowRedirects(true);
            captchaForm.setAction(ajax.getRegex("(http\\:\\/\\/www\\.badongo\\.com\\/\\w{2}\\/cfile\\/\\d+)").getMatch(0));
            ajax.submitForm(captchaForm);
            captchaForm = ajax.getForm(0);
            String url = null;
            this.sleep(45000, downloadLink);
            ajax.getPage(realURL + "?rs=getFileLink&rst=&rsrnd=" + new Date().getTime() + "&rsargs[]=yellow");
            url = ajax.getRegex("doDownload\\(\\\\\'(.*?)\\\\\'\\)").getMatch(0);
            if (url == null) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            ajax.getPage(url + "/ifr?pr=1&zenc=");
            handleErrors(ajax);
            dl = ajax.openDownload(downloadLink, url + "/loc?pr=1", true, 1);
            if (!dl.getConnection().isContentDisposition()) {
                String page = ajax.loadConnection(dl.getConnection());
                ajax.getRequest().setHtmlCode(page);
                handleErrors(ajax);
            }
            dl.startDownload();
        }
    }

    private void handleErrors(Browser br) throws PluginException {
        if (br.containsHTML("Gratis Mitglied Wartezeit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 30 * 1000l);
        if (br.containsHTML("Du hast Deine Download Quote überschritten")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);        
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    public boolean isPremium() throws PluginException, IOException {
        br.getPage("http://www.badongo.com/de/");
        String type = br.getRegex("Du bist zur Zeit als <b>(.*?)</b> eingeloggt").getMatch(0);
        if (type == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        if (new Regex(type, Pattern.compile("premium", Pattern.CASE_INSENSITIVE)).matches()) return true;
        throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
    }

    public void login(Account account) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.badongo.com", "badongoL", "de");
        br.getPage("http://www.badongo.com");
        br.getPage("http://www.badongo.com/de/login");
        Form form = br.getForm(0);
        form.put("username", Encoding.urlEncode(account.getUser()));
        form.put("password", Encoding.urlEncode(account.getPass()));
        br.submitForm(form);
        if (br.getCookie("http://www.badongo.com", "badongoU") == null || br.getCookie("http://www.badongo.com", "badongoP") == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        try {
            isPremium();
        } catch (PluginException e) {
            ai.setStatus("Not Premium Membership");
            ai.setValid(false);
            return ai;
        }
        ai.setStatus("Account ok");
        ai.setValid(true);
        return ai;
    }

}
