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
import java.util.Random;

import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "rapideo.pl" }, urls = { "" })
public class RapideoPl extends PluginForHost {
    private static final String          NOCHUNKS = "NOCHUNKS";
    private static MultiHosterManagement mhm      = new MultiHosterManagement("rapideo.pl");

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
        login(account, true);
        /* API used in their browser addons */
        br.postPage("https://enc." + this.getHost() + "/", "site=newrd&output=json&loc=1&info=1&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + md5HEX(account.getPass()));
        final String traffic_left_str = PluginJSonUtils.getJson(br, "balance");
        long traffic_left = 0;
        if (traffic_left_str != null && traffic_left_str.matches("\\d+")) {
            traffic_left = Long.parseLong(traffic_left_str) * 1024;
            ac.setTrafficLeft(traffic_left);
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
        if (traffic_left > 0) {
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
        mhm.runCheck(account, link);
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
                mhm.handleErrorGeneric(account, link, "id_null", 20);
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
                mhm.handleErrorGeneric(account, link, "dllink_null", 20);
            }
            dllink = dllink.replace("\\", "");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, maxChunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            mhm.handleErrorGeneric(account, link, "Final downloadlink did not lead to downloadable content", 20);
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

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    private void login(final Account account, final boolean verifyCookies) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
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
                    if (loggedIN()) {
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
                } else if (!loggedIN()) {
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

    private boolean loggedIN() {
        return br.containsHTML("Logged in as:|Zalogowany jako:");
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