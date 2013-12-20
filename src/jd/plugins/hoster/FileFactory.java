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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Scriptable;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "https?://(www\\.)?filefactory\\.com(/|//)(file/[\\w]+/?|(trafficshare|digitalsales)/[a-z0-9]{32}/.+/?)" }, flags = { 2 })
public class FileFactory extends PluginForHost {

    // DEV NOTES
    // other: currently they 302 redirect all non www. to www. which kills most of this plugin.
    // Adjust COOKIE_HOST to suite future changes, or remove COOKIE_HOST from that section of the script.

    private static AtomicInteger maxPrem            = new AtomicInteger(1);
    private final String         NO_SLOT            = ">All free download slots";
    private final String         NO_SLOT_USERTEXT   = "No free slots available";
    private final String         NOT_AVAILABLE      = "class=\"box error\"|have been deleted";
    private final String         SERVERFAIL         = "(<p>Your download slot has expired\\.|Unfortunately the file you have requested cannot be downloaded at this time|temporarily unavailable)";
    private final String         LOGIN_ERROR        = "The email or password you have entered is incorrect";
    private final String         SERVER_DOWN        = "server hosting the file you are requesting is currently down";
    private final String         CAPTCHALIMIT       = "<p>We have detected several recent attempts to bypass our free download restrictions originating from your IP Address";
    private static Object        LOCK               = new Object();
    private final String         COOKIE_HOST        = "http://www.filefactory.com";
    private String               dlUrl              = null;
    private final String         TRAFFICSHARELINK   = "filefactory.com/trafficshare/";
    private final String         TRAFFICSHARETEXT   = ">Download with FileFactory TrafficShare<";
    private final String         PASSWORDPROTECTED  = ">You are trying to access a password protected file|This File has been password protected by the uploader\\.";
    private final String         DBCONNECTIONFAILED = "Couldn't get valid connection to DB";

    public FileFactory(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/info/premium.php");
    }

    private static StringContainer agent = new StringContainer();

    public static class StringContainer {
        public String string = null;
    }

