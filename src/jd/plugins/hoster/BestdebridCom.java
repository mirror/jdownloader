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
import java.util.LinkedHashMap;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "bestdebrid.com" }, urls = { "" })
public class BestdebridCom extends PluginForHost {
    private static final String          API_BASE            = "https://bestdebrid.com/api/v1";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("bestdebrid.com");
    private static final int             defaultMAXDOWNLOADS = -1;
    private static final int             defaultMAXCHUNKS    = 0;
    private static final boolean         defaultRESUME       = true;

    @SuppressWarnings("deprecation")
    public BestdebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://bestdebrid.com/plans");
    }

    @Override
    public String getAGBLink() {
        return "https://bestdebrid.com/";
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink downloadLink, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        /* handle premium should never be called */
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(final Account account, final DownloadLink link) throws Exception {
        String dllink = checkDirectLink(link, this.getHost() + "directlink");
        br.setFollowRedirects(true);
        if (dllink == null) {
            /*
             * 2019-07-12: TODO: Find out which result we get back. Also find out what the parameter 'ip' is good for. API documentation
             * only states: "ip=dst_client_ip".
             */
            getAPISafe(API_BASE + "/generateLink?auth=" + Encoding.urlEncode(this.getApiKey(account)) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)) + "&ip=null", account, link);
            // dllink = PluginJSonUtils.getJsonValue(br, "TODO_VALUE_KEY_UNKNOWN");
            dllink = br.getRegex("\"(https?://[^\"]+)\"").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "dllinknull", 50, 5 * 60 * 1000l);
            }
        }
        link.setProperty(this.getHost() + "directlink", dllink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, defaultMAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection(true);
            handleKnownErrors(this.br, account, link);
            mhm.handleErrorGeneric(account, link, "unknown_dl_error", 50, 5 * 60 * 1000l);
        }
        this.dl.startDownload();
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        mhm.runCheck(account, link);
        handleDL(account, link);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                logger.log(e);
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return dllink;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.br = newBrowser();
        final AccountInfo ai = new AccountInfo();
        loginAPI(account);
        /* 2019-07-12: No idea what 'bypass_api_limit' means! */
        // final String bypass_api_limit = PluginJSonUtils.getJson(br, "bypass_api_limit");
        /* 2019-07-12: TODO: Find out in which form/datatype expiredate and trafficleft are given! */
        final String expire = PluginJSonUtils.getJson(br, "expire");
        final String traffic_leftStr = PluginJSonUtils.getJson(br, "credit");
        long validuntil = 0;
        if (expire != null && expire.matches("\\d+")) {
            validuntil = Long.parseLong(expire) * 1000l;
        }
        if (validuntil < System.currentTimeMillis()) {
            account.setType(AccountType.FREE);
            ai.setStatus("Free account");
            // account.setMaxSimultanDownloads(1);
            /*
             * 2019-07-12: Seems like they have hosts which free account users can download from but their hostlist does not state which
             * ones these are. In the "Informations" tab there is a list of "Free hosters": https://bestdebrid.com/downloader (more precise:
             * https://bestdebrid.com/info.php) For now, we will simply set the free account traffic to ZERO.
             */
            ai.setTrafficLeft(0);
        } else {
            account.setType(AccountType.PREMIUM);
            ai.setStatus("Premium account");
            account.setMaxSimultanDownloads(defaultMAXDOWNLOADS);
            ai.setValidUntil(validuntil, this.br);
            ai.setTrafficLeft(SizeFormatter.getSize(traffic_leftStr));
        }
        br.getPage(API_BASE + "/hosts?auth=" + Encoding.urlEncode(this.getApiKey(account)));
        final ArrayList<String> supportedhostslist = new ArrayList<String>();
        LinkedHashMap<String, Object> entries;
        final ArrayList<Object> hosters;
        final Object hostersO = JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        int counter = 0;
        if (hostersO instanceof LinkedHashMap) {
            /* 2019-07-15: They are using a map with numbers as String as key --> This is a workaround for that */
            entries = (LinkedHashMap<String, Object>) hostersO;
            hosters = new ArrayList<Object>();
            Object tempO = null;
            do {
                if (counter == 0) {
                    /* 2019-07-15: Special case: Array might start with 1 so check here for Object on [0] */
                    tempO = entries.get("0");
                    if (tempO != null) {
                        hosters.add(tempO);
                    }
                    counter++;
                }
                tempO = entries.get(Integer.toString(counter));
                if (tempO != null) {
                    hosters.add(tempO);
                }
                counter++;
            } while (tempO != null && counter > 1);
        } else {
            /* 2019-07-15: In case they ever correct their Map to an Array, we will need the following line. */
            hosters = (ArrayList<Object>) JavaScriptEngineFactory.jsonToJavaObject(br.toString());
        }
        for (final Object hosterO : hosters) {
            entries = (LinkedHashMap<String, Object>) hosterO;
            // final String downsincedate = (String)entries.get("downsincedate");
            final String status = (String) entries.get("status");
            /* 2019-07-12: TLDs are missing - admin has been advised to change this! */
            final String host = (String) entries.get("name");
            if (!StringUtils.isEmpty(host) && "up".equalsIgnoreCase(status)) {
                supportedhostslist.add(host);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void loginAPI(final Account account) throws IOException, PluginException {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.getPage(API_BASE + "/user?auth=" + Encoding.urlEncode(this.getApiKey(account)));
                /** 2019-07-05: No idea how long this token is valid! */
                final String status = PluginJSonUtils.getJson(br, "error");
                if (status != null && !"0".equals(status)) {
                    /* E.g. {"error":"bad username OR bad password"} */
                    final String fail_reason = PluginJSonUtils.getJson(br, "message");
                    if (!StringUtils.isEmpty(fail_reason)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, fail_reason, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                /*
                 * Used logs in via apikey - via website, username & email are required. Set mail as username so: 1. User can identify
                 * different accounts in JD better and 2. If someone steals the users' database he still cannot login via website!
                 */
                final String email = PluginJSonUtils.getJson(br, "email");
                // final String username = PluginJSonUtils.getJson(br, "username");
                if (!StringUtils.isEmpty(email)) {
                    account.setUser(email);
                }
            } catch (PluginException e) {
                throw e;
            }
        }
    }

    private String getApiKey(final Account account) {
        return account.getPass();
    }

    /** getPage with errorhandling */
    private void getAPISafe(final String accesslink, final Account account, final DownloadLink link) throws IOException, PluginException, InterruptedException {
        this.br.getPage(accesslink);
        handleKnownErrors(this.br, account, link);
    }

    private void handleKnownErrors(final Browser br, final Account account, final DownloadLink link) throws PluginException, InterruptedException {
        final String status = PluginJSonUtils.getJson(br, "error");
        final String errorStr = PluginJSonUtils.getJson(br, "message");
        if (status != null && !"0".equals(status)) {
            if (errorStr != null) {
                if (errorStr.equalsIgnoreCase("Bad token (expired, invalid)")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
            }
            mhm.handleErrorGeneric(account, link, "generic_api_error", 50, 5 * 60 * 1000l);
        }
    }

    // public static interface BestdebridComConfigInterface extends UsenetAccountConfigInterface {
    // };
    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new BestdebridAccountFactory(callback);
    }

    public static class BestdebridAccountFactory extends MigPanel implements AccountBuilderInterface {
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

        public BestdebridAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key:"));
            add(new JLink("https://bestdebrid.com/profile.php"));
            add(new JLabel("API Key:"));
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

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}