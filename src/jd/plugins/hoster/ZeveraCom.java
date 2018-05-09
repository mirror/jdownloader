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
import java.util.HashMap;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "zevera.com" }, urls = { "" })
public class ZeveraCom extends PluginForHost {
    private static final String                            NICE_HOST                 = "zevera.com";
    private static final String                            NICE_HOSTproperty         = NICE_HOST.replaceAll("(\\.|\\-)", "");
    /* Connection limits */
    private static final boolean                           ACCOUNT_PREMIUM_RESUME    = true;
    private static final int                               ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    private static final boolean                           USE_API                   = true;
    private final String                                   client_id                 = "306575304";
    private static Object                                  LOCK                      = new Object();
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap        = new HashMap<Account, HashMap<String, Long>>();
    private Account                                        currentAcc                = null;
    private DownloadLink                                   currentLink               = null;
    private static MultiHosterManagement                   mhm                       = new MultiHosterManagement("zevera.com");

    public ZeveraCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setAccountwithoutUsername(true);
        this.enablePremium("https://www.zevera.com/premium");
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new ZeveraComAccountFactory(callback);
    }

    @Override
    public String getAGBLink() {
        return "https://www.zevera.com/legal";
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    private void setConstants(final Account acc, final DownloadLink dl) {
        this.currentAcc = acc;
        this.currentLink = dl;
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        }
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = prepBR(this.br);
        setConstants(account, link);
        mhm.runCheck(currentAcc, currentLink);
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
        String dllink = getDllink(link);
        if (StringUtils.isEmpty(dllink)) {
            mhm.handleErrorGeneric(currentAcc, currentLink, "dllinknull", 2, 5 * 60 * 1000l);
        }
        handleDL(account, link, dllink);
    }

    private String getDllink(final DownloadLink link) throws IOException, PluginException {
        String dllink = checkDirectLink(link, NICE_HOSTproperty + "directlink");
        if (dllink == null) {
            if (USE_API) {
                dllink = getDllinkAPI(this.br, this.client_id, this.currentAcc, link, this);
            } else {
                dllink = getDllinkWebsite(link);
            }
        }
        return dllink;
    }

    public static String getDllinkAPI(final Browser br, final String clientID, final Account account, final DownloadLink link, final PluginForHost plugin) throws IOException, PluginException {
        br.getPage("https://www." + account.getHoster() + "/api/transfer/directdl?client_id=" + clientID + "&pin=" + Encoding.urlEncode(account.getPass()) + "&src=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, plugin)));
        final String dllink = PluginJSonUtils.getJsonValue(br, "location");
        return dllink;
    }

    public static String getDllinkWebsite(final DownloadLink link) throws IOException, PluginException {
        return null;
    }

    private void handleDL(final Account account, final DownloadLink link, final String dllink) throws Exception {
        link.setProperty(NICE_HOSTproperty + "directlink", dllink);
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
            final String contenttype = dl.getConnection().getContentType();
            if (contenttype.contains("html")) {
                br.followConnection();
                updatestatuscode();
                handleAPIErrors(this.br);
                mhm.handleErrorGeneric(currentAcc, currentLink, "unknowndlerror", 2, 5 * 60 * 1000l);
            }
            this.dl.startDownload();
        } catch (final Exception e) {
            link.setProperty(NICE_HOSTproperty + "directlink", Property.NULL);
            throw e;
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
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
        setConstants(account, null);
        this.br = prepBR(this.br);
        final AccountInfo ai;
        if (USE_API) {
            ai = fetchAccountInfoAPI(this.br, this.client_id, account);
        } else {
            ai = fetchAccountInfoWebsite(account);
        }
        return ai;
    }

    public static AccountInfo fetchAccountInfoWebsite(final Account account) throws Exception {
        return null;
    }

    public AccountInfo fetchAccountInfoAPI(final Browser br, final String clientID, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /* TODO: Check if this is actually the fair use value ... */
        final String fair_use_used_str = PluginJSonUtils.getJson(br, "limit_used");
        final String premium_until_str = PluginJSonUtils.getJson(br, "premium_until");
        final long premium_until = premium_until_str != null ? Long.parseLong(premium_until_str) * 1000 : 0;
        if (premium_until > System.currentTimeMillis()) {
            account.setType(AccountType.PREMIUM);
            if (!StringUtils.isEmpty(fair_use_used_str)) {
                final double d = Double.parseDouble(fair_use_used_str);
                ai.setStatus("Premium | Fair usage:" + (100 - ((int) (d * 100.0))) + "%");
            } else {
                ai.setStatus("Premium");
            }
            ai.setValidUntil(premium_until);
            ai.setUnlimitedTraffic();
        } else {
            /* Expired == FREE */
            account.setType(AccountType.FREE);
            ai.setTrafficLeft(0);
        }
        br.getPage("/api/services/list?client_id=" + clientID + "&pin=" + Encoding.urlEncode(account.getPass()));
        final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        // final ArrayList<String> supportedHosts = new ArrayList<String>();
        final ArrayList<String> ressourcelist = (ArrayList<String>) entries.get("directdl");
        ai.setMultiHostSupport(this, ressourcelist);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            /* Load cookies */
            br.setCookiesExclusive(true);
            this.br = prepBR(this.br);
            if (USE_API) {
                loginAPI(this.br, this.client_id, account, force);
            } else {
                loginWebsite(account, force);
            }
        }
    }

    private void loginWebsite(final Account account, final boolean force) throws Exception {
    }

    public static void loginAPI(final Browser br, final String clientID, final Account account, final boolean force) throws Exception {
        br.getPage("https://www." + account.getHoster() + "/api/account/info?client_id=" + clientID + "&pin=" + Encoding.urlEncode(account.getPass()));
        final String status = PluginJSonUtils.getJson(br, "status");
        if (!"success".equalsIgnoreCase(status)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Login PIN invalid! Make sure you're using your PIN as password see " + account.getHoster() + "/account", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
    }

    public static class ZeveraComAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your Apikey";

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
            add(new JLabel("Click here to find your Apikey:"));
            add(new JLink("https://www.zevera.com/account"));
            add(new JLabel("Apikey:"));
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
    private void updatestatuscode() {
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