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

import java.util.LinkedHashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "huaban.com" }, urls = { "https?://(?:www\\.)?huaban\\.com/pins/\\d+" }, flags = { 0 })
public class HuabanCom extends PluginForHost {

    public HuabanCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium("https://www.huaban.com/");
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://huaban.com/";
    }

    /* Site constants */
    public static final String default_extension = ".jpg";

    /* don't touch the following! */
    private String             dllink            = null;

    @SuppressWarnings({ "deprecation", "unchecked" })
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        String filename = null;
        final String pin_id = new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0);
        /* Display ids for offline links */
        link.setName(pin_id);
        link.setLinkID(pin_id);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        String site_title = null;
        dllink = checkDirectLink(link, "free_directlink");
        if (dllink != null) {
            /* Avoid unnecessary site requests. */
            site_title = link.getFinalFileName();
            if (site_title == null) {
                site_title = pin_id;
            }
        } else {
            // final String source_url = link.getStringProperty("source_url", null);
            // final String boardid = link.getStringProperty("boardid", null);
            // final String username = link.getStringProperty("username", null);
            br.getPage(link.getDownloadURL());
            if (this.br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            /*
             * Site actually contains similar json compared to API --> Grab that and get the final link via that as it is not always present
             * in the normal html code.
             */
            final String json = br.getRegex("app\\.page\\[\"pin\"\\] = (\\{.*?\\});[\t\n\r]+").getMatch(0);
            if (json == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) jd.plugins.hoster.DummyScriptEnginePlugin.jsonToJavaObject(json);
            dllink = getDirectlinkFromJson(entries);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            link.setProperty("free_directlink", dllink);
        }
        String ext = dllink.substring(dllink.lastIndexOf("."));
        if (ext == null || ext.length() > 5) {
            ext = default_extension;
        }
        filename = pin_id;
        if (!filename.endsWith(ext)) {
            filename += ext;
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, false, 1, "free_directlink");
    }

    private void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty(directlinkproperty, dllink);
        dl.startDownload();
    }

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

    public static String getDirectlinkFromJson(final LinkedHashMap<String, Object> entries) {
        String directlink = null;
        final String key = (String) DummyScriptEnginePlugin.walkJson(entries, "file/key");
        if (key != null) {
            directlink = "http://img.hb.aicdn.com/" + key;
        }
        return directlink;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    // private static final String MAINPAGE = "http://huaban.com";
    // private static Object LOCK = new Object();
    //
    // @SuppressWarnings("unchecked")
    // public static void login(final Browser br, final Account account, final boolean force) throws Exception {
    // synchronized (LOCK) {
    // try {
    // // Load cookies
    // br.setCookiesExclusive(true);
    // final Cookies cookies = account.loadCookies("");
    // if (cookies != null && !force) {
    // br.setCookies(account.getHoster(), cookies);
    // return;
    // }
    // br.setFollowRedirects(true);
    // br.getPage("http://huaban.com/");
    // try {
    // br.postPageRaw("http://huaban.com/", "");
    // } catch (final BrowserException e) {
    // if (br.getHttpConnection() != null && br.getHttpConnection().getResponseCode() == 401) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // throw e;
    // }
    // if (br.containsHTML("jax CsrfErrorPage Module") || br.getCookie(MAINPAGE, "_b") == null) {
    // if ("de".equalsIgnoreCase(System.getProperty("user.language"))) {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nUngültiger Benutzername oder ungültiges Passwort!\r\nSchnellhilfe: \r\nDu bist dir sicher, dass dein eingegebener Benutzername und Passwort stimmen?\r\nFalls dein Passwort Sonderzeichen enthält, ändere es und versuche es erneut!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // } else {
    // throw new PluginException(LinkStatus.ERROR_PREMIUM,
    // "\r\nInvalid username/password!\r\nQuick help:\r\nYou're sure that the username and password you entered are correct?\r\nIf your password contains special characters, change it (remove them) and try again!",
    // PluginException.VALUE_ID_PREMIUM_DISABLE);
    // }
    // }
    // account.saveCookies(br.getCookies(account.getHoster()), "");
    // } catch (final PluginException e) {
    // account.clearCookies("");
    // throw e;
    // }
    // }
    // }
    //
    // @SuppressWarnings("deprecation")
    // @Override
    // public AccountInfo fetchAccountInfo(final Account account) throws Exception {
    // final AccountInfo ai = new AccountInfo();
    // try {
    // login(this.br, account, true);
    // } catch (PluginException e) {
    // account.setValid(false);
    // throw e;
    // }
    // ai.setUnlimitedTraffic();
    // account.setType(AccountType.FREE);
    // account.setMaxSimultanDownloads(-1);
    // account.setConcurrentUsePossible(false);
    // ai.setStatus("Free Account");
    // account.setValid(true);
    // return ai;
    // }
    //
    // @Override
    // public void handlePremium(final DownloadLink link, final Account account) throws Exception {
    // requestFileInformation(link);
    // /* We already logged in in requestFileInformation */
    // br.setFollowRedirects(false);
    // doFree(link, false, 1, "account_free_directlink");
    // }

    /** Avoid chars which are not allowed in filenames under certain OS' */
    private static String encodeUnicode(final String input) {
        String output = input;
        output = output.replace(":", ";");
        output = output.replace("|", "¦");
        output = output.replace("<", "[");
        output = output.replace(">", "]");
        output = output.replace("/", "⁄");
        output = output.replace("\\", "∖");
        output = output.replace("*", "#");
        output = output.replace("?", "¿");
        output = output.replace("!", "¡");
        output = output.replace("\"", "'");
        return output;
    }

    @Override
    public String getDescription() {
        return "JDownloader's huaban.com plugin helps downloading pictures from huaban.com.";
    }

    public static final String  ENABLE_DESCRIPTION_IN_FILENAMES        = "ENABLE_DESCRIPTION_IN_FILENAMES";
    public static final boolean defaultENABLE_DESCRIPTION_IN_FILENAMES = false;

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), ENABLE_DESCRIPTION_IN_FILENAMES, JDL.L("plugins.hoster.HuabanCom.enableDescriptionInFilenames", "Add pind-escription to filenames?\r\nNOTE: If enabled, Filenames might get very long!")).setDefaultValue(defaultENABLE_DESCRIPTION_IN_FILENAMES));
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}