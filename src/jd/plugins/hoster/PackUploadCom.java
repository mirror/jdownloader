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
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "packupload.com" }, urls = { "http://([a-z]+\\.)?packupload.com/[A-Z0-9]+" }, flags = { 0 })
public class PackUploadCom extends PluginForHost {

    public PackUploadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.packupload.com/cgu";
    }

    private static final String            INVALIDLINKS = "http://([a-z]+\\.)?packupload.com/(contact|reportFile|register|functionalities|news|about|emptyPage|connect|donate|myfiles)";
    private static AtomicReference<String> userAgent    = new AtomicReference<String>(null);

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getDownloadURL().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        this.setBrowserExclusive();
        if (userAgent.get() == null) {
            /* we first have to load the plugin, before we can reference it */
            JDUtilities.getPluginForHost("mediafire.com");
            userAgent.set(jd.plugins.hoster.MediafireCom.stringUserAgent());
        }
        br.getHeaders().put("User-Agent", userAgent.get());
        br.getHeaders().put("Accept-Language", "en-US,en;q=0.5");
        br.setFollowRedirects(true);
        br.getPage(link.getDownloadURL());
        if (br.getRequest().getHttpConnection().getResponseCode() == 403 || br.containsHTML(">Oops, page non trouvée|La page que vous essayez d\\'afficher n\\'existe pas|>Fichier ou dossier indisponible<|>Unavailable file or folder<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String filename = br.getRegex("<title>Download ([^<>\"]*?) for free \\- PackUpload</title>").getMatch(0);
        final String filesize = br.getRegex(">Size :</span> <span style=\"[^<>\"]*?\">([^<>\"]*?)</span>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim().replace(",", ".")));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final String dllink = br.getRegex("\"(http://s\\d+\\.packupload\\.com/[A-Z0-9]+)\"").getMatch(0);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        // following wont be used
        int wait = 15;
        final String waittime = br.getRegex("var delay = (\\d+);").getMatch(0);
        if (waittime != null) {
            wait = Integer.parseInt(waittime);
        }
        // these guys impose more wait than advertised, in browser and in JD
        while (wait < 100 || wait > 180) {
            wait = new Random().nextInt(180);
        }
        sleep(wait * 1001l, downloadLink); // Additional wait time is needed (You must wait ...)
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, "", false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">You must wait befor downloading this file\\.<")) {
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML(">The requested file is impossible to find\\.<")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}