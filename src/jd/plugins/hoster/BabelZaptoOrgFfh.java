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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "babel.zapto.org" }, urls = { "http://[\\w\\.]*?babel\\.zapto\\.org/ffh/download\\.php\\?file=[a-z0-9]+" }, flags = { 0 })
public class BabelZaptoOrgFfh extends PluginForHost {

    public BabelZaptoOrgFfh(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String DOMAIN = "babel.zapto.org";
    private static final String IP     = "136.145.164.231:8080";

    @Override
    public String getAGBLink() {
        return "http://babel.zapto.org/ffh/index.php?page=tos";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL().replace(DOMAIN, IP));
        if (br.containsHTML(">Enlace para bajar invalido\\.<br")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInformation = br.getRegex("<br /><h1>(.*?) - ([0-9\\.]+ [a-zA-Z]+)</h1><div id=\"dl\" align=\"center\">");
        String filename = fileInformation.getMatch(0);
        String filesize = fileInformation.getMatch(1);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("document\\.getElementById\\(\"dl\"\\)\\.innerHTML = \\'<a href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://babel\\.zapto\\.org/ffh/d2\\.php\\?a=[a-z0-9]+\\&b=[a-z0-9]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = dllink.replace(DOMAIN, IP);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}