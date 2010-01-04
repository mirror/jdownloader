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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.RandomUserAgent;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xup.in" }, urls = { "http://[\\w\\.]*?xup\\.in/dl,\\d+/?.+?" }, flags = { 0 })
public class XupIn extends PluginForHost {

    private static final String AGB_LINK = "http://www.xup.in/terms/";

    public XupIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        String filename = br.getRegex("<legend>.*?<.*?>Download:(.*?)</.*?>").getMatch(0);
        String filesize = br.getRegex("File Size:(.*?)</li>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        this.requestFileInformation(downloadLink);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        Form download = br.getForm(0);
        String passCode = null;
        if (download.hasInputFieldByName("vpass")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            download.put("vpass", passCode);
        }
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download);

        if (!dl.getConnection().isContentDisposition()) {
            String page = br.loadConnection(dl.getConnection());
            if (page.contains("richtige Passwort erneut ein")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.xupin.errors.passwrong", "Password wrong"));
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("pass", passCode);
        dl.startDownload();

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
