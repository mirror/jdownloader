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

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
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
import jd.utils.locale.JDL;

import org.appwork.utils.StringUtils;
import org.jdownloader.downloader.hds.HDSDownloader;
import org.jdownloader.plugins.components.hds.HDSContainer;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "playvid.com" }, urls = { "http://playviddecrypted\\.com/\\d+" })
public class PlayVidCom extends PluginForHost {
    public PlayVidCom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium();
        this.setConfigElements();
    }

    private String dllink = null;

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
    private static final String quality_360   = "360p";
    private static final String quality_480   = "480p";
    private static final String quality_720   = "720p";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("offline", false)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String qualityvalue = downloadLink.getStringProperty("qualityvalue", null);
        this.setBrowserExclusive();
        final Account account = AccountController.getInstance().getValidAccount(this);
        if (account != null) {
            login(this.br, account, false);
        }
        if (!StringUtils.containsIgnoreCase(qualityvalue, "hds_")) {
            br.setFollowRedirects(true);
            String filename = downloadLink.getStringProperty("directname", null);
            dllink = checkDirectLink(downloadLink, "directlink");
            if (dllink == null) {
                /* Refresh directlink */
                br.getPage(downloadLink.getStringProperty("mainlink", null));
                if (isOffline(this.br)) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                final String videosource = getVideosource(this.br);
                if (videosource == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (quality_720.equals(qualityvalue) && account == null) {
                    logger.info("User is not logged in but tries to download a quality which needs login");
                    return AvailableStatus.TRUE;
                }
                dllink = getQuality(qualityvalue, videosource);
            }
            if (filename == null || dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setFinalFileName(filename);
            // In case the link redirects to the finallink
            if (downloadLink.getKnownDownloadSize() == -1) {
                URLConnectionAdapter con = null;
                try {
                    con = br.openHeadConnection(dllink);
                    if (!con.getContentType().contains("html") && con.isOK()) {
                        downloadLink.setDownloadSize(con.getLongContentLength());
                    } else {
                        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                    }
                    downloadLink.setProperty("directlink", dllink);
                } finally {
                    try {
                        if (con != null) {
                            con.disconnect();
                        }
                    } catch (Throwable e) {
                    }
                }
            }
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        doDownload(downloadLink, null);
    }

    private void doDownload(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        final String qualityvalue = downloadLink.getStringProperty("qualityvalue", null);
        if (qualityvalue == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (StringUtils.containsIgnoreCase(qualityvalue, "hds_")) {
            final HDSContainer container = HDSContainer.read(downloadLink);
            if (container == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            } else {
                br.setFollowRedirects(true);
                br.getPage(downloadLink.getStringProperty("mainlink", null).replace("http://", "https://"));
                final LinkedHashMap<String, String> foundQualities = getQualities(br);
                final String f4m = foundQualities.get(qualityvalue);
                if (f4m == null) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                br.getPage(f4m);
                br.followRedirect();
                final List<HDSContainer> all = HDSContainer.getHDSQualities(br);
                if (all == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                } else {
                    final HDSContainer hit = HDSContainer.getBestMatchingContainer(all, container);
                    if (hit == null) {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    } else {
                        hit.write(downloadLink);
                        final HDSDownloader dl = new HDSDownloader(downloadLink, br, hit.getFragmentURL());
                        this.dl = dl;
                        dl.setEstimatedDuration(hit.getDuration());
                    }
                }
            }
        } else {
            if (quality_720.equals(qualityvalue) && account == null) {
                /* Should never happen! */
                logger.info("User is not logged in but tries to download a quality which needs login");
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } else {
                dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
                if (dl.getConnection().getContentType().contains("html")) {
                    br.followConnection();
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
        }
        dl.startDownload();
    }

    public static final boolean isOffline(final Browser br) {
        if (br.containsHTML("Video not found<|>This video has been removed|class=\"play\\-error\"|class=\"error\\-sorry\"") || br.getHttpConnection().getResponseCode() == 404) {
            return true;
        }
        return false;
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
                if (acmatch) {
                    acmatch = Encoding.urlEncode(account.getPass()).equals(account.getStringProperty("pass", Encoding.urlEncode(account.getPass())));
                }
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
                br.postPage("https://accounts.playvid.com/de/login/playvid", "remember_me=on&back_url=&login=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
                final String lang = System.getProperty("user.language");
                if (br.containsHTML("\"status\":\"error\"")) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                String continuelink = br.getRegex("\"redirect\":\"(https[^<>\"]*?)\"").getMatch(0);
                if (continuelink == null) {
                    if ("de".equalsIgnoreCase(lang)) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin defekt, bitte den JDownloader Support kontaktieren!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlugin broken, please contact the JDownloader Support!", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                continuelink = continuelink.replace("\\", "");
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

    @SuppressWarnings("deprecation")
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
        doDownload(link, account);
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    public static String getVideosource(final Browser br) {
        final String videosource = br.getRegex("flashvars=\"(.*?)\"").getMatch(0);
        if (videosource == null) {
            return null;
        } else {
            return Encoding.htmlDecode(videosource);
        }
    }

    public static LinkedHashMap<String, String> getQualities(final Browser br) {
        final String videosource = getVideosource(br);
        if (videosource == null) {
            return null;
        }
        final LinkedHashMap<String, String> foundqualities = new LinkedHashMap<String, String>();
        /** Decrypt qualities START */
        /** First, find all available qualities */
        final String[] qualities = { "hds_manifest", "hds_manifest_720", "hds_manifest_480", "hds_manifest_360", "720p", "480p", "360p" };
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
        return new Regex(videosource, "video_vars(?:\\[video_urls\\])?\\[" + quality + "\\]= ?(https?://[^<>\"]*?)(\\&(?!sec)|$)").getMatch(0);
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        final String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = null;
                try {
                    con = br2.openGetConnection(dllink);
                    if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                        downloadLink.setProperty(property, Property.NULL);
                    } else {
                        downloadLink.setDownloadSize(con.getLongContentLength());
                        return dllink;
                    }
                } finally {
                    if (con != null) {
                        con.disconnect();
                    }
                }
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
            }
        }
        return null;
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

    public Browser prepBrowser(final Browser prepBr) {
        prepBr.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
        prepBr.setAllowedResponseCodes(429);
        return prepBr;
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
