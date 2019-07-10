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

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForHost;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "tiktok.com" }, urls = { "https?://(?:www\\.)?tiktok\\.com/(@[^/]+)/video/(\\d+)|https?://m\\.tiktok\\.com/v/(\\d+)\\.html" })
public class TiktokCom extends antiDDoSForHost {
    public TiktokCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("");
    }

    @Override
    public String getAGBLink() {
        return "https://www.tiktok.com/";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME       = true;
    /* 2019-07-10: More chunks possible but that would not be such a good idea! */
    private final int     FREE_MAXCHUNKS    = 1;
    private final int     FREE_MAXDOWNLOADS = 20;
    // private final boolean ACCOUNT_FREE_RESUME = true;
    // private final int ACCOUNT_FREE_MAXCHUNKS = 0;
    // private final int ACCOUNT_FREE_MAXDOWNLOADS = 20;
    // private final boolean ACCOUNT_PREMIUM_RESUME = true;
    // private final int ACCOUNT_PREMIUM_MAXCHUNKS = 0;
    // private final int ACCOUNT_PREMIUM_MAXDOWNLOADS = 20;

    @Override
    public String getLinkID(final DownloadLink link) {
        String linkid = getFID(link);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        String fid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(1);
        if (fid == null) {
            fid = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        }
        return fid;
    }

    private String  dllink        = null;
    private boolean server_issues = false;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        String user = null;
        final String fid = getFID(link);
        if (link.getPluginPatternMatcher().matches("@[^/]+/video/\\d+")) {
            user = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        } else {
            /* 2nd linktype which does not contain username --> Find username */
            br.setFollowRedirects(false);
            br.getPage(link.getPluginPatternMatcher());
            final String redirect = br.getRedirectLocation();
            if (redirect != null) {
                user = new Regex(redirect, this.getSupportedLinks()).getMatch(0);
            }
        }
        String filename = "";
        if (user != null) {
            filename += user + "_";
        }
        filename += fid + ".mp4";
        dllink = String.format("https://www.tiktok.com/node/video/playwm?id=%s", fid);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.setFollowRedirects(true);
        if (!StringUtils.isEmpty(dllink)) {
            URLConnectionAdapter con = null;
            try {
                con = openAntiDDoSRequestConnection(br, br.createHeadRequest(dllink));
                if (!con.getContentType().contains("video")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (!con.isOK() || con.getLongContentLength() == -1) {
                    server_issues = true;
                } else {
                    /* Try to add date to filename */
                    final String createDate = con.getHeaderField("Last-Modified");
                    if (!StringUtils.isEmpty(createDate)) {
                        final String dateFormatted = convertDateFormat(createDate);
                        filename = dateFormatted + "_" + filename;
                    }
                    link.setFinalFileName(filename);
                    link.setDownloadSize(con.getLongContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        } else {
            /* Do not yet set final filename */
        }
        return AvailableStatus.TRUE;
    }

    private String convertDateFormat(String sourceDate) {
        if (sourceDate == null) {
            return null;
        }
        final String sourceDatePart = new Regex(sourceDate, "^[A-Za-z]+, (\\d{1,2} \\w+ \\d{4})").getMatch(0);
        if (sourceDatePart == null) {
            return sourceDate;
        }
        sourceDate = sourceDatePart;
        String result = null;
        SimpleDateFormat source_format = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat target_format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            Date date = null;
            try {
                date = source_format.parse(sourceDate);
                result = target_format.format(date);
            } catch (Throwable e) {
            }
        } catch (Throwable e) {
            result = sourceDate;
            return sourceDate;
        }
        return result;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (server_issues) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 10 * 60 * 1000l);
        } else if (StringUtils.isEmpty(dllink)) {
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
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    // private String checkDirectLink(final DownloadLink downloadLink, final String property) {
    // String dllink = downloadLink.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // br2.setFollowRedirects(true);
    // con = br2.openHeadConnection(dllink);
    // if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // }
    // } catch (final Exception e) {
    // logger.log(e);
    // downloadLink.setProperty(property, Property.NULL);
    // dllink = null;
    // } finally {
    // if (con != null) {
    // con.disconnect();
    // }
    // }
    // }
    // return dllink;
    // }
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    // private static Object LOCK = new Object();
    //
    // private void login(final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // br.setFollowRedirects(true);
    // br.setCookiesExclusive(true);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null && !force) {
    // this.br.setCookies(this.getHost(), cookies);
    // return;
    // }
    // br.getPage("");
    // if (br.containsHTML("")) {
    // final DownloadLink dlinkbefore = this.getDownloadLink();
    // final DownloadLink dl_dummy;
    // if (dlinkbefore != null) {
    // dl_dummy = dlinkbefore;
    // } else {
    // dl_dummy = new DownloadLink(this, "Account", this.getHost(), "https://" + account.getHoster(), true);
    // this.setDownloadLink(dl_dummy);
    // }
    // final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
    // if (dlinkbefore != null) {
    // this.setDownloadLink(dlinkbefore);
    // }
    // // g-recaptcha-response
    // }
    // br.postPage("", "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
    // if (!isLoggedin()) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // account.saveCookies(this.br.getCookies(this.getHost()), "");
    // } catch (final PluginException e) {
    // account.clearCookies("");
    // throw e;
    // }
    // }
    // }
    //
    // private boolean isLoggedin() {
    // return br.getCookie(this.getHost(), "", Cookies.NOTDELETEDPATTERN) != null;
    // }
    //
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // final AccountInfo ai = new AccountInfo();
    // try {
    // login(account, true);
    // } catch (final PluginException e) {
    // throw e;
    // }
    // String space = br.getRegex("").getMatch(0);
    // if (space != null) {
    // ai.setUsedSpace(space.trim());
    // }
    // ai.setUnlimitedTraffic();
    // if (br.containsHTML("")) {
    // account.setType(AccountType.FREE);
    // /* free accounts can still have captcha */
    // account.setMaxSimultanDownloads(ACCOUNT_FREE_MAXDOWNLOADS);
    // account.setConcurrentUsePossible(false);
    // ai.setStatus("Registered (free) user");
    // } else {
    // final String expire = br.getRegex("").getMatch(0);
    // if (expire == null) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // ai.setValidUntil(TimeFormatter.getMilliSeconds(expire, "dd MMMM yyyy", Locale.ENGLISH));
    // }
    // account.setType(AccountType.PREMIUM);
    // account.setMaxSimultanDownloads(ACCOUNT_PREMIUM_MAXDOWNLOADS);
    // account.setConcurrentUsePossible(true);
    // ai.setStatus("Premium account");
    // }
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // login(account, false);
    // br.getPage(link.getPluginPatternMatcher());
    // if (account.getType() == AccountType.FREE) {
    // doFree(link, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS, "account_free_directlink");
    // } else {
    // String dllink = this.checkDirectLink(link, "premium_directlink");
    // if (dllink == null) {
    // dllink = br.getRegex("").getMatch(0);
    // if (StringUtils.isEmpty(dllink)) {
    // logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // }
    // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
    // if (dl.getConnection().getContentType().contains("html")) {
    // if (dl.getConnection().getResponseCode() == 403) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
    // } else if (dl.getConnection().getResponseCode() == 404) {
    // throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
    // }
    // logger.warning("The final dllink seems not to be a file!");
    // br.followConnection();
    // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // }
    // link.setProperty("premium_directlink", dl.getConnection().getURL().toString());
    // dl.startDownload();
    // }
    // }
    //
    // @Override
    // public int getMaxSimultanPremiumDownloadNum() {
    // return ACCOUNT_FREE_MAXDOWNLOADS;
    // }
    //
    // @Override
    // public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
    // return false;
    // }
    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}