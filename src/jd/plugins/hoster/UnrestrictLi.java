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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Property;
import jd.gui.UserIO;
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

@HostPlugin(revision = "$Revision: 19045 $", interfaceVersion = 3, names = { "unrestrict.li" }, urls = { "http://\\w+\\.(unrestrict|unr)\\.li/dl/\\w+/.+" }, flags = { 2 })
public class UnrestrictLi extends PluginForHost {

    private static final String VERSION = "0.1";

    private static Object       LOCK    = new Object();

    public UnrestrictLi(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        this.enablePremium("http://unrestrict.li");
    }

    public void setConfigElements() {
        // Version (testing)
        getConfig().addEntry(new ConfigEntry(ConfigContainer.TYPE_LABEL, getPluginConfig(), "Version", "Current version: " + VERSION));
    }

    public void setBrowser() {
        br.getHeaders().put("User-Agent", "JDownloader");
        br.setCustomCharset("utf-8");
        br.setConnectTimeout(60 * 1000);
        br.setReadTimeout(60 * 1000);
    }

    @Override
    public String getAGBLink() {
        return "http://unrestrict.li/terms_and_conditions";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink dl) throws PluginException, IOException {
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(dl.getDownloadURL());
            if (con.isContentDisposition()) {
                if (dl.getFinalFileName() == null) dl.setFinalFileName(getFileNameFromHeader(con));
                dl.setVerifiedFileSize(con.getLongContentLength());
                dl.setAvailable(true);
                return AvailableStatus.TRUE;
            } else {
                dl.setAvailable(false);
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    @Override
    public boolean canHandle(DownloadLink downloadLink, Account account) {
        return true;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        handleDL(downloadLink, downloadLink.getDownloadURL());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 1;
    }

    @Override
    public void handlePremium(DownloadLink link, Account account) throws Exception {
        login(account, false);
        showMessage(link, "Task 1: Check URL validity!");
        requestFileInformation(link);
        showMessage(link, "Task 2: Download begins!");
        handleDL(link, link.getDownloadURL());
    }

    public void handleMultiHost(DownloadLink link, Account acc) throws Exception {
        setBrowser();
        login(acc, false);
        showMessage(link, "Task 1: Generating Link");
        br.setCookie("http://unrestrict.li", "lang", "EN");
        br.setCookie("http://unrestrict.li", "ssl", "0");
        br.postPage("http://unrestrict.li/unrestrict.php", "jdownloader=1&domain=long&link=" + Encoding.urlEncode(link.getDownloadURL()) + (link.getStringProperty("pass", null) != null ? "&download_password=" + Encoding.urlEncode(link.getStringProperty("pass", null)) : ""));
        String generated = br.getRegex("\\{\"(.*?)\":\\{\"host").getMatch(0);
        // Get and set chunks to use
        String cons = br.getRegex("\"cons\":(.*?)\\}").getMatch(0);
        if (cons != null) {
            try {
                link.setProperty("cons", Integer.parseInt(cons));
            } catch (NumberFormatException e) {
            }
        }
        /* START Possible Error Messages */
        if (br.containsHTML("invalid\":\"File offline")) {
            logger.info("File offline");
            MessageDialog("Error", "File offline", false);
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (br.containsHTML("invalid\":\"Host is not supported or unknown link format")) {
            logger.info("Unknown link format");
            MessageDialog("Error", "Unknown link format", false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"You are not allowed to download from this host")) {
            logger.info("You are not allowed to download from this host");
            MessageDialog("Error", "You are not allowed to download from this host", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"Host is down")) {
            logger.info("Host is down");
            MessageDialog("Error", "Host is down", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"This file is too large")) {
            logger.info("This file is too large");
            MessageDialog("Error", "This file is too large", false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"You have reached your total daily limit. (Fair Use)")) {
            logger.info("You have reached your total daily limit. (Fair Use)");
            MessageDialog("Error", "You have reached your total daily limit. (Fair Use)", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"You have reached your daily limit for this host")) {
            logger.info("You have reached your daily limit for this host");
            MessageDialog("Error", "You have reached your daily limit for this host", false);
            removeHostFromMultiHost(link, acc);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"This link has been reported and blocked")) {
            logger.info("This link has been reported and blocked");
            MessageDialog("Error", "This link has been reported and blocked", false);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (br.containsHTML("invalid\":\"Error receiving page")) {
            logger.info("Error receiving page");
            if (link.getLinkStatus().getRetryCount() >= 3) {
                link.getLinkStatus().setRetryCount(0);
                MessageDialog("Error", "Error receiving page", false);
                removeHostFromMultiHost(link, acc);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            String msg = "(" + link.getLinkStatus().getRetryCount() + 1 + "/" + 3 + ")";
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Error fetching file information:" + msg, 20 * 1000l);
        }
        if (generated == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        /* END Possible Error Messages */
        showMessage(link, "Task 2: Download begins!");
        generated = generated.replaceAll("\\\\/", "/");
        try {
            handleDL(link, generated);
            return;
        } catch (PluginException e1) {
            try {
                dl.getConnection().disconnect();
            } catch (final Throwable e) {
            }
            /* START Possible Error Messages */
            if (br.containsHTML("Invalid API response.")) {
                logger.info("Invalid API response.");
                MessageDialog("Error", "Invalid API response", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Host not supported.")) {
                logger.info("Host not supported.");
                MessageDialog("Error", "Host not supported", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Error downloading file.")) {
                logger.info("Error downloading file.");
                MessageDialog("Error", "Error downloading file", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Invalid URL returned.")) {
                logger.info("Invalid URL returned.");
                MessageDialog("Error", "Invalid URL returned", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Wrong server.")) {
                logger.info("Wrong server.");
                MessageDialog("Error", "Wrong server", false);
                removeHostFromMultiHost(link, acc);
            }
            if (br.containsHTML("Request denied")) {
                logger.info("Request denied. Please wait at least 5 seconds before refreshing. (Flooding)");
                MessageDialog("Error", "Request denied. Please wait at least 5 seconds before refreshing. (Flooding)", false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Error receiving page.")) {
                logger.info("Error receiving page.");
                MessageDialog("Error", "Error receiving page", false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("An unknown error has occured.")) {
                logger.info("An unknown error has occured.");
                MessageDialog("Error", "An unknown error has occured", false);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            /* END Possible Error Messages */
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void handleDL(DownloadLink link, String generated) throws Exception {
        if (generated.startsWith("https")) {
            generated = generated.replace("https://", "http://");
        }
        // Get and set chunks, default = 0 (up to 20)
        int chunks = link.hasProperty("cons") ? ((int) link.getProperty("cons")) : 0;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, generated, true, chunks);
        if (dl.getConnection().isContentDisposition()) {
            dl.startDownload();
            return;
        } else {
            br.followConnection();
        }
        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    }

    private void removeHostFromMultiHost(DownloadLink link, Account acc) throws PluginException {
        Object supportedHosts = acc.getAccountInfo().getProperty("multiHostSupport", null);
        if (supportedHosts != null && supportedHosts instanceof List) {
            ArrayList<String> newList = new ArrayList<String>((List<String>) supportedHosts);
            newList.remove(link.getHost());
            acc.getAccountInfo().setProperty("multiHostSupport", newList);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
    }

    private void showMessage(DownloadLink link, String message) {
        link.getLinkStatus().setStatusText(message);
    }

    private void MessageDialog(String title, String text, boolean report) {
        UserIO.getInstance().requestMessageDialog(0, title, text);
        // To do: Reporting errors
        /*
         * if(report && UserIO.getInstance().requestConfirmDialog(0, "Error", "Do you want to report this error?") == 2){
         * 
         * }
         */
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {

    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account, true);
        } catch (PluginException e) {
            account.setValid(false);
            ai.setStatus("Wrong username/password");
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        account.setConcurrentUsePossible(true);
        // Max. 1 downloads
        account.setMaxSimultanDownloads(1);

        br.getPage("http://unrestrict.li/api/jdownloader/user.php");
        String expires = br.getRegex("<expires>(\\d+)</expires>").getMatch(0);
        String traffic = br.getRegex("<traffic>(\\d+)</traffic>").getMatch(0);
        if (expires != null) {
            account.setValid(true);
            ai.setValidUntil(Long.parseLong(expires) * 1000);
            // Max. 100 GB/day
            ai.setTrafficMax(107374182400l);
            ai.setTrafficLeft(Long.parseLong(traffic));
            ai.setStatus("VIP");
        } else {
            // Not a VIP member
            account.setValid(false);
            MessageDialog("Error", "Please upgrade to VIP to use this plugin", false);
            ai.setStatus("Upgrade to VIP");
            ai.setProperty("multiHostSupport", Property.NULL);
            return ai;
        }
        try {
            String apihosts = br.cloneBrowser().getPage("http://unrestrict.li/api/jdownloader/hosts.php");
            String[] hosts = new Regex(apihosts, "<host>(.*?)</host>").getColumn(0);
            ArrayList<String> supportedHosts = new ArrayList<String>(Arrays.asList(hosts));
            // remove youtube support from this multihoster. Our youtube plugin works from cdn/cached final links and this does not work
            // with multihosters as it has geolocation issues. To over come this we need to pass the watch link and not decrypted finallink
            // results...
            supportedHosts.remove("youtube.com");
            supportedHosts.remove("youtu.be");
            ai.setProperty("multiHostSupport", supportedHosts);
        } catch (Throwable e) {
            account.setProperty("multiHostSupport", Property.NULL);
            logger.info("Failed to load Unrestrict.li hosts list. Error:" + e.toString());
        }
        return ai;
    }

    private void login(Account account, boolean force) throws Exception {
        synchronized (LOCK) {
            try {
                br.setCookiesExclusive(true);
                final Object ret = account.getProperty("cookies", null);
                if (ret != null && ret instanceof HashMap<?, ?> && !force) {
                    final HashMap<String, String> cookies = (HashMap<String, String>) ret;
                    if (account.isValid()) {
                        for (final Map.Entry<String, String> cookieEntry : cookies.entrySet()) {
                            final String key = cookieEntry.getKey();
                            final String value = cookieEntry.getValue();
                            this.br.setCookie("http://unrestrict.li", key, value);
                        }
                        return;
                    }
                }
                setBrowser();
                br.postPage("http://unrestrict.li/sign_in", "return=home&username=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()) + "&signin=Log%20in");
                if (br.getCookie("http://unrestrict.li", "unrestrict_user") == null) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
                final HashMap<String, String> cookies = new HashMap<String, String>();
                final Cookies add = this.br.getCookies("http://unrestrict.li");
                for (final Cookie c : add.getCookies()) {
                    cookies.put(c.getKey(), c.getValue());
                }
                account.setProperty("cookies", cookies);
            } catch (final PluginException e) {
                account.setProperty("cookies", Property.NULL);
                throw e;
            }
        }
    }
}
