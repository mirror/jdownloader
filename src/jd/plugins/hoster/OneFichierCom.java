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
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.http.requests.GetRequest;
import jd.http.requests.PostRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
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
    public static final String   PROPERTY_PASSWORD_PROTECTED       = "password_protected";
    /** URLs can be restricted for various reason: https://1fichier.com/console/acl.pl */
    public static final String   PROPERTY_ACL_ACCESS_CONTROL_LIMIT = "acl_access_control_limit";
    /** 2019-04-04: Documentation: https://1fichier.com/api.html */
    public static final String   API_BASE                          = "https://api.1fichier.com/v1";
    /*
     * Max total connections for premium = 30 (RE: admin, updated 07.03.2019) --> See also their FAQ: https://1fichier.com/hlp.html#dllent
     */
    private static final boolean resume_account_premium            = true;
    private static final int     maxchunks_account_premium         = -3;
    private static final int     maxdownloads_account_premium      = 10;
    /* 2015-07-10: According to admin, resume is free mode is not possible anymore. On attempt this will lead to 404 server error! */
    private static final int     maxchunks_free                    = 1;
    private static final boolean resume_free                       = true;
    private static final int     maxdownloads_free                 = 1;
    /*
     * Settings for hotlinks - basically such links are created by premium users so free users can download them without limits (same limits
     * as premium users).
     */
    private static final boolean resume_free_hotlink               = true;
    private static final int     maxchunks_free_hotlink            = -3;

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
        /** 2021-02-10: 1 request per second is also fine according to admin */
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 2000);
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
            final String current_domain = Browser.getHost(link.getPluginPatternMatcher());
            link.setPluginPatternMatcher("https://" + current_domain + "/?" + fid);
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
                        dllink.setProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT, false);
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

    /** 2021-01-29: Not required at this moment. Review this before using it! */
    // /** Checks single URLs via API, TODO: Add crawler compatibility once crawler is done */
    // public AvailableStatus requestFileInformationAPI(final Browser br, final DownloadLink link, final Account account, final boolean
    // isDownload) throws IOException, PluginException {
    // prepareBrowserAPI(br, account);
    // performAPIRequest(API_BASE + "/file/info.cgi", "{\"url\":\"" + link.getPluginPatternMatcher() + "\"}");
    // if (br.getHttpConnection().getResponseCode() == 404) {
    // /* E.g. message": "Resource not found #469" */
    // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // } else if (br.getHttpConnection().getResponseCode() == 403) {
    // /* TODO: Do not jump into this only based on answer 403! Check for exact errormessage! */
    // /* 2020-01-30: e.g. {"status":"KO","message":"Resource not allowed #631"} */
    // /*
    // * Password-protected (no information given at all but we know that file is online). Example reasons: file is not allowed to be
    // * downloaded in current country, by current user, file is private
    // */
    // pwProtected = true;
    // link.setProperty("privatelink", true);
    // // link.setName(this.getFID(link));
    // if (isDownload) {
    // throwErrorPrivateLink();
    // }
    // /* Else all is fine - URL is online but we won't be able to download it. */
    // return AvailableStatus.TRUE;
    // }
    // /* 2019-04-05: This type of checksum is not supported by JDonloader so far */
    // // final String checksum = PluginJSonUtils.getJson(br, "checksum");
    // // if (!StringUtils.isEmpty(checksum)) {
    // // link.setSha256Hash(checksum);
    // // }
    // final String description = PluginJSonUtils.getJson(br, "description");
    // String filename = PluginJSonUtils.getJson(br, "filename");
    // if (StringUtils.isEmpty(filename)) {
    // filename = this.getFID(link);
    // }
    // String filesize = PluginJSonUtils.getJson(br, "size");
    // link.setName(filename);
    // if (filesize != null && filesize.matches("\\d+")) {
    // link.setDownloadSize(SizeFormatter.getSize(filesize));
    // }
    // if (!StringUtils.isEmpty(description) && StringUtils.isEmpty(link.getComment())) {
    // link.setComment(description);
    // }
    // /* 2020-01-30: We cannot work with this checksum */
    // // final String checksum = PluginJSonUtils.getJson(br, "checksum");
    // return AvailableStatus.TRUE;
    // }
    /* Old linkcheck removed AFTER revision 29396 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        correctDownloadLink(link);
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
        String dllink = link.getStringProperty(PROPERTY_HOTLINK, null);
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
        dllink = link.getStringProperty(PROPERTY_FREELINK, null);
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
                // link has expired... but it could be for any reason! dont care!
                // clear saved final link
                link.setProperty(PROPERTY_FREELINK, Property.NULL);
                br = new Browser();
                prepareBrowserWebsite(br);
            }
        }
        /* this covers virgin downloads which end up been hot link-able... */
        dllink = link.getPluginPatternMatcher();
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_free_hotlink, maxchunks_free_hotlink);
        if (this.looksLikeDownloadableContent(dl.getConnection())) {
            /* resume download */
            link.setProperty(PROPERTY_HOTLINK, dllink);
            dl.startDownload();
            return;
        }
        /* not hotlinkable.. standard free link... */
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
                br.getPage(link.getPluginPatternMatcher());
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
                        dllink = br3.getRegex("<a href=\"([^<>\"]*?)\"[^<>]*?>Click here to download").getMatch(0);
                    }
                    if (dllink == null) {
                        dllink = br3.getRegex("window\\.location\\s*=\\s*('|\")(https?://[a-zA-Z0-9_\\-]+\\.(1fichier|desfichiers)\\.com/[a-zA-Z0-9]+/.*?)\\1").getMatch(1);
                    }
                    if (dllink == null) {
                        String wait = br3.getRegex(" var count = (\\d+);").getMatch(0);
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
        if (ibr.containsHTML(">\\s*IP Locked|>\\s*Will be unlocked within 1h\\.")) {
            // jdlog://2958376935451/ https://board.jdownloader.org/showthread.php?t=67204&page=2
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "IP will be locked 1h", 60 * 60 * 1000l);
        } else if (ibr.containsHTML(">\\s*File not found")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (ibr.containsHTML("Warning ! Without subscription, you can only download one file at|<span style=\"color:red\">Warning\\s*!\\s*</span>\\s*<br/>Without subscription, you can only download one file at a time\\.\\.\\.")) {
            // jdlog://3278035891641 jdlog://7543779150841
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many downloads - wait before starting new downloads", 3 * 60 * 1000l);
        } else if (ibr.containsHTML(">\\s*Software error:<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Software error'", 10 * 60 * 1000l);
        } else if (ibr.containsHTML(">\\s*Connexion à la base de données impossible<|>Can\\'t connect DB")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
        } else if (ibr.containsHTML(">\\s*Votre adresse IP ouvre trop de connexions vers le serveur")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads", 3 * 60 * 1000l);
        } else if (ibr.containsHTML("not possible to free unregistered users")) {
            throw new AccountRequiredException();
        } else if (ibr.containsHTML("Your account will be unlock")) {
            if (account != null) {
                throw new AccountUnavailableException("Locked for security reasons", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP blocked for security reasons", 60 * 60 * 1000l);
            }
        } else if (ibr.containsHTML(">\\s*Access to this file is protected|>\\s*This file is protected")) {
            /* Access restricted by IP / only registered users / only premium users / only owner */
            if (ibr.containsHTML(">\\s*The owner of this file has reserved access to the subscribers of our services")) {
                throw new AccountRequiredException();
            } else {
                errorAccessControlLimit(link);
            }
        } else if (ibr.getURL().contains("/?c=DB")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
        } else if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 15 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (ibr.getHttpConnection().getResponseCode() == 503 && ibr.containsHTML(">\\s*Our services are in maintenance\\. Please come back after")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Hoster is in maintenance mode!", 20 * 60 * 1000l);
        } else {
            ipBlockedErrorHandling(ibr);
        }
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

    private static void ipBlockedErrorHandling(final Browser br) throws PluginException {
        String waittime = br.getRegex("you must wait (at least|up to) (\\d+) minutes between each downloads").getMatch(1);
        if (waittime == null) {
            waittime = br.getRegex(">You must wait (\\d+) minutes").getMatch(0);
        }
        if (waittime == null) {
            waittime = br.getRegex(">\\s*Vous devez attendre encore (\\d+) minutes").getMatch(0);
        }
        boolean isBlocked = waittime != null;
        isBlocked |= br.containsHTML("/>Téléchargements en cours");
        isBlocked |= br.containsHTML("En téléchargement standard, vous ne pouvez télécharger qu\\'un seul fichier");
        isBlocked |= br.containsHTML(">veuillez patienter avant de télécharger un autre fichier");
        isBlocked |= br.containsHTML(">You already downloading (some|a) file");
        isBlocked |= br.containsHTML(">You can download only one file at a time");
        isBlocked |= br.containsHTML(">Please wait a few seconds before downloading new ones");
        isBlocked |= br.containsHTML(">You must wait for another download");
        isBlocked |= br.containsHTML("Without premium status, you can download only one file at a time");
        isBlocked |= br.containsHTML("Without Premium, you can only download one file at a time");
        isBlocked |= br.containsHTML("Without Premium, you must wait between downloads");
        // <div style="text-align:center;margin:auto;color:red">Warning ! Without premium status, you must wait between each
        // downloads<br/>Your last download finished 05 minutes ago</div>
        isBlocked |= br.containsHTML("you must wait between each downloads");
        // <div style="text-align:center;margin:auto;color:red">Warning ! Without premium status, you must wait 15 minutes between each
        // downloads<br/>You must wait 15 minutes to download again or subscribe to a premium offer</div>
        isBlocked |= br.containsHTML("you must wait \\d+ minutes between each downloads<");
        if (isBlocked) {
            final boolean preferReconnect = PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferReconnectEnabled();
            if (waittime != null && preferReconnect) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            } else if (waittime != null && Integer.parseInt(waittime) >= 10) {
                /* High waittime --> Reconnect */
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Integer.parseInt(waittime) * 60 * 1001l);
            } else if (preferReconnect) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 5 * 60 * 1000l);
            } else if (waittime != null) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between download, Reconnect is disabled in plugin settings", Integer.parseInt(waittime) * 60 * 1001l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait between download, Reconnect is disabled in plugin settings", 5 * 60 * 1001);
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
        if (account.getUser() == null || !account.getUser().matches(".+@.+")) {
            ai.setStatus(":\r\nYou need to use Email as username!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You need to use Email as username!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.setAllowedResponseCodes(new int[] { 403, 503 });
        loginWebsite(account, true);
        /* And yet another workaround for broken API case ... */
        br.getPage("https://" + this.getHost() + "/en/console/index.pl");
        final boolean isPremium = br.containsHTML(">\\s*Premium\\s*(offer)\\s*Account\\s*<");
        final boolean isAccess = br.containsHTML(">\\s*Access\\s*(offer)\\s*Account\\s*<");
        // final boolean isFree = br.containsHTML(">\\s*Free\\s*(offer)\\s*Account\\s*<");
        if (isPremium || isAccess) {
            final GetRequest get = new GetRequest("https://" + this.getHost() + "/en/console/abo.pl");
            get.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.setFollowRedirects(true);
            br.getPage(get);
            final String validUntil = br.getRegex("subscription is valid until\\s*<[^<]*>(\\d+-\\d+-\\d+)").getMatch(0);
            if (validUntil != null) {
                final long validUntilTimestamp = TimeFormatter.getMilliSeconds(validUntil, "yyyy'-'MM'-'dd", Locale.FRANCE);
                if (validUntilTimestamp > 0) {
                    ai.setValidUntil(validUntilTimestamp + (24 * 60 * 60 * 1000l), this.br);
                }
            }
            /** TODO: Check for extra traffic */
            // final String traffic=br.getRegex("Your account have ([^<>\"]*?) of CDN credits").getMatch(0);
            if (isPremium) {
                ai.setStatus("Premium Account");
            } else {
                ai.setStatus("Access Account");
            }
            ai.setUnlimitedTraffic();
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
            /* 2019-04-04: Credits are only relevent for free accounts according to website: https://1fichier.com/console/params.pl */
            final String credits = br.getRegex(">\\s*Your account have ([^<>\"]*?) of (?:Hotlinks|direct download) credits").getMatch(0);
            final boolean useOwnCredits = StringUtils.equalsIgnoreCase("checked", br.getRegex("<input\\s*type=\"checkbox\"\\s*checked=\"(.*?)\"\\s*name=\"own_credit\"").getMatch(0));
            if (credits != null && useOwnCredits) {
                ai.setStatus("Free Account (Credits available[using credits])");
                ai.setTrafficLeft(SizeFormatter.getSize(credits));
            } else {
                if (credits != null) {
                    ai.setStatus("Free Account (Credits available[NOT using credits])");
                } else {
                    ai.setStatus("Free Account");
                }
                ai.setUnlimitedTraffic();
            }
        }
        return ai;
    }

    /**
     * 2019-04-04: This API can only be used by premium users! It might still work when a premium account expires and the key stays valid
     * but we don't know this yet!
     */
    public AccountInfo fetchAccountInfoAPI(final Account account) throws Exception {
        /* 2021-02-18: Remove linebreaks from the end RE forum: https://board.jdownloader.org/showthread.php?t=83954 */
        account.setPass(StringUtils.trim(account.getPass()));
        if (!isApiKey(account.getPass())) {
            invalidApiKey(account);
        }
        prepareBrowserAPI(br, account);
        /*
         * This request can only be used every ~5 minutes - using it more frequently will e.g. cause response:
         * {"status":"KO","message":"Flood detected: IP Locked #38"} [DOWNLOADS VIA API WILL STILL WORK!!]
         */
        performAPIRequest(API_BASE + "/user/info.cgi", "");
        AccountInfo ai = new AccountInfo();
        final String apierror = this.getAPIErrormessage(br);
        final boolean apiTempBlocked = !StringUtils.isEmpty(apierror) && apierror.matches("Flood detected: (User|IP) Locked.*?");
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
        final double available_credits_in_gigabyte = available_credits_in_gigabyteO != null && available_credits_in_gigabyteO instanceof Double ? ((Number) available_credits_in_gigabyteO).doubleValue() : 0;
        final long useOwnCredits = JavaScriptEngineFactory.toLong(entries.get("use_cdn"), 0);
        long validuntil = 0;
        if (!StringUtils.isEmpty(subscription_end)) {
            validuntil = TimeFormatter.getMilliSeconds(subscription_end, "yyyy-MM-dd HH:mm:ss", Locale.FRANCE);
        }
        String accountStatus;
        if (validuntil > System.currentTimeMillis()) {
            /* Premium */
            account.setType(AccountType.PREMIUM);
            accountStatus = "Premium account";
            account.setMaxSimultanDownloads(maxdownloads_account_premium);
            account.setConcurrentUsePossible(true);
            ai.setUnlimitedTraffic();
            ai.setValidUntil(validuntil, this.br);
        } else {
            /* Free --> 2019-07-18: API Keys are only available for premium users so this should never happen! */
            account.setType(AccountType.FREE);
            accountStatus = "Free account";
            account.setMaxSimultanDownloads(maxdownloads_free);
            account.setConcurrentUsePossible(false);
            /* 2019-04-04: Credits are only relevent for free accounts according to website: https://1fichier.com/console/params.pl */
            String creditsStatus = "";
            if (available_credits_in_gigabyte > 0) {
                creditsStatus = "  (" + available_credits_in_gigabyte + " Credits available";
                if (useOwnCredits == 1) {
                    creditsStatus += "[Using credits]";
                    ai.setTrafficLeft((long) available_credits_in_gigabyte);
                } else {
                    creditsStatus += "[NOT using credits]";
                    creditsStatus += ")";
                    ai.setUnlimitedTraffic();
                }
                accountStatus += creditsStatus;
            } else {
                ai.setUnlimitedTraffic();
            }
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
        final Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
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
                invalidApiKey(account);
            } else if (message.matches("(?i)No such user #\\d+")) {
                invalidApiKey(account);
            } else if (message.matches("(?i)Owner locked #\\d+")) {
                /* 2021-01-29 */
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account banned: " + message, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        return errorMsg != null && errorMsg.matches("(?i).*(Invalid password\\.|Password not provided\\.).*Resource not allowed #\\d+");
    }

    private void invalidApiKey(final Account account) throws PluginException {
        if (account != null) {
            /* Assume APIKey is invalid or simply not valid anymore (e.g. user disabled or changed APIKey) */
            throw new AccountInvalidException("Invalid API Key - you can find your API Key here: 1fichier.com/console/params.pl\r\nPlease keep in mind that API Keys are only available for premium customers.\r\nIf you do not own a premium account, disable the API Key setting in JD plugin settings so that you can login via username & password!\r\nKeep in mind that 2-factor-authentification login via JD and username/password is not supported!\r\nIf you want to login into your FREE 1fichier account in JD via username & password you will first have to disable 2-factor-authentication in your 1fichier account!");
        } else {
            throw new AccountRequiredException();
        }
    }

    private String getAPIErrormessage(final Browser br) {
        return PluginJSonUtils.getJson(br, "message");
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
        return isLoginCookieExists(br) && br.containsHTML("/logout\\.pl");
    }

    private boolean isLoginCookieExists(final Browser br) {
        return br.getCookie(this.getHost(), "SID", Cookies.NOTDELETEDPATTERN) != null;
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
                br.postPage("https://" + this.getHost() + "/login.pl", "lt=on&valider=Send&mail=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                if (!isLoggedinWebsite(this.br)) {
                    if (br.containsHTML("following many identification errors") && br.containsHTML("Your account will be unlock")) {
                        throw new AccountUnavailableException("Your account will be unlock within 1 hour", 60 * 60 * 1000l);
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
        return "http://www.1fichier.com/en/cgu.html";
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
            if (knownDownloadSize > 0 && knownDownloadSize <= 50 * 1024 * 1024) {
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
        /**
         * 2021-02-11: Don't do availablecheck in premium mode to reduce requests. </br> According to their admin, using the public
         * availablecheck call just before downloading via API can be troublesome
         */
        if (AccountType.FREE.equals(account.getType())) {
            /**
             * Website mode is required for free account downloads
             */
            loginWebsite(account, false);
            doFree(account, link);
            return;
        } else {
            String dllink = checkDirectLink(link, PROPERTY_PREMLINK);
            if (dllink == null) {
                if (canUseAPI(account)) {
                    dllink = getDllinkPremiumAPI(link, account);
                } else {
                    dllink = getDllinkPremiumWebsite(link, account);
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final boolean preferSSL = PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled();
            if (preferSSL && dllink.startsWith("http://")) {
                dllink = dllink.replace("http://", "https://");
            }
            br.setFollowRedirects(true);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_account_premium, maxchunks_account_premium);
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
            dl.startDownload();
        }
    }

    private String getDllinkPremiumAPI(final DownloadLink link, final Account account) throws IOException, PluginException {
        /**
         * 2019-04-05: At the moment there are no benefits for us when using this. </br> 2021-01-29: Removed this because if login is
         * blocked because of "flood control" this won't work either!
         */
        // requestFileInformationAPI(this.br, link, account, true);
        // this.checkErrorsAPI(account);
        setPremiumAPIHeaders(br, account);
        /* Do NOT trust pwProtected as this is obtained via website or old mass-linkcheck API!! */
        String dllink = null;
        synchronized (lastSessionPassword) {
            String passCode = link.getDownloadPassword();
            /* Check if we already know that this file is password protected ... */
            /** Try passwords in this order: 1. DownloadLink stored password, 2. Last used password, 3. Ask user */
            boolean usedLastPassword = false;
            if (this.isPasswordProtected(link)) {
                if (passCode == null) {
                    if (lastSessionPassword.get() != null) {
                        usedLastPassword = true;
                        passCode = lastSessionPassword.get();
                    } else {
                        passCode = Plugin.getUserInput("Password?", link);
                    }
                }
            }
            /**
             * TODO: Check if/when we need additional json POST parameters: inline, restrict_ip, no_ssl, folder_id, sharing_user
             */
            /** Description of optional parameters: cdn=0/1 - use download-credits, */
            performAPIRequest(API_BASE + "/download/get_token.cgi", String.format("{\"url\":\"%s\",\"pass\":\"%s\"}", link.getPluginPatternMatcher(), passCode));
            final String api_error = this.getAPIErrormessage(br);
            /**
             * 2021-02-10: This will ask for a password for all kinds of access limited files. They will have to update their API to fix
             * this. Example self uploaded file, only downloadable from afghanistan: https://1fichier.com/?uczre58xge6pif2d9n6g
             */
            if (!StringUtils.isEmpty(api_error) && api_error.matches(".*(Invalid password\\.|Password not provided\\.).*Resource not allowed #\\d+")) {
                if (usedLastPassword) {
                    lastSessionPassword.set(null);
                } else {
                    link.setDownloadPassword(null);
                }
                if (this.isPasswordProtected(link)) {
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password wrong!");
                } else {
                    this.setPasswordProtected(link, true);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Password required!");
                }
            } else if (passCode != null) {
                lastSessionPassword.set(passCode);
                link.setDownloadPassword(passCode);
            } else {
                /* File is not password protected */
                link.setProperty(PROPERTY_PASSWORD_PROTECTED, false);
            }
        }
        /* 2019-04-04: Downloadlink is officially only valid for 5 minutes */
        handleErrorsAPI(account);
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
        try {
            ipBlockedErrorHandling(br);
        } catch (final PluginException e) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 45 * 1000l, e);
        }
        if (dllink == null) {
            /* The link is always SSL - based on user setting it will redirect to either https or http. */
            String postData = "did=0&";
            postData += getSSLFormValue();
            br.postPage(link.getPluginPatternMatcher(), postData);
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                if (br.containsHTML("\">Warning \\! Without premium status, you can download only")) {
                    logger.info("Seems like this is no premium account or it's vot valid anymore -> Disabling it");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else if (br.containsHTML(">\\s*You can use your account only for downloading from") || br.containsHTML(">\\s*Our services are not compatible with massively shared internet access") || br.containsHTML(">\\s*Be carrefull? to not use simultaneously your IPv4 and IPv6 IP")) {
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

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
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

    /** Required to authenticate via API. Wrapper for setPremiumAPIHeaders(String). */
    public static void setPremiumAPIHeaders(final Browser br, final Account account) {
        setPremiumAPIHeaders(br, getAPIKey(account));
    }

    public static String getAPIKey(final Account account) {
        return account.getPass();
    }

    public static boolean canUseAPI(final Account account) {
        /**
         * true = use premium API, false = use combination of website + OLD basic auth API - ONLY RELEVANT FOR PREMIUM USERS; IF ENABLED,
         * USER HAS TO ENTER API_KEY INSTEAD OF USERNAME:PASSWORD (or APIKEY:APIKEY)!!
         */
        final boolean useAPI_setting = PluginJsonConfig.get(OneFichierConfigInterface.class).isUsePremiumAPIEnabled();
        return account != null && useAPI_setting && (account.getType() == AccountType.PREMIUM || account.getType() == AccountType.UNKNOWN);
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
                    passCode = Plugin.getUserInput("Password?", link);
                }
            }
            pwform.put("pass", Encoding.urlEncode(passCode));
            /*
             * Set pw protected flag so in case this downloadlink is ever tried to be downloaded via API, we already know that it is
             * password protected!
             */
            link.setProperty(PROPERTY_PASSWORD_PROTECTED, true);
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

    private boolean isPasswordProtected(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_PASSWORD_PROTECTED, false);
    }

    private void setPasswordProtected(final DownloadLink link, final boolean passwordProtected) {
        link.setProperty(PROPERTY_PASSWORD_PROTECTED, passwordProtected);
    }

    private boolean isaccessControlLimited(final DownloadLink link) {
        return link.getBooleanProperty(PROPERTY_ACL_ACCESS_CONTROL_LIMIT, false);
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
        // we want ENGLISH!
        br.setCookie(this.getHost(), "LG", "en");
        br.setAllowedResponseCodes(new int[] { 403, 503 });
    }

    public static Browser prepareBrowserAPI(final Browser br, final Account account) {
        if (br == null) {
            return null;
        }
        br.setConnectTimeout(3 * 60 * 1000);
        br.setReadTimeout(3 * 60 * 1000);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("Content-Type", "application/json");
        br.setAllowedResponseCodes(new int[] { 401, 403, 503 });
        if (account != null) {
            setPremiumAPIHeaders(br, account);
        }
        return br;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        if (PluginJsonConfig.get(OneFichierConfigInterface.class).isUsePremiumAPIEnabled()) {
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