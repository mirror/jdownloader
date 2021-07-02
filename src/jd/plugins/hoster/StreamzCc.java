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
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
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

import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.captcha.v2.challenge.hcaptcha.CaptchaHelperHostPluginHCaptcha;
import org.jdownloader.plugins.components.antiDDoSForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class StreamzCc extends antiDDoSForHost {
    public StreamzCc(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://streamzz.to/signup.dll");
    }

    @Override
    public String getAGBLink() {
        return "https://streamz.cc/contact.dll";
    }

    @Override
    protected boolean useRUA() {
        return true;
    }

    private static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "streamz.ws", "streamz.cc", "streamz.bz", "streamz.vg", "streamzz.to" });
        return ret;
    }

    @Override
    public String rewriteHost(final String host) {
        /* 2020-09-01: Main domain has changed from streamz.cc to streamz.ws */
        return this.rewriteHost(getPluginDomains(), host, new String[0]);
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([a-z0-9==]{10,})");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = -10;
    private static final int     FREE_MAXDOWNLOADS = -1;

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        if (link != null && link.getPluginPatternMatcher() != null) {
            final Regex embedURL = new Regex(link.getPluginPatternMatcher(), "(https?://[^/]+)/i([A-Z]+[A-Za-z0-9]+)");
            if (embedURL.matches()) {
                /* Change to "file" URL */
                link.setPluginPatternMatcher(embedURL.getMatch(0) + "/f" + embedURL.getMatch(1));
            }
        }
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
        return requestFileInformation(link, 0);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, int recursed) throws Exception {
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + ".mp4");
        }
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        // final String shareLink = br.getRegex("Please share this link to your friends:\\s*(https?://streamz.cc/[a-z0-9==]+)").getMatch(0);
        // if (StringUtils.isNotEmpty(shareLink)) {
        // link.setPluginPatternMatcher(shareLink);
        // }
        if ((br.getHttpConnection().getResponseCode() == 404 || br.containsHTML(">\\s*File not found")) && !br.containsHTML("/download")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("The link in your browser URL is only valid for")) {
            if (recursed >= 5) {
                logger.warning("Too many failed attempts");
                return AvailableStatus.FALSE;
            }
            final Regex realLinkRegex = br.getRegex("(?i)Please share this link to your friends:\\s*(https?://[^/]+/(f[A-Za-z0-9]{10,}))<br>");
            if (realLinkRegex.matches()) {
                final String realFUIDNew = realLinkRegex.getMatch(1);
                final String realFUIDPart = realFUIDNew.substring(0, realFUIDNew.length() - 4);
                if (link.getPluginPatternMatcher().contains(realFUIDPart)) {
                    /* All ok */
                } else {
                    /* TODO */
                    return AvailableStatus.FALSE;
                }
            } else {
                final String redirectLink = link.getStringProperty("redirect_link");
                if (StringUtils.isEmpty(redirectLink)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "page expired and no redirect_link found");
                }
                br.setFollowRedirects(false);
                br.getPage(redirectLink);
                if (br.getRedirectLocation() != null) {
                    link.setPluginPatternMatcher(br.getRedirectLocation());
                    return requestFileInformation(link, recursed + 1);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        final String fallbackFilename = link.getStringProperty("fallback_filename");
        String filename = br.getRegex("(?i)<title>stream[a-z]+\\.[a-z]+ ([^<>\"]+)</title>").getMatch(0);
        if (StringUtils.isEmpty(filename) || (new Regex(filename, "^(?:[a-z0-9]+|file title unknown)$").matches() && !StringUtils.isEmpty(fallbackFilename))) {
            filename = br.getRegex("<h5>([^<>\"]+)</h5>").getMatch(0);
        }
        if (StringUtils.isNotEmpty(filename) && (!new Regex(filename, "^(?:[a-z0-9]+|file title unknown)$").matches() || StringUtils.isEmpty(fallbackFilename))) {
            filename = Encoding.htmlDecode(filename).trim();
            if (!filename.endsWith(".mp4")) {
                filename += ".mp4";
            }
            link.setFinalFileName(filename);
        } else if (!StringUtils.isEmpty(fallbackFilename)) {
            link.setName(fallbackFilename);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, null, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final Account account, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(link, directlinkproperty);
        if (dllink == null) {
            requestFileInformation(link);
            final Regex downloadInfo = br.getRegex("(/download([a-z0-9]+))");
            if (!downloadInfo.matches()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final boolean downloadStream = account == null || true;
            if (downloadStream) {
                /* Stream download */
                dllink = "/getlink-" + downloadInfo.getMatch(1) + ".dll";
            } else {
                /* Official download */
                final String urlContinue = downloadInfo.getMatch(0);
                if (urlContinue == null) {
                    logger.warning("Failed to find url_continue");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                br.getPage(urlContinue);
                if (br.containsHTML("(?i)>\\s*Too many downloads? in the last few minutes")) {
                    throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Too many download in the last few minutes", 5 * 60 * 1000l);
                } else if (br.containsHTML("(?i)color=\"red\">\\s*Please|before you try to download this movie") && account == null) {
                    throw new AccountRequiredException();
                }
                final Form continueForm = br.getFormbyActionRegex(".*dodownload\\.dll");
                if (continueForm == null) {
                    logger.warning("Failed to find continueForm");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final String hcaptchaResponse = new CaptchaHelperHostPluginHCaptcha(this, br).getToken();
                continueForm.put("h-captcha-response", Encoding.urlEncode(hcaptchaResponse));
                // this.sleep(10000, link);
                this.submitForm(continueForm);
                dllink = br.getRegex("(/getlink-[a-z0-9]+\\.dll)").getMatch(0);
                if (StringUtils.isEmpty(dllink)) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                // this.sleep(30000, link);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        } else if (dl.getConnection().getLongContentLength() == 839075) {
            dl.getConnection().disconnect();
            /* 2020-03-16: Cat & mouse - they're sending an "Turn adblock off" video. */
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Plugin broken contact JDownloader support", 60 * 60 * 1000l);
        }
        link.setProperty(directlinkproperty, dl.getConnection().getURL().toString());
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
    }

    private boolean login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    logger.info("Attempting cookie login");
                    this.br.setCookies(cookies);
                    if (!force && System.currentTimeMillis() - account.getCookiesTimeStamp("") < 5 * 60 * 1000l) {
                        logger.info("Cookies are still fresh --> Trust cookies without login");
                        return false;
                    }
                    br.getPage("https://" + this.getHost() + "/account.dll");
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
                br.getPage("https://" + this.getHost() + "/login.dll");
                final Form loginform = br.getFormbyActionRegex("(?i).*dologin.*");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("username", Encoding.urlEncode(account.getUser()));
                loginform.put("password", Encoding.urlEncode(account.getPass()));
                br.submitForm(loginform);
                if (!isLoggedin()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* 2021-07-02: We're not (yet) making use of that apikey */
                // final String apikey = br.getRegex("/api\\.dll\"[^^>]*>([a-f0-9]{32})").getMatch(0);
                // if (apikey != null) {
                // account.setProperty("apikey", apikey);
                // }
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
        return br.containsHTML("/logout\\.dll");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, true);
        ai.setUnlimitedTraffic();
        account.setType(AccountType.FREE);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        this.handleDownload(link, account, FREE_RESUME, FREE_MAXCHUNKS, "account_directurl");
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public boolean hasCaptcha(final DownloadLink link, final jd.plugins.Account acc) {
        /*
         * 2021-07-02: Captcha is required for official downloads. Captcha is not required for watching/downloading streams but we do not
         * support that due to js/crypto.
         */
        return false;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
            link.removeProperty("free_directlink");
            link.removeProperty("account_directurl");
        }
    }
}