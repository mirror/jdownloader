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

import jd.PluginWrapper;
import jd.controlling.AccountController;
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
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "naughtymachinima.com" }, urls = { "https?://(?:www\\.)?naughtymachinima\\.com/video/(\\d+)(/([a-z0-9\\-_]+))?" })
public class NaughtymachinimaCom extends PluginForHost {
    public NaughtymachinimaCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.naughtymachinima.com/signup");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    /* DEV NOTES */
    // Tags: porn plugin
    // protocol: no https
    // other:
    private String  dllink         = null;
    private boolean privatecontent = false;

    @Override
    public String getAGBLink() {
        return "https://www.naughtymachinima.com/static/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = getFID(link);
        if (linkid != null) {
            return this.getHost() + "://" + linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return true;
    }

    public int getMaxChunks(final DownloadLink link, final Account account) {
        return 1;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return requestFileInformation(link, account, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        dllink = null;
        privatecontent = false;
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (account != null) {
            this.login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher().replaceFirst("^(?i)http://", "https://"));
        if (br.getHttpConnection().getResponseCode() == 404 || !br.getURL().contains("/" + getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        privatecontent = br.containsHTML("(?i)>\\s*This is a private video");
        final String urlName = new Regex(this.br.getURL(), this.getSupportedLinks()).getMatch(2);
        String filename;
        if (urlName != null) {
            /* Prefer URL name over name Regexed in html as chances are higher to reliably get it there. */
            filename = urlName.replace("-", " ");
        } else {
            filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]+)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>([^<>\"]+) \\- Naughty Machinima</title>").getMatch(0);
            }
        }
        /* Find highest quality */
        final String videos[] = br.getRegex("src\\s*=\\s*\"([^\"]+/media/videos/[^\"]+" + getFID(link) + "[^\"]*\\.mp4)").getColumn(0);
        if (videos != null) {
            int size = -1;
            for (final String video : videos) {
                String resolution = new Regex(video, "_(\\d+)p\\.mp4").getMatch(0);
                if (resolution == null && StringUtils.containsIgnoreCase(video, "/hd/")) {
                    resolution = "720";
                }
                if (resolution == null && StringUtils.containsIgnoreCase(video, "/iphone/")) {
                    resolution = "360";
                }
                if (size == -1 || resolution == null || Integer.parseInt(resolution) > size) {
                    size = resolution != null ? Integer.parseInt(resolution) : -1;
                    dllink = video;
                }
            }
        }
        filename = Encoding.htmlDecode(filename).trim();
        final String ext = ".mp4";
        if (!StringUtils.isEmpty(dllink)) {
            filename = applyFilenameExtension(filename, ext);
            link.setName(filename);
            if (!isDownload) {
                basicLinkCheck(br.cloneBrowser(), br.createHeadRequest(dllink), link, filename, ext);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (privatecontent) {
            if (account != null) {
                throw new PluginException(LinkStatus.ERROR_FATAL, "Private video");
            } else {
                throw new AccountRequiredException();
            }
        } else if (StringUtils.isEmpty(dllink)) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, this.isResumeable(link, account), this.getMaxChunks(link, account));
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            br.setFollowRedirects(true);
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                logger.info("Attempting cookie login");
                this.br.setCookies(this.getHost(), cookies);
                if (!force) {
                    /* Do not validate cookies */
                    return false;
                }
                br.getPage("https://" + this.getHost() + "/");
                if (this.isLoggedin(this.br)) {
                    logger.info("Cookie login successful");
                    /* Refresh cookie timestamp */
                    account.saveCookies(br.getCookies(this.getHost()), "");
                    return true;
                } else {
                    logger.info("Cookie login failed");
                    br.clearCookies(null);
                    account.clearCookies("");
                }
            }
            logger.info("Performing full login");
            br.getPage("https://" + this.getHost() + "/login");
            final Form loginform = br.getFormbyProperty("name", "login_form");
            if (loginform == null) {
                logger.warning("Failed to find loginform");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            loginform.put("username", Encoding.urlEncode(account.getUser()));
            loginform.put("password", Encoding.urlEncode(account.getPass()));
            loginform.put("login_remember", "on");
            br.submitForm(loginform);
            if (!isLoggedin(this.br)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            account.saveCookies(this.br.getCookies(this.getHost()), "");
            return true;
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("/logout");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        /*
         * 2021-06-17: They only got free accounts and the only benefit of those is that users can download private videos if the owner of
         * those has added them to the friends list.
         */
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final Account acc) {
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
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
