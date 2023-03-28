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

import org.appwork.storage.JSonMapperException;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.Exceptions;
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
import jd.plugins.PluginException;

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
    public static final String   API_BASE                                         = "https://uptobox.com/api";
    /* If pre-download-waittime is > this, reconnect exception will be thrown! */
    private static final int     WAITTIME_UPPER_LIMIT_UNTIL_RECONNECT             = 240;
    private static final String  PROPERTY_timestamp_lastcheck                     = "timestamp_lastcheck";
    /* If a file-ID is also available on uptostream, we might be able to select between different video qualities. */
    public static final String   PROPERTY_available_on_uptostream                 = "available_on_uptostream";
    /* 2020-04-12: All files >=5GB are premiumonly but instead of hardcoding this, their filecheck-API returns the property. */
    private static final String  PROPERTY_needs_premium                           = "needs_premium";
    private static final String  PROPERTY_preset_api_errorcode                    = "api_errorcode";
    private static final String  PROPERTY_last_downloaded_quality                 = "last_downloaded_quality";
    private static final boolean use_api_availablecheck_for_single_availablecheck = true;
    private static final int     api_errorcode_password_required_or_wrong         = 17;
    private static final int     api_errorcode_file_temporarily_unavailable       = 25;
    private static final int     api_errorcode_file_offline                       = 28;

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        final boolean requires_premium = link.getBooleanProperty(PROPERTY_needs_premium, false);
        if (requires_premium && (account == null || account.getType() != AccountType.PREMIUM)) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("http://", "https://"));
    }

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
    private String getWeakFilename(final DownloadLink link) {
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
            prepBrowserStatic(br);
        }
        return prepBr;
    }

    public static Browser prepBrowserStatic(final Browser br) {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Accept", "application/json");
        br.setFollowRedirects(true);
        return br;
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
        } else if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    private AvailableStatus requestFileInformationWebsite(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getWeakFilename(link));
        }
        this.setBrowserExclusive();
        prepBrowser(this.br, this.getHost());
        getPage(link.getPluginPatternMatcher().replaceFirst("http://", "https://"));
        /* 2020-04-07: Website returns proper 404 error for offline which is enough for us to check */
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex finfo = br.getRegex("class='file-title'>([^<>\"]+) \\((\\d+[^\\)]+)\\)\\s*<");
        final String filename = finfo.getMatch(0);
        String filesize = finfo.getMatch(1);
        if (!StringUtils.isEmpty(filename)) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (!StringUtils.isEmpty(filesize)) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (br.getRegex(">\\s*You must be premium to download this file").matches()) {
            link.setProperty(PROPERTY_needs_premium, true);
        } else {
            link.setProperty(PROPERTY_needs_premium, false);
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
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() > 100) {
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
                final Map<String, Object> entries = restoreFromString(checkbr.toString(), TypeRef.MAP);
                final List<Object> linkcheckResults = (List<Object>) JavaScriptEngineFactory.walkJson(entries, "data/list");
                /* Number of results should be == number of file-ids we wanted to check! */
                if (linkcheckResults.size() != index) {
                    /* Fail-safe: This should never happen */
                    logger.warning("Result size mismatch");
                    return false;
                }
                int index2 = 0;
                for (final DownloadLink dl : links) {
                    /* We expect the results to be delivered in the same order we requested them! */
                    if (!dl.isNameSet()) {
                        dl.setName(getWeakFilename(dl));
                    }
                    final Map<String, Object> linkinfo = (Map<String, Object>) linkcheckResults.get(index2);
                    final String fuid = this.getFUID(dl);
                    final String fuid_of_json_response = (String) linkinfo.get("file_code");
                    if (!fuid.equals(fuid_of_json_response)) {
                        /* Fail-safe: This should never happen */
                        logger.warning("fuid mismatch");
                        return false;
                    }
                    boolean isOffline = false;
                    final Object errorO = linkinfo.get("error");
                    if (errorO != null) {
                        final Map<String, Object> errormap = (Map<String, Object>) errorO;
                        final int errorCode = ((Number) errormap.get("code")).intValue();
                        dl.setProperty(PROPERTY_preset_api_errorcode, errorCode);
                        if (errorCode == api_errorcode_file_offline) {
                            isOffline = true;
                        } else if (errorCode == api_errorcode_password_required_or_wrong) {
                            /* E.g. "error":{"code":17,"message":"Password required"} */
                            dl.setPasswordProtected(true);
                        }
                    } else {
                        dl.setPasswordProtected(false);
                        dl.removeProperty(PROPERTY_preset_api_errorcode);
                    }
                    final String file_name = (String) linkinfo.get("file_name");
                    final Number file_size = (Number) linkinfo.get("file_size");
                    /* This key is not always given e.g. not for password protected content. */
                    dl.setProperty(PROPERTY_available_on_uptostream, linkinfo.get("available_uts"));
                    dl.setProperty(PROPERTY_needs_premium, linkinfo.get("need_premium"));
                    if (file_size != null) {
                        dl.setVerifiedFileSize(file_size.longValue());
                    }
                    if (!StringUtils.isEmpty(file_name)) {
                        /* Filename should always be available! */
                        dl.setFinalFileName(file_name);
                    }
                    if (isOffline) {
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    index2++;
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            logger.log(e);
            logger.warning("Unexpected (json) response?");
            return false;
        }
        return true;
    }

    /** Website handling */
    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        final String directlinkproperty = this.getDirectlinkProperty(null);
        String dllink = link.getStringProperty(directlinkproperty);
        final boolean isFreshDirecturl;
        if (dllink != null) {
            isFreshDirecturl = false;
            logger.info("Re-using previously generated directurl");
        } else {
            /* Always check for errors here as download1 Form can be present e.g. along with a (reconnect-waittime) error. */
            isFreshDirecturl = true;
            this.requestFileInformationAPI(link);
            handlePropertyBasedErrors(link, null);
            if (link.getBooleanProperty(PROPERTY_needs_premium, false)) {
                throw new AccountRequiredException();
            }
            /* Now access website - check availablestatus again here! */
            requestFileInformationWebsite(link);
            checkErrorsWebsite(br, link, null);
            dllink = this.getDllinkWebsite(br);
            if (dllink == null) {
                String passCode = null;
                Form dlform = null;
                for (Form form : br.getForms()) {
                    if (form.containsHTML("waitingToken")) {
                        dlform = form;
                        break;
                    }
                }
                if (dlform != null) {
                    if (dlform.hasInputFieldByName("file-password")) {
                        link.setPasswordProtected(true);
                        passCode = link.getDownloadPassword();
                        if (passCode == null) {
                            passCode = getUserInput("Password?", link);
                        }
                        dlform.put("file-password", Encoding.urlEncode(passCode));
                    } else {
                        link.setPasswordProtected(false);
                    }
                    final int waittime = regexPreDownloadWaittimeWebsite(br);
                    if (waittime > 0) {
                        logger.info("Found pre-download-waittime: " + waittime);
                        this.sleep(waittime * 1001l, link);
                    } else {
                        logger.info("ZERO pre-download waittime");
                    }
                    this.submitForm(dlform);
                    checkErrorsWebsite(br, link, null);
                    dllink = getDllinkWebsite(br);
                    /* Save correctly entered password. */
                    if (passCode != null) {
                        link.setDownloadPassword(passCode);
                    }
                }
                if (StringUtils.isEmpty(dllink)) {
                    logger.warning("Failed to find final downloadurl");
                    if (br.containsHTML("id='ban'")) {
                        /*
                         * 2020-06-12: E.g. "<h1>This page is not allowed in the US</h1>",
                         * "We're sorry but it appears your IP comes from the US so you're not allowed to download or stream.",
                         * "If you have a premium account, please login to remove the limitation."
                         */
                        throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "GEO-blocked");
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
        }
        dllink = correctProtocolOfFinalDownloadURL(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, null), this.getMaxChunks(null));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (!isFreshDirecturl) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Old directurl expired");
            }
            this.checkErrorsWebsite(br, link, null);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        /* Save final downloadurl for later usage */
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String getDllinkWebsite(final Browser br) {
        return br.getRegex("\"(https?://[^\"]+/dl/[^\"]+)\"").getMatch(0);
    }

    protected void checkErrorsWebsite(final Browser br, final DownloadLink link, final Account account) throws NumberFormatException, PluginException {
        checkResponsecodeErrors(br, link, account);
        if (br.containsHTML(">\\s*Wrong password")) {
            link.setPasswordProtected(true);
            link.setDownloadPassword(null);
            throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
        }
        final int waittimeSeconds = regexPreDownloadWaittimeWebsite(br);
        if (waittimeSeconds > WAITTIME_UPPER_LIMIT_UNTIL_RECONNECT) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waittimeSeconds * 1001l);
        }
        /* Basically the same what waittimeSecondsStr does --> More complicated fallback */
        final String preciseWaittime = br.getRegex("(?i)or you can wait ([^<>\"]+)<").getMatch(0);
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
        } else if (br.containsHTML("<h1>\\s*Banned\\s*</h1>|>\\s*We suspected fraudulent activity from your connection")) {
            throw new AccountUnavailableException("Account banned", 3 * 60 * 60 * 1000l);
        } else if (br.containsHTML("Please use the original uptobox link|Your IP have changed between download link generation and this page|You are using a VPN/Proxy or a data saver|Someone other than you generated this link")) {
            /*
             * Most likely an extreme edge case: User has switched IP outside JD between waittime and final downloadlink generation ->
             * Hotlink protection kicks in
             */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hot linking is not allowed on Uptobox", 3 * 60 * 1000l);
        } else if (br.containsHTML("(?i)>\\s*This file is temporarily unavailable, please retry")) {
            errorFileTemporarilyUnavailable();
        }
    }

    private void errorFileTemporarilyUnavailable() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "This file is temporarily unavailable, please retry later", 5 * 60 * 1000);
    }

    private void checkResponsecodeErrors(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        if (br.getHttpConnection().getResponseCode() == 428) {
            /* Rare case e.g. user is turning on/off a VPN during pre download wait. */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "IP mismatch: Your IP has changed between pre download wait and download attempt", 1 * 60 * 1000l);
        }
    }

    private int regexPreDownloadWaittimeWebsite(final Browser br) {
        final String waittimeSecondsStr = br.getRegex("data-remaining-time='(\\d+)'").getMatch(0);
        if (waittimeSecondsStr != null) {
            return Integer.parseInt(waittimeSecondsStr);
        } else {
            return 0;
        }
    }

    private void handleDownloadAPI(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (account == null) {
            /* This function should never be called without account */
            throw new AccountRequiredException();
        }
        String directlinkproperty = getDirectlinkProperty(account);
        final UrlQuery queryBasic = new UrlQuery();
        queryBasic.append("token", account.getPass(), true);
        queryBasic.append("file_code", this.getFUID(link), false);
        final boolean streamDownloadAvailable = link.getBooleanProperty(PROPERTY_available_on_uptostream, false);
        String dllink = null;
        final String preferredQuality = getConfiguredQuality();
        final String preferredQualityLastTime = link.getStringProperty(PROPERTY_last_downloaded_quality, null);
        boolean isDownloadingStream = false;
        boolean isFreshDirecturl = true;
        if (preferredQuality != null && streamDownloadAvailable && account.getType() == AccountType.PREMIUM) {
            logger.info("Download preferred quality: " + preferredQuality);
            /* Different streaming URLs = different property to store them on! */
            final String directlinkpropertyTmp = directlinkproperty + preferredQuality;
            dllink = link.getStringProperty(directlinkpropertyTmp);
            if (dllink != null) {
                logger.info("Successfully re-used last streaming-URL");
                directlinkproperty = directlinkpropertyTmp;
                isDownloadingStream = true;
                isFreshDirecturl = false;
            } else {
                logger.info("Trying to generate new streaming URL in preferred quality");
                isFreshDirecturl = true;
                this.getPage(API_BASE + "/streaming?" + queryBasic.toString());
                try {
                    /*
                     * Streams can also contain multiple video streams with e.g. different languages --> We will ignore this rare case and
                     * always download the first stream of the user preferred quality we get.
                     */
                    final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
                    dllink = (String) JavaScriptEngineFactory.walkJson(entries, "data/streamLinks/" + preferredQuality + "/{0}");
                    if (!StringUtils.isEmpty(dllink)) {
                        logger.info("Successfully found user preferred quality " + preferredQuality);
                        directlinkproperty = directlinkpropertyTmp;
                        isDownloadingStream = true;
                    } else {
                        logger.info("Failed to find user preferred streaming quality --> Fallback to original download");
                    }
                } catch (final Throwable e) {
                    logger.log(e);
                    logger.warning("Stream download failed");
                }
            }
        }
        if (dllink == null) {
            dllink = link.getStringProperty(directlinkproperty);
            if (dllink != null) {
                logger.info("Download original | Re-using old directurl");
                isFreshDirecturl = false;
            }
        }
        if (StringUtils.isEmpty(dllink)) {
            isFreshDirecturl = true;
            Map<String, Object> entries = null;
            int statusCode = 0;
            final UrlQuery queryDownload = queryBasic;
            String passCode = link.getDownloadPassword();
            /*
             * 2020-04-12: Do not ask for password on first attempt even if we know that the file is password protected: Uploaders can
             * download their own uploaded files without password even if they're password protected.
             */
            boolean hasAskedUserForPassword = false;
            do {
                if (passCode != null) {
                    queryDownload.addAndReplace("password", Encoding.urlEncode(passCode));
                }
                this.getPage(API_BASE + "/link?" + queryDownload.toString());
                entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                statusCode = ((Number) entries.get("statusCode")).intValue();
                if (hasAskedUserForPassword) {
                    /* Ask user max one time */
                    break;
                } else if (statusCode == api_errorcode_password_required_or_wrong) {
                    if (passCode == null) {
                        logger.info("Link is password protected and initially given password is wrong: " + passCode);
                    } else {
                        logger.info("Link is password protected");
                    }
                    link.setPasswordProtected(true);
                    passCode = getUserInput("Enter download password", link);
                    hasAskedUserForPassword = true;
                    continue;
                } else {
                    break;
                }
            } while (hasAskedUserForPassword == false);
            if (statusCode == api_errorcode_password_required_or_wrong) {
                logger.info("User entered incorrect password: " + passCode);
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            } else if (passCode != null) {
                /* User entered correct password --> Save it */
                logger.info("User entered correct download password: " + passCode);
                link.setDownloadPassword(passCode);
            }
            /**
             * 2021-02-18: E.g. for high waittimes, token is not given {"statusCode":16,"message":"Waiting
             * needed","data":{"waiting":125,"waitingToken":null}}
             */
            if (statusCode != 16) {
                /* Unexpected statuscode -> Check for errors */
                this.checkErrorsAPI(br, link, account);
            }
            entries = (Map<String, Object>) entries.get("data");
            final String waitingToken = (String) entries.get("waitingToken");
            if (!StringUtils.isEmpty(waitingToken)) {
                /* Pre download waittime is usually only present in free -account mode, not in premium. */
                final int waitSeconds = ((Number) entries.get("waiting")).intValue();
                /* Waittime too high? Temp. disable account until nex downloads is possible. */
                if (waitSeconds > WAITTIME_UPPER_LIMIT_UNTIL_RECONNECT) {
                    throw new AccountUnavailableException("Download limit reached", waitSeconds * 1001l);
                }
                /* 2020-04-16: Add 2 extra wait seconds otherwise download may fail as we're too fast. */
                this.sleep((waitSeconds + 2) * 1000, link);
                final UrlQuery queryDL = queryBasic;
                queryDL.append("waitingToken", waitingToken, true);
                this.getPage(API_BASE + "/link?" + queryDL.toString());
                entries = restoreFromString(br.toString(), TypeRef.MAP);
                entries = (Map<String, Object>) entries.get("data");
            }
            dllink = (String) entries.get("dlLink");
            if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
                /* This should never happen! */
                this.checkErrorsAPI(br, link, account);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
            }
        }
        /*
         * TODO: Reset progress instead of disabling resume otherwise user would get the "this download is not resumable" warning which is
         * not true in this case.
         */
        boolean resumable = this.isResumeable(link, account);
        final boolean isDifferentQualityThanLastTime = !StringUtils.equals(preferredQualityLastTime, preferredQuality);
        if (isDifferentQualityThanLastTime) {
            /* Disable resume for this time otherwise we may end up with a broken file. */
            resumable = false;
        }
        if (isDownloadingStream) {
            /*
             * Important: During availablecheck, verified filesize of original file is set. If we're now downloading a transcoded stream
             * instead we will get another filesize now!
             */
            link.setVerifiedFileSize(-1);
            link.setProperty(PROPERTY_last_downloaded_quality, preferredQuality);
        } else {
            link.removeProperty(PROPERTY_last_downloaded_quality);
        }
        dllink = correctProtocolOfFinalDownloadURL(dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, getMaxChunks(account));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (!isFreshDirecturl) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Old directurl expired");
            }
            this.checkResponsecodeErrors(br, link, account);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String correctProtocolOfFinalDownloadURL(final String finalDownloadurl) {
        if (PluginJsonConfig.get(UpToBoxComConfig.class).isUseHTTPSForDownloads()) {
            logger.info("User prefers https");
            return finalDownloadurl.replaceFirst("http://", "https://");
        } else {
            logger.info("User prefers http");
            return finalDownloadurl.replaceFirst("https?://", "http://");
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private Map<String, Object> loginAPI(final Account account, final boolean verifySession) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            prepBrowser(this.br, this.getHost());
            final String apikey = account.getPass();
            if (StringUtils.isEmpty(apikey)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (!verifySession) {
                logger.info("Trust apikey without verification");
                return null;
            } else {
                logger.info("Performing full login");
                this.getPage(API_BASE + "/user/me?token=" + Encoding.urlEncode(apikey));
                this.checkErrorsAPI(br, null, account);
                account.setProperty(PROPERTY_timestamp_lastcheck, System.currentTimeMillis());
                return restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> entries = loginAPI(account, true);
        final Map<String, Object> userdata = (Map<String, Object>) entries.get("data");
        final Number premium = (Number) userdata.get("premium");
        final Object point = userdata.get("point");
        String accountStatus;
        if (premium != null && premium.intValue() == 1) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            accountStatus = "Premium Account";
            /* 2020-04-06: All premium accounts should have an expiredate given but let's just assume lifetime accounts can exist. */
            final String expireDate = (String) userdata.get("premium_expire");
            if (!StringUtils.isEmpty(expireDate)) {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH), br);
                if (ai.isExpired()) {
                    account.setType(AccountType.FREE);
                    account.setMaxSimultanDownloads(1);
                    accountStatus = "Free Account (expired Premium)";
                }
            }
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(1);
            accountStatus = "Free Account";
        }
        accountStatus += String.format(" | %s points", (point != null ? point.toString() : "Unknown"));
        final String user = (String) userdata.get("login");
        if (!StringUtils.isEmpty(user)) {
            account.setUser(user);
        }
        ai.setStatus(accountStatus);
        ai.setUnlimitedTraffic();
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformationAPI(link);
        handlePropertyBasedErrors(link, account);
        loginAPI(account, false);
        handleDownloadAPI(link, account);
    }

    private String getDirectlinkProperty(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return "account_free_directlink";
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return "premium_directlink";
        } else {
            /* Free(anonymous) and unknown account type */
            return "directlink";
        }
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    private int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    /** Throws Exception if DownloadLink contains preset property that indicates that a download is not possible. */
    private void handlePropertyBasedErrors(final DownloadLink link, final Account account) throws PluginException {
        final int presetErrorcode = link.getIntegerProperty(PROPERTY_preset_api_errorcode, -1);
        switch (presetErrorcode) {
        case -1:
            break;
        case api_errorcode_file_offline:
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        case api_errorcode_file_temporarily_unavailable:
            errorFileTemporarilyUnavailable();
            break;
        case api_errorcode_password_required_or_wrong:
            // ignore;
            break;
        default:
            logger.warning("Unknown cached errorcode: " + presetErrorcode);
            break;
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null || acc.getType() == AccountType.FREE) {
            /* no account or free account, yes we can expect captcha */
            return true;
        } else {
            /* Premium accounts do not have captchas */
            return false;
        }
    }

    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account) throws PluginException {
        try {
            final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
            checkErrorsAPI(br, link, account, entries);
        } catch (final JSonMapperException jse) {
            /* Assume we got html code and check for errors in html code */
            try {
                this.checkErrorsWebsite(br, link, account);
            } catch (final PluginException e) {
                throw Exceptions.addSuppressed(e, jse);
            }
            logger.log(jse);
            throw new AccountUnavailableException("Invalid API response", 1 * 60 * 1000l);
        }
    }

    /**
     * Handle errorcodes according to: https://docs.uptobox.com/#status-code
     *
     * @throws PluginException
     */
    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account, final Map<String, Object> entries) throws PluginException {
        try {
            String errorMsg = (String) entries.get("message");
            if (StringUtils.isEmpty(errorMsg)) {
                errorMsg = "Unknown error";
            }
            final int errorCode = ((Number) entries.get("statusCode")).intValue();
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
                invalidLogin();
            case 5:
                /* Premium account required to download this file */
                throw new AccountRequiredException(errorMsg);
            case 7:
                /*
                 * E.g. {"statusCode":7,"message":"Invalid Parameter","data":"bad file_code"} --> Should never happen - will only happen if
                 * you e.g. try to download a file_code in an invalid format e.g. too long.
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            case 11:
                /* "Too much fail attempt" --> Wait some more time than usually */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 15 * 60 * 1000l);
            case 13:
                /* "Invalid token" --> Permanently disable account */
                invalidLogin();
            case 39:
                /* Waittime between multiple (free account) downloads -> Temp. disable account */
                long waitSeconds = JavaScriptEngineFactory.toLong(JavaScriptEngineFactory.walkJson(entries, "data/waiting"), 0);
                if (waitSeconds <= 0) {
                    /* Fallback (should not be needed) */
                    waitSeconds = 5 * 60;
                }
                throw new AccountUnavailableException(errorMsg, waitSeconds * 1000l);
            case api_errorcode_password_required_or_wrong:
                link.setPasswordProtected(true);
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            case api_errorcode_file_offline:
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
                 * * Error 8 reads itself as if it was a "trafficlimit reached" error but it has nothingt todo with downloading, it is only
                 * for their "voucher reedem API call".
                 */
                if (link == null) {
                    throw new AccountUnavailableException(errorMsg, 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, errorMsg, 5 * 60 * 1000l);
                }
            }
        } catch (final JSonMapperException jse) {
            /* Assume we got html code and check for errors in html code */
            try {
                this.checkErrorsWebsite(br, link, account);
            } catch (PluginException e) {
                throw Exceptions.addSuppressed(e, jse);
            }
            logger.log(jse);
            throw new AccountUnavailableException("Unvalid API response", 1 * 60 * 1000l);
        }
    }

    /** @return null = original (no stream download) */
    protected String getConfiguredQuality() {
        /* Returns user-set value which can be used to circumvent government based GEO-block. */
        PreferredQuality cfgquality = PluginJsonConfig.get(UpToBoxComConfig.class).getPreferredQuality();
        if (cfgquality == null) {
            cfgquality = PreferredQuality.DEFAULT;
        }
        switch (cfgquality) {
        case QUALITY1:
            return "360";
        case QUALITY2:
            return "480";
        case QUALITY3:
            return "720";
        case QUALITY4:
            return "1080";
        case QUALITY5:
            return "2160";
        case DEFAULT:
            return null;
        default:
            return null;
        }
    }

    private void invalidLogin() throws PluginException {
        if ("fr".equalsIgnoreCase(System.getProperty("user.language"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nToken invalide. / Vous pouvez trouver votre token ici : uptobox.com/my_account.\r\nSi vous utilisez JDownloader à distance/myjdownloader/headless, entrez le token dans les champs de nom d'utilisateur de de mot de passe.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        } else {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid token!\r\nYou can find your token here: uptobox.com/my_account\r\nIf you are running JDownloader headless or using myjdownloader, just put your token into the username and password field.", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new UptoboxAccountFactory(callback);
    }

    public static class UptoboxAccountFactory extends MigPanel implements AccountBuilderInterface {
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your token";

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
            add(new JLabel("Click here to find your token:"));
            add(new JLink("https://uptobox.com/my_account"));
            add(new JLabel("Token:"));
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
    public void resetDownloadlink(final DownloadLink link) {
    }
}