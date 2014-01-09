//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.UserIO;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "catshare.net" }, urls = { "http://(www\\.)?catshare\\.net/[A-Za-z0-9]{16}" }, flags = { 2 })
public class CatShareNet extends PluginForHost {

    private String        BRBEFORE = "";
    private String        HOSTER   = "http://catshare.net";
    private static Object LOCK     = new Object();

    // DEV NOTES
    // captchatype: recaptcha
    // non account: 1 * 1

    public CatShareNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(HOSTER + "/login");
    }

    public void checkErrors(DownloadLink theLink, boolean beforeRecaptcha) throws NumberFormatException, PluginException {
        // Some waittimes...
        if (beforeRecaptcha) {
            if (br.containsHTML("<h4 style=\"margin-right: 10px;\">Odczekaj <big id=\"counter\"></big> lub kup")) {
                // possible waiitime after last download
                String waitTime = br.getRegex("<script>[ \t\n\r\f]+var count = ([0-9]+);").getMatch(0);
                logger.warning("Waittime detected for link " + theLink.getDownloadURL());
                Long waitTimeSeconds = Long.parseLong(waitTime);
                if (waitTimeSeconds != 60l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Waittime detected", (waitTimeSeconds + 5) * 1000L); }

            }
        }

    }

    // never got one, but left this for future usage
    public void checkServerErrors() throws NumberFormatException, PluginException {
        if (new Regex(BRBEFORE, Pattern.compile("No file", Pattern.CASE_INSENSITIVE)).matches()) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
        if (new Regex(BRBEFORE, "(Not Found|<h1>(404 )?Not Found</h1>)").matches()) {
            logger.warning("Server says link offline, please recheck that!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public void doFree(DownloadLink downloadLink, boolean resumable, int maxChunks) throws Exception, PluginException {
        String passCode = null;
        checkErrors(downloadLink, true);

        String dllink = null;
        long timeBefore = System.currentTimeMillis();
        boolean password = false;
        boolean skipWaittime = false;

        // only ReCaptcha
        Form dlForm = new Form();
        if (new Regex(BRBEFORE, "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)").matches()) {
            dlForm = br.getForm(0);
            if (dlForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "no reCaptcha form!");

            logger.info("Detected captcha method \"Re Captcha\" for this host");
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
            rc.setForm(dlForm);
            String id = this.br.getRegex("\\?k=([A-Za-z0-9%_\\+\\- ]+)\"").getMatch(0);
            rc.setId(id);
            for (int i = 0; i < 5; i++) {
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, downloadLink);
                Form rcform = rc.getForm();
                rcform.put("recaptcha_challenge_field", rc.getChallenge());
                rcform.put("recaptcha_response_field", Encoding.urlEncode(c));
                logger.info("Put captchacode " + c + " obtained by captcha metod \"Re Captcha\" in the form and submitted it.");
                dlForm = rc.getForm();
                // waittime is often skippable for reCaptcha handling
                // skipWaittime = true;
                br.submitForm(dlForm);
                logger.info("Submitted DLForm");
                if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    rc.reload();
                    continue;
                }
                break;
            }

        } else {
            logger.warning("Unknown ReCaptcha method for: " + downloadLink.getDownloadURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown ReCaptcha method!");
        }

        /* Captcha END */
        // if (password) passCode = handlePassword(passCode, dlForm, downloadLink);
        if (br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
            logger.info("5 reCaptcha tryouts for <" + downloadLink.getDownloadURL() + "> were incorrect");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "reCaptcha error", 1 * 60 * 1000l);
        }

        doSomething();
        checkErrors(downloadLink, false);
        dllink = getDllink();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Regex didn't match!");
        }
        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    // Removed fake messages which can kill the plugin
    public void doSomething() throws NumberFormatException, PluginException {
        BRBEFORE = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        regexStuff.add("(display: none;\">.*?</div>)");
        regexStuff.add("(visibility:hidden>.*?<)");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            BRBEFORE = BRBEFORE.replace(fun, "");
        }
    }

    @Override
    public String getAGBLink() {
        return HOSTER + "/regulamin";
    }

    public String getDllink() {
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            dllink = new Regex(BRBEFORE, "Download: <a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = new Regex(BRBEFORE, "(https?://(\\w+\\.)?catshare\\.net/dl/(\\d+/){4}[^\"]+)").getMatch(0);
                if (dllink == null) {
                    dllink = new Regex(BRBEFORE, "(https?://\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/catshare/\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}/dl/(\\d+/){4}[^\"]+)").getMatch(0);
                }
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, false, 1);
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        final String downloadURL = link.getDownloadURL();
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadURL);
        doSomething();
        if (br.containsHTML("<title>Error 404</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String fileName = new Regex(BRBEFORE, "<h3 class=\"pull-left\" style=\"margin-left: 10px;\">(.*)</h3>[ \t\n\r\f]+<h3 class=\"pull-right\"").getMatch(0);

        String fileSize = new Regex(BRBEFORE, "<h3 class=\"pull-right\" style=\"margin-right: 10px;\">(.+?)</h3>").getMatch(0);

        if (fileName == null || fileSize == null) {
            logger.warning("For link: " + downloadURL + ", final filename or filesize is null!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "filename or filesize not found!");
        }

        link.setName(fileName.trim());
        link.setDownloadSize(SizeFormatter.getSize(fileSize));
        // setting this prevents from setting incorrect (shortened) filename from the request header
        link.setFinalFileName(link.getName());
        return AvailableStatus.TRUE;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            ai.setStatus("Login failed or not Premium");
            UserIO.getInstance().requestMessageDialog(0, "CatShare.net Premium Error", "Login failed or not Premium!\r\nPlease check your Username and Password!");
            account.setValid(false);
            return ai;
        }

        final String dailyLimitLeft = br.getRegex("<li><a href=\"/premium\">([^<>\"\\']+)</a></li>").getMatch(0);
        if (dailyLimitLeft != null) {
            ai.setTrafficMax(SizeFormatter.getSize("20 GB"));
            ai.setTrafficLeft(SizeFormatter.getSize(dailyLimitLeft));
        } else
            ai.setUnlimitedTraffic();

        String expire = br.getRegex(">Konto premium ważne do : <strong>(\\d{4}\\-\\d+{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})<").getMatch(0);
        if (expire == null) {
            expire = br.getRegex("(\\d{4}\\-\\d+{2}\\-\\d{2} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire == null) {
                // for the last day of the premium period
                if (br.containsHTML("Konto premium ważne do : <strong>-</strong></span>")) {
                    // 0 days left
                    expire = br.getRegex("<a href=\"/premium\">Konto:[ \t\n\r]+ Premium \\(<b>(\\d) dni</b>\\)+[ \t\n\r]+</a>").getMatch(0);
                }
                if (expire == null) {
                    ai.setExpired(true);
                    account.setValid(false);
                    return ai;
                }
            }
        }
        if (expire.equals("0") && (dailyLimitLeft != null)) {
            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            String dateNow = formatter.format(Calendar.getInstance().getTime());
            dateNow = dateNow + " 23:59:59";
            ai.setValidUntil(TimeFormatter.getMilliSeconds(dateNow, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        } else
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        account.setValid(true);
        try {
            account.setMaxSimultanDownloads(-1);
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            // not available in old Stable 0.9.581
        }

        ai.setStatus("Premium User");
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
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
                            this.br.setCookie("http://catshare.net", key, value);
                        }
                        return;
                    }
                }
                br.getPage("http://catshare.net/login");
                Form login = br.getForm(0);
                if (login == null) {
                    logger.warning("Couldn't find login form");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                login.put("user_email", Encoding.urlEncode(account.getUser()));
                login.put("user_password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                br.getPage("/");
                if (!br.containsHTML("Konto:[\r\t\n ]+Premium \\(<b>\\d+ dni</b>\\)")) {
                    logger.warning("Couldn't determine premium status or account is Free not Premium!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium Account is invalid: it's free or not recognized!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://catshare.net");
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
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        String passCode = null;
        requestFileInformation(downloadLink);
        login(account, false);
        br.getPage(downloadLink.getDownloadURL());
        doSomething();
        String dllink = getDllink();
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        logger.info("Final downloadlink = " + dllink + " starting the download...");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            doSomething();
            checkServerErrors();
            if (br.containsHTML("Twój dzienny limit transferu został przekroczony")) {
                UserIO.getInstance().requestMessageDialog(0, "CatShare.net Premium Error", "Daily Limit exceeded!" + "\r\nPremium disabled, will continue downloads as Free User");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }
}