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

public class FilelocityCom extends PluginForHost {

    public FilelocityCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(62000l);
    }

    @Override
    public String getAGBLink() {
        return "http://filelocity.com/?act=terms";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("requested to download is unavailable")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("requested\\sto\\sdownload\\s<b>'(.*?)'</b>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("The\\sfile\\ssize\\sis:\\s<b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        br.setDebug(true);
        String link = downloadLink.getDownloadURL();
        br.setFollowRedirects(false);
        // this.sleep(7001, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = br.openDownload(downloadLink, link, "submit2=Download+now...");
        if (dl.getConnection().getContentType().equalsIgnoreCase("text/html")) {
            String wait = br.getRegex("wait\\sat\\sleast\\s(.*?)\\smore\\sseconds").getMatch(0);
            dl.getConnection().disconnect();
            this.sleep((1+(Integer.parseInt(wait))*1001), downloadLink);
            }
        dl.startDownload();

    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}