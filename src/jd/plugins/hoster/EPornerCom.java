//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package jd.plugins.hoster;

import java.io.IOException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.plugins.components.config.EpornerComConfig;
import org.jdownloader.plugins.components.config.EpornerComConfig.PreferredStreamQuality;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountUnavailableException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "eporner.com" }, urls = { "https?://(?:\\w+\\.)?eporner\\.com/(?:hd\\-porn/|video-)(\\w+)(/([^/]+))?" })
public class EPornerCom extends PluginForHost {
    public String  dllink = null;
    private String vq     = null;

    public EPornerCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://www.eporner.com/");
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    @Override
    public String getAGBLink() {
        return "https://www.eporner.com/terms/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null, false);
    }

    public AvailableStatus requestFileInformation(final DownloadLink link, final Account account, final boolean isDownload) throws Exception {
        final String titleByURL = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(2);
        String fallbackFilename;
        if (titleByURL != null) {
            fallbackFilename = titleByURL.replace("-", " ").trim() + ".mp4";
        } else {
            fallbackFilename = this.getFID(link) + ".mp4";
        }
        if (!link.isNameSet()) {
            link.setName(fallbackFilename);
        }
        dllink = null;
        if (account != null) {
            this.login(account, false);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("id=\"deletedfile\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!this.br.getURL().contains(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String betterTitleByURL = new Regex(br.getURL(), this.getSupportedLinks()).getMatch(2);
        if (betterTitleByURL != null) {
            fallbackFilename = titleByURL.replace("-", " ").trim() + ".mp4";
        }
        String title = br.getRegex("(?i)<title>([^<>\"]*?) \\- EPORNER Free HD Porn Tube\\s*</title>").getMatch(0);
        if (title == null) {
            /* 2022-11-21 */
            title = br.getRegex("(?i)<title>([^<>\"]+) - EPORNER</title>").getMatch(0);
        }
        long filesizeMax = 0;
        getDllink(this.br, link);
        if (dllink == null) {
            /* First try to get DOWNLOADurls */
            final String[][] dloadinfo = this.br.getRegex("(?i)href=\"(/dload/[^<>\"]+)\">Download MP4 \\(\\d+p, ([^<>\"]+)\\)</a>").getMatches();
            if (dloadinfo != null && dloadinfo.length != 0) {
                for (final String[] dlinfo : dloadinfo) {
                    final String directurl = dlinfo[0];
                    final String tempsizeFoundInHTMLStr = dlinfo[1];
                    final long tempsizel = SizeFormatter.getSize(tempsizeFoundInHTMLStr);
                    if (tempsizel > filesizeMax) {
                        filesizeMax = tempsizel;
                        dllink = directurl;
                    }
                }
            }
        }
        /* Failed to find official downloadurl? Try to download stream. */
        if (dllink == null && isDownload) {
            final String correctedBR = br.toString().replace("\\", "");
            final String continueLink = new Regex(correctedBR, "(\"|\\')(/config\\d+/\\w+/[0-9a-f]+(/)?)(\"|\\')").getMatch(1);
            if (continueLink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(Encoding.htmlDecode(continueLink) + (continueLink.endsWith("/") ? "1920" : "/1920"));
            dllink = br.getRegex("<hd\\.file>(https?://.*?)</hd\\.file>").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("<file>(https?://.*?)</file>").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("file:[\r\n\r ]*?\"(https?://[^<>\"]*?)\"").getMatch(0);
                }
            }
        }
        if (title != null) {
            title = Encoding.htmlDecode(title).trim();
            link.setFinalFileName(title + ".mp4");
        } else {
            link.setFinalFileName(fallbackFilename);
        }
        if (isBadDirecturl(this.dllink)) {
            /* Video is online but can't be streamed/downloaded at this moment. */
            return AvailableStatus.TRUE;
        }
        /*
         * 2020-05-26: Checking their downloadlink counts towards their daily downloadlimit so only check them if the filesize has not been
         * found already!
         */
        if (filesizeMax > 0) {
            link.setDownloadSize(filesizeMax);
        } else if (link.getView().getBytesTotal() <= 0 && dllink != null) {
            /* Only get filesize from url if we were not able to find it in html --> Saves us time! */
            final Browser br2 = br.cloneBrowser();
            // In case the link redirects to the finallink
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
                connectionErrorhandling(con, link, account);
                if (con.getCompleteContentLength() > 0) {
                    link.setVerifiedFileSize(con.getCompleteContentLength());
                }
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        handleDownload(link, null);
    }

    public void handleDownload(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link, account, true);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (isBadDirecturl(this.dllink)) {
            errorBrokenVideo();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        connectionErrorhandling(dl.getConnection(), link, account);
        dl.startDownload();
    }

    private void connectionErrorhandling(final URLConnectionAdapter con, final DownloadLink link, final Account account) throws PluginException, IOException {
        /* E.g. https://static.eporner.com/na.mp4 */
        if (isBadDirecturl(con.getURL().toString())) {
            errorBrokenVideo();
        }
        /* Double-check for broken/bad/dummy-video */
        final String etag = con.getRequest().getResponseHeader("etag");
        if (StringUtils.equalsIgnoreCase(etag, "\"52ee70df-9cdc1\"")) {
            errorBrokenVideo();
        }
        if (!this.looksLikeDownloadableContent(con)) {
            br.followConnection(true);
            /* 2020-05-26: Limit = 100 videos per day for unregistered users, no limit for registered users */
            if (br.containsHTML("(?i)>\\s*You have downloaded more than|>\\s*Please try again tomorrow or register for free to unlock unlimited")) {
                if (account != null) {
                    /* 2020-05-26: This should never happen in account mode */
                    throw new AccountUnavailableException("Daily download limit reached or session error", 10 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily download limit reached", 60 * 60 * 1000l);
                }
            } else {
                errorBrokenVideo();
            }
        }
    }

    private boolean isBadDirecturl(final String url) {
        if (url != null && url.matches("(?i)https?://[^/]+/na\\.(flv|mp4).*")) {
            return true;
        } else {
            return false;
        }
    }

    private void errorBrokenVideo() throws PluginException {
        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Broken video?", 5 * 60 * 1000l);
    }

    private void getDllink(final Browser br, final DownloadLink link) {
        vq = getPreferredStreamQuality();
        if (vq != null) {
            logger.info("Looking for user selected quality");
            dllink = br.getRegex("<a href\\s*=\\s*\"(/dload/[^\"]+)\"\\s*>\\s*Download MP4 \\(" + vq).getMatch(0);
            if (dllink != null) {
                logger.info("Found user selected quality: " + vq);
            } else {
                logger.info("Failed to find user selected quality: " + vq);
            }
        }
        if (dllink == null) {
            logger.info("Looking for BEST quality");
            final String[] allQualities = new String[] { "2160p", "1440p", "1080p", "720p", "480p", "360p", "240p" };
            for (final String qualityCandidate : allQualities) {
                vq = qualityCandidate;
                dllink = br.getRegex("<a href\\s*=\\s*\"(/dload/[^\"]+)\"\\s*>\\s*Download MP4 \\(" + vq).getMatch(0);
                if (dllink != null) {
                    logger.info("Picked quality: " + vq);
                    break;
                }
            }
        }
        final String filesize = br.getRegex(">\\s*Download MP4 \\(" + vq + "\\s*,\\s*(\\d+[^<>\"]+)\\)").getMatch(0);
        if (filesize != null) {
            logger.info("Found filesize for picked quality: " + filesize);
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return;
    }

    private String getPreferredStreamQuality() {
        final EpornerComConfig cfg = PluginJsonConfig.get(this.getConfigInterface());
        final PreferredStreamQuality quality = cfg.getPreferredStreamQuality();
        switch (quality) {
        case Q2160P:
            return "2160p";
        case Q1440P:
            return "1440p";
        case Q1080P:
            return "1080p";
        case Q720P:
            return "720p";
        case Q480P:
            return "480p";
        case Q360P:
            return "360p";
        case Q240P:
            return "240p";
        case BEST:
        default:
            return null;
        }
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(this.getHost(), cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/");
                    if (this.isLoggedin()) {
                        logger.info("Cookie login successful");
                        /* Refresh cookie timestamp */
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return true;
                    } else {
                        logger.info("Cookie login failed");
                    }
                }
                logger.info("Performing full login");
                final Form loginform = new Form();
                loginform.setMethod(MethodType.POST);
                loginform.setAction("https://www." + this.getHost() + "/xhr/login/");
                loginform.put("xhr", "1");
                loginform.put("act", "login");
                loginform.put("login", Encoding.urlEncode(account.getUser()));
                loginform.put("haslo", Encoding.urlEncode(account.getPass()));
                loginform.put("ref", "/");
                br.submitForm(loginform);
                /* 2020-05-26: E.g. login failed: {"status":0,"msg_head":"Login failed.","msg_body":"Bad login\/password"} */
                br.getPage("/");
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
                return true;
            } catch (final PluginException e) {
                if (e.getLinkStatus() == LinkStatus.ERROR_PREMIUM) {
                    account.clearCookies("");
                }
                throw e;
            }
        }
    }

    private boolean isLoggedin() {
        return br.containsHTML("/logout");
    }

    /** Free accounts do not have any downloadlimits. Anonymous users can download max. 100 files per day. */
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        account.setConcurrentUsePossible(false);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        this.handleDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public Class<? extends EpornerComConfig> getConfigInterface() {
        return EpornerComConfig.class;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /* 2020-05-26: No captchas at all */
        return false;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}