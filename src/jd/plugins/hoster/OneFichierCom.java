//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DefaultEditAccountPanel;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.components.config.OneFichierConfigInterface;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OneFichierCom extends PluginForHost {
    private final String         PROPERTY_FREELINK                 = "freeLink";
    private final String         PROPERTY_HOTLINK                  = "hotlink";
    private final String         PROPERTY_PREMLINK                 = "premLink";
    /** URLs can be restricted for various reason: https://1fichier.com/console/acl.pl */
    public static final String   PROPERTY_ACL_ACCESS_CONTROL_LIMIT = "acl_access_control_limit";
    /** 2019-04-04: Documentation: https://1fichier.com/api.html */
    public static final String   API_BASE                          = "https://api.1fichier.com/v1";
    /*
     * Max total connections for premium = 30 (RE: admin, updated 07.03.2019) --> See also their FAQ: https://1fichier.com/hlp.html#dllent
     */
    private static final boolean resume_account_premium            = true;
    private static final int     maxdownloads_account_premium      = 10;
    /* 2015-07-10: According to admin, resume in free mode is not possible anymore. On attempt this will lead to 404 server error! */
    private static final int     maxchunks_free                    = 1;
    private static final boolean resume_free                       = true;
    private static final int     maxdownloads_free                 = 1;
    /*
     * Settings for hotlinks - basically such links are created by premium users so free users can download them without limits (same limits
     * as premium users).
     */
    private static final boolean resume_free_hotlink               = true;
    private static final int     maxchunks_free_hotlink            = -3;
    private static final boolean PREFER_API_FOR_SINGLE_LINKCHECK   = false;

    @Override
    public String[] siteSupportedNames() {
        /* 1st domain = current domain! */
        final String[] supportedDomains = buildSupportedNames(getPluginDomains());
        final List<String> ret = new ArrayList<String>(Arrays.asList(supportedDomains));
        ret.add("1fichier");
        return ret.toArray(new String[0]);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "1fichier.com", "alterupload.com", "cjoint.net", "desfichiers.net", "dfichiers.com", "megadl.fr", "mesfichiers.org", "piecejointe.net", "pjointe.com", "tenvoi.com", "dl4free.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            /* URL format according to API page --> General: https://1fichier.com/api.html */
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/\\?([a-z0-9]{5,20})");
        }
        return ret.toArray(new String[0]);
    }

    public OneFichierCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.1fichier.com/en/register.pl");
    }

    @Override
    public void init() {
        setRequestIntervalLimits();
    }

    public static void setRequestIntervalLimits() {
        /** 2021-02-10: We use 2500ms as default, 1 request per second is also fine according to admin. */
        final OneFichierConfigInterface cfg = PluginJsonConfig.get(OneFichierConfigInterface.class);
        Browser.setRequestIntervalLimitGlobal("1fichier.com", cfg.getGlobalRequestIntervalLimit1fichierComMilliseconds());
        Browser.setRequestIntervalLimitGlobal("api.1fichier.com", true, cfg.getGlobalRequestIntervalLimitAPI1fichierComMilliseconds());
    }

    private String correctProtocol(final String input) {
        return input.replaceFirst("(?i)http://", "https://");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /**
         * 2019-04-24: Do NOT change domains here! Uploaders can decide which domain is the only valid domain for their files e.g.
         * "alterupload.com". Using their main domain (1fichier.com) will result in OFFLINE URLs!
         */
        final String fid = getFID(link);
        if (fid != null) {
            /* Use new/current linktype and keep original domain of inside added URL! */
            final String domainFromURL = Browser.getHost(link.getPluginPatternMatcher());
            link.setPluginPatternMatcher("https://" + domainFromURL + "/?" + fid);
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    /** Returns the unique file/link-ID of given downloadLink. */
    private String getFID(final DownloadLink link) {
        if (link.getPluginPatternMatcher() == null) {
            return null;
        } else {
            return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        }
    }

    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            final Browser br = new Browser();
            prepareBrowserWebsite(br);
            br.getHeaders().put("User-Agent", "");
            br.getHeaders().put("Accept", "");
            br.getHeaders().put("Accept-Language", "");
            br.setCookiesExclusive(true);
            final StringBuilder sb = new StringBuilder();
            final ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            while (true) {
                links.clear();
                while (true) {
                    /* we test 100 links at once */
                    if (index == urls.length || links.size() == 100) {
                        break;
                    } else {
                        links.add(urls[index]);
                        index++;
                    }
                }
                sb.delete(0, sb.capacity());
                for (final DownloadLink dl : links) {
                    sb.append("links[]=");
                    sb.append(Encoding.urlEncode(dl.getPluginPatternMatcher()));
                    sb.append("&");
                }
                // remove last &
                sb.deleteCharAt(sb.length() - 1);
                /**
                 * This method is serverside deprecated but we're still using it because: </br> 1. It is still working. </br> 2. It is the
                 * only method that can be used to check multiple items with one request.
                 */
                br.postPageRaw(correctProtocol("http://" + this.getHost() + "/check_links.pl"), sb.toString());
                for (final DownloadLink dllink : links) {
                    // final String addedLink = dllink.getDownloadURL();
                    final String addedlink_id = this.getFID(dllink);
                    if (addedlink_id == null) {
                        // invalid uid
                        dllink.setAvailable(false);
                    } else if (br.containsHTML(addedlink_id + "[^;]*;;;(NOT FOUND|BAD LINK)")) {
                        dllink.setAvailable(false);
                        if (!dllink.isNameSet()) {
                            dllink.setName(addedlink_id);
                        }
                    } else if (br.containsHTML(addedlink_id + "[^;]*;;;PRIVATE")) {
                        /**
                         * Private or password protected file. </br> Admin was asked to change this to return a more precise status instead
                         * but declined that suggestion.
                         */
                        dllink.setProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT, true);
                        dllink.setAvailable(true);
                        if (!dllink.isNameSet()) {
                            dllink.setName(addedlink_id);
                        }
                    } else {
                        final String[] linkInfo = br.getRegex(addedlink_id + "[^;]*;([^;]+);(\\d+)").getRow(0);
                        if (linkInfo.length != 2) {
                            logger.warning("Linkchecker for 1fichier.com is broken!");
                            return false;
                        }
                        dllink.removeProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT);
                        dllink.setAvailable(true);
                        /* Trust API information. */
                        dllink.setFinalFileName(Encoding.htmlDecode(linkInfo[0]));
                        dllink.setVerifiedFileSize(Long.parseLong(linkInfo[1]));
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

    /* Old linkcheck removed AFTER revision 29396 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        if (PREFER_API_FOR_SINGLE_LINKCHECK && account != null && DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            return requestFileInformationAPI(br, link, account);
        } else {
            checkLinks(new DownloadLink[] { link });
            prepareBrowserWebsite(br);
            if (!link.isAvailabilityStatusChecked()) {
                return AvailableStatus.UNCHECKED;
            } else if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                return AvailableStatus.TRUE;
            }
        }
    }

    /** 2021-01-29: Not required at this moment. Review this before using it! Do not use this as it will cause IP-blocks!! */
    /**
     * Checks single URLs via API.
     *
     * @throws Exception
     */
    public AvailableStatus requestFileInformationAPI(final Browser br, final DownloadLink link, final Account account) throws Exception {
        prepareBrowserAPI(br, account);
        final Map<String, Object> postData = new HashMap<String, Object>();
        postData.put("url", link.getPluginPatternMatcher());
        postData.put("pass", link.getDownloadPassword());
        performAPIRequest(API_BASE + "/file/info.cgi", JSonStorage.serializeToJson(postData));
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String errorMsg = (String) entries.get("message");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. message": "Resource not found #469" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (errorMsg != null && errorMsg.matches("(?i).*Resource not allowed.*")) {
            /* 2020-01-30: e.g. {"status":"KO","message":"Resource not allowed #631"} */
            /*
             * Password-protected or private file: No information given at all but we know that file is online. Example reasons: file is not
             * allowed to be downloaded in current country, by current user, file is private, file is password protected.
             */
            link.setProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT, true);
            /* Else all is fine - URL is online but we might not be able to download it. */
            return AvailableStatus.TRUE;
        }
        link.removeProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT);
        final short passwordProtected = ((Number) entries.get("pass")).shortValue();
        if (passwordProtected == 1) {
            link.setPasswordProtected(true);
        } else {
            link.setPasswordProtected(false);
        }
        final String description = (String) entries.get("description");
        link.setName((String) entries.get("filename"));
        link.setVerifiedFileSize(((Number) entries.get("size")).longValue());
        if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
            link.setComment(description);
        }
        /* 2020-01-30: We cannot work with this checksum */
        // final String checksum = (String) entries.get("checksum");
        // if (!StringUtils.isEmpty(checksum)) {
        // link.setSha256Hash(checksum);
        // }
        return AvailableStatus.TRUE;
    }

    @Override
    protected int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account == null && (link != null && link.getProperty(PROPERTY_HOTLINK, null) != null)) {
            return Integer.MAX_VALUE;
        } else {
            return super.getMaxSimultanDownload(link, account);
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        /* Do not perform availablecheck here to save requests */
        // requestFileInformation(link);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        doFree(null, link);
    }

    private String regex_dllink_middle = "align:middle\">\\s+<a href=(\"|')(https?://[a-zA-Z0-9_\\-]+\\.(1fichier|desfichiers)\\.com/[a-zA-Z0-9]+.*?)\\1";

    public void doFree(final Account account, final DownloadLink link) throws Exception, PluginException {
        /* The following code will cover saved hotlinks */
        String dllink = link.getStringProperty(PROPERTY_HOTLINK);
        if (dllink != null) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_free_hotlink, maxchunks_free_hotlink);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                // link has expired... but it could be for any reason! dont care!
                // clear saved final link
                link.setProperty(PROPERTY_HOTLINK, Property.NULL);
                br = new Browser();
                prepareBrowserWebsite(br);
            } else {
                /* resume download */
                logger.info("Hotlink download active");
                link.setProperty(PROPERTY_HOTLINK, dllink);
                dl.startDownload();
                return;
            }
        }
        /* retry/resume of cached free link! */
        dllink = link.getStringProperty(PROPERTY_FREELINK);
        if (dllink != null) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_free, maxchunks_free);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                /* resume download */
                link.setProperty(PROPERTY_FREELINK, dllink);
                dl.startDownload();
                return;
            } else {
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                /* link has expired... but it could be for any reason! dont care! */
                /* Clear saved final link */
                link.removeProperty(PROPERTY_FREELINK);
                br.clearAll();
                prepareBrowserWebsite(br);
            }
        }
        final String contentURL = getContentURLWebsite(link);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, contentURL, resume_free_hotlink, maxchunks_free_hotlink);
        if (this.looksLikeDownloadableContent(dl.getConnection())) {
            /* Hotlink or user is using CDN credits for downloading */
            link.setProperty(PROPERTY_HOTLINK, dl.getConnection().getURL().toString());
            dl.startDownload();
            return;
        }
        /* Not hotlinkable.. standard free link... */
        br.followConnection();
        dllink = null;
        br.setFollowRedirects(false);
        boolean retried = false;
        int i = 0;
        while (true) {
            i++;
            if (i > 1) {
                br.setFollowRedirects(true);
                // no need to do this link twice as it's been done above.
                br.getPage(contentURL);
                br.setFollowRedirects(false);
            }
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            errorHandlingWebsite(link, account, br);
            if (this.getDownloadPasswordForm() != null) {
                handleDownloadPasswordWebsite(link);
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    dllink = br.getRegex(regex_dllink_middle).getMatch(1);
                    if (dllink == null) {
                        logger.warning("Failed to find final downloadlink after password handling success");
                        this.handleErrorsLastResortWebsite(link, account);
                    }
                }
                logger.info("Successfully went through the password handling");
                break;
            } else {
                // base > submit:Free Download > submit:Show the download link + t:35140198 == link
                final Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                sleep(2000, link);
                br2.postPageRaw(br.getURL(), "");
                errorHandlingWebsite(link, account, br2);
                dllink = br2.getRedirectLocation();
                if (dllink == null) {
                    dllink = br2.getRegex(regex_dllink_middle).getMatch(1);
                }
                if (dllink == null) {
                    final Form a2 = br2.getForm(0);
                    if (a2 == null) {
                        this.handleErrorsLastResortWebsite(link, account);
                    }
                    a2.remove("save");
                    final Browser br3 = br.cloneBrowser();
                    br3.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    sleep(2000, link);
                    br3.submitForm(a2);
                    errorHandlingWebsite(link, account, br3);
                    if (dllink == null) {
                        dllink = br3.getRedirectLocation();
                    }
                    if (dllink == null) {
                        dllink = br3.getRegex("<a href=\"([^<>\"]*?)\"[^<>]*?>\\s*Click here to download").getMatch(0);
                    }
                    if (dllink == null) {
                        dllink = br3.getRegex("window\\.location\\s*=\\s*('|\")(https?://[a-zA-Z0-9_\\-]+\\.(1fichier|desfichiers)\\.com/[a-zA-Z0-9]+/.*?)\\1").getMatch(1);
                    }
                    if (dllink == null) {
                        String wait = br3.getRegex("var count = (\\d+);").getMatch(0);
                        if (wait != null && retried == false) {
                            retried = true;
                            sleep(1000 * Long.parseLong(wait), link);
                            continue;
                        }
                        this.handleErrorsLastResortWebsite(link, account);
                    }
                }
            }
            if (dllink != null) {
                break;
            }
        }
        br.setFollowRedirects(true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_free, maxchunks_free);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            logger.warning("The final dllink seems not to be a file!");
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            errorHandlingWebsite(link, account, br);
            this.handleErrorsLastResortWebsite(link, account);
        }
        link.setProperty(PROPERTY_FREELINK, dllink);
        dl.startDownload();
    }

    private static void errorHandlingWebsite(final DownloadLink link, final Account account, final Browser ibr) throws Exception {
        long responsecode = 200;
        if (ibr.getHttpConnection() != null) {
            responsecode = ibr.getHttpConnection().getResponseCode();
        }
        if (ibr.containsHTML("(?i)>\\s*File not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (ibr.containsHTML("(?i)>\\s*Software error:<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Software error'", 10 * 60 * 1000l);
        } else if (ibr.containsHTML("(?i)>\\s*Connexion à la base de données impossible<|>Can\\'t connect DB")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
        } else if (ibr.containsHTML("(?i)not possible to free unregistered users")) {
            throw new AccountRequiredException();
        } else if (ibr.containsHTML("(?i)Your account will be unlock")) {
            if (account != null) {
                throw new AccountUnavailableException("Locked for security reasons", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP blocked for security reasons", 60 * 60 * 1000l);
            }
        } else if (ibr.containsHTML("(?i)>\\s*Access to this file is protected|>\\s*This file is protected")) {
            /* Access restricted by IP / only registered users / only premium users / only owner */
            if (ibr.containsHTML("(?i)>\\s*The owner of this file has reserved access to the subscribers of our services")) {
                throw new AccountRequiredException();
            } else {
                errorAccessControlLimit(link);
            }
        } else if (ibr.containsHTML("(?i)>\\s*Your requests are too fast")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Rate limit reached", 30 * 1000l);
        } else if (ibr.getURL().contains("/?c=DB")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
        } else if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 15 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (ibr.getHttpConnection().getResponseCode() == 503 && ibr.containsHTML("(?i)>\\s*Our services are in maintenance\\.\\s*Please come back after")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Hoster is in maintenance mode!", 20 * 60 * 1000l);
        } else {
            ipBlockedErrorHandling(account, ibr);
        }
    }

    /** Returns content-URL for website requests */
    private String getContentURLWebsite(final DownloadLink link) {
        String contentURL = link.getPluginPatternMatcher();
        if (contentURL.contains("?") && !contentURL.contains("&")) {
            /* 2021-07-27: Another attempt to force English language as only setting the cookie may not be enough. */
            contentURL += "&lg=en";
        }
        return contentURL;
    }

    /**
     * Access restricted by IP / only registered users / only premium users / only owner. </br> See here for all possible reasons (login
     * required): https://1fichier.com/console/acl.pl
     *
     * @throws PluginException
     */
    private static void errorAccessControlLimit(final DownloadLink link) throws PluginException {
        if (link != null) {
            link.setProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT, true);
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Access to this file has been restricted");
    }

    /** Contains all errorhandling for IP related limits/errormessages. */
    private static void ipBlockedErrorHandling(final Account account, final Browser br) throws PluginException {
        String waittimeMinutesStr = br.getRegex("you must wait (at least|up to)\\s*(\\d+)\\s*minutes between each downloads").getMatch(1);
        if (waittimeMinutesStr == null) {
            waittimeMinutesStr = br.getRegex(">\\s*You must wait\\s*(\\d+)\\s*minutes").getMatch(0);
            if (waittimeMinutesStr == null) {
                waittimeMinutesStr = br.getRegex(">\\s*Vous devez attendre encore\\s*(\\d+)\\s*minutes").getMatch(0);
            }
        }
        if (br.containsHTML("(?i)>\\s*IP Locked|>\\s*Will be unlocked within 1h\\.")) {
            waittimeMinutesStr = "60";
        }
        final int defaultWaitMinutes = 5;
        boolean isBlocked = waittimeMinutesStr != null;
        isBlocked |= br.containsHTML("(?i)/>\\s*Téléchargements en cours");
        isBlocked |= br.containsHTML("(?i)En téléchargement standard, vous ne pouvez télécharger qu\\'un seul fichier");
        isBlocked |= br.containsHTML("(?i)>\\s*veuillez patienter avant de télécharger un autre fichier");
        isBlocked |= br.containsHTML("(?i)>\\s*You already downloading (some|a) file");
        isBlocked |= br.containsHTML("(?i)>\\s*You can download only one file at a time");
        isBlocked |= br.containsHTML("(?i)>\\s*Please wait a few seconds before downloading new ones");
        isBlocked |= br.containsHTML("(?i)>\\s*You must wait for another download");
        isBlocked |= br.containsHTML("(?i)Without premium status, you can download only one file at a time");
        isBlocked |= br.containsHTML("(?i)Without Premium, you can only download one file at a time");
        isBlocked |= br.containsHTML("(?i)Without Premium, you must wait between downloads");
        // jdlog://3278035891641 jdlog://7543779150841
        isBlocked |= br.containsHTML("(?i)Warning ! Without subscription, you can only download one file at|<span style=\"color:red\">Warning\\s*!\\s*</span>\\s*<br/>Without subscription, you can only download one file at a time\\.\\.\\.");
        isBlocked |= br.containsHTML("(?i)>\\s*Votre adresse IP ouvre trop de connexions vers le serveur");
        if (isBlocked) {
            if (account != null) {
                final long waitMilliseconds;
                if (waittimeMinutesStr != null) {
                    waitMilliseconds = Integer.parseInt(waittimeMinutesStr) * 60 * 1001l;
                } else {
                    waitMilliseconds = defaultWaitMinutes * 60 * 1000l;
                }
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between downloads", waitMilliseconds);
            } else {
                final boolean preferReconnect = PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferReconnectEnabled();
                if (waittimeMinutesStr != null && preferReconnect) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittimeMinutesStr) * 60 * 1001l);
                } else if (waittimeMinutesStr != null && Integer.parseInt(waittimeMinutesStr) >= 10) {
                    /* High waittime --> Reconnect */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittimeMinutesStr) * 60 * 1001l);
                } else if (preferReconnect) {
                    /* User prefers reconnect --> Throw Exception with LinkStatus to trigger reconnect */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, defaultWaitMinutes * 60 * 1000l);
                } else if (waittimeMinutesStr != null) {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between download, Reconnect is disabled in plugin settings", Integer.parseInt(waittimeMinutesStr) * 60 * 1001l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between download, Reconnect is disabled in plugin settings", defaultWaitMinutes * 60 * 1001);
                }
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        if (canUseAPI(account)) {
            return fetchAccountInfoAPI(account);
        } else {
            return fetchAccountInfoWebsite(account);
        }
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        br.setAllowedResponseCodes(new int[] { 403, 503 });
        loginWebsite(account, true);
        /* And yet another workaround for broken API case ... */
        br.getPage("https://" + this.getHost() + "/en/console/index.pl");
        final boolean isPremium = br.containsHTML("(?i)>\\s*Premium\\s*(offer)\\s*Account\\s*<");
        final boolean isAccess = br.containsHTML("(?i)>\\s*Access\\s*(offer)\\s*Account\\s*<");
        // final boolean isFree = br.containsHTML(">\\s*Free\\s*(offer)\\s*Account\\s*<");
        String accountStatus = null;
        if (isPremium || isAccess) {
            final GetRequest get = new GetRequest("https://" + this.getHost() + "/en/console/abo.pl");
            get.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            br.getPage(get);
            final String validUntil = br.getRegex("(?i)subscription is valid until\\s*<[^<]*>(\\d+-\\d+-\\d+)").getMatch(0);
            if (validUntil != null) {
                final long validUntilTimestamp = TimeFormatter.getMilliSeconds(validUntil, "yyyy'-'MM'-'dd", Locale.FRANCE);
                if (validUntilTimestamp > 0) {
                    setValidUntil(ai, validUntilTimestamp);
                }
            }
            if (isPremium) {
                accountStatus = "Premium Account";
            } else {
                accountStatus = "Access Account";
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(maxdownloads_account_premium);
            account.setConcurrentUsePossible(true);
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(maxdownloads_free);
            account.setConcurrentUsePossible(false);
            final GetRequest get = new GetRequest("https://" + this.getHost() + "/en/console/params.pl");
            get.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            br.getPage(get);
            accountStatus = "Free Account";
        }
        ai.setUnlimitedTraffic();
        /* Credits are only relevant if usage of credits for downloads is enabled: https://1fichier.com/console/params.pl */
        final String creditsStr = br.getRegex("(?)>\\s*Your account have ([^<>\"]*?) of[^<]*credits").getMatch(0);
        final boolean useOwnCredits = StringUtils.equalsIgnoreCase("checked", br.getRegex("<input\\s*type=\"checkbox\"\\s*checked=\"(.*?)\"[^>]*name=\"own_credit\"").getMatch(0));
        long creditsAsFilesize = 0;
        if (creditsStr != null) {
            creditsAsFilesize = SizeFormatter.getSize(creditsStr);
        }
        if (creditsAsFilesize > 0) {
            if (account.getType() == AccountType.FREE) {
                ai.setTrafficLeft(creditsAsFilesize);
                /* Display traffic but do not care about how much is actually left. */
                ai.setSpecialTraffic(true);
            }
            if (useOwnCredits) {
                accountStatus += " [Using credits]";
                if (account.getType() == AccountType.FREE) {
                    /* Treat Free accounts like a premium account if credits are used. */
                    account.setMaxSimultanDownloads(maxdownloads_account_premium);
                }
            } else {
                accountStatus += " [NOT using credits]";
            }
        }
        ai.setStatus(accountStatus);
        return ai;
    }

    /** Sets end of the day of given timestamp as validUntil date. */
    private void setValidUntil(final AccountInfo ai, final long originalValidUntilTimestamp) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(originalValidUntilTimestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        ai.setValidUntil(calendar.getTimeInMillis());
    }

    /**
     * 2019-04-04: This API can only be used by premium users! It might still work when a premium account expires and the key stays valid
     * but we don't know this yet!
     */
    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        prepareBrowserAPI(br, account);
        /*
         * This request can only be used every ~5 minutes - using it more frequently will e.g. cause response:
         * {"status":"KO","message":"Flood detected: IP Locked #38"} [DOWNLOADS VIA API WILL STILL WORK!!]
         */
        performAPIRequest(API_BASE + "/user/info.cgi", "");
        AccountInfo ai = new AccountInfo();
        final String apierror = this.getAPIErrormessage(br);
        final boolean apiTempBlocked = !StringUtils.isEmpty(apierror) && apierror.matches("(?i)Flood detected: (User|IP) Locked.*?");
        if (apiTempBlocked) {
            if (account.lastUpdateTime() > 0) {
                logger.info("Cannot get account details because of API limits but account has been checked before and is ok");
                // /* Return last accountInfo if available */
                // if (account.getAccountInfo() != null) {
                // logger.info("Returning last accountInfo");
                // ai = account.getAccountInfo();
                // ai.setStatus("Premium account (can't display more detailed info at this moment)");
                // return ai;
                // }
            } else {
                /*
                 * Account got added for the first time but API is blocked at the moment. We know the account must be premium because only
                 * premium users can generate APIKeys but we cannot get any information at the moment ...
                 */
                logger.info("Cannot get account details because of API limits and account has never been checked before --> Adding account without info");
            }
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account (can't display more detailed info at this moment)");
            account.setMaxSimultanDownloads(maxdownloads_account_premium);
            account.setConcurrentUsePossible(true);
            ai.setUnlimitedTraffic();
            return ai;
        }
        handleErrorsAPI(account);
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final String mail = (String) entries.get("email");
        if (!StringUtils.isEmpty(mail)) {
            /* don't store the complete username for security purposes. */
            final String shortuserName = "***" + mail.substring(3, mail.length());
            account.setUser(shortuserName);
        }
        final String subscription_end = (String) entries.get("subscription_end");
        final Object available_credits_in_gigabyteO = entries.get("cdn");
        double available_credits_in_gigabyte = 0;
        if (available_credits_in_gigabyteO != null) {
            if (available_credits_in_gigabyteO instanceof Number) {
                available_credits_in_gigabyte = ((Number) available_credits_in_gigabyteO).doubleValue();
            } else {
                available_credits_in_gigabyte = Double.parseDouble(available_credits_in_gigabyteO.toString());
            }
        }
        final long useOwnCredits = JavaScriptEngineFactory.toLong(entries.get("use_cdn"), 0);
        long validuntil = 0;
        if (!StringUtils.isEmpty(subscription_end)) {
            validuntil = TimeFormatter.getMilliSeconds(subscription_end, "yyyy-MM-dd HH:mm:ss", Locale.FRANCE);
        }
        ai.setUnlimitedTraffic();
        String accountStatus;
        if (validuntil > System.currentTimeMillis()) {
            /* Premium */
            account.setType(AccountType.PREMIUM);
            accountStatus = "Premium Account";
            account.setMaxSimultanDownloads(maxdownloads_account_premium);
            account.setConcurrentUsePossible(true);
            this.setValidUntil(ai, validuntil);
        } else {
            /* Free --> 2019-07-18: API Keys are only available for premium users so this should never happen! */
            account.setType(AccountType.FREE);
            accountStatus = "Free Account";
            account.setMaxSimultanDownloads(maxdownloads_free);
            account.setConcurrentUsePossible(false);
        }
        /* 2019-04-04: Credits are only relevant for free accounts according to website: https://1fichier.com/console/params.pl */
        String creditsStatus = "";
        if (available_credits_in_gigabyte > 0) {
            if (account.getType() == AccountType.FREE) {
                ai.setTrafficLeft((long) available_credits_in_gigabyte);
                /* Display traffic but do not care about how much is actually left. */
                ai.setSpecialTraffic(true);
            }
            if (useOwnCredits == 1) {
                creditsStatus += " [Using credits]";
                if (account.getType() == AccountType.FREE) {
                    /* Treat Free accounts like a premium account if credits are used. */
                    account.setMaxSimultanDownloads(maxdownloads_account_premium);
                }
            } else {
                creditsStatus += " [NOT using credits]";
            }
            accountStatus += creditsStatus;
        }
        ai.setStatus(accountStatus);
        return ai;
    }

    /**
     * Check- and handle API errors
     *
     * @throws PluginException
     */
    private void handleErrorsAPI(final Account account) throws PluginException {
        Map<String, Object> entries = null;
        try {
            entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        } catch (final Exception ignore) {
            throw new AccountUnavailableException("Invalid API response", 60 * 1000);
        }
        final Object statusO = entries.get("status");
        if (statusO != null && "KO".equalsIgnoreCase((String) statusO)) {
            final String message = (String) entries.get("message");
            if (StringUtils.isEmpty(message)) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error", 5 * 60 * 1000l);
            } else if (message.matches("(?i)Flood detected: IP Locked #\\d+")) {
                /*
                 * 2019-07-18: This may even happen on the first login attempt. When this happens we cannot know whether the account is
                 * valid or not!
                 */
                throw new AccountUnavailableException("API flood detection has been triggered", 5 * 60 * 1000l);
            } else if (message.matches("(?i)Flood detected: User Locked #\\d+")) {
                throw new AccountUnavailableException("API flood detection has been triggered", 5 * 60 * 1000l);
            } else if (message.matches("(?i)Not authenticated #\\d+")) {
                /* Login required but not logged in (this should never happen) */
                errorInvalidAPIKey(account);
            } else if (message.matches("(?i)No such user\\s*#\\d+")) {
                errorInvalidAPIKey(account);
            } else if (message.matches("(?i)Owner locked\\s*#\\d+")) {
                /* 2021-01-29 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account banned: " + message, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (message.matches("(?i)IP Locked\\s*#\\d+")) {
                throw new AccountUnavailableException(message, 60 * 60 * 1000l);
            } else if (message.matches("(?i).*Must be a customer.*")) {
                /* 2020-06-09: E.g. {"message":"Must be a customer (Premium, Access) #200","status":"KO"} */
                /* Free account (most likely expired premium) apikey entered by user --> API can only be used by premium users */
                // showAPIFreeAccountLoginFailureInformation();
                final AccountInfo ai = new AccountInfo();
                ai.setExpired(true);
                account.setAccountInfo(ai);
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Premium expired: Only premium users can use the 1fichier API)", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else if (isAPIErrorPassword(message)) {
                /* 2021-02-10: This will usually be handled outside of this errorhandling! */
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            } else if (message.matches("(?i).*Resource not allowed #\\d+")) {
                errorAccessControlLimit(this.getDownloadLink());
            } else if (message.matches("(?i).*Resource not found #\\d+")) {
                /* Usually goes along with http response 404 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (message.matches("(?i).*Only \\d+ locations? allowed at a time\\s*#\\d+")) {
                /* 2021-03-17: Tmp. account ban e.g. because of account sharing or user is just using it with too many IPs. */
                throw new AccountUnavailableException(message, 5 * 60 * 1000l);
            } else {
                /* Unknown/unhandled error */
                logger.warning("Handling unknown API error: " + message);
                if (this.getDownloadLink() == null) {
                    /* Account error */
                    throw new AccountUnavailableException(message, 5 * 60 * 1000l);
                } else {
                    /* Error during download/linkcheck */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, message, 5 * 60 * 1000l);
                }
            }
        }
    }

    private boolean isAPIErrorPassword(final String errorMsg) {
        if (errorMsg == null) {
            return false;
        } else if (errorMsg.matches("(?i).*(Invalid password\\.|Password not provided\\.).*Resource not allowed.*")) {
            return true;
        } else {
            return false;
        }
    }

    private static void errorInvalidAPIKey(final Account account) throws PluginException {
        if (account != null) {
            // clear invalid apiKey
            account.setPass(null);
            /* Assume APIKey is invalid or simply not valid anymore (e.g. user disabled or changed APIKey) */
            throw new AccountInvalidException("Invalid API Key - you can find your API Key here: 1fichier.com/console/params.pl\r\nPlease keep in mind that API Keys are only available for premium customers.\r\nIf you do not own a premium account, disable the API Key setting in JD plugin settings so that you can login via username & password!\r\nKeep in mind that 2-factor-authentification login via JD and username/password is not supported!\r\nIf you want to login into your FREE 1fichier account in JD via username & password you will first have to disable 2-factor-authentication in your 1fichier account!");
        } else {
            throw new AccountRequiredException();
        }
    }

    private String getAPIErrormessage(final Browser br) {
        try {
            final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
            return (String) entries.get("message");
        } catch (final Throwable e) {
            /* E.g. no valid json in browser. */
            return null;
        }
    }

    /**
     * 2020-06-10: This message was designed to be displayed whenever a premium account which was used in API mode is not premium anymore.
     * Because free accounts of this host are pretty much useless and we do not want to encourage users to use the website mode, this has
     * only been used in one revision for a short time.
     */
    @Deprecated
    private Thread showAPIFreeAccountLoginFailureInformation() {
        final Thread thread = new Thread() {
            public void run() {
                try {
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = "1fichier.com - Free Account Login";
                        message += "Hallo liebe(r) 1fichier NutzerIn\r\n";
                        message += "Du hast gerade versucht, einen kostenlosen 1fichier Account im API Modus zu verwenden oder dein Account war bis vor kurzem ein Premium Account und ist nun abgelaufen.\r\n";
                        message += "Im API Modus ist die Verwendung eines kostenlosen 1fichier Accounts nicht möglich.\r\n";
                        message += "Falls du dennoch einen solchen Account in JDownloader verwenden möchtest, beachte bitte die folgende Anleitung:\r\n";
                        message += "1. Deaktiviere die 2-Faktor-Authentifizierung deines 1fichier Accounts - diese wird von JD nicht unterstützt!\r\n";
                        message += "2. DEAKTIVIERE die folgende Einstellung: Einstellungen --> Plugins --> 1fichier.com --> \"Use premium API?\"\r\n";
                        message += "3. Jetzt kannst du deinen kostenlosen 1fichier Account mit E-Mail und Passwort eingeben.\r\n";
                    } else {
                        title = "1fichier.com - Free Account Login";
                        message += "Hello dear 1fichier user\r\n";
                        message += "You've just tried to add a free 1fichier account to JDownloader in API mode or your former premikum account has expired and is now a free account.\r\n";
                        message += "Using a 1fichier free account in API mode is impossible.\r\n";
                        message += "If you're planning to use your free 1fichier account in JDownloader nonetheless, please follow these instructions:\r\n";
                        message += "1. If enabled, deactivate the 2-factor-authentication in your 1fichier account - JD does not support this.\r\n";
                        message += "2. DEACTIVATE the following setting: Settings --> Plugins --> 1fichier.com --> \"Use premium API?\"\r\n";
                        message += "3. Now you can add your 1fichier free account via E-Mail and password.\r\n";
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    final ConfirmDialogInterface ret = UIOManager.I().show(ConfirmDialogInterface.class, dialog);
                    ret.throwCloseExceptions();
                } catch (final Throwable e) {
                    getLogger().log(e);
                }
            };
        };
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    /** Checks whether we're logged in via website. */
    private boolean isLoggedinWebsite(final Browser br) {
        if (isLoginCookieExists(br) && br.containsHTML("/logout\\.pl")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isLoginCookieExists(final Browser br) {
        if (br.getCookie(this.getHost(), "SID", Cookies.NOTDELETEDPATTERN) != null) {
            return true;
        } else {
            return false;
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                prepareBrowserWebsite(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(this.getHost(), cookies);
                    setBasicAuthHeader(br, account);
                    if (!force) {
                        logger.info("Trust cookies without check");
                        return;
                    } else {
                        br.getPage("https://" + this.getHost() + "/console/index.pl");
                        if (isLoggedinWebsite(br)) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(getHost()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearAll();
                            this.prepareBrowserWebsite(this.br);
                        }
                    }
                }
                logger.info("Performing full website login");
                final String username = account.getUser();
                final String password = account.getPass();
                if (username == null || !username.matches(".+@.+")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "You need to use Email as username!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (password == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "No password given!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final UrlQuery query = new UrlQuery();
                query.add("mail", Encoding.urlEncode(username));
                query.add("pass", Encoding.urlEncode(password));
                query.add("lt", "on"); // long term session
                query.add("other", "on"); // set cookies also on other 1fichier domains
                query.add("valider", "ok");
                br.postPage("https://" + this.getHost() + "/login.pl", query);
                Form twoFAForm = null;
                final String formKey2FA = "tfa";
                final Form[] forms = br.getForms();
                for (final Form form : forms) {
                    final InputField twoFAField = form.getInputField(formKey2FA);
                    if (twoFAField != null) {
                        twoFAForm = form;
                        break;
                    }
                }
                if (!isLoggedinWebsite(this.br) && twoFAForm != null) {
                    logger.info("2FA code required");
                    final DownloadLink dl_dummy;
                    if (this.getDownloadLink() != null) {
                        dl_dummy = this.getDownloadLink();
                    } else {
                        dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
                    }
                    String twoFACode = getUserInput("Enter 2-Factor authentication code", dl_dummy);
                    if (twoFACode != null) {
                        twoFACode = twoFACode.trim();
                    }
                    if (twoFACode == null || !twoFACode.matches("\\d{6}")) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new AccountUnavailableException("\r\nUngültiges Format der 2-faktor-Authentifizierung!", 1 * 60 * 1000l);
                        } else {
                            throw new AccountUnavailableException("\r\nInvalid 2-factor-authentication code format!", 1 * 60 * 1000l);
                        }
                    }
                    logger.info("Submitting 2FA code");
                    twoFAForm.put(formKey2FA, twoFACode);
                    br.submitForm(twoFAForm);
                    if (!isLoggedinWebsite(this.br)) {
                        if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                            throw new AccountUnavailableException("\r\nUngültiger 2-faktor-Authentifizierungscode!", 1 * 60 * 1000l);
                        } else {
                            throw new AccountUnavailableException("\r\nInvalid 2-factor-authentication code!", 1 * 60 * 1000l);
                        }
                    }
                }
                if (!isLoggedinWebsite(this.br)) {
                    final String errorTooManyLoginAttempts = br.getRegex("(?i)>(More than \\d+ login try per \\d+ minutes is not allowed)").getMatch(0);
                    if (errorTooManyLoginAttempts != null) {
                        throw new AccountUnavailableException(errorTooManyLoginAttempts, 1 * 60 * 1000l);
                    }
                    if (br.containsHTML("(?i)following many identification errors")) {
                        if (br.containsHTML("(?i)Your account will be unlock")) {
                            throw new AccountUnavailableException("Your account will be unlocked within 1 hour", 60 * 60 * 1000l);
                        } else if (br.containsHTML("(?i)your IP address") && br.containsHTML("(?i)is temporarily locked")) {
                            throw new AccountUnavailableException("For security reasons, following many identification errors, your IP address is temporarily locked.", 60 * 60 * 1000l);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                    logger.info("Username/Password also invalid via site login or user has 2FA login enabled!");
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort oder Zwei-Faktor-Authentifizierung aktiviert!\r\nFalls du die Zwei-Faktor-Authentifizierung aktiviert hast, kannst du diese deaktivieren und es erneut versuchen.\r\nPremium Benutzer können die Zwei-Faktor-Authentifizierung aktiviert lassen und es per API Login erneut versuchen.\r\nErklärung des Logins per API (nur premium Benutzer):\r\n1. Login per API in den JD Einstellungen aktivieren: Einstellungen -> Plugins -> 1fichier.com -> Use Premium API\r\n2. Deinen API Key von der 1fichier Webseite kopieren: 1fichier.com/console/params.pl\r\n3. Erneut versuchen, deinen 1fichier Account in JD hinzuzufügen und dabei den API Key eingeben.\r\nFalls du myjdownloader verwendest, gib deinen API Key in das Benutzername- & Passwort Feld ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password or 2-factor-authentication enabled!\r\nIf you have 2-factor-authentication enabled, you have to disable it and try again.\r\nPremium users can leave 2FA login enabled and try again via API login:\r\nHow to login in JD via API key (premium users only):\r\n1. Enable API Key login for JD via Settings -> Plugins -> 1fichier.com -> Use Premium API\r\n2. Get your API Key from the following webpage: 1fichier.com/console/params.pl\r\n3. Open this add-account-dialog again and enter your API key to add your account to JD.\r\nIn case you are using myjdownloader, enter your API Key in both the username- and password field.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                setBasicAuthHeader(br, account);
                account.saveCookies(br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.1fichier.com/en/cgu.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return maxdownloads_free;
    }

    @Override
    protected long getStartIntervall(final DownloadLink link, final Account account) {
        if (account == null || !AccountType.PREMIUM.equals(account.getType()) || link == null) {
            return super.getStartIntervall(link, account);
        } else {
            final long knownDownloadSize = link.getKnownDownloadSize();
            if (knownDownloadSize <= 50 * 1024 * 1024) {
                final int wait = PluginJsonConfig.get(OneFichierConfigInterface.class).getSmallFilesWaitInterval();
                // avoid IP block because of too many downloads in short time
                return Math.max(0, wait * 1000);
            } else {
                return super.getStartIntervall(link, account);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (AccountType.FREE.equals(account.getType())) {
            /**
             * Website mode is required for free account downloads
             */
            loginWebsite(account, false);
            doFree(account, link);
            return;
        } else {
            int maxChunks = PluginJsonConfig.get(OneFichierConfigInterface.class).getMaxPremiumChunks();
            if (maxChunks == 1) {
                maxChunks = 1;
            } else if (maxChunks < 1 || maxChunks >= 20) {
                maxChunks = 0;
            } else {
                maxChunks = -maxChunks;
            }
            logger.info("Max Chunks:" + maxChunks);
            if (!this.attemptStoredDownloadurlDownload(link, PROPERTY_PREMLINK, resume_account_premium, maxChunks)) {
                /**
                 * 2021-02-11: Don't do availablecheck in premium mode to reduce requests. </br> According to their admin, using the public
                 * availablecheck call just before downloading via API can be troublesome
                 */
                String dllink;
                if (canUseAPI(account)) {
                    dllink = getDllinkPremiumAPI(link, account);
                } else {
                    dllink = getDllinkPremiumWebsite(link, account);
                }
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled()) {
                    dllink = dllink.replace("http://", "https://");
                } else {
                    dllink = dllink.replaceFirst("https://", "http://");
                }
                br.setFollowRedirects(true);
                link.setProperty(PROPERTY_PREMLINK, dllink);
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_account_premium, maxChunks);
                if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                    logger.warning("The final dllink seems not to be a file!");
                    try {
                        br.followConnection(true);
                    } catch (final IOException e) {
                        logger.log(e);
                    }
                    errorHandlingWebsite(link, account, br);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
                }
            }
            dl.startDownload();
        }
    }

    private String getDllinkPremiumAPI(final DownloadLink link, final Account account) throws Exception {
        /**
         * 2019-04-05: At the moment there are no benefits for us when using this. </br> 2021-01-29: Removed this because if login is
         * blocked because of "flood control" this won't work either!
         */
        final boolean checkFileInfoBeforeDownloadAttempt = false;
        if (checkFileInfoBeforeDownloadAttempt) {
            requestFileInformationAPI(this.br, link, account);
        }
        setPremiumAPIHeaders(br, account);
        /* Do NOT trust pwProtected as this is obtained via website or old mass-linkcheck API!! */
        String dllink = null;
        synchronized (lastSessionPassword) {
            String passCode = link.getDownloadPassword();
            /* Check if we already know that this file is password protected ... */
            /** Try passwords in this order: 1. DownloadLink stored password, 2. Last used password, 3. Ask user */
            boolean usedLastPassword = false;
            if (link.isPasswordProtected()) {
                if (passCode == null) {
                    if (lastSessionPassword.get() != null) {
                        usedLastPassword = true;
                        passCode = lastSessionPassword.get();
                    } else {
                        passCode = getUserInput("Password?", link);
                    }
                }
            }
            /**
             * TODO: Check if/when we need additional json POST parameters: inline, restrict_ip, no_ssl, folder_id, sharing_user
             */
            /** Description of optional parameters: cdn=0/1 - use download-credits, */
            performAPIRequest(API_BASE + "/download/get_token.cgi", String.format("{\"url\":\"%s\",\"pass\":\"%s\"}", link.getPluginPatternMatcher(), passCode));
            final String apiError = this.getAPIErrormessage(br);
            /**
             * 2021-02-10: This will ask for a password for all kinds of access limited files. They will have to update their API to fix
             * this. Example self uploaded file, only downloadable from afghanistan: https://1fichier.com/?uczre58xge6pif2d9n6g
             */
            if (isAPIErrorPassword(apiError)) {
                if (usedLastPassword) {
                    lastSessionPassword.set(null);
                } else {
                    link.setDownloadPassword(null);
                }
                if (link.isPasswordProtected()) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                } else {
                    link.setPasswordProtected(true);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password required!");
                }
            } else if (passCode != null) {
                lastSessionPassword.set(passCode);
                link.setDownloadPassword(passCode);
            } else {
                /* File is not password protected (anymore) */
                link.setPasswordProtected(false);
            }
        }
        /* 2019-04-04: Downloadlink is officially only valid for 5 minutes */
        handleErrorsAPI(account);
        /* TODO: Use json parser */
        dllink = PluginJSonUtils.getJson(br, "url");
        if (StringUtils.isEmpty(dllink)) {
            /* This should never happen */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to find final downloadurl");
        }
        return dllink;
    }

    private String getDllinkPremiumWebsite(final DownloadLink link, final Account account) throws Exception {
        String dllink = null;
        loginWebsite(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getPluginPatternMatcher());
        // error checking, offline links can happen here.
        errorHandlingWebsite(link, account, br);
        dllink = br.getRedirectLocation();
        if (this.getDownloadPasswordForm() != null) {
            handleDownloadPasswordWebsite(link);
            /*
             * The users' 'direct download' setting has no effect on the password handling so we should always get a redirect to the final
             * downloadlink after having entered the correct download password (for premium users).
             */
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                dllink = br.getRegex(regex_dllink_middle).getMatch(1);
                if (dllink == null) {
                    logger.warning("After successful password handling: Final downloadlink 'dllink' is null");
                    this.handleErrorsLastResortWebsite(link, account);
                }
            }
        }
        ipBlockedErrorHandling(account, br);
        if (dllink == null) {
            /* The link is always SSL - based on user setting it will redirect to either https or http. */
            String postData = "did=0&";
            postData += getSSLFormValue();
            br.postPage(getContentURLWebsite(link), postData);
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                if (br.containsHTML("(?i)\">Warning \\! Without premium status, you can download only")) {
                    logger.info("Seems like this is no premium account or it's vot valid anymore -> Disabling it");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML("(?i)>\\s*You can use your account only for downloading from") || br.containsHTML(">\\s*Our services are not compatible with massively shared internet access") || br.containsHTML(">\\s*Be carrefull? to not use simultaneously your IPv4 and IPv6 IP")) {
                    logger.warning("Your using account on multiple IP addresses at once");
                    throw new AccountUnavailableException("Account been used on another Internet connection", 10 * 60 * 1000l);
                } else {
                    logger.warning("Final downloadlink 'dllink' is null");
                    this.handleErrorsLastResortWebsite(link, account);
                }
            }
        }
        return dllink;
    }

    private void performAPIRequest(final String url, final String json_params) throws IOException {
        PostRequest downloadReq = br.createJSonPostRequest(url, json_params);
        downloadReq.setContentType("application/json");
        br.openRequestConnection(downloadReq);
        br.loadConnection(null);
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String property, final boolean resume, final int maxchunks) throws Exception {
        String url = link.getStringProperty(property);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        final boolean preferSSL = PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled();
        if (preferSSL) {
            url = url.replaceFirst("http://", "https://");
        } else {
            url = url.replaceFirst("https://", "http://");
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(property);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    /** Required to authenticate via API. Wrapper for setPremiumAPIHeaders(String). */
    public static void setPremiumAPIHeaders(final Browser br, final Account account) throws PluginException {
        final String apiKey = getAPIKey(account);
        if (apiKey == null) {
            errorInvalidAPIKey(account);
        } else {
            setPremiumAPIHeaders(br, apiKey);
        }
    }

    public static String getAPIKey(final Account account) {
        /* 2021-02-18: Remove linebreaks from the end RE forum: https://board.jdownloader.org/showthread.php?t=83954 */
        final String apiKey = StringUtils.trim(account != null ? account.getPass() : null);
        if (isApiKey(apiKey)) {
            return apiKey;
        } else {
            return null;
        }
    }

    public static boolean canUseAPI(final Account account) {
        /**
         * true = use premium API, false = use combination of website + OLD basic auth API - ONLY RELEVANT FOR PREMIUM USERS; IF ENABLED,
         * USER HAS TO ENTER API_KEY INSTEAD OF USERNAME:PASSWORD (or APIKEY:APIKEY)!!
         */
        if (account != null && (account.getType() == AccountType.PREMIUM || account.getType() == AccountType.UNKNOWN) && getAPIKey(account) != null) {
            return PluginJsonConfig.get(OneFichierConfigInterface.class).isUsePremiumAPIEnabled();
        } else {
            return false;
        }
    }

    /** Required to authenticate via API. */
    public static void setPremiumAPIHeaders(final Browser br, final String apiKey) {
        br.getHeaders().put("Authorization", "Bearer " + apiKey);
    }

    private void setBasicAuthHeader(final Browser br, final Account account) {
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
    }

    private static AtomicReference<String> lastSessionPassword = new AtomicReference<String>(null);

    private Form getDownloadPasswordForm() throws Exception {
        final Form ret = br.getFormbyKey("pass");
        if (ret != null && this.canHandle(ret.getAction())) {
            return ret;
        } else {
            return null;
        }
    }

    @Override
    public Class<OneFichierConfigInterface> getConfigInterface() {
        return OneFichierConfigInterface.class;
    }

    private void handleDownloadPasswordWebsite(final DownloadLink link) throws Exception {
        synchronized (lastSessionPassword) {
            logger.info("Handling supposedly password protected link...");
            final Form pwform = getDownloadPasswordForm();
            /** Try passwords in this order: 1. DownloadLink stored password, 2. Last used password, 3. Ask user */
            boolean usedLastPassword = false;
            String passCode = link.getDownloadPassword();
            if (passCode == null) {
                if (lastSessionPassword.get() != null) {
                    usedLastPassword = true;
                    passCode = lastSessionPassword.get();
                } else {
                    passCode = getUserInput("Password?", link);
                }
            }
            pwform.put("pass", Encoding.urlEncode(passCode));
            /*
             * Set pw protected flag so in case this downloadlink is ever tried to be downloaded via API, we already know that it is
             * password protected!
             */
            link.setPasswordProtected(true);
            /** That is a multi purpose Form containing some default fields which we don't want or and to correct. */
            pwform.remove("save");
            pwform.put("did", "1");
            br.submitForm(pwform);
            if (getDownloadPasswordForm() != null) {
                if (usedLastPassword) {
                    lastSessionPassword.set(null);
                } else {
                    link.setDownloadPassword(null);
                }
                throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
            } else {
                /* Save download-password */
                lastSessionPassword.set(passCode);
                link.setDownloadPassword(passCode);
            }
        }
    }

    private boolean isaccessControlLimited(final DownloadLink link) {
        if (link.hasProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT)) {
            return true;
        } else {
            return false;
        }
    }

    /* Returns postPage key + data based on the users' SSL preference. */
    private String getSSLFormValue() {
        String formdata;
        if (PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled()) {
            logger.info("User prefers download with SSL");
            formdata = "dlssl=SSL+Download";
        } else {
            logger.info("User prefers download without SSL");
            formdata = "dl=Download";
        }
        return formdata;
    }

    /** Ends up in PluginException(LinkStatus.ERROR_PLUGIN_DEFECT). */
    private void handleErrorsLastResortWebsite(final DownloadLink link, final Account account) throws PluginException {
        if (account != null && !this.isLoggedinWebsite(this.br)) {
            throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
        } else if (this.isaccessControlLimited(link)) {
            // throw new PluginException(LinkStatus.ERROR_FATAL, "This link is private. You're not authorized to download it!");
            /*
             * 2021-02-10: Not sure - seems like thi could be multiple reasons: registered only, premium only, IP/country only or private
             * file --> Owner only. See https://1fichier.com/console/acl.pl
             */
            throw new PluginException(LinkStatus.ERROR_FATAL, "Access to this file has been restricted");
        } else {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
    }

    private void prepareBrowserWebsite(final Browser br) {
        if (br == null) {
            return;
        }
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/48.0.2564.103 Safari/537.36");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us,en;q=0.5");
        br.getHeaders().put("Pragma", null);
        br.getHeaders().put("Cache-Control", null);
        br.setCustomCharset("utf-8");
        /* we want ENGLISH! */
        br.setCookie(this.getHost(), "LG", "en");
        br.setAllowedResponseCodes(new int[] { 403, 503 });
    }

    public static Browser prepareBrowserAPI(final Browser br, final Account account) throws Exception {
        if (br == null) {
            return null;
        }
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Content-Type", "application/json");
        br.setAllowedResponseCodes(new int[] { 401, 403, 503 });
        setPremiumAPIHeaders(br, account);
        return br;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        if (PluginJsonConfig.get(getLazyP(), OneFichierConfigInterface.class).isUsePremiumAPIEnabled()) {
            return new OnefichierAccountFactory(callback);
        } else {
            return new DefaultEditAccountPanel(callback, !getAccountwithoutUsername());
        }
    }

    public static class OnefichierAccountFactory extends MigPanel implements AccountBuilderInterface {
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your API Key";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else {
                final String pw = new String(this.pass.getPassword()).trim();
                if (EMPTYPW.equals(pw)) {
                    return null;
                } else {
                    return pw;
                }
            }
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
        private static String          EMPTYPW = " ";
        private final JLabel           idLabel;

        public OnefichierAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key (premium users only)"));
            add(new JLink("https://1fichier.com/console/params.pl"));
            this.add(this.idLabel = new JLabel("Enter your API Key:"));
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
            final String password = getPassword();
            if (!isApiKey(password)) {
                idLabel.setForeground(Color.RED);
                return false;
            }
            idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    private static boolean isApiKey(final String str) {
        return str != null && str.matches("[A-Za-z0-9\\-_=]{32}");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}