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

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filejungle.com" }, urls = { "http://(www\\.)?filejungle\\.com/f/[A-Za-z0-9]+" }, flags = { 2 })
public class FileJungleCom extends PluginForHost {
    
    private static final String CAPTCHAFAILED       = "\"error\":\"incorrect\\-captcha\\-sol\"";
    private static final String MAINPAGE            = "http://filejungle.com/";
    private static Object       LOCK                = new Object();
    private static final String DLYOURFILESUSERTEXT = "You can only download files which YOU uploaded!";
    private static final String DLYOURFILESTEXT     = "(>You can only retrieve files from FileJungle after logging in to your file manager|>Only files you have uploaded personally can be retrieved)";
    
    public FileJungleCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://filejungle.com/premium.php");
    }
    
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            final Browser checkbr = new Browser();
            checkbr.getHeaders().put("Accept-Encoding", "identity");
            checkbr.setCustomCharset("utf-8");
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            final StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("urls=");
                links.clear();
                while (true) {
                    /*
                     * we test 100 links at once - its tested with 500 links, probably we could test even more at the same time...
                     */
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                int c = 0;
                for (final DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report anything else
                     */
                    if (c > 0) {
                        sb.append("%0D%0A");
                    }
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                checkbr.postPage("http://www.filejungle.com/check_links.php", sb.toString());
                for (final DownloadLink dl : links) {
                    if (br.toString().length() < 50) {
                        dl.setAvailable(false);
                        continue;
                    }
                    final String linkpart = new Regex(dl.getDownloadURL(), "(filejungle\\.com/f/.+)").getMatch(0);
                    if (linkpart == null) {
                        logger.warning("Filejungle availablecheck is broken!");
                        return false;
                    }
                    final String regexForThisLink = "(http://(www\\.)?" + linkpart + "</a></div>[\t\n\r ]+<div class=\"col2\">.*?</div>[\t\n\r ]+<div class=\"col3\">.*?</div>[\t\n\r ]+<div class=\"col4\"><span class=\"icon (approved|declined))";
                    final String theData = checkbr.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) {
                        logger.warning("Filejungle availablecheck is broken!");
                        return false;
                    }
                    final Regex linkinformation = new Regex(theData, ".*?</a></div>[\t\n\r ]+<div class=\"col2\">(.*?)</div>[\t\n\r ]+<div class=\"col3\">(.*?)</div>[\t\n\r ]+<div class=\"col4\"><span class=\"icon (approved|declined)");
                    final String status = linkinformation.getMatch(2);
                    String filename = linkinformation.getMatch(0);
                    String filesize = linkinformation.getMatch(1);
                    if (filename == null || filesize == null) {
                        logger.warning("Filejungle availablecheck is broken!");
                        dl.setAvailable(false);
                    } else if (!status.equals("approved") || filename.equals("--") || filesize.equals("--")) {
                        filename = linkpart;
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    dl.getLinkStatus().setStatusText(DLYOURFILESUSERTEXT);
                    dl.setName(filename);
                    if (filesize != null) {
                        if (filesize.contains(",") && filesize.contains(".")) {
                            /* workaround for 1.000,00 MB bug */
                            filesize = filesize.replaceFirst("\\.", "");
                        }
                        dl.setDownloadSize(SizeFormatter.getSize(filesize));
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            return false;
        }
        return true;
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
        br.getPage("http://filejungle.com/dashboard.php");
        ai.setUnlimitedTraffic();
        String validUntil = br.getRegex("\"/extend_premium\\.php\">Until (\\d+ [A-Za-z]+ \\d+)<br").getMatch(0);
        if (validUntil != null) ai.setValidUntil(TimeFormatter.getMilliSeconds(validUntil, "dd MMMM yyyy", null));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }
    
    @Override
    public String getAGBLink() {
        return "http://www.filejungle.com/terms_and_conditions.php";
    }
    
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }
    
    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }
    
    private void handleErrors() throws PluginException {
        final String theURL = br.getURL();
        if (theURL.contains("filejungle.com/landing-1703")) {
            logger.warning("Found unknown error, landing page: " + theURL);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Landing error 1703 found, please check if you can download via browser!");
        }
        String landing = new Regex(theURL, ".*?filejungle\\.com/landing\\-(\\d+)").getMatch(0);
        if (landing != null) {
            logger.warning("Found unknown error, landing page: " + theURL);
            throw new PluginException(LinkStatus.ERROR_FATAL, "Unknown landing error " + landing + "found, please contact our support!");
        }
    }
    
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        if (true) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
        final String damnLanding = br.getRegex("\"(/landing/L\\d+/download_captcha\\.js\\?\\d+)\"").getMatch(0);
        final String id = br.getRegex("var reCAPTCHA_publickey=\\'([^\\'\"<>]+)\\'").getMatch(0);
        final String rcShortenCode = br.getRegex("id=\"recaptcha_shortencode_field\" name=\"recaptcha_shortencode_field\" value=\"([^\\'\"<>]+)\"").getMatch(0);
        if (id == null || damnLanding == null || rcShortenCode == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.filejungle.com" + damnLanding);
        Browser br2 = br.cloneBrowser();
        br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br2.setCustomCharset("utf-8");
        final String postlink = downloadLink.getDownloadURL() + "/" + downloadLink.getName();
        br2.postPage(postlink, "checkDownload=check");
        if (br2.containsHTML("\"captchaFail\"")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many wrong captcha attempts!", 10 * 60 * 1000l);
        if (br2.containsHTML("\"fail\":\"timeLimit\"")) {
            br.postPage(postlink, "checkDownload=showError&errorType=timeLimit");
            String reconnectWait = br.getRegex("Please wait for (\\d+) seconds to download the next file").getMatch(0);
            int wait = 600;
            if (reconnectWait != null) wait = Integer.parseInt(reconnectWait);
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, wait * 1001l);
        }
        if (!br2.containsHTML("\"success\":\"showCaptcha\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br2);
        Form dlform = new Form();
        dlform.setMethod(MethodType.POST);
        dlform.setAction("http://www.filejungle.com/checkReCaptcha.php");
        dlform.put("recaptcha_shortencode_field", rcShortenCode);
        rc.setForm(dlform);
        rc.setId(id);
        rc.load();
        for (int i = 0; i <= 5; i++) {
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, downloadLink);
            rc.setCode(c);
            if (br2.containsHTML(CAPTCHAFAILED)) {
                rc.reload();
                continue;
            }
            break;
        }
        if (br2.containsHTML(CAPTCHAFAILED)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (!br2.containsHTML("\"success\":1")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br2.postPage(postlink, "downloadLink=wait");
        int wait = 30;
        String waittime = br2.getRegex("\"waitTime\":(\\d+),\"").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1001l, downloadLink);
        br2.postPage(postlink, "downloadLink=show");
        // Use normal browser here
        br.postPage(postlink, "download=normal");
        if (br.containsHTML("(>File is not available<|>The page you requested cannot be displayed right now|The file may have removed by the uploader or expired\\.<)")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        String dllink = br.getRedirectLocation();
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            logger.warning("The final dllink seems not to be a file!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
    
    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML(DLYOURFILESTEXT)) throw new PluginException(LinkStatus.ERROR_FATAL, DLYOURFILESUSERTEXT);
        // There is also another way to download (not via direct redirect) but
        // it didn't work when i changed it in the accountsettings
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            /* no direct download enabled */
            logger.warning("Indirect");
            Form form = new Form();
            form.setMethod(MethodType.POST);
            form.setAction(link.getDownloadURL());
            form.put("download", "premium");
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, form, true, 0);
        } else {
            logger.warning("Direct");
            br.setFollowRedirects(true);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        }
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            handleErrors();
            logger.warning("The final dllink seems not to be a file!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }
    
    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }
    
    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }
    
    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            // Load cookies
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
                        this.br.setCookie(MAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.postPage("http://filejungle.com/login.php", "loginUserName=" + Encoding.urlEncode(account.getUser()) + "&loginUserPassword=" + Encoding.urlEncode(account.getPass()) + "&recaptcha_response_field=&recaptcha_challenge_field=&recaptcha_shortencode_field=&autoLogin=on&loginFormSubmit=Login");
            if (br.getCookie(MAINPAGE, "cookie") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = this.br.getCookies(MAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
        }
    }
    
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        return getAvailableStatus(link);
    }
    
    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) return (AvailableStatus) ret;
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
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