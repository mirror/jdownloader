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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapideo.pl" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class RapideoPl extends PluginForHost {

    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NOCHUNKS           = "NOCHUNKS";
    private static final String                            MAINPAGE           = "http://rapideo.pl";
    private static final String                            NICE_HOST          = MAINPAGE.replaceAll("(https://|http://)", "");
    private static final String                            NICE_HOSTproperty  = MAINPAGE.replaceAll("(https://|http://|\\.|\\-)", "");
    private static final String[][]                        HOSTS              = { { "unibytes", "unibytes.com" }, { "uptobox", "uptobox.com" }, { "fileshark", "fileshark.pl" }, { "megaszafa", "megaszafa.com" }, { "nowvideo", "nowvideo.sx" }, { "divxstage", "divxstage.eu" }, { "oboom", "oboom.com" }, { "rapidu", "rapidu.net" }, { "ddlstorage", "ddlstorage.com" }, { "uploadable", "uploadable.ch" }, { "uloz", "uloz.to" }, { "cloudstor", "cloudstor.es" }, { "1fichier", "1fichier.com" }, { "datafile", "datafile.com" }, { "hugefiles", "hugefiles.net" }, { "zippyshare", "zippyshare.com" }, { "fastshare", "fastshare.cz" }, { "lunaticfiles", "lunaticfiles.com" }, { "fileparadox", "fileparadox.in" }, { "filesmonster", "filesmonster.com" }, { "hd3d", "hd3d.cc" }, { "lumfile", "lumfile.com" }, { "catshare", "catshare.org" }, { "ultramegabit", "ultramegabit.com" }, { "hitfile", "hitfile.net" },
        { "uploaded", "uploaded.to" }, { "4shared", "4shared.com" }, { "turbobit", "turbobit.net" }, { "2shared", "2shared.com" }, { "ifilez", "depfile.com" }, { "freakshare", "freakshare.com" }, { "rapidgator", "rapidgator.net" }, { "mediafire", "mediafire.com" }, { "filefactory", "filefactory.com" }, { "filevelocity", "filevelocity.com" }, { "sendspace", "sendspace.com" }, { "cloudnator", "cloudnator.com" }, { "uptobox", "uptobox.com" }, { "filereactor", "filereactor.com" }, { "ifile", "filecloud.io" }, { "share-online", "share-online.biz" }, { "glumbouploads", "glumbouploads.com" } };

    public RapideoPl(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.rapideo.pl/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.rapideo.pl/";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    /*
     * TODO: Probably they also have time accounts (see answer of the browser extension API) --> Implement (we did not yet get such an
     * account type to test)
     */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ac = new AccountInfo();
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
        // check if account is valid
        if (!login(account, true)) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }

        /* API used in their browser addons */
        br.postPage("http://enc.rapideo.pl/", "site=newrd&output=json&loc=1&info=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + md5HEX(account.getPass()));
        final String traffic_left = br.getRegex("\"balance\":(\\d+)").getMatch(0);
        ac.setTrafficLeft(Long.parseLong(traffic_left) * 1024);

        // now let's get a list of all supported hosts:
        br.getPage("https://www.rapideo.pl/twoje_pliki");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        for (final String[] filehost : HOSTS) {
            final String crippledHost = filehost[0];
            final String realHost = filehost[1];
            if (br.containsHTML("<li>" + crippledHost + "</li>")) {
                supportedHosts.add(realHost);
            }
        }
        /* They only have accounts with traffic, no free/premium difference (other than no traffic) */
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        ac.setMultiHostSupport(this, supportedHosts);
        ac.setStatus("Premium User");
        return ac;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 0;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
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

        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap != null) {
                Long lastUnavailable = unavailableMap.get(link.getHost());
                if (lastUnavailable != null && System.currentTimeMillis() < lastUnavailable) {
                    final long wait = lastUnavailable - System.currentTimeMillis();
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Host is temporarily unavailable via " + this.getHost(), wait);
                } else if (lastUnavailable != null) {
                    unavailableMap.remove(link.getHost());
                    if (unavailableMap.size() == 0) {
                        hostUnavailableMap.remove(account);
                    }
                }
            }
        }

        login(account, false);
        int maxChunks = 0;
        if (link.getBooleanProperty(RapideoPl.NOCHUNKS, false)) {
            maxChunks = 1;
        }
        String dllink = checkDirectLink(link, "rapideopldirectlink");
        if (dllink == null) {
            final String url = Encoding.urlEncode(link.getDownloadURL());
            /* Generate session ID which we use below */
            final int random = new Random().nextInt(1000000);
            final DecimalFormat df = new DecimalFormat("000000");
            final String random_session = df.format(random);

            final String filename = link.getName();
            final String filename_encoded = Encoding.urlEncode(filename);
            String id;

            br.getPage("https://www.rapideo.pl/twoje_pliki");
            br.postPage("https://www.rapideo.pl/twoje_pliki", "loadfiles=1");
            br.postPage("https://www.rapideo.pl/twoje_pliki", "loadfiles=2");
            br.postPage("https://www.rapideo.pl/twoje_pliki", "loadfiles=3");

            br.postPage("https://www.rapideo.pl/twoje_pliki", "session=" + random_session + "&links=" + url);
            if (br.containsHTML("strong>Brak transferu</strong>")) {
                logger.info("Traffic empty");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            id = br.getRegex("data\\-id=\"([a-z0-9]+)\"").getMatch(0);
            if (id == null) {
                handleErrors(account, link, "id_null", 10);
            }

            br.postPage("https://www.rapideo.pl/twoje_pliki", "downloadprogress=1");
            br.postPage("https://www.rapideo.pl/progress", "session=" + random_session + "&total=1");

            br.postPage("https://www.rapideo.pl/twoje_pliki", "insert=1&ds=false&di=false&note=false&notepaths=" + url + "&sids=" + id + "&hids=&iids=&wids=");

            br.postPage("https://www.rapideo.pl/twoje_pliki", "loadfiles=1");
            br.postPage("https://www.rapideo.pl/twoje_pliki", "loadfiles=2");
            br.postPage("https://www.rapideo.pl/twoje_pliki", "loadfiles=3");

            boolean downloading = false;
            /* Sometimes it takes over 10 minutes until the file is on the MOCH server. */
            for (int i = 1; i <= 280; i++) {
                br.postPage("https://www.rapideo.pl/twoje_pliki", "downloadprogress=1");
                final String files_text = br.getRegex("\"StandardFiles\":\\[(.*?)\\]").getMatch(0);
                final String[] all_links = files_text.split("\\},\\{");
                for (final String link_info : all_links) {
                    if (link_info.contains(filename) || link_info.contains(filename_encoded)) {
                        if (link_info.contains("\"status\":\"initialization\"")) {
                            downloading = true;
                            continue;
                        }
                        downloading = false;
                        dllink = new Regex(link_info, "\"download_url\":\"(http[^<>\"]*?)\"").getMatch(0);
                        break;
                    }
                }
                /* File is not yet on server, reload progress to check again */
                if (downloading) {
                    this.sleep(3 * 1000l, link);
                    continue;
                }
                break;
            }
            if (dllink == null) {
                handleErrors(account, link, "dllink_null", 10);
            }
            dllink = dllink.replace("\\", "");
        }

        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            logger.info("Unhandled download error on rapideo.pl: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("rapideopldirectlink", dllink);
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

    /**
     * Is intended to handle errors which might occur seldom by re-tring a couple of times before we temporarily remove the host from the
     * host list.
     *
     * @param dl
     *            : The DownloadLink
     * @param error
     *            : The name of the error
     * @param maxRetries
     *            : Max retries before out of date error is thrown
     */
    private void handleErrors(final Account acc, final DownloadLink dl, final String error, final int maxRetries) throws PluginException {
        int timesFailed = dl.getIntegerProperty(NICE_HOSTproperty + "failedtimes_" + error, 0);
        dl.getLinkStatus().setRetryCount(0);
        if (timesFailed <= maxRetries) {
            logger.info(NICE_HOST + ": " + error + " -> Retrying");
            timesFailed++;
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, timesFailed);
            throw new PluginException(LinkStatus.ERROR_RETRY, error);
        } else {
            dl.setProperty(NICE_HOSTproperty + "failedtimes_" + error, Property.NULL);
            logger.info(NICE_HOST + ": " + error + " -> Disabling current host");
            tempUnavailableHoster(acc, dl, 1 * 60 * 60 * 1000l);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private static Object LOCK = new Object();

    @SuppressWarnings("unchecked")
    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return true;
                    }
                }
                br.setFollowRedirects(true);
                br.postPage("https://www.rapideo.pl/logowanie", "remember=on&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (!br.containsHTML("Logged in as:|Zalogowany jako:")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
                return true;
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                return false;
            }
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void tempUnavailableHoster(Account account, DownloadLink downloadLink, long timeout) throws PluginException {
        if (downloadLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unable to handle this errorcode!");
        }
        synchronized (hostUnavailableMap) {
            HashMap<String, Long> unavailableMap = hostUnavailableMap.get(account);
            if (unavailableMap == null) {
                unavailableMap = new HashMap<String, Long>();
                hostUnavailableMap.put(account, unavailableMap);
            }
            /* wait to retry this host */
            unavailableMap.put(downloadLink.getHost(), (System.currentTimeMillis() + timeout));
        }
        throw new PluginException(LinkStatus.ERROR_RETRY);
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}