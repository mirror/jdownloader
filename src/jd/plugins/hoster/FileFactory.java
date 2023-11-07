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

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import org.appwork.loggingv3.NullLogger;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Application;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class FileFactory extends PluginForHost {
    // DEV NOTES
    // other: currently they 302 redirect all non www. to www. which kills most of this plugin.
    // Adjust COOKIE_HOST to suite future changes, or remove COOKIE_HOST from that section of the script.
    // Connection Management
    // note: CAN NOT be negative or zero! (ie. -1 or 0) Otherwise math sections fail. .:. use [1-20]
    private static AtomicInteger totalMaxSimultanFreeDownload     = new AtomicInteger(20);
    private static AtomicInteger maxPrem                          = new AtomicInteger(1);
    private static AtomicInteger maxFree                          = new AtomicInteger(1);
    private final String         PROPERTY_ACCOUNT_APIKEY          = "apiKey";
    private final String         PROPERTY_TRAFFICSHARE            = "trafficshare";
    private final String         PROPERTY_LAST_URL_WITH_ERRORCODE = "last_url_with_errorcode";
    private String               dllink                           = null;
    private static AtomicBoolean useAPI                           = new AtomicBoolean(true);

    public FileFactory(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + this.getHost() + "/info/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/legal/terms.php";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        br.getHeaders().put("Cache-Control", null);
        br.getHeaders().put("Pragma", null);
        br.setReadTimeout(3 * 60 * 1000);
        br.setConnectTimeout(3 * 60 * 1000);
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (isTrafficshareLink(link)) {
            return true;
        } else {
            if (this.isPremiumAccount(account)) {
                /* Premium account */
                return true;
            } else {
                /* Free(anonymous) and unknown account type */
                return false;
            }
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        if (isTrafficshareLink(link)) {
            return 0;
        } else {
            if (this.isPremiumAccount(account)) {
                /* Premium account */
                return 0;
            } else {
                /* Free(anonymous) and unknown account type */
                return 1;
            }
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFUID(link);
        if (fid != null) {
            return getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
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

    public void checkErrorsWebsite(final DownloadLink link, final Browser br) throws PluginException {
        if (br.containsHTML("class=\"box error\"|have been deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (isPremiumOnly(br)) {
            throw new AccountRequiredException();
        }
        // this should cover error codes jumping to stream links in redirect, since filefactory wont fix this issue, this is my workaround.
        String code = getErrorcodeFromURL(br.getURL());
        if (code == null) {
            code = link != null ? this.getErrorcodeFromURL(link.getStringProperty(PROPERTY_LAST_URL_WITH_ERRORCODE)) : null;
        }
        long waittimeFromHTMLMillis = -1;
        final String waittimeString = br.getRegex("Please try again in\\s*<span>(.*?)</span>").getMatch(0);
        if (waittimeString != null) {
            /* That usually goes along with error 275 */
            logger.info("Found possible IP limit wait string: " + waittimeString);
            String tmpYears = new Regex(waittimeString, "(\\d+)\\s+years?").getMatch(0);
            String tmpdays = new Regex(waittimeString, "(\\d+)\\s+days?").getMatch(0);
            String tmphrs = new Regex(waittimeString, "(\\d+)\\s+hours?").getMatch(0);
            String tmpmin = new Regex(waittimeString, "(\\d+)\\s+min(ute)?s?").getMatch(0);
            String tmpsec = new Regex(waittimeString, "(\\d+)\\s+sec(ond)?s?").getMatch(0);
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
            waittimeFromHTMLMillis = ((years * 86400000 * 365) + (days * 86400000) + (hours * 3600000) + (minutes * 60000) + (seconds * 1000));
        }
        final int errcode = (code != null && code.matches("\\d+") ? Integer.parseInt(code) : -1);
        // final String errormessage = br.getRegex("id=\"wp-body-box-message\"[^>]*>([^<]+)").getMatch(0);
        /* Error-Code based errors */
        if (errcode == 152) {
            throw new AccountInvalidException("The account you have tried to sign into is pending deletion. Please contact FileFactory support if you require further assistance.");
        } else if (errcode == 251) {
            /* Invalid downloadlink */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (errcode == 252) {
            /* File is not available anymore due to an unexpected error (= also permanently offline) */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (errcode == 254) {
            /* File does not exist anymore */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (errcode == 257) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "No free slots available", 10 * 60 * 1000l);
        } else if (errcode == 263) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Could not retrieve information about your download, or your download key has expired. Please try again. ", 5 * 60 * 1000l);
        } else if (errcode == 265) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "The requested Download URL was invalid.  Please retry your download", 5 * 60 * 1000l);
        } else if (errcode == 266) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error 266: This download is not yet ready", 2 * 60 * 1000l);
        } else if (errcode == 274) {
            // <h2>File Unavailable</h2>
            // <p>
            // This file cannot be downloaded at this time. Please let us know about this issue by using the contact link below. </p>
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file cannot be downloaded at this time.", 20 * 60 * 1000l);
        } else if (errcode == 275 || br.containsHTML("You are currently downloading too many files at once") || br.containsHTML(">\\s*You have recently started a download")) {
            final long waitMillis;
            if (waittimeFromHTMLMillis == -1) {
                waitMillis = 5 * 60 * 1000l;
            } else {
                waitMillis = waittimeFromHTMLMillis;
            }
            if (br.containsHTML("You are currently downloading too many files at once")) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You are currently downloading too many files at once", waitMillis);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "You have exceeded the hourly limit for free users", waitMillis);
            }
        }
        if (waittimeFromHTMLMillis != -1) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittimeFromHTMLMillis);
        }
        /* Text based errors */
        if (br.containsHTML("(?i)<p>\\s*We have detected several recent attempts to bypass our free download restrictions originating from your IP Address")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "We have detected several recent attempts to bypass our free download restrictions originating from your IP Address", 10 * 60 * 1000l);
        } else if (br.containsHTML("(<p>Your download slot has expired\\.|temporarily unavailable)")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 20 * 60 * 1000l);
        } else if (br.containsHTML("server hosting the file you are requesting is currently down")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server hosting the file you are requesting is currently down", 20 * 60 * 1000l);
        } else if (br.containsHTML("Couldn't get valid connection to DB")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        } else if (br.containsHTML("Unfortunately we have encountered a problem locating your file")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "Unfortunately we have encountered a problem locating your file");
        }
        /* Misc errors */
        final String waitMinutesStr = br.getRegex("Please wait (\\d+) minutes to download more files,\\s*or").getMatch(0);
        if (waitMinutesStr != null) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waitMinutesStr) * 60 * 1001l);
        }
    }

    private String getErrorcodeFromURL(final String url) {
        return new Regex(url, "(?:\\?|&)code=(\\d+)").getMatch(0);
    }

    private final String invalidAuthKey = "Invalid authorization key";

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
                if (invalidAuthKey.equals(errormessage)) {
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
                /* Linkcheck via API */
                final String apiKey = account != null ? getApiKey(account) : null;
                if (!StringUtils.isEmpty(apiKey)) {
                    checkLinks_API(urls, account, apiKey);
                } else {
                    checkLinks_API(urls, null, null);
                }
                return true;
            }
        }
        final Browser br = this.createNewBrowserInstance();
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
        if (loggedIn) {
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
                    for (final DownloadLink link : links) {
                        sb.append(Encoding.urlEncode(this.getContentURL(link)));
                        sb.append("%0D%0A");
                    }
                    // lets remove last "%0D%0A"
                    sb.replace(sb.length() - 6, sb.length(), "");
                    sb.append("&Submit=Check+Links");
                    br.setFollowRedirects(false);
                    br.setCurrentURL("https://www." + this.getHost() + "/account/tools/link-checker.php");
                    br.postPage("https://www." + this.getHost() + "/account/tools/link-checker.php", sb.toString());
                    final String trElements[] = br.getRegex("<tr>\\s*(.*?)\\s*</tr>").getColumn(0);
                    for (final DownloadLink link : links) {
                        String name;
                        if (link.isNameSet()) {
                            name = link.getName();
                        } else {
                            /* Obtain filename from URL */
                            name = new Regex(link.getPluginPatternMatcher(), "(?i)https?://[^/]+/(.+)").getMatch(0);
                        }
                        try {
                            if (br.getRedirectLocation() != null && (br.getRedirectLocation().endsWith("/member/setpwd.php") || br.getRedirectLocation().endsWith("/member/setdob.php"))) {
                                // password needs changing or dob needs setting.
                                link.setAvailable(true);
                                continue;
                            }
                            final String fileID = getFUID(link);
                            /* Search html snippet belonging to the link we are working on to determine online status. */
                            String filehtml = null;
                            for (final String trElement : trElements) {
                                if (new Regex(trElement, ">\\s*(" + Pattern.quote(fileID) + ".*?</small>\\s*</span>)").getMatch(0) != null) {
                                    filehtml = trElement;
                                    break;
                                }
                            }
                            if (filehtml == null) {
                                link.setAvailable(false);
                            } else {
                                final String size = new Regex(filehtml, "Size:\\s*([\\d\\.]+\\s*(KB|MB|GB|TB))").getMatch(0);
                                if (size != null) {
                                    link.setDownloadSize(SizeFormatter.getSize(size));
                                }
                                final String filenameFromHTML = new Regex(filehtml, "Filename:\\s*(.*?)\\s*<br>").getMatch(0);
                                if (filenameFromHTML != null) {
                                    name = Encoding.htmlDecode(filenameFromHTML).trim();
                                }
                                if (filehtml.matches("(?s).*>\\s*(Gültig|Valid)\\s*</abbr>.*")) {
                                    link.setAvailable(true);
                                } else {
                                    link.setAvailable(false);
                                }
                            }
                        } finally {
                            if (name != null) {
                                link.setName(name.trim());
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
        } else {
            // no account present or disabled account -> Fallback into requestFileInformation
            for (final DownloadLink link : urls) {
                try {
                    link.setAvailableStatus(requestFileInformationWebsite(null, link));
                } catch (final PluginException e) {
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
        }
        return true;
    }

    private String getContentURL(final DownloadLink link) throws PluginException {
        if (link == null || link.getPluginPatternMatcher() == null) {
            return null;
        }
        String url = link.getPluginPatternMatcher();
        url = url.replaceFirst("\\.com//", ".com/");
        url = url.replaceFirst("://filefactory", "://www.filefactory");
        url = url.replaceFirst("/stream/", "/file/");
        // set trafficshare links like 'normal' links, this allows downloads to continue living if the uploader discontinues trafficshare
        // for that uid. Also re-format premium only links!
        final Regex trafficshareregex = new Regex(url, "(?i)(https?://.*?filefactory\\.com/)(trafficshare|digitalsales)/[a-f0-9]{32}/([^/]+)/?");
        if (trafficshareregex.patternFind()) {
            String[] uid = trafficshareregex.getRow(0);
            if (uid != null && (uid[0] != null || uid[2] != null)) {
                return uid[0] + "file/" + uid[2];
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return url;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (!isMail(account.getUser())) {
            throw new AccountInvalidException("Please enter your E-Mail address as username!");
        }
        final AccountInfo ai = new AccountInfo();
        if (useAPI(account)) {
            try {
                return fetchAccountInfo_API(account, ai);
            } catch (final PluginException e) {
                if (!useAPI(account) || invalidAuthKey.equals(e.getMessage())) {
                    // auto retry without api
                    logger.log(e);
                    logger.info("API login failed -> Auto try in website mode");
                } else {
                    throw e;
                }
            }
        }
        loginWebsite(account, true, br);
        if (br.getURL() == null || !br.getURL().endsWith("/account/")) {
            br.getPage("https://www." + this.getHost() + "/account/");
        }
        // <li class="tooltipster" title="Premium valid until: <strong>30th Jan, 2014</strong>">
        boolean isPremiumLifetime = false;
        boolean isPremium = false;
        final String accountTypeStr = br.getRegex("member_type\\s*:\\s*\"([^\"]+)").getMatch(0);
        if (accountTypeStr != null) {
            if (accountTypeStr.equalsIgnoreCase("lifetime")) {
                isPremiumLifetime = true;
            } else if (accountTypeStr.equalsIgnoreCase("premium")) {
                // TODO: 2021-01-12: Not sure about this as I didn't have the html code for checking yet!
                isPremium = true;
            }
            /**
             * Other possible values: </br>
             * "expired" -> Free Account
             */
        }
        if (!isPremium && !isPremiumLifetime) {
            /* Fallback/Old handling */
            isPremiumLifetime = br.containsHTML("(?i)<strong>\\s*Lifetime\\s*</strong>") || br.containsHTML("(?i)>\\s*Lifetime Member\\s*<");
            isPremium = br.containsHTML("(?i)>\\s*Premium valid until\\s*<");
        }
        long expireTimestamp = 0;
        final String expireTimestampStr = br.getRegex("premium_ends\\s*:\\s*\"?(\\d+)").getMatch(0);
        if (expireTimestampStr != null) {
            expireTimestamp = Long.parseLong(expireTimestampStr) * 1000;
        } else {
            /* Fallback/Old handling */
            final String expireDateStr = br.getRegex("(?i)Premium valid until\\s*:\\s*<strong>(.*?)</strong>").getMatch(0);
            if (expireDateStr != null) {
                expireTimestamp = TimeFormatter.getMilliSeconds(expireDateStr.replaceFirst("(st|nd|rd|th)", ""), "dd MMM, yyyy", Locale.UK);
            }
        }
        if (isPremium || isPremiumLifetime || expireTimestamp > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            if (isPremiumLifetime) {
                account.setType(AccountType.LIFETIME);
                ai.setValidUntil(-1);
            } else {
                if (expireTimestamp > System.currentTimeMillis()) {
                    ai.setValidUntil(expireTimestamp);
                }
                final String space = br.getRegex("<strong>([0-9\\.]+ ?(KB|MB|GB|TB))</strong>[\r\n\t ]+Free Space").getMatch(0);
                if (space != null) {
                    ai.setUsedSpace(space);
                }
                final String traffic = br.getRegex("donoyet(.*?)xyz").getMatch(0);
                if (traffic != null) {
                    // OLD SHIT
                    String loaded = br.getRegex("(?i)You have used (.*?) out").getMatch(0);
                    String max = br.getRegex("limit of (.*?)\\. ").getMatch(0);
                    if (max != null && loaded != null) {
                        // you don't need to strip characters or reorder its structure. The source is fine!
                        ai.setTrafficMax(SizeFormatter.getSize(max));
                        ai.setTrafficLeft(ai.getTrafficMax() - SizeFormatter.getSize(loaded));
                    } else {
                        max = br.getRegex("(?i)You can now download up to (.*?) in").getMatch(0);
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
        } else {
            ai.setUnlimitedTraffic();
            account.setType(AccountType.FREE);
        }
        final String createTimestampStr = br.getRegex("created_at\\s*:\\s*\"?(\\d+)").getMatch(0);
        if (createTimestampStr != null) {
            ai.setCreateTime(Long.parseLong(createTimestampStr) * 1000);
        }
        return ai;
    }

    private boolean isMail(final String parameter) {
        return parameter.matches(".+@.+");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxFree.get();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* Start downloads sequentially. */
        return maxPrem.get();
    }

    @Override
    public int getTimegapBetweenConnections() {
        return 200;
    }

    /**
     * Returns final downloadurl </br>
     * TODO: 2023-11-03: Check if this is still needed
     */
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
        link.removeProperty(PROPERTY_LAST_URL_WITH_ERRORCODE);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        final Account account = null;
        if (useAPI(account)) {
            handleDownload_API(link, account);
        } else {
            handleFreeWebsite(link, account);
        }
    }

    @Deprecated
    public void handleFreeWebsite(final DownloadLink link, final Account account) throws Exception {
        requestFileInformationWebsite(account, link);
        if (isTrafficshareLink(br)) {
            link.setProperty(PROPERTY_TRAFFICSHARE, true);
            handleTrafficShare(link, account);
            return;
        } else {
            link.removeProperty(PROPERTY_TRAFFICSHARE);
        }
        final String directlinkproperty;
        if (account == null) {
            directlinkproperty = "directurl_free";
        } else if (account.getType() == AccountType.FREE) {
            directlinkproperty = "directurl_account_free";
        } else {
            directlinkproperty = "directurl_account_premium";
        }
        if (StringUtils.isEmpty(dllink)) {
            dllink = this.checkDirectLink(link, directlinkproperty);
        } else {
            /* Maybe trafficshare direct-URL */
        }
        try {
            long waitMillis;
            if (dllink != null) {
                logger.finer("DIRECT free-download (or saved directurl)");
                br.setFollowRedirects(true);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
            } else {
                checkErrorsWebsite(link, br);
                if (isPasswordProtectedFile(br)) {
                    String passCode = link.getDownloadPassword();
                    if (passCode == null) {
                        passCode = getUserInput("Password?", link);
                    }
                    // stable is lame
                    br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    br.postPage(br.getURL(), "password=" + Encoding.urlEncode(passCode) + "&Submit=Continue");
                    br.getHeaders().put("Content-Type", null);
                    if (isPasswordProtectedFile(br)) {
                        throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
                    }
                    link.setDownloadPassword(passCode);
                }
                // new 20130911
                dllink = br.getRegex("\"(https?://[a-z0-9\\-]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                String timer = br.getRegex("<div id=\"countdown_clock\" data-delay=\"(\\d+)").getMatch(0);
                if (timer != null) {
                    sleep(Integer.parseInt(timer) * 1001, link);
                }
                if (dllink != null) {
                    /* Trafficshare link */
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
                } else {
                    // TODO: Check if this is still needed
                    final String urlWithFilename = getUrl();
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
                    checkErrorsWebsite(link, br);
                    String waitSecondsStr = br.getRegex("class=\"countdown\">\\s*(\\d+)\\s*</span>").getMatch(0);
                    if (waitSecondsStr != null) {
                        waitMillis = Long.parseLong(waitSecondsStr) * 1000l;
                        if (waitMillis > 60000) {
                            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitMillis);
                        }
                    }
                    dllink = getUrl();
                    if (dllink == null) {
                        logger.warning("getUrl is broken!");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    waitSecondsStr = br.getRegex("class=\"countdown\">(\\d+)</span>").getMatch(0);
                    waitMillis = 60 * 1000l;
                    if (waitSecondsStr != null) {
                        waitMillis = Long.parseLong(waitSecondsStr) * 1000l;
                    }
                    if (waitMillis > 60000l) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitMillis);
                    }
                    waitMillis += 1000;
                    sleep(waitMillis, link);
                    br.setFollowRedirects(true);
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink);
                }
            }
            if (!dl.getConnection().isContentDisposition()) {
                br.followConnection(true);
                checkErrorsWebsite(link, br);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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
        link.removeProperty(PROPERTY_LAST_URL_WITH_ERRORCODE);
        if (useAPI(account)) {
            try {
                handleDownload_API(link, account);
                return;
            } catch (PluginException e) {
                if (!useAPI(account) || invalidAuthKey.equals(e.getMessage())) {
                    // auto retry without api
                    logger.log(e);
                } else {
                    throw e;
                }
            }
        }
        /* Fallback to website-mode (old code, deprecated) */
        if (this.isPremiumAccount(account)) {
            loginWebsite(account, false, br);
            final String contenturl = this.getContentURL(link);
            // NOTE: no premium, pre download password handling yet...
            br.setFollowRedirects(false);
            br.getPage(contenturl);
            String finallink = br.getRedirectLocation();
            while (finallink != null && canHandle(finallink)) {
                // follow http->https redirect
                br.getPage(finallink);
                finallink = br.getRedirectLocation();
            }
            if (finallink == null) {
                // No directlink
                finallink = br.getRegex("\"(https?://[a-z0-9]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                if (finallink == null) {
                    this.checkErrorsWebsite(link, br);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            br.setFollowRedirects(true);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finallink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                checkErrorsWebsite(link, br);
                String redirecturl = br.getRegex("\"(https?://[a-z0-9]+\\.filefactory\\.com/get/[^<>\"]+)\"").getMatch(0);
                if (redirecturl == null) {
                    redirecturl = br.getRegex(Pattern.compile("10px 0;\">.*<a href=\"(.*?)\">Download with FileFactory Premium", Pattern.DOTALL)).getMatch(0);
                }
                if (redirecturl == null) {
                    redirecturl = br.getRegex("subPremium.*?ready.*?<a href=\"(.*?)\"").getMatch(0);
                    if (redirecturl == null) {
                        redirecturl = br.getRegex("downloadLink.*?href\\s*=\\s*\"(.*?)\"").getMatch(0);
                        if (redirecturl == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
                logger.finer("Indirect download");
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, redirecturl, this.isResumeable(link, account), this.getMaxChunks(link, account));
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    br.followConnection(true);
                    checkErrorsWebsite(link, br);
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
        } else {
            if (checkShowFreeDialog(getHost())) {
                showFreeDialog(getHost());
            }
            this.handleFreeWebsite(link, account);
        }
    }

    public void handleTrafficShare(final DownloadLink link, final Account account) throws Exception {
        /*
         * This is for filefactory.com/trafficshare/ sharing links or I guess what we call public premium links. This might replace dlUrl,
         * Unknown until proven otherwise.
         */
        logger.finer("Traffic sharing link - Free Premium Download");
        String finalLink = br.getRegex("<a href=\"(https?://\\w+\\.filefactory\\.com/get/t/[^\"]+)\"[^\r\n]*Download with FileFactory TrafficShare").getMatch(0);
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (Application.getJavaVersion() < Application.JAVA17) {
            finalLink = finalLink.replaceFirst("(?i)https", "http");
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, finalLink, true, 0);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            checkErrorsWebsite(link, br);
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

    private void loginWebsite(final Account account, final boolean force, final Browser br) throws Exception {
        synchronized (account) {
            setBrowserExclusive();
            br.getHeaders().put("Accept-Encoding", "gzip");
            br.setFollowRedirects(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Do not verify cookies */
                    return;
                } else {
                    br.getPage("https://www." + this.getHost() + "/");
                    if (isLoggedinWebsite(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
            }
            logger.info("Performing full login");
            br.getPage("https://www." + this.getHost() + "/member/signin.php");
            br.postPage("/member/signin.php", "loginEmail=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()) + "&Submit=Sign+In");
            if (!this.isLoggedinWebsite(br)) {
                this.checkErrorsWebsite(null, br);
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    /** Checks html code and or cookies to see if we're logged in. */
    private boolean isLoggedinWebsite(final Browser br) {
        if (br.getCookie(br.getHost(), "auth", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
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

    @Deprecated
    private AvailableStatus requestFileInformationWebsite(final Account account, final DownloadLink link) throws Exception {
        dllink = null;
        setBrowserExclusive();
        br.setFollowRedirects(true);
        for (int i = 0; i < 4; i++) {
            try {
                Thread.sleep(200);
            } catch (final Exception e) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            URLConnectionAdapter con = null;
            try {
                final String contenturl = this.getContentURL(link);
                con = br.openGetConnection(contenturl);
                if (this.looksLikeDownloadableContent(con)) {
                    link.setFinalFileName(Plugin.getFileNameFromHeader(con));
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    dllink = con.getURL().toExternalForm();
                    link.setAvailable(true);
                    return AvailableStatus.TRUE;
                } else {
                    br.followConnection();
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
        if (isPasswordProtectedFile(br)) {
            final String fileName = br.getRegex("<title>([^<>\"]*?)- FileFactory</title>").getMatch(0);
            if (fileName != null) {
                link.setName(Encoding.htmlDecode(fileName));
            }
            link.setAvailable(true);
            link.setPasswordProtected(true);
            return AvailableStatus.TRUE;
        } else {
            link.setPasswordProtected(false);
        }
        this.checkErrorsWebsite(link, br);
        String fileName = null;
        String fileSize = null;
        if (isTrafficshareLink(br)) {
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
        if (fileName != null) {
            link.setName(Encoding.htmlDecode(fileName).trim());
        }
        if (fileSize != null) {
            link.setDownloadSize(SizeFormatter.getSize(fileSize));
        }
        link.setAvailable(true);
        return AvailableStatus.TRUE;
    }

    private boolean isTrafficshareLink(final Browser br) {
        if (StringUtils.containsIgnoreCase(br.getURL(), "/trafficshare/") || br.containsHTML("(?i)>\\s*Download with FileFactory TrafficShare\\s*<")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isTrafficshareLink(final DownloadLink link) {
        if (link != null && link.hasProperty(PROPERTY_TRAFFICSHARE)) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isPasswordProtectedFile(final Browser br) {
        if (br.containsHTML("(?i)>\\s*You are trying to access a password protected file|This File has been password protected by the uploader\\.")) {
            return true;
        } else {
            return false;
        }
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

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public boolean hasAutoCaptcha() {
        return false;
    }

    private String getFUID(final DownloadLink link) {
        String contenturl;
        try {
            contenturl = this.getContentURL(link);
            final String fuid = new Regex(contenturl, "(?i)file/([\\w]+)").getMatch(0);
            return fuid;
        } catch (PluginException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String getApiBase() {
        return "https://api.filefactory.com/v1";
    }

    private boolean checkLinks_API(final DownloadLink[] urls, Account account, String apiKey) {
        try {
            final Browser br = this.createNewBrowserInstance();
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
                for (final DownloadLink link : links) {
                    // password is last value in fuid response, needed because filenames or other values could contain }. It then returns
                    // invalid response.
                    final String filter = br.getRegex("(\"" + getFUID(link) + "\"\\s*:\\s*\\{.*?\\})").getMatch(0);
                    if (filter == null) {
                        return false;
                    }
                    final String status = PluginJSonUtils.getJsonValue(filter, "status");
                    if (!"online".equalsIgnoreCase(status)) {
                        link.setAvailable(false);
                    } else {
                        link.setAvailable(true);
                    }
                    final String name = PluginJSonUtils.getJsonValue(filter, "name");
                    final String size = PluginJSonUtils.getJsonValue(filter, "size");
                    final String md5 = PluginJSonUtils.getJsonValue(filter, "md5");
                    final String prem = PluginJSonUtils.getJsonValue(filter, "premiumOnly");
                    final String pass = PluginJSonUtils.getJsonValue(filter, "password");
                    if (StringUtils.isNotEmpty(name)) {
                        link.setName(name);
                    }
                    if (size != null && size.matches("^\\d+$")) {
                        link.setVerifiedFileSize(Long.parseLong(size));
                    }
                    if (StringUtils.isNotEmpty(md5)) {
                        link.setMD5Hash(md5);
                    }
                    if (prem != null) {
                        link.setProperty("premiumRequired", Boolean.parseBoolean(prem));
                    }
                    if (pass != null) {
                        link.setPasswordProtected(Boolean.parseBoolean(pass));
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

    private void handleDownload_API(final DownloadLink link, final Account account) throws Exception {
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
        } else if (account.getType() == AccountType.FREE) {
            directlinkproperty = "directurl_account_free";
        } else {
            directlinkproperty = "directurl_account_premium";
        }
        br.setFollowRedirects(true);
        this.dllink = this.checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final boolean isPremium;
            if (this.isTrafficshareLink(link)) {
                isPremium = true;
            } else if (this.isPremiumAccount(account)) {
                isPremium = true;
            } else {
                isPremium = false;
            }
            if (link.getBooleanProperty("premiumRequired", false) && !isPremium) {
                // free dl isn't possible, place before password protected check!
                throw new AccountRequiredException();
            } else if (link.isPasswordProtected()) {
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
            final String fuid = this.getFUID(link);
            final UrlQuery query = new UrlQuery();
            query.add("file", Encoding.urlEncode(fuid));
            if (passCode != null) {
                query.add("password", Encoding.urlEncode(passCode));
            }
            getPage(br, getApiBase() + "/getDownloadLink?" + query.toString(), link, account, apiKey);
            // TODO: 2023-11-03: Add check for invalid password
            final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            final Map<String, Object> result = (Map<String, Object>) entries.get("result");
            dllink = (String) result.get("url");
            if (StringUtils.isEmpty(dllink)) {
                logger.warning("Failed to find final downloadlink");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (passCode != null) {
                /* We know that the entered password was correct so let's save it to re-use it later. */
                link.setDownloadPassword(passCode);
            }
            final String linkType = (String) result.get("linkType");
            if ("trafficshare".equalsIgnoreCase(linkType)) {
                link.setProperty(PROPERTY_TRAFFICSHARE, true);
            } else {
                link.removeProperty(PROPERTY_TRAFFICSHARE);
            }
            final Number waitSeconds = (Number) result.get("delay");
            if (waitSeconds != null) {
                sleep((waitSeconds.intValue() * 1001) + 1111, link);
            }
        }
        handleDownloadAPI(link, account, directlinkproperty);
    }

    private void handleDownloadAPI(final DownloadLink link, final Account account, final String directlinkproperty) throws Exception {
        /*
         * Since I fixed the download core setting correct redirect referrer I can no longer use redirect header to determine error code for
         * max connections. This is really only a problem with media files as filefactory redirects to /stream/ directly after code=\d+
         * which breaks our generic handling. This will fix it!! - raztoki
         */
        int i = -1;
        br.setFollowRedirects(false);
        URLConnectionAdapter con = null;
        String lastUrlWithErrorcode = null;
        String lastRedirect = null;
        while (i++ < 10) {
            String url = dllink;
            if (lastRedirect != null) {
                url = lastRedirect;
            }
            try {
                con = br.openGetConnection(url);
                if (!this.looksLikeDownloadableContent(con) && br.getRedirectLocation() != null) {
                    // redirect, we want to store and continue down the rabbit hole!
                    final String redirect = br.getRedirectLocation();
                    if (this.getErrorcodeFromURL(redirect) != null) {
                        lastUrlWithErrorcode = redirect;
                    }
                    lastRedirect = redirect;
                    continue;
                } else if (!this.looksLikeDownloadableContent(con)) {
                    // error final destination/html
                    br.followConnection(true);
                    if (con.getRequestMethod() == RequestMethod.HEAD) {
                        br.getPage(url);
                    }
                    break;
                } else {
                    // finallink! (usually doesn't contain redirects)
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
        if (!StringUtils.equals(br.getURL(), lastUrlWithErrorcode)) {
            link.setProperty(PROPERTY_LAST_URL_WITH_ERRORCODE, lastUrlWithErrorcode);
        }
        if (!this.looksLikeDownloadableContent(con)) {
            checkErrorsWebsite(link, br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            // this shouldn't happen anymore!
            br.followConnection(true);
            checkErrorsWebsite(link, br);
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

    private boolean isPremiumAccount(final Account account) {
        if (account == null) {
            return false;
        } else if (account.getType() == AccountType.PREMIUM || account.getType() == AccountType.LIFETIME) {
            return true;
        } else {
            return false;
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
                link.removeProperty(property);
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
                    account.setProperty(PROPERTY_ACCOUNT_APIKEY, apikey);
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
            final String ret = account.getStringProperty(PROPERTY_ACCOUNT_APIKEY);
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
                if (StringUtils.isEmpty(apiKey) || StringUtils.equals(apiKey, account.getStringProperty(PROPERTY_ACCOUNT_APIKEY, null))) {
                    account.removeProperty(PROPERTY_ACCOUNT_APIKEY);
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
            if (expire != null) {
                ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(expire), br);
            }
        } else {
            account.setType(AccountType.FREE);
            account.setProperty("totalMaxSim", 20);
            account.setMaxSimultanDownloads(20);
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