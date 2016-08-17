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

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Cookies;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "shahid.mbc.net" }, urls = { "shahid.mbc.netrtmpe://mbc\\d\\.csl\\.delvenetworks\\.com/.+" }) 
public class ShaHidMbcNet extends PluginForHost {

    private static final String ALLOW_HD                        = "ALLOW_HD";
    private static final String ALLOW_HIGH                      = "ALLOW_HIGH";
    private static final String ALLOW_LOW                       = "ALLOW_LOW";
    private static final String ALLOW_LOWEST                    = "ALLOW_LOWEST";
    private static final String ALLOW_MEDIUM                    = "ALLOW_MEDIUM";
    private static final String COMPLETE_SEASON                 = "COMPLETE_SEASON";
    private String              DLLINK                          = null;
    private String              fid                             = null;
    private static final String PROPERTY_DLLINK_ACCOUNT_PREMIUM = "premlink";
    private static Object       LOCK                            = new Object();

    public ShaHidMbcNet(final PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("https://shahid.mbc.net/");
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("shahid.mbc.netrtmp", "rtmp"));
    }

    private String convertRtmpUrlToHttpUrl(String url) throws Exception {
        if (!url.matches("rtmp.+?://(.*?)/[0-9a-f]+/[0-9a-f]+(-|/)[0-9a-f]+/[\\w\\-]+\\.\\w+")) {
            return null;
        }
        final String ext = url.substring(url.lastIndexOf(".") + 1);
        url = url.substring(url.indexOf("//") + 2);

        if (ext.matches("(mp3|mp4)")) {
            if (url.indexOf("/" + ext + ":") != -1) {
                url = url.replace(url.substring(url.indexOf("/") + 1, url.indexOf(":") + 1), "");
            }
        }

        url = "http://" + url.replaceAll("\\.csl\\.", ".cpl.");
        url = convertToMediaVaultUrl(url);
        if (url == null) {
            return null;
        }
        return url;
    }

    private String convertToMediaVaultUrl(String url) {
        final Browser getTime = br.cloneBrowser();
        String time = null;
        try {
            getTime.getPage("http://assets.delvenetworks.com/time.php");
            time = getTime.getRegex("(\\d+)").getMatch(0);
        } catch (final Throwable e) {
        }
        if (time == null) {
            return null;
        }
        final int e = (int) Math.floor(Double.parseDouble(time) + 1500);
        url = url + "?e=" + e;
        final String h = JDHash.getMD5(Encoding.Base64Decode("Z0RuU1lzQ0pTUkpOaVdIUGh6dkhGU0RqTFBoMTRtUWc=") + url);
        url = url + "&h=" + h;
        return url;
    }

    @Override
    public String getAGBLink() {
        return "http://shahid.mbc.net/User/register";
    }

    @Override
    public String getDescription() {
        return "JDownloader's ShahidVOD Plugin helps downloading VideoClip from shahid.mbc.net. Shahid provides different video qualities.";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("premiumonly", false)) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        DLLINK = convertRtmpUrlToHttpUrl(downloadLink.getDownloadURL());
        if (DLLINK == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, DLLINK, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private void prepareBrowser(final Browser pp) {
        pp.getHeaders().put("User-Agent", "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; WOW64; Trident/5.0)");
        pp.getHeaders().put("Accept", "*/*");
        pp.getHeaders().put("Accept-Charset", null);
        pp.getHeaders().put("Accept-Language", "de-DE");
        pp.getHeaders().put("Cache-Control", null);
        pp.getHeaders().put("Pragma", null);
        pp.getHeaders().put("Referer", "http://s.delvenetworks.com/deployments/player/player-3.37.5.3.swf?ldr=ldr");
        pp.getHeaders().put("x-flash-version", "10,3,183,7");
    }

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        if (downloadLink.getBooleanProperty("premiumonly", false)) {
            final String mainlink = downloadLink.getStringProperty("mainlink", null);
            if (mainlink == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            fid = new Regex(mainlink, "movie/(\\d+)/").getMatch(0);
            if (fid == null) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            this.br.getPage(mainlink);
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String filename = this.br.getRegex("<title>([^<>\"]+)</title>").getMatch(0);
            if (filename == null) {
                filename = new Regex(mainlink, "shahid\\.mbc\\.net/(.+)").getMatch(0);
            }
            downloadLink.setFinalFileName(filename + ".wvm");
        } else {
            if (getPluginConfig().getBooleanProperty("COMPLETE_SEASON")) {
                return AvailableStatus.TRUE;
            }
            DLLINK = convertRtmpUrlToHttpUrl(downloadLink.getDownloadURL());
            if (DLLINK == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            prepareBrowser(br);
            br.setFollowRedirects(true);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        return AvailableStatus.TRUE;
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

    @SuppressWarnings("deprecation")
    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        final AccountInfo ai = new AccountInfo();
        if (!account.getUser().matches(".+@.+\\..+")) {
            if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nBitte gib deine E-Mail Adresse ins Benutzername Feld ein!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            } else {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nPlease enter your e-mail adress in the username field!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
        }
        try {
            login(account, true);
        } catch (final PluginException e) {
            account.setValid(false);
            throw e;
        }
        account.setType(AccountType.PREMIUM);
        account.setMaxSimultanDownloads(-1);
        account.setConcurrentUsePossible(true);
        ai.setStatus("Premium Account");
        return ai;
    }

    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                /* Load cookies */
                br.setCookiesExclusive(true);
                final Cookies cookies = account.loadCookies("");
                if (cookies != null && !force) {
                    this.br.setCookies(this.getHost(), cookies);
                    return;
                }
                br.setFollowRedirects(true);
                this.br.getPage("https://shahid.mbc.net/ar/widgets.html?widget=login");
                String postdata = "{\"email\":\"" + account.getUser() + "\",\"password\":\"" + account.getPass() + "\",\"basic\":\"false\"}";
                postdata = Encoding.urlEncode(postdata);
                this.br.getHeaders().put("Content-Type", "application/json;charset=UTF-8");
                /* TODO: Fix 'd3a6c9e5-b611-314b-1cc8-6a4c1ef514e5' */
                br.postPageRaw("https://shahid.mbc.net/wd/service/users/login?d3a6c9e5-b611-314b-1cc8-6a4c1ef514e5", postdata);
                if (br.getCookie(this.getHost(), "JSESSIONID") == null || br.getCookie(this.getHost(), "si") == null) {
                    if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nUngültiger Benutzername/Passwort!\r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen? Versuche folgendes:\r\n1. Falls dein Passwort Sonderzeichen enthält, ändere es (entferne diese) und versuche es erneut!\r\n2. Gib deine Zugangsdaten per Hand (ohne kopieren/einfügen) ein.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste.", PluginException.VALUE_ID_PREMIUM_DISABLE);
                    }
                }
                account.saveCookies(this.br.getCookies(this.getHost()), "");
            } catch (final PluginException e) {
                account.clearCookies("");
                throw e;
            }
        }
    }

    @Override
    public void handlePremium(final DownloadLink downloadLink, final Account account) throws Exception {
        requestFileInformation(downloadLink);
        if (!downloadLink.getBooleanProperty("premiumonly", false)) {
            handleFree(downloadLink);
        } else {
            login(account, false);
            String dllink = checkDirectLink(downloadLink, PROPERTY_DLLINK_ACCOUNT_PREMIUM);
            if (dllink == null) {
                this.br.getPage("https://shahid.mbc.net/arContent/getPlayerContent-param-.id-" + this.fid + ".type-player.html");
                dllink = this.br.getRegex("\"url\":\"(http[^<>\"]+)\"").getMatch(0);
                if (dllink != null) {
                    dllink = dllink.replace("\\", "");
                }
            }
            if (dllink == null) {
                logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            logger.info("Final downloadlink = " + dllink + " starting the download...");
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                logger.warning("The final dllink seems not to be a file!");
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            downloadLink.setProperty(PROPERTY_DLLINK_ACCOUNT_PREMIUM, dllink);
            dl.startDownload();
        }
    }

    /**
     * Check if a stored directlink exists under property 'property' and if so, check if it is still valid (leads to a downloadable content
     * [NOT html]).
     */
    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
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

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), COMPLETE_SEASON, JDL.L("plugins.hoster.shahidmbcnet.completeseason", "Complete season?")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, JDL.L("plugins.hoster.shahidmbcnet.configlabel", "Select Media Quality:")));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HD, "HD @ 1200Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_HIGH, "HIGH @ 900Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_MEDIUM, "MEDIUM @ 600Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LOW, "LOW @ 450Kbps").setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ALLOW_LOWEST, "LOWEST @ 224Kbps").setDefaultValue(true));
    }

}