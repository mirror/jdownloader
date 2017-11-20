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
import java.net.URL;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "https?://(www\\.)?4shared(-china)?\\.com/(account/)?(download|get|file|document|embed|photo|video|audio|mp3|office|rar|zip|archive|music)/.+?/.*|https?://api\\.4shared(-china)?\\.com/download/[A-Za-z0-9]+" })
public class FourSharedCom extends PluginForHost {
    // DEV NOTES:
    // old versions of JDownloader can have troubles with Java7+ with HTTPS posts.
    public final String                    PLUGINS_HOSTER_FOURSHAREDCOM_ONLY4PREMIUM = "plugins.hoster.foursharedcom.only4premium";
    private final String                   PASSWORDTEXT                              = "enter a password to access";
    private final String                   COOKIE_HOST                               = "http://4shared.com";
    private static Object                  LOCK                                      = new Object();
    private static final boolean           TRY_FAST_FREE                             = true;
    private static final String            type_api_direct                           = "https?://api\\.4shared(-china)?\\.com/download/[A-Za-z0-9]+";
    private static AtomicReference<String> agent                                     = new AtomicReference<String>();
    private String                         DLLINK                                    = null;

    public FourSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.4shared.com/ref/14368016/1");
        setConfigElements();
    }

    /**
     * TODO: Implement API: http://www.4shared.com/developer/docs/rest/resources/#files 19.12.12: Their support never responded so we don't
     * know how to use the API...
     */
    /* IMPORTANT: When checking logs, look for these elements: class="warn", "limitErrorMsg" */
    private static final String DOWNLOADSTREAMS              = "DOWNLOADSTREAMS";
    private static final String DOWNLOADSTREAMSERRORHANDLING = "DOWNLOADSTREAMSERRORHANDLING";
    private static final String NOCHUNKS                     = "NOCHUNKS";

    private Browser prepBrowser(Browser prepBr) {
        if (agent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            agent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        prepBr.getHeaders().put("User-Agent", agent.get());
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setCookie("http://www.4shared.com", "4langcookie", "en");
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
                final String[] linkIDs = new Regex(link.getDownloadURL(), "(download|get|file|document|embed|photo|video|audio|mp3|office|rar|zip|archive|music)/(.*?)/").getRow(0);
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
                        link.setUrlDownload("http://www.4shared.com/file/" + id);
                    }
                }
            } else {
                link.setUrlDownload(link.getDownloadURL().replaceAll("red.com/[^/]+/", "red.com/file/").replace("account/", ""));
            }
        }
    }

    @Override
    public String getAGBLink() {
        return "http://www.4shared.com/terms.jsp";
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
    private void doFree(final DownloadLink downloadLink, final Account acc) throws Exception {
        String pass = null;
        int maxChunks = -10;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        if (!isDownloadURLSet(DLLINK)) {
            pass = handlePassword(downloadLink);
            DLLINK = checkDirectLink(downloadLink, "direct_link");
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
                    final String host = new Regex(downloadLink.getDownloadURL(), "https?://(www\\.)?(4shared(-china)?\\.com)").getMatch(1);
                    final Browser cbr = new Browser();
                    cbr.getHeaders().put("User-Agent", "UniversalUserAgent(winHTTP)");
                    cbr.getPage("http://www." + host + "/downloadhelper/flink?login=" + Encoding.urlEncode(acc.getUser()) + "&password=" + Encoding.urlEncode(acc.getPass()) + "&url=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&forDownloadHelper%3Dtrue%26lgfp%3D" + new Random().nextInt(10000));
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
                handleErrors(acc, downloadLink);
                boolean downloadStreams = getPluginConfig().getBooleanProperty(DOWNLOADSTREAMS);
                if (downloadLink.getBooleanProperty("streamDownloadDisabled", false) == false && downloadStreams) {
                    DLLINK = getStreamLinks();
                    /* Shouldn't happen */
                    if (DLLINK != null && DLLINK.contains("4shared_Desktop_")) {
                        downloadLink.setProperty("streamDownloadDisable", true);
                        throw new PluginException(LinkStatus.ERROR_RETRY);
                    }
                }
                if (!isDownloadURLSet(DLLINK)) {
                    String continueLink = null;
                    if (!isDownloadURLSet(DLLINK)) {
                        /* If file isn't available for free users we can still try to get the stream link */
                        handleErrors(acc, downloadLink);
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
                        handleErrors(acc, downloadLink);
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
                            sleep(tt * 1000l, downloadLink);
                        }
                    }
                }
            }
            br.setDebug(true);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, maxChunks);
        /**
         * Maybe download failed because we got a wrong directlink, disable getting directlinks first, if it then fails again the correct
         * error message is shown
         */
        if (br.getURL().contains("401waitm") && downloadLink.getBooleanProperty("streamDownloadDisabled", false)) {
            downloadLink.setProperty("streamDownloadDisabled", true);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (br.getURL().contains("401waitm") && downloadLink.getBooleanProperty("streamDownloadDisabled", false)) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unexpected error happened", 5 * 60 * 1000l);
        }
        final String error = br.getURL();
        if (error != null && error.contains("/linkerror.jsp")) {
            br.followConnection();
            dl.getConnection().disconnect();
            long retryCount = getLongProperty(downloadLink, "retrycount_linkerror", 0);
            if (retryCount == 3) {
                /* Try 3 times, then do extended errorhandling */
                logger.info("linkerror occured more than 3 times --> Extended errorhandling");
                downloadLink.setProperty("retrycount_linkerror", Property.NULL);
                if (br.containsHTML("The file link that you requested is not valid")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error, please contact the 4shared.com support!", 3 * 60 * 60 * 1000l);
            } else {
                retryCount++;
                downloadLink.setProperty("retrycount_linkerror", retryCount);
                throw new PluginException(LinkStatus.ERROR_RETRY, error);
            }
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (downloadLink.getBooleanProperty("streamDownloadDisabled", false)) {
                downloadLink.setProperty("streamDownloadDisabled", true);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            handleErrors(acc, downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (pass != null) {
            downloadLink.setProperty("pass", pass);
        }
        downloadLink.setProperty("direct_link", DLLINK);
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(FourSharedCom.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(FourSharedCom.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(FourSharedCom.NOCHUNKS, false) == false) {
                downloadLink.setProperty(FourSharedCom.NOCHUNKS, Boolean.valueOf(true));
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
        String pass = link.getStringProperty("pass");
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
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password wrong");
            }
        }
        return pass;
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private boolean isDownloadURLSet(final String url) {
        return url != null && !getSupportedLinks().matcher(url).matches();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        if (account.getStringProperty("nopremium") != null) {
            br.getPage(downloadLink.getDownloadURL());
            doFree(downloadLink, account);
        } else {
            String pass = null;
            br.setFollowRedirects(false);
            if (!isDownloadURLSet(DLLINK)) {
                br.getPage(downloadLink.getDownloadURL());
                DLLINK = br.getRedirectLocation();
                if (!isDownloadURLSet(DLLINK)) {
                    br.followRedirect();
                    pass = handlePassword(downloadLink);
                    // direct download or not?
                    DLLINK = br.getRedirectLocation();
                    if (!isDownloadURLSet(DLLINK)) {
                        br.followRedirect();
                        handleErrors(account, downloadLink);
                        DLLINK = br.getRegex("id=\"btnLink\" href=\"(https?://[^<>\"]*?)\"").getMatch(0);
                        if (DLLINK == null) {
                            DLLINK = br.getRegex("\"(http://dc\\d+\\.4shared\\.com/download/[^<>\"]*?)\"").getMatch(0);
                        }
                        if (!isDownloadURLSet(DLLINK)) {
                            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        }
                    }
                }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
            final String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
            if (error != null) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY, error);
            }
            if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) {
                    throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l);
                }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (pass != null) {
                downloadLink.setProperty("pass", pass);
            }
            dl.startDownload();
        }
    }

    @SuppressWarnings("unchecked")
    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /** Load everything required login and fetchAccountInfo also! */
                br.setCookiesExclusive(true);
                prepBrowser(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie(COOKIE_HOST, key, value);
                        }
                        return;
                    }
                }
                br.forceDebug(true);
                br.setReadTimeout(3 * 60 * 1000);
                // stable does not send this header with post request!!!!!
                String protocol = "https://";
                if (isJava7nJDStable()) {
                    if (!stableSucks.get()) {
                        showSSLWarning(this.getHost());
                    }
                    protocol = "http://";
                }
                br.getPage(protocol + "www.4shared.com/");
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                final String lang = System.getProperty("user.language");
                final boolean ajax_login = false;
                if (ajax_login) {
                    final Browser br2 = br.cloneBrowser();
                    br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                    br2.postPage(protocol + "www.4shared.com/web/login/validate", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                    if (!br2.containsHTML("\"success\":true")) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                } else {
                    br.postPage(protocol + "www.4shared.com/web/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&returnTo=https%253A%252F%252Fwww.4shared.com%252Faccount%252Fhome.jsp&remember=on&_remember=on");
                }
                br.postPage(protocol + "www.4shared.com/web/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=on&_remember=on&returnTo=https%253A%252F%252Fwww.4shared.com%252Faccount%252Fhome.jsp");
                br.getHeaders().put("Content-Type", null);
                if (br.getCookie("http://www.4shared.com", "ulin") == null || !br.getCookie("http://www.4shared.com", "ulin").equals("true")) {
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /** Save cookies */
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies(COOKIE_HOST);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", Encoding.urlEncode(account.getUser()));
                account.setProperty("pass", Encoding.urlEncode(account.getPass()));
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
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
            account.setValid(true);
            account.setProperty("nopremium", true);
        } else {
            ai.setStatus(accType);
            account.setValid(true);
            account.setProperty("nopremium", Property.NULL);
        }
        return ai;
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        DLLINK = null;
        try {
            correctDownloadLink(downloadLink);
            setBrowserExclusive();
            prepBrowser(br);
            // In case the link redirects to the finallink
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                try {
                    if (isJDStable()) {
                        /* @since JD2 */
                        con = br.openHeadConnection(downloadLink.getDownloadURL());
                    } else {
                        /* Not supported in old 0.9.581 Stable */
                        con = br.openGetConnection(downloadLink.getDownloadURL());
                    }
                } catch (final BrowserException e) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                if (con.getContentType().contains("html")) {
                    logger.info("Detected normal link");
                    br.followConnection();
                } else {
                    logger.info("Detected directlink");
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(con)));
                    DLLINK = downloadLink.getDownloadURL();
                    return AvailableStatus.TRUE;
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
            br.getPage(downloadLink.getDownloadURL());
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
                downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.foursharedcom.passwordprotected", "This link is password protected"));
                filename = br.getRegex("id=\"fileNameText\">([^<>\"]*?)</h1>").getMatch(0);
            } else {
                filename = downloadLink.getStringProperty("decrypterfilename", null);
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
                        filename = br.getRegex("style=\"background-image: url\\(http://[a-z0-9]+\\.4shared\\.com/img/[A-Za-z0-9]+/[A-Za-z0-9]+/([^<>\"]*?)\\)\"").getMatch(0);
                    }
                }
                if (filename == null) {
                    filename = br.getRegex("photos and media in one place\\. Access anytime from everywhere\\! Download \\&quot;([^<>\"]*?)\\&quot; at 4shared' />").getMatch(0);
                }
                if (filename == null) {
                    // json
                    filename = PluginJSonUtils.getJsonValue(br, "filename");
                }
                /* Here, extension might be missing */
                if (filename == null) {
                    filename = br.getRegex("<h1 class=\"fileName light-blue lucida f24\">([^<>\"]*?)</h1>").getMatch(0);
                }
                /* Get filename out of forum img code - seems like the best way so far */
                if (filename == null) {
                    filename = br.getRegex("value=\"\\[URL=https?://(www\\.)?4shared(-china)?\\.com/[^<>\"]*?\\.html\\]\\[IMG\\]http://dc\\d+\\.4shared(-china)?\\.com/img/[A-Za-z0-9]+/[A-Za-z0-9]+/([^<>\"]*?)\\[/IMG\\]\\[/URL\\]\" readonly=\"readonly\"").getMatch(3);
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
            downloadLink.setFinalFileName(Encoding.htmlDecode(filename.trim()));
            if (size != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(size.replace(",", "")));
            }
            return AvailableStatus.TRUE;
        } catch (final Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private static long getLongProperty(final Property link, final String key, final long def) {
        try {
            return link.getLongProperty(key, def);
        } catch (final Throwable e) {
            try {
                Object r = link.getProperty(key, def);
                if (r instanceof String) {
                    r = Long.parseLong((String) r);
                } else if (r instanceof Integer) {
                    r = ((Integer) r).longValue();
                }
                final Long ret = (Long) r;
                return ret;
            } catch (final Throwable e2) {
                return def;
            }
        }
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMS, JDL.L("plugins.hoster.foursharedcom.downloadstreams", "Download video/audio streams if available (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMSERRORHANDLING, JDL.L("plugins.hoster.foursharedcom.activateerrorhandling", "Only download video/audio streams if normal file is not available (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
    }

    private boolean isJava7nJDStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+")) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isJDStable() {
        return System.getProperty("jd.revision.jdownloaderrevision") == null;
    }

    private static AtomicBoolean stableSucks = new AtomicBoolean(false);

    public static void showSSLWarning(final String domain) {
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    try {
                        String lng = System.getProperty("user.language");
                        String message = null;
                        String title = null;
                        boolean xSystem = CrossSystem.isOpenBrowserSupported();
                        if ("de".equalsIgnoreCase(lng)) {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Wegen einem Bug in in Java 7+ in dieser JDownloader version koennen wir keine HTTPS Post Requests ausfuehren.\r\n";
                            message += "Wir haben eine Notloesung ergaenzt durch die man weiterhin diese JDownloader Version nutzen kann.\r\n";
                            message += "Bitte bedenke, dass HTTPS Post Requests als HTTP gesendet werden. Nutzung auf eigene Gefahr!\r\n";
                            message += "Falls du keine unverschluesselten Daten versenden willst, update bitte auf JDownloader 2!\r\n";
                            if (xSystem) {
                                message += "JDownloader 2 Installationsanleitung und Downloadlink: Klicke -OK- (per Browser oeffnen)\r\n ";
                            } else {
                                message += "JDownloader 2 Installationsanleitung und Downloadlink:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                            }
                        } else if ("es".equalsIgnoreCase(lng)) {
                            title = domain + " :: Java 7+ && HTTPS Solicitudes Post.";
                            message = "Debido a un bug en Java 7+, al utilizar esta versión de JDownloader, no se puede enviar correctamente las solicitudes Post en HTTPS\r\n";
                            message += "Por ello, hemos añadido una solución alternativa para que pueda seguir utilizando esta versión de JDownloader...\r\n";
                            message += "Tenga en cuenta que las peticiones Post de HTTPS se envían como HTTP. Utilice esto a su propia discreción.\r\n";
                            message += "Si usted no desea enviar información o datos desencriptados, por favor utilice JDownloader 2!\r\n";
                            if (xSystem) {
                                message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación: Hacer Click en -Aceptar- (El navegador de internet se abrirá)\r\n ";
                            } else {
                                message += " Las instrucciones para descargar e instalar Jdownloader 2 se muestran a continuación, enlace :\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                            }
                        } else {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Due to a bug in Java 7+ when using this version of JDownloader, we can not successfully send HTTPS Post Requests.\r\n";
                            message += "We have added a work around so you can continue to use this version of JDownloader...\r\n";
                            message += "Please be aware that HTTPS Post Requests are sent as HTTP. Use at your own discretion.\r\n";
                            message += "If you do not want to send unecrypted data, please upgrade to JDownloader 2!\r\n";
                            if (xSystem) {
                                message += "Jdownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                            } else {
                                message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                            }
                        }
                        int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                        if (xSystem && JOptionPane.OK_OPTION == result) {
                            CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
                        }
                        stableSucks.set(true);
                    } catch (Throwable e) {
                    }
                }
            });
        } catch (Throwable e) {
        }
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