//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "vimeo.com" }, urls = { "decryptedforVimeoHosterPlugin://(www\\.|player\\.)?vimeo\\.com/(video/)?\\d+" }, flags = { 2 })
public class VimeoCom extends PluginForHost {

    private static final String MAINPAGE   = "http://vimeo.com";
    static private final String AGB        = "http://www.vimeo.com/terms";
    private String              finalURL;
    private static Object       LOCK       = new Object();
    private static final String Q_MOBILE   = "Q_MOBILE";
    private static final String Q_ORIGINAL = "Q_ORIGINAL";
    private static final String Q_HD       = "Q_HD";
    private static final String Q_SD       = "Q_SD";
    private static final String Q_BEST     = "Q_BEST";

    public VimeoCom(final PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://vimeo.com/join");
        setConfigElements();
    }

    @Override
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("decryptedforVimeoHosterPlugin://", "http://"));
    }

    private String[][] getQualities(Browser br, String ID, String title) throws Exception {
        boolean withDlBtn = br.containsHTML("iconify_down_b");
        /*
         * little pause needed so the next call does not return trash
         */
        Thread.sleep(1000);
        String qualities[][] = null;
        if (withDlBtn) {
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.getPage("http://vimeo.com/" + ID + "?action=download");
            qualities = br.getRegex("href=\"(/\\d+/download.*?)\" download=\"(.*?)\" .*?>(.*? file)<.*?\\d+x\\d+ /(.*?)\\)").getMatches();
        } else {
            /* withoutDlBtn */
            String[] queryValues = br.getRegex("\"timestamp\":(\\d+),\"signature\":\"([0-9a-f]+)\"").getRow(0);
            String fmts = br.getRegex("\"files\":\\{\"h264\":\\[(.*?)\\]\\}").getMatch(0);
            if (queryValues != null && fmts != null) {
                String quality[] = fmts.replaceAll("\"", "").split(",");
                qualities = new String[quality.length][4];
                for (int i = 0; i < quality.length; i++) {
                    qualities[i][0] = "http://player.vimeo.com/play_redirect?clip_id=" + ID + "&sig=" + queryValues[1] + "&time=" + queryValues[0] + "&quality=" + quality[i];
                    qualities[i][1] = title + ".mp4";
                    qualities[i][2] = quality[i];
                    qualities[i][3] = null;
                }
            }
        }
        return qualities;
    }

    public void doFree(final DownloadLink downloadLink) throws Exception {
        if (!"FREE".equals(downloadLink.getStringProperty("LASTTYPE", "FREE"))) {
            downloadLink.setProperty("LASTTYPE", "FREE");
            downloadLink.setChunksProgress(null);
            downloadLink.setDownloadSize(0);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Set the final filename here because downloads via account have other
        // extensions
        downloadLink.setFinalFileName(downloadLink.getName());
        downloadLink.setProperty("LASTTYPE", "FREE");
        dl.startDownload();
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        synchronized (LOCK) {
            final AccountInfo ai = new AccountInfo();
            if (!new Regex(account.getUser(), ".*?@.*?\\..+").matches()) {
                account.setProperty("cookies", null);
                account.setValid(false);
                ai.setStatus("Invalid email address");
                return ai;

            }
            try {
                login(account, true);
            } catch (final PluginException e) {
                account.setProperty("cookies", null);
                account.setValid(false);
                return ai;
            }
            br.getPage("http://vimeo.com/settings");
            String type = br.getRegex("acct_status\">.*?>(.*?)<").getMatch(0);
            if (type == null) type = br.getRegex("user_type', '(.*?)'").getMatch(0);
            if (type != null) {
                ai.setStatus(type);
            } else {
                ai.setStatus(null);
            }
            account.setValid(true);
            ai.setUnlimitedTraffic();
            return ai;
        }
    }

    @Override
    public String getAGBLink() {
        return AGB;
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
        if (downloadLink.getStringProperty("directName", null) != null) {
            requestFileInformation(downloadLink);
            doDirect(downloadLink);
            return;
        }
        if (!"FREE".equals(downloadLink.getStringProperty("LASTTYPE", "FREE"))) {
            downloadLink.setProperty("LASTTYPE", "FREE");
            downloadLink.setChunksProgress(null);
            downloadLink.setDownloadSize(0);
        }
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    private void doDirect(DownloadLink downloadLink) throws Exception {
        finalURL = downloadLink.getStringProperty("directURL", null);
        if (finalURL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finalURL, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            if (dl.getConnection().getResponseCode() == 404) {
                /* direct link no longer valid, lets refresh it */
                try {
                    dl.getConnection().disconnect();
                } catch (final Throwable e) {
                }
                downloadLink.setProperty("directURL", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("LASTTYPE", "DIRECT");
        dl.startDownload();
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        if (link.getStringProperty("directName", null) != null) {
            requestFileInformation(link);
            doDirect(link);
            return;
        }
        if (!"PREMIUM".equals(link.getStringProperty("LASTTYPE", "FREE"))) {
            link.setProperty("LASTTYPE", "PREMIUM");
            link.setChunksProgress(null);
            link.setDownloadSize(0);
        }
        requestFileInformation(link);
        login(account, false);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("\">Sorry, not available for download")) {
            logger.info("No download available for link: " + link.getDownloadURL() + " , downloading as unregistered user...");
            doFree(link);
            return;
        }
        String dllink = br.getRegex("class=\"download\">[\t\n\r ]+<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(/?download/video:\\d+\\?v=\\d+\\&e=\\d+\\&h=[a-z0-9]+\\&uh=[a-z0-9]+)\"").getMatch(0);
        }
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (!dllink.startsWith("/")) {
            dllink = MAINPAGE + "/" + dllink;
        } else {
            dllink = MAINPAGE + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String oldName = link.getName();
        final String newName = getFileNameFromHeader(dl.getConnection());
        final String name = oldName.substring(0, oldName.lastIndexOf(".")) + newName.substring(newName.lastIndexOf("."));
        link.setName(name);
        link.setProperty("LASTTYPE", "ACCOUNT");
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private void login(final Account account, final boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                setBrowserExclusive();
                br.setFollowRedirects(true);
                br.setDebug(true);
                final Object ret = account.getProperty("cookies", null);
                boolean acmatch = account.getUser().matches(account.getStringProperty("name", account.getUser()));
                if (acmatch) {
                    acmatch = account.getPass().matches(account.getStringProperty("pass", account.getPass()));
                }
                if (acmatch && ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (cookies.containsKey("vimeo") && account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            br.setCookie(MAINPAGE, key, value);
                        }
                        return;
                    }
                }
                br.getPage(MAINPAGE);
                br.getPage(MAINPAGE + "/log_in");
                final String xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
                if (xsrft == null) {
                    account.setProperty("cookies", null);
                    logger.warning("Login is broken!");
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                /* important, else we get a 401 */
                br.setCookie(MAINPAGE, "xsrft", xsrft);
                if (!new Regex(account.getUser(), ".*?@.*?\\..+").matches()) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                br.postPage(MAINPAGE + "/log_in", "action=login&service=vimeo&email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&token=" + Encoding.urlEncode(xsrft));
                if (br.getCookie(MAINPAGE, "vimeo") == null) {
                    account.setProperty("cookies", null);
                    throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                }
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = br.getCookies(MAINPAGE);
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("name", account.getUser());
                account.setProperty("pass", account.getPass());
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        finalURL = downloadLink.getStringProperty("directURL", null);
        if (finalURL != null) {
            try {
                con = br.openGetConnection(finalURL);
                if (con.getContentType() != null && !con.getContentType().contains("html")) {
                    downloadLink.setDownloadSize(con.getLongContentLength());
                    downloadLink.setFinalFileName(downloadLink.getStringProperty("directName", null));
                    return AvailableStatus.TRUE;
                } else {
                    /* durectURL no longer valid */
                    downloadLink.setProperty("directURL", null);
                }
            } finally {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
        /* fetch fresh directURL */
        String name = downloadLink.getStringProperty("directName", null);
        String ID = new Regex(downloadLink.getDownloadURL(), ".com/(\\d+)").getMatch(0);
        if (ID == null || name == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Browser br = new Browser();
        setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://vimeo.com", "v6f", "1");
        br.getPage("http://vimeo.com/" + ID);
        handlePW(downloadLink, br, "http://vimeo.com/" + ID + "/password");
        String newURL = null;
        String qualities[][] = getQualities(br, ID, name);
        for (String quality[] : qualities) {
            String url = quality[0];
            if (name.equals(Encoding.htmlDecode(quality[1]))) {
                if (!url.startsWith("http://")) url = "http://vimeo.com" + url;
                newURL = url;
                break;
            }
        }

        if (newURL == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setProperty("directURL", newURL);
        downloadLink.setFinalFileName(downloadLink.getStringProperty("directName", null));
        return AvailableStatus.TRUE;
    }

    private void handlePW(DownloadLink downloadLink, Browser br, String url) throws PluginException, IOException {
        if (br.containsHTML("This is a private video")) {
            String passCode = downloadLink.getStringProperty("pass", null);
            if (passCode == null) passCode = Plugin.getUserInput("Password?", downloadLink);
            if (passCode == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            final String xsrft = br.getRegex("xsrft: '(.*?)'").getMatch(0);
            br.setCookie(MAINPAGE, "xsrft", xsrft);
            br.postPage(url, "password=" + Encoding.urlEncode(passCode) + "&token=" + xsrft);
            if (br.containsHTML("This is a private video")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, "Password needed!");
            }
            downloadLink.setProperty("pass", passCode);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
        /* reset downloadtype back to free */
        link.setProperty("LASTTYPE", "FREE");
        String name = link.getStringProperty("directName", null);
        if (name != null) link.setFinalFileName(name);
    }

    @Override
    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_BEST, JDL.L("plugins.hoster.vimeo.best", "Load Best Version ONLY")).setDefaultValue(false));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_SEPARATOR));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_MOBILE, JDL.L("plugins.hoster.vimeo.loadmobile", "Load Mobile Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_ORIGINAL, JDL.L("plugins.hoster.vimeo.loadoriginal", "Load Original Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_HD, JDL.L("plugins.hoster.vimeo.loadsd", "Load HD Version")).setDefaultValue(true));
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), Q_SD, JDL.L("plugins.hoster.vimeo.loadhd", "Load SD Version")).setDefaultValue(true));
    }

}