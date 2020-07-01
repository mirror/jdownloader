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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.AccountController;
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
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class PervcityCom extends PluginForHost {
    public PervcityCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://members.pervcity.com/login.php");
    }

    @Override
    public String getAGBLink() {
        return "https://pervcity.com/pages.php?id=toc";
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "analoverdose.com" });
        ret.add(new String[] { "bangingbeauties.com" });
        ret.add(new String[] { "chocolatebjs.com" });
        ret.add(new String[] { "oraloverdose.com" });
        ret.add(new String[] { "pervcity.com" });
        ret.add(new String[] { "upherasshole.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:members\\.|www\\.)?" + buildHostsPatternPart(domains) + "/(?:scenes|trailers)/([^<>\"/]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME                  = true;
    private static final int     FREE_MAXCHUNKS               = 0;
    private static final int     FREE_MAXDOWNLOADS            = -1;
    private static final boolean ACCOUNT_PREMIUM_RESUME       = true;
    private static final int     ACCOUNT_PREMIUM_MAXCHUNKS    = 0;
    private static final int     ACCOUNT_PREMIUM_MAXDOWNLOADS = -1;
    private boolean              download_not_yet_possible    = false;
    private String               dllink                       = null;

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

    private Browser prepBR(final Browser br) {
        br.setCookie(this.getHost(), "warn", "false");
        return br;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        download_not_yet_possible = false;
        this.setBrowserExclusive();
        prepBR(this.br);
        /* Trailers are only downloadable for free users */
        if (link.getPluginPatternMatcher().contains("/trailers/")) {
            /* Trailer download */
            br.getPage(String.format("https://%s/trailers/%s.html", this.getHost(), this.getFID(link)));
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.dllink = br.getRegex("(/trailers/[^<>\"\\']+\\.mp4)").getMatch(0);
            final String filename = this.getFID(link).replace("-", " ") + ".mp4";
            link.setFinalFileName(filename);
            if (this.dllink != null) {
                /* Find filesize */
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (con.getContentType().contains("text") || !con.isOK() || con.getLongContentLength() == -1) {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
                    } else {
                        link.setDownloadSize(con.getCompleteContentLength());
                    }
                } finally {
                    try {
                        con.disconnect();
                    } catch (final Throwable e) {
                    }
                }
            }
        } else {
            /* Account required */
            final Account account = AccountController.getInstance().getValidAccount(this.getHost());
            if (account == null) {
                throw new AccountRequiredException();
            }
            this.login(account);
            br.getPage(String.format("https://members.%s/scenes/%s.html", this.getHost(), this.getFID(link)));
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            if (filename == null) {
                /* Fallback */
                filename = this.getFID(link);
            }
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()) + ".mp4");
            /* Old handling */
            // final String setid = this.br.getRegex("\"setId=(\\d+)\"").getMatch(0);
            // if (setid == null) {
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // this.br.postPage("/showsetdata.php", "SetId=" + setid);
            // if (!this.br.containsHTML("class=\"downloadoption\\-container\"")) {
            // download_not_yet_possible = true;
            // return AvailableStatus.TRUE;
            // }
            /* Find best quality */
            final String downloadHTML = br.getRegex("<select[^>]*>\\s+<option value=\"\" selected=\"selected\">Choose Format</option>(.*?)</select>").getMatch(0);
            final String[] downloadHTMLs = new Regex(downloadHTML, "(<option.*?)</option>").getColumn(0);
            long filesizeMax = 0;
            for (final String html : downloadHTMLs) {
                String url = new Regex(html, "\"(https?://[^\"]+\\.mp4[^\"]*)\"").getMatch(0);
                final String filesizeStr = new Regex(html, "\\((\\d+\\.\\d{1,2} (MB|GB))\\)").getMatch(0);
                if (url == null || filesizeStr == null) {
                    /* Skip invalid items e.g. non-MP4 items */
                    continue;
                }
                final long filesizeTmp = SizeFormatter.getSize(filesizeStr);
                if (filesizeTmp > filesizeMax) {
                    filesizeMax = filesizeTmp;
                    this.dllink = url;
                }
            }
            if (filesizeMax > 0) {
                link.setDownloadSize(filesizeMax);
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        doFree(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void doFree(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        /* Not every item has trailers available --> No downloadurl found --> Assume content is only available for premium users */
        if (this.dllink == null) {
            throw new AccountRequiredException();
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, FREE_RESUME, FREE_MAXCHUNKS);
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
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error");
        }
        dl.startDownload();
    }

    // private String checkDirectLink(final DownloadLink link, final String property) {
    // String dllink = link.getStringProperty(property);
    // if (dllink != null) {
    // URLConnectionAdapter con = null;
    // try {
    // final Browser br2 = br.cloneBrowser();
    // con = br2.openHeadConnection(dllink);
    // if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
    // link.setProperty(property, Property.NULL);
    // dllink = null;
    // }
    // } catch (final Exception e) {
    // link.setProperty(property, Property.NULL);
    // dllink = null;
    // } finally {
    // try {
    // con.disconnect();
    // } catch (final Throwable e) {
    // }
    // }
    // }
    // return dllink;
    // }
    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    public static final long trust_cookie_age = 300000l;

    private void login(final Account account) throws Exception {
        synchronized (account) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                br.setFollowRedirects(true);
                prepBR(this.br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    this.br.setCookies(this.getHost(), cookies);
                    if (System.currentTimeMillis() - account.getCookiesTimeStamp("") <= trust_cookie_age) {
                        /* We trust these cookies --> Do not check them */
                        logger.info("Trust login cookies as they're not that old");
                        return;
                    }
                    br.getPage("https://members." + this.getHost() + "/");
                    if (isLoggedIN()) {
                        logger.info("Cookie login successful");
                        account.saveCookies(this.br.getCookies(this.getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        this.br.clearAll();
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://members." + this.getHost() + "/login.php");
                final Form loginform = br.getFormbyActionRegex(".*auth\\.form");
                if (loginform == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                loginform.put("uid", Encoding.urlEncode(account.getUser()));
                loginform.put("pwd", Encoding.urlEncode(account.getPass()));
                /* 2020-07-01: Login captcha is probably always required */
                if (br.containsHTML("/img\\.cptcha")) {
                    logger.info("Login captcha required");
                    final DownloadLink dummy = new DownloadLink(this, "Account", "members." + this.getHost(), "http://members." + this.getHost(), true);
                    if (this.getDownloadLink() == null) {
                        this.setDownloadLink(dummy);
                    }
                    final String code = this.getCaptchaCode("/img.cptcha", this.getDownloadLink());
                    loginform.put("img", Encoding.urlEncode(code));
                }
                /* Check "Remember Me" checkbox to get long-lasting cookies */
                loginform.put("rmb", "y");
                br.submitForm(loginform);
                if (!isLoggedIN()) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.containsHTML("class=\"fa fa-user\"");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        login(account);
        final AccountInfo ai = new AccountInfo();
        ai.setUnlimitedTraffic();
        /* 2020-07-01: Assume that all valid accounts of this website are premium accounts ... */
        account.setType(AccountType.PREMIUM);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium account");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        if (download_not_yet_possible) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Download not yet possible - content is not yet released!", 3 * 60 * 60 * 1000l);
        } else if (this.dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_PREMIUM_RESUME, ACCOUNT_PREMIUM_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return ACCOUNT_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}