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

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
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
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class RapideoPl extends PluginForHost {
    private static final String          NOCHUNKS           = "NOCHUNKS";
    private static final String          PROPERTY_DIRECTURL = "rapideopldirectlink";
    private static MultiHosterManagement mhm                = new MultiHosterManagement("rapideo.net");

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

    @Override
    public String rewriteHost(final String host) {
        /* 2022-02-22: Domain has changed from rapideo.pl to rapideo.net */
        return this.rewriteHost(getPluginDomains(), host);
    }

    public static String[] getAnnotationUrls() {
        return new String[] { "" };
    }

    public RapideoPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.rapideo.net/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.rapideo.net/";
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    private Browser prepBR(final Browser br) {
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        return br;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        prepBR(this.br);
        login(account, true);
        final boolean obtainTrafficViaOldAPI = true;
        if (obtainTrafficViaOldAPI) {
            /* API used in their browser addons */
            br.postPage("https://enc.rapideo.pl/", "site=newrd&output=json&loc=1&info=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + md5HEX(account.getPass()));
            final Map<String, Object> root = restoreFromString(br.toString(), TypeRef.MAP);
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
        // now let's get a list of all supported hosts:
        br.getPage("https://www." + account.getHoster() + "/twoje_pliki");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] crippledHosts = br.getRegex("<li>([A-Za-z0-9\\-\\.]+)</li>").getColumn(0);
        for (String crippledHost : crippledHosts) {
            crippledHost = crippledHost.toLowerCase();
            if (crippledHost.equals("mega")) {
                supportedHosts.add("mega.nz");
            } else {
                supportedHosts.add(crippledHost);
            }
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
        StringBuilder sb = new StringBuilder(a.length * 2);
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
        mhm.runCheck(account, link);
        final boolean resume = true;
        int maxChunks = 0;
        if (link.getBooleanProperty(RapideoPl.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        if (!this.attemptStoredDownloadurlDownload(link, resume, maxChunks)) {
            login(account, false);
            final String url = Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this));
            /* Generate session ID which we use below */
            final int random = new Random().nextInt(1000000);
            final DecimalFormat df = new DecimalFormat("000000");
            final String random_session = df.format(random);
            final String filename = link.getName();
            br.getPage("https://www." + this.getHost() + "/twoje_pliki");
            br.postPage("/twoje_pliki", "loadfiles=1");
            br.postPage("/twoje_pliki", "loadfiles=2");
            br.postPage("/twoje_pliki", "loadfiles=3");
            br.postPage("/twoje_pliki", "session=" + random_session + "&links=" + url);
            if (br.containsHTML("(?i)strong>Brak transferu</strong>")) {
                throw new AccountUnavailableException("Out of traffic", 1 * 60 * 1000l);
            }
            final String id = br.getRegex("data\\-id=\"([a-z0-9]+)\"").getMatch(0);
            if (id == null) {
                mhm.handleErrorGeneric(account, link, "Failed to find transferID", 20);
            }
            br.postPage("/twoje_pliki", "downloadprogress=1");
            br.postPage("/progress", "session=" + random_session + "&total=1");
            br.postPage("/twoje_pliki", "insert=1&ds=false&di=false&note=false&notepaths=" + url + "&sids=" + id + "&hids=&iids=&wids=");
            br.postPage("/twoje_pliki", "loadfiles=1");
            br.postPage("/twoje_pliki", "loadfiles=2");
            br.postPage("/twoje_pliki", "loadfiles=3");
            /* Sometimes it takes over 10 minutes until the file has been downloaded to the remote server. */
            String dllink = null;
            for (int i = 1; i <= 280; i++) {
                br.postPage("/twoje_pliki", "downloadprogress=1");
                final Map<String, Object> entries = restoreFromString(br.toString(), TypeRef.MAP);
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
            if (dllink == null) {
                mhm.handleErrorGeneric(account, link, "dllink_null", 20);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxChunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadlink did not lead to downloadable content", 50);
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(RapideoPl.NOCHUNKS, false) == false) {
                    link.setProperty(RapideoPl.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 chunk errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(RapideoPl.NOCHUNKS, false) == false) {
                link.setProperty(RapideoPl.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final boolean resume, final int chunks) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, chunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(PROPERTY_DIRECTURL);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(this.getHost(), cookies);
                    if (!verifyCookies) {
                        logger.info("Trust cookies without check");
                        return;
                    }
                    logger.info("Checking login cookies");
                    br.getPage("https://www." + this.getHost() + "/");
                    if (loggedIN(this.br)) {
                        logger.info("Successfully loggedin via cookies");
                        account.saveCookies(br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Failed to login via cookies");
                        br.clearAll();
                    }
                }
                logger.info("Attempting full login");
                br.getPage("https://www." + this.getHost() + "/");
                /*
                 * 2020-11-02: There are two Forms matching this but the first one is the one we want - the 2nd one is the
                 * "register new account" Form.
                 */
                final Form loginform = br.getFormbyKey("remember");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "on");
                br.submitForm(loginform);
                final String geoLoginFailure = br.getRegex(">\\s*(You login from a different location than usual[^<]*)<").getMatch(0);
                if (geoLoginFailure != null) {
                    /* 2020-11-02: Login from unusual location -> User has to confirm via URL send by mail and then try again in JD (?!). */
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, geoLoginFailure + "\r\nOnce done, refresh your account in JDownloader.", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                } else if (!loggedIN(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(this.getHost()), "");
                return;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean loggedIN(final Browser br) {
        if (br.containsHTML("(?i)Logged in as:|Zalogowany jako:")) {
            return true;
        } else {
            return false;
        }
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                final URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    return dllink;
                }
                con.disconnect();
            } catch (final Exception e) {
                return null;
            }
        }
        return null;
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