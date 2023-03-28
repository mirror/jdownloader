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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import jd.PluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.MultiHosterManagement;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.scripting.JavaScriptEngineFactory;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cocoleech.com" }, urls = { "" })
public class CocoleechCom extends PluginForHost {
    private static final String          API_ENDPOINT        = "https://members.cocoleech.com/auth/api";
    /* Last updated: 2017-02-08 according to admin request. */
    private static final int             defaultMAXDOWNLOADS = 20;
    private static final int             defaultMAXCHUNKS    = -4;
    private static final boolean         defaultRESUME       = true;
    // private final String apikey = "cdb5efc9c72196c1bd8b7a594b46b44f";
    private static final String          PROPERTY_DIRECTURL  = "cocoleechcom_directlink";
    private static final String          PROPERTY_MAXCHUNKS  = "cocoleechcom_maxchunks";
    private static MultiHosterManagement mhm                 = new MultiHosterManagement("cocoleech.com");

    @SuppressWarnings("deprecation")
    public CocoleechCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://members.cocoleech.com/");
        this.setStartIntervall(3000l);
    }

    @Override
    public String getAGBLink() {
        return "https://members.cocoleech.com/terms";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException, IOException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            /* without account its not possible to download the link */
            return false;
        } else {
            return true;
        }
    }

    @Override
    /** This should never get called. */
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    /** This should never get called. */
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String url = link.getStringProperty(PROPERTY_DIRECTURL);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, defaultRESUME, link.getIntegerProperty(PROPERTY_MAXCHUNKS, defaultMAXCHUNKS));
            dl.setFilenameFix(true);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            link.removeProperty(PROPERTY_DIRECTURL);
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            return false;
        }
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        prepBR(this.br);
        mhm.runCheck(account, link);
        if (!attemptStoredDownloadurlDownload(link)) {
            br.setFollowRedirects(true);
            /* Request creation of downloadlink */
            this.br.getPage(API_ENDPOINT + "?key=" + Encoding.urlEncode(account.getPass()) + "&link=" + Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            handleAPIErrors(this.br, account, link);
            final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
            String maxchunksStr = null;
            final Object chunksO = entries.get("chunks");
            if (chunksO != null) {
                maxchunksStr = chunksO.toString();
            }
            final String dllink = (String) entries.get("download");
            if (StringUtils.isEmpty(dllink)) {
                mhm.handleErrorGeneric(account, link, "Failed to find final downloadurl", 50, 5 * 60 * 1000l);
            }
            int maxChunks = defaultMAXCHUNKS;
            if (!StringUtils.isEmpty(maxchunksStr) && maxchunksStr.matches("^\\d+$")) {
                maxChunks = -Integer.parseInt(maxchunksStr);
                link.setProperty(PROPERTY_MAXCHUNKS, maxChunks);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, defaultRESUME, maxChunks);
            dl.setFilenameFix(true);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (dl.getConnection().getContentType().contains("json")) {
                    handleAPIErrors(this.br, account, link);
                }
                mhm.handleErrorGeneric(account, link, "Unknown download error", 50, 5 * 60 * 1000l);
            }
            link.setProperty(PROPERTY_DIRECTURL, dllink);
        }
        this.dl.startDownload();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        prepBR(this.br);
        final AccountInfo ai = new AccountInfo();
        login(account);
        Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        /*
         * 2021-11-29: Users enter API key only from now on --> Try to find username in API answer and set it so accounts in JD still have
         * unique username strings!
         */
        final String username = (String) entries.get("username");
        if (!StringUtils.isEmpty(username)) {
            account.setUser(username);
        }
        final String accounttype = (String) entries.get("type");
        final String trafficleft = (String) entries.get("traffic_left");
        final String validuntil = (String) entries.get("expire_date");
        long timestampValiduntil = 0;
        if (validuntil != null) {
            timestampValiduntil = TimeFormatter.getMilliSeconds(validuntil, "yyyy-MM-dd HH:mm:ss", Locale.ENGLISH);
        }
        if ("premium".equalsIgnoreCase(accounttype) && timestampValiduntil > System.currentTimeMillis()) {
            ai.setValidUntil(timestampValiduntil);
            account.setType(AccountType.PREMIUM);
            account.setConcurrentUsePossible(true);
            /*
             * 2017-02-08: Accounts do usually not have general traffic limits - however there are individual host traffic limits see
             * mainpage (when logged in) --> Right side "Daily Limit(s)"
             */
            if (StringUtils.equalsIgnoreCase(trafficleft, "unlimited")) {
                ai.setUnlimitedTraffic();
            } else {
                ai.setTrafficLeft(Long.parseLong(trafficleft));
            }
        } else {
            account.setType(AccountType.FREE);
            /*
             * 2016-05-05: According to admin, free accounts cannot download anything.
             */
            account.setConcurrentUsePossible(false);
            account.setMaxSimultanDownloads(1);
            ai.setTrafficLeft(0);
        }
        /* Overwrite previously set status in case an account package-name is available */
        final String accountPackage = (String) entries.get("package"); // E.g. "1 Month Premium" or "No Package" for free accounts
        if (!StringUtils.isEmpty(accountPackage)) {
            ai.setStatus(accountPackage);
        }
        this.br.getPage(API_ENDPOINT + "/hosts-status");
        ArrayList<String> supportedhostslist = new ArrayList();
        entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        final List<Map<String, Object>> hosters = (List<Map<String, Object>>) entries.get("result");
        for (final Map<String, Object> hostinfo : hosters) {
            String host = (String) hostinfo.get("host");
            final String status = (String) hostinfo.get("status");
            if (StringUtils.isEmpty(host)) {
                /* Skip invalid items */
                continue;
            }
            if ("online".equalsIgnoreCase(status)) {
                supportedhostslist.add(host);
            } else {
                logger.info("Not adding currently unsupported host: " + host);
            }
        }
        ai.setMultiHostSupport(this, supportedhostslist);
        return ai;
    }

    private void login(final Account account) throws Exception {
        synchronized (account) {
            account.setPass(correctPassword(account.getPass()));
            if (!isAPIKey(account.getPass())) {
                throw new AccountInvalidException("Invalid API key format");
            }
            this.br.getPage(API_ENDPOINT + "/info?key=" + Encoding.urlEncode(account.getPass()));
            /* No error here = account is valid. */
            handleAPIErrors(this.br, account, null);
        }
    }

    private void handleAPIErrors(final Browser br, final Account account, final DownloadLink link) throws Exception {
        final Map<String, Object> entries = JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        /**
         * 2021-09-10: "status" is independent from e.g. {"status":"100","message":"Incorrect log-in or password."} </br>
         * {"status":"100","message":"Your IP is blocked for today. Please contact support."}
         */
        final String statusmsg = (String) entries.get("message");
        if (!StringUtils.isEmpty(statusmsg)) {
            if (statusmsg.equalsIgnoreCase("Incorrect log-in or password.")) {
                throw new AccountInvalidException(statusmsg);
            } else if (statusmsg.equalsIgnoreCase("Incorrect API key.")) {
                String errormsg = statusmsg + "\r\n Find your API Key here: members.cocoleech.com/settings";
                errormsg += "\r\nIf you're using myjdownloader, enter your API Key into both the username and password fields.";
                throw new AccountInvalidException(errormsg);
            } else if (statusmsg.equalsIgnoreCase("Premium membership expired.")) {
                account.getAccountInfo().setExpired(true);
                throw new AccountUnavailableException(statusmsg, 5 * 60 * 1000l);
            } else if (statusmsg.equalsIgnoreCase("Your IP is blocked for today. Please contact support.")) {
                /* Put all account temp. unavailable errors here. */
                throw new AccountUnavailableException(statusmsg, 5 * 60 * 1000l);
            } else {
                /* Unknown error or link based error */
                if (link == null) {
                    throw new AccountUnavailableException(statusmsg, 3 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, statusmsg, 50, 5 * 60 * 1000l);
                }
            }
        }
    }

    @Override
    public int getMaxSimultanDownload(final DownloadLink link, final Account account) {
        if (account != null) {
            return defaultMAXDOWNLOADS;
        } else {
            return 0;
        }
    }

    private static boolean isAPIKey(final String str) {
        if (str == null) {
            return false;
        } else if (str.matches("[a-f0-9]{24}")) {
            return true;
        } else {
            return false;
        }
    }

    private static String correctPassword(final String pw) {
        return pw.trim();
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new CocoleechAccountFactory(callback);
    }

    public static class CocoleechAccountFactory extends MigPanel implements AccountBuilderInterface {
        /**
         *
         */
        private static final long serialVersionUID = 1L;
        private final String      APIKEYHELP       = "Enter your API Key";
        private final JLabel      apikeyLabel;

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else {
                return correctPassword(new String(this.pass.getPassword()));
            }
        }

        public boolean updateAccount(Account input, Account output) {
            if (!StringUtils.equals(input.getUser(), output.getUser())) {
                output.setUser(input.getUser());
                return true;
            } else if (!StringUtils.equals(input.getPass(), output.getPass())) {
                output.setPass(input.getPass());
                return true;
            } else {
                return false;
            }
        }

        private final ExtPasswordField pass;

        public CocoleechAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key:"));
            add(new JLink("https://members.cocoleech.com/settings"));
            add(apikeyLabel = new JLabel("API Key:"));
            add(this.pass = new ExtPasswordField() {
                @Override
                public void onChanged() {
                    callback.onChangedInput(this);
                }
            }, "");
            pass.setHelpText(APIKEYHELP);
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
            final String pw = getPassword();
            if (CocoleechCom.isAPIKey(pw)) {
                apikeyLabel.setForeground(Color.BLACK);
                return true;
            } else {
                apikeyLabel.setForeground(Color.RED);
                return false;
            }
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