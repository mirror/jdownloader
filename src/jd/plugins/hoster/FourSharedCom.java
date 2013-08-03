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
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
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
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.os.CrossSystem;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "https?://(www\\.)?4shared(\\-china)?\\.com/(account/)?(download|get|file|document|embed|photo|video|audio|mp3|office|rar|zip|archive|music)/.+?/.*" }, flags = { 2 })
public class FourSharedCom extends PluginForHost {

    // DEV NOTES:
    // old versions of JDownloader can have troubles with Java7+ with HTTPS posts.

    public final String            PLUGINS_HOSTER_FOURSHAREDCOM_ONLY4PREMIUM = "plugins.hoster.foursharedcom.only4premium";
    private static StringContainer agent                                     = new StringContainer();
    private final String           PASSWORDTEXT                              = "enter a password to access";
    private static Object          LOCK                                      = new Object();
    private final String           COOKIE_HOST                               = "http://4shared.com";

    public FourSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.4shared.com/ref/14368016/1");
        setConfigElements();
    }

    /**
     * TODO: Implement API: http://www.4shared.com/developer/ 19.12.12: Their support never responded so we don't know how to use the API...
     * */
    private static final String DOWNLOADSTREAMS              = "DOWNLOADSTREAMS";
    private static final String DOWNLOADSTREAMSERRORHANDLING = "DOWNLOADSTREAMSERRORHANDLING";

    public static class StringContainer {
        public String string = null;
    }

    private Browser prepBrowser(Browser prepBr) {
        if (agent.string == null) {
            /*
             * we first have to load the plugin, before we can reference it
             */
            JDUtilities.getPluginForHost("mediafire.com");
            agent.string = jd.plugins.hoster.MediafireCom.stringUserAgent();
        }
        prepBr.getHeaders().put("User-Agent", agent.string);
        prepBr.setCookie("http://www.4shared.com", "4langcookie", "en");
        return prepBr;
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com").replaceFirst("https:", "http:"));
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

    @Override
    public String getAGBLink() {
        return "http://www.4shared.com/terms.jsp";
    }

    private String getStreamLinks() {
        String url = br.getRegex("<meta property=\"og:audio\" content=\"(http://.*?)\"").getMatch(0);
        if (url == null) {
            url = br.getRegex("var (flvLink|mp4Link|oggLink|mp3Link|streamerLink) = \\'(http://[^<>\"\\']{5,500})\\'").getMatch(1);
        }
        return url;
    }

    private String getNormalDownloadlink() {
        String url = br.getRegex("<div class=\"xxlarge bold\">[\t\n\r ]+<a class=\"linkShowD3.*?href=\\'(http://[^<>\"\\']+)\\'").getMatch(0);
        if (url == null) {
            url = br.getRegex("<input type=\"hidden\" name=\"d3torrent\" value=\"(http://dc\\d+\\.4shared\\.com/download\\-torrent/[^<>\"\\']+)\"").getMatch(0);
            if (url != null) url = url.replace("/download-torrent/", "/download/");
            /** For registered users */
            if (url == null) url = br.getRegex("<div class=\"xxlarge bold\">[\t\n\r ]+<a href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        }
        return url;
    }

    private String getDirectDownloadlink() {
        String url = br.getRegex("size=\"tall\" annotation=\"inline\" width=\"200\" count=\"false\"[\t\n\r ]+href=\"(http://[^<>\"\\']+)\"").getMatch(0);
        if (url == null) url = br.getRegex("<a href=\"(http://[^<>\"\\']+)\"  class=\"dbtn nt gaClick\" data\\-element").getMatch(0);
        if (url == null) url = br.getRegex("value=\"(http://dc\\d+\\.4shared\\.com/download/[^<>\"']+)").getMatch(0);
        return url;
    }

    private void checkErrors(Browser cbr) throws PluginException {
        if (cbr.containsHTML("In order to download files bigger that 500MB you need to login at 4shared"))
        // links too large!
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L(PLUGINS_HOSTER_FOURSHAREDCOM_ONLY4PREMIUM, "Files over 500MB are only downloadable for premiumusers!"));
        if (cbr.containsHTML("The file link that you requested is not valid\\.")) {
            // link has been removed
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
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
        doFree(downloadLink);
    }

    private void doFree(DownloadLink downloadLink) throws Exception {
        String pass = handlePassword(downloadLink);
        boolean downloadStreams = getPluginConfig().getBooleanProperty(DOWNLOADSTREAMS);
        String url = null;
        if (downloadLink.getStringProperty("streamDownloadDisabled") == null && downloadStreams) {
            url = getStreamLinks();
            /** Shouldn't happen */
            if (url != null && url.contains("4shared_Desktop_")) {
                downloadLink.setProperty("streamDownloadDisabled", "true");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
        }
        if (url == null) {
            // If file isn't available for free users we can still try to get the stream link
            checkErrors(br);
            url = br.getRegex("<a href=\"(http://(www\\.)?4shared(\\-china)?\\.com/get[^\\;\"]+)\"  ?class=\".*?dbtn.*?\" tabindex=\"1\"").getMatch(0);
            if (url == null) {
                url = br.getRegex("\"(http://(www\\.)?4shared(\\-china)?\\.com/get/[A-Za-z0-9\\-_]+/.*?)\"").getMatch(0);
            }
            if (url == null) {
                // Maybe direct download
                url = getDirectDownloadlink();
                if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                br.getPage(url);
                checkErrors(br);
                url = getNormalDownloadlink();
                if (url == null) url = getDirectDownloadlink();
                /** Will be disabled if we use stream links */
                boolean wait = true;
                boolean downloadStreamsErrorhandling = getPluginConfig().getBooleanProperty(DOWNLOADSTREAMSERRORHANDLING);
                if (url == null && downloadStreamsErrorhandling) {
                    url = getStreamLinks();
                    wait = false;
                }
                if (url == null) {
                    if (br.containsHTML("onclick=\"return authenticate\\(event, \\{returnTo:") && !downloadStreamsErrorhandling) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.foursharedcom.tempunavailable1", "File only downloadable for registered/premium users at the moment [Or activate stream downloading and try again]"), 30 * 60 * 1000l);
                    if (br.containsHTML("onclick=\"return authenticate\\(event, \\{returnTo:") && downloadStreamsErrorhandling) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, JDL.L("plugins.hoster.foursharedcom.tempunavailable2", "File only downloadable for registered/premium users at the moment"), 30 * 60 * 1000l);
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (url.contains("linkerror.jsp")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (wait) {
                    // Ticket Time
                    final String ttt = br.getRegex(" var c = (\\d+);").getMatch(0);
                    int tt = 20;
                    if (ttt != null) {
                        /* logger.info */System.out.println("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
                        tt = Integer.parseInt(ttt);
                    }
                    sleep(tt * 1000l, downloadLink);
                }
            }
        }
        br.setDebug(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, false, 1);
        /**
         * Maybe download failed because we got a wrong directlink, disable getting directlinks first, if it then fails again the correct
         * error message is shown
         */
        if (br.getURL().contains("401waitm") && downloadLink.getStringProperty("streamDownloadDisabled") == null) {
            downloadLink.setProperty("streamDownloadDisabled", "true");
            throw new PluginException(LinkStatus.ERROR_RETRY);
        } else if (br.getURL().contains("401waitm") && downloadLink.getStringProperty("streamDownloadDisabled") != null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Unexpected error happened", 5 * 60 * 1000l); }
        final String error = br.getURL();
        if (error != null && error.contains("/linkerror.jsp")) {
            dl.getConnection().disconnect();
            if (downloadLink.getLinkStatus().getRetryCount() == 3)
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster error, please contact the 4shared.com support!", 3 * 60 * 60 * 1000l);
            else
                throw new PluginException(LinkStatus.ERROR_RETRY, error);
        }
        if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (downloadLink.getStringProperty("streamDownloadDisabled") == null) {
                downloadLink.setProperty("streamDownloadDisabled", "true");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            handleFreeErrors(downloadLink);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (pass != null) downloadLink.setProperty("pass", pass);
        dl.startDownload();

    }

    private void handleFreeErrors(DownloadLink downloadLink) throws PluginException {
        if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short\\-time maintenance)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l); }
        String ttt = br.getRegex(" var c = (\\d+);").getMatch(0);
        if (ttt != null) { throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Too many simultan downloads", 5 * 60 * 1000l); }
        if (br.containsHTML("Sorry, the file link that you requested is not valid")) throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered/premium users!");
    }

    private String handlePassword(DownloadLink link) throws Exception {
        String pass = link.getStringProperty("pass");
        if (br.containsHTML(PASSWORDTEXT)) {
            Form pwform = br.getFormbyProperty("name", "theForm");
            if (pwform == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
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

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        login(account, false);
        br.getPage(downloadLink.getDownloadURL());
        if (account.getStringProperty("nopremium") != null) {
            doFree(downloadLink);
        } else {
            String pass = handlePassword(downloadLink);
            // direct download or not?
            String link = br.getRedirectLocation();
            if (link == null) {
                checkErrors(br);
                link = getDirectDownloadlink();
                if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, link, true, 0);
            final String error = new Regex(dl.getConnection().getURL(), "\\?error(.*)").getMatch(0);
            if (error != null) {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_RETRY, error);
            }
            if (!dl.getConnection().isContentDisposition() && dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                if (br.containsHTML("(Servers Upgrade|4shared servers are currently undergoing a short-time maintenance)")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 60 * 60 * 1000l); }
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            if (pass != null) downloadLink.setProperty("pass", pass);
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
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
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
                br.getPage("http://www.4shared.com/");
                br.setCookie("http://www.4shared.com", "4langcookie", "en");
                // stable does not send this header with post request!!!!!
                String protocol = "https://";
                if (isJava7nJDStable()) {
                    if (!stableSucks.get()) showSSLWarning(this.getHost());
                    protocol = "http://";
                }
                br.getHeaders().put("Content-Type", "application/x-www-form-urlencoded");
                Browser br2 = br.cloneBrowser();
                br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br2.postPage(protocol + "www.4shared.com/web/login/validate", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String lang = System.getProperty("user.language");
                if (!br2.containsHTML("\"success\":true")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.postPage(protocol + "www.4shared.com/web/login", "login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=true&returnTo=https%253A%252F%252Fwww.4shared.com%252Faccount%252Fhome.jsp");
                br.getHeaders().put("Content-Type", null);
                if (br.getCookie("http://www.4shared.com", "ulin") == null || !br.getCookie("http://www.4shared.com", "ulin").equals("true")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
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
        br.getPage("/web/account/settings/overview");
        final String expire = br.getRegex(">Expires in:</div>[\t\n\r ]+<div[^>]+>(\\d+) days</div>").getMatch(0);
        final String accType = br.getRegex(">Account type:</div>[\t\n\r ]+<div[^>]+>(.*?)</div").getMatch(0);
        final String usedSpace = br.getRegex(">Used space:</div>[\t\n\r ]+<div[^>]+>([0-9\\.]+(KB|MB|GB|TB)) of").getMatch(0);
        final String[] traffic = br.getRegex(">Premium traffic:</div>[\t\n\r ]+<div[^>]+>([0-9\\.]+(KB|MB|GB|TB)) of ([0-9\\.]+(KB|MB|GB|TB))").getRow(0);
        if (expire == null || accType == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        if (usedSpace != null) {
            ai.setUsedSpace(SizeFormatter.getSize(usedSpace));
        }
        if (traffic != null && traffic.length == 4) {
            ai.setTrafficLeft(SizeFormatter.getSize(traffic[2]) - SizeFormatter.getSize(traffic[0]));
            ai.setTrafficMax(SizeFormatter.getSize(traffic[2]));
        }
        ai.setValidUntil(System.currentTimeMillis() + (Long.parseLong(expire) * 24 * 60 * 60 * 1000l));
        if ("FREE  (<a href=\"/premium.jsp\">Upgrade</a>)".equalsIgnoreCase(accType)) {
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

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        try {
            setBrowserExclusive();
            prepBrowser(br);
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            // need password?
            if (br.containsHTML(PASSWORDTEXT)) downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.foursharedcom.passwordprotected", "This link is password protected"));
            if (br.containsHTML("The file link that you requested is not valid")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
            String filename = br.getRegex("title\" content=\"(.*?)\"").getMatch(0);
            if (filename == null) {
                filename = br.getRegex(Pattern.compile("id=\"fileNameTextSpan\">(.*?)</span>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<title>(.*?) \\- 4shared\\.com \\- online file sharing and storage \\- download</title>").getMatch(0);
                    if (filename == null) filename = br.getRegex("<h1 id=\"fileNameText\">(.*?)</h1>").getMatch(0);
                }
            }
            String size = br.getRegex("<td class=\"finforight lgraybox\" style=\"border\\-top:1px #dddddd solid\">([0-9,]+ [a-zA-Z]+)</td>").getMatch(0);
            if (size == null) {
                size = br.getRegex("<span title=\"Size: (.*?)\">").getMatch(0);
                // For mp3 stream- and maybe also normal stream links
                if (size == null) size = br.getRegex("class=\"fileOwner dark\\-gray lucida f11\">[^<>\"/]*?</a>([^<>\"]*?) \\|").getMatch(0);
            }
            if (filename == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMS, JDL.L("plugins.hoster.foursharedcom.downloadstreams", "Download video/audio streams if available (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FourSharedCom.DOWNLOADSTREAMSERRORHANDLING, JDL.L("plugins.hoster.foursharedcom.activateerrorhandling", "Only download video/audio streams if normal file is not available (faster download but this can decrease audio/video quality)")).setDefaultValue(false));
    }

    private boolean isJava7nJDStable() {
        if (System.getProperty("jd.revision.jdownloaderrevision") == null && System.getProperty("java.version").matches("1\\.[7-9].+"))
            return true;
        else
            return false;
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
                            if (xSystem)
                                message += "JDownloader 2 Installationsanleitung und Downloadlink: Klicke -OK- (per Browser oeffnen)\r\n ";
                            else
                                message += "JDownloader 2 Installationsanleitung und Downloadlink:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        } else {
                            title = domain + " :: Java 7+ && HTTPS Post Requests.";
                            message = "Due to a bug in Java 7+ when using this version of JDownloader, we can not succesfully send HTTPS Post Requests.\r\n";
                            message += "We have added a work around so you can continue to use this version of JDownloader...\r\n";
                            message += "Please be aware that HTTPS Post Requests are sent as HTTP. Use at your own discretion.\r\n";
                            message += "If you do not want to send unecrypted data, please upgrade to JDownloader 2!\r\n";
                            if (xSystem)
                                message += "Jdownloader 2 install instructions and download link: Click -OK- (open in browser)\r\n ";
                            else
                                message += "JDownloader 2 install instructions and download link:\r\n" + new URL("http://board.jdownloader.org/showthread.php?t=37365") + "\r\n";
                        }
                        int result = JOptionPane.showConfirmDialog(jd.gui.swing.jdgui.JDGui.getInstance().getMainFrame(), message, title, JOptionPane.CLOSED_OPTION, JOptionPane.CLOSED_OPTION);
                        if (xSystem && JOptionPane.OK_OPTION == result) CrossSystem.openURL(new URL("http://board.jdownloader.org/showthread.php?t=37365"));
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