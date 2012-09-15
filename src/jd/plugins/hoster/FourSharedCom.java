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

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

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
import org.appwork.utils.formatter.TimeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "https?://(www\\.)?4shared(\\-china)?\\.com/(account/)?(download|get|file|document|photo|video|audio|mp3|office|rar|zip|archive|music)/.+?/.*" }, flags = { 2 })
public class FourSharedCom extends PluginForHost {

    public final String   PLUGINS_HOSTER_FOURSHAREDCOM_ONLY4PREMIUM = "plugins.hoster.foursharedcom.only4premium";
    private String        agent                                     = null;
    private final String  PASSWORDTEXT                              = "enter a password to access";
    private static Object LOCK                                      = new Object();
    private final String  COOKIE_HOST                               = "http://4shared.com";

    public FourSharedCom(final PluginWrapper wrapper) {
        super(wrapper);
        enablePremium("http://www.4shared.com/ref/14368016/1");
        setConfigElements();
    }

    private static final String DOWNLOADSTREAMS              = "DOWNLOADSTREAMS";
    private static final String DOWNLOADSTREAMSERRORHANDLING = "DOWNLOADSTREAMSERRORHANDLING";

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("\\.viajd", ".com").replaceFirst("https:", "http:"));
        if (link.getDownloadURL().contains(".com/download")) {
            boolean fixLink = true;
            try {
                final Browser br = new Browser();
                if (agent == null) {
                    /*
                     * we first have to load the plugin, before we can reference it
                     */
                    JDUtilities.getPluginForHost("mediafire.com");
                    agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
                }
                br.getHeaders().put("User-Agent", agent);
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
                String id = new Regex(link.getDownloadURL(), ".com/download/(.*?)/").getMatch(0);
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
        if (url == null) url = br.getRegex("href=\"(http://dc\\d+\\.4shared\\.com/download/[^<>\"\\']+)\"").getMatch(0);
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
        // If file isn't available for freeusers we can still try to get the
        // streamlink
        if (br.containsHTML("In order to download files bigger that 500MB you need to login at 4shared") && url == null) throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L(PLUGINS_HOSTER_FOURSHAREDCOM_ONLY4PREMIUM, "Files over 500MB are only downloadable for premiumusers!"));
        if (url == null) {
            url = br.getRegex("<a href=\"(http://(www\\.)?4shared(\\-china)?\\.com/get[^\\;\"]+)\"  ?class=\".*?dbtn.*?\" tabindex=\"1\"").getMatch(0);
            if (url == null) {
                url = br.getRegex("\"(http://(www\\.)?4shared(\\-china)?\\.com/get/[A-Za-z0-9\\-_]+/.*?)\"").getMatch(0);
            }
            if (url == null) {
                // Maybe directdownload
                url = getDirectDownloadlink();
                if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                br.getPage(url);
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
                        logger.info("Waittime detected, waiting " + ttt.trim() + " seconds from now on...");
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
        login(account, false);
        br.getPage(downloadLink.getDownloadURL());
        if (account.getStringProperty("nopremium") != null) {
            doFree(downloadLink);
        } else {
            String pass = handlePassword(downloadLink);
            // direct download or not?
            String link = br.getRedirectLocation();
            if (link == null) link = getDirectDownloadlink();
            if (link == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
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
                if (agent == null) {
                    /*
                     * we first have to load the plugin, before we can reference it
                     */
                    JDUtilities.getPluginForHost("mediafire.com");
                    agent = jd.plugins.hoster.MediafireCom.stringUserAgent();
                }
                br.getHeaders().put("User-Agent", agent);
                br.setReadTimeout(3 * 60 * 1000);
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
                br.getPage("http://www.4shared.com/");
                br.setCookie("http://www.4shared.com", "4langcookie", "en");
                br.postPage("http://www.4shared.com/login", "callback=jsonp" + System.currentTimeMillis() + "&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&remember=false&doNotRedirect=true");
                final String premlogin = br.getCookie("http://www.4shared.com", "premiumLogin");
                if (premlogin == null || !premlogin.contains("true")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
                if (br.getCookie("http://www.4shared.com", "Password") == null || br.getCookie("http://www.4shared.com", "Login") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
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
        br.forceDebug(true);
        login(account, true);
        final String redirect = br.getRegex("loginRedirect\":\"(http.*?)\"").getMatch(0);
        if (redirect == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.setFollowRedirects(true);
        br.getPage(redirect);
        final String[] dat = br.getRegex("Bandwidth\\:.*?<div class=\"quotacount\">(.+?)\\% of (.*?)</div>").getRow(0);
        if (dat != null && dat.length == 2) {
            ai.setTrafficMax(SizeFormatter.getSize(dat[1]));
            ai.setTrafficLeft((long) (ai.getTrafficMax() * (100.0 - Float.parseFloat(dat[0])) / 100.0));
        }
        final String accountDetails = br.getRegex("(/account/(myAccount|settingsAjax)\\.jsp\\?sId=([^\"]+))").getMatch(2);
        if (accountDetails == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
        br.getPage("http://www.4shared.com/account/myAccountAjax.jsp?sId=" + accountDetails);
        final String expire = br.getRegex("<td>Expiration Date:</td>.*?<td>(.*?)<span").getMatch(0);
        String accType = br.getRegex("Account Type:</td>.*?>(.*?)(&|<)").getMatch(0);
        if (expire == null || accType == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
        ai.setValidUntil(TimeFormatter.getMilliSeconds(expire.trim(), "dd.MM.yyyy", Locale.UK) + (24 * 60 * 60 * 1000l));
        accType = accType.trim();
        if ("FREE".equalsIgnoreCase(accType)) {
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
            br.getHeaders().put("User-Agent", agent);
            br.setCookie("http://www.4shared.com", "4langcookie", "en");
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
            String size = br.getRegex("<td class=\"finforight lgraybox\" style=\"border-top:1px #dddddd solid\">([0-9,]+ [a-zA-Z]+)</td>").getMatch(0);
            if (size == null) {
                size = br.getRegex("<span title=\"Size: (.*?)\">").getMatch(0);
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