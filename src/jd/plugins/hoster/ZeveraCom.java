//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.HashSet;
import java.util.LinkedHashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.config.Property;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zevera.com" }, urls = { "https?://[^/]+\\.zeveracdn\\.com/dl/.+|zeveradecrypted://" })
public class ZeveraCom extends UseNet {
    private static final String          NICE_HOST                 = "zevera.com";
    private static final String          NICE_HOSTproperty         = NICE_HOST.replaceAll("(\\.|\\-)", "");
    /* Connection limits */
    private static final boolean         ACCOUNT_PREMIUM_RESUME    = true;
    private static final int             ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private final String                 client_id                 = "306575304";
    private static Object                LOCK                      = new Object();
    private static MultiHosterManagement mhm                       = new MultiHosterManagement("zevera.com");

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        if (isDirectURL(link.getDownloadLink())) {
            final String new_url = link.getPluginPatternMatcher().replaceAll("[a-z0-9]+decrypted://", "https://");
            link.setPluginPatternMatcher(new_url);
        }
    }

    public ZeveraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium("https://www." + this.getHost() + "/premium");
    }

    @Override
    public String getAGBLink() {
        return "https://www." + this.getHost() + "/legal";
    }

    public static Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new ZeveraComAccountFactory(callback);
    }

    @Override
    public boolean isSpeedLimited(DownloadLink link, Account account) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (isUsenetLink(link)) {
            return super.requestFileInformation(link);
        } else {
            return requestFileInformationDirectURL(this.br, link);
        }
    }

    public static AvailableStatus requestFileInformationDirectURL(final Browser br, final DownloadLink link) throws Exception {
        URLConnectionAdapter con = null;
        try {
            con = br.openHeadConnection(link.getPluginPatternMatcher());
            if (!con.getContentType().contains("html") && con.isOK()) {
                if (link.getFinalFileName() == null) {
                    link.setFinalFileName(Encoding.urlDecode(Plugin.getFileNameFromHeader(con), false));
                }
                link.setVerifiedFileSize(con.getLongContentLength());
                return AvailableStatus.TRUE;
            } else if (con.getResponseCode() == 404) {
                /* Usually 404 when offline */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                /* E.g. 403 because of bad fair use status */
                return AvailableStatus.UNCHECKABLE;
            }
        } finally {
            try {
                /* make sure we close connection */
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (isDirectURL(downloadLink) && account == null) {
            // generated links do not require an account
            return true;
        }
        return account != null;
    }

    public boolean isDirectURL(final DownloadLink downloadLink) {
        return StringUtils.equals(getHost(), downloadLink.getHost());
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (!isDirectURL(link)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        handleDL_DIRECT(null, link);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        if (isUsenetLink(link)) {
            super.handleMultiHost(link, account);
            return;
        } else if (isDirectURL(link)) {
            handleDL_DIRECT(account, link);
        } else {
            this.br = prepBR(this.br);
            mhm.runCheck(account, link);
            ZeveraCom.login(this.br, account, false, client_id);
            String dllink = ZeveraCom.getDllink(this.br, account, link, client_id, this);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 2, 5 * 60 * 1000l);
            }
            handleDL_MOCH(account, link, dllink);
        }
    }

    private void handleDL_MOCH(final Account account, final DownloadLink link, final String dllink) throws Exception {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                handleAPIErrors(this.br);
                mhm.handleErrorGeneric(account, link, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    /** Account is not required */
    private void handleDL_DIRECT(final Account account, final DownloadLink link) throws Exception {
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getPluginPatternMatcher(), ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                // handleAPIErrors(this.br);
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    public static String getDllink(final Browser br, final Account account, final DownloadLink link, final String client_id, final PluginForHost hostPlugin) throws IOException, PluginException {
        String dllink = checkDirectLink(br, link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            /* TODO: Check if the cache function is useful for us */
            // br.getPage("https://www." + account.getHoster() + "/api/cache/check?client_id=" + client_id + "&pin=" +
            // Encoding.urlEncode(account.getPass()) + "&items%5B%5D=" +
            // Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, hostPlugin)));
            br.getPage("https://www." + account.getHoster() + "/api/transfer/directdl?client_id=" + client_id + "&pin=" + Encoding.urlEncode(account.getPass()) + "&src=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, hostPlugin)));
            dllink = PluginJSonUtils.getJsonValue(br, "location");
        }
        return dllink;
    }

    public static String checkDirectLink(final Browser br, final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = prepBR(this.br);
        final AccountInfo ai = fetchAccountInfoAPI(this, this.br, this.client_id, account);
        return ai;
    }

    public static AccountInfo fetchAccountInfoAPI(final PluginForHost hostPlugin, final Browser br, final String client_id, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(br, account, true, client_id);
        /* 2018-12-07: Rare serverside issue returns bad values e.g.: "limit_used":9.3966473825276e-5 */
        final String fair_use_used_str = PluginJSonUtils.getJson(br, "limit_used");
        final String premium_until_str = PluginJSonUtils.getJson(br, "premium_until");
        if (StringUtils.equalsIgnoreCase("false", premium_until_str)) {
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        } else {
            final long premium_until = premium_until_str != null ? Long.parseLong(premium_until_str) * 1000 : 0;
            if (premium_until > System.currentTimeMillis()) {
                account.setType(AccountType.PREMIUM);
                if (!StringUtils.isEmpty(fair_use_used_str)) {
                    final double d = Double.parseDouble(fair_use_used_str);
                    final int fairUsagePercent = (int) (d * 100.0);
                    if (fairUsagePercent > 200) {
                        /* Workaround for serverside issue returning e.g. 800% */
                        ai.setUnlimitedTraffic();
                        ai.setStatus("Premium | Fair usage: unknown");
                    } else if (fairUsagePercent >= 100) {
                        /* Fair use limit reached --> No traffic left, no downloads possible at the moment */
                        ai.setTrafficLeft(0);
                        ai.setStatus("Premium | Fair usage: " + fairUsagePercent + "% (limit reached)");
                    } else {
                        ai.setUnlimitedTraffic();
                        ai.setStatus("Premium | Fair usage:" + fairUsagePercent + "%");
                    }
                } else {
                    /* This should never happen */
                    ai.setStatus("Premium | Fair usage: unknown");
                    ai.setUnlimitedTraffic();
                }
                ai.setValidUntil(premium_until);
            } else {
                /* Expired == FREE */
                account.setType(AccountType.FREE);
                ai.setTrafficLeft(0);
            }
        }
        account.setMaxSimultanDownloads(-1);
        br.getPage("/api/services/list?client_id=" + client_id + "&pin=" + Encoding.urlEncode(account.getPass()));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        // final ArrayList<String> supportedHosts = new ArrayList<String>();
        final ArrayList<String> directdl = (ArrayList<String>) entries.get("directdl");
        final ArrayList<String> cache = (ArrayList<String>) entries.get("cache");
        final HashSet<String> list = new HashSet<String>();
        if (account.getHoster().equalsIgnoreCase("premiumize.me")) {
            list.add("usenet");
        }
        if (directdl != null) {
            list.addAll(directdl);
        }
        if (cache != null) {
            // FIXME: zevera has a bug and reporting supported hosts /eg rapidgator in cache only but it still works with directdl api
            // method
            list.addAll(cache);
        }
        ai.setMultiHostSupport(hostPlugin, new ArrayList<String>(list));
        return ai;
    }

    public static void login(Browser br, final Account account, final boolean force, final String clientID) throws Exception {
        synchronized (LOCK) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            br = prepBR(br);
            loginAPI(br, clientID, account, force);
        }
    }

    public static void loginAPI(final Browser br, final String clientID, final Account account, final boolean force) throws Exception {
        br.getPage("https://www." + account.getHoster() + "/api/account/info?client_id=" + clientID + "&pin=" + Encoding.urlEncode(account.getPass()));
        final String status = PluginJSonUtils.getJson(br, "status");
        if (!"success".equalsIgnoreCase(status)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "API-Key / PIN invalid! Make sure you entered your current API-Key / PIN which can be found here: " + account.getHoster() + "/account", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public static class ZeveraComAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your API-Key / PIN";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            }
            if (EMPTYPW.equals(new String(this.pass.getPassword()))) {
                return null;
            }
            return new String(this.pass.getPassword());
        }

        public boolean updateAccount(Account input, Account output) {
            boolean changed = false;
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                changed = true;
            }
            if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                changed = true;
            }
            return changed;
        }

        private final ExtPasswordField pass;
        private static String          EMPTYPW = "                 ";

        public ZeveraComAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API-Key / PIN:"));
            add(new JLink("https://www.zevera.com/account"));
            add(new JLabel("API-Key / PIN:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(PINHELP);
        }

        @Override
        public JComponent getComponent() {
            return this;
        }

        @Override
        public void setAccount(Account defaultAccount) {
            if (defaultAccount != null) {
                // name.setText(defaultAccount.getUser());
                pass.setText(defaultAccount.getPass());
            }
        }

        @Override
        public boolean validateInputs() {
            // final String userName = getUsername();
            // if (userName == null || !userName.trim().matches("^\\d{9}$")) {
            // idLabel.setForeground(Color.RED);
            // return false;
            // }
            // idLabel.setForeground(Color.BLACK);
            return getPassword() != null;
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    /** Keep this for possible future API implementation */
    private void handleAPIErrors(final Browser br) throws PluginException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}