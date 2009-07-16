//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org & pspzockerscene
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
// add errorhandling for error after the dl start "<b>Security Risk:</b> The file name does not match it's UID<br>This may be a fake file and for your security, we will not let you download it"
// add errorhandling leere Page nach dl start

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "ipauploader.com" }, urls = { "http://[\\w\\.]*?ipauploader.com/download/[a-z|0-9]+/.+" }, flags = { 0 })
public class IPAUploaderCom extends PluginForHost {

    public IPAUploaderCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://ipauploader.com/index.php#about";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("The file you are requesting doesn't seem to exist in our database!")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<title>IPAUploader :: Download File :: (.*?)</title>").getMatch(0));
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        return AvailableStatus.TRUE;
    }

    // @Override
    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        Form DLForm = br.getForm(0);
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

        br.openDownload(downloadLink, DLForm, false, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            logger.warning("The link you added isn't valid!");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "See log");

        }

        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

}