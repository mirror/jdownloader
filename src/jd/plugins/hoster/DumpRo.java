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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dump.ro" }, urls = { "http://[\\w\\.]*?dump\\.ro/[0-9A-Za-z/\\-\\.\\?\\=\\&]+" }, flags = { 0 })
public class DumpRo extends PluginForHost {

    public DumpRo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.dump.ro/termeni-si-conditii";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.setBrowserExclusive();
        Form dlform = br.getFormbyProperty("name", "download");
        if (dlform == null) dlform = br.getForm(2);
        if (dlform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.submitForm(dlform);
        br.setFollowRedirects(false);
        String dllink = br.getRegex("window\\.location='(http.*?)'").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("Link invalid")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Pattern.compile("REGEXP", Pattern.DOTALL);
        String filename = br.getRegex(Pattern.compile("<title>Dump - Fisiere -(.*?)</title>", Pattern.DOTALL)).getMatch(0);
        if (filename == null) filename = br.getRegex(Pattern.compile("<div>Nume fisier:</div>(.*?)<br", Pattern.DOTALL)).getMatch(0);
        String filesize = br.getRegex(Pattern.compile("<div>Marime:</div>(.*?)<br", Pattern.DOTALL)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    /*
     * /* public String getVersion() { return getVersion("$Revision$"); }
     */

}
