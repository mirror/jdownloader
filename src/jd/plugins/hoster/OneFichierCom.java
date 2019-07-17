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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
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
import jd.plugins.AccountRequiredException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class OneFichierCom extends PluginForHost {
    private final String         HTML_PASSWORDPROTECTED       = "(This file is Password Protected|Ce fichier est protégé par mot de passe|access with a password)";
    private final String         PROPERTY_FREELINK            = "freeLink";
    private final String         PROPERTY_HOTLINK             = "hotlink";
    private final String         PROPERTY_PREMLINK            = "premLink";
    private static final String  MAINPAGE                     = "https://1fichier.com/";
    /** 2019-04-04: Documentation: https://1fichier.com/api.html */
    public static final String   API_BASE                     = "https://api.1fichier.com/v1";
    private boolean              pwProtected                  = false;
    private DownloadLink         currDownloadLink             = null;
    /* Max total connections for premium = 30 (RE: admin, updated 07.03.2019) */
    private static final boolean resume_account_premium       = true;
    private static final int     maxchunks_account_premium    = -3;
    private static final int     maxdownloads_account_premium = 10;
    /* 2015-07-10: According to admin, resume is free mode is not possible anymore. On attempt this will lead to 404 server error! */
    private static final int     maxchunks_free               = 1;
    private static final boolean resume_free                  = true;
    private static final int     maxdownloads_free            = 1;
    /*
     * Settings for hotlinks - basically such links are created by premium users so free users can download them without limits (same limits
     * as premium users).
     */
    private static final boolean resume_free_hotlink          = true;
    private static final int     maxchunks_free_hotlink       = -4;
    // public static String[] domains = new String[] { "1fichier.com", "alterupload.com", "cjoint.net", "desfichiers.net", "dfichiers.com",
    // "megadl.fr", "mesfichiers.org", "piecejointe.net", "pjointe.com", "tenvoi.com", "dl4free.com" };

    @Override
    public String[] siteSupportedNames() {
        /* 1st domain = current domain! */
        final String[] supportedDomains = buildAnnotationNames(getPluginDomains());
        final List<String> ret = Arrays.asList(supportedDomains);
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/\\?[a-z0-9]{5,20}|https?://[a-z0-9]{5,20}\\." + buildHostsPatternPart(domains));
        }
        return ret.toArray(new String[0]);
    }

    public OneFichierCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.1fichier.com/en/register.pl");
    }

    @Override
    public void init() {
        Browser.setRequestIntervalLimitGlobal(this.getHost(), 2000);
    }

    private String correctProtocol(final String input) {
        return input.replaceFirst("http://", "https://");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /**
         * 2019-04-24: Do NOT change domains here! Uploaders can decide which domain is the only valid domain for their files e.g.
         * "alterupload.com". Using their main domain (1fichier.com) will result in OFFLINE URLs!
         */
        final String linkid = getLinkID(link);
        final String current_domain = Browser.getHost(link.getPluginPatternMatcher());
        if (linkid != null) {
            /* Always use main domain & new linktype */
            link.setPluginPatternMatcher(String.format("https://%s/?%s", current_domain, linkid));
        }
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getLinkidFromURL(link.getPluginPatternMatcher());
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    public static String getLinkidFromURL(final String url) {
        if (url == null) {
            return null;
        }
        final String linkid;
        if (url.matches("https?://[a-z0-9]{5,20}\\.")) {
            /* Old linktype */
            linkid = new Regex(url, "https?://([a-z0-9]{5,20})\\..+").getMatch(0);
        } else {
            /* New linktype */
            linkid = new Regex(url, "([a-z0-9]+)$").getMatch(0);
        }
        return linkid;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currDownloadLink = dl;
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
                if (this.isAbort()) {
                    logger.info("User stopped downloads --> Stepping out of loop");
                    throw new PluginException(LinkStatus.ERROR_RETRY, "User aborted download");
                }
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
                for (final DownloadLink dl : links) {
                    sb.append("links[]=");
                    sb.append(Encoding.urlEncode(dl.getPluginPatternMatcher()));
                    sb.append("&");
                }
                // remove last &
                sb.deleteCharAt(sb.length() - 1);
                br.postPageRaw(correctProtocol("http://1fichier.com/check_links.pl"), sb.toString());
                checkConnection(br);
                for (final DownloadLink dllink : links) {
                    // final String addedLink = dllink.getDownloadURL();
                    final String addedlink_id = this.getFID(dllink);
                    if (addedlink_id == null) {
                        // invalid uid
                        dllink.setAvailable(false);
                    } else if (br.containsHTML(addedlink_id + "[^;]*;;;(NOT FOUND|BAD LINK)")) {
                        dllink.setAvailable(false);
                        dllink.setName(addedlink_id);
                    } else if (br.containsHTML(addedlink_id + "[^;]*;;;PRIVATE")) {
                        dllink.setProperty("privatelink", true);
                        dllink.setAvailable(true);
                        dllink.setName(addedlink_id);
                    } else {
                        final String[] linkInfo = br.getRegex(addedlink_id + "[^;]*;([^;]+);(\\d+)").getRow(0);
                        if (linkInfo.length != 2) {
                            logger.warning("Linkchecker for 1fichier.com is broken!");
                            return false;
                        }
                        dllink.setProperty("privatelink", false);
                        dllink.setAvailable(true);
                        /* Trust API information. */
                        dllink.setFinalFileName(Encoding.htmlDecode(linkInfo[0]));
                        dllink.setDownloadSize(SizeFormatter.getSize(linkInfo[1]));
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

    /** Checks single URLs via API, TODO: Add crawler compatibility once crawler is done */
    public AvailableStatus requestFileInformationAPI(final Browser br, final DownloadLink link, final Account account) throws IOException, PluginException {
        prepareBrowserAPI(br, null);
        performAPIRequest(API_BASE + "/file/info.cgi", "{\"url\":\"" + link.getPluginPatternMatcher() + "\"}");
        if (br.getHttpConnection().getResponseCode() == 404) {
            /* E.g. message": "Resource not found #469" */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 403) {
            /* Password-protected (no information given at all but we know that file is online) */
            pwProtected = true;
            link.setProperty("privatelink", true);
            // link.setName(this.getLinkID(link));
            return AvailableStatus.TRUE;
        }
        /* 2019-04-05: This type of checksum is not supported by JDonloader so far */
        // final String checksum = PluginJSonUtils.getJson(br, "checksum");
        // if (!StringUtils.isEmpty(checksum)) {
        // link.setSha256Hash(checksum);
        // }
        final String description = PluginJSonUtils.getJson(br, "description");
        String filename = PluginJSonUtils.getJson(br, "filename");
        if (StringUtils.isEmpty(filename)) {
            filename = this.getLinkID(link);
        }
        String filesize = PluginJSonUtils.getJson(br, "size");
        link.setName(filename);
        if (filesize != null && filesize.matches("\\d+")) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (!StringUtils.isEmpty(description) && link.getComment().isEmpty()) {
            link.setComment(description);
        }
        return AvailableStatus.TRUE;
    }

    /* Old linkcheck removed AFTER revision 29396 */
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        /* Offline links should also get nice filenames. */
        correctDownloadLink(link);
        checkLinks(new DownloadLink[] { link });
        prepareBrowserWebsite(br);
        if (!link.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (link.isAvailabilityStatusChecked() && !link.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanDownload(DownloadLink link, Account account) {
        if (account == null && (link != null && link.getProperty(PROPERTY_HOTLINK, null) != null)) {
            return Integer.MAX_VALUE;
        }
        return super.getMaxSimultanDownload(link, account);
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        this.setConstants(null, downloadLink);
        requestFileInformation(downloadLink);
        if (checkShowFreeDialog(getHost())) {
            showFreeDialog(getHost());
        }
        doFree(null, downloadLink);
    }

    private String regex_dllink_middle = "align:middle\">\\s+<a href=(\"|')(https?://[a-zA-Z0-9_\\-]+\\.(1fichier|desfichiers)\\.com/[a-zA-Z0-9]+.*?)\\1";

    public void doFree(final Account account, final DownloadLink downloadLink) throws Exception, PluginException {
        checkDownloadable(account);
        // to prevent wasteful requests.
        int i = 0;
        /* The following code will cover saved hotlinks */
        String dllink = downloadLink.getStringProperty(PROPERTY_HOTLINK, null);
        if (dllink != null) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume_free_hotlink, maxchunks_free_hotlink);
            if (dl.getConnection().getContentType().contains("html")) {
                dl.getConnection().disconnect();
                // link has expired... but it could be for any reason! dont care!
                // clear saved final link
                downloadLink.setProperty(PROPERTY_HOTLINK, Property.NULL);
                br = new Browser();
                prepareBrowserWebsite(br);
            } else {
                /* resume download */
                downloadLink.setProperty(PROPERTY_HOTLINK, dllink);
                dl.startDownload();
                return;
            }
        }
        // retry/resume of cached free link!
        dllink = downloadLink.getStringProperty(PROPERTY_FREELINK, null);
        if (dllink != null) {
            dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume_free, maxchunks_free);
            if (dl.getConnection().getContentType().contains("html")) {
                dl.getConnection().disconnect();
                // link has expired... but it could be for any reason! dont care!
                // clear saved final link
                downloadLink.setProperty(PROPERTY_FREELINK, Property.NULL);
                br = new Browser();
                prepareBrowserWebsite(br);
            } else {
                /* resume download */
                downloadLink.setProperty(PROPERTY_FREELINK, dllink);
                dl.startDownload();
                return;
            }
        }
        // this covers virgin downloads which end up been hot link-able...
        dllink = getDownloadlinkNEW(downloadLink);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume_free_hotlink, maxchunks_free_hotlink);
        if (!dl.getConnection().getContentType().contains("html")) {
            /* resume download */
            downloadLink.setProperty(PROPERTY_HOTLINK, dllink);
            dl.startDownload();
            return;
        }
        // not hotlinkable.. standard free link...
        // html yo!
        br.followConnection();
        checkConnection(br);
        dllink = null;
        br.setFollowRedirects(false);
        // use the English page, less support required
        boolean retried = false;
        while (true) {
            i++;
            // redirect log 2414663166931
            if (i > 1) {
                br.setFollowRedirects(true);
                // no need to do this link twice as it's been done above.
                br.getPage(this.getDownloadlinkNEW(downloadLink));
                br.setFollowRedirects(false);
            }
            errorHandling(downloadLink, account, br);
            if (pwProtected || br.containsHTML(HTML_PASSWORDPROTECTED)) {
                handlePasswordWebsite();
                dllink = br.getRedirectLocation();
                if (dllink == null) {
                    // Link; 8182111113541.log; 464810; jdlog://8182111113541
                    dllink = br.getRegex(regex_dllink_middle).getMatch(1);
                    if (dllink == null) {
                        logger.warning("Failed to find final downloadlink after password handling success");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                logger.info("Successfully went through the password handling");
                break;
            } else {
                // base > submit:Free Download > submit:Show the download link + t:35140198 == link
                final Browser br2 = br.cloneBrowser();
                // final Form a1 = br2.getForm(0);
                // if (a1 == null) {
                // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                // }
                // a1.remove(null);
                br2.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                sleep(2000, downloadLink);
                // br2.submitForm(a1);
                br2.postPageRaw(br.getURL(), "");
                errorHandling(downloadLink, account, br2);
                if (br2.containsHTML("not possible to unregistered users")) {
                    final Account aa = AccountController.getInstance().getValidAccount(this);
                    if (aa != null) {
                        try {
                            synchronized (aa) {
                                loginWebsite(aa, true);
                                ensureSiteLogin(aa);
                            }
                        } catch (final PluginException e) {
                            logger.log(e);
                        }
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                    }
                }
                dllink = br2.getRedirectLocation();
                if (dllink == null) {
                    dllink = br2.getRegex(regex_dllink_middle).getMatch(1);
                }
                if (dllink == null) {
                    final Form a2 = br2.getForm(0);
                    if (a2 == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    a2.remove("save");
                    final Browser br3 = br.cloneBrowser();
                    br3.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                    sleep(2000, downloadLink);
                    br3.submitForm(a2);
                    errorHandling(downloadLink, account, br3);
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
                            sleep(1000 * Long.parseLong(wait), downloadLink);
                            continue;
                        }
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
            }
            if (dllink != null) {
                break;
            }
        }
        br.setFollowRedirects(true);
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, resume_free, maxchunks_free);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            errorHandling(downloadLink, account, br);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(PROPERTY_FREELINK, dllink);
        dl.startDownload();
    }

    private void errorHandling(final DownloadLink downloadLink, final Account account, final Browser ibr) throws Exception {
        long responsecode = 200;
        if (ibr.getHttpConnection() != null) {
            responsecode = ibr.getHttpConnection().getResponseCode();
        }
        if (ibr.containsHTML(">IP Locked|>Will be unlocked within 1h.")) {
            // jdlog://2958376935451/ https://board.jdownloader.org/showthread.php?t=67204&page=2
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "IP will be locked 1h", 60 * 60 * 1000l);
        } else if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 15 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 30 * 60 * 1000l);
        } else if (ibr.containsHTML(">\\s*File not found !\\s*<br/>It has could be deleted by its owner\\.\\s*<")) {
            // api linkchecking can be out of sync (wrong)
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (ibr.containsHTML("Warning ! Without subscription, you can only download one file at|<span style=\"color:red\">Warning\\s*!\\s*</span>\\s*<br/>Without subscription, you can only download one file at a time\\.\\.\\.")) {
            // jdlog://3278035891641 jdlog://7543779150841
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many downloads - wait before starting new downloads", 3 * 60 * 1000l);
        } else if (ibr.containsHTML("<h1>Select files to send :</h1>")) {
            // for some reason they linkcheck correct, then show upload page. re: jdlog://3895673179241
            // https://svn.jdownloader.org/issues/65003
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster issue?", 60 * 60 * 1000l);
        } else if (ibr.containsHTML(">Software error:<")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Software error'", 10 * 60 * 1000l);
        } else if (ibr.containsHTML(">Connexion à la base de données impossible<|>Can\\'t connect DB")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
        } else if (ibr.containsHTML(">Votre adresse IP ouvre trop de connexions vers le serveur")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many connections - wait before starting new downloads", 3 * 60 * 1000l);
        } else if (ibr.containsHTML("not possible to free unregistered users")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (ibr.containsHTML("Your account will be unlock")) {
            if (account != null) {
                throw new AccountUnavailableException("Locked for security reasons", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP blocked for security reasons", 60 * 60 * 1000l);
            }
        }
        errorIpBlockedHandling(ibr);
    }

    private void errorIpBlockedHandling(final Browser br) throws PluginException {
        String waittime = br.getRegex("you must wait (at least|up to) (\\d+) minutes between each downloads").getMatch(1);
        if (waittime == null) {
            waittime = br.getRegex(">You must wait (\\d+) minutes").getMatch(0);
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
        setConstants(account, null);
        AccountInfo ai = new AccountInfo();
        if (canUseAPI(account)) {
            ai = fetchAccountInfoAPI(account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    public AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (account.getUser() == null || !account.getUser().matches(".+@.+")) {
            ai.setStatus(":\r\nYou need to use Email as username!");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "You need to use Email as username!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        br.setAllowedResponseCodes(new int[] { 403, 503 });
        br = new Browser();
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
                final long validUntilTimestamp = TimeFormatter.getMilliSeconds(validUntil, "yyyy'-'MM'-'dd", Locale.ENGLISH);
                if (validUntilTimestamp > 0) {
                    ai.setValidUntil(validUntilTimestamp + (24 * 60 * 60 * 1000l));
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
            account.setProperty("freeAPIdisabled", true);
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
        br = new Browser();
        prepareBrowserAPI(br, account);
        /*
         * This request can only be used every ~5 minutes - using it more frequently will e.g. cause response:
         * {"status":"KO","message":"Flood detected: IP Locked #38"} [DOWNLOADS VIA API WILL STILL WORK!!]
         */
        performAPIRequest(API_BASE + "/user/info.cgi", "");
        final AccountInfo ai = new AccountInfo();
        final String apierror = this.getAPIErrormessage();
        final boolean apiTempBlocked = !StringUtils.isEmpty(apierror) && apierror.matches("Flood detected: .+");
        if (apiTempBlocked && account.lastUpdateTime() > 0) {
            logger.info("Cannot get account details because of API limits but account has been checked before so we'll not throw an error");
            return account.getAccountInfo();
        } else if (apiTempBlocked) {
            /*
             * Account got added for the first time but API is blocked at the moment. We know the account must be premium but we cannot get
             * any information at the moment ...
             */
            logger.info("Cannot get account details because of API limits and account has never been checked before --> Adding account without info");
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account (can't display info at this moment)");
            account.setMaxSimultanDownloads(maxdownloads_account_premium);
            account.setConcurrentUsePossible(true);
            ai.setUnlimitedTraffic();
            return ai;
        }
        checkErrorsAPI(account);
        String mail = PluginJSonUtils.getJson(br, "email");
        if (!StringUtils.isEmpty(mail)) {
            /* don't store the complete username for security purposes. */
            final String shortuserName = "***" + mail.substring(3, mail.length());
            account.setUser(shortuserName);
        }
        final String subscription_end = PluginJSonUtils.getJson(br, "subscription_end");
        final String available_credits_in_gigabyte_str = PluginJSonUtils.getJson(br, "cdn");
        final double available_credits_in_gigabyte = available_credits_in_gigabyte_str != null ? Double.parseDouble(available_credits_in_gigabyte_str) : 0;
        final String useOwnCredits = PluginJSonUtils.getJson(br, "use_cdn");
        long validuntil = 0;
        if (!StringUtils.isEmpty(subscription_end)) {
            validuntil = TimeFormatter.getMilliSeconds(subscription_end, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        String accountStatus;
        if (validuntil > System.currentTimeMillis()) {
            /* Premium */
            account.setType(AccountType.PREMIUM);
            accountStatus = "Premium account";
            account.setMaxSimultanDownloads(maxdownloads_account_premium);
            account.setConcurrentUsePossible(true);
            ai.setUnlimitedTraffic();
            ai.setValidUntil(validuntil);
        } else {
            /* Free */
            account.setType(AccountType.FREE);
            accountStatus = "Free account";
            account.setMaxSimultanDownloads(maxdownloads_free);
            account.setConcurrentUsePossible(false);
            /* 2019-04-04: Credits are only relevent for free accounts according to website: https://1fichier.com/console/params.pl */
            String creditsStatus = "";
            if (available_credits_in_gigabyte > 0) {
                creditsStatus = "  (" + available_credits_in_gigabyte + " Credits available";
                if (!StringUtils.isEmpty(useOwnCredits) && useOwnCredits.equals("1")) {
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
    private void checkErrorsAPI(final Account account) throws PluginException {
        final String status = PluginJSonUtils.getJson(br, "status");
        final String message = getAPIErrormessage();
        if ("KO".equalsIgnoreCase(status)) {
            if (StringUtils.isEmpty(message)) {
                /* This should never happen! */
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown API error", 5 * 60 * 1000l);
            }
            final String account_invalid_text = "Invalid API Key - you can find your API Key here: 1fichier.com/console/params.pl";
            if (message.matches("Flood detected: IP Locked #\\d+")) {
                if (account != null) {
                    throw new AccountUnavailableException("API flood detection #38 has been triggered", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API flood detection #38 has been triggered", 5 * 60 * 1000l);
                }
            } else if (message.matches("Flood detected: User Locked #\\d+")) {
                /* 2019-04-04: Not sure what the difference to #38 is ... */
                if (account != null) {
                    throw new AccountUnavailableException("API flood detection #218 has been triggered", 5 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "API flood detection #218 has been triggered", 5 * 60 * 1000l);
                }
            } else if (message.matches("Not authenticated #\\d+")) {
                /* Login required but not logged in */
                if (account != null) {
                    /* Assume APIKey is invalid or simply not valid anymore (e.g. user disabled or changed APIKey) */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, account_invalid_text, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new AccountRequiredException();
                }
            } else if (message.matches("No such user #\\d+")) {
                /* Login required but not logged in */
                if (account != null) {
                    /* Assume APIKey is invalid or simply not valid anymore (e.g. user disabled or changed APIKey) */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, account_invalid_text, PluginException.VALUE_ID_PREMIUM_DISABLE);
                } else {
                    throw new AccountRequiredException();
                }
            } else {
                /* Unknown/unhandled error */
                /** TODO: Maybe replace PLUGIN_DEFECT with retry handling */
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    private String getAPIErrormessage() {
        return PluginJSonUtils.getJson(br, "message");
    }

    private void checkConnection(final Browser br) throws PluginException {
        if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 503 && br.containsHTML(">\\s*Our services are in maintenance\\. Please come back after")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Hoster is in maintenance mode!", 20 * 60 * 1000l);
        }
    }

    /** Checks whether we're logged in via API. */
    private boolean checkSID(Browser br) {
        final String sid = br.getCookie(MAINPAGE, "SID");
        return !StringUtils.isEmpty(sid) && !StringUtils.equalsIgnoreCase(sid, "deleted");
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* Load cookies */
                prepareBrowserWebsite(br);
                Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(MAINPAGE, cookies);
                    if (!force) {
                        setBasicAuthHeader(br, account);
                        return;
                    } else {
                        br.getPage("https://1fichier.com/console/index.pl");
                        if (!checkSID(br)) {
                            cookies = null;
                            br.clearCookies(MAINPAGE);
                        }
                    }
                }
                if (cookies == null) {
                    logger.info("Using site login because API is either wrong or no free credits...");
                    br.postPage("https://1fichier.com/login.pl", "lt=on&valider=Send&mail=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()));
                    if (!checkSID(br)) {
                        if (br.containsHTML("following many identification errors") && br.containsHTML("Your account will be unlock")) {
                            throw new AccountUnavailableException("Your account will be unlock within 1 hour", 60 * 60 * 1000l);
                        }
                        logger.info("Username/Password also invalid via site login!");
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(getHost()), "");
                setBasicAuthHeader(br, account);
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
    protected long getStartIntervall(final DownloadLink downloadLink, final Account account) {
        if (account == null || !AccountType.PREMIUM.equals(account.getType()) || downloadLink == null) {
            return super.getStartIntervall(downloadLink, account);
        } else {
            final long knownDownloadSize = downloadLink.getKnownDownloadSize();
            if (knownDownloadSize > 0 && knownDownloadSize <= 50 * 1024 * 1024) {
                final int wait = PluginJsonConfig.get(OneFichierConfigInterface.class).getSmallFilesWaitInterval();
                // avoid IP block because of too many downloads in short time
                return Math.max(0, wait * 1000);
            } else {
                return super.getStartIntervall(downloadLink, account);
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        setConstants(account, link);
        requestFileInformation(link);
        checkDownloadable(account);
        br = new Browser();
        if (AccountType.FREE.equals(account.getType()) && account.getBooleanProperty("freeAPIdisabled")) {
            /**
             * Used if the API fails and is wrong or user uses a free account.
             */
            synchronized (account) {
                loginWebsite(account, false);
                ensureSiteLogin(account);
            }
            doFree(account, link);
            return;
        }
        String dllink = getDllinkPremium(link, account);
        final boolean preferSSL = PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled();
        if (preferSSL && dllink.startsWith("http://")) {
            dllink = dllink.replace("http://", "https://");
        }
        for (int i = 0; i != 2; i++) {
            /** 2019-04-04: TODO: Try to remove this overcomplicated handling! */
            if (dl == null || i > 0) {
                try {
                    logger.info("Connecting to dllink: " + dllink);
                    dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, resume_account_premium, maxchunks_account_premium);
                } catch (final Exception e) {
                    logger.info("Download failed because: " + e.getMessage());
                    throw e;
                }
                if (dl.getConnection().getContentType().contains("html")) {
                    if ("http://www.1fichier.com/?c=DB".equalsIgnoreCase(br.getURL())) {
                        dl.getConnection().disconnect();
                        if (i + 1 == 2) {
                            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Internal database error", 5 * 60 * 1000l);
                        }
                        continue;
                    }
                    logger.warning("The final dllink seems not to be a file!");
                    br.followConnection();
                    errorHandling(link, account, br);
                    /** TODO: Check this */
                    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 30 * 60 * 1000l);
                }
            }
            link.setProperty(PROPERTY_PREMLINK, dllink);
            dl.startDownload();
            return;
        }
    }

    private String getDllinkPremium(final DownloadLink link, final Account account) throws Exception {
        String dllink = checkDirectLink(link, PROPERTY_PREMLINK);
        if (dllink == null) {
            if (canUseAPI(account)) {
                dllink = getDllinkPremiumAPI(link, account);
            } else {
                dllink = getDllinkPremiumWebsite(link, account);
            }
        }
        return dllink;
    }

    private String getDllinkPremiumAPI(final DownloadLink link, final Account account) throws IOException, PluginException {
        /* 2019-04-05: At the moment there are no benefits for us when using this. */
        // requestFileInformationAPI(this.br, link, account);
        setPremiumAPIHeaders(br, account);
        /* Do NOT trust pwProtected as this is obtained via website or old mass-linkcheck API!! */
        String dllink = null;
        String passCode = null;
        boolean passwordFailure = true;
        for (int i = 0; i <= 3; i++) {
            /**
             * TODO: Check if/when we need additional json POST parameters: inline, restrict_ip, no_ssl, folder_id, sharing_user
             */
            /** Description of optional parameters: cdn=0/1 - use download-credits, */
            performAPIRequest(API_BASE + "/download/get_token.cgi", String.format("{\"url\":\"%s\",\"pass\":\"%s\"}", link.getPluginPatternMatcher(), passCode));
            final String api_error = this.getAPIErrormessage();
            if (!StringUtils.isEmpty(api_error) && api_error.matches("Resource not allowed #\\d+")) {
                /** Try passwords in this order: 1. DownloadLink stored password, 2. Last used password, 3. Ask user */
                this.pwProtected = true;
                switch (i) {
                case 0:
                    /* Try stored password if available */
                    passCode = currDownloadLink.getDownloadPassword();
                    break;
                case 1:
                    passCode = lastSessionPassword.get();
                    break;
                case 2:
                    passCode = Plugin.getUserInput("Password?", currDownloadLink);
                    break;
                default:
                    break;
                }
            } else {
                passwordFailure = false;
                if (passCode != null) {
                    currDownloadLink.setDownloadPassword(passCode);
                }
                break;
            }
        }
        if (passwordFailure) {
            currDownloadLink.setDownloadPassword(null);
            throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
        }
        /* 2019-04-04: Downloadlink is officially only valid for 5 minutes */
        dllink = PluginJSonUtils.getJson(br, "url");
        if (StringUtils.isEmpty(dllink) || !dllink.startsWith("http")) {
            checkErrorsAPI(account);
        }
        return dllink;
    }

    private String getDllinkPremiumWebsite(final DownloadLink link, final Account account) throws Exception {
        String dllink = null;
        // for some silly reason we have reverted from api to webmethod, so we need cookies!. 20150201
        br = new Browser();
        synchronized (account) {
            loginWebsite(account, false);
            ensureSiteLogin(account);
        }
        br.setFollowRedirects(false);
        br.getPage(link.getPluginPatternMatcher());
        // error checking, offline links can happen here.
        errorHandling(link, account, br);
        dllink = br.getRedirectLocation();
        if (pwProtected || br.containsHTML(HTML_PASSWORDPROTECTED)) {
            handlePasswordWebsite();
            /*
             * The users' 'direct download' setting has no effect on the password handling so we should always get a redirect to the final
             * downloadlink after having entered the correct download password (for premium users).
             */
            dllink = br.getRedirectLocation();
            if (dllink == null) {
                dllink = br.getRegex(regex_dllink_middle).getMatch(1);
                if (dllink == null) {
                    logger.warning("After successful password handling: Final downloadlink 'dllink' is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (dllink.contains("login.pl")) { // jdlog://4209376935451/
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "login.pl?exp=1", 3 * 60 * 1000l);
            }
        }
        try {
            errorIpBlockedHandling(br);
        } catch (PluginException e) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 45 * 1000l);
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
                } else if (br.containsHTML("You can use your account only for downloading from 1 Internet access at a time") || br.containsHTML("You can use your Premium account for downloading from 1 Internet access at a time") || br.containsHTML("You can use your account for downloading from 1 Internet access at a time")) {
                    logger.warning("Your using account on multiple IP addresses at once");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Account been used on another Internet connection", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else {
                    logger.warning("Final downloadlink 'dllink' is null");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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

    /**
     * Check if a stored directlink exists under property 'property' and if so, check if it is still valid (leads to a downloadable content
     * [NOT html]).
     */
    public String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1 || !con.isOK()) {
                    /* Failure */
                    dllink = null;
                    return dllink;
                } else {
                    /* Ok */
                    return dllink;
                }
            } catch (final Exception e) {
                /* Failure */
                logger.log(e);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
                if (dllink == null) {
                    downloadLink.setProperty(property, Property.NULL);
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
        return account != null && (account.getType() == AccountType.PREMIUM || account.getType() == AccountType.UNKNOWN) && useAPI_setting;
    }

    /** Required to authenticate via API. */
    public static void setPremiumAPIHeaders(final Browser br, final String apiKey) {
        br.getHeaders().put("Authorization", "Bearer " + apiKey);
    }

    private void setBasicAuthHeader(final Browser br, final Account account) {
        br.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
    }

    private static AtomicReference<String> lastSessionPassword = new AtomicReference<String>(null);

    private Form getPasswordForm() throws Exception {
        final Form ret = br.getFormbyKey("pass");
        if (ret == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            ret.remove("save");
            if (!PluginJsonConfig.get(OneFichierConfigInterface.class).isPreferSSLEnabled()) {
                ret.put("dl_no_ssl", "on");
            }
            return ret;
        }
    }

    /** Try passwords in this order: 1. DownloadLink stored password, 2. Last used password, 3. Ask user */
    private void handlePasswordWebsite() throws Exception {
        synchronized (lastSessionPassword) {
            logger.info("This link seems to be password protected ...");
            Form pwForm = null;
            String passCode = null;
            for (int i = 0; i <= 2; i++) {
                switch (i) {
                case 0:
                    /* Try stored password if available */
                    pwForm = getPasswordForm();
                    // if property is set use it over lastSessionPassword!
                    passCode = currDownloadLink.getDownloadPassword();
                    break;
                case 1:
                    // next lastSessionPassword
                    passCode = lastSessionPassword.get();
                    break;
                case 2:
                    // last user input
                    passCode = Plugin.getUserInput("Password?", currDownloadLink);
                    break;
                default:
                    break;
                }
                if (passCode == null) {
                    continue;
                }
                pwForm.put("pass", Encoding.urlEncode(passCode));
                br.submitForm(pwForm);
                if (!br.containsHTML(HTML_PASSWORDPROTECTED)) {
                    lastSessionPassword.set(passCode);
                    currDownloadLink.setDownloadPassword(passCode);
                    return;
                } else {
                    pwForm = getPasswordForm();
                    // nullify stored password
                    currDownloadLink.setDownloadPassword(null);
                }
            }
            throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.onefichiercom.wrongpassword", "Password wrong!"));
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

    /** Returns an accessible downloadlink in the NEW format. */
    private String getDownloadlinkNEW(final DownloadLink dl) {
        final String host_of_current_downloadlink = Browser.getHost(dl.getDownloadURL());
        return "https://" + host_of_current_downloadlink + "/?" + getFID(dl);
    }

    /**
     * Makes sure that we're allowed to download a link. This function will also find out of a link is password protected.
     *
     * @throws Exception
     */
    private void checkDownloadable(final Account account) throws Exception {
        if (this.currDownloadLink.getBooleanProperty("privatelink", false)) {
            logger.info("Link is PRIVATE --> Checking whether it really is PRIVATE or just password protected");
            br.getPage(this.getDownloadlinkNEW(this.currDownloadLink));
            if (br.containsHTML(this.HTML_PASSWORDPROTECTED)) {
                logger.info("Link is password protected");
                this.pwProtected = true;
            } else if (br.containsHTML("Access to download")) {
                logger.info("Download is possible");
            } else if (br.containsHTML("Your account will be unlock")) {
                if (account != null) {
                    throw new AccountUnavailableException("Locked for security reasons", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "IP blocked for security reasons", 60 * 60 * 1000l);
                }
            } else {
                /** TODO: Check this case - check if that still exists */
                logger.info("Link is PRIVATE");
                throw new PluginException(LinkStatus.ERROR_FATAL, "This link is private. You're not authorized to download it!");
            }
        }
    }

    /** This function is there to make sure that we're really logged in (handling without API). */
    private boolean ensureSiteLogin(Account account) throws Exception {
        br.getPage("https://1fichier.com/console/index.pl");
        if (!checkSID(br) || !br.containsHTML("id=\"fileTree\"")) {
            logger.info("Site login seems not to be valid anymore - trying to refresh cookie");
            if (account != null) {
                this.loginWebsite(account, true);
                ensureSiteLogin(null);
                logger.info("Successfully refreshed login cookie");
            } else {
                if (br.containsHTML("For security reasons") && br.containsHTML("is temporarily locked")) {
                    throw new AccountUnavailableException("Locked for security reasons", 60 * 60 * 1000l);
                } else {
                    logger.warning("Failed to refresh login cookie");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        } else {
            logger.info("Success: our login cookie is fine - no need to do anything");
        }
        return true;
    }

    /** Returns the file/link-ID of any given downloadLink. */
    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        String test = new Regex(dl.getDownloadURL(), "/\\?([a-z0-9]+)$").getMatch(0);
        if (test == null) {
            test = new Regex(dl.getDownloadURL(), "://(?!www\\.)([a-z0-9]+)\\.").getMatch(0);
            if (test != null && test.matches("www")) {
                test = null;
            }
        }
        return test;
    }

    @PluginHost(host = "1fichier.com", type = Type.HOSTER)
    public static interface OneFichierConfigInterface extends PluginConfigInterface {
        public static class OneFichierConfigInterfaceTranslation {
            public String getPreferReconnectEnabled_label() {
                return _JDT.T.lit_prefer_reconnect();
            }

            public String getPreferSSLEnabled_label() {
                return _JDT.T.lit_prefer_ssl();
            }

            public String getSmallFilesWaitInterval_label() {
                return "Wait x seconds for small files (smaller than 50 mbyte) to prevent IP block";
            }

            public String getUsePremiumAPIEnabled_label() {
                return "Use premium API? This may help to get around 2FA login issues. Works ONLY for premium accounts! Once enabled, enter your API Key as username AND password!";
            }
        }

        public static final OneFichierConfigInterfaceTranslation TRANSLATION = new OneFichierConfigInterfaceTranslation();

        @AboutConfig
        @DefaultBooleanValue(false)
        @TakeValueFromSubconfig("PREFER_RECONNECT")
        boolean isPreferReconnectEnabled();

        void setPreferReconnectEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(true)
        @TakeValueFromSubconfig("PREFER_SSL")
        boolean isPreferSSLEnabled();

        void setPreferSSLEnabled(boolean b);

        @AboutConfig
        @DefaultBooleanValue(false)
        @TakeValueFromSubconfig("USE_PREMIUM_API")
        boolean isUsePremiumAPIEnabled();

        void setUsePremiumAPIEnabled(boolean b);

        @AboutConfig
        @DefaultIntValue(10)
        @SpinnerValidator(min = 0, max = 60)
        int getSmallFilesWaitInterval();

        void setSmallFilesWaitInterval(int i);
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
        setPremiumAPIHeaders(br, account);
        return br;
    }

    // public static class OnefichierAccountFactory extends MigPanel implements AccountBuilderInterface {
    // private static final long serialVersionUID = 1L;
    // private final String PINHELP = "Enter your API Key";
    //
    // private String getPassword() {
    // if (this.pass == null) {
    // return null;
    // }
    // if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
    // return null;
    // }
    // return new String(this.pass.getPassword());
    // }
    //
    // public boolean updateAccount(Account input, Account output) {
    // boolean changed = false;
    // if (!StringUtils.equals(input.getUser(), output.getUser())) {
    // output.setUser(input.getUser());
    // changed = true;
    // }
    // if (!StringUtils.equals(input.getPass(), output.getPass())) {
    // output.setPass(input.getPass());
    // changed = true;
    // }
    // return changed;
    // }
    //
    // private final ExtPasswordField pass;
    // private static String EMPTYPW = " ";
    //
    // public OnefichierAccountFactory(final InputChangedCallbackInterface callback) {
    // super("ins 0, wrap 2", "[][grow,fill]", "");
    // add(new JLabel("Click here to find your API Key:"));
    // add(new JLink("https://1fichier.com/console/params.pl"));
    // add(new JLabel("API Key:"));
    // add(this.pass = new ExtPasswordField() {
    // @Override
    // public void onChanged() {
    // callback.onChangedInput(this);
    // }
    // }, "");
    // pass.setHelpText(PINHELP);
    // }
    //
    // @Override
    // public JComponent getComponent() {
    // return this;
    // }
    //
    // @Override
    // public void setAccount(Account defaultAccount) {
    // if (defaultAccount != null) {
    // // name.setText(defaultAccount.getUser());
    // pass.setText(defaultAccount.getPass());
    // }
    // }
    //
    // @Override
    // public boolean validateInputs() {
    // // final String userName = getUsername();
    // // if (userName == null || !userName.trim().matches("^\\d{9}$")) {
    // // idLabel.setForeground(Color.RED);
    // // return false;
    // // }
    // // idLabel.setForeground(Color.BLACK);
    // return getPassword() != null;
    // }
    //
    // @Override
    // public Account getAccount() {
    // return new Account(null, getPassword());
    // }
    // }
    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}