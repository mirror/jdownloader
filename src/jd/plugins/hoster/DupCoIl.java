//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dup.co.il" }, urls = { "http://(www\\.)?dup\\.co\\.il/(file\\.php\\?akey=|v/)[\\w]{14}" }, flags = { 0 })
public class DupCoIl extends PluginForHost {

    // they have https, but not configured/setup correctly.
    // tested with 10 sim dl (all I could find)
    // no resume + no chunks

    public DupCoIl(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.dup.co.il/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 10;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookiesExclusive(true);
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        String filename = br.getRegex("<title>([^<>\"]*?) להורדה -\\ דאפ</title>").getMatch(0);
        final String extension = br.getRegex("סוג הקובץ <br> <b>([^<>\"]*?)</b>").getMatch(0);
        if (filename == null || extension == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filesize = br.getRegex("גודל קובץ <br> <b>([^<>\"]*?)</b>").getMatch(0);
        // seems like the ; is missing to be a valid unicode html expression
        // (&#NUMBER;)
        filename = filename.replaceAll("\\&\\#(\\d+)", "&#$1;");

        filename = filename + "." + extension;
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String protcol = new Regex(br.getURL(), "(https?)://").getMatch(0);
        String uid = new Regex(br.getURL(), "([\\w]{14})$").getMatch(0);
        String dllink = protcol + "://www.dup.co.il/php-download.php?akey=" + uid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("freelink", dllink);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}