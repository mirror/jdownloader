//jDownloader - Downloadmanager
//Copyright (C) 2016  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.components.UserAgents;
import jd.plugins.components.UserAgents.BrowserName;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.captcha.v2.challenge.recaptcha.v1.Recaptcha;
import org.jdownloader.captcha.v2.challenge.recaptcha.v2.CaptchaHelperHostPluginRecaptchaV2;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "pobierz.to" }, urls = { "https?://(?:www\\.)?pobierz\\.to/[A-Za-z0-9]+" })
public class PobierzTo extends PluginForHost {

    public PobierzTo(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium(mainpage + "/upgrade." + type);
    }

    /**
     * For sites which use this script: http://www.yetishare.com/<br />
     * YetiShareBasic Version 0.9.2-psp<br />
     * mods: Heavily modified - do NOT upgrade!<br />
     * limit-info:<br />
     * protocol: https<br />
     * captchatype: reCaptchaV2<br />
     * other: alternative linkcheck#2: statistics URL: host.tld/<fid>~s<br />
     */

    @Override
    public String getAGBLink() {
        return mainpage + "/terms." + type;
    }

    /* Basic constants */
    private final String                   mainpage                                     = "http://pobierz.to";
    private final String                   domains                                      = "(pobierz\\.to)";
    private final String                   type                                         = "html";
    private static final int               wait_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT = 10;
    private static final int               additional_WAIT_SECONDS                      = 3;
    private static final int               directlinkfound_WAIT_SECONDS                 = 10;
    private static final boolean           supportshttps                                = true;
    private static final boolean           supportshttps_FORCED                         = true;
    /* In case there is no information when accessing the main link */
    private static final boolean           available_CHECK_OVER_INFO_PAGE               = false;
    private static final boolean           useOldLoginMethod                            = false;
    private static final boolean           enable_RANDOM_UA                             = false;

    private static final boolean           enable_embed                                 = false;
    private static final boolean           enable_regex_stream_url                      = true;

    /* Known urlErrors */
    private static final String            url_ERROR_SIMULTANDLSLIMIT                   = ".*?e=You\\+have\\+reached\\+the\\+maximum\\+concurrent\\+downloads.*?";
    private static final String            url_ERROR_SERVER                             = ".*?e=Error%3A\\+Could\\+not\\+open\\+file\\+for\\+reading.*?";
    private static final String            url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT       = ".*?e=(You\\+must\\+wait\\+|Musisz\\+poczeka%C4%87\\+\\d+\\+godzin%C4%99\\+pomi%C4%99dzy).*?";
    /* E.g. You+must+register+for+a+premium+account+to+download+files+of+this+size */
    /* E.g. You+must+register+for+a+premium+account+to+see+or+download+files.+Please+use+the+links+above+to+register+or+login. */
    private static final String            url_ERROR_PREMIUMONLY                        = "(.+e=You\\+must\\+register\\+for\\+a\\+premium\\+account\\+to.+|.+/register\\..+|.+wykup\\+konto\\+Premium.+)";
    /* Texts for the known errors */
    private static final String            errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT = "You must wait between downloads!";
    private static final String            errortext_ERROR_SERVER                       = "Server error";
    private static final String            errortext_ERROR_PREMIUMONLY                  = "This file can only be downloaded by premium (or registered) users";
    private static final String            errortext_ERROR_SIMULTANDLSLIMIT             = "Max. simultan downloads limit reached, wait to start more downloads from this host";

    /* Connection stuff */
    private static final boolean           free_RESUME                                  = true;
    private static final int               free_MAXCHUNKS                               = 1;
    private static final int               free_MAXDOWNLOADS                            = 1;
    private static final boolean           account_FREE_RESUME                          = true;
    private static final int               account_FREE_MAXCHUNKS                       = 1;
    private static final int               account_FREE_MAXDOWNLOADS                    = 1;
    private static final boolean           account_PREMIUM_RESUME                       = true;
    private static final int               account_PREMIUM_MAXCHUNKS                    = 0;
    private static final int               account_PREMIUM_MAXDOWNLOADS                 = 20;

