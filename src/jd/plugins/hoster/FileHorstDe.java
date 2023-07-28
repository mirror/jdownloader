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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filehorst.de" }, urls = { "https?://(?:www\\.)?filehorst\\.de/(?:d/|download\\.php\\?file=)([A-Za-z0-9]+)" })
public class FileHorstDe extends PluginForHost {
    public FileHorstDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://filehorst.de/agb.php";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String fid = getFID(link);
        if (fid != null) {
            return this.getHost() + "://" + fid;
        } else {
            return super.getLinkID(link);
        }
    }

    private String getFID(final DownloadLink link) {
        return new Regex(link.getPluginPatternMatcher(), this.getSupportedLinks()).getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        final String fid = this.getFID(link);
        if (!link.isNameSet()) {
            link.setName(fid);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* 2016-08-31: New linkformat - directly access new url to avoid redirect --> It's a bit faster */
        br.getPage(Request.getLocation("/download.php?file=" + fid, br.createGetRequest(link.getPluginPatternMatcher())));
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML("(?i)Fehler:\\s*Die angegebene Datei wurde nicht gefunden")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<td>\\s*Dateiname:\\s*</td><td>([^<]*?)<").getMatch(0);
        String filesize = br.getRegex("<td>\\s*Dateigröße:\\s*</td><td>([^<>\"]*?)<").getMatch(0);
        final String md5 = br.getRegex("<td>\\s*MD5:\\s*</td><td>([^<]*?)<").getMatch(0);
        /* Server sometimes sends crippled/encoded filenames */
        if (filename != null) {
            link.setFinalFileName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        String dllink = checkDirectLink(link, "directlink");
        if (dllink == null) {
            final Regex wait_id = br.getRegex(">\\s*downloadWait\\((\\d+), \"([^\"]+)");
            final String waittime = wait_id.getMatch(0);
            final String id = wait_id.getMatch(1);
            if (waittime == null || id == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            this.sleep(Integer.parseInt(waittime) * 1001l, link);
            br.getPage("/downloadQueue.php?file=" + this.getFID(link) + "&fhuid=" + id);
            if (br.containsHTML("(?i)>\\s*Bitte versuche es nochmal")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            }
            dllink = br.getRegex("\"(downloadNow[^<>\"]*?)\"").getMatch(0);
            if (dllink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            dllink = dllink.replace("&amp;", "&");
        } else {
            /* Wait some time before we can access link again */
            this.sleep(5 * 1000l, link);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 1);
        /* Should never happen */
        // filenames can be .html so using this if statement will be a automatic false positive
        // re: https://svn.jdownloader.org/issues/82929
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (dl.getConnection().getResponseCode() == 503) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Wait before starting new downloads", 3 * 60 * 1000l);
            } else if (br.containsHTML("Dein Download konnte nicht gefunden werden")) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Unknown server error", 5 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        link.setProperty("directlink", dllink);
        dl.startDownload();
    }

    private String checkDirectLink(final DownloadLink link, final String property) {
        String dllink = link.getStringProperty(property);
        if (dllink != null) {
            URLConnectionAdapter con = null;
            try {
                final Browser br2 = br.cloneBrowser();
                br2.setFollowRedirects(true);
                con = br2.openHeadConnection(dllink);
                if (this.looksLikeDownloadableContent(con)) {
                    if (con.getCompleteContentLength() > 0) {
                        link.setVerifiedFileSize(con.getCompleteContentLength());
                    }
                    return dllink;
                } else {
                    throw new IOException();
                }
            } catch (final Exception e) {
                link.removeProperty(property);
                logger.log(e);
                return null;
            } finally {
                if (con != null) {
                    con.disconnect();
                }
            }
        }
        return null;
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