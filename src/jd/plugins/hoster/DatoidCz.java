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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datoid.cz" }, urls = { "http://(www\\.)?datoid\\.(cz|sk)/[A-Za-z0-9]+/.{1}" }, flags = { 2 })
public class DatoidCz extends PluginForHost {

    public DatoidCz(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("http://datoid.cz/cenik");
        // Prevents server errors
        this.setStartIntervall(2 * 1000);
    }

    @Override
    public String getAGBLink() {
        return "http://datoid.cz/kontakty";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("datoid.sk/", "datoid.cz/"));
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage("http://api.datoid.cz/v1/get-file-details?url=" + Encoding.urlEncode(link.getDownloadURL()));
        if (br.containsHTML("\"error\":\"File not found\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = getJson("filename");
        final String filesize = getJson("filesize_bytes");
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<div class=\"bPopup free-popup file-on-page big-file\">")) {
            logger.info("Only downloadable by Premium Account holders");
            try {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
            } catch (final Throwable e) {
                if (e instanceof PluginException) throw (PluginException) e;
            }
            throw new PluginException(LinkStatus.ERROR_FATAL, "This file can only be downloaded by premium users");
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getPage(br.getURL().replace("datoid.cz/", "datoid.cz/f/") + "?request=1&_=" + System.currentTimeMillis());
        if (br.containsHTML("\"error\":\"IP in use\"")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        // final int wait = Integer.parseInt(getJson("wait"));
        String dllink = getJson("download_link");
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        // Can be skipped
        // sleep(wait * 1001l, downloadLink);
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String getJson(final String parameter) {
        String result = br.getRegex("\"" + parameter + "\":(\\d+)").getMatch(0);
        if (result == null) result = br.getRegex("\"" + parameter + "\":\"([^<>\"]*?)\"").getMatch(0);
        return result;
    }

    private String TOKEN = null;

    @SuppressWarnings("unchecked")
    private void login(final Account account) throws Exception {
        br.setFollowRedirects(false);
        br.getPage("http://api.datoid.cz/v1/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        if (br.containsHTML("\\{\"success\":false\\}")) throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
        TOKEN = getJson("token");
        account.setProperty("logintoken", TOKEN);
    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        try {
            login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("http://api.datoid.cz/v1/get-user-details?token=" + TOKEN);
        /** 1 Credit = 1 MB */
        final String credits = getJson("credits");
        ai.setTrafficLeft(SizeFormatter.getSize(credits + " MB"));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        TOKEN = account.getStringProperty("logintoken", null);
        br.getPage("http://api.datoid.cz/v1/get-download-link?token=" + TOKEN + "&url=" + Encoding.urlEncode(link.getDownloadURL()));
        if (br.containsHTML("\"error\":\"Lack of credits\"")) throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        if (br.containsHTML("\"error\":\"File not found\"")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String dllink = getJson("download_link");
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = dllink.replace("\\", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, Encoding.htmlDecode(dllink), true, -3);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return 5;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}