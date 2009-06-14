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
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDLocale;

public class FilerNet extends PluginForHost {

    private static final Pattern PATTERN_MATCHER_ERROR = Pattern.compile("errors", Pattern.CASE_INSENSITIVE);

    public FilerNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://filer.net/premium");
    }

    // @Override
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
            File captchaFile = getLocalCaptchaFile(".png");
            Browser.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
            code = getCaptchaCode(captchaFile, downloadLink);
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
        if (br.toString().contains("Momentan sind die Limits f")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDLocale.L("plugins.hoster.filernet.errors.nofreeslots", "All free user slots occupied"), 10 * 1000 * 60l);
        String wait = new Regex(br, "Bitte warten Sie ([\\d]*?) Min bis zum").getMatch(0);
        if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, new Long(wait) * 1000 * 60l);

        }
        Form[] forms = br.getForms();
        if (forms.length < 2) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000 * 60l); }
        page = br.submitForm(forms[1]);
        sleep(61000, downloadLink);

        dl = br.openDownload(downloadLink, (String) null);
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
            if(br.containsHTML("Mit diesem Usernamen ist bereits ein Benutzer eingelogged")){
                throw new PluginException(LinkStatus.ERROR_PREMIUM,"Fraud Detection. This Username is currently used by someone else.", LinkStatus.VALUE_ID_PREMIUM_TEMP_DISABLE);
                
            }
            
            account.setEnabled(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    // @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
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
        Date date = dateFormat.parse(splitted[2] + "." + splitted[1] + "." + splitted[0]);
        ai.setValidUntil(date.getTime());

        ai.setValid(true);
        return ai;
    }

    // @Override
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

    // @Override
    public String getAGBLink() {
        return "http://www.filer.net/faq";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) {
        if (downloadLink.getDownloadURL().contains("filer.net/dl/")) {
            downloadLink.setDownloadSize(0);
            downloadLink.setFinalFileName("Please logout in your Webbrowser and copy the free user links or copy the folder link.");
            return AvailableStatus.TRUE;

            /*
             * This method removes free-traffic from the Premium Account the url
             * is linked to - do not use it Browser br = new Browser();
             * URLConnectionAdapter urlConnection = null;
             * br.setFollowRedirects(true); try { urlConnection =
             * br.openGetConnection(downloadLink.getDownloadURL());
             * downloadLink.
             * setFinalFileName(Plugin.getFileNameFormHeader(urlConnection));
             * downloadLink.setBrowserUrl(downloadLink.getDownloadURL());
             * downloadLink
             * .setDownloadSize(urlConnection.getLongContentLength());
             * downloadLink.setDupecheckAllowed(true);
             * urlConnection.disconnect(); } catch (IOException e) {
             * jd.controlling
             * .JDLogger.getLogger().log(java.util.logging.Level.SEVERE
             * ,"Exception occurred",e); } return true;
             */
        }

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
                captchaFile = getLocalCaptchaFile(".png");
                Browser.download(captchaFile, br.openGetConnection("http://www.filer.net/captcha.png"));
                code = getCaptchaCode(captchaFile, downloadLink);
                page = br.postPage(downloadLink.getDownloadURL(), "captcha=" + code);
                if (Regex.matches(page, PATTERN_MATCHER_ERROR)) { return AvailableStatus.FALSE; }
                if (downloadLink.getDownloadSize() == 0) {
                    bytes = (int) Regex.getSize(new Regex(page, "<tr class=\"even\">.*?<th>Dateigröße</th>.*?<td>(.*?)</td>").getMatch(0));
                    downloadLink.setDownloadSize(bytes);
                }
                br.setFollowRedirects(false);
                Form[] forms = br.getForms();
                if (forms.length < 2) { return AvailableStatus.TRUE; }
                if (downloadLink.getFinalFileName() != null) {
                    String filename = downloadLink.getFinalFileName();
                    downloadLink.setFinalFileName(null);
                    downloadLink.setName(filename);
                } else {
                    br.submitForm(forms[1]);
                    downloadLink.setName(Plugin.getFileNameFormURL(new URL(br.getRedirectLocation())));
                }
                return AvailableStatus.TRUE;
            } catch (Exception e) {
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            }
            tries++;
        }
        return AvailableStatus.FALSE;
    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
