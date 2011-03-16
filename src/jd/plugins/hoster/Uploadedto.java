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

package jd.plugins.hoster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploaded.to" }, urls = { "(http://[\\w\\.-]*?uploaded\\.to/.*?(file/|\\?id=|&id=)[\\w]+/?)|(http://[\\w\\.]*?ul\\.to/(\\?id=|&id=)?[\\w\\-]+/.+)|(http://[\\w\\.]*?ul\\.to/(\\?id=|&id=)?[\\w\\-]+/?)" }, flags = { 2 })
public class Uploadedto extends PluginForHost {

    public Uploadedto(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://uploaded.to/ref?id=70683&r");
        setMaxConnections(20);
        this.setStartIntervall(2000l);
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        String url = link.getDownloadURL();
        url = url.replaceFirst("http://.*?/", "http://uploaded.to/");
        url = url.replaceFirst("\\.to/.*?id=", ".to/file/");
        if (!url.contains("/file/")) {
            url = url.replaceFirst("uploaded.to/", "uploaded.to/file/");
        }
        String[] parts = url.split("\\/");
        String newLink = "";
        for (int t = 0; t < Math.min(parts.length, 5); t++) {
            newLink += parts[t] + "/";
        }
        link.setUrlDownload(newLink);
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 2000;
    }

    private void login(Account account) throws Exception {
        this.setBrowserExclusive();
        workAroundTimeOut(br);
        br.setDebug(true);
        br.setFollowRedirects(true);
        br.setAcceptLanguage("en, en-gb;q=0.8");
        br.setCookie("http://uploaded.to", "lang", "en");
        br.getPage("http://uploaded.to/language/en");
        br.postPage("http://uploaded.to/io/login", "id=" + Encoding.urlEncode(account.getUser()) + "&pw=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("User and password do not match")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        if (br.getCookie("http://uploaded.to", "auth") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.setCookie("http://uploaded.to", "lang", "en");
        /* language is saved in account itself */
        br.getPage("http://uploaded.to/language/en");
        br.getPage("http://uploaded.to/me");
        String isPremium = br.getMatch("Status:</.*?<em(>Premium<)/em>");
        if (isPremium == null) {
            ai.setStatus("Free account");
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Premium account");
        account.setValid(true);
        String balance = br.getMatch("Balance:<.*?>([0-9,]+) ");
        String points = br.getMatch("Points:<.*?>([0-9\\.]+)<");
        String traffic = br.getMatch("downloading:<.*?>([0-9,]+ [GMB]+)");
        String expire[] = br.getRegex("Duration:<.*?>(\\d+) weeks? (\\d+) days? and (\\d+) hours?").getRow(0);
        if (expire != null) {
            long weeks = Integer.parseInt(expire[0]) * 7 * 24 * 60 * 60 * 1000l;
            long days = Integer.parseInt(expire[1]) * 24 * 60 * 60 * 1000l;
            long hours = Integer.parseInt(expire[2]) * 60 * 60 * 1000l;
            ai.setValidUntil(System.currentTimeMillis() + weeks + days + hours);
        } else {
            expire = br.getRegex("Duration:<.*?>(\\d+) days? and (\\d+) hours?").getRow(0);
            if (expire != null) {
                long days = Integer.parseInt(expire[0]) * 24 * 60 * 60 * 1000l;
                long hours = Integer.parseInt(expire[1]) * 60 * 60 * 1000l;
                ai.setValidUntil(System.currentTimeMillis() + days + hours);
            } else {
                expire = br.getRegex("Duration:<.*?>(\\d+) hours?").getRow(0);
                if (expire != null) {
                    long hours = Integer.parseInt(expire[0]) * 60 * 60 * 1000l;
                    ai.setValidUntil(System.currentTimeMillis() + hours);
                } else {
                    if (br.getRegex("Duration:<.*?(>unlimited<)").matches()) {
                        ai.setValidUntil(-1);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        if (balance != null) {
            balance = balance.replaceAll(",", ".");
            ai.setAccountBalance((long) (Double.parseDouble(balance) * 100));
        }
        ai.setTrafficLeft(SizeFormatter.getSize(traffic));
        ai.setTrafficMax(50 * 1024 * 1024 * 1024l);
        if (points != null) {
            points = points.replaceAll("\\.", "");
            ai.setPremiumPoints(Long.parseLong(points));
        }
        return ai;
    }

    private static void workAroundTimeOut(final Browser br) {
        try {
            if (br != null) {
                br.setConnectTimeout(30000);
                br.setReadTimeout(30000);
            }
        } catch (final Throwable e) {
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account);
        br.setFollowRedirects(false);
        String id = getID(downloadLink);
        br.getPage("http://uploaded.to/file/" + id);
        String error = new Regex(br.getRedirectLocation(), "http://uploaded.to/\\?view=(.*)").getMatch(0);
        if (error == null) {
            error = new Regex(br.getRedirectLocation(), "\\?view=(.*?)&i").getMatch(0);
        }
        if (error != null) {
            if (error.contains("error_traffic")) throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.uploadedto.errorso.premiumtrafficreached", "Traffic limit reached"), PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.getRedirectLocation() == null) {
            logger.info("InDirect Downloads active");
            Form form = br.getForm(0);
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            logger.info("Download from:" + form.getAction());
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 0);
        } else {
            logger.info("Direct Downloads active");
            logger.info("Download from:" + br.getRedirectLocation());
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.getRedirectLocation(), true, 0);
        }
        try {
            /* remove next major update */
            /* workaround for broken timeout in 0.9xx public */
            dl.getRequest().setConnectTimeout(30000);
            dl.getRequest().setReadTimeout(60000);
        } catch (final Throwable ee) {
        }
        dl.setFileSizeVerified(true);
        if (dl.getConnection().getLongContentLength() == 0 || !dl.getConnection().isContentDisposition()) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            if (br.getURL().contains("view=error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public String getAGBLink() {
        return "http://uploaded.to/agb";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException, InterruptedException {
        this.correctDownloadLink(downloadLink);
        this.checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        } else if (!downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    private String getPassword(DownloadLink downloadLink) throws Exception {
        String passCode = null;
        if (br.containsHTML("<h2>Authentifizierung</h2>")) {
            logger.info("pw protected link");
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
        }
        return passCode;
    }

    private String getID(DownloadLink downloadLink) {
        return new Regex(downloadLink.getDownloadURL(), "uploaded.to/file/(.*?)/").getMatch(0);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        this.setBrowserExclusive();
        workAroundTimeOut(br);
        String id = getID(downloadLink);
        br.setFollowRedirects(false);
        br.setCookie("http://uploaded.to/", "lang", "de");
        br.getPage("http://uploaded.to/language/de");
        br.getPage("http://uploaded.to/file/" + id);
        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains(".to/404")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
        String passCode = null;
        if (br.containsHTML("<h2>Authentifizierung</h2>")) {
            passCode = getPassword(downloadLink);
            Form form = br.getForm(0);
            form.put("pw", Encoding.urlEncode(passCode));
            br.submitForm(form);
            if (br.containsHTML("<h2>Authentifizierung</h2>")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
            }
            downloadLink.setProperty("pass", passCode);
        }
        Browser brc = br.cloneBrowser();
        brc.getPage("http://uploaded.to/js/download.js");
        String recaptcha = brc.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        rc.setId(recaptcha.trim());
        rc.load();
        Form rcForm = new Form();
        rcForm.setMethod(MethodType.POST);
        rcForm.setAction("http://uploaded.to/io/ticket/captcha/" + getID(downloadLink));
        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        rc.setForm(rcForm);
        String wait = br.getRegex("Aktuelle Wartezeit: <span>(\\d+)</span> Sekunden</span>").getMatch(0);
        String c = getCaptchaCode(cf, downloadLink);
        rc.setCode(c);
        if (br.containsHTML("err:\"captcha")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("limit-dl")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        if (br.containsHTML("limit-parallel")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You're already downloading", 60 * 60 * 1000l);
        if (br.containsHTML("limit-size")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only Premiumusers are allowed to download files lager than 1,00 GB.");
        if (wait != null) {
            this.sleep(Integer.parseInt(wait) * 1000l, downloadLink);
        }
        String url = br.getRegex("url:'(http:.*?)'").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
        try {
            /* remove next major update */
            /* workaround for broken timeout in 0.9xx public */
            dl.getRequest().setConnectTimeout(30000);
            dl.getRequest().setReadTimeout(60000);
        } catch (final Throwable ee) {
        }
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.getURL().contains("view=error")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerError", 10 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            Browser br = new Browser();
            workAroundTimeOut(br);
            br.setCookiesExclusive(true);
            StringBuilder sb = new StringBuilder();
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 80 links at once */
                    if (index == urls.length || links.size() > 80) break;
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("apikey=hP5Y37ulYfr8gSsS97LCT7kG5Gqp8Uug");
                int c = 0;
                for (DownloadLink dl : links) {
                    sb.append("&id_" + c + "=" + getID(dl));
                    c++;
                }
                br.postPage("http://uploaded.to/api/filemultiple", sb.toString());
                String infos[][] = br.getRegex(Pattern.compile("(.*?),(.*?),(.*?),(.*?),(.*?)(\r|\n|$)")).getMatches();
                for (DownloadLink dl : links) {
                    String id = getID(dl);
                    int hit = -1;
                    for (int i = 0; i < infos.length; i++) {
                        if (infos[i][1].equalsIgnoreCase(id)) {
                            hit = i;
                            break;
                        }
                    }
                    if (hit == -1) {
                        /* id not in response, so its offline */
                        dl.setAvailable(false);
                    } else {
                        dl.setFinalFileName(infos[hit][4].trim());
                        dl.setDownloadSize(SizeFormatter.getSize(infos[hit][2]));
                        if ("online".equalsIgnoreCase(infos[hit][0].trim())) {
                            dl.setAvailable(true);
                            dl.setMD5Hash(infos[hit][3].trim());
                        } else {
                            dl.setAvailable(false);
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

}
