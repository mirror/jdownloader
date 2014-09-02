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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountError;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hdstream.to" }, urls = { "https?://(www\\.)?hdstream\\.to/(#\\!f=[A-Za-z0-9]+(\\-[A-Za-z0-9]+)?|f/.+)" }, flags = { 2 })
public class HdStreamTo extends PluginForHost {

    public HdStreamTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hdstream.to/#!p=reg");
    }

    @Override
    public String getAGBLink() {
        return "http://hdstream.to/#!p=tos";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = false;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 10;

    private static final int     PREMIUM_OVERALL_MAXCON       = -ACCOUNT_PREMIUM_MAXDOWNLOADS;

    /* don't touch the following! */
    private static AtomicInteger maxPrem                      = new AtomicInteger(1);

    /** Using API: http://hdstream.to/#!p=api */
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
            prepBrowser(br);
            br.setCookiesExclusive(true);
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
                sb.append("check=");
                for (final DownloadLink dl : links) {
                    sb.append(dl.getDownloadURL());
                    sb.append("%0A");
                }
                br.postPage("http://hdstream.to/json/check.php", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = this.getFID(dllink);
                    final String thisjson = br.getRegex("(\"" + fid + "\":\\{.*?\\})").getMatch(0);
                    if (fid == null || thisjson == null || !"on".equals(this.getJson(thisjson, "state"))) {
                        dllink.setAvailable(false);
                    } else {
                        final String hash = this.getJson(thisjson, "hash");
                        final String name = this.getJson(thisjson, "name");
                        final String size = this.getJson(thisjson, "size");
                        dllink.setAvailable(true);
                        dllink.setFinalFileName(encodeUnicode(Encoding.htmlDecode(name)));
                        dllink.setDownloadSize(SizeFormatter.getSize(size));
                        if (hash != null) {
                            dllink.setMD5Hash(hash);
                        }
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            return AvailableStatus.UNCHECKED;
        }
        if (downloadLink.isAvailabilityStatusChecked() && !downloadLink.isAvailable()) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink, boolean resume, int maxchunks) throws Exception, PluginException {
        checkDownloadable();
        String premiumtoken = getPremiumToken(downloadLink);
        final String free_downloadable = getJson("downloadable");
        final String free_downloadable_max_filesize = new Regex(free_downloadable, "mb(\\d+)").getMatch(0);
        /* Note that premiumtokens can override this */
        if (free_downloadable.equals("premium") || (free_downloadable_max_filesize != null && downloadLink.getDownloadSize() >= SizeFormatter.getSize(free_downloadable_max_filesize + " mb")) && premiumtoken.equals("")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) {
                    throw (PluginException) e;
                }
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        String dllink = getDllink(downloadLink);
        dllink += "&premium=" + premiumtoken;
        final String fid = getFID(downloadLink);
        try {
            br.getPage("http://hdstream.to/json/filelist.php?file=" + fid);
        } catch (final Throwable e) {
        }
        br.getPage("http://hdstream.to/send.php?visited=" + fid + "&premium=" + premiumtoken);
        final String waittime = getJson("wait");
        if (waittime == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int wait = Integer.parseInt(waittime);
        /* Make sure that the premiumtoken is valid - if it is not valid, wait is higher than 0 */
        if (!premiumtoken.equals("") && wait == 0) {
            logger.info("Seems like the user is using a valid premiumtoken, enabling chunks & resume...");
            resume = ACCOUNT_PREMIUM_RESUME;
            maxchunks = PREMIUM_OVERALL_MAXCON;
        } else {
            this.sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resume, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            /* 403 error means different things for free and premium */
            /* Should never happen here */
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
            }
            handleServerErrors();
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error - please contact our support", 30 * 60 * 1000l);
        }
        dl.startDownload();
    }

    private void handleServerErrors() throws PluginException {
        /* Should never happen */
        if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    /** Get fid of the link, no matter which linktype is added by the user. */
    private String getFID(final DownloadLink dl) {
        final String addedlink = dl.getDownloadURL();
        String fid = new Regex(addedlink, "([A-Za-z0-9]+)\\.html$").getMatch(0);
        if (fid == null) {
            fid = new Regex(addedlink, "hdstream\\.to/(f/|#\\!f=)([A-Za-z0-9]+)").getMatch(1);
        }
        return fid;
    }

    /**
     * Links which contain a premium token can be downloaded via free like a premium user - in case such a token exists in a link, this
     * function will return it.
     *
     * @return: "" (empty String) if there is no token and the token if there is one
     */
    private String getPremiumToken(final DownloadLink dl) {
        final String addedlink = dl.getDownloadURL();
        String premtoken = new Regex(addedlink, "hdstream\\.to/(f/|#\\!f=)[A-Za-z0-9]+\\-([A-Za-z0-9]+)$").getMatch(1);
        if (premtoken == null) {
            premtoken = "";
        }
        return premtoken;
    }

    /**
     * Check if the link can be downloaded - "download"=0 = NOT downloadable, not even for premium users - either server problems or only
     * streamable - rare case
     */
    private void checkDownloadable() throws PluginException {
        if (!this.getJson("download").equals("1")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This link is not downloadable");
        }
    }

    /**
     * Tries to return value of key from JSon response, from String source.
     *
     * @author raztoki
     * */
    protected String getJson(final String source, final String key) {
        String result = new Regex(source, "\"" + key + "\":(-?\\d+(\\.\\d+)?|true|false|null)").getMatch(0);
        if (result == null) {
            result = new Regex(source, "\"" + key + "\":\"([^\"]+)\"").getMatch(0);
        }
        if (result != null) {
            result = result.replaceAll("\\\\/", "/");
        }
        return result;
    }

    /**
     * Tries to return value of key from JSon response, from default 'br' Browser.
     *
     * @author raztoki
     * */
    protected String getJson(final String key) {
        return getJson(br.toString(), key);
    }

    private void prepBrowser(final Browser br) {
        br.setCookie("http://hdstream.to/", "lang", "en");
        /* User can select https or http in his hdstream account, therefore, redirects should be allowed */
        br.setFollowRedirects(true);
        br.getHeaders().put("User-Agent", "JDownloader");
    }

    /** Returns final downloadlink, same for free and premium */
    private String getDllink(final DownloadLink dl) {
        return "http://s" + getJson("server") + ".hdstream.to/send.php?token=" + getFID(dl);
    }

    private static final String MAINPAGE = "http://hdstream.to";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBrowser(this.br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.postPage("http://hdstream.to/json/login.php", "data=%7B%22username%22%3A%22" + Encoding.urlEncode(account.getUser()) + "%22%2C+%22password%22%3A%22" + Encoding.urlEncode(account.getPass()) + "%22%7D");
                if (br.getCookie(MAINPAGE, "username") == null || br.containsHTML("\"logged_in\":false")) {
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
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        /* reset maxPrem workaround on every fetchaccount info */
        maxPrem.set(1);
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("http://hdstream.to/json/userdata.php?user=" + Encoding.urlEncode(account.getUser()));
        final String createtime = this.getJson("joined");
        final String expire = this.getJson("premium");
        final String trafficleft = this.getJson("remaining_traffic");
        ai.setCreateTime(TimeFormatter.getMilliSeconds(createtime, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        ai.setTrafficLeft(Long.parseLong(trafficleft) * 1024 * 1024);
        if (expire.equals("0")) {
            /*
             * Free accounts are accepted but basically never used because their traffic is ZERO. Admin told us they only bring minor
             * advantages, not related to downloading links of others so it's all fine.
             */
            account.setProperty("free", true);
            try {
                account.setType(AccountType.FREE);
                maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
                /* free accounts can still have captcha */
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(false);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Registered (free) user");
        } else {
            account.setProperty("free", false);
            br.getPage("http://s.hdstream.to/js/data.js");
            int max_prem_dls = ACCOUNT_PREMIUM_MAXDOWNLOADS;
            try {
                max_prem_dls = Integer.parseInt(getJson("max_connections_premium"));
            } catch (final Throwable e) {
            }
            ai.setValidUntil(Long.parseLong(expire) * 1000l);
            try {
                account.setType(AccountType.PREMIUM);
                maxPrem.set(max_prem_dls);
                account.setMaxSimultanDownloads(maxPrem.get());
                account.setConcurrentUsePossible(true);
            } catch (final Throwable e) {
                /* not available in old Stable 0.9.581 */
            }
            ai.setStatus("Premium User");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        String dllink;
        requestFileInformation(link);
        checkDownloadable();
        dllink = getDllink(link);
        login(account, false);
        if (account.getBooleanProperty("free", false)) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
        } else {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            if (dl.getConnection().getContentType().contains("html")) {
                if (dl.getConnection().getResponseCode() == 403) {
                    logger.info("Received 403 error in premium mode --> Traffic limit reached or server error (usually traffic limit reached)");
                    try {
                        account.setError(AccountError.TEMP_DISABLED, "Daily traffic limit reached");
                    } catch (final Throwable e) {
                    }
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily traffic limit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
                }
                handleServerErrors();
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error - please contact our support", 30 * 60 * 1000l);
            }
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}