//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Random;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "x-art.com" }, urls = { "https?://(?:www\\.)?(x\\-art(decrypted)?\\.com/(members/)?(videos|galleries)/.+|([a-z0-9]+\\.)?x-art(decrypted)?\\.com/.+\\.(mov|mp4|wmv|zip).*)" })
public class XArtCom extends PluginForHost {
    public static interface XArtConfigInterface extends PluginConfigInterface {
        @DefaultBooleanValue(false)
        @Order(10)
        boolean isGrabBestVideoVersionEnabled();

        void setGrabBestVideoVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(20)
        boolean isGrab4KVideoEnabled();

        void setGrab4KVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(30)
        boolean isGrab1080pVideoEnabled();

        void setGrab1080pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(40)
        boolean isGrab720pVideoEnabled();

        void setGrab720pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(50)
        boolean isGrab540pVideoEnabled();

        void setGrab540pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(60)
        boolean isGrab360pVideoEnabled();

        void setGrab360pVideoEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(70)
        boolean isGrabBestImagesVersionEnabled();

        void setGrabBestImagesVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(80)
        boolean isGrab1200pImagesVersionEnabled();

        void setGrab1200pImagesVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(90)
        boolean isGrab2000pImagesVersionEnabled();

        void setGrab2000pImagesVersionEnabled(boolean b);

        @DefaultBooleanValue(true)
        @Order(100)
        boolean isGrab4000pImagesVersionEnabled();

        void setGrab4000pImagesVersionEnabled(boolean b);
    }

