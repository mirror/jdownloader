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

package jd.plugins.hoster;

import java.io.IOException;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filesend.net" }, urls = { "http://[\\w\\.]*?filesend\\.net/download\\.php\\?f=[a-z0-9]+" }, flags = { 0 })
public class FileSendNet extends PluginForHost {

    public FileSendNet(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(5000l);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.filesend.net/tos.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("File Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex(Pattern.compile("File Name:</strong>\\s+(.*?)\\s+</td>", Pattern.CASE_INSENSITIVE)).getMatch(0));
        String filesize = br.getRegex("File Size:</strong>\\s+(.*?)\\s+</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * /* public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        // br.setDebug(true);
        String linkform = br.getRegex("innerHTML=.(<form.*?</form>)").getMatch(0);
        String linkaction = new Regex(linkform, "\\smethod=\"POST\"\\saction=\"(.*?)\"").getMatch(0);
        String linkname = new Regex(linkform, "\\stype=\"hidden\"\\sname=\"(.*?)\"").getMatch(0);
        String linkvalue = new Regex(linkform, "\\sname=\"" + linkname + "\"\\svalue=\"(.*?)\"").getMatch(0);
        // System.out.println(linkform+" "+linkaction+" "+linkname+" "+linkvalue);
        if (linkaction == null || linkname == null || linkform == null || linkvalue == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.setFollowRedirects(false);
        // this.sleep(24000, downloadLink); // uncomment when they find a better
        // way to force wait time
        dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, linkaction, linkname + "=" + linkvalue + "&download=");
        dl.startDownload();

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
