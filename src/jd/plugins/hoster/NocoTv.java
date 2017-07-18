//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import java.math.BigInteger;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
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
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "noco.tv" }, urls = { "https?://(?:www\\.)?online\\.nolife\\-tv\\.com/emission/#(!|%21)/\\d+/[a-z0-9\\-_]+|https?://(?:www\\.)?noco\\.tv/emission/\\d+/.+" })
public class NocoTv extends PluginForHost {
    private String              dllink              = null;
    private static final String ONLYPREMIUMUSERTEXT = "Only downloadable for premium members";
    private boolean             notDownloadable     = false;
    private static Object       LOCK                = new Object();
    private static final String MAINPAGE            = "https://noco.tv/";
    private static final String SALT                = "YTUzYmUxODUzNzcwZjBlYmUwMzExZDY5OTNjN2JjYmU=";

    public NocoTv(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.nolife-tv.com/souscription");
    }

    public void correctDownloadLink(final DownloadLink link) {
        /** Videos come from a decrypter */
        link.setUrlDownload(link.getDownloadURL().replace("online.nolife-tvdecrypted.com/", "online.nolife-tv.com/"));
    }

    /** 2016-06-24: domainchange from online.nolife-tv.com to noco.tv - plugin has not yet been fixed - seems to be a pay-only site! */
    @Override
    public String rewriteHost(String host) {
        if ("online.nolife-tv.com".equals(getHost())) {
            if (host == null || "online.nolife-tv.com".equals(host)) {
                return "noco.tv";
            }
        }
        return super.rewriteHost(host);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        br.getPage(MAINPAGE);
        if (true) {
            logger.info("Plugin does not have any working free account support and premium account support is broken ...");
            ai.setStatus("No premium account?!");
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
            account.setValid(true);
            return ai;
        }
        ai.setUnlimitedTraffic();
        final String expire = br.getRegex(">Valable jusqu'au : <strong>(\\d{2}/\\d{2}/\\d{4})</strong>").getMatch(0);
        if (expire == null) {
            account.setValid(false);
            return ai;
        } else {
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy", null));
        }
        account.setValid(true);
        account.setType(AccountType.FREE);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public String getAGBLink() {
        return MAINPAGE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (notDownloadable) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, ONLYPREMIUMUSERTEXT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
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
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.postPage("https://" + this.getHost() + "/do.php", "cookie=1&a=login&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                if (br.getCookie(MAINPAGE, "id_user") == null) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    /**
     * JD2 CODE. DO NOT USE OVERRIDE FOR JD=) COMPATIBILITY REASONS!
     */
    public boolean isProxyRotationEnabledForLinkChecker() {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        /* Offline files should also get nice names */
        downloadLink.setName(new Regex(downloadLink.getDownloadURL(), "([a-z0-9\\-_]+)$").getMatch(0));
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:21.0) Gecko/20100101 Firefox/21.0");
        /* Richtige Dateigröße bezogen auf den Accounttyp erhalten */
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null && br.getCookie(MAINPAGE, "bbpassword") == null) {
            login(aa, false);
        }
        String dllink = downloadLink.getDownloadURL();
        br.getPage(dllink);
        if (this.br.containsHTML("Erreur : émission non trouvée")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<title>Nolife Online \\- ([^<>\"]+)</title>").getMatch(0);
        if (filename == null) {
            filename = new Regex(dllink, "https?://[^/]+/(.+)").getMatch(0);
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        filename = Encoding.htmlDecode(filename);
        if (br.containsHTML("id=\"message\\-non\\-abo\"") && br.containsHTML("class=\"noco\\-error\"")) {
            notDownloadable = true;
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.onlinenolifetvcom.only4premium", ONLYPREMIUMUSERTEXT));
            downloadLink.setName(filename + ".mp4");
            return AvailableStatus.TRUE;
        } else {
            /*
             * Hier wird über die Kekse(Premium/Free) bestimmt welche Videoqualität man bekommt. Spart oben in den dlmethoden einige Zeilen
             * an Code. Die Methode "setBrowserExclusive()" muss dabei deaktiviert sein.
             */
            long ts = System.currentTimeMillis();
            callAPI(this.br, getId(dllink));
            dllink = PluginJSonUtils.getJson(this.br, "file");
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            filename = filename.trim();
            final String ext = getFileNameExtensionFromString(dllink, ".mp4");
            downloadLink.setFinalFileName(filename + ext);
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openGetConnection(dllink);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    /* https://github.com/rg3/youtube-dl/blob/master/youtube_dl/extractor/noco.py */
    private void callAPI(final Browser br, final String videoID) throws Exception {
        final String ts = Long.toString(System.currentTimeMillis());
        // Missing encoding
        final String hash1 = JDHash.getMD5(ts);
        final String hexdigest1 = hexdigest(hash1);
        final String hash2 = JDHash.getMD5(hexdigest1 + "#8S?uCraTedap6a");
        // Mixxing encoding
        final String tk = hexdigest(hash2);
        // br.getPage(String.format("https://api.noco.tv/1.1/shows/%s/medias?ts=%s&tk=%s&", videoID, ts, tk));
        br.getPage(String.format("https://api.noco.tv/1.1/shows/%s/video/hd_1080/fr?ts=%s&tk=%s&", videoID, ts, tk));
    }

    /* http://pad.yohdah.com/101/python-vs-java-md5-hexdigest */
    public String hexdigest(final String message) throws Exception {
        String hd;
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.update(message.getBytes());
        BigInteger hash = new BigInteger(1, md5.digest());
        hd = hash.toString(16); // BigInteger strips leading 0's
        while (hd.length() < 32) {
            hd = "0" + hd;
        } // pad with leading 0's
        return hd;
    }

    private String getSKey(long s) {
        return JDHash.getMD5(JDHash.getMD5(String.valueOf(s)) + Encoding.Base64Decode(SALT));
    }

    private String getId(final String url) {
        final String id = new Regex(url, "/emission(?:/[^/]+)?/(\\d+)").getMatch(0);
        return id;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}