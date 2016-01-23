//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

/*Same script for AbelhasPt, LolaBitsEs, CopiapopEs, MinhatecaComBr*/
/* ChomikujPlScript */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kumpulbagi.com" }, urls = { "http://kumpulbagidecrypted\\.com/\\d+" }, flags = { 0 })
public class KumpulbagiCom extends PluginForHost {

    public KumpulbagiCom(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(MAINPAGE);
    }

    @Override
    public String getAGBLink() {
        return "http://kumpulbagi.com/termosecondicoes.aspx";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        final String url = link.getStringProperty("mainlink", null);
        if (url == null) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(url);
        if (br.containsHTML("class=\"noFile\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = link.getStringProperty("plain_filename", null);
        final String filesize = link.getStringProperty("plain_filesize", null);
        link.setFinalFileName(filename);
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink, true, 0, "free_directlink");
    }

    public void doFree(final DownloadLink downloadLink, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        handlePWProtected(downloadLink);
        final String req_token = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
        final String fid = downloadLink.getStringProperty("plain_fid", null);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            if (req_token == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
            br.postPage("/action/DownloadFile?location=fi&f=" + fid, "fileId=" + fid + "&__RequestVerificationToken=" + Encoding.urlEncode(req_token));
            dllink = PluginJSonUtils.getJson(br, "DownloadUrl");
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, resumable, maxchunks);
        if (dl.getConnection().getContentType().contains("html") && dl.getConnection().getResponseCode() != 206) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private void handlePWProtected(final DownloadLink dl) throws PluginException, IOException {
        String passCode = dl.getStringProperty("pass", null);
        if (br.containsHTML("class=\"LoginToFolderForm\"")) {
            final String reqtoken = br.getRegex("name=\"__RequestVerificationToken\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            final String chomikid = br.getRegex("type=\"hidden\" name=\"ChomikId\" value=\"(\\d+)\"").getMatch(0);
            final String folderid = br.getRegex("name=\"FolderId\" type=\"hidden\" value=\"(\\d+)\"").getMatch(0);
            final String foldername = br.getRegex("id=\"FolderName\" name=\"FolderName\" type=\"hidden\" value=\"([^<>\"]*?)\"").getMatch(0);
            if (reqtoken == null || chomikid == null || folderid == null || foldername == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (int i = 1; i <= 3; i++) {
                if (passCode == null) {
                    passCode = Plugin.getUserInput("Password?", dl);
                }
                br.postPageRaw("/action/Files/LoginToFolder", "Remember=true&Remember=false&ChomikId=" + chomikid + "&FolderId=" + folderid + "&FolderName=" + Encoding.urlEncode(foldername) + "&Password=" + Encoding.urlEncode(passCode) + "&__RequestVerificationToken=" + Encoding.urlEncode(reqtoken));
                if (br.containsHTML("\"IsSuccess\":false")) {
                    passCode = null;
                    dl.setProperty("pass", Property.NULL);
                    continue;
                }
                break;
            }
            if (br.containsHTML("\"IsSuccess\":false")) {
                throw new PluginException(LinkStatus.ERROR_RETRY, "Wrong password entered");
            }
            /* We don't want to work with the encoded json bla html response */
            br.getPage(dl.getStringProperty("mainlink", null));
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }
}