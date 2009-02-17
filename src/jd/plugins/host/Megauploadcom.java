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
import java.util.Date;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.http.requests.Request;
import jd.nutils.JDHash;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Megauploadcom extends PluginForHost {

    private static final String MU_PARAM_PORT = "MU_PARAM_PORT";

    private String user;

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

    public AccountInfo getAccountInformation(Account account) throws Exception {
        AccountInfo ai = new AccountInfo(this, account);
        this.setBrowserExclusive();

        br.postPage("http://megaupload.com/mgr_login.php", "u=" + account.getUser() + "&b=0&p=" + JDHash.getMD5(account.getPass()));
        logger.finer(br+"");
        HashMap<String, String> query = Request.parseQuery(br + "");
        this.user = query.get("s");
        String validUntil = query.get("p");
        Date d = new Date(Long.parseLong(validUntil.trim()) * 1000l);
        if (d.compareTo(new Date()) >= 0) {
            ai.setValid(true);
        } else {
            ai.setValid(false);
        }
        ai.setValidUntil(d.getTime());
        return ai;
    }

    public void handlePremium(DownloadLink parameter, Account account) throws Exception {
        br.forceDebug(true);
        LinkStatus linkStatus = parameter.getLinkStatus();
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String id = Request.parseQuery(link).get("d");

        AccountInfo ai = this.getAccountInformation(account);
        if (!ai.isValid()) { throw new PluginException(LinkStatus.ERROR_PREMIUM, LinkStatus.VALUE_ID_PREMIUM_DISABLE); }
        br.setFollowRedirects(false);

        getRedirect("http://megaupload.com/mgr_dl.php?d=" + id + "&u=" + user, downloadLink);
        logger.finer(br+"");
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(id)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 0);
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
        if (linkStatus.hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && linkStatus.getErrorMessage().contains("Limit Exceeded")) {
            downloadLink.setChunksProgress(null);
            linkStatus.setStatus(LinkStatus.ERROR_RETRY);
        }
    }

    /**
     * checks password and gets the final redirect url
     * 
     * @param string
     * @throws PluginException
     * @throws InterruptedException
     */
    private void getRedirect(String url, DownloadLink downloadLink) throws PluginException, InterruptedException {
        try {
            br.getPage(url);
        } catch (IOException e) {
            int pi = 0;
            String pass = this.getPluginConfig().getStringProperty("PASSWORD");
            while (true) {
                e.printStackTrace();
                try {
                    this.br.getPage(url + "&p=" + pass);
                    this.getPluginConfig().setProperty("PASSWORD", pass);
                    this.getPluginConfig().save();
                    break;
                } catch (IOException e2) {
                    pass = JDUtilities.getUserInput(JDLocale.LF("plugins.host.megaupload.getpassword", "Get Password for %s", downloadLink.getName()));
                    if (pass == null) { throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.megaupload.pw_wring", "Password wrong")); }
                }
                pi++;
                if (pi > 3) {
                    if (pass == null) { throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.host.megaupload.pw_wring", "Password wrong")); }
                }
            }
        }

    }

    public String getAGBLink() {
        return "http://megaupload.com/terms/";
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String id = Request.parseQuery(link).get("d");
        br.postPage("http://megaupload.com/mgr_linkcheck.php", "id0=" + id);
logger.finer(br+"");
        HashMap<String, String> query = Request.parseQuery(br + "");
        if (!query.get("id0").equals("0")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setFinalFileName(query.get("n"));

        downloadLink.setDownloadSize(Long.parseLong(query.get("s")));
        return true;
    }

    public String getVersion() {
        return getVersion("$Revision$");
    }

    public void handleFree(DownloadLink parameter) throws Exception {
        br.forceDebug(true);
        getFileInformation(parameter);

        LinkStatus linkStatus = parameter.getLinkStatus();
        DownloadLink downloadLink = (DownloadLink) parameter;
        String link = downloadLink.getDownloadURL().replaceAll("/de", "");
        String id = Request.parseQuery(link).get("d");

        br.setFollowRedirects(false);
        getRedirect("http://megaupload.com/mgr_dl.php?d=" + id, downloadLink);
        logger.finer(br+"");
        if (br.getRedirectLocation() == null || br.getRedirectLocation().contains(id)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 15 * 60 * 1000l); }
        dl = br.openDownload(downloadLink, br.getRedirectLocation(), true, 1);
        if (!dl.getConnection().isOK()) {
            linkStatus.addStatus(LinkStatus.ERROR_IP_BLOCKED);
            if (dl.getConnection().getHeaderField("Retry-After") != null) {
                dl.getConnection().disconnect();
                br.getPage("http://megaupload.com/premium/de/?");
                String wait = br.getRegex("Warten Sie bitte (.*?)Minuten").getMatch(0);
                if (wait != null) {
                    linkStatus.setValue(Integer.parseInt(wait.trim()) * 60 * 1000l);
                } else {
                    linkStatus.setValue(120 * 60 * 1000);
                }
                return;
            } else {
                dl.getConnection().disconnect();
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 30 * 60 * 1000l);
            }

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
        if (linkStatus.hasStatus(LinkStatus.ERROR_DOWNLOAD_FAILED) && linkStatus.getErrorMessage().contains("Limit Exceeded")) {
            downloadLink.setChunksProgress(null);
            linkStatus.setStatus(LinkStatus.ERROR_RETRY);
        }

    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void reset() {

    }

    // public int getMaxSimultanPremiumDownloadNum() {
    // return simultanpremium;
    // }

    public void resetPluginGlobals() {
    }

    private void setConfigElements() {
        // String[] ports = new String[] { "80", "800", "1723" };
        // config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX,
        // JDUtilities.getConfiguration(), MU_PARAM_PORT, ports,
        // "Use this Port:").setDefaultValue("80"));
    }
}
