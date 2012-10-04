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

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bezvadata.cz" }, urls = { "http://(www\\.)?bezvadata\\.cz/stahnout/\\d+" }, flags = { 0 })
public class BezVadataCz extends PluginForHost {

    public BezVadataCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://bezvadata.cz/smluvni-podminky/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        br.postPage(br.getURL() + "?do=stahnoutForm-submit", "stahnoutSoubor=St%C3%A1hnout");
        String dllink = br.getRedirectLocation();
        try {
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            if (dl.getConnection().getContentType().contains("html")) {
                br.followConnection();
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dl.startDownload();
        } catch (PluginException e) {
            if (br.getRegex("soubor nen. v tuto chv.li dostupn.").matches())
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            else
                throw e;
        }
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>Soubor nenalezen<|<title>BezvaData \\| Soubor nenalezen</title>|Omlouváme se, soubor byl již odstraněn na žádost autora nebo z důvodů porušování autorských práv\\.|>Tento soubor byl na žádost uživatele nebo vlastníka)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("<title>BezvaData\\.cz \\| St.hnout soubor (.*?)</title>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1 title=\"St.hnout soubor (.*?)\">").getMatch(0);
        String filesize = br.getRegex("strong>Velikost:</strong> (.*?)</li>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);

        link.setName(filename.trim());
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}