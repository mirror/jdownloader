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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
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
import jd.plugins.components.MultiHosterManagement;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RapideoPl extends PluginForHost {
    private static final String          PROPERTY_DIRECTURL = "rapideopldirectlink";
    private static MultiHosterManagement mhm                = new MultiHosterManagement("rapideo.net");

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        /* Prefer English language */
        br.setCookie(br.getHost(), "lang2", "EN");
        return br;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 0;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "rapideo.net", "rapideo.pl" });
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
        return new String[] { "" };
    }

    public RapideoPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www." + getHost() + "/rejestracja");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    protected String getAPIDomain() {
        // return getHost();
        return "rapideo.pl";
    }

    protected String getAPISiteParam() {
        return "newrd";
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        login(account, true);
        final boolean obtainTrafficViaOldAPI = true;
        if (obtainTrafficViaOldAPI) {
            /* API used in their browser addons */
            final Browser brc = br.cloneBrowser();
            final UrlQuery query = new UrlQuery();
            query.add("site", getAPISiteParam());
            query.add("output", "json");
            query.add("loc", "1");
            query.add("info", "1");
            query.add("username", Encoding.urlEncode(account.getUser()));
            query.add("password", md5HEX(account.getPass()));
            brc.postPage("https://enc." + getAPIDomain() + "/", query);
            final Map<String, Object> root = restoreFromString(brc.getRequest().getHtmlCode(), TypeRef.MAP);
            final String trafficLeftStr = root.get("balance").toString();
            if (trafficLeftStr != null && trafficLeftStr.matches("\\d+")) {
                ac.setTrafficLeft(Long.parseLong(trafficLeftStr) * 1024);
            }
        } else {
            /* Obtain "Traffic left" value from website */
            final String trafficLeftStr = br.getRegex("(?i)\">\\s*(?:Account balance|Stan Twojego konta)\\s*:\\s*(\\d+(\\.\\d{1,2})? [A-Za-z]{1,5})").getMatch(0);
            if (trafficLeftStr != null) {
                ac.setTrafficLeft(SizeFormatter.getSize(trafficLeftStr));
            }
        }
        final boolean obtainSupportedHostsListViaAPI = true;
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        boolean accountEmailActivationNeeded = false;
        if (obtainSupportedHostsListViaAPI) {
            /* API */
            br.getPage("https://www." + getHost() + "/clipboard.php?json=3");
            final List<Map<String, Object>> hosterlist = (List<Map<String, Object>>) restoreFromString(br.getRequest().getHtmlCode(), TypeRef.OBJECT);
            final boolean skipNonZeroSdownloadItems = false;
            for (final Map<String, Object> hosterinfo : hosterlist) {
                final List<String> domains = (List<String>) hosterinfo.get("domains");
                final Object sdownload = hosterinfo.get("sdownload");
                if (sdownload == null || domains == null) {
                    /* Skip invalid items (yes they exist, tested 2024-08-20) */
                    continue;
                }
                if (sdownload.toString().equals("0") || skipNonZeroSdownloadItems == false) {
                    supportedHosts.addAll(domains);
                } else {
                    logger.info("Skipping serverside disabled domains: " + domains);
                }
            }
        } else {
            /* Website */
            /* Get a list of all supported hosts */
            br.getPage("/twoje_pliki");
            final HashSet<String> crippledhosts = new HashSet<String>();
            /*
             * This will not catch stuff like <li><strong>nitroflare (beta) </strong></li> ... but we are lazy and ignore this. Such hosts
             * will be found too by the 2nd regex down below.
             */
            final String[] crippledHosts = br.getRegex("<li>([A-Za-z0-9\\-\\.]+)</li>").getColumn(0);
            if (crippledHosts != null && crippledHosts.length > 0) {
                for (final String crippledhost : crippledHosts) {
                    crippledhosts.add(crippledhost);
                }
            }
            /* Alternative place to obtain supported hosts from */
            final String[] crippledhosts2 = br.getRegex("class=\"active\"[^<]*>([^<]+)</a>").getColumn(0);
            if (crippledhosts2 != null && crippledhosts2.length > 0) {
                for (final String crippledhost : crippledhosts2) {
                    crippledhosts.add(crippledhost);
                }
            }
            /* Alternative place to obtain supported hosts from */
            String htmlMetadataStr = br.getRegex("name=\"keywords\" content=\"([\\w, ]+)").getMatch(0);
            if (htmlMetadataStr != null) {
                htmlMetadataStr = htmlMetadataStr.replace(" ", "");
                final String[] crippledhosts3 = htmlMetadataStr.split(",");
                if (crippledhosts3 != null && crippledhosts3.length > 0) {
                    for (final String crippledhost : crippledhosts3) {
                        crippledhosts.add(crippledhost);
                    }
                }
            }
            /* Sanitize data and add to final list */
            for (String crippledhost : crippledhosts) {
                crippledhost = crippledhost.toLowerCase(Locale.ENGLISH);
                if (crippledhost.equalsIgnoreCase("mega")) {
                    supportedHosts.add("mega.nz");
                } else {
                    supportedHosts.add(crippledhost);
                }
            }
            accountEmailActivationNeeded = br.containsHTML("Check your email and");
        }
        /*
         * They only have accounts with traffic, no free/premium difference (other than no traffic) - we treat no-traffic as FREE --> Cannot
         * download anything
         */
        if (ac.getTrafficLeft() > 0) {
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(-1);
            ac.setStatus("Premium account");
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(-1);
            ac.setStatus("Free account (no traffic left)");
        }
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        if (ac.getTrafficLeft() == 0 && AccountType.FREE.equals(account.getType()) && accountEmailActivationNeeded) {
            /* Un-activated free account without traffic -> Not usable at all. */
            throw new AccountUnavailableException("Check your email and click the confirmation link to activate your account", 5 * 60 * 1000l);
        }
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new AccountRequiredException();
    }

    public static String md5HEX(String s) {
        String result = null;
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(s.getBytes());
            result = toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // this won't happen, we know Java has MD5!
        }
        return result;
    }

    public static String toHex(byte[] a) {
        final StringBuilder sb = new StringBuilder(a.length * 2);
        for (int i = 0; i < a.length; i++) {
            sb.append(Character.forDigit((a[i] & 0xf0) >> 4, 16));
            sb.append(Character.forDigit(a[i] & 0x0f, 16));
        }
        return sb.toString();
    }

    /* no override to keep plugin compatible to old stable */
    /*
     * TODO: Improve errorhandling, remove all unneeded requests, maybe add a check if the desired file is already in the account(access
     * downloadprogress=1, see loop below)
     */
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        final String storedDirecturl = link.getStringProperty(PROPERTY_DIRECTURL);
        String dllink = null;
        if (storedDirecturl != null) {
            logger.info("Re-using stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            mhm.runCheck(account, link);
            login(account, false);
            final String url_urlencoded = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            final boolean useAPI = false;
            if (useAPI) {
                // TODO: Test this
                final Browser brc = br.cloneBrowser();
                final UrlQuery query = new UrlQuery();
                query.add("site", getAPISiteParam());
                query.add("output", "json");
                // query.add("loc", "1");
                query.add("info", "0");
                query.add("username", Encoding.urlEncode(account.getUser()));
                query.add("password", md5HEX(account.getPass()));
                query.add("url", url_urlencoded);
                brc.postPage("https://enc." + getAPIDomain() + "/", query);
                dllink = brc.getRequest().getHtmlCode();
            } else {
                /* Website */
                final int random = new Random().nextInt(1000000);
                final DecimalFormat df = new DecimalFormat("000000");
                final String random_session = df.format(random);
                final String filename = link.getName();
                br.getPage("https://www." + getHost() + "/twoje_pliki");
                br.postPage("/twoje_pliki", "loadfiles=1");
                br.postPage("/twoje_pliki", "loadfiles=2");
                br.postPage("/twoje_pliki", "loadfiles=3");
                br.postPage("/twoje_pliki", "session=" + random_session + "&links=" + url_urlencoded);
                if (br.containsHTML("(?i)strong>Brak transferu</strong>")) {
                    throw new AccountUnavailableException("Out of traffic", 1 * 60 * 1000l);
                }
                final String id = br.getRegex("data\\-id=\"([a-z0-9]+)\"").getMatch(0);
                if (id == null) {
                    mhm.handleErrorGeneric(account, link, "Failed to find transferID", 20);
                }
                br.postPage("/twoje_pliki", "downloadprogress=1");
                br.postPage("/progress", "session=" + random_session + "&total=1");
                br.postPage("/twoje_pliki", "insert=1&ds=false&di=false&note=false&notepaths=" + url_urlencoded + "&sids=" + id + "&hids=&iids=&wids=");
                br.postPage("/twoje_pliki", "loadfiles=1");
                br.postPage("/twoje_pliki", "loadfiles=2");
                br.postPage("/twoje_pliki", "loadfiles=3");
                /* Sometimes it takes over 10 minutes until the file has been downloaded to the remote server. */
                for (int i = 1; i <= 280; i++) {
                    br.postPage("/twoje_pliki", "downloadprogress=1");
                    final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                    final List<Map<String, Object>> standardFiles = (List<Map<String, Object>>) entries.get("StandardFiles");
                    Map<String, Object> activeDownloadingFileInfo = null;
                    for (final Map<String, Object> standardFile : standardFiles) {
                        final String thisFilename = (String) standardFile.get("filename");
                        final String thisFilenameFull = (String) standardFile.get("filename_full");
                        /* Find our file as multiple files could be downloading at the same time. */
                        if (thisFilename.equalsIgnoreCase(filename) || thisFilenameFull.equalsIgnoreCase(filename)) {
                            activeDownloadingFileInfo = standardFile;
                            break;
                        }
                    }
                    if (activeDownloadingFileInfo == null) {
                        mhm.handleErrorGeneric(account, link, "Failed to locate added info to actively downloading file", 20);
                    }
                    final String status = activeDownloadingFileInfo.get("status").toString();
                    if (status.equalsIgnoreCase("finish")) {
                        dllink = (String) activeDownloadingFileInfo.get("download_url");
                        break;
                    } else if (status.equalsIgnoreCase("initialization")) {
                        this.sleep(3 * 1000l, link);
                        continue;
                    } else {
                        /* Serverside download has never been started or stopped for unknown reasons. */
                        logger.warning("Serverside download failed?!");
                        break;
                    }
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllink_null", 20);
            }
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadlink did not lead to downloadable content", 50);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(PROPERTY_DIRECTURL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired?", e);
            } else {
                throw e;
            }
        }
        link.setProperty(PROPERTY_DIRECTURL, dllink);
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(this.getHost(), cookies);
                if (!verifyCookies) {
                    /* Don't verify */
                    return;
                }
                logger.info("Checking login cookies");
                br.getPage("https://www." + getHost() + "/");
                if (loggedINWebsite(this.br)) {
                    logger.info("Successfully loggedin via cookies");
                    account.saveCookies(br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Failed to login via cookies");
                }
            }
            logger.info("Attempting full login");
            br.getPage("https://www." + getHost() + "/");
            /*
             * 2020-11-02: There are two Forms matching this but the first one is the one we want - the 2nd one is the
             * "register new account" Form.
             */
            final Form loginform = br.getFormbyKey("remember");
            if (loginform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find loginform");
            }
            loginform.put("login", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            loginform.put("remember", "on");
            br.submitForm(loginform);
            final String geoLoginFailure = br.getRegex(">\\s*(You login from a different location than usual[^<]*)<").getMatch(0);
            if (geoLoginFailure != null) {
                /* 2020-11-02: Login from unusual location -> User has to confirm via URL send by mail and then try again in JD (?!). */
                throw new AccountUnavailableException(geoLoginFailure + "\r\nOnce done, refresh your account in JDownloader.", 5 * 60 * 1000l);
            } else if (!loggedINWebsite(br)) {
                throw new AccountInvalidException();
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return;
        }
    }

    private boolean loggedINWebsite(final Browser br) {
        if (br.containsHTML("(?i)Logged in as:|Zalogowany jako:")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}