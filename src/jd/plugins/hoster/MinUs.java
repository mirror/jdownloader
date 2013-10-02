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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

/** Links always come rom a decrypter */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "min.us", "minus.com" }, urls = { "cvj84ezu45gj0wojgHZiF238ß3üpj5uUNUSED_REGEX", "http://([a-zA-Z0-9]+\\.)?minusdecrypted\\.com/[A-Za-z0-9\\-_]+" }, flags = { 0, 0 })
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (link.getBooleanProperty("offline", false)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        // Decrypter marks it as offline
        if (br.containsHTML("(<h2>Not found\\.</h2>|<p>Our records indicate that the gallery/image you are referencing has been deleted or does not exist|The page you requested does not exist)") || br.containsHTML("\"items\": \\[\\]") || br.containsHTML("class=\"guesthomepage_cisi_h1\">Upload and share your files instantly") || br.containsHTML(">The folder you requested has been deleted or has expired") || br.containsHTML(">You're invited to join Minus")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("'name': '([^<>\"]*?)'").getMatch(0);
        if (filename == null) filename = br.getRegex("<meta name=\"title\" content=\"([^<>\"]*?) - Minus\"").getMatch(0);
        final String filesize = br.getRegex("<div class=\"item-actions-right\">[\t\n\r ]+<a title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        // useful for generic regex failovers, to filter false postitives.
        String uid = new Regex(downloadLink.getDownloadURL(), "\\.com/[a-z]([A-Za-z0-9\\-_]+)").getMatch(0);

        // Sometimes servers are pretty slow
        br.setReadTimeout(3 * 60);

        // generic for all download types, based on button!
        String finallink = br.getRegex("class=\"btn-action[^>]+no-counter[^>]+href=\"(https?://[^\"]+\\.minus\\.com/[^\"]+)").getMatch(0);
        if (finallink == null) {
            // secondary for images
            finallink = br.getRegex("class=\"item-main is-image\"[^>]+href=\"(https?://[^\"]+\\.minus\\.com/[^\"]+)").getMatch(0);
            if (finallink == null) {
                // generic fail over for images! (largest)
                finallink = br.getRegex("(https?://i\\d+\\.minus\\.com/i" + uid + "[^<>\"]+)").getMatch(0);
                if (finallink == null) {
                    // standard downloads fail over.
                    finallink = br.getRegex("(https?://i\\.minus\\.com/\\d+/[^<>\"]+)").getMatch(0);
                    if (finallink == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
                }
            }
        }
        // Resume/Chunks depends on link and/or fileserver so to prevent errors we deactivate it
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            /* linkrefresh is needed here */
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        if (downloadLink.getFinalFileName() == null) downloadLink.setFinalFileName(Encoding.htmlDecode(getFileNameFromHeader(dl.getConnection())));
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}