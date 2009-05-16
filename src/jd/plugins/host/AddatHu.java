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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class AddatHu extends PluginForHost {

    public AddatHu(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public String getAGBLink() {
        return "http://www.addat.hu/";
    }

    //@Override
    public String getCoder() {
        return "TnS";
    }

    //@Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        String[] dat = br.getRegex("<b>http://addat.hu/.*/(.*)</b> \\((.*) bytes\\)").getRow(0);
        long length = Regex.getSize(dat[1].trim());
        downloadLink.setDownloadSize(length);
        downloadLink.setName(dat[0].trim());
        return AvailableStatus.TRUE;
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        requestFileInformation(downloadLink);
        String link = br.getRegex(Pattern.compile("<a href=\"(.*)\"><img border=\"0\" src=\"/images/letoltes_btn.jpg\"></a>", Pattern.CASE_INSENSITIVE)).getMatch(0);
        br.openDownload(downloadLink, link, true, 1).startDownload();
    }

    //@Override
    public int getTimegapBetweenConnections() {
        return 500;
    }

    //@Override
    public int getMaxConnections() {
        return 1;
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    //@Override
    public void reset() {
    }

    //@Override
    public void resetPluginGlobals() {
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub
        
    }
}