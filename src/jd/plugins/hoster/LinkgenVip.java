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

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "linkgen.vip" }, urls = { "" })
public class LinkgenVip extends PluginForHost {
    /* Connection limits */
    private static MultiHosterManagement mhm = new MultiHosterManagement("linkgen.vip");

    public LinkgenVip(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://" + getHost() + "/premium.php");
    }

    @Override
    public String getAGBLink() {
        return "https://" + getHost() + "/legal.php";
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws PluginException {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public boolean canHandle(final DownloadLink link, final Account account) throws Exception {
        if (account == null) {
            return false;
        } else {
            mhm.runCheck(account, link);
            return true;
        }
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
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
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
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        final String directlinkproperty = this.getHost() + "_directlink";
        final String storedDirecturl = link.getStringProperty(directlinkproperty);
        final String dllink;
        if (storedDirecturl != null) {
            logger.info("Attempting to re-use stored directurl: " + storedDirecturl);
            dllink = storedDirecturl;
        } else {
            login(account, true);
            final String url = link.getDefaultPlugin().buildExternalDownloadURL(link, this);
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.setAction("/downloader.php?rand=0." + System.currentTimeMillis());
            dlform.put("urllist", Encoding.urlEncode(url));
            dlform.put("captcha", "none");
            br.submitForm(dlform);
            dllink = br.getRegex("<a href=.(https?://[^<>\"']+)' target=.\\d+").getMatch(0);
            if (StringUtils.isEmpty(dllink)) {
                String errormessage = br.getRegex("<font color=red[^>]*>([^<]+)</font>").getMatch(0);
                if (errormessage != null) {
                    /* E.g. <b> <font color=red face=Arial size=2> Error On Generating. Please Retry Later !!!</font></b><BR> */
                    errormessage = Encoding.htmlDecode(errormessage).trim();
                    mhm.handleErrorGeneric(account, link, errormessage, 20, 5 * 60 * 1000l);
                } else {
                    mhm.handleErrorGeneric(account, link, "Failed to find final downloadlink", 20, 5 * 60 * 1000l);
                }
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 10, 3 * 60 * 1000l);
        }
        try {
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Final downloadurl did not lead to file", 10, 3 * 60 * 1000l);
            }
        } catch (final Exception e) {
            if (storedDirecturl != null) {
                link.removeProperty(directlinkproperty);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Stored directurl expired", e);
            } else {
                throw e;
            }
        }
        if (storedDirecturl == null) {
            link.setProperty(directlinkproperty, dllink);
        }
        this.dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account, true);
        final AccountInfo ai = new AccountInfo();
        final Regex expireRegex = br.getRegex("(\\d+) Days?\\s*(\\d+) Hours?");
        final Regex linksUsedDailyRegex = br.getRegex("Links Today:\\s*<[^>]*>(\\d+)\\s*/\\s*(\\d+)");
        final Regex trafficUsedDailyRegex = br.getRegex("Used Today:\\s*<[^>]*>([^/<]+)/([^<]+)</font>");
        if (!expireRegex.patternFind()) {
            /* No expire time found -> Assume that this is an expired account or it has never been a premium account. */
            ai.setExpired(true);
            return ai;
        } else if (!linksUsedDailyRegex.patternFind()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find daily links limits");
        } else if (!trafficUsedDailyRegex.patternFind()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find daily traffic limits");
        }
        account.setType(AccountType.PREMIUM);
        final int dailyLinksUsed = Integer.parseInt(linksUsedDailyRegex.getMatch(0));
        final int dailyLinksMax = Integer.parseInt(linksUsedDailyRegex.getMatch(1));
        if (dailyLinksUsed >= dailyLinksMax) {
            throw new AccountUnavailableException("Reached max daily links limit", 5 * 60 * 1000);
        }
        final int days = Integer.parseInt(expireRegex.getMatch(0));
        final int hours = Integer.parseInt(expireRegex.getMatch(1));
        final long validMilliseconds = (days * 24 * 60 * 60 * 1000) + (hours * 60 * 1000);
        String statusText = account.getType().getLabel();
        statusText += " | Daily links used: " + dailyLinksUsed + "/" + dailyLinksMax;
        final long trafficUsed = SizeFormatter.getSize(trafficUsedDailyRegex.getMatch(0));
        final long trafficMax = SizeFormatter.getSize(trafficUsedDailyRegex.getMatch(1));
        ai.setTrafficLeft(trafficMax - trafficUsed);
        ai.setTrafficMax(trafficMax);
        ai.setValidUntil(System.currentTimeMillis() + validMilliseconds, br);
        ai.setStatus(statusText);
        br.getPage("/host.php");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] htmls = br.getRegex("<tr class=\"text-center\">(.*?)</tr>").getColumn(0);
        if (htmls == null || htmls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find list of supported hosts");
        }
        for (final String html : htmls) {
            String host = new Regex(html, "fhimg/[^>]*>([^<]+)<").getMatch(0);
            if (host == null) {
                logger.info("Skipping invalid entry: " + html);
                continue;
            }
            host = host.trim();
            // TODO: Find- and set individual host limits
            // final String[] allRows = new Regex(html, "<td>([^<]+)</td>").getColumn(0);
            // final boolean isOnline = new Regex(html, "Online\\s*<").patternFind();
            final boolean isOffline = new Regex(html, "Offline\\s*<").patternFind();
            // final boolean isUnstable = new Regex(html, "Unstable\\s*<").patternFind();
            if (isOffline) {
                logger.info("Skipping offline entry: " + host);
                continue;
            }
            supportedHosts.add(host);
        }
        if (supportedHosts.size() == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Failed to find list of supported hosts");
        }
        ai.setMultiHostSupport(this, supportedHosts);
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            final Cookies cookies = account.loadCookies("");
            final String targetPath = "/downloader.php";
            if (cookies != null) {
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Do not check cookies */
                    return;
                }
                br.getPage("https://" + this.getHost() + targetPath);
                if (isLoggedin(br)) {
                    logger.info("Cookie login successful");
                    account.saveCookies(this.br.getCookies(br.getHost()), "");
                    return;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            br.getPage("https://" + this.getHost() + "/login.php");
            final Form loginform = br.getFormbyProperty("name", "UsernameLoginForm");
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
            loginform.put("g-recaptcha-response", Encoding.urlEncode(recaptchaV2Response));
            br.submitForm(loginform);
            final Form loginform2 = br.getFormbyActionRegex("signin.php");
            if (loginform2 != null) {
                /* Strange 2nd step - "Please Enter Username To Login" */
                logger.info("Performing 2nd login step");
                loginform2.put("secure", Encoding.urlEncode(account.getUser()));
                br.submitForm(loginform2);
            }
            if (!isLoggedin(br)) {
                final String loginErrormessage = br.getRegex("class=\"error\"[^>]*>\\s*<p><b>([^<]+)</b></p>").getMatch(0);
                if (loginErrormessage != null) {
                    throw new AccountInvalidException(Encoding.htmlDecode(loginErrormessage).trim());
                } else {
                    throw new AccountInvalidException();
                }
            }
            br.getPage(targetPath);
            account.saveCookies(br.getCookies(br.getHost()), "");
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("logout.php");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}