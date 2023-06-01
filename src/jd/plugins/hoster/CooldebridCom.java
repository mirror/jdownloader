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
import java.util.Locale;
import java.util.Map;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
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

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "cooldebrid.com" }, urls = { "" })
public class CooldebridCom extends PluginForHost {
    private static final String          WEBSITE_BASE = "https://cooldebrid.com";
    private static MultiHosterManagement mhm          = new MultiHosterManagement("cooldebrid.com");
    private static final boolean         resume       = true;
    private static final int             maxchunks    = -10;

    @SuppressWarnings("deprecation")
    public CooldebridCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(WEBSITE_BASE + "/register.html");
    }

    @Override
    public String getAGBLink() {
        return WEBSITE_BASE + "/tos";
    }

    private Browser prepBR(final Browser br) {
        br.setCookiesExclusive(true);
        br.setFollowRedirects(true);
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
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
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.MULTIHOST };
    }

    @Override
    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        mhm.runCheck(account, link);
        this.login(account, false);
        if (!attemptStoredDownloadurlDownload(link)) {
            final Form dlform = new Form();
            dlform.setMethod(MethodType.POST);
            dlform.setAction(WEBSITE_BASE + "/api/admin/generate.php");
            dlform.put("link", Encoding.urlEncode(link.getDefaultPlugin().buildExternalDownloadURL(link, this)));
            dlform.put("res", "");
            final Browser brc = br.cloneBrowser();
            this.setAjaxHeaders(brc);
            brc.getHeaders().put("Referer", WEBSITE_BASE + "/generate.html");
            brc.submitForm(dlform);
            final Map<String, Object> root = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
            /* TODO: Add/improve errorhandling */
            final String dllink = (String) root.get("dl_link");
            if (StringUtils.isEmpty(dllink)) {
                final String msg = (String) root.get("msg");
                if (msg != null) {
                    /*
                     * E.g. {"status":"error","msg":"Could Not Be Generate Link Please Try Again Later ..."}
                     */
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, msg, 1 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, null, 1 * 60 * 1000l);
                }
            }
            link.setProperty(this.getHost() + "directlink", dllink);
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resume, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                mhm.handleErrorGeneric(account, link, "Unknown download error", 50, 5 * 60 * 1000l);
            }
        }
        this.dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link) throws Exception {
        final String directurlproperty = this.getHost() + "directlink";
        final String url = link.getStringProperty(directurlproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        boolean valid = false;
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resume, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                valid = true;
                return true;
            } else {
                link.removeProperty(directurlproperty);
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            return false;
        } finally {
            if (!valid) {
                try {
                    dl.getConnection().disconnect();
                } catch (Throwable ignore) {
                }
                dl = null;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        if (!br.getURL().contains("/generate.html")) {
            br.getPage("/generate.html");
        }
        final String accountType = br.getRegex("(?i)(Free User|Premium User)\\s*</span>").getMatch(0);
        if (accountType == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final Regex usedTrafficRegex = br.getRegex("id=\"used_bw\">([^<]+)</span>\\s*/\\s*([^<]+)");
        if (!usedTrafficRegex.matches()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        ai.setTrafficMax(SizeFormatter.getSize(usedTrafficRegex.getMatch(1)));
        ai.setTrafficLeft(ai.getTrafficMax() - SizeFormatter.getSize(usedTrafficRegex.getMatch(0)));
        final Regex usedLinksRegex = br.getRegex("(?i)id=\"used_links\">(\\d+)</span>\\s*/\\s*(\\d+)\\s*links");
        if (!usedLinksRegex.matches()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final int linksPerDayUsed = Integer.parseInt(usedLinksRegex.getMatch(0));
        final int linksPerDayMax = Integer.parseInt(usedLinksRegex.getMatch(1));
        final int linksPerDayLeft = linksPerDayMax - linksPerDayUsed;
        if (linksPerDayLeft <= 0) {
            logger.info("Setting zero traffic left because max daily links limit has been reached");
            ai.setTrafficLeft(0);
        }
        ai.setStatus(accountType + " | " + "Daily links left: " + linksPerDayLeft + "/" + linksPerDayMax);
        if (accountType.equalsIgnoreCase("Premium User")) {
            account.setType(AccountType.PREMIUM);
            final String daysLeft = br.getRegex("(?i)(\\d+)\\s*Days\\s*Left\\s*<").getMatch(0);
            if (daysLeft == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            ai.setValidUntil(System.currentTimeMillis() + Long.parseLong(daysLeft) * 24 * 60 * 60 * 1000l, this.br);
        } else {
            /*
             * 2022-02-22: Website claims to also support some hosts for free users but when this plugin was developed they did not have a
             * single free host on their list.
             */
            // account.setType(AccountType.FREE);
            ai.setExpired(true);
        }
        // br.getPage("/host-status.html");
        final ArrayList<String> supportedHosts = new ArrayList<String>();
        final String[] htmls = br.getRegex("<tr>(.*?)</tr>").getColumn(0);
        for (final String html : htmls) {
            final String domain = new Regex(html, "favicons\\?domain=([^\"]+)").getMatch(0);
            if (domain == null) {
                /* Skip invalid items */
                continue;
            }
            final String[] columns = new Regex(html, "<td(.*?)</td>").getColumn(0);
            if (columns.length != 3) {
                logger.warning("Skipping row because of column length mismatch --> @developer! Check plugin code!");
                continue;
            }
            /* Skip hosts that are marked as broken/offline by this multihost */
            final String hostStatusColumn = columns[2].toLowerCase(Locale.ENGLISH);
            final boolean isHostAvailable = hostStatusColumn.contains("online") || hostStatusColumn.contains("unstable");
            if (!isHostAvailable) {
                logger.info("Skipping currently unsupported host: " + domain);
                continue;
            }
            /*
             * Skip hosts if individual limits have been reached. Some have "unlimited" links or bandwidth -> Limit-RegEx will fail for them
             * which automatically makes them pass rthis check.
             */
            final String hostLimitsHTML = columns[1];
            final Regex maxLinkLimitRegex = new Regex(hostLimitsHTML, "used_count=\"[^\"]+\">(\\d+)</span>\\s*/\\s*(\\d+)\\s*link\\s*</p>");
            if (maxLinkLimitRegex.matches()) {
                final int linksUsed = Integer.parseInt(maxLinkLimitRegex.getMatch(0));
                final int linksMax = Integer.parseInt(maxLinkLimitRegex.getMatch(1));
                final int linksLeft = linksMax - linksUsed;
                if (linksLeft <= 0) {
                    logger.info("Skipping host because user reached individual max links limit for it: " + domain + " | " + linksUsed + "/" + linksMax);
                    continue;
                }
            }
            final Regex maxQuotaRegex = new Regex(hostLimitsHTML, "used_mb=\"[^\"]+\">(\\d+(?:\\.\\d{1,2})? [A-Za-z]{1,5})</span>\\s*/\\s*(\\d+(?:\\.\\d{1,2})? [A-Za-z]{1,5})\\s*<br>");
            if (maxQuotaRegex.matches()) {
                final long trafficUsed = SizeFormatter.getSize(maxQuotaRegex.getMatch(0));
                final long trafficMax = SizeFormatter.getSize(maxQuotaRegex.getMatch(1));
                final long trafficLeft = trafficMax - trafficUsed;
                if (trafficLeft <= 0) {
                    logger.info("Skipping host because user reached individual traffic limit for it: " + domain + " | " + maxQuotaRegex.getMatch(0) + "/" + maxQuotaRegex.getMatch(1));
                    continue;
                }
            }
            supportedHosts.add(domain);
        }
        ai.setMultiHostSupport(this, supportedHosts);
        account.setConcurrentUsePossible(true);
        return ai;
    }

    private void setAjaxHeaders(final Browser br) {
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    }

    private void login(final Account account, final boolean validateLogins) throws Exception {
        synchronized (account) {
            try {
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Trying to re-use cookies");
                    br.setCookies(cookies);
                    if (!validateLogins) {
                        /* Trust cookies without checking. */
                        return;
                    }
                    br.getPage(WEBSITE_BASE + "/generate.html");
                    if (this.isLoggedinHTML(this.br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(br.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(br.getHost());
                    }
                }
                logger.info("Performing full login");
                br.getPage(WEBSITE_BASE);
                final Form loginform = br.getFormbyProperty("id", "login_form");
                if (loginform == null) {
                    logger.warning("Failed to find loginform");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.setAction("/api/login.php");
                loginform.setMethod(MethodType.POST);
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("userpass", Encoding.urlEncode(account.getPass()));
                final String captcha = this.getCaptchaCode(WEBSITE_BASE + "/api/antibot/index.php", new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true));
                loginform.put("antibot", Encoding.urlEncode(captcha));
                final Browser brc = br.cloneBrowser();
                setAjaxHeaders(brc);
                brc.submitForm(loginform);
                /* We expect a json response */
                final Map<String, Object> root = JSonStorage.restoreFromString(brc.toString(), TypeRef.HASHMAP);
                if (root.get("status").toString().equalsIgnoreCase("error")) {
                    /*
                     * Usually e.g. {"status":"error","msg":"Security Code Incorrect"} or
                     * {"status":"error","msg":"Username Or Password Is Incorrect"}
                     */
                    final String msg = (String) root.get("msg");
                    if (!StringUtils.isEmpty(msg)) {
                        if (msg.equalsIgnoreCase("Security Code Incorrect")) {
                            /* Invalid login captcha */
                            throw new AccountUnavailableException(msg, 1 * 60 * 1000l);
                        } else {
                            throw new AccountInvalidException(msg);
                        }
                    } else {
                        throw new AccountInvalidException();
                    }
                }
                /*
                 * {"status":"ok","msg":"Login Successful.."} --> Returns cookie user_lang, userid and userpw (some hash, always the same
                 * per user [dangerous])
                 */
                account.saveCookies(br.getCookies(br.getHost()), "");
            } catch (PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedinHTML(final Browser br) {
        if (br.containsHTML("href=\"javascript:logout\\(\\)")) {
            return true;
        } else {
            return false;
        }
    }

    private void checkErrors(final DownloadLink link, final Account account) throws PluginException, InterruptedException {
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}