    /**
     * defines custom browser requirements.
     * */
    private Browser prepBrowser(final Browser prepBr) {
        if (agent.string == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setReadTimeout(3 * 60 * 1000);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        return prepBr;
    }

    public void checkErrors(final boolean premiumActive) throws PluginException {
        if (isPremiumOnly(br)) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to Premium Members");
        }
        if (br.getURL().contains("code=265")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The requested Download URL was invalid.  Please retry your download", 5 * 60 * 1000l); }
        if (br.getURL().contains("code=263")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Could not retrieve information about your download, or your download key has expired. Please try again. ", 5 * 60 * 1000l); }
        if (!premiumActive) {
            if (br.containsHTML(CAPTCHALIMIT)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            if (br.containsHTML(NO_SLOT) || br.getURL().contains("code=257")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NO_SLOT_USERTEXT, 10 * 60 * 1000l); }
            if (br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0) != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0)) * 60 * 1001l); }
        }
        if (br.containsHTML(SERVERFAIL)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l); }
        if (br.containsHTML(NOT_AVAILABLE)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        if (br.containsHTML(SERVER_DOWN)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l); }
        if (br.containsHTML(DBCONNECTIONFAILED)) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l); }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        final Browser br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("Accept-Encoding", "");
        // logic to grab account cookie to do fast linkchecking vs one at a time.
        boolean loggedIn = false;
        ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
        if (accounts != null && accounts.size() != 0) {
            Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    try {
                        login(n, false, br);
                        loggedIn = true;
                    } catch (Exception e) {
                    }
                    break;
                }
            }
        }
        if (!loggedIn) {
            // no account present or disabled account, we port back into requestFileInformation
            for (DownloadLink link : urls) {
                try {
                    requestFileInformation(link);
                } catch (Throwable e) {
                    return false;
                }
            }
            return true;
        }
        try {
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                br.getPage(COOKIE_HOST + "/account/tools/link-checker.php");
                links.clear();
                while (true) {
                    if (index == urls.length || links.size() > 100) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("links=");
                for (final DownloadLink dl : links) {
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    sb.append("%0D%0A");
                }
                // lets remove last "%0D%0A"
                sb.replace(sb.length() - 6, sb.length(), "");
                sb.append("&Submit=Check+Links");
                br.postPage(br.getURL(), sb.toString());
                for (final DownloadLink dl : links) {
                    dl.setName(new Regex(dl.getDownloadURL(), "filefactory\\.com/(.+)").getMatch(0));
                    if (br.getRedirectLocation() != null && (br.getRedirectLocation().endsWith("/member/setpwd.php") || br.getRedirectLocation().endsWith("/member/setdob.php"))) {
                        // password needs changing or dob needs setting.
                        dl.setAvailable(true);
                        continue;
                    }
                    String filter = br.getRegex("(<tr([^\n]+\n){4}[^\"]+\"" + dl.getDownloadURL() + "([^\n]+\n){4})").getMatch(0);
                    if (filter == null) dl.setAvailable(false);
                    String size = new Regex(filter, ">([\\d\\.]+ (KB|MB|GB|TB))<").getMatch(0);
                    String name = new Regex(filter, "<a href=\".*?/file/[a-z0-9]+/([^\"]+)").getMatch(0);
                    if (name != null) {
                        // Temporary workaround because they don't show full filenames (yet)
                        name = name.replace("_rar", ".rar");
                        name = name.replace("_zip", ".zip");
                        name = name.replace("_avi", ".avi");
                        name = name.replace("_mkv", ".mkv");
                        name = name.replace("_mp4", ".mp4");
                        dl.setName(name.trim());
                    }
                    if (size != null) dl.setDownloadSize(SizeFormatter.getSize(size));
                    if (filter != null && filter.contains(">Valid</abbr>"))
                        dl.setAvailable(true);
                    else
                        dl.setAvailable(false);
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
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.com//", ".com/"));
        link.setUrlDownload(link.getDownloadURL().replaceAll("://filefactory", "://www.filefactory"));
        // set trafficshare links like 'normal' links, this allows downloads to continue living if the uploader discontinues trafficshare
        // for that uid. Also re-format premium only links!
        if (link.getDownloadURL().contains(TRAFFICSHARELINK) || link.getDownloadURL().contains("/digitalsales/")) {
            String[][] uid = new Regex(link.getDownloadURL(), "(https?://.*?filefactory\\.com/)(trafficshare|digitalsales)/[a-z0-9]{32}/([^/]+)/?").getMatches();
            if (uid != null && (uid[0][0] != null || uid[0][2] != null)) {
                link.setUrlDownload(uid[0][0] + "file/" + uid[0][2]);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        // remove old account setter - delete after next stable update 20130911
        account.setProperty("freeAcc", Property.NULL);

        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        if (!isMail(account.getUser())) {
            ai.setStatus("Please enter your E-Mail adress as username!");
            account.setValid(false);
            return ai;
        }
        try {
            login(account, true, br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        if (!br.getURL().endsWith("/account/")) br.getPage("/account/");
        // <li class="tooltipster" title="Premium valid until: <strong>30th Jan, 2014</strong>">
        if (!br.containsHTML("title=\"(Premium valid until|Lifetime Member)")) {
            ai.setStatus("Registered (free) User");
            ai.setUnlimitedTraffic();
            account.setProperty("free", true);
            try {
                maxPrem.set(1);
                account.setMaxSimultanDownloads(1);
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
            }
        } else {
            account.setProperty("free", false);
            if (br.containsHTML(">Lifetime Member<")) {
                ai.setValidUntil(-1);
            } else {
                String expire = br.getRegex("Premium valid until: <strong>(.*?)</strong>").getMatch(0);
                if (expire == null) {
                    account.setValid(false);
                    return ai;
                }
                // remove st/nd/rd/th
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.replaceFirst("(st|nd|rd|th)", ""), "d MMM, yyyy", Locale.UK));
            }
            String space = br.getRegex("<strong>([0-9\\.]+ ?(KB|MB|GB|TB))</strong>[\r\n\t ]+Free Space").getMatch(0);
            if (space != null) ai.setUsedSpace(space);
            String traffic = br.getRegex("donoyet(.*?)xyz").getMatch(0);
            if (traffic != null) {
                // OLD SHIT
                String loaded = br.getRegex("You have used (.*?) out").getMatch(0);
                String max = br.getRegex("limit of (.*?)\\. ").getMatch(0);
                if (max != null && loaded != null) {
                    // you don't need to strip characters or reorder its structure. The source is fine!
                    ai.setTrafficMax(SizeFormatter.getSize(max));
                    ai.setTrafficLeft(ai.getTrafficMax() - SizeFormatter.getSize(loaded));
                } else {
                    max = br.getRegex("You can now download up to (.*?) in").getMatch(0);
                    if (max != null) {
                        ai.setTrafficLeft(SizeFormatter.getSize(max));
                    } else {
                        ai.setUnlimitedTraffic();
                    }
                }
            } else {
                ai.setUnlimitedTraffic();
            }
            ai.setStatus("Premium User");
            try {
                maxPrem.set(-1);
                account.setMaxSimultanDownloads(-1);
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
            }
        }
        return ai;
    }

    private boolean isMail(final String parameter) {
        return parameter.matches(".+@.+");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/legal/terms.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 200;
    }

    public String getUrl() throws IOException, PluginException {
        String url = br.getRegex("\"(http://[a-z0-9\\-]+\\.filefactory\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (url == null) url = br.getRegex("id=\"downloadLinkTarget\" style=\"display: none;\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        // New
        if (url == null) {
            url = br.getRegex("\\'(/dlf/f/[^<>\"]*?)\\'").getMatch(0);
            if (url != null) url = "http://filefactory.com" + url;
        }
        if (url == null) {
            Context cx = null;
            try {
                cx = ContextFactory.getGlobal().enterContext();
                final Scriptable scope = cx.initStandardObjects();
                final String[] eval = br.getRegex("var (.*?) = (.*?), (.*?) = (.*?)+\"(.*?)\", (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?);").getRow(0);
                if (eval != null) {
                    // first load js
                    Object result = cx.evaluateString(scope, "function g(){return " + eval[1] + "} g();", "<cmd>", 1, null);
                    final String link = "/file" + result + eval[4];
                    br.getPage(COOKIE_HOST + link);
                    final String[] row = br.getRegex("var (.*?) = '';(.*;) (.*?)=(.*?)\\(\\);").getRow(0);
                    result = cx.evaluateString(scope, row[1] + row[3] + " ();", "<cmd>", 1, null);
                    if (result.toString().startsWith("http")) {
                        url = result + "";
                    } else {
                        url = COOKIE_HOST + result;
                    }
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            } finally {
                if (cx != null) Context.exit();
            }
        }
        return url;

    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.getURL().contains(TRAFFICSHARELINK) || br.containsHTML(TRAFFICSHARETEXT)) {
            handleTrafficShare(downloadLink);
        } else {
            doFree(downloadLink);
        }
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        String passCode = downloadLink.getStringProperty("pass", null);
        try {
            long waittime;
            if (dlUrl != null) {
                logger.finer("DIRECT free-download");
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 1);
            } else {
                checkErrors(false);
                if (br.containsHTML(PASSWORDPROTECTED)) {
                    if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
                    // stable is lame
                    br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode) + "&Submit=Continue");
                    br.getHeaders().put("Content-Type", null);
                    if (br.containsHTML(PASSWORDPROTECTED)) throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
                }
                // new 20130911
                String dllink = br.getRegex("\"(http://[a-z0-9\\-]+\\.filefactory\\.com/get/[^<>\"]*?)\"").getMatch(0);
                String timer = br.getRegex("<div id=\"countdown_clock\" data-delay=\"(\\d+)").getMatch(0);
                if (timer != null) sleep(Integer.parseInt(timer) * 1001, downloadLink);
                if (dllink != null) {
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
                } else {
                    // old
                    String urlWithFilename = null;
                    if (br.getRegex("Recaptcha\\.create\\(([\r\n\t ]+)?\"([^\"]+)").getMatch(1) != null) {
                        urlWithFilename = handleRecaptcha(downloadLink);
                    } else {
                        urlWithFilename = getUrl();
                    }
                    if (urlWithFilename == null) {
                        logger.warning("getUrl is broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getPage(urlWithFilename);

                    // Sometimes there is an ad
                    final String skipAds = br.getRegex("\"(http://(www\\.)?filefactory\\.com/dlf/[^<>\"]*?)\"").getMatch(0);
                    if (skipAds != null) br.getPage(skipAds);

                    checkErrors(false);
                    String wait = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
                    if (wait != null) {
                        waittime = Long.parseLong(wait) * 1000l;
                        if (waittime > 60000) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
                    }
                    String downloadUrl = getUrl();
                    if (downloadUrl == null) {
                        logger.warning("getUrl is broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }

                    wait = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
                    waittime = 60 * 1000l;
                    if (wait != null) {
                        waittime = Long.parseLong(wait) * 1000l;
                    }
                    if (waittime > 60000l) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
                    waittime += 1000;
                    sleep(waittime, downloadLink);
                    br.setFollowRedirects(true);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl);
                }
            }
            // Prüft ob content disposition header da sind
            if (dl.getConnection().isContentDisposition()) {
                if (passCode != null) downloadLink.setProperty("pass", passCode);
                dl.startDownload();
            } else {
                br.followConnection();
                if (br.containsHTML("have exceeded the download limit")) {
                    waittime = 10 * 60 * 1000l;
                    try {
                        waittime = Long.parseLong(br.getRegex("Please wait (\\d+) minutes to download more files").getMatch(0)) * 1000l;
                    } catch (final Exception e) {
                    }
                    if (waittime > 0) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime); }
                }
                if (br.containsHTML("You are currently downloading too many files at once") || br.containsHTML(">You have recently started a download") || br.getURL().contains("code=275")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l); }
                checkErrors(false);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } catch (final PluginException e4) {
            throw e4;
        } catch (final InterruptedException e2) {
            return;
        } catch (final IOException e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
            if (e.getMessage() != null && e.getMessage().contains("502")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else if (e.getMessage() != null && e.getMessage().contains("503")) {
                logger.severe("Filefactory returned Bad gateway.");
                Thread.sleep(1000);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (br.getURL().contains(TRAFFICSHARELINK) || br.containsHTML(TRAFFICSHARETEXT)) {
            handleTrafficShare(downloadLink);
        } else {
            login(account, false, br);
            if (account.getBooleanProperty("free")) {
                br.setFollowRedirects(true);
                br.getPage(downloadLink.getDownloadURL());
                doFree(downloadLink);
            } else {
                // NOTE: no premium, pre download password handling yet...
                br.setFollowRedirects(false);
                br.getPage(downloadLink.getDownloadURL());
                // Directlink
                String finallink = br.getRedirectLocation();
                // No directlink
                if (finallink == null) finallink = br.getRegex("\"(http://[a-z0-9]+\\.filefactory\\.com/get/[^<>\"]*?)\"").getMatch(0);
                if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
                if (!dl.getConnection().isContentDisposition()) {
                    br.followConnection();
                    checkErrors(true);
                    String red = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                    if (red == null) red = br.getRegex("subPremium.*?ready.*?<a href=\"(.*?)\"").getMatch(0);
                    if (red == null) red = br.getRegex("downloadLink.*?href=\"(.*?)\"").getMatch(0);
                    logger.finer("Indirect download");
                    br.setFollowRedirects(true);
                    if (red == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, red, true, 0);
                    if (!dl.getConnection().isContentDisposition()) {
                        br.followConnection();
                        if (br.containsHTML("Unfortunately we have encountered a problem locating your file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                } else {
                    logger.finer("DIRECT download");
                }
                dl.startDownload();
            }
        }
    }

    public void handleTrafficShare(final DownloadLink downloadLink) throws Exception {
        /*
         * This is for filefactory.com/trafficshare/ sharing links or I guess what we call public premium links. This might replace dlUrl,
         * Unknown until proven otherwise.
         */
        logger.finer("Traffic sharing link - Free Premium Donwload");
        String finalLink = br.getRegex("<a href=\"(https?://\\w+\\.filefactory\\.com/[^\"]+)\"([^>]+)?>Download").getMatch(0);
        if (finalLink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Unfortunately we have encountefinalLink a problem locating your file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    public String handleRecaptcha(final DownloadLink link) throws Exception {
        final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
        jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
        final String id = br.getRegex("Recaptcha\\.create\\(([\r\n\t ]+)?\"([^\"]+)").getMatch(1);
        rc.setId(id);
        final Form form = new Form();
        form.setAction("/file/checkCaptcha.php");
        final String check = br.getRegex("check: ?'(.*?)'").getMatch(0);
        form.put("check", check);
        form.setMethod(MethodType.POST);
        rc.setForm(form);
        rc.load();
        final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
        final String c = getCaptchaCode(cf, link);
        rc.setCode(c);
        if (br.containsHTML(CAPTCHALIMIT)) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        if (!br.containsHTML("status\":\"ok")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String url = br.getRegex("path\":\"(.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        url = url.replaceAll("\\\\/", "/");
        if (url.startsWith("http")) { return url; }
        return COOKIE_HOST + url;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return true;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    private void login(final Account account, final boolean force, final Browser lbr) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                setBrowserExclusive();
                prepBrowser(lbr);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            lbr.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                lbr.getHeaders().put("Accept-Encoding", "gzip");
                lbr.setFollowRedirects(true);
                lbr.getPage(COOKIE_HOST + "/member/signin.php");
                lbr.postPage("/member/signin.php", "loginEmail=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()) + "&Submit=Sign+In");
                if (lbr.containsHTML(LOGIN_ERROR) || lbr.getCookie(COOKIE_HOST, "auth") == null || "deleted".equalsIgnoreCase(lbr.getCookie(COOKIE_HOST, "auth")) || (lbr.getURL() != null && lbr.getURL().contains("/error.php?code=152"))) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = lbr.getCookies(COOKIE_HOST);
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
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        prepBrowser(br);
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "filefactory\\.com/(.+)").getMatch(0));
        br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (final Exception e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            URLConnectionAdapter con = null;
            try {
                dlUrl = null;
                con = br.openGetConnection(downloadLink.getDownloadURL());
                if (con.isContentDisposition()) {
                    downloadLink.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    con.disconnect();
                    dlUrl = downloadLink.getDownloadURL();
                    downloadLink.setAvailable(true);
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
                }
                break;
            } catch (final Exception e) {
                if (i == 3) { throw e; }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        if (br.containsHTML("This file has been deleted\\.|have been deleted") || br.getURL().contains("error.php?code=254")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("This file is no longer available due to an unexpected server error") || br.getURL().contains("error.php?code=252")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML(NO_SLOT)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN)) {
            return AvailableStatus.UNCHECKABLE;
        } else if (br.containsHTML(PASSWORDPROTECTED)) {
            final String fileName = br.getRegex("<title>([^<>\"]*?)\\- FileFactory</title>").getMatch(0);
            if (fileName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            downloadLink.setName(Encoding.htmlDecode(fileName.trim()));
            downloadLink.getLinkStatus().setStatusText("This link is password protected");
            downloadLink.setAvailable(true);
        } else {
            if (isPremiumOnly(br)) {
                downloadLink.getLinkStatus().setErrorMessage("This file is only available to Premium Members");
                downloadLink.getLinkStatus().setStatusText("This file is only available to Premium Members");
            } else if (br.containsHTML(NO_SLOT) || br.getURL().contains("error.php?code=257")) {
                downloadLink.getLinkStatus().setErrorMessage(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", NO_SLOT_USERTEXT));
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", NO_SLOT_USERTEXT));
            } else if (br.containsHTML("Server Maintenance")) {
                downloadLink.getLinkStatus().setStatusText("Server Maintenance");
            } else {
                String fileName = null;
                String fileSize = null;
                if (br.containsHTML("File Not Found")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
                if (br.getURL().contains(TRAFFICSHARELINK)) {
                    fileName = br.getRegex("<section class=\"file\" style=\"margin\\-top:20px;\">[\t\n\r ]+<h2>([^<>\"]*?)</h2>").getMatch(0);
                    fileSize = br.getRegex("<p class=\"size\">[\r\n\t ]+([\\d\\.]+ (KB|MB|GB|TB))").getMatch(0);
                } else {
                    String regex = "<h2>([^\r\n]+)</h2>[\r\n\t ]+<div id=\"file_info\">\\s*([\\d\\.]+ (KB|MB|GB|TB))";
                    fileName = br.getRegex(regex).getMatch(0);
                    if (fileName == null) fileName = br.getRegex("<title>([^<>\"]*?) - FileFactory</title>").getMatch(0);
                    fileSize = br.getRegex(regex).getMatch(1);
                }
                if (fileName == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                downloadLink.setName(Encoding.htmlDecode(fileName.trim()));
                if (fileSize != null) downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
                downloadLink.setAvailable(true);
            }

        }
        return AvailableStatus.TRUE;
    }

    private boolean isPremiumOnly(Browser tbr) {
        if ((tbr.getURL() != null && tbr.getURL().contains("/error.php?code=258")) || tbr.containsHTML("(Please purchase an account to download this file\\.|>This file is only available to Premium Members|Sorry, this file can only be downloaded by Premium members|Please purchase an account in order to instantly download this file|Currently only Premium Members can download files larger)")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
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