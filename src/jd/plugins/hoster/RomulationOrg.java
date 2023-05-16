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

import java.io.IOException;
import java.util.Locale;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "romulation.org" }, urls = { "https?://(?:www\\.)?romulation\\.(?:net|org)/rom/([^/]+/[^/]+)" })
public class RomulationOrg extends PluginForHost {
    public RomulationOrg(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.romulation.net/user/sign-up");
    }

    @Override
    public String getAGBLink() {
        return "https://www.romulation.net/privacy-policy";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 1;
    private static final int     FREE_MAXDOWNLOADS            = 1;
    private static final boolean ACCOUNT_FREE_RESUME          = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS       = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS    = 1;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.canHandle(br.getURL())) {
            /* Redirect to unknown page */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("Full Name</strong></td>[\t\n\r ]*?<td>([^<>\"]+)</td>").getMatch(0);
        String filesize = br.getRegex("Filesize</strong></td>[\t\n\r ]*?<td>([^<>\"]+)</td>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        } else {
            link.setName(new Regex(link.getPluginPatternMatcher(), "/([^/]+)$").getMatch(0) + ".7z");
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (attemptStoredDownloadurlDownload(link, directlinkproperty, resumable, maxchunks)) {
            logger.info("Using previously generated downloadurl");
        } else {
            logger.info("Generating fresh directurls");
            requestFileInformation(link);
            String dllink = br.getRegex("lass=\"game\\-header_download\">[\t\n\r ]*?<a href=\"(http[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("\"(/roms/download/[^/]+/[^<>\"]+)\"").getMatch(0);
            }
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getPage(dllink);
            /* Downloadable via free account */
            final boolean tooBigForGuests = br.containsHTML("(?i)File too big for guests");
            /* Premium required */
            final boolean premiumonly = br.containsHTML("(?i)Sorry, this game is restricted");
            if (tooBigForGuests || premiumonly) {
                throw new AccountRequiredException();
            }
            if (br.containsHTML("(?i)>\\s*Too many active connections")) {
                /* If the user is using only JD for downloading, this should never happen. */
                /*
                 * <li>Too many active connections. <a
                 * href="/buy/premium?utm_source=romulation&utm_medium=website&utm_campaign=member-download-connections">Upgrade to
                 * Premium</a> to download instantly or wait for your current downloads to finish.</li>
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait until you can start more downloads");
            }
            dllink = br.getRegex("\"(https?://[^/]+/files/[^/]+/[^<>\"]+)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        }
        dl.startDownload();
    }

    private boolean attemptStoredDownloadurlDownload(final DownloadLink link, final String directlinkproperty, final boolean resumes, final int maxchunks) throws Exception {
        final String url = link.getStringProperty(directlinkproperty);
        if (StringUtils.isEmpty(url)) {
            return false;
        }
        try {
            final Browser brc = br.cloneBrowser();
            dl = new jd.plugins.BrowserAdapter().openDownload(brc, link, url, resumes, maxchunks);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                return true;
            } else {
                brc.followConnection(true);
                throw new IOException();
            }
        } catch (final Throwable e) {
            logger.log(e);
            try {
                dl.getConnection().disconnect();
            } catch (Throwable ignore) {
            }
            link.removeProperty(directlinkproperty);
            return false;
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public static AccountInfo fetchAccountInfo(final Plugin plugin, Browser br, final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        RomulationOrg.login(plugin, br, account, true);
        if (!br.getURL().contains("/user")) {
            br.getPage("/user");
        }
        ai.setUnlimitedTraffic();
        if (br.containsHTML("User class: Premium")) {
            // <li>User class: Premium</li>
            // <li>Premium status: active
            // <li>Premium renews at: 2023-02-27 UTC</li>
            final String renew = br.getRegex("Premium renews at: (\\d+-\\d+-\\d+)").getMatch(0);
            final String active = br.getRegex("Premium status:\\s*(active)").getMatch(0);
            if (active != null) {
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
                account.setConcurrentUsePossible(true);
                if (renew != null) {
                    ai.setValidUntil(TimeFormatter.getMilliSeconds(renew, "yyyy'-'MM'-'dd", Locale.ENGLISH));
                }
                if (!ai.isExpired()) {
                    return ai;
                }
            }
        }
        if (br.containsHTML("User class: Regular Member")) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        } else {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
        }
        return ai;
    }

    public static void login(Plugin plugin, Browser br, final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(plugin.getHost(), cookies);
                    if (!force) {
                        /* Trust cookies without check */
                        return;
                    } else {
                        plugin.getLogger().info("Checking cookies...");
                        br.getPage("https://www." + plugin.getHost() + "/");
                        if (isLoggedIn(br)) {
                            plugin.getLogger().info("Cookie login successful");
                            account.saveCookies(br.getCookies(plugin.getHost()), "");
                            return;
                        } else {
                            plugin.getLogger().info("Cookie login failed");
                            br.clearCookies(br.getHost());
                        }
                    }
                }
                plugin.getLogger().info("Performing full login");
                br.getPage("https://www." + account.getHoster() + "/user/login");
                final Form loginform = br.getFormbyActionRegex(".*/user/login");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                loginform.put("remember", "1");
                br.submitForm(loginform);
                if (!isLoggedIn(br)) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(plugin.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private static boolean isLoggedIn(final Browser br) {
        if (br.containsHTML("/user/logout")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        return fetchAccountInfo(this, br, account);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(this, br, account, false);
        handleDownload(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2019-01-29: No captchas at all */
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}