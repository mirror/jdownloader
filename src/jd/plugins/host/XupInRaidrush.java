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

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class XupInRaidrush extends PluginForHost {

    private static final String AGB_LINK = "http://www.xup.in/terms/";

    public XupInRaidrush(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return AGB_LINK;
    }

    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.getPage(downloadLink.getDownloadURL());
        String title = br.getRegex("<title>(.*?)\\|.*?</title>").getMatch(0).trim();
        String size = br.getRegex("Gr.*?e / Size</font></td>.*?<td>(\\d+?)</td>").getMatch(0).trim();

        downloadLink.setName(title);
        downloadLink.setDownloadSize(Integer.parseInt(size));
        return true;

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        if (!getFileInformation(downloadLink)) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        Form download = br.getForms()[0];
        br.openDownload(downloadLink, download).startDownload();

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