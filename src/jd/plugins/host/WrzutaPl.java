//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.host;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class WrzutaPl extends PluginForHost {


    public WrzutaPl(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wrzuta.pl/regulamin/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Nie odnaleziono pliku.")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filetype = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/([^/]*)").getMatch(0);
        String filext = null;
        if (filetype.equalsIgnoreCase("film")) filext = "flv";
        if (filetype.equalsIgnoreCase("audio")) filext = "mp3";
        if (filetype.equalsIgnoreCase("obraz")) filext = "jpg";
        String filename = (Encoding.htmlDecode(br.getRegex(Pattern.compile("anie</h3><h2>(.*?)</h2><div", Pattern.CASE_INSENSITIVE)).getMatch(0)) + "." + filext );
        String filesize = br.getRegex(Pattern.compile("Rozmiar: <strong>(.*?)</strong>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filext == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        downloadLink.setName(filename);
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4390 $");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
    getFileInformation(downloadLink);

    String filtype = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/([^/]*)").getMatch(0);
    String filid = new Regex(downloadLink.getDownloadURL(), ".*?wrzuta.pl/"+filtype+"/([^/]*)").getMatch(0);
    String linkurl = null;
    String filname = downloadLink.getName();
    System.out.println("NAZWA: "+filname);
    if (filtype.equalsIgnoreCase("audio")) {
    linkurl="http://wrzuta.pl/aud/file/"+filid+"/";
    }
    if (filtype.equalsIgnoreCase("film")) {
        linkurl="http://wrzuta.pl/vid/file/"+filid+"/";
    }  
    if (filtype.equalsIgnoreCase("obraz")) {
        linkurl="http://wrzuta.pl/img/file/"+filid+"/";
    }
    if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
    br.setFollowRedirects(true);
    dl = br.openDownload(downloadLink, linkurl);
    dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}