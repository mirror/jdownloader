//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.AccountInvalidException;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "gigapeta.com" }, urls = { "https?://[\\w\\.]*?gigapeta\\.com/dl/(\\w+)" })
public class GigaPetaCom extends PluginForHost {
    // Gehört zu tenfiles.com/tenfiles.info
    public GigaPetaCom(PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://gigapeta.com/premium/");
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

    private Browser prepBR(final Browser br) {
        br.setCookie(this.getHost(), "lang", "us");
        br.setFollowRedirects(true);
        return br;
    }

    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        if (!link.isNameSet()) {
            link.setName(getFID(link));
        }
        prepBR(br);
        /* 2021-11-22: https unsupported */
        br.getPage(link.getPluginPatternMatcher().replaceFirst("(?i)https://", "http://"));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("class=\"big_error\">\\s*404\\s*<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        /* 2021-11-22: File information is sometimes even given for offline items! */
        final Regex infos = br.getRegex(Pattern.compile("<img src=\".*\" alt=\"file\" />\\-\\->(.*?)</td>.*?</tr>.*?<tr>.*?<th>.*?</th>.*?<td>(.*?)</td>", Pattern.DOTALL));
        String fileName = infos.getMatch(0);
        String fileSize = infos.getMatch(1);
        if (fileName != null) {
            link.setName(Encoding.htmlDecode(fileName).trim());
        }
        if (fileSize != null) {
            link.setDownloadSize(SizeFormatter.getSize(fileSize.trim()));
        }
        if (br.containsHTML("(?i)All threads for IP")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
        } else if (br.containsHTML("(?i)Due to technical reasons, file is temporarily not available")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Due to technical reasons, file is temporarily not available.");
        } else if (br.containsHTML("<div id=\"page_error\">") && !br.containsHTML("(?i)To download this file please ")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (fileName == null || fileSize == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        return AvailableStatus.TRUE;
    }

    public void doFree(final DownloadLink link) throws Exception {
        if (this.br.containsHTML("To download this file please")) {
            /*
             * E.g. html:
             * "To download this file please <a href=/reg>register</a>. It is fast, free, and assumes no obligations.        </p>"
             */
            throw new AccountRequiredException();
        }
        br.setFollowRedirects(true);
        for (int i = 1; i <= 3; i++) {
            final Form dlform = br.getFormbyActionRegex(".*dl/.*");
            if (dlform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final String captchaKey = (int) (Math.random() * 100000000) + "";
            final String captchaUrl = "/img/captcha.gif?x=" + captchaKey;
            final String captchaCode = getCaptchaCode(captchaUrl, link);
            if (captchaCode == null || !captchaCode.matches("[0-9]{6}")) {
                logger.info("Wrong captcha code format");
                continue;
            }
            dlform.put("captcha_key", captchaKey);
            dlform.put("captcha", Encoding.urlEncode(captchaCode));
            dlform.put("download", "Download");
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlform, false, 1);
            if (this.looksLikeDownloadableContent(dl.getConnection())) {
                break;
            } else {
                br.followConnection();
                final String waittimeString = br.getRegex("(?i)You will get ability to download next file after.*?\\.\\s*</b>").getMatch(-1);
                if (waittimeString != null) {
                    final String hours = new Regex(waittimeString, "(\\d+)\\s*hr\\.").getMatch(0);
                    final String minutes = new Regex(waittimeString, "(\\d+)\\s*min\\.").getMatch(0);
                    final String seconds = new Regex(waittimeString, "(\\d+)\\s*sec\\.").getMatch(0);
                    long time = 0;
                    if (hours != null) {
                        time += Long.parseLong(hours) * 60 * 60 * 1000l;
                    }
                    if (minutes != null) {
                        time += Long.parseLong(minutes) * 60 * 1000l;
                    }
                    if (seconds != null) {
                        time += Long.parseLong(seconds) * 1000l;
                    }
                    if (time > 0) {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, time);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
                    }
                }
                this.dl = null;
                continue;
            }
        }
        if (this.dl == null) {
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }
        dl.startDownload();
    }

    protected void handleErrors(final Browser br, final boolean downloading) throws PluginException {
        if (br.containsHTML("(?i)All threads for IP")) {
            if (downloading) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.gigapeta.unavailable", "Your IP is already downloading a file"));
            }
        } else if (br.containsHTML("(?i)Due to technical reasons, file is temporarily not available.")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Due to technical reasons, file is temporarily not available.");
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        this.login(account, true);
        final AccountInfo ai = new AccountInfo();
        final String expire = br.getRegex("You have <b>premium</b> account till(.*?)</p>").getMatch(0);
        if (expire != null) {
            account.setType(AccountType.PREMIUM);
            ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy HH:mm", null));
            if (!ai.isExpired()) {
                account.setMaxSimultanDownloads(-1);
            }
        }
        if (br.containsHTML("(?i)You have <b>basic</b> account") || ai.isExpired()) {
            account.setMaxSimultanDownloads(1);
            account.setConcurrentUsePossible(false);
            account.setType(AccountType.FREE);
        }
        ai.setUnlimitedTraffic();
        return ai;
    }

    private boolean isLoggedIN(final Browser br) {
        if (br.containsHTML("id=\"authinfo\"") || br.containsHTML("\\?exit")) {
            return true;
        } else {
            return false;
        }
    }

    public String getAGBLink() {
        return "http://gigapeta.com/rules/";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        doFree(link);
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (AccountType.FREE.equals(account.getType())) {
            handleFree(link);
        } else {
            br.setFollowRedirects(true);
            dl = new jd.plugins.BrowserAdapter().openDownload(br, link, link.getPluginPatternMatcher(), true, -6);
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                handleErrors(br, true);
                Form dlform = br.getFormBySubmitvalue("Download");
                if (dlform == null) {
                    dlform = br.getForm(0);
                    if (dlform == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    }
                }
                dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dlform, true, -6);
            }
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                handleErrors(br, true);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        }
    }

