//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class YourFilesBiz extends PluginForHost {

    public YourFilesBiz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://yourfiles.biz/rules.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());

        downloadLink.setName(br.getRegex("<td align=left width=20%><b>Dateiname:</b></td>[\\s]*?<td align=left width=80%>(.*?)</td>").getMatch(0).trim());
        downloadLink.setDownloadSize(Regex.getSize(br.getRegex("<td align=left><b>Dateigr.*?e:</b></td>.*?<td align=left>(.*?)</td>").getMatch(0).trim()));

        return true;

    }

    @Override
    public String getVersion() {
        
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String url = br.getRegex(Pattern.compile("value='http://(.*?)'>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        br.openDownload(downloadLink, "http://" + url).startDownload();
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