    private String downloadURL = null;

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceAll("decrypted\\.com", ".com"));
    }

    public XArtCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.x-art.com/join/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public Class<? extends PluginConfigInterface> getConfigInterface() {
        return XArtConfigInterface.class;
    }

    @Override
    public String getAGBLink() {
        return "https://www." + getHost() + "/legal/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        downloadURL = null;
        if (!link.isNameSet()) {
            String name = getFileNameFromURL(new URL(link.getPluginPatternMatcher()));
            if (name == null) {
                /* Fallback */
                name = new Regex(link.getPluginPatternMatcher(), "https?://[^/]+/(.+)").getMatch(0);
            }
            if (name != null) {
                link.setName(name);
            }
        }
        final ArrayList<Account> accounts = AccountController.getInstance().getValidAccounts(getHost());
        Account account = null;
        if (accounts != null && accounts.size() != 0) {
            final Iterator<Account> it = accounts.iterator();
            while (it.hasNext()) {
                final Account n = it.next();
                if (n.isEnabled() && n.isValid()) {
                    account = n;
                    break;
                }
            }
        }
        this.setBrowserExclusive();
        if (account != null) {
            login(account, br, false);
        }
        br.setFollowRedirects(true);
        URLConnectionAdapter urlcon = null;
        try {
            urlcon = br.openHeadConnection(link.getPluginPatternMatcher());
            final int responseCode = urlcon.getResponseCode();
            if (this.looksLikeDownloadableContent(urlcon)) {
                downloadURL = urlcon.getURL().toExternalForm();
                if (urlcon.getCompleteContentLength() > 0) {
                    if (urlcon.isContentDecoded()) {
                        link.setDownloadSize(urlcon.getCompleteContentLength());
                    } else {
                        link.setVerifiedFileSize(urlcon.getCompleteContentLength());
                    }
                }
                return AvailableStatus.TRUE;
            }
            if (account != null) {
                logger.info("Trying to obtain fresh directurl");
                final String pageURL = link.getStringProperty("pageURL", null);
                if (pageURL != null) {
                    final String videoID = link.getStringProperty("videoID", null);
                    final String imageID = link.getStringProperty("imageID", null);
                    final String quality = link.getStringProperty("quality", null);
                    final String ext = link.getStringProperty("ext", null);
                    final ArrayList<DownloadLink> results = new ArrayList<DownloadLink>();
                    jd.plugins.decrypter.XArt.parseUrl(br, this, results, pageURL, true);
                    if (imageID != null) {
                        for (final DownloadLink result : results) {
                            if (StringUtils.equals(imageID, result.getStringProperty("imageID", null)) && StringUtils.equals(quality, result.getStringProperty("quality", null))) {
                                correctDownloadLink(result);
                                downloadURL = result.getPluginPatternMatcher();
                                break;
                            }
                        }
                    } else if (videoID != null) {
                        for (final DownloadLink result : results) {
                            if (StringUtils.equals(videoID, result.getStringProperty("videoID", null)) && StringUtils.equals(quality, result.getStringProperty("quality", null)) && StringUtils.equals(ext, result.getStringProperty("ext", null))) {
                                correctDownloadLink(result);
                                downloadURL = result.getPluginPatternMatcher();
                                break;
                            }
                        }
                    }
                }
                if (downloadURL != null) {
                    link.setPluginPatternMatcher(downloadURL);
                    return AvailableStatus.TRUE;
                }
            }
            if (responseCode == 401 && account != null) {
                throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                if (urlcon != null) {
                    urlcon.disconnect();
                }
            } catch (final Exception ignore) {
            }
        }
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        /* Download without account is not possible. */
        throw new AccountRequiredException();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        // check if it's time for the next full login.
        final long nextFullLogin = account.getLongProperty("nextFullLogin", 0);
        if (System.currentTimeMillis() <= nextFullLogin) {
            /* Do not validate login */
            login(account, br, false);
        } else {
            /* Validate login */
            login(account, br, true);
        }
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    public void login(final Account account, final Browser lbr, final boolean force) throws Exception {
        synchronized (account) {
            final boolean redirect = lbr.isFollowingRedirects();
            try {
                lbr.getHeaders().put("Authorization", "Basic " + Encoding.Base64Encode(account.getUser() + ":" + account.getPass()));
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    lbr.setCookies(this.getHost(), cookies);
                    prepBrowser(lbr);
                    lbr.setFollowRedirects(true);
                    lbr.getPage("https://www." + this.getHost() + "/members/");
                    if (isLoggedIN(lbr)) {
                        logger.info("Cookie/basic auth login successful");
                        account.saveCookies(lbr.getCookies(lbr.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie/basic auth login failed");
                        lbr.clearCookies(null);
                    }
                }
                prepBrowser(lbr);
                lbr.setFollowRedirects(true);
                lbr.getPage("https://www." + this.getHost() + "/members/");
                if (!isLoggedIN(lbr)) {
                    /* 2022-11-03: They're using basic auth now but we'll still leave this in as fallback */
                    Form loginform = br.getFormbyActionRegex(".*auth.form");
                    if (loginform == null) {
                        // 2024-07-31
                        loginform = br.getFormbyProperty("id", "login_frm");
                    }
                    if (loginform != null) {
                        logger.info("Found loginForm");
                        loginform.put("username", Encoding.urlEncode(account.getUser()));
                        loginform.put("rpassword", Encoding.urlEncode(account.getPass()));
                        /* Make cookies last long */
                        loginform.put("remember", "y");
                        lbr.submitForm(loginform);
                    } else {
                        logger.warning("Failed to find loginform");
                    }
                }
                if (!isLoggedIN(lbr)) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(lbr.getCookies(lbr.getHost()), "");
                // logic to randomize the next login attempt, to prevent issues with static login detection
                long ran2 = 0;
                // between 2 hours && 6 hours
                while (ran2 == 0 || (ran2 <= 7200000 && ran2 >= 21600000)) {
                    // generate new ran1 for each while ran2 valuation.
                    long ran1 = 0;
                    while (ran1 <= 1) {
                        ran1 = new Random().nextInt(7);
                    }
                    // random of 1 hour, times ran1
                    ran2 = new Random().nextInt(3600000) * ran1;
                }
                account.setProperty("nextFullLogin", System.currentTimeMillis() + ran2);
                account.setProperty("lastFullLogin", System.currentTimeMillis());
                // end of logic
            } catch (final PluginException e) {
                account.clearCookies("");
                account.setProperty("nextFullLogin", Property.NULL);
                account.setProperty("lastFullLogin", Property.NULL);
                throw e;
            } finally {
                lbr.setFollowRedirects(redirect);
            }
        }
    }

    private boolean isLoggedIN(final Browser br) {
        return br.containsHTML("(?i)>\\s*Logout\\s*</a>");
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (downloadURL == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, downloadURL, true, -5);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 401) {
                /* Login failure */
                throw new AccountUnavailableException("Session expired?", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, Account acc) {
        return false;
    }

    private Browser prepBrowser(Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setFollowRedirects(true);
        return prepBr;
    }

    @Override
    public String getDescription() {
        return "Download videos- and pictures from the x-art.com.";
    }
}
