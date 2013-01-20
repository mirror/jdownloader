//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datei.to", "sharebase.to" }, urls = { "http://(www\\.)?(sharebase\\.(de|to)/(files/|1,)|datei\\.to/datei/)[\\w]+\\.html", "blablablaInvalid_regex" }, flags = { 2, 2 })
public class DateiTo extends PluginForHost {

    private static final String  APIPAGE          = "http://api.datei.to/";
    private static final String  FILEIDREGEX      = "datei\\.to/datei/(.*?)\\.html";
    private static final String  DOWNLOADPOSTPAGE = "http://datei.to/response/download";
    private static final String  RECAPTCHATEXT    = "Eingabe war leider falsch\\. Probiere es erneut";
    private static AtomicBoolean useAPI           = new AtomicBoolean(true);

    public DateiTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datei.to/premium");
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws MalformedURLException {
        if ("sharebase.to".equals(link.getHost())) {
            /* README: this is how to change hostname in 09581 stable */
            try {
                final Field pidField = link.getClass().getDeclaredField("host");
                pidField.setAccessible(true);
                pidField.set(link, "datei.to");
            } catch (Throwable e) {
                logger.severe("could not rewrite host: " + e.getMessage());
            }
        }
        String id = new Regex(link.getDownloadURL(), "(/files/|/1,)([\\w]+\\.html)").getMatch(1);
        if (id != null) link.setUrlDownload("http://datei.to/datei/" + id);
    }

