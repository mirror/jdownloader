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
import java.util.LinkedHashMap;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
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
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "playvid.com" }, urls = { "http://playviddecrypted.com/\\d+" }, flags = { 2 })
public class PlayVidCom extends PluginForHost {

    public PlayVidCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    private String DLLINK = null;

    @Override
    public String getAGBLink() {
        return "http://www.playvid.com/terms.html";
    }

    /** Settings stuff */
    private static final String FASTLINKCHECK = "FASTLINKCHECK";
    private static final String ALLOW_BEST    = "ALLOW_BEST";
    private static final String ALLOW_360P    = "ALLOW_360P";
    private static final String ALLOW_480P    = "ALLOW_480P";
    private static final String ALLOW_720     = "ALLOW_720";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        if (downloadLink.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        // finalfilename approach is old but we need to keep it for users who still have old links in JD
        String filename = downloadLink.getFinalFileName();
        if (filename == null) filename = downloadLink.getStringProperty("directname", null);
        DLLINK = checkDirectLink(downloadLink, "directlink");
        if (DLLINK == null) {
            if (downloadLink.getStringProperty("mainlink", null) == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            br.getPage(downloadLink.getStringProperty("mainlink", null));
            final String videosource = getVideosource(this.br);
            if (videosource == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            DLLINK = getQuality(downloadLink.getStringProperty("qualityvalue", null), videosource);
        }
        if (filename == null || DLLINK == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(filename);
        // In case the link redirects to the finallink
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(DLLINK);
            if (!con.getContentType().contains("html"))
                downloadLink.setDownloadSize(con.getLongContentLength());
            else
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setProperty("directlink", DLLINK);
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
        doFree(downloadLink);
    }

    private void doFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private static final String MAINPAGE = "http://playvid.com";
    private static Object       LOCK     = new Object();

    @SuppressWarnings("unchecked")
    public void login(final Browser br, final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                // Load cookies
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = Encoding.urlEncode(account.getUser()).equals(account.getStringProperty("name", Encoding.urlEncode(account.getUser())));
                if (acmatch) acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.setFollowRedirects(false);
                br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
                br.postPage("http://accounts.playvid.com/login/playvid", "remember_me=on&back_url=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String lang = System.getProperty("user.language");
                if (br.containsHTML("\"status\":\"error\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String continuelink = br.getRegex("\"redirect\":\"(\\\\/[^<>\"]*?)\"").getMatch(0);
                if (continuelink == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                continuelink = "http://accounts.playvid.com" + continuelink.replace("\\", "");
                br.getPage(continuelink);
                final String cookie = br.getCookie(MAINPAGE, "sunsid");
                if (cookie == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                br.setCookie("https://accounts.playvid.com/", "sunsid", cookie);
                br.setCookie(MAINPAGE, "sunsid", cookie);
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

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(this.br, account, true);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        ai.setUnlimitedTraffic();
        account.setValid(true);
        ai.setStatus("Registered (free) user");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        login(this.br, account, false);
        doFree(link);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public static String getVideosource(final Browser br) {
        String videosource = br.getRegex("flashvars=\"(.*?)\"").getMatch(0);
        if (videosource == null) return null;
        videosource = Encoding.htmlDecode(videosource);
        return videosource;
    }

    public static LinkedHashMap<String, String> getQualities(final Browser br) {
        final String videosource = getVideosource(br);
        if (videosource == null) return null;
        final LinkedHashMap<String, String> foundqualities = new LinkedHashMap<String, String>();
        /** Decrypt qualities START */
        /** First, find all available qualities */
        final String[] qualities = { "720p", "480p", "360p" };
        for (final String quality : qualities) {
            final String currentQualityUrl = getQuality(quality, videosource);
            if (currentQualityUrl != null) {
                foundqualities.put(quality, currentQualityUrl);
            }
        }
        /** Decrypt qualities END */
        return foundqualities;
    }

    public static String getQuality(final String quality, final String videosource) {
        return new Regex(videosource, "video_vars\\[video_urls\\]\\[" + quality + "\\]= ?(http://[^<>\"]*?)\\&").getMatch(0);
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
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public String getDescription() {
        return "JDownloader's PlayVid Plugin helps downloading Videoclips from playvid.com. PlayVid provides different video formats and qualities.";
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FASTLINKCHECK, JDL.L("plugins.hoster.playvidcom.fastLinkcheck", "Fast linkcheck (filesize won't be shown in linkgrabber)?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        final ConfigEntry hq = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_BEST, JDL.L("plugins.hoster.playvidcom.checkbest", "Only grab the best available resolution")).setDefaultValue(false);
        getConfig().addEntry(hq);
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_360P, JDL.L("plugins.hoster.playvidcom.check360p", "Grab 360p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_480P, JDL.L("plugins.hoster.playvidcom.check480p", "Grab 480p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_720, JDL.L("plugins.hoster.playvidcom.check720p", "Grab 720p?")).setDefaultValue(true).setEnabledCondidtion(hq, false));
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
