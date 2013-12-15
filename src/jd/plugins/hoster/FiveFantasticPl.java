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
        br.getHeaders().put("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64; rv:23.0) Gecko/20100101 Firefox/23.0");
        br.getHeaders().put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
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
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String encodedLink = Encoding.urlEncode(downloadLink.getDownloadURL());
        String vsKey = br.getRegex("type=\"hidden\" name=\"vs_key\" id=\"vs_key\" value=\"([^<>\"]*?)\"").getMatch(0);
        if (vsKey == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        vsKey = Encoding.urlEncode(vsKey);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.getHeaders().put("X-MicrosoftAjax", "Delta=true");
        // Download
        // old
        // &__EVENTTARGET=ctl00%24panelLogowania%24dvZamknijLogowanie
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24panelLogowania%24updPanelLogowania%7Cctl00%24panelLogowania%24dvZamknijLogowanie&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24panelLogowania%24txtLogin=&ctl00%24panelLogowania%24txtPass=&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&__EVENTTARGET=ctl00$ctnMetryka$metrykaPliku$lnkPobierz&__EVENTARGUMENT=&__ASYNCPOST=true&");

        if (br.containsHTML("Przepraszamy, plik jest chwilowo niedostepny")) { throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Link temporary unavailable!", 60 * 1000L); }
        this.sleep(3000, downloadLink);
        // Select FREE download
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierz&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&__EVENTTARGET=ctl00%24ctnMetryka%24metrykaPliku%24lnkZgodaNaPobierz&__EVENTARGUMENT=&__ASYNCPOST=true&");
        this.sleep(3000, downloadLink);
        // Confirm TOS
        br.postPage(br.getURL(), "ctl00%24ScriptManager1=ctl00%24ctnMetryka%24metrykaPliku%24updPobieraniePliku%7Cctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin&ctl00_ScriptManager1_HiddenField=&vs_key=" + vsKey + "&__VIEWSTATE=&ctl00%24hidSuwakStep=0&ctl00%24ctnMetryka%24metrykaPliku%24hidOcena=&ctl00%24ctnMetryka%24metrykaPliku%24inputPrzyjaznyLink=" + encodedLink + "&ctl00%24ctnMetryka%24metrykaPliku%24chkAkcepujeRegulamin=on&__EVENTTARGET=ctl00$ctnMetryka$metrykaPliku$chkAkcepujeRegulamin\\&__EVENTARGUMENT=\\&__LASTFOCUS=&__ASYNCPOST=true&");
        if (br.containsHTML("Z Twojego numeru IP jest już pobierany plik. Poczekaj na zwolnienie zasobów, albo ściągnij plik za punkty")) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Download in progress detected from your IP!", 5 * 60 * 1000l); }
        final String dllink = br.getRegex("\"(https?://[a-z0-9]+\\.5fantastic\\.pl/download\\.php\\?[^<>\"]*?)\"").getMatch(0);
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