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

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.JComponent;
import javax.swing.JLabel;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.utils.StringUtils;
import org.appwork.utils.Time;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.gui.InputChangedCallbackInterface;
import org.jdownloader.plugins.accounts.AccountBuilderInterface;

import jd.PluginWrapper;
import jd.gui.swing.components.linkbutton.JLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FikperCom extends PluginForHost {
    public FikperCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://fikper.com/register");
    }

    @Override
    public String getAGBLink() {
        return "https://fikper.com/terms-of-use";
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fikper.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([A-Za-z0-9]+)/([^/]+)(\\.html)?");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return false;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return 0;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

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
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    private String getFiletitleFromURL(final DownloadLink link) {
        String title = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        final Regex looksLikeTitleWithExtension = new Regex(title, "(.+)([A-Za-z0-9]{3})$");
        if (looksLikeTitleWithExtension.matches()) {
            /* This will work for all file extensions with length == 3. */
            return looksLikeTitleWithExtension.getMatch(0) + "." + looksLikeTitleWithExtension.getMatch(1);
        } else {
            return title;
        }
    }

    private final String API_BASE = "https://sapi.fikper.com/";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        requestFileInformation(link, null);
        return AvailableStatus.TRUE;
    }

    private Map<String, Object> requestFileInformation(final DownloadLink link, final Account account) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(getFiletitleFromURL(link));
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.postPageRaw(API_BASE, "{\"fileHashName\":\"" + this.getFID(link) + "\"}");
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            /* {"code":404,"message":"The file might be deleted."} */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
        link.setFinalFileName(entries.get("name").toString());
        final Object filesize = entries.get("size");
        if (filesize instanceof Number) {
            link.setVerifiedFileSize(((Number) filesize).longValue());
        } else {
            link.setVerifiedFileSize(Long.parseLong(filesize.toString()));
        }
        link.setPasswordProtected(((Boolean) entries.get("password")).booleanValue());
        return entries;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception, PluginException {
        if (attemptStoredDownloadurlDownload(link, account)) {
            logger.info("Re-using previously generated directurl");
        } else {
            String dllink = null;
            if (account != null && account.getType() == AccountType.PREMIUM) {
                this.login(account, false);
                br.getPage(API_BASE + "api/file/download/" + Encoding.urlEncode(this.getFID(link)));
                this.checkErrorsAPI(br, link, account, null);
                dllink = br.toString();
            } else {
                // if (account != null) {
                // this.login(account, false);
                // }
                final Map<String, Object> entries = requestFileInformation(link, account);
                if (link.isPasswordProtected()) {
                    throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected items are not yet supported");
                }
                final Object remainingDelay = entries.get("remainingDelay");
                if (remainingDelay != null) {
                    /* Downloadlimit has been reached */
                    final int limitWaitSeconds;
                    if (remainingDelay instanceof Number) {
                        limitWaitSeconds = ((Number) remainingDelay).intValue();
                    } else {
                        limitWaitSeconds = Integer.parseInt(remainingDelay.toString());
                    }
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, limitWaitSeconds * 1000l);
                }
                final long timeBefore = Time.systemIndependentCurrentJVMTimeMillis();
                final int waitBeforeDownloadMillis = ((Number) entries.get("delayTime")).intValue();
                final boolean skipPreDownloadWaittime = true; // 2022-11-14: Waittime is skippable
                final boolean useHcaptcha = true;
                final Map<String, Object> postdata = new HashMap<String, Object>();
                if (useHcaptcha) {
                    /* 2023-01-16 */
                    final CaptchaHelperHostPluginHCaptcha hCaptcha = getHcaptchaHelper(br);
                    final String hCaptchaResponse = hCaptcha.getToken();
                    postdata.put("recaptcha", hCaptchaResponse);
                } else {
                    /* Old handling */
                    final String recaptchaV2Response = getRecaptchaHelper(br).getToken();
                    postdata.put("recaptcha", recaptchaV2Response);
                }
                final long passedTimeDuringCaptcha = Time.systemIndependentCurrentJVMTimeMillis() - timeBefore;
                postdata.put("fileHashName", this.getFID(link));
                postdata.put("downloadToken", entries.get("downloadToken").toString());
                final long waitBeforeDownloadMillisLeft = waitBeforeDownloadMillis - passedTimeDuringCaptcha;
                if (waitBeforeDownloadMillisLeft > 0 && !skipPreDownloadWaittime) {
                    this.sleep(waitBeforeDownloadMillisLeft, link);
                }
                br.postPageRaw(API_BASE, JSonStorage.serializeToJson(postdata));
                final Map<String, Object> dlresponse = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                dllink = dlresponse.get("directLink").toString();
                final Object filesizeO = dlresponse.get("size");
                if (filesizeO != null) {
                    if (filesizeO instanceof Number) {
                        link.setVerifiedFileSize(((Number) filesizeO).longValue());
                    } else if (filesizeO instanceof String) {
                        final String filesizeStr = filesizeO.toString();
                        if (filesizeStr.matches("\\d+")) {
                            link.setVerifiedFileSize(Long.parseLong(filesizeStr));
                        }
                    }
                }
            }
            if (StringUtils.isEmpty(dllink)) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), getMaxChunks(account));
            /* Save directurl even if it is "invalid" -> E.g. if error 429 happens we might be able to use the same URL later. */
            link.setProperty(getDirectlinkproperty(account), dl.getConnection().getURL().toString());
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                if (dl.getConnection().getResponseCode() == 403) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
                } else if (dl.getConnection().getResponseCode() == 404) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
                }
                checkError429(dl.getConnection());
                try {
                    br.followConnection(true);
                } catch (final IOException e) {
                    logger.log(e);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    private void checkError429(final URLConnectionAdapter con) throws PluginException {
        if (con.getResponseCode() == 429) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Error 429 too many connections", 30 * 1000l);
        }
    }

    private CaptchaHelperHostPluginRecaptchaV2 getRecaptchaHelper(final Browser br) {
        final String host = br.getHost(false);
        return new CaptchaHelperHostPluginRecaptchaV2(this, br, "6Ley0XQeAAAAAK-H0p0T_zeun7NnUgMcLFQy0cU3") {
            @Override
            protected String getSiteUrl() {
                /* We are on subdomain.fikper.com but captcha needs to be solved on fikper.com. */
                return br._getURL().getProtocol() + "://" + host;
            }
        };
    }

    private CaptchaHelperHostPluginHCaptcha getHcaptchaHelper(final Browser br) {
        final String host = br.getHost(false);
        return new CaptchaHelperHostPluginHCaptcha(this, br, "ddd70c6f-e4cb-45e2-9374-171fbc0d4137") {
            @Override
            protected String getSiteUrl() {
                /* We are on subdomain.fikper.com but captcha needs to be solved on fikper.com. */
                return br._getURL().getProtocol() + "://" + host;
            }
        };
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final Account account) throws Exception {
        final String property = getDirectlinkproperty(account);
        final String url = link.getStringProperty(property);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        boolean throwException = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, this.isResumeable(link, account), this.getMaxChunks(account));
            try {
                checkError429(dl.getConnection());
            } catch (final PluginException error429) {
                /* URL is valid but we can't use it right now. */
                valid = true;
                throwException = true;
                throw error429;
            }
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Exception e) {
            if (throwException) {
                throw e;
            } else {
                logger.log(e);
                return false;
            }
        } finally {
            if (!valid) {
                link.removeProperty(property);
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                this.dl = null;
            }
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private Map<String, Object> login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                account.setPass(correctPassword(account.getPass()));
                if (!isApiKey(account.getPass())) {
                    throw new AccountInvalidException("Invalid API key format! Find your API key here: fikper.com/settings/api\r\nHeadless users: Enter that API key into username and password field.");
                }
                prepareBrowserAPI(br, account);
                if (!force) {
                    return null;
                }
                br.getPage(API_BASE + "api/account");
                final Map<String, Object> entries = restoreFromString(br.getRequest().getHtmlCode(), TypeRef.MAP);
                checkErrorsAPI(br, null, account, entries);
                return entries;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private void checkErrorsAPI(final Browser br, final DownloadLink link, final Account account, final Map<String, Object> map) throws PluginException {
        Number code = br.getHttpConnection().getResponseCode();
        String message = "Unknown error";
        if (map != null) {
            code = (Number) map.get("code");
            message = (String) map.get("message");
        }
        if (code == null || StringUtils.isEmpty(message)) {
            /* No error */
            return;
        }
        switch (code.intValue()) {
        case 200:
            /* No error */
            return;
        case 401:
            throw new AccountInvalidException(message);
        case 404:
            if (link != null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        default:
            throw new AccountUnavailableException(message, 1 * 60 * 1000l);
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        final Map<String, Object> user = login(account, true);
        /* User can enter whatever he wants into username field -> Correct that as we want to have unique usernames. */
        account.setUser(user.get("email").toString());
        ai.setUsedSpace(((Number) user.get("usedSpace")).longValue());
        final long trafficUsed = ((Number) user.get("usedBandwidth")).longValue();
        final long traffixMax = ((Number) user.get("totalBandwidth")).longValue();
        ai.setTrafficLeft(traffixMax - trafficUsed);
        ai.setTrafficMax(traffixMax);
        final String expireDate = (String) user.get("premiumExpire");
        if (StringUtils.isEmpty(expireDate)) {
            ai.setExpired(true);
            return ai;
        }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expireDate, "yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.ENGLISH));
        account.setType(AccountType.PREMIUM);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public String getDirectlinkproperty(final Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return "free_directurl";
        } else {
            return "account_ " + acc.getType() + "_directurl";
        }
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (acc.getType() == AccountType.FREE) {
            /* Free accounts can have captchas */
            return true;
        } else {
            /* Premium accounts do not have captchas */
            return false;
        }
    }

    @Override
    public AccountBuilderInterface getAccountFactory(InputChangedCallbackInterface callback) {
        return new FikperAccountFactory(callback);
    }

    public static Browser prepareBrowserAPI(final Browser br, final Account account) throws Exception {
        if (br == null) {
            return null;
        }
        br.getHeaders().put("User-Agent", "JDownloader");
        br.getHeaders().put("x-api-key", account.getPass());
        return br;
    }

    public static class FikperAccountFactory extends MigPanel implements AccountBuilderInterface {
        private static final long serialVersionUID = 1L;
        private final String      PINHELP          = "Enter your API Key";

        private String getPassword() {
            if (this.pass == null) {
                return null;
            } else {
                final String pw = new String(this.pass.getPassword()).trim();
                if (EMPTYPW.equals(pw)) {
                    return null;
                } else {
                    return correctPassword(pw);
                }
            }
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
        private static String          EMPTYPW = " ";
        private final JLabel           idLabel;

        public FikperAccountFactory(final InputChangedCallbackInterface callback) {
            super("ins 0, wrap 2", "[][grow,fill]", "");
            add(new JLabel("Click here to find your API Key"));
            add(new JLink("https://fikper.com/settings/api"));
            this.add(this.idLabel = new JLabel("Enter your API Key:"));
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
            final String password = getPassword();
            if (!isApiKey(password)) {
                idLabel.setForeground(Color.RED);
                return false;
            } else {
                idLabel.setForeground(Color.BLACK);
                return getPassword() != null;
            }
        }

        @Override
        public Account getAccount() {
            return new Account(null, getPassword());
        }
    }

    private static boolean isApiKey(final String str) {
        return str != null && str.matches("[A-Za-z0-9]{20,}");
    }

    private static String correctPassword(final String pw) {
        if (pw == null) {
            return null;
        } else {
            return pw.trim();
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}