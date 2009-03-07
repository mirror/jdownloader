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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

public class Filer extends PluginForHost {

    private static final Pattern PATTERN_MATCHER_ERROR = Pattern.compile("errors", Pattern.CASE_INSENSITIVE);

    public Filer(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filer.net/premium");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        int maxCaptchaTries = 5;
        String code;
        String page = null;

        br.setCookiesExclusive(true);
        br.clearCookies("filer.net");
        br.getPage(downloadLink.getDownloadURL());
        int tries = 0;
        while (tries < maxCaptchaTries) {
            File captchaFile = Plugin.getLocalCaptchaFile(this, ".png");
            Browser.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
            code = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
            page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
            tries++;
            if (!page.contains("captcha.png")) {
                break;
            }
        }
        if (page != null && page.contains("captcha.png")) {
            linkStatus.addStatus(LinkStatus.ERROR_CAPTCHA);
            return;
        }

        if (Regex.matches(page, PATTERN_MATCHER_ERROR)) {
            String error = new Regex(page, "folgende Fehler und versuchen sie es erneut.*?<ul>.*?<li>(.*?)<\\/li>").getMatch(0);
            logger.severe("Error: " + error);
            linkStatus.addStatus(LinkStatus.ERROR_RETRY);
            return;

        }

        br.setFollowRedirects(false);
        if (br.toString().contains("Momentan sind die Limits f")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "All Free-User Slots Full", 10 * 1000 * 60l);
        String wait = new Regex(br, "Bitte warten Sie ([\\d]*?) Min bis zum").getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, new Long(wait) * 1000 * 60l);

        }
        Form[] forms = br.getForms();
        if (forms.length < 2) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000 * 60l); }
        page = br.submitForm(forms[1]);
        sleep(61000, downloadLink);

        dl = RAFDownload.download(downloadLink, br.createGetRequest(null));
        dl.startDownload();

    }

    public void login(Account account) throws IOException, PluginException, InterruptedException {
        br.getPage("http://www.filer.net/");
        Thread.sleep(500);
        br.getPage("http://www.filer.net/login");
        Thread.sleep(500);
        br.postPage("http://www.filer.net/login", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&commit=Einloggen");
        Thread.sleep(500);
        String cookie = br.getCookie("http://filer.net", "filer_net");
        if (cookie == null) {
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        String trafficleft = br.getRegex(Pattern.compile("<th>Traffic</th>.*?<td>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String validuntil = br.getRegex(Pattern.compile("<th>Mitglied bis</th>.*?<td>(.*?)</td>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        ai.setTrafficLeft(trafficleft);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");

        String[] splitted = validuntil.split("-");
        Date date = dateFormat.parse(splitted[2] + "." +  splitted[1] + "." + splitted[0]);
        ai.setValidUntil(date.getTime());

        ai.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        setBrowserExclusive();
        login(account);
        br.getPage(downloadLink.getDownloadURL());
        Thread.sleep(500);
        br.setFollowRedirects(false);
        String id = br.getRegex("<a href=\"\\/dl\\/(.*?)\">.*?<\\/a>").getMatch(0);
        if (id == null) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 10 * 60 * 1000l);
        br.getPage("http://www.filer.net/dl/" + id);
        String url = br.getRegex("url=(http.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        br.setFollowRedirects(true);
        dl = br.openDownload(downloadLink, url, true, 0);
        if (dl.getConnection().getContentType().contains("text")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE); }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://www.filer.net/faq";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        String page;
        File captchaFile;
        String code;
        int bytes;
        int maxCaptchaTries = 5;
        int tries = 0;
        while (maxCaptchaTries > tries) {
            try {
                Browser br = new Browser();
                br.getPage(downloadLink.getDownloadURL());
                captchaFile = Plugin.getLocalCaptchaFile(this, ".png");
                Browser.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
                code = Plugin.getCaptchaCode(captchaFile, this, downloadLink);
                page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
                if (Regex.matches(page, PATTERN_MATCHER_ERROR)) { return false; }
                if (downloadLink.getDownloadSize() == 0) {
                    bytes = (int) Regex.getSize(new Regex(page, "<tr class=\"even\">.*?<th>DateigrÃ¶ÃŸe</th>.*?<td>(.*?)</td>").getMatch(0));
                    downloadLink.setDownloadSize(bytes);
                }
                br.setFollowRedirects(false);
                Form[] forms = br.getForms();
                if (forms.length < 2) { return true; }
                if (downloadLink.getFinalFileName() != null) {
                    String filename = downloadLink.getFinalFileName();
                    downloadLink.setFinalFileName(null);
                    downloadLink.setName(filename);
                } else {
                    br.submitForm(forms[1]);
                    downloadLink.setName(Plugin.getFileNameFormURL(new URL(br.getRedirectLocation())));
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            tries++;
        }
        return false;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    public int getTimegapBetweenConnections() {
        return 500;
    }

    @Override
    public void resetPluginGlobals() {

    }
}
