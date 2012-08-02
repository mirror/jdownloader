//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

/** Links always come rom a decrypter */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "min.us", "minus.com" }, urls = { "cvj84ezu45gj0wojgHZiF238ß3üpj5uUNUSED_REGEX", "http://(www\\.)?i\\.minus(decrypted)?\\.com/\\d+/[A-Za-z0-9\\-_]+/[A-Za-z0-9\\-_]+/.+" }, flags = { 0, 0 })
public class MinUs extends PluginForHost {

    public MinUs(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void correctDownloadLink(final DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("minusdecrypted.com/", "minus.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://minus.com/pages/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /**
         * More will work fine for pictures but will cause server errors for other links
         */
        return 2;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        /**
         * Resume/Chunks depends on link and/or fileserver so to prevent errors we deactivate it
         */
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            /* linkrefresh is needed here */
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        setBrowserExclusive();
        if (link.getDownloadURL().endsWith(".offline")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}