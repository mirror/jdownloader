//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;

import jd.PluginWrapper;
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
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class TwojplikPl extends PluginForHost {
    public TwojplikPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/odkryj-pelnie-mozliwosci");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/terms-and-conditions";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "twojplik.pl" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:(?:download|view)/)?([A-Z0-9\\-]{24,})(/([^/#\\?]+))?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private final boolean USE_API                      = true;
    private final String  PROPERTY_ACCOUNT_SESSION_KEY = "session_key";

    private String getApiBase() {
        return "https://jd2.twojplik.pl/api/v1";
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* Allow max 100 items. */
                    if (index == urls.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink link : links) {
                    sb.append(this.getFID(link));
                    sb.append("|");
                }
                final UrlQuery query = new UrlQuery();
                query.add("files", Encoding.urlEncode(sb.toString()));
                br.postPage(this.getApiBase() + "/file/getFilesData", query);
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final Map<String, Object> data = (Map<String, Object>) entries.get("data");
                final List<Map<String, Object>> filemaps = (List<Map<String, Object>>) data.get("files");
                for (final DownloadLink link : links) {
                    final String fid = this.getFID(link);
                    Map<String, Object> filemap = null;
                    /* Find the map of the file we are checking. */
                    for (final Map<String, Object> afilemap : filemaps) {
                        final String code = (String) afilemap.get("code");
                        if (StringUtils.equals(code, fid)) {
                            filemap = afilemap;
                            break;
                        }
                    }
                    this.setWeakFilename(link);
                    if (filemap == null) {
                        /* Invalid fileID. */
                        link.setAvailable(false);
                        continue;
                    }
                    final String filename = (String) filemap.get("name");
                    final Number sizeO = (Number) filemap.get("size");
                    if (filename != null) {
                        link.setFinalFileName(filename);
                    }
                    if (sizeO != null) {
                        link.setVerifiedFileSize(sizeO.longValue());
                    }
                    if (Boolean.FALSE.equals(filemap.get("status"))) {
                        link.setAvailable(false);
                    } else {
                        link.setAvailable(true);
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        if (USE_API) {
            return requestFileInformationAPI(link, account);
        } else {
            return requestFileInformationWebsite(link, account);
        }
    }

    private AvailableStatus requestFileInformationAPI(final DownloadLink link, final Account account) throws Exception {
        checkLinks(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        setWeakFilename(link);
        if (account != null) {
            this.loginWebsite(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filesize = br.getRegex("File Size\\s*:\\s*<br> ([^<>\"]+)<").getMatch(0);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    private void setWeakFilename(final DownloadLink link) {
        if (!link.isNameSet()) {
            final Regex urlinfo = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks());
            final String fileid = urlinfo.getMatch(0);
            final String filenameFromURL = urlinfo.getMatch(2);
            if (filenameFromURL != null) {
                link.setName(Encoding.htmlDecode(filenameFromURL).trim());
            } else {
                link.setName(fileid);
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private boolean isLoggedinWebsite(final Browser br) {
        return br.containsHTML("auth/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (USE_API) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        final Map<String, Object> usermap = loginAPI(account, true);
        final Map<String, Object> premiuminfo = (Map<String, Object>) usermap.get("premium");
        final AccountInfo ai = new AccountInfo();
        if (premiuminfo != null) {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(((Number) premiuminfo.get("expireTime")).longValue() * 1000);
            ai.setTrafficLeft(((Number) premiuminfo.get("transferLeft")).longValue());
            ai.setTrafficMax(((Number) premiuminfo.get("transferLimit")).longValue());
            ai.setStatus(premiuminfo.get("name").toString());
        } else {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
            /* 2024-03-15: Free users can't download anything atm. */
            ai.setExpired(true);
        }
        return ai;
    }

    private Map<String, Object> loginAPI(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Map<String, Object> postdata = new HashMap<String, Object>();
            postdata.put("login", account.getUser());
            postdata.put("password", account.getPass());
            String sessionkey = account.getStringProperty(PROPERTY_ACCOUNT_SESSION_KEY);
            if (sessionkey != null) {
                logger.info("Attempting session key login");
                if (!force) {
                    /* Don't validate cookies */
                    return null;
                }
                final UrlQuery query = new UrlQuery();
                query.add("sessionKey", Encoding.urlEncode(sessionkey));
                br.postPage(this.getApiBase() + "/user/getUserSession", query);
                try {
                    final Map<String, Object> data = (Map<String, Object>) checkErrorsAPI(br, null, account);
                    logger.info("Successfully logged in via session key");
                    return data;
                } catch (final Throwable e) {
                    logger.info("Looks like session has expired");
                    account.removeProperty(PROPERTY_ACCOUNT_SESSION_KEY);
                }
            }
            /* Generate new session key. */
            logger.info("Performing full login");
            final UrlQuery query = new UrlQuery();
            query.add("login", Encoding.urlEncode(account.getUser()));
            query.add("password", Encoding.urlEncode(account.getPass()));
            br.postPage(this.getApiBase() + "/user/getUserSession", query);
            final Map<String, Object> data = (Map<String, Object>) checkErrorsAPI(br, null, account);
            final Map<String, Object> user = (Map<String, Object>) data.get("user");
            sessionkey = user.get("sessionKey").toString();
            // String alertText = (String) entries.get("alertText");
            // if (alertText != null) {
            // alertText += "\r\nDo not use your normal email/username website login!";
            // alertText += "\r\nFind your special JDownloader API login credentials here: " + this.getHost() + "/panel -> JDownloader 2 ->
            // Show data";
            // throw new AccountInvalidException(alertText);
            // }
            account.setProperty(PROPERTY_ACCOUNT_SESSION_KEY, sessionkey);
            return user;
        }
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        loginWebsite(account, true);
        br.getPage("/panel");
        final AccountInfo ai = new AccountInfo();
        final String expire = br.getRegex("(\\d{1,2}\\.\\d{1,2}\\.\\d{4}, \\d{2}:\\d{2})").getMatch(0);
        if (expire != null) {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd.MM.yyyy HH:mm", Locale.ENGLISH));
            account.setType(AccountType.PREMIUM);
        } else {
            account.setType(AccountType.FREE);
        }
        String trafficleftStr = br.getRegex(">\\s*Twój transfer dzienny\\s*</span></p>.*?<p style=\"color:#ffbb44;font-weight: 900;\">([^<]+)</p>").getMatch(0);
        if (trafficleftStr != null) {
            trafficleftStr = trafficleftStr.replace(",", ".");
            ai.setTrafficLeft(SizeFormatter.getSize(trafficleftStr));
        } else {
            ai.setUnlimitedTraffic();
        }
        return ai;
    }

    private boolean loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Don't validate cookies */
                    return false;
                }
                br.getPage("https://" + this.getHost() + "/");
                if (this.isLoggedinWebsite(br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/");
            final Form loginform = new Form();
            loginform.setMethod(MethodType.POST);
            loginform.setAction("/auth/login");
            loginform.put("action", "fastLogin");
            loginform.put("mail", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginform);
            if (br.getRequest().getHtmlCode().startsWith("{")) {
                /* Login failed -> Json response with more information */
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                final String alertText = (String) entries.get("alertText");
                if (alertText != null) {
                    throw new AccountInvalidException(alertText);
                } else {
                    throw new AccountInvalidException();
                }
            }
            /* Double-check if we're logged in */
            if (!isLoggedinWebsite(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    protected static String getDownloadModeDirectlinkProperty(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return "freelink2";
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return "premlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "freelink";
        }
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (USE_API) {
            handleDownloadAPI(link, account);
        } else {
            handleDownloadWebsite(link, account);
        }
    }

    private void handleDownloadAPI(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (account == null) {
            throw new AccountRequiredException();
        } else if (account.getType() != AccountType.PREMIUM) {
            throw new AccountRequiredException();
        }
        final String directurlproperty = getDownloadModeDirectlinkProperty(account);
        final String storedDirecturl = link.getStringProperty(directurlproperty);
        final String dllink;
        if (storedDirecturl != null) {
            dllink = storedDirecturl;
        } else {
            final UrlQuery query = new UrlQuery();
            query.add("fileCode", Encoding.urlEncode(this.getFID(link)));
            query.add("sessionKey", Encoding.urlEncode(account.getStringProperty(PROPERTY_ACCOUNT_SESSION_KEY)));
            br.postPage(this.getApiBase() + "/file/getDirectlink", query);
            final Map<String, Object> data = (Map<String, Object>) checkErrorsAPI(br, link, account);
            // TODO: Make use of the user object here - it is always here so we can use it to set account data before each download-attempt
            // final Map<String, Object> user = (Map<String, Object>) data.get("user");
            final Map<String, Object> file = (Map<String, Object>) data.get("file");
            dllink = file.get("directlink").toString();
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, this.isResumeable(link, account), 1);
            final URLConnectionAdapter con = dl.getConnection();
            if (!this.looksLikeDownloadableContent(con)) {
                br.followConnection(true);
                if (con.getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
                } else if (con.getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
                } else if (con.getResponseCode() == 500) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 500", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken?");
                }
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directurlproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        if (storedDirecturl == null) {
            link.setProperty(directurlproperty, dl.getConnection().getURL().toExternalForm());
        }
        dl.startDownload();
    }

    private void handleDownloadWebsite(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (account == null) {
            throw new AccountRequiredException();
        } else if (account.getType() != AccountType.PREMIUM) {
            throw new AccountRequiredException();
        }
        final String dllink = br.getRegex("class=\"download-url-input\"[^>]*value=\"(https?://[^\"]+)").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, dllink, true, 0);
        final URLConnectionAdapter con = dl.getConnection();
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            if (con.getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (con.getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "File broken?");
            }
        }
        dl.startDownload();
    }

    private Object checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        Map<String, Object> entries = null;
        try {
            entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        } catch (final JSonMapperException e) {
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Invalid API response", 60 * 1000l);
            } else {
                throw new AccountUnavailableException("Invalid API response", 60 * 1000);
            }
        }
        final Object dataO = entries.get("data");
        if (Boolean.TRUE.equals(entries.get("status"))) {
            /* No error */
            return dataO;
        }
        final String errormsg = entries.get("alertText").toString();
        if (link == null) {
            throw new AccountUnavailableException(errormsg, 1 * 60 * 1000l);
        } else {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errormsg, 1 * 60 * 1000l);
        }
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        /* 2024-02-29: Only premium users can download */
        if (account != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}