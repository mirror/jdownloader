//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

import java.util.Locale;
import java.util.Map;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.downloader.hls.HLSDownloader;

import jd.PluginWrapper;
import jd.http.Cookies;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/**
 *
 * @author raztoki
 *
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "flimmit.com" }, urls = { "http://flimmit\\.com/\\d+" })
public class FlimmitCom extends PluginForHost {
    public FlimmitCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.flimmit.com/customer/account/create/");
    }

    @Override
    public String getAGBLink() {
        return "http://www.flimmit.com/agb/";
    }

    /* Connection stuff */
    private static final int FREE_MAXDOWNLOADS = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        // final Account aa = AccountController.getInstance().getValidAccount(this);
        // if (aa == null) {
        // link.getLinkStatus().setStatusText("Only downloadable for registered/premium users");
        // return AvailableStatus.UNCHECKABLE;
        // }
        // this.login(aa, false);
        // br.setFollowRedirects(true);
        // br.getPage(link.getDownloadURL());
        // if (!br.getURL().contains("/play/")) {
        // throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        // }
        String filename = link.getStringProperty("filename", null);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) {
                throw (PluginException) e;
            }
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered/premium users");
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    br.setCookies(cookies);
                    if (!force) {
                        logger.info("Trust cookies without login");
                        return;
                    } else {
                        br.getPage("https://flimmit.at/account");
                        if (this.isLoggedIN()) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(br.getHost()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearAll();
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://flimmit.at/de/login");
                br.postPageRaw("/de/dynamically/user/login", String.format("{\"email\":\"%s\",\"password\":\"%s\",\"_csrf_token\":null}", account.getUser(), account.getPass()));
                br.getPage("/account");
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* E.g. bad response: {"status":"failure","message":"Email oder Passwort ist nicht korrekt","extraData":[]} */
                /* E.g. good response: */
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail address in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        /* All accounts are "premium" - users have to buy the movies to get the links they can add to JD. */
        br.getPage("/dynamically/me/user-subscriptions/active");
        Map<String, Object> entries = JSonStorage.restoreFromString(br.toString(), TypeRef.HASHMAP);
        final Object dataO = entries.get("data");
        if (dataO != null) {
            entries = (Map<String, Object>) dataO;
            String expireDateStr = (String) entries.get("next_payment_date");
            if (!StringUtils.isEmpty(expireDateStr)) {
                account.setType(AccountType.PREMIUM);
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDateStr, "yyyy-MM-dd'T'HH:mm:ssXXX", Locale.ENGLISH));
                ai.setUnlimitedTraffic();
            } else {
                account.setType(AccountType.FREE);
                /* Free accounts cannot download/stream anything. */
                ai.setTrafficLeft(0);
            }
        } else {
            account.setType(AccountType.FREE);
            /* Free accounts cannot download/stream anything. */
            ai.setTrafficLeft(0);
        }
        account.setConcurrentUsePossible(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        // hls stuff
        final String dllink = link.getStringProperty("m3u8url", null);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // dllink = dllink.replace("https://flimmitcdn-a.akamaihd.net/", "https://edgecast-stream.flimmit.com/");
        checkFFmpeg(link, "Download a HLS Stream");
        dl = new HLSDownloader(link, br, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}