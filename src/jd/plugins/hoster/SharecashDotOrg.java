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

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharecash.org" }, urls = { "http://[\\w\\.]*?sharecash\\.org/download.php\\?(file|id)=\\d+" }, flags = { 0 })
public class SharecashDotOrg extends PluginForHost {
    public SharecashDotOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://sharecash.org/tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        try {
            br.getPage(downloadLink.getDownloadURL());
            String filename = br.getRegex("<td width=\"120\"><strong>(.*?)</strong></td>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            long size = Regex.getSize(br.getRegex("<b>Size:</b>(.*?)</td>").getMatch(0));
            String md5hash = br.getRegex("<b>MD5:</b>(.*?)<div").getMatch(0);
            if (md5hash != null) downloadLink.setMD5Hash(md5hash);
            downloadLink.setFinalFileName(filename.trim());
            downloadLink.setDownloadSize(size);
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SharecashDotNet.only4premium", "This file is only downloadable for premium users!"));
            return AvailableStatus.TRUE;
        } catch (NullPointerException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("plugins.hoster.SharecashDotNet.only4premium", "This file is only downloadable for premium users!"));
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