    private void login(final Account account, boolean force) throws Exception {
        this.setBrowserExclusive();
        this.prepBR(br);
        /*
         * Workaround for a serverside 502 error (date: 04.03.15). Accessing the wrong ('/dl/') link next line in the code will return a 404
         * error but we can login and download fine then.
         */
        final Cookies cookies = account.loadCookies("");
        if (cookies != null) {
            br.setCookies(cookies);
            if (!force) {
                /* Do not validate cookies */
                return;
            }
            logger.info("Validating cookies");
            br.getPage("http://" + this.getHost() + "/");
            if (this.isLoggedIN(br)) {
                logger.info("Cookie login successful");
                account.saveCookies(br.getCookies(br.getHost()), "");
                return;
            } else {
                logger.info("Cookie login failed");
                br.clearCookies(br.getHost());
            }
        }
        br.getPage("http://" + this.getHost() + "/dl/");
        Form loginform = this.getLoginForm(br);
        if (loginform == null) {
            br.getPage("http://" + this.getHost() + "/");
            loginform = this.getLoginForm(br);
        }
        if (loginform == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        loginform.put("auth_login", Encoding.urlEncode(account.getUser()));
        loginform.put("auth_passwd", Encoding.urlEncode(account.getPass()));
        br.submitForm(loginform);
        if (!this.isLoggedIN(br)) {
            throw new AccountInvalidException();
        }
        account.saveCookies(br.getCookies(br.getHost()), "");
    }

    private Form getLoginForm(final Browser br) {
        return br.getFormbyKey("auth_token");
    }

    @Override
    public boolean isHosterManipulatesFilenames() {
        return true;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return true;
        } else if (AccountType.FREE.equals(acc.getType())) {
            /* free accounts also have captchas */
            return true;
        } else {
            return false;
        }
    }
}