    private static AtomicReference<String> agent                                        = new AtomicReference<String>(null);

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        /* link cleanup, but respect users protocol choosing or forced protocol */
        if (!supportshttps) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("https://", "http://"));
        } else if (supportshttps && supportshttps_FORCED) {
            link.setUrlDownload(link.getDownloadURL().replaceFirst("http://", "https://"));
        }
    }

    @SuppressWarnings("deprecation")
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        prepBrowser(this.br);
        final String fid = getFID(link);
        link.setLinkID(fid);

        // /* 2017-04-28: For some URLs, availibility information is only visible for loggedin (or only premium??) users. */
        // final Account aa = AccountController.getInstance().getValidAccount(this);
        // if (aa != null) {
        // this.login(aa, false);
        // }

        String filename;
        String filesize;
        if (available_CHECK_OVER_INFO_PAGE) {
            br.getPage(link.getDownloadURL() + "~i");
            if (!br.getURL().contains("~i") || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String[] tableData = this.br.getRegex("class=\"responsiveInfoTable\">([^<>\"/]*?)<").getColumn(0);
            /* Sometimes we get crippled results with the 2nd RegEx so use this one first */
            filename = this.br.getRegex("data\\-animation\\-delay=\"\\d+\">(?:Information about|Informacion) ([^<>\"]*?)</div>").getMatch(0);
            if (filename == null) {
                /* "Information about"-filename-trait without the animation(delay). */
                filename = this.br.getRegex("class=\"description\\-1\">Information about ([^<>\"]+)<").getMatch(0);
            }
            if (filename == null) {
                filename = this.br.getRegex("(?:Filename|Dateiname|اسم الملف|Nome):[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            }
            filesize = br.getRegex("(?:Filesize|Dateigröße|حجم الملف|Tamanho):[\t\n\r ]*?</td>[\t\n\r ]*?<td(?: class=\"responsiveInfoTable\")?>([^<>\"]*?)<").getMatch(0);
            try {
                /* Language-independant attempt ... */
                if (filename == null) {
                    filename = tableData[0];
                }
                if (filesize == null) {
                    filesize = tableData[1];
                }
            } catch (final Throwable e) {
            }
            if (filename == null || inValidate(Encoding.htmlDecode(filename).trim()) || Encoding.htmlDecode(filename).trim().equals("  ")) {
                /* Filename might not be available here either */
                filename = fid;
            }
        } else {
            br.getPage(link.getDownloadURL());
            if (new Regex(br.getURL(), Pattern.compile("Musisz\\+poczeka%C4%87\\+1\\+godzin%C4%99\\+pomi%C4%99dzy", Pattern.CASE_INSENSITIVE)).matches()) {
                link.setName(getFID(link));
                link.getLinkStatus().setStatusText(errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT);
                return AvailableStatus.TRUE;
            } else if (new Regex(br.getURL(), Pattern.compile(url_ERROR_SERVER, Pattern.CASE_INSENSITIVE)).matches()) {
                link.setName(getFID(link));
                link.getLinkStatus().setStatusText(errortext_ERROR_SERVER);
                return AvailableStatus.TRUE;
            } else if (new Regex(br.getURL(), Pattern.compile(url_ERROR_PREMIUMONLY, Pattern.CASE_INSENSITIVE)).matches()) {
                link.getLinkStatus().setStatusText(errortext_ERROR_PREMIUMONLY);
                return AvailableStatus.TRUE;
            }
            handleErrors();
            if (br.getURL().contains("/error." + type) || br.getURL().contains("/index." + type) || (!br.containsHTML("class=\"downloadPageTable(V2)?\"") && !br.containsHTML("class=\"download\\-timer\"")) || br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final Regex fInfo = br.getRegex("<strong>([^<>\"]*?) \\((\\d+(?:,\\d+)?(?:\\.\\d+)? (?:KB|MB|GB))\\)<");
            /* 2017-05-02: The filename in the usual place may contain unwanted spaces so we take it from the 'title' tag. */
            filename = this.br.getRegex("<title>([^<>\"]+) \\- Pobierz\\.to</title>").getMatch(0);
            if (filename == null) {
                filename = fInfo.getMatch(0);
            }
            filesize = fInfo.getMatch(1);
            if (filesize == null) {
                filesize = br.getRegex("(\\d+(?:,\\d+)?(\\.\\d+)? (?:KB|MB|GB))").getMatch(0);
            }
        }
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename).trim());
        link.setDownloadSize(SizeFormatter.getSize(Encoding.htmlDecode(filesize.replace(",", "")).trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null, free_RESUME, free_MAXCHUNKS, "free_directlink");
    }

    @SuppressWarnings("deprecation")
    public void doFree(final DownloadLink link, final Account aa, final boolean resume, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        boolean skipWaittime = false;
        String continue_link = null;
        boolean captcha = false;
        boolean success = false;
        final long timeBeforeDirectlinkCheck = System.currentTimeMillis();
        long timeBeforeCaptchaInput;
        continue_link = checkDirectLink(link, directlinkproperty);
        if (continue_link != null) {
            /*
             * Let the server 'calm down' (if it was slow before) otherwise it will thing that we tried to open two connections as we
             * checked the directlink before and return an error.
             */
            if ((System.currentTimeMillis() - timeBeforeDirectlinkCheck) > 1500) {
                sleep(directlinkfound_WAIT_SECONDS * 1000l, link);
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
        } else {
            if (enable_embed) {
                try {
                    final Browser br2 = this.br.cloneBrowser();
                    br2.getPage(String.format("/embed/u=%s/", this.getFID(link)));
                    continue_link = this.getStreamUrl(br2);
                } catch (final BrowserException e) {
                }
            }
            if (available_CHECK_OVER_INFO_PAGE && continue_link == null) {
                br.getPage(link.getDownloadURL());
            }
            if (continue_link == null) {
                handleErrors();
            }
            /* Passwords are usually before waittime. */
            handlePassword(link);
            /* Handle up to 3 pre-download pages before the (eventually existing) captcha */
            for (int i = 1; i <= 5; i++) {
                logger.info("Handling pre-download page #" + i);
                timeBeforeCaptchaInput = System.currentTimeMillis();
                if (continue_link == null || i > 1) {
                    continue_link = getContinueLink();
                }
                if (i == 1 && continue_link == null) {
                    logger.info("No continue_link available, plugin broken");
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else if (continue_link == null) {
                    logger.info("No continue_link available, stepping out of pre-download loop");
                    break;
                } else {
                    logger.info("Found continue_link, continuing...");
                }
                final String rcID = br.getRegex("recaptcha/api/noscript\\?k=([^<>\"]*?)\"").getMatch(0);
                /* 2017-20-04: Added workaround for Premium account + reCaptchaV2 */
                if (isDownloadlink(continue_link)) {
                    /*
                     * If we already found a downloadlink let's try to download it because html can still contain captcha html --> We don't
                     * need a captcha in this case for sure! E.g. host '3rbup.com'.
                     */
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
                } else if (br.containsHTML("data-sitekey=|g-recaptcha") && (aa == null || aa.getType() != AccountType.PREMIUM)) {
                    captcha = true;
                    final String recaptchaV2Response = new CaptchaHelperHostPluginRecaptchaV2(this, br).getToken();
                    success = true;
                    waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, "submit=Submit&submitted=1&d=1&capcode=false&g-recaptcha-response=" + recaptchaV2Response, resume, maxchunks);
                } else if (rcID != null) {
                    captcha = true;
                    success = false;
                    final Recaptcha rc = new Recaptcha(this.br, this);
                    rc.setId(rcID);
                    rc.load();
                    final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                    final String c = getCaptchaCode("recaptcha", cf, link);
                    waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, "submit=continue&submitted=1&d=1&recaptcha_challenge_field=" + rc.getChallenge() + "&recaptcha_response_field=" + c, resume, maxchunks);
                } else if (br.containsHTML("solvemedia\\.com/papi/")) {
                    captcha = true;
                    success = false;
                    logger.info("Detected captcha method \"solvemedia\" for this host");
                    final org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia sm = new org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia(br);
                    if (br.containsHTML("api\\-secure\\.solvemedia\\.com/")) {
                        sm.setSecure(true);
                    }
                    File cf = null;
                    try {
                        cf = sm.downloadCaptcha(getLocalCaptchaFile());
                    } catch (final Exception e) {
                        if (org.jdownloader.captcha.v2.challenge.solvemedia.SolveMedia.FAIL_CAUSE_CKEY_MISSING.equals(e.getMessage())) {
                            throw new PluginException(LinkStatus.ERROR_FATAL, "Host side solvemedia.com captcha error - please contact the " + this.getHost() + " support");
                        }
                        throw e;
                    }
                    final String code = getCaptchaCode("solvemedia", cf, link);
                    final String chid = sm.getChallenge(code);
                    waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, "submit=continue&submitted=1&d=1&adcopy_challenge=" + Encoding.urlEncode(chid) + "&adcopy_response=" + Encoding.urlEncode(code), resume, maxchunks);
                } else {
                    success = true;
                    waitTime(link, timeBeforeCaptchaInput, skipWaittime);
                    dl = jd.plugins.BrowserAdapter.openDownload(br, link, continue_link, resume, maxchunks);
                }
                checkResponseCodeErrors(dl.getConnection());
                if (dl.getConnection().isContentDisposition()) {
                    success = true;
                    break;
                }
                br.followConnection();
                handleErrors();
                if (captcha && br.containsHTML("(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)")) {
                    logger.info("Wrong captcha");
                    continue;
                }
            }
        }
        checkResponseCodeErrors(dl.getConnection());
        if (!dl.getConnection().isContentDisposition()) {
            br.followConnection();
            if (captcha && !success) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
            handleErrors();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        continue_link = dl.getConnection().getURL().toString();
        link.setProperty(directlinkproperty, continue_link);
        dl.startDownload();
    }

    private String getContinueLink() {
        String continue_link = br.getRegex("\\$\\(\\'\\.download\\-timer\\'\\)\\.html\\(\"<a href=\\'(https?://[^<>\"]*?)\\'").getMatch(0);
        if (continue_link == null) {
            continue_link = br.getRegex("class=\\'btn btn\\-free\\' href=\\'(https?://[^<>\"]*?)\\'>").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("<div class=\"captchaPageTable\">[\t\n\r ]+<form method=\"POST\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = br.getRegex("(?:\"|\\')(https?://(www\\.)?" + domains + "/[^<>\"]*?pt=[^<>\"]*?)(?:\"|\\')").getMatch(0);
        }
        if (continue_link == null) {
            continue_link = getDllink();
        }
        if (continue_link == null && enable_regex_stream_url) {
            continue_link = getStreamUrl();
        }
        return continue_link;
    }

    private String getDllink() {
        return getDllink(this.br);
    }

    private String getDllink(final Browser br) {
        return br.getRegex("\"(https?://(?:www\\.)?(?:[A-Za-z0-9\\.\\-]+\\.)?" + domains + "/[^<>\"\\?]*?\\?download_token=[A-Za-z0-9]+)\"").getMatch(0);
    }

    private String getStreamUrl() {
        return getStreamUrl(this.br);
    }

    private String getStreamUrl(final Browser br) {
        return br.getRegex("file\\s*?:\\s*?\"(https?://[^<>\"]+)\"").getMatch(0);
    }

    private boolean isDownloadlink(final String url) {
        final boolean isdownloadlink = url.contains("download_token=");
        return isdownloadlink;
    }

    private void handlePassword(final DownloadLink dl) throws PluginException, IOException {
        if (br.getURL().contains("/file_password.html")) {
            logger.info("Current link is password protected");
            String passCode = dl.getStringProperty("pass", null);
            if (passCode == null) {
                passCode = Plugin.getUserInput("Password?", dl);
                if (passCode == null || passCode.equals("")) {
                    logger.info("User has entered blank password, exiting handlePassword");
                    dl.setProperty("pass", Property.NULL);
                    throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
                }
                dl.setProperty("pass", passCode);
            }
            br.postPage(br.getURL(), "submit=access+file&submitme=1&file=" + this.getFID(dl) + "&filePassword=" + Encoding.urlEncode(passCode));
            if (br.getURL().contains("/file_password.html")) {
                logger.info("User entered incorrect password --> Retrying");
                dl.setProperty("pass", Property.NULL);
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            logger.info("User entered correct password --> Continuing");
        }
    }

    /** Handles pre download (pre-captcha) waittime. */
    private void waitTime(final DownloadLink downloadLink, final long timeBefore, final boolean skipWaittime) throws PluginException {
        if (skipWaittime) {
            logger.info("Skipping waittime");
        } else {
            int wait = 0;
            int passedTime = (int) ((System.currentTimeMillis() - timeBefore) / 1000) - 1;
            /* Ticket Time */
            String ttt = this.br.getRegex("\\$\\(\\'\\.download\\-timer\\-seconds\\'\\)\\.html\\((\\d+)\\);").getMatch(0);
            if (ttt == null) {
                /* Special */
                ttt = this.br.getRegex("var\\s*?seconds\\s*?= (\\d+);").getMatch(0);
            }
            if (ttt != null) {
                logger.info("Found waittime, parsing waittime: " + ttt);
                wait = Integer.parseInt(ttt) + additional_WAIT_SECONDS;
                wait -= passedTime;
                if (wait > 0) {
                    logger.info("Waittime minus captcha input time: " + wait);
                    sleep(wait * 1000l, downloadLink);
                } else {
                    logger.info("Waittime is zero or lower, not waiting");
                }
            } else {
                logger.info("Failed to find waittime, either there is none or plugin is out of date");
            }
        }
    }

    private void handleErrors() throws PluginException {
        if (br.containsHTML("Error: Too many concurrent download requests")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        } else if (new Regex(br.getURL(), Pattern.compile(url_ERROR_SIMULTANDLSLIMIT, Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, errortext_ERROR_SIMULTANDLSLIMIT, 1 * 60 * 1000l);
        } else if (br.getURL().contains("error.php?e=Error%3A+Could+not+open+file+for+reading")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
        } else if (new Regex(br.getURL(), Pattern.compile(url_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT, Pattern.CASE_INSENSITIVE)).matches()) {
            final String wait_minutes = new Regex(br.getURL(), "wait\\+(\\d+)\\+minutes?").getMatch(0);
            if (wait_minutes != null) {
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT, Integer.parseInt(wait_minutes) * 60 * 1001l);
            }
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, errortext_ERROR_WAIT_BETWEEN_DOWNLOADS_LIMIT, wait_BETWEEN_DOWNLOADS_LIMIT_MINUTES_DEFAULT * 60 * 1001l);
        } else if (new Regex(br.getURL(), Pattern.compile(url_ERROR_PREMIUMONLY, Pattern.CASE_INSENSITIVE)).matches()) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.getURL().contains("You+have+reached+the+maximum+permitted+downloads+in")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Daily limit reached", 3 * 60 * 60 * 1001l);
        } else if (br.toString().equals("unknown user")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 'Unknown user'", 30 * 60 * 1000l);
        }
        checkResponseCodeErrors(this.br.getHttpConnection());
    }

    /** Handles all kinds of error-responsecodes! */
    private void checkResponseCodeErrors(final URLConnectionAdapter con) throws PluginException {
        if (con == null) {
            return;
        }
        final long responsecode = con.getResponseCode();
        if (responsecode == 403) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 5 * 60 * 1000l);
        } else if (responsecode == 404) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 5 * 60 * 1000l);
        } else if (responsecode == 416) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 416", 2 * 60 * 1000l);
        } else if (responsecode == 429) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 429 connection limit reached, please contact our support!", 5 * 60 * 1000l);
        }
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            final Browser br2 = this.br.cloneBrowser();
            br2.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br2.openHeadConnection(dllink);
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

    @SuppressWarnings("deprecation")
    private String getFID(final DownloadLink dl) {
        return new Regex(dl.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
    }

    /**
     * Validates string to series of conditions, null, whitespace, or "". This saves effort factor within if/for/while statements
     *
     * @param s
     *            Imported String to match against.
     * @return <b>true</b> on valid rule match. <b>false</b> on invalid rule match.
     * @author raztoki
     */
    private boolean inValidate(final String s) {
        if (s == null || s != null && (s.matches("[\r\n\t ]+") || s.equals(""))) {
            return true;
        } else {
            return false;
        }
    }

    private String getProtocol() {
        if ((this.br.getURL() != null && this.br.getURL().contains("https://")) || supportshttps_FORCED) {
            return "https://";
        } else {
            return "http://";
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return free_MAXDOWNLOADS;
    }

    private Browser prepBrowser(final Browser br) {
        br.setAllowedResponseCodes(new int[] { 416, 429 });
        if (enable_RANDOM_UA) {
            if (agent.get() == null) {
                agent.set(UserAgents.stringUserAgent(BrowserName.Chrome));
            }
            br.getHeaders().put("User-Agent", agent.get());
        }
        return br;
    }

    private static final Object LOCK = new Object();

    private void login(final Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(this.br);
                br.setFollowRedirects(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.getPage(this.getProtocol() + this.getHost() + "/");
                final String lang = System.getProperty("user.language");
                final String loginstart = new Regex(br.getURL(), "(https?://(www\\.)?)").getMatch(0);
                if (useOldLoginMethod) {
                    br.postPage(this.getProtocol() + this.getHost() + "/login." + type, "submit=Login&submitme=1&loginUsername=" + Encoding.urlEncode(account.getUser()) + "&loginPassword=" + Encoding.urlEncode(account.getPass()));
                    if (br.containsHTML(">Your username and password are invalid<") || !br.containsHTML("/logout\\.html\">")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                } else {
                    br.getPage(this.getProtocol() + this.getHost() + "/login." + type);
                    final String loginpostpage = loginstart + this.getHost() + "/ajax/_account_login.ajax.php";
                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
                    br.postPage(loginpostpage, "username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (!br.containsHTML("\"login_status\":\"success\"")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                }
                br.getPage(loginstart + this.getHost() + "/account_home." + type);
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        if (!br.containsHTML("class=\"badge badge\\-success\">(?:PAID USER|USUARIO DE PAGO)</span>|<li class=\"green\">\\s*?<a href=\"[^\"]+\">\\s*?PREMIUM")) {
            account.setType(AccountType.FREE);
            account.setMaxSimultanDownloads(account_FREE_MAXDOWNLOADS);
            /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
            account.setConcurrentUsePossible(false);
            ai.setStatus("Registered (free) account");
            ai.setUnlimitedTraffic();
        } else {
            br.getPage("/account_edit." + type);

            /* If the premium account is expired we'll simply accept it as a free account. */
            String expire = br.getRegex("Reverts To Free Account:[\t\n\r ]+</td>[\t\n\r ]+<td>[\t\n\r ]+(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})").getMatch(0);
            if (expire == null) {
                /* More wide RegEx to be more language independant */
                expire = br.getRegex(">[\t\n\r ]*?(\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}:\\d{2})[\t\n\r ]*?<").getMatch(0);
            }
            if (expire == null) {
                account.setValid(false);
                return ai;
            }
            long expire_milliseconds = 0;
            expire_milliseconds = TimeFormatter.getMilliSeconds(expire, "dd/MM/yyyy hh:mm:ss", Locale.ENGLISH);
            if ((expire_milliseconds - System.currentTimeMillis()) <= 0) {
                account.setType(AccountType.FREE);
                account.setMaxSimultanDownloads(account_FREE_MAXDOWNLOADS);
                /* All accounts get the same (IP-based) downloadlimits --> Simultan free account usage makes no sense! */
                account.setConcurrentUsePossible(false);
                ai.setStatus("Registered (free) user");
                ai.setUnlimitedTraffic();
            } else {
                ai.setValidUntil(expire_milliseconds);
                account.setType(AccountType.PREMIUM);
                account.setMaxSimultanDownloads(account_PREMIUM_MAXDOWNLOADS);
                ai.setStatus("Premium account");
                /*
                 * Free accounts also show traffic related information at this position in the html code but that is the storage-space left
                 * of the account --> Show unlimited trafficleft for free accounts!
                 */
                final Regex trafficleftStuff = this.br.getRegex("<span>\\s*?Pobrano\\s*?<span>([^<>\"]+)</span>\\s*?/\\s*?([0-9\\.]+ (?:GB|MB|KB))");
                final String trafficUsedStr = trafficleftStuff.getMatch(0);
                final String trafficMaxDailyStr = trafficleftStuff.getMatch(1);
                if (trafficUsedStr != null && trafficMaxDailyStr != null) {
                    final long trafficUsed = SizeFormatter.getSize(trafficUsedStr);
                    final long trafficMaxDaily = SizeFormatter.getSize(trafficMaxDailyStr);
                    ai.setTrafficLeft(trafficMaxDaily - trafficUsed);
                    ai.setTrafficMax(trafficMaxDaily);
                } else {
                    ai.setUnlimitedTraffic();
                }
            }
        }
        account.setValid(true);
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            if (!available_CHECK_OVER_INFO_PAGE) {
                br.getPage(link.getDownloadURL());
            }
            doFree(link, account, account_FREE_RESUME, account_FREE_MAXCHUNKS, "free_acc_directlink");
        } else {
            /* 2017-04-20: New: Same handling as free here. */
            if (!available_CHECK_OVER_INFO_PAGE) {
                br.getPage(link.getDownloadURL());
            }
            doFree(link, account, account_FREE_RESUME, account_PREMIUM_MAXCHUNKS, "premium_acc_directlink");
            // String dllink = link.getDownloadURL();
            // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_PREMIUM_RESUME, account_PREMIUM_MAXCHUNKS);
            // checkResponseCodeErrors(dl.getConnection());
            // if (!dl.getConnection().isContentDisposition()) {
            // logger.warning("The final dllink seems not to be a file, checking for errors...");
            // br.followConnection();
            // handleErrors();
            // logger.info("Found no errors, let's see if we can find the dllink now...");
            // handlePassword(link);
            // dllink = this.getDllink();
            // if (dllink == null) {
            // dllink = getContinueLink();
            // }
            // if (dllink == null) {
            // handleErrors();
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, account_PREMIUM_RESUME, account_PREMIUM_MAXCHUNKS);
            // checkResponseCodeErrors(dl.getConnection());
            // }
            // if (!dl.getConnection().isContentDisposition()) {
            // logger.warning("The final dllink seems not to be a file!");
            // br.followConnection();
            // handleErrors();
            // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            // }
            // dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return account_PREMIUM_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.MFScripts_YetiShare;
    }

}