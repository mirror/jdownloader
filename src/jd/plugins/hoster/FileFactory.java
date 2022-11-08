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
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UserAgents;
import jd.utils.locale.JDL;

import org.appwork.loggingv3.NullLogger;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FileFactory extends PluginForHost {
    // DEV NOTES
    // other: currently they 302 redirect all non www. to www. which kills most of this plugin.
    // Adjust COOKIE_HOST to suite future changes, or remove COOKIE_HOST from that section of the script.
    // Connection Management
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static AtomicInteger totalMaxSimultanFreeDownload = new AtomicInteger(20);
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);
    private static AtomicInteger maxFree                      = new AtomicInteger(1);
    private final String         NO_SLOT                      = ">All free download slots";
    private final String         PROPERTY_APIKEY              = "apiKey";
    private final String         NO_SLOT_USERTEXT             = "No free slots available";
    private final String         NOT_AVAILABLE                = "class=\"box error\"|have been deleted";
    private final String         SERVERFAIL                   = "(<p>Your download slot has expired\\.|temporarily unavailable)";
    private final String         LOGIN_ERROR                  = "The email or password you have entered is incorrect";
    private final String         SERVER_DOWN                  = "server hosting the file you are requesting is currently down";
    private final String         CAPTCHALIMIT                 = "<p>We have detected several recent attempts to bypass our free download restrictions originating from your IP Address";
    private String               dlUrl                        = null;
    private final String         TRAFFICSHARELINK             = "filefactory.com/trafficshare/";
    private final String         TRAFFICSHARETEXT             = ">Download with FileFactory TrafficShare<";
    private final String         PASSWORDPROTECTED            = ">You are trying to access a password protected file|This File has been password protected by the uploader\\.";
    private final String         DBCONNECTIONFAILED           = "Couldn't get valid connection to DB";

    public FileFactory(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/info/premium.php");
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "filefactory.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    /**
     * https://api.filefactory.com
     *
     * @return
     */
    protected String getAuthKey() {
        return "cfbc9099994d3bafd5a5f13c38c542f0";
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "(/|//)((?:file|stream)/[\\w]+(/.*)?|(trafficshare|digitalsales)/[a-f0-9]{32}/.+/?)");
        }
        return ret.toArray(new String[0]);
    }

    private static AtomicReference<String> agent = new AtomicReference<String>();

    /**
     * defines custom browser requirements.
     */
    private Browser prepBrowser(final Browser prepBr) {
        if (agent.get() == null) {
            agent.set(UserAgents.stringUserAgent());
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

    public void checkErrorsWebsite(final boolean freeDownload, final boolean postDownload) throws PluginException {
        if (isPremiumOnly(br)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        // this should cover error codes jumping to stream links in redirect, since filefactory wont fix this issue, this is my workaround.
        String code = new Regex(br.getURL(), "(?:\\?|&)code=(\\d+)").getMatch(0);
        if (code == null) {
            @SuppressWarnings("unchecked")
            final List<String> redirectUrls = (List<String>) this.getDownloadLink().getProperty(dlRedirects, null);
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
        final int errCode = (!StringUtils.isEmpty(code) && code.matches("\\d+") ? Integer.parseInt(code) : -1);
        final String errRetry = "retry_" + errCode;
        final int tri = this.getDownloadLink().getIntegerProperty(errRetry, 0) + 1;
        this.getDownloadLink().setProperty(errRetry, (tri >= errTries ? 0 : tri));
        String errMsg = (tri >= errTries ? "Exausted try count " : "Try count ") + tri + ", for '" + errCode + "' error";
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
                        if (!StringUtils.isEmpty(tmpYears)) {
                            years = Integer.parseInt(tmpYears);
                        }
                        if (!StringUtils.isEmpty(tmpdays)) {
                            days = Integer.parseInt(tmpdays);
                        }
                        if (!StringUtils.isEmpty(tmphrs)) {
                            hours = Integer.parseInt(tmphrs);
                        }
                        if (!StringUtils.isEmpty(tmpmin)) {
                            minutes = Integer.parseInt(tmpmin);
                        }
                        if (!StringUtils.isEmpty(tmpsec)) {
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
                if (br.containsHTML("You are currently downloading too many files at once")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You are currently downloading too many files at once", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have exceeded the hourly limit for free users", 5 * 60 * 1000l);
                }
            }
            if (errCode == 266) {
                // <strong>Download error (266)</strong><br>This download is not yet ready. Please retry your download and wait for the
                // countdown timer to complete. </div>
                if (tri >= errTries) {
                    // throw new PluginException(LinkStatus.ERROR_FATAL, errMsg);
                    // want to see this issue reported to statserv so I can monitor / report back to admin!
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "266 IS STILL HAPPENING!");
                } else {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "This download is not yet ready", 2 * 60 * 1000l);
                }
            }
        }
        if (errCode == 265) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The requested Download URL was invalid.  Please retry your download", 5 * 60 * 1000l);
        } else if (errCode == 263) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Could not retrieve information about your download, or your download key has expired. Please try again. ", 5 * 60 * 1000l);
        } else if (freeDownload) {
            if (br.containsHTML(CAPTCHALIMIT)) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
            } else if (br.containsHTML(NO_SLOT) || errCode == 257) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, NO_SLOT_USERTEXT, 10 * 60 * 1000l);
            } else if (br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0) != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(br.getRegex("Please wait (\\d+) minutes to download more files, or").getMatch(0)) * 60 * 1001l);
            }
        }
        if (errCode == 274) {
            // <h2>File Unavailable</h2>
            // <p>
            // This file cannot be downloaded at this time. Please let us know about this issue by using the contact link below. </p>
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file cannot be downloaded at this time.", 20 * 60 * 1000l);
        } else if (br.containsHTML(SERVERFAIL)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l);
        } else if (br.containsHTML(NOT_AVAILABLE)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 20 * 60 * 1000l);
        } else if (br.containsHTML(DBCONNECTIONFAILED)) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        } else if (postDownload && br.containsHTML("Unfortunately we have encountered a problem locating your file")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    /** Handles errors according to: https://api.filefactory.com/#appendix-error-matrix */
    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account, final String apiKey) throws PluginException {
        if ("error".equalsIgnoreCase(PluginJSonUtils.getJsonValue(br, "type"))) {
            final String errorcodeStr = PluginJSonUtils.getJsonValue(br, "code");
            String errormessage = getErrormsgAPI(br);
            if (StringUtils.isEmpty(errormessage)) {
                errormessage = "Unknown API error";
            }
            final int errorcode = Integer.parseInt(errorcodeStr);
            switch (errorcode) {
            case 1:
                /*
                 * 2020-03-11: {"type":"error","message":"File cannot be called directly","code":1} --> Undocumented errorcode --> Short
                 * waittime
                 */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 1 * 60 * 1000);
            case 700:
                /* This should never happen */
                // ERR_API_INVALID_METHOD
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 701:
                /* This should never happen */
                // ERR_API_INTERNAL_ERROR
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 702:
                if ("Invalid authorization key".equals(errormessage)) {
                    useAPI.set(false);
                }
                /* This should never happen */
                // ERR_API_REQ_MALFORMED
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 703:
                /* This should never happen */
                // ERR_API_REQ_MISSING_PARAM
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 704:
                /* This should never happen */
                // ERR_API_REQ_LIMIT
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errormessage);
            case 705:
                // 705 ERR_API_LOGIN_ATTEMPTS
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 706:
                // 706 ERR_API_LOGIN_FAILED
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 707:
                // 707 ERR_API_ACCOUNT_DELETED Account has been deleted, or is pending deletion
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            case 708:
                // ERR_API_PREMIUM_REQUIRED
                throw new AccountRequiredException();
            case 709:
                /* This should never happen */
                // ERR_API_SESS_KEY_MISSING
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 710:
                /* This should never happen */
                // ERR_API_SESS_KEY_INVALID
                clearApiKey(account, apiKey);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            case 711:
                /* This should never happen */
                // ERR_API_SESS_KEY_EXPIRED
                clearApiKey(account, apiKey);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            case 712:
                // ERR_API_FILE_INVALID
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 713:
                // ERR_API_FILE_OFFLINE --> The requested file is temporarily unavailable due to system maintenance, etc
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 714:
                // ERR_API_FILE_SERVER_LOAD
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            case 715:
                // ERR_API_PASSWORD_REQUIRED
                /* This gets handled in another place */
                break;
            case 716:
                // ERR_API_PASSWORD_INVALID
                if (link != null) {
                    link.setDownloadPassword(null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            case 717:
                // ERR_API_PASSWORD_ATTEMPTS --> Too many failed password attempts. Try again in 5 minutes
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage, 6 * 60 * 1001l);
            case 718:
                // 718 ERR_API_IP_SUSPENDED
                if (account != null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errormessage);
                }
            case 719:
                // 719 ERR_API_ACCOUNT_SUSPENDED
                throw new PluginException(LinkStatus.ERROR_PREMIUM, errormessage, PluginException.VALUE_ID_PREMIUM_DISABLE);
            default:
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormessage);
            }
        }
        if (accountIsPendingDeletion(this.br)) {
            /* 2019-08-17: No sure whether this is still required & up-to-date */
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "The account you have tried to sign into is pending deletion. Please contact FileFactory support if you require further assistance.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        boolean useAPI = useAPI(null);
        if (useAPI) {
            final Account account = AccountController.getInstance().getValidAccount(getHost());
            useAPI = useAPI(account);
            if (useAPI) {
                final String apiKey = account != null ? getApiKey(account) : null;
                if (!StringUtils.isEmpty(apiKey)) {
                    checkLinks_API(urls, account, apiKey);
                } else {
                    checkLinks_API(urls, null, null);
                }
            }
        }
        if (!useAPI) {
            final Browser br = new Browser();
            br.setCookiesExclusive(true);
            br.getHeaders().put("Accept-Encoding", "identity");
            // logic to grab account cookie to do fast linkchecking vs one at a time.
            boolean loggedIn = false;
            ArrayList<Account> accounts = AccountController.getInstance().getAllAccounts(this.getHost());
            if (accounts != null && accounts.size() != 0) {
                final Iterator<Account> it = accounts.iterator();
                while (it.hasNext()) {
                    final Account n = it.next();
                    if (n.isEnabled() && n.isValid()) {
                        try {
                            loginWebsite(n, false, br);
                            loggedIn = true;
                            break;
                        } catch (Exception e) {
                            logger.log(e);
                        }
                    }
                }
            }
            if (!loggedIn) {
                // no account present or disabled account, we port back into requestFileInformation
                for (final DownloadLink link : urls) {
                    try {
                        link.setAvailableStatus(requestFileInformationWebsite(null, link));
                    } catch (PluginException e) {
                        logger.log(e);
                        if (e.getLinkStatus() == LinkStatus.ERROR_FILE_NOT_FOUND) {
                            link.setAvailable(false);
                        } else {
                            return false;
                        }
                    } catch (Throwable e) {
                        logger.log(e);
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
                    links.clear();
                    while (true) {
                        if (index == urls.length || links.size() == 100) {
                            break;
                        } else {
                            links.add(urls[index]);
                            index++;
                        }
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
                    br.setFollowRedirects(false);
                    br.setCurrentURL("https://www." + this.getHost() + "/account/tools/link-checker.php");
                    br.postPage("https://www." + this.getHost() + "/account/tools/link-checker.php", sb.toString());
                    final String trElements[] = br.getRegex("<tr>\\s*(.*?)\\s*</tr>").getColumn(0);
                    for (final DownloadLink dl : links) {
                        String name;
                        if (dl.isNameSet()) {
                            name = dl.getName();
                        } else {
                            name = new Regex(dl.getDownloadURL(), "filefactory\\.com/(.+)").getMatch(0);
                        }
                        try {
                            if (br.getRedirectLocation() != null && (br.getRedirectLocation().endsWith("/member/setpwd.php") || br.getRedirectLocation().endsWith("/member/setdob.php"))) {
                                // password needs changing or dob needs setting.
                                dl.setAvailable(true);
                                continue;
                            }
                            final String fileID = getFUID(dl);
                            String fileElement = null;
                            for (final String trElement : trElements) {
                                if (new Regex(trElement, ">\\s*(" + Pattern.quote(fileID) + ".*?</small>\\s*</span>)").getMatch(0) != null) {
                                    fileElement = trElement;
                                    break;
                                }
                            }
                            if (fileElement == null) {
                                dl.setAvailable(false);
                            } else {
                                final String size = new Regex(fileElement, "Size:\\s*([\\d\\.]+\\s*(KB|MB|GB|TB))").getMatch(0);
                                if (size != null) {
                                    dl.setDownloadSize(SizeFormatter.getSize(size));
                                }
                                String elementName = new Regex(fileElement, "Filename:\\s*(.*?)\\s*<br>").getMatch(0);
                                if (elementName != null) {
                                    // Temporary workaround because they don't show full filenames (yet)
                                    elementName = elementName.replace("_rar", ".rar");
                                    elementName = elementName.replace("_zip", ".zip");
                                    elementName = elementName.replace("_avi", ".avi");
                                    elementName = elementName.replace("_mkv", ".mkv");
                                    elementName = elementName.replace("_mp4", ".mp4");
                                    name = elementName;
                                }
                                if (fileElement.matches("(?s).*>\\s*Valid\\s*</abbr>.*")) {
                                    dl.setAvailable(true);
                                } else {
                                    dl.setAvailable(false);
                                }
                            }
                        } finally {
                            if (name != null) {
                                dl.setName(name.trim());
                            }
                        }
                    }
                    if (index == urls.length) {
                        break;
                    }
                }
            } catch (final Exception e) {
                logger.log(e);
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
            throw new AccountInvalidException("Please enter your E-Mail address as username!");
        } else if (useAPI(account)) {
            ai = fetchAccountInfo_API(account, ai);
        } else {
            loginWebsite(account, true, br);
            if (br.getURL() == null || !br.getURL().endsWith("/account/")) {
                br.getPage("https://www." + this.getHost() + "/account/");
            }
            // <li class="tooltipster" title="Premium valid until: <strong>30th Jan, 2014</strong>">
            if (!br.containsHTML("title=\"(Premium valid until|Lifetime Member)") && !br.containsHTML("<strong>Lifetime</strong>")) {
                ai.setUnlimitedTraffic();
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
                if (br.containsHTML(">Lifetime Member<") || br.containsHTML("<strong>Lifetime</strong>")) {
                    ai.setValidUntil(-1);
                    ai.setStatus("Lifetime User");
                } else {
                    final String expire = br.getRegex("Premium valid until: <strong>(.*?)</strong>").getMatch(0);
                    if (expire == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    // remove st/nd/rd/th
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.replaceFirst("(st|nd|rd|th)", ""), "dd MMM, yyyy", Locale.UK));
                    final String space = br.getRegex("<strong>([0-9\\.]+ ?(KB|MB|GB|TB))</strong>[\r\n\t ]+Free Space").getMatch(0);
                    if (space != null) {
                        ai.setUsedSpace(space);
                    }
                    final String traffic = br.getRegex("donoyet(.*?)xyz").getMatch(0);
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
                }
            }
        }
        return ai;
    }

    private boolean isMail(final String parameter) {
        return parameter.matches(".+@.+");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/legal/terms.php";
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

    public String getUrl() throws Exception {
        String url = br.getRegex("\"(https?://[a-z0-9\\-]+\\.filefactory\\.com/dl/[^<>\"]*?)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("id=\"downloadLinkTarget\" style=\"display: none;\">[\t\n\r ]+<a href=\"(https?://[^<>\"]*?)\"").getMatch(0);
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
                br.getPage("https://www." + this.getHost() + link);
                final String[] row = br.getRegex("var (.*?) = '';(.*;) (.*?)=(.*?)\\(\\);").getRow(0);
                result = engine.eval(row[1] + row[3] + " ();");
                if (result.toString().startsWith("http")) {
                    url = result + "";
                } else {
                    url = "https://www." + this.getHost() + result;
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return url;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        // reset setter
        link.setProperty(dlRedirects, Property.NULL);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        final Account account = null;
        if (useAPI(account)) {
            handleDownload_API(link, account);
        } else {
            requestFileInformationWebsite(account, link);
            if (br.getURL().contains(TRAFFICSHARELINK) || br.containsHTML(TRAFFICSHARETEXT)) {
                handleTrafficShare(link, account);
            } else {
                doFree(link, account);
            }
        }
    }

    public void doFree(final DownloadLink link, final Account account) throws Exception {
        final String directlinkproperty;
        if (account == null) {
            directlinkproperty = "directurl_free";
        } else if (account.getType() == AccountType.PREMIUM) {
            directlinkproperty = "directurl_account_premium";
        } else {
            directlinkproperty = "directurl_account_free";
        }
        if (StringUtils.isEmpty(dlUrl)) {
            dlUrl = this.checkDirectLink(link, directlinkproperty);
        }
        String passCode = link.getDownloadPassword();
        try {
            long waittime;
            if (dlUrl != null) {
                logger.finer("DIRECT free-download (or saved directurl)");
                br.setFollowRedirects(true);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlUrl, true, 1);
            } else {
                checkErrorsWebsite(true, false);
                if (br.containsHTML(PASSWORDPROTECTED)) {
                    if (passCode == null) {
                        passCode = getUserInput("Password?", link);
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
                dlUrl = br.getRegex("\"(https?://[a-z0-9\\-]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                String timer = br.getRegex("<div id=\"countdown_clock\" data-delay=\"(\\d+)").getMatch(0);
                if (timer != null) {
                    sleep(Integer.parseInt(timer) * 1001, link);
                }
                if (dlUrl != null) {
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlUrl, true, 1);
                } else {
                    // old
                    String urlWithFilename = null;
                    if (br.getRegex("Recaptcha\\.create\\(([\r\n\t ]+)?\"([^\"]+)").getMatch(1) != null) {
                        urlWithFilename = handleRecaptcha(link);
                    } else {
                        urlWithFilename = getUrl();
                    }
                    if (urlWithFilename == null) {
                        logger.warning("getUrl is broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    br.getPage(urlWithFilename);
                    // Sometimes there is an ad
                    final String skipAds = br.getRegex("\"(https?://(www\\.)?filefactory\\.com/dlf/[^<>\"]+)\"").getMatch(0);
                    if (skipAds != null) {
                        br.getPage(skipAds);
                    }
                    checkErrorsWebsite(true, false);
                    String wait = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
                    if (wait != null) {
                        waittime = Long.parseLong(wait) * 1000l;
                        if (waittime > 60000) {
                            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittime);
                        }
                    }
                    dlUrl = getUrl();
                    if (dlUrl == null) {
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
                    sleep(waittime, link);
                    br.setFollowRedirects(true);
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlUrl);
                }
            }
            // Prüft ob content disposition header da sind
            if (!dl.getConnection().isContentDisposition()) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                checkErrorsWebsite(true, true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                link.setDownloadPassword(passCode);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
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
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        // reset setter
        link.setProperty(dlRedirects, Property.NULL);
        if (useAPI(account)) {
            handleDownload_API(link, account);
        } else {
            requestFileInformationWebsite(account, link);
            if (br.getURL().contains(TRAFFICSHARELINK) || br.containsHTML(TRAFFICSHARETEXT)) {
                handleTrafficShare(link, account);
            } else {
                loginWebsite(account, false, br);
                if (AccountType.FREE == account.getType()) {
                    br.setFollowRedirects(true);
                    br.getPage(link.getDownloadURL());
                    if (checkShowFreeDialog(getHost())) {
                        showFreeDialog(getHost());
                    }
                    doFree(link, account);
                } else {
                    // NOTE: no premium, pre download password handling yet...
                    br.setFollowRedirects(false);
                    br.getPage(link.getDownloadURL());
                    // Directlink
                    String finallink = br.getRedirectLocation();
                    // No directlink
                    if (finallink == null) {
                        finallink = br.getRegex("\"(https?://[a-z0-9]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                        if (finallink == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    br.setFollowRedirects(true);
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finallink, true, 0);
                    if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                        try {
                            br.followConnection(true);
                        } catch (final IOException ignore) {
                            logger.log(ignore);
                        }
                        checkErrorsWebsite(false, true);
                        String red = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                        if (red == null) {
                            red = br.getRegex("subPremium.*?ready.*?<a href=\"(.*?)\"").getMatch(0);
                            if (red == null) {
                                red = br.getRegex("downloadLink.*?href\\s*=\\s*\"(.*?)\"").getMatch(0);
                                if (red == null) {
                                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                                }
                            }
                        }
                        logger.finer("Indirect download");
                        br.setFollowRedirects(true);
                        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, red, true, 0);
                        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                            try {
                                br.followConnection(true);
                            } catch (final IOException ignore) {
                                logger.log(ignore);
                            }
                            checkErrorsWebsite(false, true);
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

    public void handleTrafficShare(final DownloadLink link, final Account account) throws Exception {
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
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalLink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            checkErrorsWebsite(isFree, true);
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
        } else if (!br.containsHTML("status\":\"ok")) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        String url = br.getRegex("path\":\"(.*?)\"").getMatch(0);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        url = url.replaceAll("\\\\/", "/");
        if (url.startsWith("http")) {
            return url;
        } else {
            return "https://www." + this.getHost() + url;
        }
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    private boolean loginWebsite(final Account account, final boolean force, final Browser lbr) throws Exception {
        synchronized (account) {
            try {
                setBrowserExclusive();
                prepBrowser(lbr);
                lbr.getHeaders().put("Accept-Encoding", "gzip");
                lbr.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    lbr.setCookies(this.getHost(), cookies);
                    if (!force) {
                        return false;
                    } else {
                        lbr.getPage("https://www." + this.getHost() + "/");
                        if (lbr.containsHTML(LOGIN_ERROR) || lbr.getCookie(lbr.getHost(), "auth", Cookies.NOTDELETEDPATTERN) == null || (lbr.getURL() != null && lbr.getURL().contains("/error.php?code=152"))) {
                            lbr.clearCookies(getHost());
                        } else {
                            account.saveCookies(lbr.getCookies(lbr.getHost()), "");
                            return false;
                        }
                    }
                }
                lbr.getPage("https://www." + this.getHost() + "/member/signin.php");
                lbr.postPage("/member/signin.php", "loginEmail=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()) + "&Submit=Sign+In");
                if (lbr.containsHTML(LOGIN_ERROR) || lbr.getCookie(lbr.getHost(), "auth", Cookies.NOTDELETEDPATTERN) == null || (lbr.getURL() != null && lbr.getURL().contains("/error.php?code=152"))) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(lbr.getCookies(lbr.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                if (StringUtils.containsIgnoreCase(lbr.getRedirectLocation(), "code=105") || StringUtils.containsIgnoreCase(lbr.getURL(), "code=105")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "The account you have tried to sign into is pending deletion. Please contact FileFactory support if you require further assistance.", PluginException.VALUE_ID_PREMIUM_DISABLE, e);
                } else {
                    throw e;
                }
            }
        }
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account, final String apiKey) throws Exception {
        correctDownloadLink(link);
        if (!checkLinks_API(new DownloadLink[] { link }, account, apiKey) || !link.isAvailabilityStatusChecked()) {
            link.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        } else if (!link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return getAvailableStatus(link);
    }

    private boolean useAPI(final Account account) {
        return useAPI.get();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (useAPI(account)) {
            final String apiKey = account != null ? getApiKey(account) : null;
            if (!StringUtils.isEmpty(apiKey)) {
                return requestFileInformationAPI(link, account, apiKey);
            } else {
                return requestFileInformationAPI(link, null, null);
            }
        } else {
            return requestFileInformationWebsite(account, link);
        }
    }

    public AvailableStatus requestFileInformationWebsite(final Account account, final DownloadLink link) throws Exception {
        setBrowserExclusive();
        prepBrowser(br);
        fuid = getFUID(link);
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
                con = br.openGetConnection(link.getDownloadURL());
                if (this.looksLikeDownloadableContent(con)) {
                    link.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    link.setDownloadSize(con.getLongContentLength());
                    con.disconnect();
                    dlUrl = link.getDownloadURL();
                    link.setAvailable(true);
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(link.getDownloadURL());
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
        } else if (br.containsHTML("This file is no longer available due to an unexpected server error") || br.getURL().contains("error.php?code=252")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(NOT_AVAILABLE) && !br.containsHTML(NO_SLOT)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(SERVER_DOWN)) {
            return AvailableStatus.UNCHECKABLE;
        } else if (br.containsHTML(PASSWORDPROTECTED)) {
            final String fileName = br.getRegex("<title>([^<>\"]*?)- FileFactory</title>").getMatch(0);
            if (fileName == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setName(Encoding.htmlDecode(fileName.trim()));
            link.getLinkStatus().setStatusText("This link is password protected");
            link.setAvailable(true);
        } else {
            if (isPremiumOnly(br)) {
                link.getLinkStatus().setErrorMessage("This file is only available to Premium Members");
                link.getLinkStatus().setStatusText("This file is only available to Premium Members");
            } else if (br.containsHTML(NO_SLOT) || br.getURL().contains("error.php?code=257")) {
                link.getLinkStatus().setErrorMessage(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", NO_SLOT_USERTEXT));
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.filefactorycom.errors.nofreeslots", NO_SLOT_USERTEXT));
            } else if (br.containsHTML("Server Maintenance")) {
                link.getLinkStatus().setStatusText("Server Maintenance");
            } else {
                String fileName = null;
                String fileSize = null;
                if (br.containsHTML("File Not Found")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (br.getURL().contains(TRAFFICSHARELINK)) {
                    /* 2022-11-07: check me */
                    fileName = br.getRegex("<section class=\"file\" style=\"margin-top:20px;\">[\t\n\r ]+<h2>([^<>\"]+)</h2>").getMatch(0);
                    if (fileName == null) {
                        fileName = br.getRegex("<h2>(.*?)</h2>").getMatch(0);
                    }
                    fileSize = br.getRegex("id=\"file_info\">([\\d\\.]+ (KB|MB|GB|TB))").getMatch(0);
                } else {
                    fileName = br.getRegex("class\\s*=\\s*\"file-name\"[^>]*>\\s*(.*?)\\s*<").getMatch(0);
                    if (fileName == null) {
                        fileName = br.getRegex("<title>\\s*([^<>\"]*?)\\s*-\\s*FileFactory\\s*</title>").getMatch(0);
                    }
                    fileSize = br.getRegex("id\\s*=\\s*\"file_info\"[^>]*>\\s*([0-9\\.]+\\s*(KB|MB|GB|TB))").getMatch(0);
                }
                if (fileName == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    link.setName(Encoding.htmlDecode(fileName.trim()));
                    if (fileSize != null) {
                        link.setDownloadSize(SizeFormatter.getSize(fileSize));
                    }
                    link.setAvailable(true);
                }
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

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account account) {
        return false;
    }

    private String getFUID(final DownloadLink link) {
        final String fuid = new Regex(link.getDownloadURL(), "file/([\\w]+)").getMatch(0);
        return fuid;
    }

    private String getApiBase() {
        return "https://api.filefactory.com/v1";
    }

    private boolean checkLinks_API(final DownloadLink[] urls, Account account, String apiKey) {
        try {
            final Browser br = new Browser();
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    if (links.size() == 100 || index == urls.length) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                sb.append("file=");
                for (final DownloadLink dl : links) {
                    sb.append(getFUID(dl));
                    sb.append(",");
                }
                // lets remove last ","
                sb.replace(sb.length() - 1, sb.length(), "");
                try {
                    getPage(br, getApiBase() + "/getFileInfo?" + sb, null, account, apiKey);
                } catch (final PluginException e) {
                    if (e.getLinkStatus() == LinkStatus.ERROR_RETRY) {
                        logger.log(e);
                        getPage(br, getApiBase() + "/getFileInfo?" + sb, null, null, null);
                    } else {
                        throw e;
                    }
                }
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
            logger.log(e);
            return false;
        }
        return true;
    }

    /* 2022-11-07: disabled due to deactivated api key */
    private static AtomicBoolean useAPI  = new AtomicBoolean(false);
    private String               fuid    = null;
    private String               dllink  = null;
    private int                  chunks  = 0;
    private boolean              resumes = true;
    private boolean              isFree  = true;

    private void setConstants(final Account account, final boolean trafficShare) {
        if (trafficShare) {
            // traffic share download
            chunks = 0;
            resumes = true;
            isFree = false;
            logger.finer("setConstants = Traffic Share Download :: isFree = " + isFree + ", upperChunks = " + chunks + ", Resumes = " + resumes);
        } else {
            if (account != null) {
                if (AccountType.FREE == account.getType()) {
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

    private void handleDownload_API(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, false);
        fuid = getFUID(link);
        prepApiBrowser(br);
        String apiKey = null;
        if (account != null) {
            synchronized (account) {
                apiKey = getApiKey(account);
                if (StringUtils.isEmpty(apiKey)) {
                    apiKey = loginAPI(account);
                }
            }
            if (StringUtils.isEmpty(apiKey)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        requestFileInformationAPI(link, account, apiKey);
        String passCode = link.getDownloadPassword();
        final String directlinkproperty;
        if (account == null) {
            directlinkproperty = "directurl_free";
        } else if (account.getType() == AccountType.PREMIUM) {
            directlinkproperty = "directurl_account_premium";
        } else {
            directlinkproperty = "directurl_account_free";
        }
        br.setFollowRedirects(true);
        this.dllink = this.checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            if (link.getBooleanProperty("premiumRequired", false) && isFree) {
                // free dl isn't possible, place before passwordRequired!
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            }
            if (link.getBooleanProperty("passwordRequired", false)) {
                // dl requires pre download password
                if (StringUtils.isEmpty(passCode)) {
                    passCode = getUserInput("Password Required!", link);
                }
                if (StringUtils.isEmpty(passCode)) {
                    logger.info("User has entered blank password!");
                    link.setDownloadPassword(null);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Invalid password", 1 * 60 * 1001);
                }
            }
            getPage(br, getApiBase() + "/getDownloadLink?file=" + fuid + (!StringUtils.isEmpty(passCode) ? "&password=" + Encoding.urlEncode(passCode) : ""), link, account, apiKey);
            dllink = PluginJSonUtils.getJsonValue(br, "url");
            final String linkType = PluginJSonUtils.getJsonValue(br, "linkType");
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadlink");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if ("trafficshare".equalsIgnoreCase(linkType)) {
                setConstants(account, true);
            }
            String delay = PluginJSonUtils.getJsonValue(br, "delay");
            if (!StringUtils.isEmpty(passCode)) {
                link.setDownloadPassword(passCode);
            }
            if (!StringUtils.isEmpty(delay)) {
                final int s = Integer.parseInt(delay);
                sleep((s * 1001) + 1111, link);
            }
        }
        handleDL(link, account, directlinkproperty);
    }

    private static final String dlRedirects = "dlRedirects";

    private void handleDL(final DownloadLink link, final Account account, final String directlinkproperty) throws Exception {
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
                if (!this.looksLikeDownloadableContent(con) && br.getRedirectLocation() != null) {
                    // redirect, we want to store and continue down the rabbit hole!
                    final String redirect = br.getRedirectLocation();
                    urls.add(redirect);
                    continue;
                } else if (!this.looksLikeDownloadableContent(con)) {
                    // error final destination/html
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
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
            link.setProperty(dlRedirects, urls);
        }
        if (!this.looksLikeDownloadableContent(con)) {
            checkErrorsWebsite(isFree, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resumes, chunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            // this shouldn't happen anymore!
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            checkErrorsWebsite(isFree, true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(directlinkproperty, dllink);
        // add download slot
        controlSlot(+1, account);
        try {
            dl.startDownload();
        } finally {
            // remove download slot
            controlSlot(-1, account);
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private String loginAPI(final Account account) throws Exception {
        synchronized (account) {
            prepApiBrowser(this.br);
            /* First try to login with previous session/apikey */
            String apikey = this.getApiKey(account);
            boolean loggedIN = false;
            if (!StringUtils.isEmpty(apikey)) {
                logger.info("Trying to re-use previous apikey");
                this.br.getPage(getApiBase() + "/getMemberInfo?key=" + apikey);
                br.followRedirect();
                loggedIN = !sessionKeyInvalid(account, this.br, apikey);
                if (loggedIN) {
                    logger.info("Successfully loggedin via previous apikey");
                } else {
                    logger.info("Failed to login via previous apikey");
                }
            }
            if (!loggedIN) {
                logger.info("Performing full login");
                /*
                 * 2019-08-16: According to their API documentation, the sessionkey/apikey is valid 15 minutes from its' first generation.
                 * It will be renewed to 15 minutes every time it gets used!
                 */
                final LogInterface logger = br.getLogger();
                try {
                    if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                        br.setLogger(new NullLogger());
                    }
                    this.br.getPage(getApiBase() + "/getSessionKey?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&authkey=" + getAuthKey());
                } finally {
                    br.setLogger(logger);
                }
                br.followRedirect();
                apikey = PluginJSonUtils.getJsonValue(this.br, "key");
                if (StringUtils.isNotEmpty(apikey)) {
                    account.setProperty(PROPERTY_APIKEY, apikey);
                    return apikey;
                } else {
                    checkErrorsAPI(this.br, null, account, apikey);
                }
            }
            return apikey;
        }
    }

    private String getApiKey(final Account account) {
        synchronized (account) {
            final String ret = account.getStringProperty(PROPERTY_APIKEY, null);
            if (StringUtils.isEmpty(ret)) {
                return null;
            } else {
                return ret;
            }
        }
    }

    private Browser prepApiBrowser(final Browser ibr) {
        ibr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        return ibr;
    }

    private boolean accountIsPendingDeletion(Browser br) {
        if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "code=105") || StringUtils.containsIgnoreCase(br.getURL(), "code=105")) {
            return true;
        } else if (StringUtils.containsIgnoreCase(br.getRedirectLocation(), "code=152") || StringUtils.containsIgnoreCase(br.getURL(), "code=152")) {
            return true;
        } else {
            return false;
        }
    }

    private void getPage(final Browser ibr, final String url, final DownloadLink downloadLink, final Account account, final String apiKey) throws Exception {
        if (account != null) {
            if (StringUtils.isEmpty(apiKey)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                ibr.getPage(url + (url.matches("(" + getApiBase() + ")?/[a-zA-Z0-9]+\\?[a-zA-Z0-9]+.+") ? "&" : "?") + "key=" + apiKey);
            }
        } else {
            ibr.getPage(url);
        }
        ibr.followRedirect();
        this.checkErrorsAPI(ibr, downloadLink, account, apiKey);
    }

    private String getErrormsgAPI(final Browser ibr) {
        final String message = PluginJSonUtils.getJsonValue(ibr, "message");
        if (message != null) {
            logger.warning(message);
            return message;
        } else {
            return null;
        }
    }

    private boolean clearApiKey(final Account account, final String apiKey) {
        if (account != null) {
            synchronized (account) {
                if (StringUtils.isEmpty(apiKey) || StringUtils.equals(apiKey, account.getStringProperty(PROPERTY_APIKEY, null))) {
                    account.removeProperty(PROPERTY_APIKEY);
                    return true;
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    private boolean sessionKeyInvalid(final Account account, final Browser ibr, final String apiKey) {
        final String response_type = PluginJSonUtils.getJsonValue(ibr, "type");
        final String errorcodeStr = PluginJSonUtils.getJsonValue(ibr, "code");
        if ("error".equalsIgnoreCase(response_type) && ("710".equalsIgnoreCase(errorcodeStr) || "711".equalsIgnoreCase(errorcodeStr))) {
            // 710 ERR_API_SESS_KEY_INVALID The session key has expired or is invalid. Please obtain a new one via getSessionKey.
            // 711 ERR_API_LOGIN_EXPIRED The session key has expired. Please obtain a new one via getSessionKey.
            clearApiKey(account, apiKey);
            return true;
        } else {
            return false;
        }
    }

    private AccountInfo fetchAccountInfo_API(final Account account, final AccountInfo ai) throws Exception {
        if (StringUtils.isEmpty(account.getPass())) {
            return ai;
        }
        final String apiKey = loginAPI(account);
        if (br.getURL() == null || !br.getURL().contains("/getMemberInfo")) {
            /* E.g. on full login we've already done this API call before! */
            getPage(br, getApiBase() + "/getMemberInfo", null, account, apiKey);
        }
        /*
         * 2020-03-11: Workaround for API issue {"type":"error","message":"File cannot be called directly","code":1} --> Try to let existing
         * accounts stay active, avoid displaying premium accounts as free.
         */
        final AccountInfo oldAccountInfo = account.getAccountInfo();
        final long last_checked_timestamp = account.getLongProperty("last_checked_timestamp", 0);
        final long account_last_checked_time_ago = System.currentTimeMillis() - last_checked_timestamp;
        try {
            this.checkErrorsAPI(br, null, account, apiKey);
        } catch (final Exception e) {
            logger.log(e);
            logger.info("API error happened");
            if (oldAccountInfo != null && account_last_checked_time_ago <= 3 * 60 * 60 * 1000) {
                logger.info("Returning old AccountInfo");
                return oldAccountInfo;
            }
            throw e;
        }
        final String expire = PluginJSonUtils.getJsonValue(br, "expiryMs");
        final String type = PluginJSonUtils.getJsonValue(br, "accountType");
        if ("premium".equalsIgnoreCase(type)) {
            account.setType(AccountType.PREMIUM);
            account.setProperty("totalMaxSim", 20);
            account.setMaxSimultanDownloads(20);
            ai.setStatus("Premium Account");
            if (expire != null) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expire), br);
            }
        } else {
            account.setType(AccountType.FREE);
            account.setProperty("totalMaxSim", 20);
            account.setMaxSimultanDownloads(20);
            ai.setStatus("Free Account");
            ai.setUnlimitedTraffic();
        }
        account.setProperty("last_checked_timestamp", System.currentTimeMillis());
        return ai;
    }

    private AvailableStatus getAvailableStatus(DownloadLink link) {
        try {
            final Field field = link.getClass().getDeclaredField("availableStatus");
            field.setAccessible(true);
            final Object ret = field.get(link);
            if (ret != null && ret instanceof AvailableStatus) {
                return (AvailableStatus) ret;
            }
        } catch (final Throwable e) {
        }
        return AvailableStatus.UNCHECKED;
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
     */
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