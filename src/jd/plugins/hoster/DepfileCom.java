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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "depfile.com" }, urls = { "https?://(www\\.)?d[ei]pfile\\.com/(downloads/i/\\d+/f/.+|[a-zA-Z0-9]+)" })
public class DepfileCom extends PluginForHost {

    private static final String            CAPTCHATEXT                  = "includes/vvc\\.php\\?vvcid=";
    private static final String            MAINPAGE                     = "https://depfile.com/";
    private static Object                  LOCK                         = new Object();
    private static final String            ONLY4PREMIUM                 = ">Owner of the file is restricted to download this file only Premium users|>File is available only for Premium users.<";
    private static final String            ONLY4PREMIUMUSERTEXT         = "Only downloadable for premium users";

    private static final long              FREE_RECONNECTWAIT           = 1 * 60 * 60 * 1001L;
    private String                         PROPERTY_LASTIP              = "DEPFILECOM_PROPERTY_LASTIP";
    private static final String            PROPERTY_LASTDOWNLOAD        = "depfilecom_lastdownload_timestamp";
    private final String                   ACTIVATEACCOUNTERRORHANDLING = "ACTIVATEACCOUNTERRORHANDLING";
    private final String                   EXPERIMENTALHANDLING         = "EXPERIMENTALHANDLING";
    private Pattern                        IPREGEX                      = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);
    private static AtomicReference<String> lastIP                       = new AtomicReference<String>();
    private static AtomicReference<String> currentIP                    = new AtomicReference<String>();
    private static HashMap<String, Long>   blockedIPsMap                = new HashMap<String, Long>();
    private static Object                  CTRLLOCK                     = new Object();
    private static String[]                IPCHECK                      = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };

    public DepfileCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(MAINPAGE + "premium");
        // Would be needed if multiple free-downloads were possible
        // this.setStartIntervall(11 * 1000l);
    }

    @Override
    public String rewriteHost(String host) {
        if (host == null || "i-filez.com".equals(host) || "depfile.com".equals(host) || "dipfile.com".equals(host)) {
            return "depfile.com";
        }
        return super.rewriteHost(host);
    }

    @Override
    public String getAGBLink() {
        return "http://dipfile.com/terms";
    }

    public String correctDownloadLink(final String parameter) {
        final String result = parameter.replaceFirst("(?:i-filez|dipfile)\\.com/", "depfile.com/").replace("http://", "https://");
        return result;
    }

    public void correctDownloadLink(DownloadLink link) {
        // Links come from a decrypter
        link.setUrlDownload(correctDownloadLink(link.getPluginPatternMatcher()));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        // Set English language
        br.setCookie(MAINPAGE, "sdlanguageid", "2");
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        final DepfileConfigInterface cfg = PluginJsonConfig.get(jd.plugins.hoster.DepfileCom.DepfileConfigInterface.class);
        if (isOfflineHTML() && this.br.containsHTML("RESTORE ACCESS TO THE FILE") && cfg.isEnableDMCADownload()) {
            return AvailableStatus.UNCHECKABLE;
        } else if (isOfflineURL()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = new Regex(link.getDownloadURL(), "/downloads/i/\\d+/f/(.+)").getMatch(0);
        if (!link.getDownloadURL().matches(".+/downloads/i/\\d+/f/.+")) {
            filename = br.getRegex("<th>File name:</th>[\t\n\r ]+<td>([^<>\"]*?)</td>").getMatch(0);
        }
        String filesize = br.getRegex("<th>Size:</th>[\r\t\n ]+<td>(.*?)</td>").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim().replace(".html", "")));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        String md5 = br.getRegex("<th>MD5:</th>[\r\t\n ]+<td>([a-f0-9]{32})</td>").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        if (br.containsHTML(ONLY4PREMIUM)) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ifilezcom.only4premium", ONLY4PREMIUMUSERTEXT));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        if (br.containsHTML(ONLY4PREMIUM)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, JDL.L("plugins.hoster.ifilezcom.only4premium", ONLY4PREMIUMUSERTEXT), PluginException.VALUE_ID_PREMIUM_ONLY);
        }

        this.getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, Property.NULL);
        currentIP.set(this.getIP());
        synchronized (CTRLLOCK) {
            /* Load list of saved IPs + timestamp of last download */
            final Object lastdownloadmap = this.getPluginConfig().getProperty(PROPERTY_LASTDOWNLOAD);
            if (lastdownloadmap != null && lastdownloadmap instanceof HashMap && blockedIPsMap.isEmpty()) {
                blockedIPsMap = (HashMap<String, Long>) lastdownloadmap;
            }
        }

        /* 2017-03-25: It is not possible to re-use generated direct URLs --> So we don't even try it! */

        /**
         * Experimental reconnect handling to prevent having to enter a captcha just to see that a limit has been reached!
         */
        logger.info("New Download: currentIP = " + currentIP.get());
        if (PluginJsonConfig.get(jd.plugins.hoster.DepfileCom.DepfileConfigInterface.class).isEnableReconnectWorkaround()) {
            long lastdownload = 0;
            long passedTimeSinceLastDl = 0;
            /*
             * If the user starts a download in free (unregistered) mode the waittime is on his IP. This also affects free accounts if he
             * tries to start more downloads via free accounts afterwards BUT nontheless the limit is only on his IP so he CAN download
             * using the same free accounts after performing a reconnect!
             */
            lastdownload = getPluginSavedLastDownloadTimestamp();
            passedTimeSinceLastDl = System.currentTimeMillis() - lastdownload;
            if (passedTimeSinceLastDl < FREE_RECONNECTWAIT) {
                logger.info("Experimental handling active --> There still seems to be a waittime on the current IP --> ERROR_IP_BLOCKED");
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, FREE_RECONNECTWAIT - passedTimeSinceLastDl);
            }
        }

        String verifycode = br.getRegex("name='vvcid\' value=\'(\\d+)\'").getMatch(0);
        if (verifycode == null) {
            verifycode = br.getRegex("\\?vvcid=(\\d+)").getMatch(0);
        }
        if (!br.containsHTML(CAPTCHATEXT) || verifycode == null) {
            logger.warning("Captchatext not found or verifycode null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String code = getCaptchaCode("/includes/vvc.php?vvcid=" + verifycode, downloadLink);
        br.postPage(br.getURL(), "vvcid=" + verifycode + "&verifycode=" + code + "&FREE=Download+for+free");
        if (br.getURL().endsWith("/premium")) {
            // no reason why
            // jdlog://2883963166931/ = maybe hit some known session limit?? I could not reproduce this myself -raztoki
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "unknown limit reached", 30 * 60 * 1000l);
        }
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
        /* <p class='notice'>Download limit for free user.</p> */
        if (br.containsHTML(">Download limit for free user")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
        }
        if (br.containsHTML(">Free users can download up to \\d+G per day.")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily download limit reached", 4 * 60 * 60 * 1000l);
        }
        if (br.containsHTML(CAPTCHATEXT) || br.containsHTML(">The image code you entered is incorrect\\!<")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        br.setFollowRedirects(false);
        final String dllink = getDllink();
        int wait = 60;
        final String regexedWaittime = br.getRegex("var sec=(\\d+);").getMatch(0);
        if (regexedWaittime != null) {
            wait = Integer.parseInt(regexedWaittime);
        }
        sleep(wait * 1001l, downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        /*
         * The download attempt triggers reconnect waittime (if the user stops downloads before this step, no limit will be set!)! Save
         * timestamp here to calculate correct remaining waittime later!
         */
        synchronized (CTRLLOCK) {
            blockedIPsMap.put(currentIP.get(), System.currentTimeMillis());
            getPluginConfig().setProperty(PROPERTY_LASTDOWNLOAD, blockedIPsMap);
            setIP(downloadLink);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getDllink() throws PluginException {
        String dllink = br.getRegex("document\\.getElementById\\(\"wait_input\"\\)\\.value= unescape\\('(.*?)'\\);").getMatch(0);
        if (dllink != null) {
            dllink = Encoding.deepHtmlDecode(dllink).trim();
            return dllink;
        }
        // base64,
        dllink = Encoding.Base64Decode(br.getRegex("('|\")(aHR0[a-zA-Z0-9_/\\+\\=\\-%]+)\\1").getMatch(1));
        if (dllink == null) {
            logger.warning("dllink is null...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return dllink;
    }

    private AccountInfo login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                br.setCustomCharset("utf-8");
                // Set English language
                br.setCookie(MAINPAGE, "sdlanguageid", "2");
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return null;
                }
                br.setFollowRedirects(true);
                br.postPage(MAINPAGE, "login=login&loginemail=" + Encoding.urlEncode(account.getUser()) + "&loginpassword=" + Encoding.urlEncode(account.getPass()) + "&submit=login&rememberme=on");
                /*
                 * they set language based on account profile, so it can be wrong post login. Language not English? Change setting and go
                 * on!
                 */
                if (!"2".equals(br.getCookie(MAINPAGE, "sdlanguageid"))) {
                    br.setCookie(MAINPAGE, "sdlanguageid", "2");
                    br.getPage("/");
                }

                if (br.getCookie(MAINPAGE, "sduserid") == null || br.getCookie(MAINPAGE, "sdpassword") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // pre-determiner, doesn't VALIDATE expire time!
                if (isAccountPremium() || isAccountAffiliate()) {
                    account.setType(AccountType.PREMIUM);
                } else {
                    account.setType(AccountType.FREE);
                }
                final AccountInfo ai = new AccountInfo();
                if (AccountType.FREE.equals(account.getType())) {
                    // free accounts can still have captcha.
                    account.setMaxSimultanDownloads(1);
                    account.setConcurrentUsePossible(false);
                    ai.setStatus("Free Account");
                } else {
                    String expire = br.getRegex("href='/myspace/space/premium'>(\\d{2}[\\.\\-]\\d{2}[\\.\\-]\\d{2} \\d{2}:\\d{2})<").getMatch(0);
                    // only premium accounts expire, affiliate doesn't.. they are a form of premium according to admin.
                    if (expire == null && isAccountPremium()) {
                        ai.setExpired(true);
                        account.setValid(false);
                        return ai;
                    } else if (expire != null && isAccountPremium()) {
                        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, (expire.contains(".") ? "dd.MM.yy hh:mm" : "MM-dd-yy hh:mm"), null), br);
                        ai.setStatus("Premium Account");
                    } else if (expire == null && isAccountAffiliate()) {
                        ai.setStatus("Affiliate Account");
                    }
                    /* Max 20 downloads * each 1 connection in total */
                    account.setMaxSimultanDownloads(-1);
                    account.setConcurrentUsePossible(true);
                }
                account.setValid(true);
                setAccountTrafficLimits(account, ai);
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return ai;
            } catch (PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private void checkForTwoFactor() throws PluginException {
        // jdlog://4112035891641/ jdlog://8112035891641/
        if (br.containsHTML("<h1>Protect your account with Both your password and your phone\\.\\s*<br>2-Step Verification</h1>") && br.containsHTML("<p class=\"intro\">Extra protection for your account\\.\\s*\\(TOTP\\) - Time-based One-Time Password\\.</p>")) {
            throw new PluginException(PluginException.VALUE_ID_PREMIUM_DISABLE, "2 Factor Authentication setup required!");
        }
    }

    private boolean isAccountAffiliate() {
        return br.containsHTML("/myspace/space/income");
    }

    private boolean isAccountPremium() {
        // they expire accounts slightly before time.... Link; 3654971887641.log; 398264;
        // jdlog://3654971887641
        return br.containsHTML("/myspace/space/premium") && !br.containsHTML("src='/images/i_premium_end\\.png'");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            return login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        br = new Browser();
        requestFileInformation(downloadLink);
        login(account, false);
        if (AccountType.FREE.equals(account.getType())) {
            br.getPage(downloadLink.getDownloadURL());
            checkForTwoFactor();
            doFree(downloadLink);
        } else {
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            checkForTwoFactor();
            checkForStupidFileDownloadQuotaAndSetQuota(account);
            {
                // they now have captcha for premium users, nice hey?
                int count = -1;
                String verifycode = getVerifyCode();
                if (verifycode != null) {
                    do {
                        if (++count > 5) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        final String code = getCaptchaCode("/includes/vvc.php?vvcid=" + verifycode, downloadLink);
                        if (StringUtils.isEmpty(code)) {
                            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                        }
                        br.postPage(br.getURL(), "vvcid=" + verifycode + "&verifycode=" + code + "&prem_plus=Next");
                    } while ((verifycode = getVerifyCode()) != null);
                    // can be another output after captcha... Link; 7944971887641.log; 4880971; jdlog://7944971887641
                    checkForStupidFileDownloadQuotaAndSetQuota(account);
                }
            }
            final String dllink = getPremiumDllink(br);
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads", 3 * 60 * 1000l);
            }
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private String getPremiumDllink(Browser br) {
        String dllink = br.getRegex("<th>A link for 24 hours:</th>[\t\n\r ]+<td><input type=\"text\" readonly=\"readonly\" class=\"text_field width100\" onclick=\"this\\.select\\(\\);\" value=\"(https?://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("(\"|')(https?://[a-z0-9]+\\.d[ei]pfile\\.com/premdw/\\d+/[a-z0-9]+/.*?)\\1").getMatch(1);
            if (dllink == null) {
                // Link; 4855091887641.log; 271792; jdlog://4855091887641
                dllink = br.getRegex("<th>Download:</th>\\s*<td><a href=('|\")(http.*?)\\1").getMatch(1);
            }
        }
        return dllink;
    }

    private void checkForStupidFileDownloadQuotaAndSetQuota(final Account account) throws PluginException {
        synchronized (LOCK) {
            final AccountInfo ai = account.getAccountInfo();
            if (br.containsHTML("class='notice'>You spent limit on urls/files per \\d+ hours|class='notice'>Sorry, you spent downloads limit on urls/files per \\d+ hours|class='notice'>You spent daily limit on \\d+Gb\\.</")) {
                logger.info("Daily limit reached, temp disabling premium");
                ai.setTrafficLeft(0);
                account.setAccountInfo(ai);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            setAccountTrafficLimits(account, ai);
        }
    }

    private void setAccountTrafficLimits(final Account account, final AccountInfo ai) {
        if (account.getType() == AccountType.PREMIUM) {
            /*
             * VERY very bad usability: Used- and max traffic is only shown when user accesses download URLs which are available - this is
             * the only way we can get these values!
             */
            final long traffic_max;
            final String traffic_used_str = this.br.getRegex("Used today\\s*?:\\s*?<b>([^<>\"\\']+)</b>").getMatch(0);
            String traffic_max_str = this.br.getRegex("Download traffic per day\\s*?:\\s*?<b>([^<>\"\\']+)</b>").getMatch(0);
            if (traffic_max_str == null) {
                /* 2017-03-24: Hardcoded fallback */
                traffic_max_str = "40GB";
            }
            traffic_max = SizeFormatter.getSize(traffic_max_str);
            if (traffic_used_str != null) {
                ai.setTrafficLeft(traffic_max - SizeFormatter.getSize(traffic_used_str));
            } else {
                /* Pretend as if we knew this. */
                ai.setTrafficLeft(traffic_max);
            }
            ai.setTrafficMax(traffic_max);
        } else {
            /* Free accounts have the same limits as guests (= no account) - nothing we can display here --> Unlimited! */
            ai.setUnlimitedTraffic();
        }
    }

    private long getPluginSavedLastDownloadTimestamp() {
        long lastdownload = 0;
        synchronized (blockedIPsMap) {
            if (blockedIPsMap != null) {
                final Iterator<Entry<String, Long>> it = blockedIPsMap.entrySet().iterator();
                while (it.hasNext()) {
                    final Entry<String, Long> ipentry = it.next();
                    final String ip = ipentry.getKey();
                    final long timestamp = ipentry.getValue();
                    if (System.currentTimeMillis() - timestamp >= FREE_RECONNECTWAIT) {
                        /* Remove old entries */
                        it.remove();
                    }
                    if (ip.equals(currentIP.get())) {
                        lastdownload = timestamp;
                    }
                }
            }
        }
        return lastdownload;
    }

    private String getIP() throws PluginException {
        Browser ip = new Browser();
        String currentIP = null;
        ArrayList<String> checkIP = new ArrayList<String>(Arrays.asList(IPCHECK));
        Collections.shuffle(checkIP);
        for (String ipServer : checkIP) {
            if (currentIP == null) {
                try {
                    ip.getPage(ipServer);
                    currentIP = ip.getRegex(IPREGEX).getMatch(0);
                    if (currentIP != null) {
                        break;
                    }
                } catch (Throwable e) {
                }
            }
        }
        if (currentIP == null) {
            logger.warning("firewall/antivirus/malware/peerblock software is most likely is restricting accesss to JDownloader IP checking services");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return currentIP;
    }

    @SuppressWarnings("deprecation")
    private boolean setIP(final DownloadLink link) throws PluginException {
        synchronized (IPCHECK) {
            if (currentIP.get() != null && !new Regex(currentIP.get(), IPREGEX).matches()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (ipChanged(link) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                String lastIP = currentIP.get();
                link.setProperty(PROPERTY_LASTIP, lastIP);
                DepfileCom.lastIP.set(lastIP);
                getPluginConfig().setProperty(PROPERTY_LASTIP, lastIP);
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
    }

    private boolean ipChanged(final DownloadLink link) throws PluginException {
        String currIP = null;
        if (currentIP.get() != null && new Regex(currentIP.get(), IPREGEX).matches()) {
            currIP = currentIP.get();
        } else {
            currIP = getIP();
        }
        if (currIP == null) {
            return false;
        }
        String lastIP = link.getStringProperty(PROPERTY_LASTIP, null);
        if (lastIP == null) {
            lastIP = DepfileCom.lastIP.get();
        }
        if (lastIP == null) {
            lastIP = this.getPluginConfig().getStringProperty(PROPERTY_LASTIP, null);
        }
        return !currIP.equals(lastIP);
    }

    private String getVerifyCode() {
        String verifyCode = br.getRegex("name='vvcid\' value=\'(\\d+)\'").getMatch(0);
        if (verifyCode == null) {
            verifyCode = br.getRegex("\\?vvcid=(\\d+)").getMatch(0);
        }
        return verifyCode;
    }

    private boolean isOfflineURL() {
        if (this.br._getURL().getPath().equals("/premium")) {
            return true;
        }
        return false;
    }

    private boolean isOfflineHTML() {
        final boolean offline;
        if (br.containsHTML("(>File was not found in the d[ei]pFile database\\.|It is possible that you provided wrong link\\.<|>Файл не найден в базе d[ei]pfile\\.com\\. Возможно Вы неправильно указали ссылку\\.<|The file was blocked by the copyright holder|>Page Not Found)")) {
            offline = true;
        } else if (br.containsHTML(">403 Forbidden</")) {
            /* Invalid links */
            offline = true;
        } else if (br.containsHTML(">The file was deleted by the owner\\.</")) {
            offline = true;
        } else {
            offline = false;
        }
        return offline;
    }

    public void handleErrors() throws PluginException {
        if (isOfflineHTML()) {
            logger.warning("File not found OR file removed from provider");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        }
        if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        }
        return false;
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return DepfileConfigInterface.class;
    }

    public static interface DepfileConfigInterface extends PluginConfigInterface {

        public static class TRANSLATION {

            public String getEnableDMCADownload_label() {
                return "Activate download of DMCA blocked links?\r\n-This function enabled uploaders to download their own links which have a 'legacy takedown' status till depfile irrevocably deletes them\r\nNote the following:\r\n-When activated, links which have the public status 'offline' will get an 'uncheckable' status instead\r\n--> If they're still downloadable, their filename- and size will be shown on downloadstart\r\n--> If they're really offline, the correct (offline) status will be shown on downloadstart";
            }

            public String getEnableReconnectWorkaround_label() {
                return "Activate reconnect workaround for freeusers: Prevents having to enter additional captchas in between downloads.";
            }

        }

        public static final TRANSLATION TRANSLATION = new TRANSLATION();

        @DefaultBooleanValue(true)
        @Order(8)
        boolean isEnableDMCADownload();

        void setEnableDMCADownload(boolean b);

        @DefaultBooleanValue(false)
        @Order(9)
        boolean isEnableReconnectWorkaround();

        void setEnableReconnectWorkaround(boolean b);

    }

}