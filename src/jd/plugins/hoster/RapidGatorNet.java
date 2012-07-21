//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.LnkCrptWs;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidgator.net" }, urls = { "http://(www\\.)?rapidgator\\.net/file/\\d+(/[^/<>]+\\.html)?" }, flags = { 2 })
public class RapidGatorNet extends PluginForHost {

    public RapidGatorNet(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://rapidgator.net/article/premium");
    }

    private static final String  MAINPAGE   = "http://rapidgator.net/";
    private static final Object  LOCK       = new Object();
    private static String        agent      = null;
    private static boolean       hasDled    = false;
    private static long          timeBefore = 0;
    private static String        lastIP     = null;
    static private final Pattern IPREGEX    = Pattern.compile("(([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9])\\.([1-2])?([0-9])?([0-9]))", Pattern.CASE_INSENSITIVE);

    private static String[]      IPCHECK    = new String[] { "http://ipcheck0.jdownloader.org", "http://ipcheck1.jdownloader.org", "http://ipcheck2.jdownloader.org", "http://ipcheck3.jdownloader.org" };

    @Override
    public String getAGBLink() {
        return "http://rapidgator.net/article/terms";
    }

    public static Browser prepareBrowser(Browser prepBr) {
        if (prepBr == null) return prepBr;
        if (agent == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.setRequestIntervalLimit("http://rapidgator.net/", 319 * (int) Math.round(Math.random() * 3 + Math.random() * 3));
        prepBr.getHeaders().put("User-Agent", agent);
        prepBr.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.3");
        prepBr.getHeaders().put("Accept-Language", "en-US,en;q=0.8");
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setCookie("http://rapidgator.net/", "lang", "en");
        prepBr.setCustomCharset("UTF-8");
        try {
            prepBr.setConnectTimeout(1 * 60 * 1000);
        } catch (final Throwable e) {
        }
        try {
            prepBr.setReadTimeout(3 * 60 * 1000);
        } catch (final Throwable e) {
        }
        prepBr.setConnectTimeout(3 * 60 * 1000);
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        prepareBrowser(br);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("400 Bad Request") && link.getDownloadURL().contains("%")) {
            link.setUrlDownload(link.getDownloadURL().replace("%", ""));
            br.getPage(link.getDownloadURL());
        }
        if (br.containsHTML("File not found")) {
            String filenameFromURL = new Regex(link.getDownloadURL(), ".+/(.+)\\.html").getMatch(0);
            if (filenameFromURL != null) link.setName(filenameFromURL);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String freedlsizelimit = br.getRegex("\\'You can download files up to ([\\d\\.]+ ?(MB|GB)) in free mode<").getMatch(0);
        if (freedlsizelimit != null) link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.rapidgatornet.only4premium", "This file is restricted to Premium users only"));
        String filename = br.getRegex("Downloading:[\t\n\r ]+</strong>([^<>\"]+)</p>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>Download file ([^<>\"]+)</title>").getMatch(0);
        String filesize = br.getRegex("File size:[\t\n\r ]+<strong>([^<>\"]+)</strong>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        br.setFollowRedirects(false);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        // experimental code - raz
        // so called 15mins between your last download, ends up with your IP blocked for the day.. Trail and error until we find the sweet
        // spot.
        String currentIP = getIP();
        logger.info("New Download: currentIP = " + currentIP);
        if (hasDled && ipChanged(currentIP) == false) {
            long result = System.currentTimeMillis() - timeBefore;
            // 35 minute wait less time since last download.
            logger.info("Wait time between downloads to prevent your IP from been blocked for 1 Day!");
            if (result > 0) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Wait time between download session", 2100000 - result);
        }
        try {
            // far as I can tell it's not needed.
            requestFileInformation(downloadLink);
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            for (String link : sitelinks) {
                if (link.matches("(.+\\.(js|css))")) {
                    Browser br2 = br.cloneBrowser();
                    simulateBrowser(br2, link);
                }
            }
            // end of experiment
            if (br.containsHTML("You have reached your daily downloads limit. Please try")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have reached your daily downloads limit", 60 * 60 * 1000l);
            if (br.containsHTML("(You can`t download not more than 1 file at a time in free mode\\.<|>Wish to remove the restrictions\\?)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You can`t download more than 1 file at a periode of time in free mode", 60 * 60 * 1000l);
            final String freedlsizelimit = br.getRegex("\\'You can download files up to ([\\d\\.]+ ?(MB|GB)) in free mode<").getMatch(0);
            if (freedlsizelimit != null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.rapidgatornet.only4premium", "No free download link for this file"));
            final String reconnectWait = br.getRegex("Delay between downloads must be not less than (\\d+) min\\.<br>Don`t want to wait\\? <a style=\"").getMatch(0);
            if (reconnectWait != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, (Integer.parseInt(reconnectWait) + 1) * 60 * 1000l);
            int wait = 30;
            final String waittime = br.getRegex("var secs = (\\d+);").getMatch(0);
            if (waittime != null) wait = Integer.parseInt(waittime);
            String fid = new Regex(downloadLink.getDownloadURL(), "rapidgator\\.net/file/(\\d+)").getMatch(0);
            Browser br2 = br.cloneBrowser();
            prepareBrowser(br2);
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
            if (fid != null)
                br2.getPage("http://rapidgator.net/download/AjaxStartTimer?fid=" + fid);
            else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String sid = br2.getRegex("sid\":\"([a-zA-Z0-9]{32})").getMatch(0);
            String state = br2.getRegex("state\":\"([^\"]+)").getMatch(0);
            if (!"started".equalsIgnoreCase(state)) {
                logger.info(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (sid == null) {
                logger.info(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            sleep(wait * 1001l, downloadLink);
            /* needed so we have correct referrer ;) (back to original br) */
            br2 = br.cloneBrowser();
            prepareBrowser(br2);
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.getPage("http://rapidgator.net/download/AjaxGetDownloadLink?sid=" + sid);
            state = br2.getRegex("state\":\"(.*?)\"").getMatch(0);
            if (!"done".equalsIgnoreCase(state)) {
                // {"state":"error","code":"You didn`t wait specified time. Try again or contact to administrator","0":"step3"}
                logger.info(br2.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            URLConnectionAdapter con1 = br.openGetConnection("http://rapidgator.net/download/captcha");
            if (con1.getResponseCode() == 500) {
                try {
                    con1.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many simultan downloads", 10 * 60 * 1000l);
            }
            if (con1.getResponseCode() == 302) {
                try {
                    con1.disconnect();
                } catch (final Throwable e) {
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "ServerIssue", 2 * 60 * 1000l);
            }
            // wasn't needed for raz, but psp said something about a redirect)
            br.followConnection();
            if (br.containsHTML("(api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                for (int i = 0; i <= 5; i++) {
                    rc.parse();
                    rc.load();
                    File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    String c = getCaptchaCode(cf, downloadLink);
                    rc.getForm().put("DownloadCaptchaForm%5Bcaptcha%5D", "");
                    rc.setCode(c);
                    if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/)")) continue;
                    break;
                }
            } else {
                Form captcha = br.getFormbyProperty("id", "captchaform");
                if (captcha == null) {
                    logger.info(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }

                captcha.put("DownloadCaptchaForm[captcha]", "");
                String code = null, challenge = null;
                Browser capt = br.cloneBrowser();

                if (br.containsHTML("//api\\.solvemedia\\.com/papi")) {
                    PluginForDecrypt solveplug = JDUtilities.getPluginForDecrypt("linkcrypt.ws");
                    jd.plugins.decrypter.LnkCrptWs.SolveMedia sm = ((LnkCrptWs) solveplug).getSolveMedia(br);
                    File cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    code = getCaptchaCode(cf, downloadLink);
                    String chid = sm.verify(code);
                    captcha.put("adcopy_challenge", chid);
                    captcha.put("adcopy_response", "manual_challenge");

                } else if (br.containsHTML("//api\\.adscapchta\\.com/")) {
                    String captchaAdress = captcha.getRegex("<iframe src=\'(http://api\\.adscaptcha\\.com/NoScript\\.aspx\\?CaptchaId=\\d+\\&PublicKey=[^\'<>]+)").getMatch(0);
                    String captchaType = new Regex(captchaAdress, "CaptchaId=(\\d+)\\&").getMatch(0);
                    if (captchaAdress == null || captchaType == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

                    if (!"3017".equals(captchaType)) {
                        logger.warning("ADSCaptcha: Captcha type not supported!");
                        return;
                    }
                    capt.getPage(captchaAdress);
                    challenge = capt.getRegex("<img src=\"(http://api\\.adscaptcha\\.com//Challenge\\.aspx\\?cid=[^\"]+)").getMatch(0);
                    code = capt.getRegex("class=\"code\">([0-9a-f\\-]+)<").getMatch(0);

                    if (challenge == null || code == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    challenge = getCaptchaCode(challenge, downloadLink);
                    captcha.put("adscaptcha_response_field", challenge);
                    captcha.put("adscaptcha_challenge_field", code);
                } else {
                    logger.info(br.toString());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.submitForm(captcha);
            }
            if (br.containsHTML("(>Please fix the following input errors|>The verification code is incorrect|api\\.recaptcha\\.net/|google\\.com/recaptcha/api/|//api\\.solvemedia\\.com/papi|//api\\.adscaptcha\\.com)")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            String dllink = br.getRegex("location\\.href = \\'(http://.*?)\\'").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\\'(http://pr_srv\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)\\'").getMatch(0);
            if (dllink == null) {
                logger.info(br.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("<div class=\"error\">[\r\n ]+Error\\. Link expired. You have reached your daily limit of downloads\\."))
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Link expired, or You've reached your daily limit ", 60 * 60 * 1000l);
                else if (br.containsHTML("<div class=\"error\">[\r\n ]+File is already downloading</div>"))
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download session in progress", 20 * 60 * 1000l);
                else
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
            hasDled = true;
        } catch (Exception e) {
            hasDled = false;
            throw e;
        } finally {
            timeBefore = System.currentTimeMillis();
            setIP(currentIP);
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setUnlimitedTraffic();
        String expire = br.getRegex("Premium till (\\d{4}\\-\\d{2}\\-\\d{2})").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd", null));
        }
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepareBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("http://rapidgator.net/auth/login", "LoginForm%5Bemail%5D=" + Encoding.urlEncode(account.getUser()) + "&LoginForm%5Bpassword%5D=" + Encoding.urlEncode(account.getPass()) + "&LoginForm%5BrememberMe%5D=1");
                if (br.getCookie(MAINPAGE, "user__") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        prepareBrowser(br);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String dllink = br.getRegex("var premium_download_link = \\'(http://[^<>\"\\']+)\\';").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\\'(http://pr_srv\\.rapidgator\\.net//\\?r=download/index\\&session_id=[A-Za-z0-9]+)\\'").getMatch(0);
        if (dllink == null) {
            if (br.containsHTML("You have reached daily quota of downloaded information for premium accounts")) {
                logger.info("You have reached daily quota of downloaded information for premium accounts");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            if (br.containsHTML("<h2>Error 500</h2>[\r\n ]+<div class=\"error\">"))
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster is issues", 60 * 60 * 1000l);
            else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void simulateBrowser(Browser rb, String url) {
        if (rb == null || url == null) return;
        URLConnectionAdapter con = null;
        try {
            con = rb.openGetConnection(url);
        } catch (final Throwable e) {
        } finally {
            con.disconnect();
        }
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
                    if (currentIP != null) break;
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

    private boolean ipChanged(String IP) throws PluginException {
        String currentIP = null;
        if (IP != null && new Regex(IP, IPREGEX).matches()) {
            currentIP = IP;
        } else {
            currentIP = getIP();
        }
        if (currentIP == null) return false;
        return !currentIP.equals(lastIP);
    }

    private boolean setIP(String IP) throws PluginException {
        synchronized (IPCHECK) {
            if (IP != null && !new Regex(IP, IPREGEX).matches()) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (ipChanged(IP) == false) {
                // Static IP or failure to reconnect! We don't change lastIP
                logger.warning("Your IP hasn't changed since last download");
                return false;
            } else {
                lastIP = IP;
                logger.info("LastIP = " + lastIP);
                return true;
            }
        }
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

}