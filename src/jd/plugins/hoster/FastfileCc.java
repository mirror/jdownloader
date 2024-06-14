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

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.plugins.components.XFileSharingProBasic;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class FastfileCc extends XFileSharingProBasic {
    public FastfileCc(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(super.getPurchasePremiumURL());
    }

    /**
     * DEV NOTES XfileSharingProBasic Version SEE SUPER-CLASS<br />
     * mods: See overridden functions<br />
     * limit-info: 2020-11-27: Premium untested, set FREE limits | Free max. simultaneous downloads slot increase after every download e.g.
     * Start download1, wait 5 minutes (reconnect-waittime) -> Start Download2 and so on<br />
     * captchatype-info: 2020-11-27: reCaptchaV2 <br />
     * other:<br />
     */
    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "fastfile.cc" });
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
        return XFileSharingProBasic.buildAnnotationUrls(getPluginDomains());
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return false;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return false;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    @Override
    public int getMaxChunks(final Account account) {
        if (account != null && account.getType() == AccountType.FREE) {
            /* Free Account */
            return 1;
        } else if (account != null && account.getType() == AccountType.PREMIUM) {
            /* Premium account */
            return 1;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public int getMaxSimultaneousFreeAnonymousDownloads() {
        return 20;
    }

    @Override
    public int getMaxSimultaneousFreeAccountDownloads() {
        return 20;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 20;
    }

    @Override
    protected boolean supportsAPIMassLinkcheck() {
        return looksLikeValidAPIKey(this.getAPIKey());
    }

    @Override
    protected boolean supportsAPISingleLinkcheck() {
        return looksLikeValidAPIKey(this.getAPIKey());
    }

    @Override
    protected String regExTrafficLeft(final Browser br) {
        String betterTrafficLeft = br.getRegex("Traffic available today:\\s*<strong>\\s*?(\\d+[^<]+)</strong>").getMatch(0);
        if (betterTrafficLeft == null) {
            betterTrafficLeft = br.getRegex(">Traffic available today</div>\\s*<div[^>]*>\\s*?(\\d+[^<]+)</div>").getMatch(0);
        }
        if (betterTrafficLeft != null) {
            return betterTrafficLeft;
        } else {
            return super.regExTrafficLeft(br);
        }
    }

    /* 2020-05-29: Just a test */
    // @Override
    // protected boolean enable_account_api_only_mode() {
    // return DebugMode.TRUE_IN_IDE_ELSE_FALSE;
    // }
    @Override
    protected String getDllink(final DownloadLink link, final Account account, final Browser br, String src) {
        String dllink = super.getDllink(link, account, br, src);
        if (dllink != null) {
            /* 2021-03-18: Workaround for missing browser auto https upgrade: https://svn.jdownloader.org/issues/89679 */
            return dllink.replaceFirst("(?i)http://", "https://");
        } else {
            return null;
        }
    }

    @Override
    public boolean loginWebsite(final DownloadLink downloadLink, final Account account, final boolean validateCookies) throws Exception {
        try {
            return super.loginWebsite(downloadLink, account, validateCookies);
        } catch (final PluginException e) {
            if (br.containsHTML("n order to protect your privacy, your account has been blocked")) {
                throw new AccountUnavailableException("Your account has been blocked temporarily", 30 * 60 * 1000);
            }
            Form twoFAForm = null;
            final String formKey2FA = "code";
            final Form[] forms = br.getForms();
            for (final Form form : forms) {
                final InputField twoFAField = form.getInputField(formKey2FA);
                if (twoFAField != null) {
                    twoFAForm = form;
                    break;
                }
            }
            if (twoFAForm == null) {
                /* Login failed, not due the need of 2FA login */
                throw e;
            }
            logger.info("2FA code required");
            final String twoFACode = this.getTwoFACode(account, "\\d{6}");
            logger.info("Submitting 2FA code");
            twoFAForm.put(formKey2FA, twoFACode);
            this.submitForm(twoFAForm);
            if (!this.br.getURL().contains("?op=my_account")) {
                throw new AccountInvalidException(org.jdownloader.gui.translate._GUI.T.jd_gui_swing_components_AccountDialog_2FA_login_invalid());
            }
            account.saveCookies(br.getCookies(br.getHost()), "");
            return true;
        }
    }

    @Override
    protected void checkErrors(final Browser br, final String html, final DownloadLink link, final Account account, final boolean checkAll) throws NumberFormatException, PluginException {
        super.checkErrors(br, html, link, account, checkAll);
        /* 2021-04-08 */
        if (new Regex(html, ">\\s*Your IP was banned by administrator").matches()) {
            if (account != null) {
                throw new AccountUnavailableException("Your IP was banned by administrator", 6 * 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Your IP was banned by administrator");
            }
        }
    }

    @Override
    public String[] scanInfo(String html, final String[] fileInfo) {
        final String strippedHtml = html.replaceAll("(?s)(<div\\s*class\\s*=\\s*\"UserHead\".*?</div>)", "");
        return super.scanInfo(strippedHtml, fileInfo);
    }
}