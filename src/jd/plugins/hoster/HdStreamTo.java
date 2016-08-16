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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.download.DownloadInterface;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hdstream.to" }, urls = { "https?://(www\\.)?hdstream\\.to/(#(!|%21)f=[A-Za-z0-9]+(\\-[A-Za-z0-9]+)?|f/.+)" }, flags = { 2 })
public class HdStreamTo extends antiDDoSForHost {

    public HdStreamTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://hdstream.to/#!p=reg");
    }

    @Override
    public String getAGBLink() {
        return "http://hdstream.to/#!p=tos";
    }

    /* Tags: 1tube.to, hdstream.to */
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

    private Exception            checklinksexception          = null;

    @Override
    protected Browser prepBrowser(Browser prepBr, String host) {
        if (!(browserPrepped.containsKey(prepBr) && browserPrepped.get(prepBr) == Boolean.TRUE)) {
            super.prepBrowser(prepBr, host);
            prepBr.setCookie(getHost(), "lang", "en");
            prepBr.getHeaders().put("User-Agent", "JDownloader");
            /* User can select https or http in his hdstream account, therefore, redirects should be allowed */
            prepBr.setFollowRedirects(true);
        }
        return prepBr;
    }

    /** Using API: http://hdstream.to/#!p=api */
    @SuppressWarnings("deprecation")
    @Override
    public boolean checkLinks(final DownloadLink[] urls) {
        if (urls == null || urls.length == 0) {
            return false;
        }
        try {
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
                postPage("http://hdstream.to/json/check.php", sb.toString());
                for (final DownloadLink dllink : links) {
                    final String fid = getFID(dllink);
                    final String thisjson = br.getRegex("(\"" + fid + "\":\\{.*?\\})").getMatch(0);
                    if (fid == null || thisjson == null || !"on".equals(PluginJSonUtils.getJsonValue(thisjson, "state"))) {
                        dllink.setAvailable(false);
                    } else {
                        final String sha1hash = PluginJSonUtils.getJsonValue(thisjson, "hash");
                        /*
                         * file_title = user defined title (without extension) - prefer that --> name = original title/server-title (with
                         * extension, can contain encoding issues)
                         */
                        final String extension = "." + PluginJSonUtils.getJsonValue(thisjson, "extension");
                        String name = PluginJSonUtils.getJsonValue(thisjson, "file_title");
                        if (name == null) {
                            name = PluginJSonUtils.getJsonValue(thisjson, "name");
                        }
                        final String size = PluginJSonUtils.getJsonValue(thisjson, "size");
                        name = Encoding.htmlDecode(name);
                        name = name.trim();
                        name = encodeUnicode(name);
                        if (!".null".equals(extension) && !name.endsWith(extension)) {
                            name += extension;
                        }
                        /* Names via API are good --> Use as final filenames */
                        dllink.setFinalFileName(name);
                        dllink.setDownloadSize(SizeFormatter.getSize(size));
                        if (sha1hash != null && !"null".equals(sha1hash)) {
                            dllink.setSha1Hash(sha1hash);
                        }
                        dllink.setAvailable(true);
                    }
                }
                if (index == urls.length) {
                    break;
                }
            }
        } catch (final Exception e) {
            checklinksexception = e;
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
        /* If exception happens in availablecheck it will be caught --> Browser is empty --> Throw it here to prevent further errors. */
        if (checklinksexception != null) {
            throw checklinksexception;
        }
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink downloadLink, boolean resumable, int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        final String fid = getFID(downloadLink);
        final String premiumtoken = getPremiumToken(downloadLink);
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            br.setFollowRedirects(false);
            checkDownloadable(br);
            getPage("https://hdstream.to/send.php?visited=" + fid + "&premium=" + premiumtoken);
            final String canPlay = PluginJSonUtils.getJsonValue(br, "canPlay");
            final String server = PluginJSonUtils.getJsonValue(br, "server");
            String waittime = null;
            final String free_downloadable = PluginJSonUtils.getJsonValue(br, "downloadable");
            final String free_downloadable_max_filesize = new Regex(free_downloadable, "mb(\\d+)").getMatch(0);
            final String traffic_left_free = PluginJSonUtils.getJsonValue(br, "traffic");
            if ("true".equals(canPlay)) {
                /* Prefer to download the stream if possible as it has the same filesize as download but no waittime. */
                final Browser br2 = br.cloneBrowser();
                getPage(br2, "https://s" + server + ".hdstream.to/send.php?token=" + fid + "&stream=1");
                dllink = br2.getRedirectLocation();
            }
            if (dllink == null) {
                /* Stream impossible? --> Download file! */
                /* Note that premiumtokens can override this */
                if ("premium".equals(free_downloadable) || (free_downloadable_max_filesize != null && downloadLink.getDownloadSize() >= SizeFormatter.getSize(free_downloadable_max_filesize + " mb")) && "".equals(premiumtoken)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
                } else if ("0".equals(traffic_left_free)) {
                    /*
                     * We can never know how long we habve to wait - also while we might have this problem for one file, other, smaller
                     * files can still be downloadable --> Let's wait an hour, then try again.
                     */
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 1 * 60 * 60 * 1000l);
                }
                dllink = getDllink(downloadLink);
                dllink += "&premium=" + premiumtoken;
                getPage("/send.php?visited=" + fid + "&premium=" + premiumtoken);
                waittime = PluginJSonUtils.getJsonValue(br, "wait");
                if (waittime == null) {
                    /* This should never happen! */
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final int wait = Integer.parseInt(waittime);
                /* Make sure that the premiumtoken is valid - if it is not valid, wait is higher than 0 */
                if (!premiumtoken.equals("") && wait == 0) {
                    logger.info("Seems like the user is using a valid premiumtoken, enabling chunks & resume...");
                    resumable = ACCOUNT_PREMIUM_RESUME;
                    maxchunks = PREMIUM_OVERALL_MAXCON;
                } else {
                    sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
                }
            }
        }
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        errorhandlingFree(dl, br);
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://hdstream.to";
    private static Object       LOCK     = new Object();

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(getHost(), cookies);
                    return;
                }
                postPage("http://hdstream.to/json/login.php", "data=%7B%22username%22%3A%22" + Encoding.urlEncode(account.getUser()) + "%22%2C+%22password%22%3A%22" + Encoding.urlEncode(account.getPass()) + "%22%7D");
                if (br.getCookie(MAINPAGE, "username") == null || br.containsHTML("\"logged_in\":false")) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                account.saveCookies(br.getCookies(getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        return fetchAccountInfoHdstream(br, account);
    }

    @SuppressWarnings("deprecation")
    public AccountInfo fetchAccountInfoHdstream(final Browser br, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        getPage(br, "https://" + account.getHoster() + "/json/userdata.php?user=" + Encoding.urlEncode(account.getUser()));
        final String createtime = PluginJSonUtils.getJsonValue(br, "joined");
        final String expire = PluginJSonUtils.getJsonValue(br, "premium");
        final String trafficleft_string = PluginJSonUtils.getJsonValue(br, "remaining_traffic");
        long trafficleft_long = 0;
        if (trafficleft_string != null && !trafficleft_string.equals("null")) {
            trafficleft_long = Long.parseLong(trafficleft_string) * 1024 * 1024;
        }
        ai.setCreateTime(TimeFormatter.getMilliSeconds(createtime, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH));
        ai.setTrafficLeft(trafficleft_long);
        if (expire.equals("0")) {
            /*
             * Free accounts are accepted but basically never used because their traffic is ZERO. Admin told us they only bring minor
             * advantages, not related to downloading links of others so it's all fine.
             */
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(false);
            ai.setStatus("Free Account");
        } else {
            getPage(br, "https://s." + account.getHoster() + "/js/data.js");
            int max_prem_dls = ACCOUNT_PREMIUM_MAXDOWNLOADS;
            try {
                max_prem_dls = Integer.parseInt(PluginJSonUtils.getJsonValue(br, "max_connections_premium"));
            } catch (final Throwable e) {
            }
            ai.setValidUntil(Long.parseLong(expire) * 1000l);
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(max_prem_dls);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium Account");
        }
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        checkDownloadable(br);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
        } else {
            final String dllink = getDllink(link);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            errorhandlingPremium(dl, br, account);
            link.setProperty("premium_directlink", dllink);
            dl.startDownload();
        }
    }

    public void errorhandlingFree(final DownloadInterface dl, final Browser br) throws PluginException, IOException {
        if (dl.getConnection().getContentType().contains("html")) {
            /* 403 error means different things for free and premium */
            /* Should never happen here */
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
            }
            errorhandlingGeneral(dl);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error - please contact our support", 30 * 60 * 1000l);
        }
    }

    public void errorhandlingPremium(final DownloadInterface dl, final Browser br, final Account account) throws PluginException, IOException {
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Daily traffic limit reached", PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            errorhandlingGeneral(dl);
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error - please contact our support", 30 * 60 * 1000l);
        }
    }

    private void errorhandlingGeneral(final DownloadInterface dl) throws PluginException {
        /* Should never happen */
        if (dl.getConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    /** Get fid of the link, no matter which linktype is added by the user. */
    @SuppressWarnings("deprecation")
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
    @SuppressWarnings("deprecation")
    private String getPremiumToken(final DownloadLink dl) {
        final String addedlink = dl.getDownloadURL();
        String premtoken = new Regex(addedlink, "hdstream\\.to/(?:f/|#\\!f=)[A-Za-z0-9]+\\-([A-Za-z0-9]+)$").getMatch(0);
        if (premtoken == null) {
            premtoken = "";
        }
        return premtoken;
    }

    /**
     * Check if the link can be downloaded - "download"=0 = NOT downloadable, not even for premium users - either server problems or only
     * streamable - rare case
     */
    public void checkDownloadable(final Browser br) throws PluginException {
        if (!"1".equals(PluginJSonUtils.getJsonValue(br, "download"))) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "This link is not downloadable");
        }
    }

    /** Returns final downloadlink, same for free and premium */
    private String getDllink(final DownloadLink dl) {
        return "http://s" + PluginJSonUtils.getJsonValue(br, "server") + ".hdstream.to/send.php?token=" + getFID(dl);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
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