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

import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.jdownloader.plugins.controller.LazyPlugin;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class BatoTo extends PluginForHost {
    public BatoTo(PluginWrapper wrapper) {
        super(wrapper);
        /* 2021-06-14: Disabled for now (login is broken and maybe not even required anymore) */
        // this.enablePremium("https://id.bato.to/register");
        setRequestLimits();
        setStartIntervall(250);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    public static void setRequestLimits() {
        for (final String[] domains : getPluginDomains()) {
            for (final String domain : domains) {
                Browser.setRequestIntervalLimitGlobal(domain, 3000);
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "https://bato.to/tos";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "bato.to", "battwo.com", "batotoo.com", "dto.to", "hto.to", "mangatoto.to", "mto.to", "wto.to", "comiko.net", "batocomic.com", "batocomic.net", "batocomic.org", "comiko.org", "mangatoto.com", "mangatoto.net", "mangatoto.org", "readtoto.com", "readtoto.net", "readtoto.org", "xbato.com", "xbato.net", "xbato.org", "zbato.com", "zbato.net", "zbato.org" });
        return ret;
    }

    /** 2023-11-10: Collect list of dead domains as that may come in handy in the future. */
    protected List<String> getDeadDomains() {
        final ArrayList<String> deadDomains = new ArrayList<String>();
        deadDomains.add("mangatoto.to");
        return deadDomains;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/areader\\?id=[a-z0-9]+\\&p=\\d+");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME               = true;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final int     FREE_MAXDOWNLOADS         = 3;
    private static final boolean ACCOUNT_FREE_RESUME       = true;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS = 3;
    private String               dllink                    = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        dllink = null;
        this.setBrowserExclusive();
        this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        this.br.getHeaders().put("Referer", "http://" + this.getHost() + "/reader");
        this.br.setAllowedResponseCodes(503);
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            login(account, false);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (this.br.getHttpConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 503 too many connections", 1 * 60 * 1000l);
        }
        /* First try to get filename from decrypter - should fit 99,99% of all cases! */
        String fname_without_ext = link.getStringProperty("fname_without_ext", null);
        if (fname_without_ext == null) {
            fname_without_ext = this.br.getRegex("document\\.title = \\'([^<>\"]*?) \\| Batoto\\!';").getMatch(0);
        }
        if (fname_without_ext == null) {
            /* Don't fail just because we couldn't find nice filenames! */
            final Regex linkinfo = new Regex(link.getDownloadURL(), "/areader\\?id=([a-z0-9]+)\\&p=(\\d+)");
            fname_without_ext = linkinfo.getMatch(0) + "_" + linkinfo.getMatch(1);
        }
        String[] unformattedSource = br.getRegex("src=\"(https?://img\\.(?:batoto\\.net|bato\\.to)/comics/\\d{4}/\\d{1,2}/\\d{1,2}/[a-z0-9]/read[^/]+/[^\"]+(\\.[a-z]+))\"").getRow(0);
        if (unformattedSource == null) {
            // <img
            // src="http://arc.bato.to/comics/t/toloverudarkness/0.5/cxc-scans/English/read4d69e24fa5247/%5BToLoveRuDarkness%5D-ch00_004d69e250dd8a4.jpg"
            unformattedSource = br.getRegex("<img[^>]+src=\"(https?://\\w+\\.(?:batoto\\.net|bato\\.to)/comics/(?:[^/]+/){1,}read[^/]+/[^\"]+(\\.[a-z]+))\"").getRow(0);
        }
        if (unformattedSource == null || unformattedSource.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = unformattedSource[0];
        final String extension = unformattedSource[1];
        link.setFinalFileName(fname_without_ext + extension);
        if (!isDownload) {
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                handleConnectionErrors(br2, con);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    if (con != null) {
                        con.disconnect();
                    }
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link, true);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        handleConnectionErrors(br, dl.getConnection());
        link.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(false);
                br.getPage("https://id.bato.to/login");
                final Form loginform = br.getFormbyKey("auth_key");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("ips_username", account.getUser());
                loginform.put("ips_password", account.getPass());
                loginform.remove("rememberMe");
                loginform.put("rememberMe", "1");
                loginform.remove("anonymous");
                loginform.remove(null);
                br.submitForm(loginform);
                if (br.getCookie(this.br.getHost(), "pass_hash", Cookies.NOTDELETEDPATTERN) == null) {
                    throw new AccountInvalidException();
                }
                account.saveCookies(br.getCookies(this.br.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        /* free accounts can still have captcha */
        account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, true);
        br.setFollowRedirects(false);
        doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}