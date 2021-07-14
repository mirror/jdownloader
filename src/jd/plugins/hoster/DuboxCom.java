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
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.decrypter.DuboxComFolder;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.DownloadLinkDownloadable;
import jd.plugins.download.HashInfo;

import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.StringUtils;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.parser.UrlQuery;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class DuboxCom extends PluginForHost {
    public DuboxCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.dubox.com/");
    }

    @Override
    public String getAGBLink() {
        return "https://www.dubox.com/";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "terabox.com", "dubox.com" });
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
        final List<String> ret = new ArrayList<String>();
        // for (final String[] domains : getPluginDomains()) {
        // ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/file/([A-Za-z0-9\\-_]+)");
        // }
        /* No pattern required at all */
        ret.add("");
        return ret.toArray(new String[0]);
    }

    public static final String PROPERTY_DIRECTURL       = "directurl";
    public static final String PROPERTY_PASSWORD_COOKIE = "password_cookie";
    public static final String PROPERTY_PAGINATION_PAGE = "pagination_page";

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    public int getMaxChunks(final Account account) {
        return 0;
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

    private String getFID(final DownloadLink link) {
        try {
            return UrlQuery.parse(link.getPluginPatternMatcher()).get("fsid");
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        if (account == null) {
            /* Without account we can't generate new directurls and can't check existing directurls! */
            return AvailableStatus.UNCHECKABLE;
        }
        if (link.hasProperty(PROPERTY_PASSWORD_COOKIE)) {
            DuboxComFolder.setPasswordCookie(this.br, this.getHost(), link.getStringProperty(PROPERTY_PASSWORD_COOKIE));
        }
        try {
            this.login(account, false);
        } catch (final Exception ignore) {
            logger.log(ignore);
            return AvailableStatus.UNCHECKABLE;
        }
        if (checkDirectLink(link, PROPERTY_DIRECTURL) != null) {
            logger.info("Availablecheck via directurl complete");
            return AvailableStatus.TRUE;
        } else {
            /*
             * Crawl the folder again to get a fresh directurl. There is no other way to do this. If the folder is big and the crawler has
             * to go through pagination, this can take a while!
             */
            final PluginForDecrypt decrypter = getNewPluginForDecryptInstance(getHost());
            final CryptedLink param = new CryptedLink(link.getContainerUrl(), link);
            if (link.getDownloadPassword() != null) {
                /* Crawler should not ask user again for that password! */
                param.setDecrypterPassword(link.getDownloadPassword());
            }
            try {
                /* 2021-04-24: Handling has been changed so array should only contain the one element we need! */
                final ArrayList<DownloadLink> items = ((jd.plugins.decrypter.DuboxComFolder) decrypter).crawlFolder(param, account, this.getFID(link));
                DownloadLink target = null;
                for (final DownloadLink tmp : items) {
                    if (StringUtils.equals(this.getFID(tmp), this.getFID(link))) {
                        target = tmp;
                        break;
                    }
                }
                /* Assume that item is offline (deleted within folder). */
                if (target == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else if (!target.hasProperty(PROPERTY_DIRECTURL)) {
                    logger.warning("Failed to refresh directurl");
                    return AvailableStatus.UNCHECKABLE;
                } else {
                    logger.info("Successfully refreshed directurl");
                    link.setProperty(PROPERTY_DIRECTURL, target.getStringProperty(PROPERTY_DIRECTURL));
                    return AvailableStatus.TRUE;
                }
            } catch (final Exception e) {
                return AvailableStatus.UNCHECKABLE;
            }
        }
    }

    public static AvailableStatus parseFileInformation(final DownloadLink link, final Map<String, Object> entries) throws IOException, PluginException {
        final String filename = (String) entries.get("server_filename");
        final long filesize = JavaScriptEngineFactory.toLong(entries.get("size"), -1);
        final String md5 = (String) entries.get("md5");
        /* Typically only available when user is logged in. */
        final String directurl = (String) entries.get("dlink");
        final String fsidStr = Long.toString(JavaScriptEngineFactory.toLong(entries.get("fs_id"), -1));
        if (!StringUtils.isEmpty(filename)) {
            link.setFinalFileName(filename);
        }
        if (filesize > 0) {
            link.setVerifiedFileSize(filesize);
        }
        if (!StringUtils.isEmpty(md5)) {
            link.setMD5Hash(md5);
        }
        if (!StringUtils.isEmpty(directurl)) {
            link.setProperty(PROPERTY_DIRECTURL, directurl);
        }
        link.setAvailable(true);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, null);
        throw new AccountRequiredException();
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            final Cookies userCookies = Cookies.parseCookiesFromJsonString(account.getPass(), getLogger());
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                    logger.info("Cookies are still fresh --> Trust cookies without login");
                    return false;
                }
                if (this.checkLoginStatus()) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(this.br.getCookies(this.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                }
            }
            logger.info("Full login required");
            if (userCookies == null) {
                showCookieLoginInformation();
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "Cookie login required", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            logger.info("Performing full (user-cookie) login");
            br.setCookies(userCookies);
            if (!this.checkLoginStatus()) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.saveCookies(this.br.getCookies(this.getHost()), "");
            return true;
        }
    }

    private Thread showCookieLoginInformation() {
        final String host = this.getHost();
        final Thread thread = new Thread() {
            public void run() {
                try {
                    final String help_article_url = "https://support.jdownloader.org/Knowledgebase/Article/View/account-cookie-login-instructions";
                    String message = "";
                    final String title;
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        title = host + " - Login";
                        message += "Hallo liebe(r) " + host + " NutzerIn\r\n";
                        message += "Um deinen " + host + " Account in JDownloader verwenden zu können, musst du folgende Schritte beachten:\r\n";
                        message += "Folge der Anleitung im Hilfe-Artikel:\r\n";
                        message += help_article_url;
                    } else {
                        title = host + " - Login";
                        message += "Hello dear " + host + " user\r\n";
                        message += "In order to use an account of this service in JDownloader, you need to follow these instructions:\r\n";
                        message += help_article_url;
                    }
                    final ConfirmDialog dialog = new ConfirmDialog(UIOManager.LOGIC_COUNTDOWN, title, message);
                    dialog.setTimeout(3 * 60 * 1000);
                    if (CrossSystem.isOpenBrowserSupported() && !Application.isHeadless()) {
                        CrossSystem.openURL(help_article_url);
                    }
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

    private boolean checkLoginStatus() throws IOException {
        br.setFollowRedirects(true);
        br.getPage("https://www." + this.getHost() + "/disk/home");
        /* NOT logged in --> Redirect to mainpage */
        if (br.getURL().contains("/disk/home")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* 2021-04-14: Only free accounts are existant/supported */
        ai.setUnlimitedTraffic();
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        if (!attemptStoredDownloadurlDownload(link, PROPERTY_DIRECTURL, this.isResumeable(link, account), this.getMaxChunks(account))) {
            /* Avoid checking seemingly invalid stored directurl again in availablecheck! */
            link.removeProperty(PROPERTY_DIRECTURL);
            this.requestFileInformation(link, account);
            final String dllink = link.getStringProperty(PROPERTY_DIRECTURL);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Failed to refresh expired directurl", 3 * 60 * 1000l);
            }
            dl = new jd.plugins.BrowserAdapter().openDownload(br, getDownloadLinkDownloadable(link), br.createGetRequest(dllink), this.isResumeable(link, account), this.getMaxChunks(account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                logger.warning("The final dllink seems not to be a file!");
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
            }
        }
        /*
         * htmldecode final filename just in case we're using in from Content-Disposition and not the one that was set during the crawl
         * process.
         */
        dl.setFilenameFix(true);
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumable, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, getDownloadLinkDownloadable(link), brc.createGetRequest(url), resumable, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    private DownloadLinkDownloadable getDownloadLinkDownloadable(final DownloadLink link) {
        final String host = this.getHost();
        /* 2021-04-20: Workaround: Given MD5 values are wrong so let's not use these ones! */
        final DownloadLinkDownloadable downloadLinkDownloadable = new DownloadLinkDownloadable(link) {
            @Override
            public HashInfo getHashInfo() {
                return null;
            }

            @Override
            public String getHost() {
                final DownloadInterface dli = getDownloadInterface();
                if (dli != null) {
                    final URLConnectionAdapter connection = dli.getConnection();
                    if (connection != null) {
                        return connection.getURL().getHost();
                    }
                }
                return host;
            }
        };
        return downloadLinkDownloadable;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* 2021-04-14: No captchas at all */
        return false;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        /* 2021-04-14: Downloads only possible via account */
        if (account != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        // if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
        // link.removeProperty(PROPERTY_DIRECTURL);
        // }
    }
}