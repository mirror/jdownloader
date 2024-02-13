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
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
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
import jd.plugins.components.SiteType.SiteTemplate;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "grabitshare.com" }, urls = { "https?://(?:www\\.)?grabitshare\\.com/((\\?d|download\\.php\\?id)=[A-Z0-9]+|([a-z]{2}/)?file/[0-9]+/)" })
public class GrabItShareCom extends PluginForHost {
    private static final String RECAPTCHATEXT    = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";
    private static final String CHEAPCAPTCHATEXT = "captcha\\.php";
    /* 2024-02-12: They do not support https lol */
    private static final String PROTOCOL         = "http://";

    public GrabItShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(PROTOCOL + "www." + getHost() + "/en/register.php");
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        br.setCustomCharset("utf-8");
        br.setCookie(getHost(), "mfh_mylang", "en");
        br.setCookie(getHost(), "yab_mylang", "en");
        return br;
    }

    // MhfScriptBasic 1.2, added new filesize-/name regexes & limit-reached text
    @Override
    public String getAGBLink() {
        return PROTOCOL + "www." + getHost() + "/rules.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return true;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return true;
        } else {
            /* Free(anonymous) and unknown account type */
            return false;
        }
    }

    public int getMaxChunks(final Account account) {
        final AccountType type = account != null ? account.getType() : null;
        if (AccountType.FREE.equals(type)) {
            /* Free Account */
            return 1;
        } else if (AccountType.PREMIUM.equals(type) || AccountType.LIFETIME.equals(type)) {
            /* Premium account */
            return -5;
        } else {
            /* Free(anonymous) and unknown account type */
            return 1;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, null);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        final String contenturl = link.getPluginPatternMatcher().replaceFirst("(?i)https://", PROTOCOL);
        if (account != null) {
            this.login(account, contenturl, true);
        } else {
            br.getPage(contenturl);
        }
        final String redirect = br.getRegex("<p>The document has moved <a href=\"(.*?)\">here</a>\\.</p>").getMatch(0);
        if (redirect != null) {
            br.getPage(redirect);
        }
        checkOffline(br);
        String filename = br.getRegex("title=\"Click this to report (.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<p align=\"center\"><b><font size=\"4\">(.*?)</font><font").getMatch(0);
        }
        String filesize = br.getRegex("<b>(File size|Filesize):</b></td>[\r\t\n ]+<td align=([\r\t\n ]+|(\")?left(\")?)>(.*?)</td>").getMatch(4);
        if (filesize == null) {
            filesize = br.getRegex("<b>\\&#4324;\\&#4304;\\&#4312;\\&#4314;\\&#4312;\\&#4321; \\&#4310;\\&#4317;\\&#4315;\\&#4304;:</b></td>[\t\r\n ]+<td align=left>(.*?)</td>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex("</font><font size=\"5\">\\&nbsp;</font>(.*?)\\&nbsp;\\&nbsp;").getMatch(0);
            }
        }
        if (filename != null && filename.matches("")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename != null) {
            link.setFinalFileName(filename.trim());
        } else {
            logger.warning("Failed to find filename");
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        } else {
            logger.warning("Failed to find filesize");
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        handleDownload(link, null);
    }

    private void handleDownload(final DownloadLink link, final Account account) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link, account);
        this.checkErrors(br);
        String passCode = null;
        Form captchaform = br.getFormbyProperty("name", "myform");
        if (captchaform == null) {
            captchaform = br.getFormbyProperty("name", "validateform");
            if (captchaform == null) {
                captchaform = br.getFormbyProperty("name", "valideform");
                if (captchaform == null) {
                    captchaform = br.getFormbyProperty("name", "verifyform");
                }
            }
        }
        if (captchaform != null) {
            if (br.containsHTML("class=textinput name=downloadpw") || br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CHEAPCAPTCHATEXT)) {
                for (int i = 0; i <= 3; i++) {
                    if (br.containsHTML(CHEAPCAPTCHATEXT)) {
                        logger.info("Found normal captcha");
                        String code = getCaptchaCode("/captcha.php", link);
                        captchaform.put("captchacode", code);
                    } else if (br.containsHTML(RECAPTCHATEXT)) {
                        logger.info("Found reCaptcha");
                        final Recaptcha rc = new Recaptcha(br, this);
                        rc.parse();
                        rc.load();
                        File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                        captchaform.put("recaptcha_challenge_field", rc.getChallenge());
                        captchaform.put("recaptcha_response_field", getCaptchaCode("recaptcha", cf, link));
                    }
                    if (br.containsHTML("class=textinput name=downloadpw")) {
                        if (link.getDownloadPassword() == null) {
                            passCode = getUserInput("Password?", link);
                        } else {
                            /* gespeicherten PassCode holen */
                            passCode = link.getDownloadPassword();
                        }
                        captchaform.put("downloadpw", passCode);
                    }
                    br.submitForm(captchaform);
                    if (br.containsHTML("Password Error")) {
                        logger.warning("Wrong password!");
                        link.setDownloadPassword(null);
                        continue;
                    }
                    checkErrors(br);
                    if (br.containsHTML("Captcha number error") || br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CHEAPCAPTCHATEXT)) {
                        logger.warning("Wrong captcha or wrong password!");
                        link.setDownloadPassword(null);
                        continue;
                    }
                    break;
                }
            } else {
                logger.info("No captcha required(?)");
                br.submitForm(captchaform);
            }
            checkErrors(br);
            if (br.containsHTML("Password Error")) {
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password");
            } else if (br.containsHTML("Captcha number error") || br.containsHTML(RECAPTCHATEXT) || br.containsHTML(CHEAPCAPTCHATEXT)) {
                logger.info("Wrong captcha or wrong password!");
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
        final Form continueform = br.getFormbyKey("downloadtype");
        if (continueform != null) {
            /* 2024-02-12: Premium download form */
            br.submitForm(continueform);
            checkErrors(br);
        }
        if (passCode != null) {
            link.setDownloadPassword(passCode);
        }
        final String finalLink = findLink(br);
        if (finalLink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, this.isResumeable(link, null), this.getMaxChunks(account));
        checkErrorsAfterDownloadAttempt();
        dl.startDownload();
    }

    private void checkOffline(final Browser br) throws PluginException {
        if (br.containsHTML("(?i)(Your requested file is not found|No file found)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void checkErrors(final Browser br) throws PluginException {
        checkOffline(br);
        if (br.containsHTML(">\\s*Pristup ovom sadržaju imaju samo")) {
            throw new AccountRequiredException();
        } else if (br.containsHTML("(You have got max allowed bandwidth size per hour|You have got max allowed download sessions from the same IP|\">Dostigli ste download limit\\. Pričekajte 1h za nastavak)")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, 10 * 60 * 1001l);
        }
    }

    private void checkErrorsAfterDownloadAttempt() throws PluginException, IOException {
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection();
            final UrlQuery query = UrlQuery.parse(br.getURL());
            final String errorStr = query.get("code");
            if (errorStr != null) {
                if (errorStr.equalsIgnoreCase("DL_FileNotFound")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                } else {
                    throw new PluginException(LinkStatus.ERROR_FATAL, errorStr);
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
    }

    public void login(final Account account, final String checkURL, final boolean validateCookies) throws Exception {
        synchronized (account) {
            /** Load cookies */
            br.setCookiesExclusive(true);
            final Cookies cookies = account.loadCookies("");
            if (cookies != null) {
                br.setCookies(cookies);
                /* Do not validate cookies */
                if (!validateCookies) {
                    return;
                } else {
                    logger.info("Validating cookies...");
                    br.getPage(checkURL);
                    if (isLoggedin(br)) {
                        logger.info("Cookie login successful");
                        account.saveCookies(br.getCookies(getHost()), "");
                        return;
                    } else {
                        logger.info("Cookie login failed");
                        br.clearCookies(null);
                    }
                }
            }
            br.getPage(PROTOCOL + "www." + getHost() + "/en/login.php");
            Form form = br.getFormbyProperty("name", "lOGIN");
            if (form == null) {
                form = br.getForm(0);
            }
            if (form == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            form.put("user", Encoding.urlEncode(account.getUser()));
            form.put("pass", Encoding.urlEncode(account.getPass()));
            // If the referer is still in the form (and if it is a valid
            // downloadlink) the download starts directly after logging in
            // so we
            // MUST remove it!
            form.remove("refer_url");
            form.put("autologin", "0");
            br.submitForm(form);
            br.getPage("/members.php");
            final String premium = br.getRegex("return overlay\\(this, \\'package_details\\',\\'width=\\d+px,height=\\d+px,center=1,resize=1,scrolling=1\\'\\)\">(Premium)</a>").getMatch(0);
            if (br.getCookie(getHost(), "mfh_passhash", Cookies.NOTDELETEDPATTERN) == null || "0".equals(br.getCookie(getHost(), "mfh_uid", Cookies.NOTDELETEDPATTERN))) {
                throw new AccountInvalidException();
            } else if (!isLoggedin(br)) {
                /* This should never happen */
                throw new AccountInvalidException();
            }
            if (premium == null) {
                account.setType(AccountType.FREE);
            } else {
                account.setType(AccountType.PREMIUM);
            }
            account.saveCookies(br.getCookies(getHost()), "");
            br.getPage(checkURL);
        }
    }

    private boolean isLoggedin(final Browser br) {
        return br.containsHTML("logout=1");
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        login(account, PROTOCOL + "www." + getHost() + "/en/members.php", true);
        String expired = getData("Aktivno");
        if (expired != null) {
            expired = expired.trim();
            if (expired.equalsIgnoreCase("No")) {
                ai.setExpired(false);
            } else if (expired.equalsIgnoreCase("Yes")) {
                ai.setExpired(true);
            }
        }
        String expires = getData("Vaše članstvo istiće");
        if (expires != null) {
            expires = expires.trim();
            if (expires.equals("Never") || expires.equals("Nikada")) {
                account.setType(AccountType.LIFETIME);
            } else {
                String[] e = expires.split("/");
                Calendar cal = new GregorianCalendar(Integer.parseInt("20" + e[2]), Integer.parseInt(e[1]) - 1, Integer.parseInt(e[0]));
                ai.setValidUntil(cal.getTimeInMillis());
            }
        }
        final String create = getData("Datum registracije");
        if (create != null) {
            String[] c = create.split("/");
            Calendar cal = new GregorianCalendar(Integer.parseInt("20" + c[2]), Integer.parseInt(c[1]) - 1, Integer.parseInt(c[0]));
            ai.setCreateTime(cal.getTimeInMillis());
        }
        return ai;
    }

    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        handleDownload(link, account);
    }

    private String getData(final String data) {
        String result = br.getRegex(">" + data + "</strong></li>[\t\n\r ]+<li class=\"col\\-w50\">([^<>\"]*?)</li>").getMatch(0);
        if (result == null) {
            result = br.getRegex("<b>" + data + "</b></td>[\t\n\r ]+<td align=left( width=\\d+px)?>([^<>\"]*?)</td>").getMatch(1);
        }
        return result;
    }

    private String findLink(final Browser br) throws Exception {
        final String newResult = br.getRegex("(https?://[a-z0-9\\-\\.]{5,30}/getfile\\.php\\?id=\\d+[^<>\"\\']*?)(\"|\\')").getMatch(0);
        if (newResult != null) {
            return newResult;
        }
        /* Old handling */
        String finalLink = br.getRegex("(https?://.{5,30}getfile\\.php\\?id=\\d+\\&a=[a-z0-9]+\\&t=[a-z0-9]+.*?)(\\'|\")").getMatch(0);
        if (finalLink == null) {
            String[] sitelinks = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), null);
            if (sitelinks == null || sitelinks.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (String alink : sitelinks) {
                alink = Encoding.htmlDecode(alink);
                if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                    finalLink = alink;
                    break;
                }
            }
        }
        return finalLink;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean hasAutoCaptcha() {
        return true;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc != null && AccountType.PREMIUM.equals(acc.getType())) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MhfScriptBasic;
    }
}