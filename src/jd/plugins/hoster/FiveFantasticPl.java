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
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "5fantastic.pl" }, urls = { "http://(www\\.)?5fantastic\\.pl/plikosfera/\\d+/[A-Za-z]+/\\d+" }, flags = { 0 })
public class FiveFantasticPl extends PluginForHost {

    public FiveFantasticPl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.5fantastic.pl/strona.aspx?gidart=4558";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<title>[\t\n\r ]+5fantastic\\.pl \\- najlepszy darmowy dysk internetowy \\- pliki udostępnione przez klubowiczów[\t\n\r ]+</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        final String filename = br.getRegex("<title>[\t\n\r ]+5fantastic\\.pl \\- ([^<>\"]*?) \\- najlepszy darmowy dysk internetowy[\t\n\r ]+</title>").getMatch(0);
        final String filesize = br.getRegex(">Rozmiar: </span><span style=\"margin\\-left:5px;line\\-height:15px;\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String vsKey = br.getRegex("type=\"hidden\" name=\"vs_key\" id=\"vs_key\" value=\"([^<>\"]*?)\"").getMatch(0);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-MicrosoftAjax", "Delta=true");
        // Download
        br.postPage(downloadLink.getDownloadURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24updLnkPobierz%7Cctl00%24ctnMetryka%24lnkPobierz&ctl00_ScriptManager1_HiddenField=&__EVENTTARGET=ctl00%24ctnMetryka%24lnkPobierz&__EVENTARGUMENT=&vs_key=" + Encoding.urlEncode(vsKey) + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPlikuGalerii=0&ctl00%24ctnMetryka%24hidOcena=&ctl00%24ctnMetryka%24inputPrzyjaznyLink=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&__ASYNCPOST=true&");
        // Select FREE download
        br.postPage(downloadLink.getDownloadURL(), "criptManager1=ctl00%24ctnMetryka%24updPobieraniePliku%7Cctl00%24ctnMetryka%24lnkZgodaNaPobierz&ctl00_ScriptManager1_HiddenField=&vs_key=" + Encoding.urlEncode(vsKey) + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPlikuGalerii=0&ctl00%24ctnMetryka%24hidOcena=&ctl00%24ctnMetryka%24inputPrzyjaznyLink=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&__EVENTTARGET=ctl00%24ctnMetryka%24lnkZgodaNaPobierz&__EVENTARGUMENT=&__ASYNCPOST=true&");
        // Confirm TOS
        br.postPage(downloadLink.getDownloadURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24updPobieraniePliku%7Cctl00%24ctnMetryka%24chkAkcepujeRegulamin&ctl00_ScriptManager1_HiddenField=&vs_key=" + Encoding.urlEncode(vsKey) + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPlikuGalerii=0&ctl00%24ctnMetryka%24hidOcena=&ctl00%24ctnMetryka%24inputPrzyjaznyLink=" + Encoding.urlEncode(downloadLink.getDownloadURL()) + "&ctl00%24ctnMetryka%24chkAkcepujeRegulamin=on&__EVENTTARGET=ctl00%24ctnMetryka%24chkAkcepujeRegulamin&__EVENTARGUMENT=&__LASTFOCUS=&__ASYNCPOST=true&");
        if (br.containsHTML("Z Twojego numeru IP jest już pobierany plik. Poczekaj na zwolnienie zasobów, albo ściągnij plik za punkty")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download in progress detected from your IP!", 10 * 60 * 1000l); }
        final String dllink = br.getRegex("\"(https?://plik\\d+\\.5fantastic\\.pl/download\\.php\\?[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
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