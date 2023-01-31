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

import java.util.ArrayList;
import java.util.List;

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BangCom extends PluginForHost {
    public BangCom(PluginWrapper wrapper) {
        super(wrapper);
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            this.enablePremium("https://www.bang.com/joinnow");
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.bang.com/terms-of-service";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "bang.com" });
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
        for (final String[] domains : getPluginDomains()) {
            ret.add("");
        }
        return ret.toArray(new String[0]);
    }

    public void setBrowser(Browser br) {
        super.setBrowser(br);
        br.setFollowRedirects(true);
    }

    private final int          ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    public static final String PROPERTY_CONTENT_ID          = "content_id";
    public static final String PROPERTY_QUALITY_IDENTIFIER  = "quality";

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return link.getStringProperty(PROPERTY_CONTENT_ID) + "_" + link.getStringProperty(PROPERTY_QUALITY_IDENTIFIER);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String directurl = link.getPluginPatternMatcher();
        try {
            final URLConnectionAdapter con;
            if (isDownload) {
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, directurl, true, 0);
                con = dl.getConnection();
            } else {
                con = br.openHeadConnection(directurl);
            }
            if (!this.looksLikeDownloadableContent(con)) {
                if (!canRefresh(link)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            }
            link.setVerifiedFileSize(con.getCompleteContentLength());
        } finally {
            if (!isDownload && dl != null) {
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                dl = null;
            }
        }
        return AvailableStatus.TRUE;
    }

    private boolean canRefresh(final DownloadLink link) {
        final String qualityIdentifier = link.getStringProperty(PROPERTY_QUALITY_IDENTIFIER);
        if (StringUtils.equals(qualityIdentifier, "THUMBNAIL") || StringUtils.equals(qualityIdentifier, "PREVIEW")) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        requestFileInformation(link, account, true);
        if (this.dl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                final Cookies userCookies = account.loadUserCookies();
                if (userCookies != null || cookies != null) {
                    if (userCookies != null) {
                        br.setCookies(userCookies);
                    } else {
                        br.setCookies(cookies);
                    }
                    logger.info("Attempting user cookie login");
                    br.getPage("https://www." + this.getHost() + "/");
                    if (this.isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        if (userCookies != null) {
                            if (account.hasEverBeenValid()) {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_expired());
                            } else {
                                throw new AccountInvalidException(_GUI.T.accountdialog_check_cookies_invalid());
                            }
                        } else {
                            /* Try full login to refresh cookies */
                            br.clearAll();
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www." + this.getHost() + "/modal/login");
                // final Map<String, Object> postdata = new HashMap<String, Object>();
                // postdata.put("username", account.getUser());
                // postdata.put("password", account.getPass());
                // postdata.put("_csrf_token", "TODO");
                // br.postPage("https://www.bang.com/modal/login", "");
                final Form loginform = br.getFormbyActionRegex(".*/modal/login.*");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedin(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        br.getPage("/subscriptions");
        ai.setUnlimitedTraffic();
        final boolean isSubscriptionRunning = br.containsHTML("(?i)>\\s*Click to cancel");
        if (!isSubscriptionRunning) {
            /* Free Accounts got no advantages over using no account at all -> Do not allow the usage of such accounts. */
            ai.setExpired(true);
            return ai;
        } else {
            // TODO: Find expire-date
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        /* No captchas at all */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}