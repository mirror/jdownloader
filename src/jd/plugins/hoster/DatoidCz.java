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
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "datoid.cz" }, urls = { "https?://(www\\.)?datoid\\.(cz|sk)/[A-Za-z0-9]+(?:/.*)?" })
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

    @SuppressWarnings("deprecation")
    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        br = new Browser();
        this.setBrowserExclusive();
        String downloadURL = link.getDownloadURL();
        br.getPage("http://api.datoid.cz/v1/get-file-details?url=" + Encoding.urlEncode(downloadURL));
        if (br.containsHTML("\"error\":\"File not found\"") && StringUtils.startsWithCaseInsensitive(downloadURL, "https://")) {
            downloadURL = downloadURL.replace("https://", "http://");
            br.getPage("http://api.datoid.cz/v1/get-file-details?url=" + Encoding.urlEncode(downloadURL));
        }
        if (br.containsHTML("\"error\":\"(File not found|File was blocked|File was deleted)\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("\"error\":\"File is password protected\"")) {
            logger.info("Password protected links are not yet supported (via API)!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = PluginJSonUtils.getJsonValue(br, "filename");
        final String filesize = PluginJSonUtils.getJsonValue(br, "filesize_bytes");
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(Long.parseLong(filesize));
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<div class=\"bPopup free-popup file-on-page big-file\">")) {
            logger.info("Only downloadable by Premium Account holders");
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        }
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("Accept", "application/json, text/javascript, */*; q=0.01");
        br.getPage(br.getURL().replace("datoid.cz/", "datoid.cz/f/") + "?request=1&_=" + System.currentTimeMillis());
        if (br.containsHTML("\"error\":\"IP in use\"")) {
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        }
        if (br.containsHTML("\"No anonymous free slots\"")) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "No free slots available", 5 * 60 * 1000l);
        }
        // final int wait = Integer.parseInt(getJson("wait"));
        String dllink = PluginJSonUtils.getJsonValue(br, "download_link");
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // Can be skipped
        // sleep(wait * 1001l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @SuppressWarnings("unchecked")
    private String login(final Account account) throws Exception {
        br.setFollowRedirects(false);
        br.getPage("http://api.datoid.cz/v1/login?email=" + Encoding.urlEncode(account.getUser()) + "&password=" + Encoding.urlEncode(account.getPass()));
        try {
            if (br.containsHTML("\\{\"success\":false\\}")) {
                throw new PluginException(LinkStatus.ERROR_PREMIUM, "\r\nInvalid username/password!\r\nUngültiger Benutzername oder ungültiges Passwort!", PluginException.VALUE_ID_PREMIUM_DISABLE);
            }
            final String TOKEN = PluginJSonUtils.getJsonValue(br, "token");
            if (TOKEN == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            account.setProperty("logintoken", TOKEN);
            return TOKEN;
        } catch (PluginException e) {
            account.removeProperty("logintoken");
            throw e;
        }

    }

    @Override
    public AccountInfo fetchAccountInfo(final Account account) throws Exception {
        AccountInfo ai = new AccountInfo();
        final String TOKEN;
        try {
            TOKEN = login(account);
        } catch (PluginException e) {
            account.setValid(false);
            throw e;
        }
        br.getPage("http://api.datoid.cz/v1/get-user-details?token=" + TOKEN);
        /** 1 Credit = 1 MB */
        final String credits = PluginJSonUtils.getJsonValue(br, "credits");
        ai.setTrafficLeft(SizeFormatter.getSize(credits + " MB"));
        account.setValid(true);
        ai.setStatus("Premium User");
        return ai;
    }

    @Override
    public void handlePremium(final DownloadLink link, final Account account) throws Exception {
        requestFileInformation(link);
        br.setFollowRedirects(false);
        final String TOKEN = account.getStringProperty("logintoken", null);
        if (TOKEN == null) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        String downloadURL = link.getDownloadURL();
        br.getPage("http://api.datoid.cz/v1/get-download-link?token=" + TOKEN + "&url=" + Encoding.urlEncode(downloadURL));
        if (br.containsHTML("\"error\":\"File not found\"") && StringUtils.startsWithCaseInsensitive(downloadURL, "https://")) {
            downloadURL = downloadURL.replace("https://", "http://");
            br.getPage("http://api.datoid.cz/v1/get-download-link?token=" + TOKEN + "&url=" + Encoding.urlEncode(downloadURL));
        }
        if (br.containsHTML("\"error\":\"Lack of credits\"")) {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        }
        if (br.containsHTML("\"error\":\"File not found\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String dllink = PluginJSonUtils.getJsonValue(br, "download_link");
        if (dllink == null) {
            logger.warning("Final downloadlink (String is \"dllink\") regex didn't match!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
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