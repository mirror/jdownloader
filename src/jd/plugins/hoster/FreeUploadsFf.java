//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "freeuploads.fr" }, urls = { "https?://(www\\.)?(freeuploads\\.fr|uploa\\.dk)/\\?(v|d)=\\d+" }, flags = { 0 })
public class FreeUploadsFf extends PluginForHost {

    // DEV NOTES:
    // protocol: no https
    // free: resumes, one chunk, unlimited connections?
    // captchatype: null

    public FreeUploadsFf(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) throws Exception {
        link.setUrlDownload(link.getDownloadURL().replaceAll("s?://(freeuploads\\.fr|uploa\\.dk)/\\?(v|d)=", "://www.freeuploads.fr/?d="));
    }

    @Override
    public String getAGBLink() {
        return "http://www.freeuploads.fr";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.setCookie("http://www.freeuploads.fr/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(?i)>This video may have been deleted or you\\'re using an invalid link\\.<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("(?i)id=\"download\">[\r\n\t ]+<h2>([^<>\"]+)</h2>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String filesize = br.getRegex("(?i)Size:?</b> ([\\d\\.]+ ?(GB|MB))").getMatch(0);
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String passCode = null;
        String dllink = downloadLink.getStringProperty("freelink");
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty("freelink", Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty("freelink", Property.NULL);
                dllink = null;
            }
        }
        Form passForm = br.getFormbyProperty("name", "filepassword");
        if (passForm != null) {
            logger.info("FreeUploads: Download seems to be password protected!");
            for (int i = 0; i <= 5; i++) {
                passCode = downloadLink.getStringProperty("pass", null);
                if (passCode == null) passCode = Plugin.getUserInput("FreeUploads.fr: Download password protected!", downloadLink);
                passForm.put("pass", Encoding.urlEncode(passCode));
                br.submitForm(passForm);
                if (br.containsHTML(">Wrong Password<"))
                    continue;
                else
                    break;
            }
        }
        if (dllink == null) {
            dllink = br.getRegex("(?i)(https?://[\\w\\-\\.]+freeuploads\\.fr/download/\\w+/[^\">]+)").getMatch(0);
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}