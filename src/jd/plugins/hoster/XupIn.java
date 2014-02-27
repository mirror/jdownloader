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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "xup.in" }, urls = { "http://[\\w\\.]*?xup\\.(in/dl,\\d+/?.+?|raidrush\\.ws/ndl_[a-z0-9]+)" }, flags = { 0 })
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
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        this.requestFileInformation(downloadLink);
        br.getHeaders().put("User-Agent", RandomUserAgent.generate());
        Form download = br.getForm(0);
        if (download == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        if (download.hasInputFieldByName("vpass")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            download.put("vpass", passCode);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, download);
        if (dl.getConnection().getContentType().contains("html")) {
            String page = br.loadConnection(dl.getConnection()) + "";// +"" due
            // to
            // refaktor
            // compatibilities.
            // old
            // <ref10000
            // returns
            // String.
            // else
            // Request
            // INstance
            if (page.contains("richtige Passwort erneut ein")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY, JDL.L("plugins.hoster.xupin.errors.passwrong", "Password wrong"));
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (passCode != null) downloadLink.setProperty("pass", passCode);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Datei existiert nicht")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = null;
        String filesize = null;
        if (downloadLink.getDownloadURL().contains("xup.raidrush.ws/")) {
            filename = br.getRegex("<title>XUP - Download (.*?) \\| ").getMatch(0);
            if (filename == null) filename = br.getRegex("<h1>XUP - Download (.*?) \\| ").getMatch(0);
            filesize = br.getRegex("Size</font></td>[\t\n\r ]+<td>(\\d+)</td>").getMatch(0);
        } else {
            filename = br.getRegex("<legend>.*?<.*?>Download:(.*?)</.*?>").getMatch(0);
            filesize = br.getRegex("File Size:(.*?)</li>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean allowHandle(final DownloadLink downloadLink, final PluginForHost plugin) {
        return downloadLink.getHost().equalsIgnoreCase(plugin.getHost());
    }

    @Override
    public void reset() {

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }

}