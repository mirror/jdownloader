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

package jd.plugins.host;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Date;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.http.requests.Request;
import jd.nutils.JDHash;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class Megauploadcom extends PluginForHost {

    private static final String MU_PARAM_PORT = "MU_PARAM_PORT";

    private String user;

    private String dlID;

    private HashMap<String, String> UserInfo = new HashMap<String, String>();

    private static int simultanpremium = 1;

    public Megauploadcom(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://megaupload.com/premium/en/");
        setConfigElements();
    }

    public int usePort() {
        switch (JDUtilities.getConfiguration().getIntegerProperty(MU_PARAM_PORT)) {
        case 1:
            return 800;
        case 2:
            return 1723;
        default:
            return 80;
        }
    }

    public boolean isPremium() {
        if (UserInfo.containsKey("p")) { return true; }
        return false;
    }

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();
        try {
            login(account);
        } catch (PluginException e) {
            ai.setValid(false);
            return ai;
        }
        if (!UserInfo.containsKey("s")) {
            ai.setValid(false);
            return ai;
        }
        if (!UserInfo.containsKey("p")) {
            ai.setValid(true);
            ai.setStatus("Free Membership");
            return ai;
        }
        Date d = new Date(Long.parseLong(UserInfo.get("p")) * 1000l);
        if (d.compareTo(new Date()) >= 0) {
            ai.setValid(true);
        } else {
            ai.setValid(false);
        }
        ai.setValidUntil(d.getTime());
        return ai;
    }

    public String getDownloadID(DownloadLink link) throws MalformedURLException {
        return Request.parseQuery(link.getDownloadURL()).get("d");
    }

    public void handlePremium(DownloadLink link, Account account) throws Exception {
        getFileInformation(link);
        login(account);
        if (!this.isPremium()) {
            simultanpremium = 1;
            handleFree0(link);
            return;
        } else {
            if (simultanpremium + 1 > 20) {
                simultanpremium = 20;
            } else {
                simultanpremium++;
            }
        }
        br.setFollowRedirects(false);
        getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(dlID)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        String url = br.getRedirectLocation();
        url = url.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort() + "/");
        br.setFollowRedirects(true);
        br.setDebug(true);
        dl = br.openDownload(link, url, true, 0);
        if (!dl.getConnection().isOK()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);

        }
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }

        dl.startDownload();
        // Wenn ein Download Premium mit mehreren chunks angefangen wird, und
        // dann versucht wird ihn free zu resumen, schlägt das fehl, weil jd die
        // mehrfachchunks aus premium nicht resumen kann.
        // In diesem Fall wird der link resetted.
        if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && link.getLinkStatus().getErrorMessage().contains("Limit Exceeded")) {
            link.setChunksProgress(null);
            link.getLinkStatus().setStatus(LinkStatus.ERROR_RETRY);
        }
    }

    private void getRedirect(String url, DownloadLink downloadLink) throws PluginException, InterruptedException {
        try {
            br.getPage(url);
        } catch (IOException e) {
            try {
                String passCode;
                if (downloadLink.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput(null, downloadLink);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = downloadLink.getStringProperty("pass", null);
                }
                br.getPage(url + "&p=" + passCode);
                getPluginConfig().setProperty("pass", passCode);
                getPluginConfig().save();
                return;
            } catch (IOException e2) {
                getPluginConfig().setProperty("pass", null);
                getPluginConfig().save();
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }
        }
    }

    public String getAGBLink() {
        return "http://megaupload.com/terms/";
    }

    public void login(Account account) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.postPage("http://megaupload.com/mgr_login.php", "u=" + account.getUser() + "&b=0&p=" + JDHash.getMD5(account.getPass()));
        if (br.toString().equalsIgnoreCase("e")) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        UserInfo = Request.parseQuery(br + "");
        user = UserInfo.get("s");
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        dlID = getDownloadID(downloadLink);
        user = null;
        br.postPage("http://megaupload.com/mgr_linkcheck.php", "id0=" + dlID);
        HashMap<String, String> query = Request.parseQuery(br + "");
        if (!query.containsKey("id0") || !query.get("id0").equals("0") || query.get("n") == null || query.get("s") == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(query.get("n"));
        downloadLink.setDownloadSize(Long.parseLong(query.get("s")));
        downloadLink.setDupecheckAllowed(true);
        return true;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree0(DownloadLink link) throws Exception {
        br.setFollowRedirects(false);
        if (user != null) {
            getRedirect("http://megaupload.com/mgr_dl.php?d=" + dlID + "&u=" + user, link);
        } else {
            br.getPage("/mgr_dl.php?d=" + dlID);
        }
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(dlID)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        String url = br.getRedirectLocation();
        url = url.replaceFirst("megaupload\\.com/", "megaupload\\.com:" + usePort() + "/");
        br.setFollowRedirects(true);
        dl = br.openDownload(link, url, true, 1);
        if (!dl.getConnection().isOK()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
        }

        dl.startDownload();
        // Wenn ein Download Premium mit mehreren chunks angefangen wird, und
        // dann versucht wird ihn free zu resumen, schlägt das fehl, weil jd die
        // mehrfachchunks aus premium nicht resumen kann.
        // In diesem Fall wird der link resetted.
        if (link.getLinkStatus().hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && link.getLinkStatus().getErrorMessage().contains("Limit Exceeded")) {
            link.setChunksProgress(null);
            link.getLinkStatus().setStatus(LinkStatus.ERROR_RETRY);
        }
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        getFileInformation(parameter);
        handleFree0(parameter);
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {
    }

    public int getMaxSimultanPremiumDownloadNum() {
        return simultanpremium;
    }

    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        String[] ports = new String[] { "80", "800", "1723" };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), MU_PARAM_PORT, ports, "Use this Port:").setDefaultValue("80"));
    }
}
