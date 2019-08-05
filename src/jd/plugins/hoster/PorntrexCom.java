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
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "porntrex.com" }, urls = { "https?://(?:www\\.)?porntrex\\.com/video/\\d+/[a-z0-9\\-]+/?" })
public class PorntrexCom extends PluginForHost {
    public PorntrexCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://www.porntrex.com/signup/");
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, getPluginConfig(), "Preferred_format", FORMATS, "Preferred Format").setDefaultValue(0));
    }

    /* Connection stuff */
    /* Connection stuff */
    private final boolean         FREE_RESUME                  = true;
    private final int             FREE_MAXCHUNKS               = 0;
    private final int             FREE_MAXDOWNLOADS            = 20;
    private final boolean         ACCOUNT_FREE_RESUME          = true;
    private final int             ACCOUNT_FREE_MAXCHUNKS       = 0;
    private final int             ACCOUNT_FREE_MAXDOWNLOADS    = 20;
    private final boolean         ACCOUNT_PREMIUM_RESUME       = true;
    private final int             ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private final int             ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;
    private String                dllink                       = null;
    private boolean               server_issues                = false;
    private static final String[] FORMATS                      = new String[] { "Best available", "2160p", "1440p", "1080p", "720p", "480p", "360p" };

    @Override
    public String getAGBLink() {
        return "https://www.Porntrex.com/support/";
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">Sorry, this video was deleted")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String url_filename = new Regex(link.getDownloadURL(), "([a-z0-9\\-]+)/?$").getMatch(0).replace("-", " ");
        String filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        if (filename == null) {
            filename = url_filename;
        }
        filename = Encoding.htmlDecode(filename);
        filename = filename.trim();
        filename = encodeUnicode(filename);
        final String ext = ".mp4";
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        link.setName(filename);
        if (isPrivateVideo()) {
            /* Private content can only be downloaded via account */
            return AvailableStatus.TRUE;
        }
        final SubConfiguration cfg = getPluginConfig();
        final int Preferred_format = cfg.getIntegerProperty("Preferred_format", 0);
        logger.info("Debug info: Preferred_format: " + Preferred_format);
        final List<String> qualities = Arrays.asList(new String[] { "2160p", "1440p", "1080p", "720p", "480p", "360p" });
        final List<String> foundQualities = new ArrayList<String>();
        for (final String quality : qualities) {
            dllink = br.getRegex("video[^']+url\\d?:\\s*'(https?[^']+)'\\,[^\\,]+" + quality).getMatch(0);
            logger.info("Debug info: Preferred_format: " + Preferred_format + ", checking format: " + quality + " ,dllink: " + dllink);
            if (dllink != null) {
                checkDllink(link);
                if (dllink == null) {
                    continue;
                }
                foundQualities.add(dllink);
                if (Preferred_format == 0) {
                    break;
                } else if (Preferred_format - 1 == qualities.indexOf(quality)) {
                    break;
                }
            }
        }
        if (StringUtils.isEmpty(dllink) && foundQualities.size() > 0) {
            dllink = foundQualities.get(0);
        }
        if (StringUtils.isEmpty(dllink)) {
            /* 2019-07-15 */
            dllink = jd.plugins.hoster.KernelVideoSharingCom.getDllink(br, this);
        }
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    private boolean isPrivateVideo() {
        return br.containsHTML("This video is a private video");
    }

    private String checkDllink(final DownloadLink link) throws Exception {
        final Browser br2 = br.cloneBrowser();
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            br2.setFollowRedirects(true);
            con = br2.openHeadConnection(dllink);
            if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                dllink = null;
            }
        } catch (final Exception e) {
            logger.log(e);
            dllink = null;
        } finally {
            if (con != null) {
                con.disconnect();
            }
        }
        return dllink;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (isPrivateVideo()) {
            throw new AccountRequiredException("Private video");
        } else if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            br.followConnection();
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error");
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                boolean loggedinViaCookies = false;
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    br.getPage("https://www." + account.getHoster() + "/my/");
                    loggedinViaCookies = this.isLoggedin();
                }
                if (!loggedinViaCookies) {
                    // br.getPage("");
                    br.postPage("https://www." + account.getHoster() + "/ajax-login/", "username=" + Encoding.urlEncode(account.getUser()) + "&pass=" + Encoding.urlEncode(account.getPass()) + "&action=login&email_link=https%3A%2F%2Fwww.porntrex.com%2Femail%2F&remember_me=1&format=json&mode=async");
                    // br.getPage("/my/");
                    if (!isLoggedin()) {
                        final String message = PluginJSonUtils.getJson(br, "message");
                        if (!StringUtils.isEmpty(message)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.getCookie(this.getHost(), "kt_member", Cookies.NOTDELETEDPATTERN) != null;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        /* 2019-07-16: So far we haven't had any premium accounts to test. */
        final boolean onlyFreeSupported = true;
        if (onlyFreeSupported) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
            ai.setStatus("Registered (free) user");
        } else {
            final String expire = br.getRegex("").getMatch(0);
            if (expire == null) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
            }
            account.setType(AccountType.PREMIUM);
            account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
            account.setConcurrentUsePossible(true);
            ai.setStatus("Premium account");
        }
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(account, false);
        requestFileInformation(link);
        doFree(link, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS, "free_directlink");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_FREE_MAXDOWNLOADS;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.KernelVideoSharing;
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
