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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "https?://(www\\.)?filefactory\\.com(/|//)((?:file|stream)/[\\w]+(/.*)?|(trafficshare|digitalsales)/[a-f0-9]{32}/.+/?)" })
public class FileFactory extends PluginForHost {

    // DEV NOTES
    // other: currently they 302 redirect all non www. to www. which kills most of this plugin.
    // Adjust COOKIE_HOST to suite future changes, or remove COOKIE_HOST from that section of the script.

    // Connection Management
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static final AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);

    private static AtomicInteger       maxPrem                      = new AtomicInteger(1);
    private static AtomicInteger       maxFree                      = new AtomicInteger(1);
    private final String               NO_SLOT                      = ">All free download slots";
    private final String               NO_SLOT_USERTEXT             = "No free slots available";
    private final String               NOT_AVAILABLE                = "class=\"box error\"|have been deleted";
    private final String               SERVERFAIL                   = "(<p>Your download slot has expired\\.|temporarily unavailable)";
    private final String               LOGIN_ERROR                  = "The email or password you have entered is incorrect";
    private final String               SERVER_DOWN                  = "server hosting the file you are requesting is currently down";
    private final String               CAPTCHALIMIT                 = "<p>We have detected several recent attempts to bypass our free download restrictions originating from your IP Address";
    private static Object              LOCK                         = new Object();
    private final String               COOKIE_HOST                  = "http://www.filefactory.com";
    private String                     dlUrl                        = null;
    private final String               TRAFFICSHARELINK             = "filefactory.com/trafficshare/";
    private final String               TRAFFICSHARETEXT             = ">Download with FileFactory TrafficShare<";
    private final String               PASSWORDPROTECTED            = ">You are trying to access a password protected file|This File has been password protected by the uploader\\.";
    private final String               DBCONNECTIONFAILED           = "Couldn't get valid connection to DB";

    public FileFactory(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(COOKIE_HOST + "/info/premium.php");
    }

    private static AtomicReference<String> agent = new AtomicReference<String>();

    /**
     * defines custom browser requirements.
     * */
    private Browser prepBrowser(final Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        prepBr.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.getHeaders().put("Cache-Control", null);
        prepBr.getHeaders().put("Pragma", null);
        prepBr.setReadTimeout(3 * 60 * 1000);
        prepBr.setConnectTimeout(3 * 60 * 1000);
        return prepBr;
    }

    public void checkErrors(final boolean freeDownload, final boolean postDownload) throws PluginException {
        if (isPremiumOnly(br)) {
            throwPremiumRequiredException();
        }
        // this should cover error codes jumping to stream links in redirect, since filefactory wont fix this issue, this is my workaround.
        String code = new Regex(br.getURL(), "(?:\\?|&)code=(\\d+)").getMatch(0);
        if (code == null) {
            @SuppressWarnings("unchecked")
            final ArrayList<String> redirectUrls = (ArrayList<String>) this.getDownloadLink().getProperty(dlRedirects, null);
            if (redirectUrls != null) {
                for (final String url : redirectUrls) {
                    code = new Regex(url, "(?:\\?|&)code=(\\d+)").getMatch(0);
                    if (code != null) {
                        break;
                    }
                }
            }
        }
        final int errTries = 4;
        final int errCode = (!inValidate(code) && code.matches("\\d+") ? Integer.parseInt(code) : -1);
        final String errRetry = "retry_" + errCode;
        final int tri = this.getDownloadLink().getIntegerProperty(errRetry, 0) + 1;
        this.getDownloadLink().setProperty(errRetry, (tri >= errTries ? 0 : tri));
        final String errMsg = (tri >= errTries ? "Exausted try count " : "Try count ") + tri + ", for '" + errCode + "' error";
        logger.warning(errMsg);

        if (postDownload && freeDownload) {
            if (br.containsHTML("have exceeded the download limit|Please try again in <span>")) {
                long waittime = 10 * 60 * 1000l;
                try {
                    final String wt2 = br.getRegex("Please try again in <span>(.*?)</span>").getMatch(0);
                    if (wt2 != null) {
                        String tmpYears = new Regex(wt2, "(\\d+)\\s+years?").getMatch(0);
                        String tmpdays = new Regex(wt2, "(\\d+)\\s+days?").getMatch(0);
                        String tmphrs = new Regex(wt2, "(\\d+)\\s+hours?").getMatch(0);
                        String tmpmin = new Regex(wt2, "(\\d+)\\s+min(ute)?s?").getMatch(0);
                        String tmpsec = new Regex(wt2, "(\\d+)\\s+sec(ond)?s?").getMatch(0);
                        long years = 0, days = 0, hours = 0, minutes = 0, seconds = 0;
                        if (!inValidate(tmpYears)) {
                            years = Integer.parseInt(tmpYears);
                        }
                        if (!inValidate(tmpdays)) {
                            days = Integer.parseInt(tmpdays);
                        }
                        if (!inValidate(tmphrs)) {
                            hours = Integer.parseInt(tmphrs);
                        }
                        if (!inValidate(tmpmin)) {
                            minutes = Integer.parseInt(tmpmin);
                        }
                        if (!inValidate(tmpsec)) {
                            seconds = Integer.parseInt(tmpsec);
                        }
                        waittime = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
                    }
                } catch (final Exception e) {
                }
                if (waittime > 0) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
                }
            }
            if (br.containsHTML("You are currently downloading too many files at once") || br.containsHTML(">You have recently started a download") || errCode == 275) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errMsg, 5 * 60 * 1000l);
            }
            if (errCode == 266) {
                // <strong>Download error (266)</strong><br>This download is not yet ready. Please retry your download and wait for the
                // countdown timer to complete. </div>
                if (tri >= errTries) {
                    // throw new PluginException(LinkStatus.ERROR_FATAL, errMsg);
                    // want to see this issue reported to statserv so I can monitor / report back to admin!
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "266 IS STILL HAPPENING!");
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, errMsg, 2 * 60 * 1000l);
            }
        }
        if (errCode == 265) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The requested Download URL was invalid.  Please retry your download", 5 * 60 * 1000l);
        }
        if (errCode == 263) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Could not retrieve information about your download, or your download key has expired. Please try again. ", 5 * 60 * 1000l);
        }
        if (freeDownload) {
            if (br.containsHTML(CAPTCHALIMIT)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            }
            if (br.containsHTML(NO_SLOT) || errCode == 257) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NO_SLOT_USERTEXT, 10 * 60 * 1000l);
            }
            if (br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0) != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0)) * 60 * 1001l);
            }
        }
        if (errCode == 274) {
            // <h2>File Unavailable</h2>
            // <p>
            // This file cannot be downloaded at this time. Please let us know about this issue by using the contact link below. </p>
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file cannot be downloaded at this time.", 20 * 60 * 1000l);
        }
        if (br.containsHTML(SERVERFAIL)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l);
        }
        if (br.containsHTML(NOT_AVAILABLE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(SERVER_DOWN)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        }
        if (br.containsHTML(DBCONNECTIONFAILED)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        if (useAPI.get()) {
            checkLinks_API(urls, null);
        }
        if (!useAPI.get()) {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.getHeaders().put("Accept-Encoding", "identity");
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
                        if (filter == null) {
                            dl.setAvailable(false);
                        }
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
                        if (size != null) {
                            dl.setDownloadSize(SizeFormatter.getSize(size));
                        }
                        if (filter != null && filter.contains(">Valid</abbr>")) {
                            dl.setAvailable(true);
                        } else {
                            dl.setAvailable(false);
                        }
                    }
                    if (index == urls.length) {
                        break;
                    }
                }
            } catch (final Exception e) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws PluginException {
        link.setUrlDownload(link.getDownloadURL().replaceFirst("\\.com//", ".com/"));
        link.setUrlDownload(link.getDownloadURL().replaceFirst("://filefactory", "://www.filefactory"));
        link.setUrlDownload(link.getDownloadURL().replaceFirst("/stream/", "/file/"));
        // set trafficshare links like 'normal' links, this allows downloads to continue living if the uploader discontinues trafficshare
        // for that uid. Also re-format premium only links!
        if (link.getDownloadURL().contains(TRAFFICSHARELINK) || link.getDownloadURL().contains("/digitalsales/")) {
            String[] uid = new Regex(link.getDownloadURL(), "(https?://.*?filefactory\\.com/)(trafficshare|digitalsales)/[a-f0-9]{32}/([^/]+)/?").getRow(0);
            if (uid != null && (uid[0] != null || uid[2] != null)) {
                link.setUrlDownload(uid[0] + "file/" + uid[2]);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        final String fid = getFUID(link);
        if (fid != null) {
            link.setLinkID(getHost() + "://" + fid);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        if (!isMail(account.getUser())) {
            ai.setStatus("Please enter your E-Mail address as username!");
            account.setValid(false);
            return ai;
        }
        if (useAPI.get()) {
            ai = fetchAccountInfo_API(account, ai);
        } else {
            try {
                login(account, true, br);
            } catch (final PluginException e) {
                account.setValid(false);
                return ai;
            }
            if (!br.getURL().endsWith("/account/")) {
                br.getPage("/account/");
            }
            // <li class="tooltipster" title="Premium valid until: <strong>30th Jan, 2014</strong>">
            if (!br.containsHTML("title=\"(Premium valid until|Lifetime Member)")) {
                ai.setStatus("Registered (free) User");
                ai.setUnlimitedTraffic();
                account.setProperty("free", true);

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
                if (space != null) {
                    ai.setUsedSpace(space);
                }
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
        return maxFree.get();
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

    public String getUrl() throws IOException, PluginException, ScriptException {
        String url = br.getRegex("\"(http://[a-z0-9\\-]+\\.filefactory\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("id=\"downloadLinkTarget\" style=\"display: none;\">[\t\n\r ]+<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        }
        // New
        if (url == null) {
            url = br.getRegex("\\'(/dlf/f/[^<>\"]*?)\\'").getMatch(0);
            if (url != null) {
                url = "http://filefactory.com" + url;
            }
        }
        if (url == null) {

            final ScriptEngineManager manager = JavaScriptEngineFactory.getScriptEngineManager(this);
            final ScriptEngine engine = manager.getEngineByName("javascript");

            final String[] eval = br.getRegex("var (.*?) = (.*?), (.*?) = (.*?)+\"(.*?)\", (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?), (.*?) = (.*?);").getRow(0);
            if (eval != null) {
                // first load js
                Object result = engine.eval("function g(){return " + eval[1] + "} g();");
                final String link = "/file" + result + eval[4];
                br.getPage(COOKIE_HOST + link);
                final String[] row = br.getRegex("var (.*?) = '';(.*;) (.*?)=(.*?)\\(\\);").getRow(0);
                result = engine.eval(row[1] + row[3] + " ();");
                if (result.toString().startsWith("http")) {
                    url = result + "";
                } else {
                    url = COOKIE_HOST + result;
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }

        }
        return url;

    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        // reset setter
        downloadLink.setProperty(dlRedirects, Property.NULL);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        if (useAPI.get()) {
            handleDownload_API(downloadLink, null);
        } else {
            requestFileInformation(null, downloadLink);
            if (br.getURL().contains(TRAFFICSHARELINK) || br.containsHTML(TRAFFICSHARETEXT)) {
                handleTrafficShare(downloadLink, null);
            } else {
                doFree(downloadLink, null);
            }
        }
    }

    public void doFree(final DownloadLink downloadLink, final Account account) throws Exception {
        String passCode = downloadLink.getStringProperty("pass", null);
        try {
            long waittime;
            if (dlUrl != null) {
                logger.finer("DIRECT free-download");
                br.setFollowRedirects(true);
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlUrl, true, 1);
            } else {
                checkErrors(true, false);
                if (br.containsHTML(PASSWORDPROTECTED)) {
                    if (passCode == null) {
                        passCode = Plugin.getUserInput("Password?", downloadLink);
                    }
                    // stable is lame
                    br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode) + "&Submit=Continue");
                    br.getHeaders().put("Content-Type", null);
                    if (br.containsHTML(PASSWORDPROTECTED)) {
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
                    }
                }
                // new 20130911
                String dllink = br.getRegex("\"(http://[a-z0-9\\-]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                String timer = br.getRegex("<div id=\"countdown_clock\" data-delay=\"(\\d+)").getMatch(0);
                if (timer != null) {
                    sleep(Integer.parseInt(timer) * 1001, downloadLink);
                }
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
                    final String skipAds = br.getRegex("\"(http://(www\\.)?filefactory\\.com/dlf/[^<>\"]+)\"").getMatch(0);
                    if (skipAds != null) {
                        br.getPage(skipAds);
                    }
                    checkErrors(true, false);
                    String wait = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
                    if (wait != null) {
                        waittime = Long.parseLong(wait) * 1000l;
                        if (waittime > 60000) {
                            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
                        }
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
                    if (waittime > 60000l) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
                    }
                    waittime += 1000;
                    sleep(waittime, downloadLink);
                    br.setFollowRedirects(true);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadUrl);
                }
            }
            // Prüft ob content disposition header da sind
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection();
                checkErrors(true, true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                downloadLink.setProperty("pass", passCode);
            }
            // add download slot
            controlSlot(+1, account);
            try {
                dl.startDownload();
            } finally {
                // remove download slot
                controlSlot(-1, account);
            }
        } catch (final PluginException e4) {
            throw e4;
        } catch (final InterruptedException e2) {
            return;
        } catch (final IOException e) {
            logger.log(e);
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
        // reset setter
        downloadLink.setProperty(dlRedirects, Property.NULL);
        if (useAPI.get()) {
            handleDownload_API(downloadLink, account);
        } else {
            requestFileInformation(account, downloadLink);
            if (br.getURL().contains(TRAFFICSHARELINK) || br.containsHTML(TRAFFICSHARETEXT)) {
                handleTrafficShare(downloadLink, account);
            } else {
                login(account, false, br);
                if (account.getBooleanProperty("free")) {
                    br.setFollowRedirects(true);
                    br.getPage(downloadLink.getDownloadURL());
                    if (checkShowFreeDialog(getHost())) {
                        showFreeDialog(getHost());
                    }
                    doFree(downloadLink, account);
                } else {
                    // NOTE: no premium, pre download password handling yet...
                    br.setFollowRedirects(false);
                    br.getPage(downloadLink.getDownloadURL());
                    // Directlink
                    String finallink = br.getRedirectLocation();
                    // No directlink
                    if (finallink == null) {
                        finallink = br.getRegex("\"(http://[a-z0-9]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                    }
                    if (finallink == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.setFollowRedirects(true);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
                    if (!dl.getConnection().isContentDisposition()) {
                        br.followConnection();
                        checkErrors(false, true);
                        String red = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                        if (red == null) {
                            red = br.getRegex("subPremium.*?ready.*?<a href=\"(.*?)\"").getMatch(0);
                        }
                        if (red == null) {
                            red = br.getRegex("downloadLink.*?href=\"(.*?)\"").getMatch(0);
                        }
                        logger.finer("Indirect download");
                        br.setFollowRedirects(true);
                        if (red == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, red, true, 0);
                        if (!dl.getConnection().isContentDisposition()) {
                            br.followConnection();
                            if (br.containsHTML("Unfortunately we have encountered a problem locating your file")) {
                                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                            }
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    } else {
                        logger.finer("DIRECT download");
                    }
                    // add download slot
                    controlSlot(+1, account);
                    try {
                        dl.startDownload();
                    } finally {
                        // remove download slot
                        controlSlot(-1, account);
                    }
                }
            }
        }
    }

    public void handleTrafficShare(final DownloadLink downloadLink, final Account account) throws Exception {
        /*
         * This is for filefactory.com/trafficshare/ sharing links or I guess what we call public premium links. This might replace dlUrl,
         * Unknown until proven otherwise.
         */
        logger.finer("Traffic sharing link - Free Premium Donwload");
        String finalLink = br.getRegex("<a href=\"(https?://\\w+\\.filefactory\\.com/get/t/[^\"]+)\"[^\r\n]*Download with FileFactory TrafficShare").getMatch(0);
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (Application.getJavaVersion() < Application.JAVA17) {
            finalLink = finalLink.replaceFirst("https", "http");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalLink, true, 0);
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (br.containsHTML("Unfortunately we have encountefinalLink a problem locating your file")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // add download slot
        controlSlot(+1, account);
        try {
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    public String handleRecaptcha(final DownloadLink link) throws Exception {
        final Recaptcha rc = new Recaptcha(br, this);
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
        final String c = getCaptchaCode("recaptcha", cf, link);
        rc.setCode(c);
        if (br.containsHTML(CAPTCHALIMIT)) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        }
        if (!br.containsHTML("status\":\"ok")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String url = br.getRegex("path\":\"(.*?)\"").getMatch(0);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        url = url.replaceAll("\\\\/", "/");
        if (url.startsWith("http")) {
            return url;
        }
        return COOKIE_HOST + url;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    private void login(final Account account, final boolean force, final Browser lbr) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            try {
                setBrowserExclusive();
                prepBrowser(lbr);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
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
                if (lbr.containsHTML(LOGIN_ERROR) || lbr.getCookie(COOKIE_HOST, "auth") == null || "deleted".equalsIgnoreCase(lbr.getCookie(COOKIE_HOST, "auth")) || (lbr.getURL() != null && lbr.getURL().contains("/error.php?code=152"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
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
                if (StringUtils.containsIgnoreCase(lbr.getRedirectLocation(), "code=105") || StringUtils.containsIgnoreCase(lbr.getURL(), "code=105")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "The account you have tried to sign into is pending deletion. Please contact FileFactory support if you require further assistance.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                throw e;
            }
        }
    }

    private AvailableStatus reqFileInformation(final DownloadLink link, final Account account) throws Exception {
        correctDownloadLink(link);
        if (!checkLinks_API(new DownloadLink[] { link }, account) || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        if (!checkLinks_API(new DownloadLink[] { link }, null) || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    public AvailableStatus requestFileInformation(final Account account, final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        prepBrowser(br);
        fuid = getFUID(downloadLink);
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
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(downloadLink.getDownloadURL());
                    }
                }
                break;
            } catch (final Exception e) {
                logger.log(e);
                if (i == 3) {
                    throw e;
                }
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        if (br.containsHTML("This file has been deleted\\.|have been deleted") || br.getURL().contains("error.php?code=254")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("This file is no longer available due to an unexpected server error") || br.getURL().contains("error.php?code=252")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML(NO_SLOT)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN)) {
            return AvailableStatus.UNCHECKABLE;
        } else if (br.containsHTML(PASSWORDPROTECTED)) {
            final String fileName = br.getRegex("<title>([^<>\"]*?)- FileFactory</title>").getMatch(0);
            if (fileName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
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
                if (br.containsHTML("File Not Found")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (br.getURL().contains(TRAFFICSHARELINK)) {
                    fileName = br.getRegex("<section class=\"file\" style=\"margin-top:20px;\">[\t\n\r ]+<h2>([^<>\"]+)</h2>").getMatch(0);
                    if (fileName == null) {
                        fileName = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
                    }
                    fileSize = br.getRegex("id=\"file_info\">([\\d\\.]+ (KB|MB|GB|TB))").getMatch(0);
                } else {
                    String regex = "<h2>([^\r\n]+)</h2>[\r\n\t ]+<div id=\"file_info\">\\s*([\\d\\.]+ (KB|MB|GB|TB))";
                    fileName = br.getRegex(regex).getMatch(0);
                    if (fileName == null) {
                        fileName = br.getRegex("<title>([^<>\"]*?) - FileFactory</title>").getMatch(0);
                    }
                    fileSize = br.getRegex(regex).getMatch(1);
                }
                if (fileName == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                downloadLink.setName(Encoding.htmlDecode(fileName.trim()));
                if (fileSize != null) {
                    downloadLink.setDownloadSize(SizeFormatter.getSize(fileSize));
                }
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
        link.setProperty("retry_701", Property.NULL);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

    private String getFUID(DownloadLink downloadLink) {
        final String fuid = new Regex(downloadLink.getDownloadURL(), "file/([\\w]+)").getMatch(0);
        return fuid;
    }

    private String getApi() {
        if (Application.getJavaVersion() < Application.JAVA17) {
            return "http://api.filefactory.com/v1";
        } else {
            return "https://api.filefactory.com/v1";
        }
    }

    private boolean checkLinks_API(final DownloadLink[] urls, Account account) {
        if (account == null) {
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            Account n = null;
            if (accounts != null && accounts.size() != 0) {
                Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        try {
                            if (getApiKey(n) != null) {
                                account = n;
                                break;
                            } else {
                                n = null;
                            }
                        } catch (Exception e) {
                        }
                    }
                }
            }
        }
        try {
            final Browser br = new Browser();
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (links.size() > 100 || index == urls.length) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                sb.append("file=");
                for (final DownloadLink dl : links) {
                    sb.append(getFUID(dl));
                    sb.append(",");
                }
                // lets remove last ","
                sb.replace(sb.length() - 1, sb.length(), "");
                getPage(br, getApi() + "/getFileInfo?" + sb, account);
                for (final DownloadLink dl : links) {
                    // password is last value in fuid response, needed because filenames or other values could contain }. It then returns
                    // invalid response.
                    final String filter = br.getRegex("(\"" + getFUID(dl) + "\"\\s*:\\s*\\{.*?\\})").getMatch(0);
                    if (filter == null) {
                        return false;
                    }
                    final String status = PluginJSonUtils.getJsonValue(filter, "status");
                    if (!"online".equalsIgnoreCase(status)) {
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    final String name = PluginJSonUtils.getJsonValue(filter, "name");
                    final String size = PluginJSonUtils.getJsonValue(filter, "size");
                    final String md5 = PluginJSonUtils.getJsonValue(filter, "md5");
                    final String prem = PluginJSonUtils.getJsonValue(filter, "premiumOnly");
                    final String pass = PluginJSonUtils.getJsonValue(filter, "password");
                    if (StringUtils.isNotEmpty(name)) {
                        dl.setName(name);
                    }
                    if (size != null && size.matches("^\\d+$")) {
                        dl.setVerifiedFileSize(Long.parseLong(size));
                    }
                    if (StringUtils.isNotEmpty(md5)) {
                        dl.setMD5Hash(md5);
                    }
                    if (prem != null) {
                        dl.setProperty("premiumRequired", Boolean.parseBoolean(prem));
                    }
                    if (pass != null) {
                        dl.setProperty("passwordRequired", Boolean.parseBoolean(pass));
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

    private final AtomicBoolean useAPI  = new AtomicBoolean(true);

    private String              fuid    = null;
    private String              dllink  = null;
    private int                 chunks  = 0;
    private boolean             resumes = true;
    private boolean             isFree  = true;

    private void setConstants(final Account account, final boolean trafficShare) {
        if (trafficShare) {
            // traffic share download
            chunks = 0;
            resumes = true;
            isFree = false;
            logger.finer("setConstants = Traffic Share Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            if (account != null) {
                if (account.getBooleanProperty("free", false)) {
                    // free account
                    chunks = 1;
                    resumes = false;
                    isFree = true;
                } else {
                    // premium account
                    chunks = 0;
                    resumes = true;
                    isFree = false;
                }
                logger.finer("setConstants = " + account.getUser() + " @ Account Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
            } else {
                // free non account
                chunks = 1;
                resumes = false;
                isFree = true;
                logger.finer("setConstants = Guest Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
            }
        }
    }

    private void handleDownload_API(final DownloadLink downloadLink, final Account account) throws Exception {
        setConstants(account, false);
        fuid = getFUID(downloadLink);
        prepApiBrowser(br);
        reqFileInformation(downloadLink, account);
        String passCode = downloadLink.getStringProperty("pass", null);
        // error handling can be within redirects.
        br.setFollowRedirects(true);
        if (dllink == null) {
            if (downloadLink.getBooleanProperty("premiumRequired", false) && isFree) {
                // free dl isn't possible, place before passwordRequired!
                throwPremiumRequiredException();
            }
            if (downloadLink.getBooleanProperty("passwordRequired", false)) {
                // dl requires pre download password
                if (inValidate(passCode)) {
                    passCode = Plugin.getUserInput("Password Required!", downloadLink);
                }
                if (inValidate(passCode)) {
                    logger.info("User has entered blank password!");
                    downloadLink.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Invalid password", 1 * 60 * 1001);
                }
            }
            getPage(br, getApi() + "/getDownloadLink?file=" + fuid + (!inValidate(passCode) ? "&password=" + Encoding.urlEncode(passCode) : ""), account);
            if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":701")) {
                // {"type":"error","message":"Error generating download link.  Please try again","code":701}
                // TODO: remove this when retry count comes back!
                int r = downloadLink.getIntegerProperty("retry_701", 0);
                if (r == 4) {
                    throw new PluginException(LinkStatus.ERROR_FATAL);
                } else {
                    r++;
                    downloadLink.setProperty("retry_701", r);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
                }
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":708")) {
                // {"type":"error","message":"This file can only be downloaded by Premium members","code":708}
                throwPremiumRequiredException();
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":712")) {
                // 712 ERR_API_FILE_INVALID
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":713")) {
                // 713 ERR_API_FILE_OFFLINE
                // {"type":"error","message":"File is temporarily unavailable due to system maintenance","code":713}
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":714")) {
                // 714 ERR_API_FILE_SERVER_LOAD || free account == too many ppl dling
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NO_SLOT_USERTEXT, 10 * 60 * 1000l);
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":715")) {
                // 715 ERR_API_PASSWORD_REQUIRED
                // this should not happen as we have a check in linkgrabber... would only be a problem if linkgrabber info was incorrect.
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":716")) {
                // 716 ERR_API_PASSWORD_INVALID
                logger.info("Invalid password!");
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Invalid password", 1 * 60 * 1001);
            } else if (br.containsHTML("\"type\":\"error\"") && br.containsHTML("\"code\":717")) {
                // 717 ERR_API_PASSWORD_ATTEMPTS
                logger.info("Invalid password! Too many incorrect guesses");
                downloadLink.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Invalid password", 5 * 60 * 1001);
            }
            dllink = PluginJSonUtils.getJsonValue(br, "url");
            final String linkType = PluginJSonUtils.getJsonValue(br, "linkType");
            if (inValidate(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if ("trafficshare".equalsIgnoreCase(linkType)) {
                setConstants(account, true);
            }
            String delay = PluginJSonUtils.getJsonValue(br, "delay");
            if (!inValidate(passCode)) {
                downloadLink.setProperty("pass", passCode);
            }
            if (!inValidate(delay)) {
                final int s = Integer.parseInt(delay);
                sleep((s * 1001) + 1111, downloadLink);
            }
        }
        dllink = dllink.replace("\\", "");
        handleDL(downloadLink, account);
    }

    private static final String dlRedirects = "dlRedirects";

    private void handleDL(final DownloadLink downloadLink, final Account account) throws Exception {
        /*
         * Since I fixed the download core setting correct redirect referrer I can no longer use redirect header to determine error code for
         * max connections. This is really only a problem with media files as filefactory redirects to /stream/ directly after code=\d+
         * which breaks our generic handling. This will fix it!! - raztoki
         */
        int i = -1;
        ArrayList<String> urls = new ArrayList<String>();
        br.setFollowRedirects(false);
        URLConnectionAdapter con = null;
        while (i++ < 10) {
            String url = dllink;
            if (!urls.isEmpty()) {
                url = urls.get(urls.size() - 1);
            }
            try {
                con = br.openGetConnection(url);
                if (!con.isContentDisposition() && br.getRedirectLocation() != null) {
                    // redirect, we want to store and continue down the rabbit hole!
                    final String redirect = br.getRedirectLocation();
                    urls.add(redirect);
                    continue;
                } else if (!con.isContentDisposition()) {
                    // error final destination/html
                    br.followConnection();
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(url);
                    }
                    break;
                } else {
                    // finallink! (usually doesn't container redirects)
                    dllink = br.getURL();
                    break;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        if (!urls.isEmpty()) {
            downloadLink.setProperty(dlRedirects, urls);
        }
        if (!con.isContentDisposition()) {
            checkErrors(isFree, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumes, chunks);
        if (!dl.getConnection().isContentDisposition()) {
            // this shouldn't happen anymore!
            br.followConnection();
            checkErrors(isFree, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // add download slot
        controlSlot(+1, account);
        try {
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    private static Object apiAccLock = new Object();

    private String loginKey(final Account account) throws Exception {
        synchronized (apiAccLock) {
            final Browser nbr = new Browser();
            prepApiBrowser(nbr);
            nbr.getPage(getApi() + "/getSessionKey?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
            final String apiKey = PluginJSonUtils.getJsonValue(nbr, "key");
            if (apiKey != null) {
                account.setProperty("apiKey", apiKey);
                return apiKey;
            }
            if ("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(nbr, "type")) && ("705".equalsIgnoreCase(PluginJSonUtils.getJsonValue(nbr, "code")) || "706".equalsIgnoreCase(PluginJSonUtils.getJsonValue(nbr, "code")) || "707".equalsIgnoreCase(PluginJSonUtils.getJsonValue(nbr, "code")))) {
                // 705 ERR_API_LOGIN_ATTEMPTS Too many failed login attempts. Please wait 15 minute and try to login again.
                // 706 ERR_API_LOGIN_FAILED Login details were incorrect
                // 707 ERR_API_ACCOUNT_DELETED Account has been deleted, or is pending deletion
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errorMsg(nbr), PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            if (isPendingDeletion(nbr)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "The account you have tried to sign into is pending deletion. Please contact FileFactory support if you require further assistance.", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private synchronized String getApiKey(final Account account) throws Exception {
        synchronized (apiAccLock) {
            String apiKey = account.getStringProperty("apiKey", null);
            if (apiKey == null) {
                apiKey = loginKey(account);
                if (apiKey == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            return apiKey;
        }
    }

    private Browser prepApiBrowser(final Browser ibr) {
        ibr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        return ibr;
    }

    private boolean isPendingDeletion(Browser br) {
        if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "code=105") || StringUtils.containsIgnoreCase(br.getURL(), "code=105")) {
            return true;
        }
        if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "code=152") || StringUtils.containsIgnoreCase(br.getURL(), "code=152")) {
            return true;
        }
        return false;
    }

    private void getPage(final Browser ibr, final String url, final Account account) throws Exception {
        if (account != null) {
            synchronized (apiAccLock) {
                String apiKey = getApiKey(account);
                ibr.getPage(url + (url.matches("(" + getApi() + ")?/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "key=" + apiKey);
                if (sessionKeyInValid(account, ibr)) {
                    apiKey = getApiKey(account);
                    if (apiKey != null) {
                        // can't sessionKeyInValid because getApiKey/loginKey return String, and loginKey uses a new Browser.
                        ibr.getPage(url + (url.matches("(" + getApi() + ")?/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "key=" + apiKey);
                    } else {
                        if (isPendingDeletion(ibr)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "The account you have tried to sign into is pending deletion. Please contact FileFactory support if you require further assistance.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                        // failure occurred.
                        throw new PluginException(LinkStatus.ERROR_FATAL);
                    }
                }
                // account specific errors which could happen at any point in time!
                if ("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "type")) && ("707".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "code")) || "719".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "code")))) {
                    // 707 ERR_API_ACCOUNT_DELETED Account has been deleted, or is pending deletion
                    // 719 ERR_API_ACCOUNT_SUSPENDED The account being used has been suspended
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\n" + errorMsg(ibr), PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
        } else {
            ibr.getPage(url);
        }
        // error handling for generic errors which could occur at any point in time!
        if ("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "type")) && ("718".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "code")))) {
            // 718 ERR_API_IP_SUSPENDED The IP Address initiating the request has been suspended
            throw new PluginException(LinkStatus.ERROR_FATAL, "\r\n" + errorMsg(ibr));
        }
    }

    private String errorMsg(final Browser ibr) {
        final String message = PluginJSonUtils.getJsonValue(ibr, "message");
        if (message != null) {
            logger.warning(message);
            return message;
        } else {
            return null;
        }
    }

    private boolean sessionKeyInValid(final Account account, final Browser ibr) {
        if ("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "type")) && ("710".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "code")) || "711".equalsIgnoreCase(PluginJSonUtils.getJsonValue(ibr, "code")))) {
            // 710 ERR_API_SESS_KEY_INVALID The session key has expired or is invalid. Please obtain a new one via getSessionKey.
            // 711 ERR_API_LOGIN_EXPIRED The session key has expired. Please obtain a new one via getSessionKey.
            account.setProperty("apiKey", Property.NULL);
            return true;
        } else {
            return false;
        }
    }

    private AccountInfo fetchAccountInfo_API(final Account account, final AccountInfo ai) throws Exception {
        if (account.getPass() == null || "".equals(account.getPass())) {
            account.setValid(false);
            return ai;
        }
        loginKey(account);
        getPage(br, getApi() + "/getMemberInfo", account);
        final String expire = PluginJSonUtils.getJsonValue(br, "expiryMs");
        final String type = PluginJSonUtils.getJsonValue(br, "accountType");
        if ("premium".equalsIgnoreCase(type)) {
            account.setProperty("free", false);
            account.setProperty("totalMaxSim", 20);
            ai.setStatus("Premium Account");
            if (expire != null) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expire));
            }
        } else {
            account.setProperty("free", true);
            account.setProperty("totalMaxSim", 20);
            ai.setStatus("Free Account");
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    private void throwPremiumRequiredException() throws PluginException {
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "This file is only available to Premium Members");
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     * */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    private static Object CTRLLOCK = new Object();

    /**
     * Prevents more than one free download from starting at a given time. One step prior to dl.startDownload(), it adds a slot to maxFree
     * which allows the next singleton download to start, or at least try.
     *
     * This is needed because xfileshare(website) only throws errors after a final dllink starts transferring or at a given step within pre
     * download sequence. But this template(XfileSharingProBasic) allows multiple slots(when available) to commence the download sequence,
     * this.setstartintival does not resolve this issue. Which results in x(20) captcha events all at once and only allows one download to
     * start. This prevents wasting peoples time and effort on captcha solving and|or wasting captcha trading credits. Users will experience
     * minimal harm to downloading as slots are freed up soon as current download begins.
     *
     * @param controlSlot
     *            (+1|-1)
     * @author raztoki
     * */
    private void controlSlot(final int num, final Account account) {
        synchronized (CTRLLOCK) {
            if (account == null) {
                int was = maxFree.get();
                maxFree.set(Math.min(Math.max(1, maxFree.addAndGet(num)), totalMaxSimultanFreeDownload.get()));
                logger.info("maxFree was = " + was + " && maxFree now = " + maxFree.get());
            } else {
                int was = maxPrem.get();
                maxPrem.set(Math.min(Math.max(1, maxPrem.addAndGet(num)), account.getIntegerProperty("totalMaxSim", 20)));
                logger.info("maxPrem was = " + was + " && maxPrem now = " + maxPrem.get());
            }
        }
    }

}