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
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.antiDDoSForHost;
import org.jdownloader.plugins.components.config.UpToBoxComConfig;
import org.jdownloader.plugins.components.config.UpToBoxComConfig.PreferredQuality;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class UpToBoxCom extends antiDDoSForHost {
    public UpToBoxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://uptobox.com/payments");
    }

    @Override
    public String getAGBLink() {
        return "https://uptobox.com/tos";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "uptobox.com" });
        // ret.add(new String[] { "uptobox.com", "uptostream.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:iframe/)?([a-z0-9]{12})(/([^/]+))?");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private final boolean        FREE_RESUME                                      = true;
    private final int            FREE_MAXCHUNKS                                   = 1;
    private final int            FREE_MAXDOWNLOADS                                = 1;
    private final boolean        ACCOUNT_FREE_RESUME                              = true;
    private final int            ACCOUNT_FREE_MAXCHUNKS                           = 1;
    private final int            ACCOUNT_FREE_MAXDOWNLOADS                        = 1;
    private final boolean        ACCOUNT_PREMIUM_RESUME                           = true;
    private final int            ACCOUNT_PREMIUM_MAXCHUNKS                        = 0;
    private final int            ACCOUNT_PREMIUM_MAXDOWNLOADS                     = 20;
    public static final String   API_BASE                                         = "https://uptobox.com/api";
    /* If pre-download-waittime is > this, reconnect exception will be thrown! */
    private static final int     WAITTIME_UPPER_LIMIT_UNTIL_RECONNECT             = 240;
    private static final String  PROPERTY_timestamp_lastcheck                     = "timestamp_lastcheck";
    /* If a file-ID is also available on uptostream, we might be able to select between different video qualities. */
    private static final String  PROPERTY_available_on_uptostream                 = "available_on_uptostream";
    private static final boolean use_api_availablecheck_for_single_availablecheck = true;
    private static final int     api_responsecode_password_required_or_wrong      = 17;
    private static final int     api_responsecode_file_offline                    = 28;

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFUID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFUID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    /*
     * Sometimes URLs can contain a filename - especially useful to have that for offline content and as a fallback. Returns file-id in case
     * a name is not present in the URL.
     */
    private String getWeakFilenameFromURL(final DownloadLink link) {
        String weakFilename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        if (weakFilename == null) {
            weakFilename = this.getFUID(link);
        }
        return weakFilename;
    }

    @Override
    public Browser prepBrowser(final Browser prepBr, final String host) {
        if (!(this.browserPrepped.containsKey(prepBr) && this.browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            br.getHeaders().put("User-Agent", "JDownloader");
            br.setFollowRedirects(true);
        }
        return prepBr;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (use_api_availablecheck_for_single_availablecheck) {
            return requestFileInformationAPI(link);
        } else {
            return requestFileInformationWebsite(link);
        }
    }

    /** https://docs.uptobox.com/#files */
    private AvailableStatus requestFileInformationAPI(final DownloadLink link) throws Exception {
        massLinkcheckerAPI(new DownloadLink[] { link });
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        prepBrowser(this.br, this.getHost());
        getPage(link.getPluginPatternMatcher());
        /* 2020-04-07: Website returns proper 404 error for offline which is enough for us to check */
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("class='file-title'>([^<>\"]+) \\((\\d+[^\\)]+)\\)\\s*<");
        final String filename = finfo.getMatch(0);
        String filesize = finfo.getMatch(1);
        if (!StringUtils.isEmpty(filename)) {
            link.setName(Encoding.htmlDecode(filename.trim()));
        } else {
            /* Fallback */
            link.setName(getWeakFilenameFromURL(link));
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        return massLinkcheckerAPI(urls);
    }

    private boolean massLinkcheckerAPI(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser checkbr = new Browser();
            prepBrowser(checkbr, this.getHost());
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 50 links at once */
                    if (index == urls.length || links.size() > 50) {
                        break;
                    }
                    links.add(urls[index]);
                    index++;
                }
                sb.delete(0, sb.capacity());
                for (int i = 0; i < links.size(); i++) {
                    sb.append(this.getFUID(links.get(i)));
                    if (i < links.size() - 1) {
                        /*
                         * Only append comma as long as we've not reached the end.
                         */
                        sb.append("%2C");
                    }
                }
                this.getPage(checkbr, API_BASE + "/link/info?fileCodes=" + sb.toString());
                Map<String, Object> entries = JSonStorage.restoreFromString(checkbr.toString(), TypeRef.HASHMAP);
                ArrayList<Object> linkcheckResults = (ArrayList<Object>) JavaScriptEngineFactory.walkJson(entries, "data/list");
                /* Number of results should be == number of file-ids we wanted to check! */
                if (linkcheckResults.size() != index) {
                    /* Fail-safe: This should never happen */
                    logger.warning("Result size mismatch");
                    return false;
                }
                int index2 = 0;
                for (final DownloadLink dl : links) {
                    /* We expect the results to be delivered in the same order we requested them! */
                    entries = (Map<String, Object>) linkcheckResults.get(index2);
                    final String fuid = this.getFUID(dl);
                    final String fuid_of_json_response = (String) entries.get("file_code");
                    if (!fuid.equals(fuid_of_json_response)) {
                        /* Fail-safe: This should never happen */
                        logger.warning("fuid mismatch");
                        return false;
                    }
                    boolean isOffline = false;
                    final Object errorO = entries.get("error");
                    if (errorO != null) {
                        Map<String, Object> errormap = (Map<String, Object>) errorO;
                        final long errorCode = JavaScriptEngineFactory.toLong(errormap.get("code"), 0);
                        if (errorCode == api_responsecode_file_offline) {
                            isOffline = true;
                        } else {
                            /* E.g. "error":{"code":17,"message":"Password required"} */
                        }
                    }
                    String file_name = null;
                    if (isOffline) {
                        dl.setAvailable(false);
                    } else {
                        file_name = (String) entries.get("file_name");
                        final long file_size = JavaScriptEngineFactory.toLong(entries.get("file_size"), 0);
                        /* This key is not always given e.g. not for password protected content. */
                        boolean available_on_uptostream = false;
                        if (entries.containsKey("available_uts")) {
                            available_on_uptostream = ((Boolean) entries.get("available_uts")).booleanValue();
                            dl.setProperty(PROPERTY_available_on_uptostream, available_on_uptostream);
                        }
                        /* TODO: Get- and set property "need_premium" */
                        if (file_size > 0) {
                            dl.setVerifiedFileSize(file_size);
                        }
                        dl.setAvailable(true);
                    }
                    if (!StringUtils.isEmpty(file_name)) {
                        /* Filename should always be available! */
                        dl.setFinalFileName(file_name);
                    } else {
                        /* Set fallback name */
                        dl.setName(getWeakFilenameFromURL(dl));
                    }
                    index2++;
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.warning("Unexpected (json) response?");
            return false;
        }
        return true;
    }

    /** Website handling */
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String directlinkproperty = "free_directlink";
        String dllink = this.checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            /* Always check for errors here as download1 Form can be present e.g. along with a (reconnect-waittime) error. */
            this.requestFileInformationAPI(link);
            /* Now access website - check availablestatus again here! */
            requestFileInformationWebsite(link);
            checkErrorsWebsite(link, null);
            dllink = this.getDllinkWebsite();
            if (dllink == null) {
                Form download1 = null;
                for (Form form : br.getForms()) {
                    if (form.containsHTML("waitingToken")) {
                        download1 = form;
                        break;
                    }
                }
                if (download1 == null) {
                    logger.warning("Failed to find Form download1");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                String passCode = null;
                if (download1.hasInputFieldByName("file-password")) {
                    passCode = link.getDownloadPassword();
                    if (passCode == null) {
                        passCode = getUserInput("Password?", link);
                    }
                    download1.put("file-password", Encoding.urlEncode(passCode));
                }
                /* TODO */
                // this.waitTime(this.getDownloadLink(), System.currentTimeMillis());
                final int waittime = getPreDownloadWaittimeWebsite();
                if (waittime > 0) {
                    logger.info("Found pre-download-waittime: " + waittime);
                    this.sleep(waittime * 1001l, link);
                }
                this.submitForm(download1);
                checkErrorsWebsite(link, null);
                if (passCode != null) {
                    /* Save correctly entered password. */
                    link.setDownloadPassword(passCode);
                }
                dllink = getDllinkWebsite();
                if (dllink == null) {
                    logger.warning("Failed to find final downloadurl");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        /*
         * TODO: Remember to set verifies filesize here in case user selected a transcoded quality and does NOT want to download the
         * original/source file!
         */
        /* TODO: Add function to obey users' quality selection. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        /* Save final downloadurl for later usage */
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String getDllinkWebsite() {
        return br.getRegex("\"(https?://[^\"]+/dl/[^\"]+)\"").getMatch(0);
    }

    protected void checkErrorsWebsite(final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        /* 2020-03-27: Special */
        if (br.getRegex(">\\s*You must be premium to download this file").matches()) {
            throw new AccountRequiredException();
        } else if (br.containsHTML(">\\s*Wrong password")) {
            link.setDownloadPassword(null);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
        }
        final int waittimeSeconds = getPreDownloadWaittimeWebsite();
        if (waittimeSeconds > WAITTIME_UPPER_LIMIT_UNTIL_RECONNECT) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittimeSeconds * 1001l);
        }
        /* Basically the same what waittimeSecondsStr does --> More complicated fallback */
        final String preciseWaittime = br.getRegex("or you can wait ([^<>\"]+)<").getMatch(0);
        if (preciseWaittime != null) {
            /* Reconnect waittime with given (exact) waittime usually either up to the minute or up to the second. */
            final String tmphrs = new Regex(preciseWaittime, "\\s*(\\d+)\\s*hours?").getMatch(0);
            final String tmpmin = new Regex(preciseWaittime, "\\s*(\\d+)\\s*minutes?").getMatch(0);
            final String tmpsec = new Regex(preciseWaittime, "\\s*(\\d+)\\s*seconds?").getMatch(0);
            final String tmpdays = new Regex(preciseWaittime, "\\s*(\\d+)\\s*days?").getMatch(0);
            int waittime;
            if (tmphrs == null && tmpmin == null && tmpsec == null && tmpdays == null) {
                /* This should not happen! This is an indicator of developer-failure! */
                logger.info("Waittime RegExes seem to be broken - using default waittime");
                waittime = 60 * 60 * 1000;
            } else {
                int minutes = 0, seconds = 0, hours = 0, days = 0;
                if (tmphrs != null) {
                    hours = Integer.parseInt(tmphrs);
                }
                if (tmpmin != null) {
                    minutes = Integer.parseInt(tmpmin);
                }
                if (tmpsec != null) {
                    seconds = Integer.parseInt(tmpsec);
                }
                if (tmpdays != null) {
                    days = Integer.parseInt(tmpdays);
                }
                waittime = ((days * 24 * 3600) + (3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            }
            logger.info("Detected reconnect waittime (milliseconds): " + waittime);
            /* Not enough wait time to reconnect -> Wait short and retry */
            if (waittime < 180000) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until new downloads can be started", waittime);
            } else if (account != null) {
                throw new AccountUnavailableException("Download limit reached", waittime);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
        } else if (br.toString().contains("showVPNWarning")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Website error: 'Warning: Your ip adress may come from a VPN. Please submit your VPN'", 15 * 60 * 1000);
        }
    }

    private int getPreDownloadWaittimeWebsite() {
        final String waittimeSecondsStr = br.getRegex("data-remaining-time='(\\d+)'").getMatch(0);
        if (waittimeSecondsStr != null) {
            return Integer.parseInt(waittimeSecondsStr);
        } else {
            return 0;
        }
    }

    private void handleDownloadAPI(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (account == null) {
            /* This function should never be called without account */
            throw new AccountRequiredException();
        }
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            final UrlQuery query = new UrlQuery();
            query.append("token", account.getPass(), true);
            query.append("file_code", this.getFUID(link), false);
            query.append("password", link.getDownloadPassword(), true);
            int maxtries = 1;
            int tries = 0;
            String passCode = link.getDownloadPassword();
            boolean passwordFailure = false;
            do {
                if (tries > 0 || passwordFailure) {
                    passCode = Plugin.getUserInput("Enter download password", link);
                    query.append("password", passCode, true);
                }
                this.getPage(API_BASE + "/link?" + query.toString());
                tries++;
                passwordFailure = getErrorcode() == api_responsecode_password_required_or_wrong;
            } while (passwordFailure && tries <= maxtries);
            if (passwordFailure) {
                logger.info("User entered INCORRECT password");
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            } else if (passCode != null) {
                /* User entered correct password --> Save it */
                logger.info("User entered correct password");
                link.setDownloadPassword(passCode);
            }
            final String waitStr = PluginJSonUtils.getJson(br, "waiting");
            final String waitingToken = PluginJSonUtils.getJson(br, "waitingToken");
            if (waitingToken != null && waitStr != null) {
                /* Waittime is usually only present in free (-account) mode. */
                final int wait = Integer.parseInt(waitStr);
                /* Waittime too high? Temp. disable account until nex downloads is possible. */
                if (wait > WAITTIME_UPPER_LIMIT_UNTIL_RECONNECT) {
                    throw new AccountUnavailableException("Download limit reached", wait * 1001l);
                }
                this.sleep(wait * 1001, link);
                final UrlQuery queryDL = new UrlQuery();
                queryDL.append("token", account.getPass(), true);
                queryDL.append("file_code", this.getFUID(link), true);
                queryDL.append("waitingToken", waitingToken, true);
                this.getPage(API_BASE + "/link?" + queryDL.toString());
            }
            dllink = PluginJSonUtils.getJson(br, "dlLink");
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                this.checkErrorsAPI(link, account);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
        }
        /*
         * TODO: Remember to set verifies filesize here in case user selected a transcoded quality and does NOT want to download the
         * original/source file!
         */
        /* TODO: Add function to obey users' quality selection. */
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink == null) {
            return null;
        }
        URLConnectionAdapter con = null;
        try {
            final Browser br2 = br.cloneBrowser();
            br2.setFollowRedirects(true);
            con = openAntiDDoSRequestConnection(br2, br2.createHeadRequest(dllink));
            if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                return null;
            }
        } catch (final Exception e) {
            logger.log(e);
            return null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return dllink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void loginAPI(final Account account, boolean verifySession) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                prepBrowser(this.br, this.getHost());
                final Cookies cookies = account.loadCookies("");
                final String apikey;
                /*
                 * Only accounts of users who never logged in via API will have cookies available --> Convert them to apikey and delete them
                 * (only on success)
                 */
                if (cookies != null) {
                    /* TODO: Remove this after 2020-07-01 */
                    logger.info("Trying to convert cookie --> apikey");
                    this.br.setCookies(this.getHost(), cookies);
                    getPage(API_BASE + "/token/get");
                    final String msg = PluginJSonUtils.getJson(br, "message");
                    apikey = PluginJSonUtils.getJson(br, "data");
                    if (!"success".equalsIgnoreCase(msg) || StringUtils.isEmpty(apikey)) {
                        /* E.g. {"statusCode":1,"message":"An error occured","data":"user not found"} */
                        logger.warning("Failed to convert cookies to apikey --> Account invalid");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                    logger.info("Successfully converted cookies to apikey");
                    account.setUser(null);
                    account.setPass(apikey);
                    /* Delete old cookies as we do not need them anymore */
                    account.clearCookies("");
                    br.clearCookies(br.getHost());
                    /* Enforce verifying the session this time */
                    verifySession = true;
                } else {
                    apikey = account.getPass();
                }
                if (!isApikey(apikey)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Invalid token/apikey format", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                if (!verifySession) {
                    /* Force verify session/apikey everx X minutes */
                    verifySession = System.currentTimeMillis() - account.getLongProperty(PROPERTY_timestamp_lastcheck, 0) > 15 * 60 * 1000;
                }
                if (!verifySession) {
                    logger.info("Trust apikey without verification");
                    return;
                }
                logger.info("Performing full login");
                this.getPage(API_BASE + "/user/me?token=" + apikey);
                this.checkErrorsAPI(this.getDownloadLink(), account);
                account.setProperty(PROPERTY_timestamp_lastcheck, System.currentTimeMillis());
            } catch (final PluginException e) {
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            loginAPI(account, true);
        } catch (final PluginException e) {
            throw e;
        }
        if (br.getURL() == null || !br.getURL().contains("/user/me")) {
            this.getPage(API_BASE + "/user/me?token=" + account.getPass());
            checkErrorsAPI(this.getDownloadLink(), account);
            /* Session verified */
            account.setProperty(PROPERTY_timestamp_lastcheck, System.currentTimeMillis());
        }
        final String premium = PluginJSonUtils.getJson(br, "premium");
        String points = PluginJSonUtils.getJson(br, "point");
        if (StringUtils.isEmpty(points)) {
            points = "Unknown";
        }
        String accountStatus;
        if (premium == null || (!premium.equals("1") && !premium.equalsIgnoreCase("true"))) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            accountStatus = "Free Account";
        } else {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            accountStatus = "Premium Account";
            /* 2020-04-06: All premium accounts should have an expiredate given but let's just assume lifetime accounts can exist. */
            final String expire = PluginJSonUtils.getJson(br, "premium_expire");
            if (!StringUtils.isEmpty(expire)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
            }
        }
        accountStatus += String.format(" [%s points]", points);
        final String mail = PluginJSonUtils.getJson(br, "email");
        if (mail != null && mail.length() > 2) {
            /* don't store the complete mail as a security purpose */
            final String shortmail = mail.substring(0, mail.length() / 2) + "****";
            account.setUser(shortmail);
        }
        ai.setStatus(accountStatus);
        ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        loginAPI(account, false);
        if (account.getType() == AccountType.FREE) {
            handleDownloadAPI(link, account, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            handleDownloadAPI(link, account, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "premium_directlink");
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null || acc.getType() == AccountType.FREE) {
            /* no account or free account, yes we can expect captcha */
            return true;
        }
        /* Premium accounts do not have captchas */
        return false;
    }

    private int getErrorcode() {
        final String errorCodeStr = PluginJSonUtils.getJson(br, "statusCode");
        if (errorCodeStr != null && errorCodeStr.matches("\\d+")) {
            return Integer.parseInt(errorCodeStr);
        }
        return 0;
    }

    /**
     * Handle errorcodes according to: https://docs.uptobox.com/#status-code
     *
     * @throws PluginException
     */
    private void checkErrorsAPI(final DownloadLink link, final Account account) throws PluginException {
        String errorMsg = PluginJSonUtils.getJson(br, "message");
        if (StringUtils.isEmpty(errorMsg)) {
            errorMsg = "Unknown error";
        }
        final int errorCode = getErrorcode();
        switch (errorCode) {
        case 0:
            /* Success --> No error */
            return;
        case 1:
            /*
             * 2020-04-07: Generic error which can have different causes. According to admin a retry is not always required which is why
             * I've chosen a very long retry time. We'll have to wait for user feedback to see if this is a good idea.
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsg, 5 * 60 * 1000);
        case 2:
            invalidApikey();
        case 5:
            throw new AccountRequiredException(errorMsg);
        case 7:
            /*
             * E.g. {"statusCode":7,"message":"Invalid Parameter","data":"bad file_code"} --> Should never happen - will only happen if you
             * e.g. try to download a file_code in an invalid format e.g. too long.
             */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 11:
            /* "Too much fail attempt" --> Wait some more time than usually */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
        case 13:
            /* "Invalid token" --> Permanently disable account */
            invalidApikey();
        case api_responsecode_password_required_or_wrong:
            link.setDownloadPassword(null);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
        case api_responsecode_file_offline:
            /* E.g. {"statusCode":28,"message":"File not found","data":"xxxxxxxxxxxx"} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case 29:
            /* "You have reached the streaming limit for today" */
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errorMsg, 1 * 60 * 60 * 1000l);
        case 30:
            /* "Folder not found" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        default:
            /* E.g. 3, 4, 6, 7, 8, 9, 10, 12, 14, 15, 16, 18, 19, 31 */
            /*
             * * Error 8 reads itself as if it was a "trafficlimit reached" error but it has nothingt todo with downloading, it is only for
             * their "voucher reedem API call".
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsg, 5 * 60 * 1000l);
        }
    }

    protected String getConfiguredQuality() {
        /* Returns user-set value which can be used to circumvent government based GEO-block. */
        PreferredQuality cfgquality = PluginJsonConfig.get(UpToBoxComConfig.class).getPreferredQuality();
        if (cfgquality == null) {
            cfgquality = PreferredQuality.DEFAULT;
        }
        switch (cfgquality) {
        case QUALITY1:
            return "x";
        case DEFAULT:
        default:
            return this.getHost();
        }
    }

    private void invalidApikey() throws PluginException {
        /* TODO: Add errortext and french translation */
        if ("fr".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nToken invalide. / Vous pouvez trouver votre token ici : uptobox.com/my_account.\r\nSi vous utilisez JDownloader à distance, entrez le token dans les champs de nom d'utilisateur de de mot de passe.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid token/apikey!\r\nYou can find your token here: uptobox.com/my_account\r\nIf you are running JDownloader headless, just put your token into the username and password field.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    private boolean isApikey(final String str) {
        if (str != null && str.matches("[a-z0-9]+")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new UptoboxAccountFactory(callback);
    }

    public static class UptoboxAccountFactory extends MigPanel implements AccountBuilderInterface {
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your Token/apikey";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            }
            if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            }
            return new String(this.pass.getPassword());
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private final ExtPasswordField pass;
        private static String          EMPTYPW = "                 ";

        public UptoboxAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your Token / apikey:"));
            add(new JLink("https://uptobox.com/my_account"));
            add(new JLabel("Token / apikey:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(PINHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            // final String userName = getUsername();
            // if (userName == null || !userName.trim().matches("^\\d{9}$")) {
            // idLabel.setForeground(Color.RED);
            // return false;
            // }
            // idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return UpToBoxComConfig.class;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}