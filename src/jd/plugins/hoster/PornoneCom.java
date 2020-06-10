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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pornone.com" }, urls = { "https?://(?:www\\.)?(?:vporn|pornone)\\.com/.*?/(\\d+)/?" })
public class PornoneCom extends PluginForHost {
    @SuppressWarnings("deprecation")
    public PornoneCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pornone.com/register/");
    }

    @Override
    public String rewriteHost(String host) {
        /* 2020-06-04: vpon.com is now pornone.com - existing vporn accounts are also working via pornone.com. */
        if (host == null || host.equalsIgnoreCase("vporn.com")) {
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    private String dllink = null;

    @Override
    public String getAGBLink() {
        return "https://pornone.com/terms/";
    }

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        if (link.getDownloadURL().contains("/embed/")) {
            link.setUrlDownload("https://" + this.getHost() + "/mature/x/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0));
        }
        link.setUrlDownload(link.getDownloadURL() + "/");
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

    @Override
    public String getMirrorID(DownloadLink link) {
        if (link != null && StringUtils.equals(getHost(), link.getHost())) {
            return getHost() + "://" + getFID(link);
        } else {
            return super.getMirrorID(link);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            logger.info("Account available");
            try {
                this.login(aa, false);
            } catch (final Throwable e) {
                logger.warning("Login failed:");
                e.printStackTrace();
            }
        }
        final String fid = this.getFID(link);
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (!br.getURL().contains(fid) || br.containsHTML("This video (is|has been) deleted|>404 not found") || br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("videoname = '([^']*?)'").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>\"]*?) - (Vporn Video|vPorn.com)</title>").getMatch(0);
        }
        if (filename == null) {
            /* Fallback */
            filename = fid;
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        filename = Encoding.htmlDecode(filename).trim() + ".mp4";
        link.setFinalFileName(filename);
        br.setFollowRedirects(true);
        int foundlinks = 0;
        boolean failed = true;
        URLConnectionAdapter con = null;
        /* videoUrlHD2 = usually only available via account, downloadUrl = Only available via account also == videoUrlLow(2) */
        final String[] quals = { "videoUrlHD2", "videoUrlMedium2", "videoUrlLow2", "videoUrlHD", "videoUrlMedium", "videoUrlLow", "downloadUrl" };
        for (final String qual : quals) {
            dllink = br.getRegex("flashvars\\." + qual + "\\s*=\\s*\"(https?://[^<>\"]*?)\"").getMatch(0);
            if (dllink != null) {
                foundlinks++;
                dllink = Encoding.htmlDecode(dllink);
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        failed = false;
                        link.setDownloadSize(con.getLongContentLength());
                        break;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        if (foundlinks == 0) { // 20170617
            dllink = br.getRegex("<source src=\"(http[^\"]+)\"").getMatch(0);
            logger.info("dllink: " + dllink);
            if (dllink != null) {
                foundlinks++;
                dllink = Encoding.htmlDecode(dllink);
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html")) {
                        failed = false;
                        link.setDownloadSize(con.getLongContentLength());
                        return AvailableStatus.TRUE;
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (Throwable e) {
                    }
                }
            }
        }
        /* js cars equals "" or just a number --> Video is not even playable via browser */
        if (foundlinks == 0 && br.containsHTML("flashvars\\.videoUrlLow\\s*=\\s*\"\"") || br.containsHTML("<source src=\"\"") || !br.containsHTML("<source src=")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (foundlinks == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* No working downloadlink available --> */
        if (failed) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        doFree(link);
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link) throws Exception {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (dl.getConnection().getResponseCode() == 416) {
            logger.info("Resume failed --> Retrying from zero");
            link.setChunksProgress(null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(cookies);
                    return;
                }
                br.setFollowRedirects(true);
                br.getPage("https://pornone.com/login/");
                Form login = br.getFormbyActionRegex(".*?/login.*?");
                login.put("username", Encoding.urlEncode(account.getUser()));
                login.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(login);
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getURL()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.getCookie(br.getHost(), "ual", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* No captchas can happen */
        account.setConcurrentUsePossible(true);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* We're already logged in! */
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
