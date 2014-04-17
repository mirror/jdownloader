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
import java.util.HashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.Property;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "flickr.com" }, urls = { "http://(www\\.)?flickrdecrypted\\.com/photos/[^<>\"/]+/\\d+" }, flags = { 2 })
public class FlickrCom extends PluginForHost {

    public FlickrCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://flickr.com";
    }

    private static Object       LOCK      = new Object();
    private static final String MAINPAGE  = "http://flickr.com";
    private static final String intl      = "us";
    private static final String lang_post = "en-US";

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("https://www.flickr.com/" + new Regex(link.getDownloadURL(), "\\.com/(.+)").getMatch(0));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        correctDownloadLink(downloadLink);
        br.clearCookies(MAINPAGE);
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa != null) {
            login(aa, false, br);
        } else {
            logger.info("No account available, continuing without account...");
        }
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("div class=\"Four04Case\">") || br.containsHTML(">This member is no longer active on Flickr") || br.containsHTML("class=\"Problem\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getURL().contains("login.yahoo.com/config")) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable via account");
            return AvailableStatus.UNCHECKABLE;
        }
        String filename = getFilename();
        if (filename == null) {
            downloadLink.getLinkStatus().setStatusText("Only downloadable for registered users [Add a flickt account to download such links!]");
            logger.warning("Filename not found, plugin must be broken...");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("(photo\\-div video\\-div|class=\"video\\-wrapper\"|<meta name=\"medium\" content=\"video\")")) {
            final String lq = createGuid();
            final String secret = br.getRegex("photo_secret=(.*?)\\&").getMatch(0);
            final String nodeID = br.getRegex("data\\-comment\\-id=\"(\\d+\\-\\d+)\\-").getMatch(0);
            if (secret == null || nodeID == null || filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage("http://www.flickr.com/video_playlist.gne?node_id=" + nodeID + "&tech=flash&mode=playlist&lq=" + lq + "&bitrate=700&secret=" + secret + "&rd=video.yahoo.com&noad=1");
            final Regex parts = br.getRegex("<STREAM APP=\"(http://.*?)\" FULLPATH=\"(/.*?)\"");
            final String part1 = parts.getMatch(0);
            final String part2 = parts.getMatch(1);
            if (part1 == null || part2 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = part1 + part2.replace("&amp;", "&");
            if (DLLINK.contains(".mp4"))
                filename += ".mp4";
            else
                filename += ".flv";
        } else {
            br.getPage(downloadLink.getDownloadURL() + "/in/photostream");
            DLLINK = getFinalLink(new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0));
            if (DLLINK == null) DLLINK = br.getRegex("\"(https?://farm\\d+\\.(static\\.flickr|staticflickr)\\.com/\\d+/.*?)\"").getMatch(0);
            if (DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String ext = DLLINK.substring(DLLINK.lastIndexOf("."));
            if (ext == null || ext.length() > 5) ext = ".jpg";
            filename = Encoding.htmlDecode(filename.trim() + ext);
        }
        // Cut filenames if they're too long
        if (filename.length() > 180) {
            final String ext = filename.substring(filename.lastIndexOf("."));
            int extLength = ext.length();
            filename = filename.substring(0, 180 - extLength);
            filename += ext;
        }
        downloadLink.setFinalFileName(filename);
        Browser br2 = br.cloneBrowser();
        // In case the link redirects to the finallink
        br2.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br2.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            return AvailableStatus.TRUE;
        } finally {
            try {
                con.disconnect();
            } catch (Throwable e) {
            }
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        if (br.getURL().contains("login.yahoo.com/config")) {
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, false, br);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setStatus("Registered (free) User");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force, final Browser br) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                prepBr(br);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?>) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("cookie_epass") && cookies.containsKey("cookie_accid") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        final Browser brc = br.cloneBrowser();
                        if (isValid(brc)) return;
                        /* Clear existing cookies - get ready to do a full login */
                        br.clearCookies(MAINPAGE);
                    }
                }
                final String lang = System.getProperty("user.language");
                br.setFollowRedirects(true);
                br.getPage("https://www.flickr.com/signin/");
                for (int i = 1; i <= 5; i++) {
                    final String u = br.getRegex("type=\"hidden\" name=\"\\.u\" value=\"([^<>\"\\'/]+)\"").getMatch(0);
                    final String challenge = br.getRegex("type=\"hidden\" name=\"\\.challenge\" value=\"([^<>\"\\'/]+)\"").getMatch(0);
                    final String done = br.getRegex("type=\"hidden\" name=\"\\.done\" value=\"([^<>\"\\']+)\"").getMatch(0);
                    final String pd = br.getRegex("type=\"hidden\" name=\"\\.pd\" value=\"([^<>\"\\'/]+)\"").getMatch(0);
                    String action = br.getRegex("<form method=\"post\" action=\"(https?://login\\.yahoo.com/config/[^<>\"]*?)\"").getMatch(0);
                    if (u == null || challenge == null || done == null || pd == null || action == null) {
                        if ("de".equalsIgnoreCase(lang)) {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        } else {
                            throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                        }
                    }
                    // action = "https://login.yahoo.com/config/login";

                    br.getHeaders().put("X-Requested-With", "XMLHttpRequest");

                    final String post_data_basic = ".tries=1&.src=flickrsignin&.md5=&.hash=&.js=&.last=&promo=&.intl=" + intl + "&.lang=" + lang_post + "&.bypass=&.partner=&.u=" + u + "&.v=0&.challenge=" + challenge + "&.yplus=&.emailCode=&pkg=&stepid=&.ev=&hasMsgr=0&.chkP=Y&.done=" + Encoding.urlEncode(done) + "&.pd=" + Encoding.urlEncode(pd) + "&.ws=1&.cp=0&nr=0&pad=6&aad=6&login=" + Encoding.urlEncode(account.getUser()) + "&passwd=" + Encoding.urlEncode(account.getPass()) + "&.persistent=y&.save=&passwd_raw=";
                    String post_data = post_data_basic;
                    /* Captcha for/before login */
                    if (action.contains("login_verify2")) {
                        post_data += getLoginCaptchaData(account);
                    }
                    br.postPage(action, post_data);
                    /* Account is valid but captcha input is needed to verify that */
                    if (br.containsHTML("\"code\":\"1213\"")) {
                        post_data += getLoginCaptchaData(account);
                        br.postPage(action, post_data);
                        break;
                    }
                    if (br.containsHTML("<legend>Login Form</legend>")) continue;
                    break;
                }
                if (br.containsHTML("\"status\"([\t\n\r]+)?:([\t\n\r]+)?\"error\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String stepForward = br.getRegex("\"url\" : \"(https?://[^<>\"\\']+)\"").getMatch(0);
                if (stepForward == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.getPage(stepForward);
                stepForward = br.getRegex("Please <a href=\"(http://(www\\.)?flickr\\.com/[^<>\"]+)\"").getMatch(0);
                if (stepForward != null) br.getPage(stepForward);
                if (!isValid(this.br)) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                // Save cookies
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
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

    public static void prepBr(final Browser br) {
        br.setCookie(MAINPAGE, "localization", "en-us%3Bde%3Bde");
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:28.0) Gecko/20100101 Firefox/28.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-us;q=0.7,en;q=0.3");
        br.getHeaders().put("Accept-Charset", null);
    }

    private boolean isValid(final Browser br) throws IOException {
        br.getPage("https://www.flickr.com/");
        if (!br.containsHTML("class=\"disk left\"")) return false;
        return true;
    }

    private String getLoginCaptchaData(final Account acc) throws PluginException, IOException {
        String post_data = "";
        br.getPage("https://login.yahoo.com/captcha/CaptchaWSProxyService.php?action=createlazy&initial_view=&.intl=" + intl + "&.lang=" + lang_post + "&login=" + Encoding.urlEncode(acc.getUser()) + "&rnd=" + System.currentTimeMillis());
        final String captchaLink = br.getRegex("Enter the characters displayed\\&quot; src=\\&quot;(https?://[A-Za-z0-9\\-_\\.]+yahoo\\.com:\\d+/img/[^<>\"]*?)\\&quot;").getMatch(0);
        if (captchaLink == null) {
            final String lang = System.getProperty("user.language");
            if ("de".equalsIgnoreCase(lang)) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        final DownloadLink dummyLink = new DownloadLink(this, "Account", "flickr.com", "http://flickr.com", true);
        final String c = getCaptchaCode(captchaLink, dummyLink);
        final String valuesText = br.getRegex("\\&lt;div id=\\&quot;captchaV5ControlElements\\&quot;\\&gt;(.*?)\\&lt;audio id=\\&quot;captchaV5Audio\\&quot;").getMatch(0);
        if (valuesText != null) {
            final String[][] data = new Regex(valuesText, "type=\\&quot;hidden\\&quot; name=\\&quot;([^<>\"]*?)\\&quot; id=\\&quot;([^<>\"]*?)\\&quot; value=\\&quot;([^<>\"]*?)\\&quot;").getMatches();
            for (final String[] single_data : data) {
                final String name = single_data[0];
                final String value = single_data[2];
                post_data += "&" + name + "=" + value;
            }
        }
        post_data += "&captchaView=visual&captchaAnswer=" + Encoding.urlEncode(c) + "&.saveC=&.persistent=y";
        return post_data;
    }

    private String createGuid() {
        String a = "";
        final String b = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789._";
        int c = 0;
        while (c < 22) {
            final int index = (int) Math.floor(Math.random() * b.length());
            a = a + b.substring(index, index + 1);
            c++;
        }
        return a;
    }

    private String getFinalLink(final String id) {
        // Make sure we get the correct downloadlinks
        final String picInfo = br.getRegex("photo\\.init\\((\\{\"id\":\"" + id + "\".*?\"is_public\":\\d+\\})").getMatch(0);
        if (picInfo == null) return null;
        final String[] sizes = { "o", "k", "h", "l", "c", "z", "m", "n", "s", "t", "q", "sq" };
        String finallink = null;
        for (final String size : sizes) {
            // Maybe remove old regex with next update
            finallink = new Regex(picInfo, "\"id\":\"" + id + "\"[^\t\n\r]+" + size + "\":\\{\"label\":\"[^\t\n\r]+\",\"file\":\"[^\t\n\r]+\",\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (finallink == null) finallink = new Regex(picInfo, "\"" + size + "\":\\{\"label\":\"[A-Za-z0-9% \\-_\\.]+\",\"file\":\"" + id + "[A-Za-z0-9\\.\\-_]+\",\"url\":\"(http[^<>\"]*?)\"").getMatch(0);
            if (finallink != null) break;
        }
        if (finallink != null) finallink = finallink.replace("\\", "");
        return finallink;
    }

    private String getFilename() {
        String filename = br.getRegex("<meta name=\"title\" content=\"(.*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"photo\\-title\">(.*?)</h1").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?) \\| Flickr \\- Photo Sharing\\!</title>").getMatch(0);
            }
        }
        if (filename == null) filename = br.getRegex("<meta name=\"og:title\" content=\"([^<>\"]*?)\"").getMatch(0);
        return filename;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