    @Override
    public String getAGBLink() {
        return "http://datei.to/agb";
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage(APIPAGE, "key=YYMHGBR9SFQA0ZWA&info=COMPLETE&datei=" + new Regex(downloadLink.getDownloadURL(), FILEIDREGEX).getMatch(0));
        if (!br.containsHTML("online") || br.containsHTML("offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex info = br.getRegex(";(.*?);(\\d+);");
        downloadLink.setFinalFileName(info.getMatch(0));
        downloadLink.setDownloadSize(SizeFormatter.getSize(info.getMatch(1)));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);

        if (true) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        final String dlID = br.getRegex("id=\"DLB\"><button id=\"([^<>\"]*?)\"").getMatch(0);
        if (dlID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(DOWNLOADPOSTPAGE, "Step=1&ID=" + dlID);
        final String waittime = br.getRegex("<span id=\"DCS\">(\\d+)</span> Sekunden").getMatch(0);
        int wait = 30;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        // Ab hier kaputt
        boolean failed = true;
        for (int i = 0; i <= 5; i++) {
            if (br.containsHTML("(Da hat etwas nicht geklappt|>Hast du etwa versucht, die Wartezeit zu umgehen)")) {
                logger.info("Countdown or server-error");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            String reCaptchaId = br.getRegex("Recaptcha\\.create\\(\"(.*?)\"").getMatch(0);
            if (reCaptchaId == null) {
                logger.warning("reCaptchaId is null...");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final Form reCaptchaForm = new Form();
            reCaptchaForm.setMethod(Form.MethodType.POST);
            reCaptchaForm.setAction("http://datei.to/response/recaptcha");
            reCaptchaForm.put("modul", "checkDLC");
            final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(reCaptchaForm);
            rc.setId(reCaptchaId);
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode(cf, downloadLink);
            reCaptchaForm.put("recaptcha_response_field", Encoding.urlEncode(c));
            reCaptchaForm.put("recaptcha_challenge_field", rc.getChallenge());
            rc.setCode(c);
            if (!br.containsHTML(RECAPTCHATEXT)) {
                failed = false;
                break;
            }
            br.postPage(DOWNLOADPOSTPAGE, "Step=1&ID=" + dlID);
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.toString();
        if (!dllink.startsWith("http") || dllink.length() > 500) {
            logger.warning("Invalid dllink...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.trim();
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
            logger.warning("The dllink doesn't seem to be a file...");
            br.followConnection();
            // Shouldn't happen often
            if (br.containsHTML("(window\\.location\\.href=\\'http://datei\\.to/login|form id=\"UploadForm\")")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1001l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        // to pick up when free account has been picked up from api and throw exception, remove when free account supported.
        account.setProperty("isPremium", true);
        if (useAPI.get() == true) {
            try {
                apiLogin(account);
            } catch (PluginException e) {
                // free accounts are not supported atm, we need to exit when free account been detected, so we don't loop unnecessarily
                if (!account.getBooleanProperty("isPremium")) {
                    throw (PluginException) e;
                } else {
                    useAPI.set(false);
                }
            }
            if (useAPI.get() == true) {
                String expireDate = br.getRegex("premium;(\\d+)").getMatch(0);
                if (expireDate != null)
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate));
                else
                    ai.setExpired(true);
                ai.setUnlimitedTraffic();
            }
        }
        if (useAPI.get() == false) {
            try {
                webLogin(account, true);
            } catch (PluginException e) {
                account.setValid(false);
                return ai;
            }
            br.getPage("/konto");
            String accountType = br.getRegex(">Konto\\-Typ:</div><div[^>]+><span[^>]+>(.*?)\\-Account</span>").getMatch(0);
            if (accountType != null && accountType.equals("Premium")) {
                // premium account
                String space = br.getRegex(">loadSpaceUsed\\(\\d+, (\\d+)").getMatch(0);
                if (space != null) {
                    ai.setUsedSpace(space + " GB");
                } else {
                    logger.warning("Couldn't find space used!");
                }
                String traffic = br.getRegex("loadTrafficUsed\\((\\d+(\\.\\d+))").getMatch(0);
                if (traffic != null) {
                    ai.setTrafficLeft(SizeFormatter.getSize(traffic + " GB"));
                } else {
                    logger.warning("Couldn't find traffic used!");
                }
                String expire = br.getRegex(">Premium aktiv bis:</div><div[^>]+>(\\d{2}\\.\\d{2}\\.\\d{4} \\d{2}:\\d{2}:\\d{2}) Uhr<").getMatch(0);
                if (expire != null) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy hh:mm:ss", Locale.ENGLISH));
                } else {
                    logger.warning("Couldn't find expire date!");
                }
            } else if (accountType != null && accountType.equals("Free")) {
                // free account not supported yet...
                account.setProperty("isPremium", false);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                // account type == not found or not supported?
                logger.warning("Can't determine account type.");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }

        }
        return ai;
    }

    public void apiLogin(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage("http://api.datei.to/", "info=jdLogin&Username=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()));

        if (br.containsHTML("free")) {
            logger.info("Free account found->Not support->Disable");
            account.setProperty("isPremium", false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (!br.containsHTML("premium") || br.containsHTML("false")) {
            // remove when api gets fixed!!
            useAPI.set(false);
            // account.setProperty("isPremium", false);
            // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public void webLogin(final Account account, final boolean force) throws Exception {
        this.setBrowserExclusive();
        try {
            /** Load cookies */
            br.setCookiesExclusive(true);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(this.getHost(), key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(false);
            br.postPage("http://datei.to/response/login", "Login_User=" + Encoding.urlEncode(account.getUser()) + "&Login_Pass=" + Encoding.urlEncode(account.getPass()));
            if (br.getCookie(this.getHost(), "User") == null || br.getCookie(this.getHost(), "Pass") == null) {
                logger.warning("Not a valid user:password");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            /** Save cookies */
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(this.getHost());
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        } catch (final PluginException e) {
            account.setProperty("cookies", Property.NULL);
            throw e;
        }
    }

    @Override
    public void handlePremium(DownloadLink downloadLink, Account account) throws Exception {
        if (useAPI.get() == true) {
            // api dl
            requestFileInformation(downloadLink);
            br.postPage("http://api.datei.to/", "info=jdPremDown&Username=" + Encoding.urlEncode(account.getUser()) + "&Password=" + Encoding.urlEncode(account.getPass()) + "&datei=" + new Regex(downloadLink.getDownloadURL(), "datei\\.to/datei/(.*?)\\.html").getMatch(0));
            String dlUrl = br.toString();
            if (dlUrl == null || !dlUrl.startsWith("http") || dlUrl.length() > 500 || dlUrl.contains("no file")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            dlUrl = dlUrl.trim();
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 0);
            br.setFollowRedirects(true);
            if (dl.getConnection() == null || dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                logger.severe("PremiumError: " + br.toString());
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            dl.startDownload();
        } else {
            // web dl
            requestFileInformation(downloadLink);
            webLogin(account, false);
            br.setFollowRedirects(false);
            // direct downloads
            br.getPage(downloadLink.getDownloadURL());
            String dllink = br.getRedirectLocation();
            if (dllink == null || !dllink.matches("(https?://\\w+\\.datei\\.to/file/[a-z0-9]{32}/[A-Za-z0-9]{8}/[A-Za-z0-9]{10}/[^\"\\']+)")) {
                // direct download failed to match or disabled feature in users profile
                String id = br.getRegex("<button id=\"([^\"]+)\">Download starten<").getMatch(0);
                if (id == null) {
                    logger.warning("'id' could not be found");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.postPage("/response/download", "Step=1&ID=" + id);
                dllink = br.getRegex("(https?://\\w+\\.datei\\.to/dl/[A-Za-z0-9]+)").getMatch(0);
                br.setFollowRedirects(true);
            }
            if (dllink == null) {
                logger.warning("Could not find dllink");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}