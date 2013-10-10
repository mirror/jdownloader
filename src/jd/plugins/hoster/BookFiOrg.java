//    jDownloader - Downloadmanager
//    Copyright (C) 2013  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bookfi.org, bookos.org" }, urls = { "http://(www\\.)?([a-z]+\\.)?bookfi\\.org/((book|dl)/\\d+(/[a-z0-9]+)?|md5/[A-F0-9]{32})", "http://(www\\.)?([a-z]+\\.)?bookos\\.org/((book|dl)/\\d+/[a-z0-9]+|md5/[A-F0-9]{32})" }, flags = { 0, 0 })
public class BookFiOrg extends PluginForHost {

    // DEV NOTES
    // they share the same template
    // hosted on different IP ranges

    public BookFiOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://" + this.getHost() + "/";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    private String DLLINK = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink param) throws Exception {
        final String parameter = param.getDownloadURL();
        this.setBrowserExclusive();
        br.setCustomCharset("utf-8");
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (parameter.contains("/md5/")) {
            // bookfi
            String bookid = br.getRegex("<a href=\"(book/\\d+)\" ><h3").getMatch(0);
            if (bookid == null) {
                // bookos
                bookid = br.getRegex("<a href=\"(book/\\d+/[a-z0-9]+)\"><h3").getMatch(0);
                if (bookid == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            }
            br.getPage("/" + bookid);
        }

        // bookfi
        String[] info = br.getRegex("<a class=\"button active\" href=\"([^\"]+)\">.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
        if (info == null) {
            // bookos
            info = br.getRegex("<a class=\"button active dnthandler\" href=\"([^\"]+)\">.*?\\([^,]+, ([^\\)]+?)\\)</a>").getRow(0);
            if (info == null || info[0] == null || info[1] == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        }
        // Goes to download link to find out filename
        String filename = br.getRegex("<h2 style=\"display:inline\">([^<>\"]*?)</h2>").getMatch(0);
        if (filename == null) filename = br.getRegex("<h1 style=\"color:#49AFD0\"  itemprop=\"name\">([^<>\"]*?)</h1>").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        param.setFinalFileName(filename + ".pdf");
        param.setDownloadSize(SizeFormatter.getSize(info[1]));
        DLLINK = info[0];
        if (parameter.contains("/md5/")) {
            // now everything is aok, we should correct to a single url/file uid
            param.setUrlDownload(br.getURL());
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, DLLINK, true, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        fixFilename(link);
        dl.startDownload();
    }

    private void fixFilename(final DownloadLink downloadLink) {
        final String serverFilename = Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection()));
        final String newExtension = serverFilename.substring(serverFilename.lastIndexOf("."));
        if (newExtension != null && !downloadLink.getFinalFileName().endsWith(newExtension)) {
            final String oldExtension = downloadLink.getFinalFileName().substring(downloadLink.getFinalFileName().lastIndexOf("."));
            if (oldExtension != null)
                downloadLink.setFinalFileName(downloadLink.getFinalFileName().replace(oldExtension, newExtension));
            else
                downloadLink.setFinalFileName(downloadLink.getFinalFileName() + newExtension);
        }
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    public boolean hasAutoCaptcha() {
        return false;
    }

    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        if (acc == null) {
            /* no account, yes we can expect captcha */
            return false;
        }
        if (Boolean.TRUE.equals(acc.getBooleanProperty("free"))) {
            /* free accounts also have captchas */
            return false;
        }
        return false;
    }

}
