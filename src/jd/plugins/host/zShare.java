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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class zShare extends PluginForHost {

    public zShare(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zshare.net/TOS.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));
            String[] fileInfo = br.getRegex("File Name: .*?<font color=\".666666\">(.*?)</font>.*?Image Size: <font color=\".666666\">([0-9\\.\\,]*)(.*?)</font></td>").getRow(0);
            downloadLink.setName(fileInfo[0]);
            try {
                double length = Double.parseDouble(fileInfo[1].replaceAll("\\,", "").trim());
                int bytes;
                if (fileInfo[2].equalsIgnoreCase("kb")) {
                    bytes = (int) (length * 1024);
                } else if (fileInfo[2].equalsIgnoreCase("mb")) {
                    bytes = (int) (length * 1024 * 1024);
                } else {
                    bytes = (int) length;
                }
                downloadLink.setDownloadSize(bytes);
            } catch (Exception e) {
            }
            // Datei ist noch verfuegbar
            return true;
        } catch (Exception e) {
            // TODO: handle exception
        }
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        br.getPage(downloadLink.getDownloadURL().replaceFirst("zshare.net/(download|video|audio|flash)", "zshare.net/image"));

        Regex reg = br.getRegex("<img src=\"(http://[^\"]*?/download/[a-f0-9]*?/[\\d]*?/[\\d]*?/.*?)\"");

        String url = reg.getMatches()[0][0];

        dl = new RAFDownload(this, downloadLink, br.createGetRequest(url));

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
