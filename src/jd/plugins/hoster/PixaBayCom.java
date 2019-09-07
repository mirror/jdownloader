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

import java.io.File;
import java.util.concurrent.atomic.AtomicInteger;

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.config.Property;
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
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pixabay.com" }, urls = { "https?://(?:www\\.)?pixabay\\.com/en/([a-z0-9\\-]+\\-\\d+)/" })
public class PixaBayCom extends PluginForHost {
    public PixaBayCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://pixabay.com/en/accounts/register/");
    }

    @Override
    public String getAGBLink() {
        return "https://pixabay.com/en/service/terms/";
    }

    /* Connection stuff */
    private static final boolean FREE_RESUME               = false;
    private static final int     FREE_MAXCHUNKS            = 1;
    private static final int     FREE_MAXDOWNLOADS         = 20;
    private static final boolean ACCOUNT_FREE_RESUME       = false;
    private static final int     ACCOUNT_FREE_MAXCHUNKS    = 1;
    private static final int     ACCOUNT_FREE_MAXDOWNLOADS = 20;
    /* TODO: Maybe add support for SVG format(s) */
    private final String[]       qualities                 = { "Original", "O", "XXL", "XL", "L", "M", "S" };
    private String               quality_max               = null;
    private String               quality_download_id       = null;
    /* don't touch the following! */
    private static AtomicInteger maxPrem                   = new AtomicInteger(1);

    @SuppressWarnings("deprecation")
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("http://", "https://"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            this.login(aa, false);
        }
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("?")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String fallback_filename = new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
        String filename = br.getRegex("<title>(?:Free photo: )?([^<>]*?)(?:· Free vector graphic on Pixabay)?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>([^<>]*?)</title>").getMatch(0);
        }
        /* Find filesize based on whether user has an account or not. */
        String filesize = null;
        if (aa != null) {
            boolean done = false;
            int heightMax = 0;
            int heightTmp = 0;
            final String[] qualityInfo = br.getRegex("(<td><input type=\"radio\" name=\"download\".*?/td></tr>)").getColumn(0);
            for (final String possiblequality : qualities) {
                for (final String quality : qualityInfo) {
                    boolean grabDownloadData = false;
                    // logger.info("quality: " + quality);
                    String quality_name = new Regex(quality, "(ORIGINAL|O|S|M|L|XXL|XL|SVG)</td>").getMatch(0);
                    if (quality_name == null) {
                        quality_name = new Regex(quality, "(\\d+(?:x|×)\\d+)").getMatch(0);
                    }
                    if (quality_name == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    final boolean accountQualityPossible = aa != null && (quality_name.equalsIgnoreCase("XXL") || quality_name.equalsIgnoreCase("XL") || quality_name.equalsIgnoreCase("O") || quality_name.equalsIgnoreCase("Original"));
                    final boolean isNoAccountQuality = !quality_name.equalsIgnoreCase("XXL") && !quality_name.equalsIgnoreCase("XL") && !quality_name.equalsIgnoreCase("O") && !quality_name.equalsIgnoreCase("Original");
                    /* Possibly unknown quality but we might be able to find the highest quality by resolution */
                    final boolean isResolution = quality_name.matches("\\d+.\\d+");
                    if (quality_name.equals(possiblequality) && (accountQualityPossible || isNoAccountQuality)) {
                        done = true;
                        grabDownloadData = true;
                        break;
                    } else if (isResolution) {
                        grabDownloadData = true;
                        final String heightStr = new Regex(quality_name, "^(\\d+)").getMatch(0);
                        heightTmp = Integer.parseInt(heightStr);
                        if (heightTmp > heightMax) {
                            heightMax = heightTmp;
                        }
                    }
                    if (grabDownloadData) {
                        filesize = new Regex(quality, "class=\"hide-xs hide-md\">([^<>\"]*?)<").getMatch(0);
                        if (filesize == null) {
                            filesize = new Regex(quality, ">(\\d+(?:\\.\\d+)? (?:kB|mB|gB))<").getMatch(0);
                        }
                        quality_max = new Regex(quality, ">(\\d+)\\s*(?:×|x)\\s*\\d+<").getMatch(0);
                        quality_download_id = new Regex(quality, "([^<>\"/]*?\\.jpg)").getMatch(0);
                        if (quality_download_id == null) {
                            quality_download_id = new Regex(quality, "name=\"download\" value=\"([^<>\"]*?)\"").getMatch(0);
                        }
                    }
                }
                if (done) {
                    break;
                }
            }
        }
        if (filename == null) {
            filename = fallback_filename;
        }
        if (filesize == null || quality_download_id == null) { // No account
            String dllink = br.getRegex("(https://cdn[^<>\"\\s]+) 1.333x").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(https://cdn[^<>\"\\s]+) 1x").getMatch(0);
            }
            if (dllink != null) {
                String ext = new Regex(dllink, "(\\.[a-z]+)$").getMatch(0);
                link.setProperty("free_directlink", dllink);
                filename = Encoding.htmlDecode(filename.trim());
                filename = encodeUnicode(filename) + ext;
                link.setFinalFileName(filename);
                return AvailableStatus.TRUE; // <=== No account
            }
        }
        if (quality_download_id == null) {
            logger.info("quality_download_id: " + quality_download_id);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (quality_max == null) {
            quality_max = "10000";
        }
        filename = Encoding.htmlDecode(filename.trim());
        filename = encodeUnicode(filename) + ".jpg";
        link.setFinalFileName(filename);
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("static-access")
    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        String dllink = checkDirectLink(downloadLink, directlinkproperty);
        if (dllink == null) {
            final String post_url = "https://pixabay.com/en/photos/download/" + quality_download_id + "?attachment&modal";
            this.br.getPage(post_url);
            final String timestamp = this.br.getRegex("name=\"timestamp\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String security_hash = this.br.getRegex("name=\"security_hash\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (timestamp == null || security_hash == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            final Recaptcha rc = new Recaptcha(br, this);
            /* Last updated: 2015-08-17 */
            rc.setId("6Ld8hL8SAAAAAKbydL06Ir20hG_u2SfRkBbfpTNf");
            rc.load();
            final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            final String c = getCaptchaCode("recaptcha", cf, downloadLink);
            this.br.postPage(post_url, "topyenoh=&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + Encoding.urlEncode(c) + "&timestamp=" + timestamp + "&security_hash=" + security_hash + "&attachment=1");
            dllink = br.getRegex("window\\.location=\"(/[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = "https://pixabay.com" + dllink;
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
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                if (isJDStable()) {
                    con = br2.openGetConnection(dllink);
                } else {
                    con = br2.openHeadConnection(dllink);
                }
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return dllink;
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                /* 2019-09-07: If life was always as simple as that ... :D */
                br.setCookie(this.getHost(), "is_human", "1");
                // br.setCookie(this.getHost(), "lang", "de");
                br.setCookie(this.getHost(), "client_width", "1920");
                br.setFollowRedirects(true);
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                boolean loggedIN = false;
                if (cookies != null) {
                    br.setCookies(account.getHoster(), cookies);
                    br.getPage("https://" + this.getHost() + "/de/accounts/media/");
                    loggedIN = this.isLoggedIN();
                }
                if (!loggedIN) {
                    logger.info("Performing full login");
                    br.getPage("https://" + this.getHost() + "/accounts/login/");
                    final Form loginform = br.getFormbyKey("password");
                    if (loginform == null) {
                        logger.warning("Failed to find loginform");
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                    loginform.put("username", account.getUser());
                    loginform.put("password", account.getPass());
                    if (loginform.getAction() != null && loginform.getAction().equals(".")) {
                        loginform.setAction(br.getURL());
                    }
                    br.submitForm(loginform);
                    if (!isLoggedIN()) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(br.getCookies(account.getHoster()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    private boolean isLoggedIN() {
        return br.containsHTML("/accounts/logout/\"");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        maxPrem.set(ACCOUNT_FREE_MAXDOWNLOADS);
        try {
            account.setType(AccountType.FREE);
            /* Free accounts cannot have captchas */
            account.setMaxSimultanDownloads(maxPrem.get());
            account.setConcurrentUsePossible(true);
        } catch (final Throwable e) {
            /* not available in old Stable 0.9.581 */
        }
        ai.setStatus("Registered (free) user");
        account.setValid(true);
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        /* Login not needed here as we already logged in above. */
        String dllink = this.br.getRegex("name=\"download\" value=\"([^<>\"]*?)\" data-perm=\"auth\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = "https://pixabay.com/en/photos/download/" + dllink + "?attachment";
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, ACCOUNT_FREE_RESUME, ACCOUNT_FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else if (this.br.containsHTML("\"fdiv_recaptcha\"|google\\.com/recaptcha/help")) {
                /*
                 * If a user downloads too much, he might get asked to enter captchas in premium mode --> Wait to get around this problem.
                 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Captcha needed - wait some time until you can download again", 10 * 60 * 1000l);
            }
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setProperty("premium_directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        /* workaround for free/premium issue on stable 09581 */
        return maxPrem.get();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}