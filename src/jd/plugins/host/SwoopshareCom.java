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

package jd.plugins.host;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class SwoopshareCom extends PluginForHost {

    public SwoopshareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://de.swoopshare.com/info/terms";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws Exception {
        int lIndex;
        if ((lIndex = downloadLink.getDownloadURL().lastIndexOf("cshare.de/")) != -1) {
            downloadLink.setUrlDownload("http://www.swoopshare.com/" + downloadLink.getDownloadURL().substring(lIndex + 10));
        }

        setBrowserExclusive();
        br.setFollowRedirects(true);

        br.getPage(downloadLink.getDownloadURL());

        // Filesize
        String size = br.getRegex("</b> \\((.*)yte\\)").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(size));

        // Filename
        String name = br.getRegex("<title>cshare.de - Download (.*)</title>").getMatch(0);
        downloadLink.setName(name);

        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());

        String redirectLocation = br.getURL();
        String country = redirectLocation.substring(0, redirectLocation.lastIndexOf(".swoopshare.com/"));

        String link = country + ".swoopshare.com" + br.getRegex("&#187;</span> <a href=\"(.*?)\"").getMatch(0);
        dl = br.openDownload(downloadLink, link);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
