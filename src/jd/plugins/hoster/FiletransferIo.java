//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "filetransfer.io" }, urls = { "https?://(?:www\\.)?filetransfer\\.io/data\\-package/([A-Za-z0-9]+)" })
public class FiletransferIo extends PluginForHost {
    public FiletransferIo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://filetransfer.io/tos";
    }

    /* Connection stuff */
    private final boolean FREE_RESUME       = true;
    private final int     FREE_MAXCHUNKS    = -2;
    private final int     FREE_MAXDOWNLOADS = -1;

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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        if (!link.isNameSet()) {
            link.setName(getFID(link) + ".zip");
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (this.br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filetitle = br.getRegex("(?i)<title>([^<>\"]+) \\- FileTransfer\\.io</title>").getMatch(0);
        final String filesizeBytes = br.getRegex("data\\-bytes=\"(\\d+)\"").getMatch(0);
        final String filesizeStr = br.getRegex("(?i)Size:\\s*<span[^>]*>([^<>\"]+)</span>").getMatch(0);
        if (filetitle != null) {
            filetitle = Encoding.htmlDecode(filetitle).trim();
            if (filetitle.equalsIgnoreCase("Data package deleted")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            if (filetitle.endsWith(".") || !filetitle.contains(".")) {
                link.setName(filetitle + ".zip");
            } else {
                /* Assume file-extension is given. */
                link.setName(filetitle);
            }
        }
        if (filesizeBytes != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeBytes));
        } else if (filesizeStr != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
        }
        /*
         * 2021-10-21: For some deleted items they will not return error 404. They will still return the file information along with the
         * offline errormessage so first set file info, then check for this message.
         */
        if (br.containsHTML("(?i)>\\s*(The data package cannot be downloaded anymore, it was deleted from the server|This data package cannot be downloaded anymore because)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else {
            return AvailableStatus.TRUE;
        }
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        handleDownload(link, FREE_RESUME, FREE_MAXCHUNKS, "free_directlink");
    }

    private void handleDownload(final DownloadLink link, final boolean resumable, final int maxchunks, final String directlinkproperty) throws Exception, PluginException {
        requestFileInformation(link);
        final String dllink = "https://" + br.getHost() + "/data-package/" + this.getFID(link) + "/download";
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, resumable, maxchunks);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            if (br.getURL().contains("/premium") || br.getURL().contains("/pricelist")) {
                throw new AccountRequiredException();
            } else if (dl.getConnection().getResponseCode() == 403) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 403", 60 * 60 * 1000l);
            } else if (dl.getConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error 404", 60 * 60 * 1000l);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}