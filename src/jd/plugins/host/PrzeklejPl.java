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
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class PrzeklejPl extends PluginForHost {

    
    private static final String PATTERN_PASSWORD_WRONG = "B≥Ídnie podane has≥o!";
    
    public PrzeklejPl(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    @Override
    public String getAGBLink() {
        return "http://przeklej.pl/regulamin";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("<h1 style=\"font-size: 40px;\">Podana strona nie istnieje</h1>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("<p>(.*?)</p><!-- tu potrzeba ograniczania dlugosci nazwy pliku -->", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex(Pattern.compile("<span class=\"size\">\\((.*?)\\)</span>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4228 $");
    }
    

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
       String passCode = null;
        if (!br.containsHTML("<span class=\"unbold\">Wprowadü has≥o:</span>")) {
            String linkurl = downloadLink.getDownloadURL();
            // String linkurl = Encoding.htmlDecode(br.getRegex("<a class=\"download\" href=\"(.*?)\">Pobierz plik</a>").getMatch(0));
            if (linkurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.setFollowRedirects(true);
            dl = br.openDownload(downloadLink, linkurl);
            dl.startDownload();
        }
        else {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            Form form = br.getFormbyName("haselko");
            form.put("haslo[haslo]", passCode);
            br.setFollowRedirects(true);
            br.submitForm(form);
            if (br.containsHTML(PATTERN_PASSWORD_WRONG)) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_FATAL, JDLocale.L("plugins.errors.wrongpassword", "Password wrong"));
            } else {
                downloadLink.setProperty("pass", passCode);
                dl = br.openDownload(downloadLink, form, true, 1);
                if (!dl.getConnection().isContentDisposition()) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT); }
                dl.startDownload();
                
            }
        }
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