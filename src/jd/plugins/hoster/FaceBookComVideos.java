//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "facebook.com" }, urls = { "http(s)?://(www\\.)?facebook\\.com/(video/video\\.php\\?v=|profile\\.php\\?id=\\d+\\&ref=ts#\\!/video/video\\.php\\?v=)\\d+" }, flags = { 2 })
public class FaceBookComVideos extends PluginForHost {

    private String              dllink           = null;
    private static String       FACEBOOKMAINPAGE = "http://www.facebook.com";
    private static final String DLLINKREGEXP     = "\\(\"(highqual|video)_src\", \"(http.*?)\"\\)";
    private static final Object LOCK             = new Object();

    public FaceBookComVideos(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://www.facebook.com/r.php");
    }

    public void correctDownloadLink(DownloadLink link) {
        String thislink = link.getDownloadURL().replace("https://", "http://");
        String videoID = new Regex(thislink, "ts#\\!/video/video\\.php\\?v=(\\d+)").getMatch(0);
        if (videoID != null) thislink = "http://facebook.com/video/video.php?v=" + videoID;
        link.setUrlDownload(thislink);
    }

    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        br.setCookie("http://www.facebook.com", "locale", "en_GB");
        final Account aa = AccountController.getInstance().getValidAccount(this);
        if (aa == null || !aa.isValid()) {
            link.getLinkStatus().setStatusText(JDL.L("plugins.hoster.facebookvideos.only4registered", "Links can only be checked if a valid account is entered"));
            return AvailableStatus.UNCHECKABLE;
        }
        br.setFollowRedirects(true);
        login(aa, false, br);
        br.getPage(link.getDownloadURL());
        String getThisPage = br.getRegex("window\\.location\\.replace\\(\"(http:.*?)\"").getMatch(0);
        if (getThisPage != null) br.getPage(getThisPage.replace("\\", ""));
        if (br.containsHTML("(No htmlCode read|>Dieses Video wurde entweder von Facebook entfernt oder)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        dllink = Encoding.urlDecode(decodeUnicode(br.getRegex(DLLINKREGEXP).getMatch(1)), true);
        // extrahiere Videoname aus HTML-Quellcode
        String filename = br.getRegex("class=\"video_title datawrap\">(.*)</h3>").getMatch(0);

        // wird nichts gefunden, versuche Videoname aus einem anderen
        // Teil des Quellcodes rauszusuchen
        if (filename == null) {
            filename = br.getRegex("<title>Facebook \\| Videos posted by .*: (.*)</title>").getMatch(0);
        }

        // falls Videoname immer noch nicht gefunden wurde, dann
        // versuche Username & Video-ID als Filename zu nehmen
        if (filename == null) {
            filename = br.getRegex("<title>Facebook \\| Videos posted by (.*): .*</title>").getMatch(0).trim();

            // falls Username gefunden wurde, so setze dies und Video-ID
            // zusammen
            if (filename != null) {
                final String videoid = new Regex(link.getDownloadURL(), "facebook\\.com/video/video\\.php\\?v=(\\d+)").getMatch(0);
                filename = filename + " - Video_" + videoid;
            }
        }

        // wurde Filename extrahiert, setze entgültiger Dateiname &
        // Dateiendung
        if (filename != null) {
            filename = filename.trim();
            link.setFinalFileName(filename + ".mp4");
            return AvailableStatus.TRUE;
        } else {
            // falls nicht, so setze den Download als nicht verfügbar
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    public String decodeUnicode(final String s) {
        final Pattern p = Pattern.compile("\\\\u([0-9a-fA-F]{4})");
        String res = s;
        final Matcher m = p.matcher(res);
        while (m.find()) {
            res = res.replaceAll("\\" + m.group(0), Character.toString((char) Integer.parseInt(m.group(1), 16)));
        }
        return res;
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        try {
            login(account, true, br);
        } catch (final PluginException e) {
            account.setValid(false);
            return ai;
        }
        ai.setStatus("Valid Facebook account is active");
        ai.setUnlimitedTraffic();
        account.setValid(true);
        return ai;
    }

    @Override
    public String getAGBLink() {
        return "http://www.facebook.com/terms.php";
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        throw new PluginException(LinkStatus.ERROR_FATAL, "Only downloadable for registered users");
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        final String addedlink = downloadLink.getDownloadURL();
        if (dllink == null) {
            br.getPage(addedlink);
            dllink = br.getRegex(DLLINKREGEXP).getMatch(1);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (dllink) is null");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final downloadlink = " + dllink + " starting download...");
        final String Vollkornkeks = downloadLink.getDownloadURL().replace(FACEBOOKMAINPAGE, "");
        br.setCookie(FACEBOOKMAINPAGE, "x-referer", Encoding.urlEncode(FACEBOOKMAINPAGE + Vollkornkeks + "#" + Vollkornkeks));
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    public void login(final Account account, final boolean force, Browser br) throws Exception {
        synchronized (LOCK) {
            // Load cookies
            br.setCookiesExclusive(false);
            final Object ret = account.getProperty("cookies", null);
            boolean acmatch = Encoding.urlEncode(account.getUser()).matches(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
            if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).matches(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
            if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                if (cookies.containsKey("c_user") && cookies.containsKey("xs") && account.isValid()) {
                    for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                        final String key = cookieEntry.getKey();
                        final String value = cookieEntry.getValue();
                        this.br.setCookie(FACEBOOKMAINPAGE, key, value);
                    }
                    return;
                }
            }
            br.setFollowRedirects(true);
            br.getPage(FACEBOOKMAINPAGE);
            final Form loginForm = br.getForm(0);
            if (loginForm == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            loginForm.remove("persistent");
            loginForm.remove(null);
            loginForm.put("email", Encoding.urlEncode(account.getUser()));
            loginForm.put("pass", Encoding.urlEncode(account.getPass()));
            br.submitForm(loginForm);
            if (br.getCookie(FACEBOOKMAINPAGE, "c_user") == null || br.getCookie(FACEBOOKMAINPAGE, "xs") == null) { throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE); }
            // Save cookies
            final HashMap<String, String> cookies = new HashMap<String, String>();
            final Cookies add = br.getCookies(FACEBOOKMAINPAGE);
            for (final Cookie c : add.getCookies()) {
                cookies.put(c.getKey(), c.getValue());
            }
            account.setProperty("name", Encoding.urlEncode(account.getUser()));
            account.setProperty("pass", Encoding.urlEncode(account.getPass()));
            account.setProperty("cookies", cookies);
            account.setValid(true);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}