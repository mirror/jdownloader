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

import org.appwork.utils.formatter.SizeFormatter;
import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.config.Property;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filehorst.de" }, urls = { "https?://(?:www\\.)?filehorst\\.de/(?:d/|download\\.php\\?file=)[A-Za-z0-9]+" })
public class FileHorstDe extends PluginForHost {

    public FileHorstDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filehorst.de/agb.php";
    }

    private String fid = null;

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        fid = new Regex(link.getDownloadURL(), "([A-Za-z0-9]+)$").getMatch(0);
        /* 2016-08-31: New linkformat - directly access new url to avoid redirect --> It's a bit faster */
        br.getPage(Request.getLocation("/download.php?file=" + fid, br.createGetRequest(link.getPluginPatternMatcher())));
        if (br.containsHTML("Fehler: Die angegebene Datei wurde nicht gefunden") || this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<td>Dateiname:</td><td>([^<>\"]*?)</td></tr>").getMatch(0);
        String filesize = br.getRegex("<td>Dateigröße:</td><td>([^<>\"]*?)</td></tr>").getMatch(0);
        final String md5 = br.getRegex("<td>MD5:</td><td>([^<>\"]*?)</td></tr>").getMatch(0);
        if (filename == null || filesize == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /* Server sometimes sends crippled/encoded filenames */
        link.setFinalFileName(encodeUnicode(Encoding.htmlDecode(filename.trim())));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = checkDirectLink(downloadLink, "directlink");
        if (dllink == null) {
            final Regex wait_id = br.getRegex(">downloadWait\\((\\d+), \"([^<>\"]*?)\"\\)<");
            final String waittime = wait_id.getMatch(0);
            final String id = wait_id.getMatch(1);
            if (waittime == null || id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.sleep(Integer.parseInt(waittime) * 1001l, downloadLink);
            br.getPage("/downloadQueue.php?file=" + fid + "&fhuid=" + id);
            if (br.containsHTML(">Bitte versuche es nochmal")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            dllink = br.getRegex("\"(downloadNow[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("&amp;", "&");
        } else {
            /* Wait some time before we can access link again */
            this.sleep(5 * 1000l, downloadLink);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, 1);
        /* Should never happen */
        if (dl.getConnection().getResponseCode() == 503) {
            throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
        }
        // filenames can be .html so using this if statement will be a automatic false positive
        // re: https://svn.jdownloader.org/issues/82929
        if (!new Regex(downloadLink.getName(), CompiledFiletypeFilter.DocumentExtensions.HTML.getPattern() + "$").matches() && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Dein Download konnte nicht gefunden werden")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink downloadLink, final String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                final Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (final Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }

}