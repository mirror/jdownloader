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

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

//Links come from a decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "soundsnap.com" }, urls = { "decryptedsndspnr=\\d+" })
public class SoundSnapCom extends PluginForHost {
    public SoundSnapCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // Lets make working links out of the crap that comes from the decrypter
    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("decryptedsndspnr=", "http://www.soundsnap.com/audio/play/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.soundsnap.com/tos";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), true, 0);
        if (!looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final URLConnectionAdapter con = br.openGetConnection(br.getRedirectLocation());
        if (!looksLikeDownloadableContent(con)) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            con.disconnect();
            final long filesize = con.getLongContentLength();
            String filename = getFileNameFromHeader(con);
            link.setName(filename.trim());
            if (filesize > 0) {
                link.setVerifiedFileSize(filesize);
            }
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}