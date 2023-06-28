//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
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
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.UserAgents;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "https?://(www\\.)?4shared(?:-china)?\\.com/(account/)?(download|get|file|document|embed|photo|video|audio|mp3|office|rar|zip|archive|music|mobile)/[A-Za-z0-9\\-_]+(?:/.*)?|https?://api\\.4shared(-china)?\\.com/download/[A-Za-z0-9\\-_]+" })
public class FourSharedCom extends PluginForHost {
    // DEV NOTES:
    // old versions of JDownloader can have troubles with Java7+ with HTTPS posts.
    public final String                    PLUGINS_HOSTER_FOURSHAREDCOM_ONLY4PREMIUM = "plugins.hoster.foursharedcom.only4premium";
    private final String                   PASSWORDTEXT                              = "enter a password to access";
    private final String                   COOKIE_HOST                               = "http://4shared.com";
    private static final boolean           TRY_FAST_FREE                             = true;
    private static final String            type_api_direct                           = "https?://api\\.4shared(-china)?\\.com/download/[A-Za-z0-9\\-_]+";
    private static AtomicReference<String> agent                                     = new AtomicReference<String>();
    private String                         DLLINK                                    = null;

    public FourSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.4shared.com/ref/14368016/1");
        setConfigElements();
    }

    @Override
    public String[] siteSupportedNames() {
        return new String[] { "4shared.com", "4shared-china.com", "4shared" };
    }

    /**
     * TODO: Implement API: http://www.4shared.com/developer/docs/rest/resources/#files 19.12.12: Their support never responded so we don't
     * know how to use the API...
     */
    /* IMPORTANT: When checking logs, look for these elements: class="warn", "limitErrorMsg" */
    private static final String DOWNLOADSTREAMS              = "DOWNLOADSTREAMS";
    private static final String DOWNLOADSTREAMSERRORHANDLING = "DOWNLOADSTREAMSERRORHANDLING";
    private static final String NOCHUNKS                     = "NOCHUNKS";

    private Browser prepBrowser(final Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            agent.set(UserAgents.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setCookie(this.getHost(), "4langcookie", "en");
        br.setReadTimeout(3 * 60 * 1000);
        return prepBr;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com").replaceFirst("http:", "https:"));
        final String locale = new Regex(link.getDownloadURL(), "((\\?|\\&)locale=[a-z]+)").getMatch(0);
        /* Remove language parameters as they can destroy all of the errorhandling */
        if (locale != null) {
            link.setUrlDownload(link.getDownloadURL().replace(locale, ""));
        }
        if (link.getSetLinkID() == null) {
            String linkID = new Regex(link.getDownloadURL(), "\\.com/download/(.*?)/").getMatch(0);
            if (linkID != null) {
                link.setLinkID("download_" + linkID);
            } else {
                final String[] linkIDs = new Regex(link.getDownloadURL(), "(download|get|file|document|embed|photo|video|audio|mp3|office|rar|zip|archive|music|mobile)/([A-Za-z0-9\\-]+)").getRow(0);
                if (linkIDs != null && linkIDs.length > 0) {
                    link.setLinkID(linkIDs[0].toLowerCase(Locale.ENGLISH) + "_" + linkIDs[1]);
                }
            }
        }
        if (link.getDownloadURL().matches(type_api_direct)) {
            /* API directlink --> Don't touch it */
        } else {
            if (link.getDownloadURL().contains(".com/download")) {
                boolean fixLink = true;
                try {
                    final Browser br = new Browser();
                    prepBrowser(br);
                    br.setFollowRedirects(false);
                    br.getPage(link.getDownloadURL());
                    final String newLink = br.getRedirectLocation();
                    if (newLink != null) {
                        final String tmp = new Regex(newLink, "(.*?)(\\?|$)").getMatch(0);
                        if (tmp != null) {
                            link.setUrlDownload(tmp);
                        } else {
                            link.setUrlDownload(newLink);
                        }
                        fixLink = false;
                    }
                } catch (final Throwable e) {
                }
                if (fixLink) {
                    String id = new Regex(link.getDownloadURL(), "\\.com/download/(.*?)/").getMatch(0);
                    if (id != null) {
                        link.setUrlDownload("https://www.4shared.com/file/" + id);
                    }
                }
            } else {
                link.setUrlDownload(link.getDownloadURL().replaceAll("red.com/[^/]+/", "red.com/file/").replace("account/", ""));
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "https://www.4shared.com/terms.jsp";
    }

    private String getStreamLinks() {
        String url = br.getRegex("<meta property=\"og:audio\" content=\"(https?://.*?)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("var (flvLink|mp4Link|oggLink|mp3Link|streamerLink) = '(https?://[^<>']{5,500})'").getMatch(1);
        }
        return url;
    }

    private String getNormalDownloadlink() {
        String url = br.getRegex("<div class=\"xxlarge bold\">[\t\n\r ]+<a class=\"linkShowD3.*?href='(https?://[^<>']+)'").getMatch(0);
        if (url == null) {
            url = br.getRegex("<a href=\"(https?://[^<>\"]*?)\" class=\"linkShowD3").getMatch(0);
        }
        if (url == null) {
            url = br.getRegex("<input type=\"hidden\" name=\"d3torrent\" value=\"(https?://dc\\d+\\.4shared\\.com/download-torrent/[^<>\"]+)\"").getMatch(0);
            if (url != null) {
                url = url.replace("/download-torrent/", "/download/");
            }
            /** For registered users */
            if (url == null) {
                url = br.getRegex("<div class=\"xxlarge bold\">[\t\n\r ]+<a href=\"(https?://[^<>\"]+)\"").getMatch(0);
            }
        }
        return url;
    }

    private String getDirectDownloadlink() {
        String url = br.getRegex("size=\"tall\" annotation=\"inline\" width=\"200\" count=\"false\"[\t\n\r ]+href=\"(https?://[^<>\"]+)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("<a href=\"(https?://[^<>\"']+)\"  class=\"dbtn nt gaClick\" data-element").getMatch(0);
        }
        if (url == null) {
            url = br.getRegex("value=\"(https?://dc\\d+\\.4shared\\.com/download/[^<>\"]+)").getMatch(0);
        }
        return url;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        doFree(downloadLink, null);
    }

    @SuppressWarnings("deprecation")
    private void doFree(final DownloadLink link, final Account acc) throws Exception {
        String pass = null;
        int maxChunks = -10;
        if (link.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        if (!isDownloadURLSet(DLLINK)) {
            pass = handlePassword(link);
            DLLINK = checkDirectLink(link, "direct_link");
            if (!isDownloadURLSet(DLLINK)) {
                DLLINK = br.getRegex("id=\"btnLink\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
                /* 2016-11-11: Make sure not to use wrong urls as final urls here!! */
                if (DLLINK != null && DLLINK.matches(".+/get/.+\\.html$")) {
                    DLLINK = null;
                }
            }
            /* Not always needed */
            boolean wait = true;
            if (DLLINK == null && acc != null && TRY_FAST_FREE) {
                try {
                    final String host = new Regex(link.getDownloadURL(), "https?://(www\\.)?(4shared(-china)?\\.com)").getMatch(1);
                    final Browser cbr = new Browser();
                    cbr.getHeaders().put("User-Agent", "UniversalUserAgent(winHTTP)");
                    cbr.getPage("http://www." + host + "/downloadhelper/flink?login=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&url=" + Encoding.urlEncode(link.getDownloadURL()) + "&forDownloadHelper%3Dtrue%26lgfp%3D" + new Random().nextInt(10000));
                    DLLINK = cbr.getRegex("<url>(http[^<>\"]*?)</url>").getMatch(0);
                    if (DLLINK != null) {
                        logger.info("FAST-WAY worked|active!");
                        DLLINK = Encoding.htmlDecode(DLLINK);
                        wait = false;
                    }
                } catch (final Throwable e) {
                }
            }
            if (!isDownloadURLSet(DLLINK)) {
                handleErrors(acc, link);
                boolean downloadStreams = getPluginConfig().getBooleanProperty(DOWNLOADSTREAMS);
                if (link.getBooleanProperty("streamDownloadDisabled", false) == false && downloadStreams) {
                    DLLINK = getStreamLinks();
                    /* Shouldn't happen */
                    if (DLLINK != null && DLLINK.contains("4shared_Desktop_")) {
                        link.setProperty("streamDownloadDisable", true);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
                if (!isDownloadURLSet(DLLINK)) {
                    String continueLink = null;
                    if (!isDownloadURLSet(DLLINK)) {
                        /* If file isn't available for free users we can still try to get the stream link */
                        handleErrors(acc, link);
                        continueLink = br.getRegex("<a href=\"(https?://(?:www\\.)?4shared(-china)?\\.com/get[^\\;\"]+)\"  ?class=\".*?dbtn.*?\" tabindex=\"1\"").getMatch(0);
                        if (continueLink == null) {
                            continueLink = br.getRegex("\"(https?://(?:www\\.)?4shared(-china)?\\.com/get/[A-Za-z0-9\\-_]+/.*?)\"").getMatch(0);
                        }
                    }
                    if (continueLink == null) {
                        /* Maybe direct download */
                        DLLINK = getDirectDownloadlink();
                        if (DLLINK == null) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    } else {
                        br.getPage(continueLink);
                        handleErrors(acc, link);
                        DLLINK = getNormalDownloadlink();
                        if (!isDownloadURLSet(DLLINK)) {
                            DLLINK = getDirectDownloadlink();
                        }
                        boolean downloadStreamsErrorhandling = getPluginConfig().getBooleanProperty(DOWNLOADSTREAMSERRORHANDLING);
                        if (DLLINK == null && downloadStreamsErrorhandling) {
                            DLLINK = getStreamLinks();
                            wait = false;
                        }
                        if (DLLINK == null) {
                            if (br.containsHTML("onclick=\"return authenticate\\(event, \\{returnTo:") && !downloadStreamsErrorhandling) {
                                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.foursharedcom.tempunavailable1", "File only downloadable for registered/premium users at the moment [Or activate stream downloading and try again]"), 30 * 60 * 1000l);
                            }
                            if (br.containsHTML("onclick=\"return authenticate\\(event, \\{returnTo:") && downloadStreamsErrorhandling) {
                                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.foursharedcom.tempunavailable2", "File only downloadable for registered/premium users at the moment"), 30 * 60 * 1000l);
                            }
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                        if (DLLINK.contains("linkerror.jsp")) {
                            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                        }
                        if (wait) {
                            // Ticket Time
                            String ttt = br.getRegex(" var c = (\\d+);").getMatch(0);
                            if (ttt == null) {
                                ttt = br.getRegex("id=\"downloadDelayTimeSec\"\\s+?class=\"sec( alignCenter light-blue)?\">(\\d+)</div>").getMatch(1);
                            }
                            int tt = 20;
                            if (ttt != null) {
                                tt = Integer.parseInt(ttt);
                                logger.info("Waiting " + ttt.trim() + " seconds from now on...");
                            } else {
                                logger.info("Wait time regex fails, default 20 seconds is used");
                            }
                            sleep(tt * 1000l, link);
                        }
                    }
                }
            }
            br.setDebug(true);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, maxChunks);
        /**
         * Maybe download failed because we got a wrong directlink, disable getting directlinks first, if it then fails again the correct
         * error message is shown
         */
        if (br.getURL().contains("401waitm") && link.getBooleanProperty("streamDownloadDisabled", false)) {
            link.setProperty("streamDownloadDisabled", true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (br.getURL().contains("401waitm") && link.getBooleanProperty("streamDownloadDisabled", false)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unexpected error happened", 5 * 60 * 1000l);
        }
        final String error = br.getURL();
        if (error != null && error.contains("/linkerror.jsp")) {
            br.followConnection();
            dl.getConnection().disconnect();
            long retryCount = link.getLongProperty("retrycount_linkerror", 0);
            if (retryCount == 3) {
                /* Try 3 times, then do extended errorhandling */
                logger.info("linkerror occured more than 3 times --> Extended errorhandling");
                link.setProperty("retrycount_linkerror", Property.NULL);
                if (br.containsHTML("The file link that you requested is not valid")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, please contact the 4shared.com support!", 3 * 60 * 60 * 1000l);
            } else {
                retryCount++;
                link.setProperty("retrycount_linkerror", retryCount);
                throw new PluginException(LinkStatus.ERROR_RETRY, error);
            }
        }
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (link.getBooleanProperty("streamDownloadDisabled", false)) {
                link.setProperty("streamDownloadDisabled", true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            handleErrors(acc, link);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (pass != null) {
            link.setDownloadPassword(pass);
        }
        link.setProperty("direct_link", DLLINK);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (link.getBooleanProperty(FourSharedCom.NOCHUNKS, false) == false) {
                    link.setProperty(FourSharedCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && link.getBooleanProperty(FourSharedCom.NOCHUNKS, false) == false) {
                link.setProperty(FourSharedCom.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private void handleErrors(final Account account, final DownloadLink downloadLink) throws PluginException {
        // cau2=0759nousr&ua=LINUX&sop=true', title:"<div>You should log in to download this file.
        final String cau2 = new Regex(br.getRequest().getUrl(), "cau2=([^\\&]+)").getMatch(0);
        logger.info("cau2=" + cau2);
        if (br.containsHTML("0759nousr") || (br.containsHTML(">You should log in to download this") && !br.containsHTML("if\\s*\\(\\s*false\\s*\\)\\s*\\{\\s*return\\s*confirmDownloadVirusFile"))) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } else if (br.containsHTML("In order to download files bigger that \\d+MB you need to login at 4shared")) {
            // links too large!
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        // Incorrect msg: <div class="limitErrorMsg"> <span>Sorry, the file link that you requested is not valid.<br/>
        if (br.containsHTML(">The download limit has been reached")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "The download limit has been reached", 60 * 60 * 1000l);
        }
        if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) {
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error - ongoing maintenance!", 60 * 60 * 1000l);
        }
        if (br.containsHTML("The file link that you requested is not valid|This file is no longer available because of a claim|This file is no longer available because it is identical to a file banned because of a claim|This file was deleted\\.<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String ttt = br.getRegex(" var c = (\\d+);").getMatch(0);
        if (ttt != null) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l);
        }
        if (cau2 != null) {
            if (cau2.equals("0322")) {
                /* 2017-02-03 */
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Server error 'Signature is invalid. Please, try again.'", 5 * 60 * 1000l);
            }
        }
    }

    private String handlePassword(DownloadLink link) throws Exception {
        String pass = link.getDownloadPassword();
        if (br.containsHTML(PASSWORDTEXT)) {
            Form pwform = br.getFormbyProperty("name", "theForm");
            if (pwform == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (pass == null) {
                pass = getUserInput(null, link);
            }
            pwform.put("userPass2", pass);
            br.submitForm(pwform);
            if (br.containsHTML(PASSWORDTEXT)) {
                /* Remove existing/stored download-password */
                link.setDownloadPassword(null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password wrong");
            }
        }
        return pass;
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

    private boolean isDownloadURLSet(final String url) {
        return url != null && !getSupportedLinks().matcher(url).matches();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        login(account, false);
        if (account.getType() == AccountType.FREE) {
            br.getPage(link.getDownloadURL());
            doFree(link, account);
        } else {
            String pass = null;
            br.setFollowRedirects(false);
            if (!isDownloadURLSet(DLLINK)) {
                br.getPage(link.getDownloadURL());
                DLLINK = br.getRedirectLocation();
                if (!isDownloadURLSet(DLLINK)) {
                    br.followRedirect();
                    pass = handlePassword(link);
                    // direct download or not?
                    DLLINK = br.getRedirectLocation();
                    if (!isDownloadURLSet(DLLINK)) {
                        br.followRedirect();
                        handleErrors(account, link);
                        DLLINK = br.getRegex("id=\"btnLink\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
                        if (DLLINK == null) {
                            DLLINK = br.getRegex("\"(https?://dc\\d+\\.4shared\\.com/download/[^<>\"]*?)\"").getMatch(0);
                        }
                        if (!isDownloadURLSet(DLLINK)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 0);
            final String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
            if (error != null) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY, error);
            }
            if (!this.looksLikeDownloadableContent(dl.getConnection())) {
                br.followConnection(true);
                if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            if (pass != null) {
                link.setDownloadPassword(pass);
            }
            dl.startDownload();
        }
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (account) {
            try {
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null) {
                    br.setCookies(cookies);
                    if (!force) {
                        logger.info("Trust cookies without login");
                        return;
                    } else {
                        /* Check cookie validity */
                        br.getPage("https://www.4shared.com/");
                        if (br.containsHTML("jsSignOutLink")) {
                            logger.info("Cookie login successful");
                            account.saveCookies(br.getCookies(br.getHost()), "");
                            return;
                        } else {
                            logger.info("Cookie login failed");
                            br.clearCookies(br.getHost());
                        }
                    }
                }
                logger.info("Performing full login");
                br.getPage("https://www.4shared.com/");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                final boolean ajax_login = false;
                if (ajax_login) {
                    final Browser br2 = br.cloneBrowser();
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br2.postPage("/web/login/validate", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (!br2.containsHTML("\"success\":true")) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                } else {
                    br.postPage("/web/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&returnTo=https%253A%252F%252Fwww.4shared.com%252Faccount%252Fhome.jsp&remember=on&_remember=on");
                }
                br.postPage("/web/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&_remember=on&returnTo=https%253A%252F%252Fwww.4shared.com%252Faccount%252Fhome.jsp");
                br.getHeaders().put("Content-Type", null);
                if (br.getCookie(this.getHost(), "ulin", Cookies.NOTDELETEDPATTERN) == null || !br.getCookie(this.getHost(), "ulin", Cookies.NOTDELETEDPATTERN).equals("true")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                account.saveCookies(br.getCookies(br.getHost()), "");
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
        if (true) {
            // they are not respecting the english cookie, they revert to account preferences once logged in!. plugin will fail if not
            // English!
            Browser br2 = br.cloneBrowser();
            br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br2.postPage("/web/user/language", "code=en");
            br2.getHeaders().put("X-Requested-With", null);
        }
        br.getPage("/web/account/settings/overview");
        if (br.getRedirectLocation() != null && br.getRedirectLocation().endsWith("/verifyemail")) {
            account.setValid(false);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, "Please verify your email account in your webbrowser: http://www.4shared.com/web/acc/verifyemail", PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        String expire = br.getRegex(">Expires in:.*?(\\d+) days<span><br>after last log in</span>").getMatch(0);
        if (expire == null) {
            expire = br.getRegex(">Expiration Date:.*\\s.*(Until Cancellation)<").getMatch(0);
            if (expire != null) {
                expire = "Until Cancellation";
            }
        }
        String accType = br.getRegex(">Account type:</div>[\t\n\r ]+<div[^>]+>(.*?)</div").getMatch(0);
        if (accType == null) {
            accType = br.getRegex("accountType : \"AccType = (\\w+)\"").getMatch(0);
        }
        final String usedSpace = br.getRegex(">Used space:</div>[\t\n\r ]+<div[^>]+>([0-9\\.]+(KB|MB|GB|TB)) of").getMatch(0);
        final String[] traffic = br.getRegex(">Premium traffic:</div>[\t\n\r ]+<div[^>]+>([0-9\\.]+(KB|MB|GB|TB)) of ([0-9\\.]+(KB|MB|GB|TB))").getRow(0);
        if (expire == null || accType == null) {
            logger.info("expire = " + expire + ", accType = " + accType);
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        }
        if (usedSpace != null) {
            ai.setUsedSpace(SizeFormatter.getSize(usedSpace));
        }
        if (traffic != null && traffic.length == 4) {
            ai.setTrafficLeft(SizeFormatter.getSize(traffic[2]) - SizeFormatter.getSize(traffic[0]));
            ai.setTrafficMax(SizeFormatter.getSize(traffic[2]));
        }
        if (!expire.equalsIgnoreCase("Until Cancellation")) {
            ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expire) * 24 * 60 * 60 * 1000l));
        }
        if (!"Premium".equalsIgnoreCase(accType)) {
            ai.setStatus("Registered (free) User");
            account.setType(AccountType.FREE);
        } else {
            ai.setStatus(accType);
            account.setType(AccountType.PREMIUM);
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        DLLINK = null;
        try {
            correctDownloadLink(link);
            setBrowserExclusive();
            prepBrowser(br);
            // In case the link redirects to the finallink
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openHeadConnection(link.getDownloadURL());
                if (!this.looksLikeDownloadableContent(con)) {
                    logger.info("Detected normal link");
                    br.followConnection();
                } else {
                    logger.info("Detected directlink");
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                    DLLINK = link.getDownloadURL();
                    return AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            br.getPage(link.getDownloadURL());
            if (br.containsHTML("The file link that you requested is not valid|This file is no longer available because of a claim|This file was deleted.</")) {
                /* Find out of this is always there for offline links */
                if (br.containsHTML("class=\"warn\"")) {
                    logger.info("WARN class-element exists.");
                }
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = null, size = null;
            // need password?
            if (br.containsHTML(PASSWORDTEXT)) {
                link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.foursharedcom.passwordprotected", "This link is password protected"));
                filename = br.getRegex("id=\"fileNameText\">([^<>\"]*?)</h1>").getMatch(0);
            } else {
                filename = link.getStringProperty("decrypterfilename", null);
                /* First corrections for specific filetypes */
                if (br.containsHTML("MPEG Audio Stream") && filename == null) {
                    filename = br.getRegex("<title>([^<>\"]*?) - MP3 Download,").getMatch(0);
                    if (filename != null) {
                        filename = Encoding.htmlDecode(filename.trim()) + ".mp3";
                    }
                } else if (br.containsHTML("MPEG-4 Video File|Matroska Video File") && filename == null) {
                    filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                    if (filename != null) {
                        /* Full filename WITHOUT extension --> add extension */
                        filename += ".mp4";
                    } else {
                        /* Crippled filename WITH extension */
                        filename = br.getRegex("style=\"background-image: url\\(https?://[a-z0-9]+\\.4shared\\.com/img/[A-Za-z0-9]+/[A-Za-z0-9]+/([^<>\"]*?)\\)\"").getMatch(0);
                    }
                }
                if (filename == null) {
                    filename = br.getRegex("photos and media in one place\\. Access anytime from everywhere\\! Download \\&quot;([^<>\"]*?)\\&quot; at 4shared' />").getMatch(0);
                }
                if (filename == null) {
                    // json
                    filename = PluginJSonUtils.getJsonValue(br, "filename");
                }
                if (filename == null) {
                    /* 2019-08-30 */
                    filename = br.getRegex("data-cp-fileName=\"([^<>\"]+)\"").getMatch(0);
                }
                /* Here, extension might be missing */
                if (filename == null) {
                    filename = br.getRegex("<h1 class=\"fileName light-blue lucida f24\">([^<>\"]*?)</h1>").getMatch(0);
                }
                /* Get filename out of forum img code - seems like the best way so far */
                if (filename == null) {
                    filename = br.getRegex("value=\"\\[URL=https?://(www\\.)?4shared(-china)?\\.com/[^<>\"]*?\\.html\\]\\[IMG\\]https?://dc\\d+\\.4shared(-china)?\\.com/img/[A-Za-z0-9]+/[A-Za-z0-9]+/([^<>\"]*?)\\[/IMG\\]\\[/URL\\]\" readonly=\"readonly\"").getMatch(3);
                }
                /* Get filename out of forum url code - seems like the best way so far */
                if (filename == null) {
                    filename = br.getRegex("value=\"\\[URL=https?://(www\\.)?4shared(-china)?\\.com/[^<>\"]*?\\.html\\]([^<>\"]*?)\\[/URL\\]\" readonly=\"readonly\"").getMatch(2);
                }
                /* This one might gets you a double extension (.rar.rar) */
                if (filename == null) {
                    filename = br.getRegex("trinityConfigInit\\('[A-Za-z0-9\\-_]+', '([^<>\"]*?)',").getMatch(0);
                }
                /* Here, extension might be missing */
                if (filename == null) {
                    filename = br.getRegex("class=\"greylink1 gaClick\" data-element=\"t1\">([^<>\"]*?)</a>").getMatch(0);
                }
                /* Here, extension might be missing */
                if (filename == null) {
                    filename = br.getRegex("property=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
                }
                if (filename == null) {
                    filename = br.getRegex(Pattern.compile("id=\"fileNameTextSpan\">(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("<title>(.*?) - 4shared\\.com - online file sharing and storage - download</title>").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("<h1 id=\"fileNameText\">(.*?)</h1>").getMatch(0);
                        }
                    }
                }
                size = br.getRegex("<td class=\"finforight lgraybox\" style=\"border-top:1px #dddddd solid\">([0-9,]+ [a-zA-Z]+)</td>").getMatch(0);
                if (size == null) {
                    size = br.getRegex("<span title=\"Size: (.*?)\">").getMatch(0);
                    // For mp3 stream- and maybe also normal stream links
                    if (size == null) {
                        size = br.getRegex("class=\"fileOwner dark-gray lucida f11\">[^<>\"/]*?</a>([^<>\"]*?) \\|").getMatch(0);
                    }
                    if (size == null) {
                        size = br.getRegex("fileInfo light-gray f11 floatLeft\">([^<>\"]*?) \\|").getMatch(0);
                    }
                }
            }
            if (filename == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /** Server sometimes sends bad filenames */
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (size != null) {
                link.setDownloadSize(SizeFormatter.getSize(size.replace(",", "")));
            }
            return AvailableStatus.TRUE;
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMS, JDL.L("plugins.hoster.foursharedcom.downloadstreams", "Download video/audio streams if available (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMSERRORHANDLING, JDL.L("plugins.hoster.foursharedcom.activateerrorhandling", "Only download video/audio streams if normal file is not available (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}