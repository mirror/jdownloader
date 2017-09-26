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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "d-h.st" }, urls = { "https?://(www\\.)?d-h\\.st/[A-Za-z0-9]+" })
public class DhSt extends PluginForHost {

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        // doesn't support https at this time. redirects back to http.
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replace("https://", "http://"));
    }

    public DhSt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://d-h.st/tos";
    }

    private final String        INVALIDLINKS  = "https?://(www\\.)?d-h\\.st/(donate|search|forgot|support|faq|news|register|tos|users)";
    private final String        tempSiteIssue = ">It appears something went wrong\\.{1,}</h\\d+>";
    private static final String NOCHUNKS      = "NOCHUNKS";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        if (link.getPluginPatternMatcher().matches(INVALIDLINKS)) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("(>File Not Found<|>The file you were looking for could not be found)") || br.getURL().equals("http://d-h.st/")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        // site issue?
        if (br.containsHTML(tempSiteIssue)) {
            return AvailableStatus.UNCHECKABLE;
        }
        // For invalid links
        if (br.containsHTML(">403 Forbidden<")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex(">Filename:</span> <div title=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<title>Dev-Host - ([^<>\"]*?) - The Ultimate Free File Hosting / File Sharing Service</title>").getMatch(0);
        }
        String filesize = br.getRegex("\\((\\d+(?:\\.\\d{1,2})? (?:bytes|MB))\\)").getMatch(0);
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        final String md5 = br.getRegex(">MD5 Sum</span>: \\&nbsp;([a-z0-9]{32})<").getMatch(0);
        if (md5 != null) {
            link.setMD5Hash(md5);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML(tempSiteIssue)) {
            logger.info("Hoster is having problems!");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Hoster is having problems!");
        }
        String dllink = getDllink();
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int maxChunks = 0;
        if (downloadLink.getBooleanProperty(NOCHUNKS, false)) {
            maxChunks = 1;
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, downloadLink, dllink, true, maxChunks);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        try {
            if (!this.dl.startDownload()) {
                try {
                    if (dl.externalDownloadStop()) {
                        return;
                    }
                } catch (final Throwable e) {
                }
                /* unknown error, we disable multiple chunks */
                if (downloadLink.getBooleanProperty(DhSt.NOCHUNKS, false) == false) {
                    downloadLink.setProperty(DhSt.NOCHUNKS, Boolean.valueOf(true));
                    throw new PluginException(LinkStatus.ERROR_RETRY);
                }
            }
        } catch (final PluginException e) {
            // New V2 errorhandling
            /* unknown error, we disable multiple chunks */
            if (e.getLinkStatus() != LinkStatus.ERROR_RETRY && downloadLink.getBooleanProperty(DhSt.NOCHUNKS, false) == false) {
                downloadLink.setProperty(DhSt.NOCHUNKS, Boolean.valueOf(true));
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            throw e;
        }
    }

    private String getDllink() {
        String dllink = br.getRegex("<form style=\"margin-top: -27px; margin-bottom: -2px;\" action=\"(https?://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("\"(https?://[a-z0-9]+\\.d-h\\.st/[A-Za-z0-9]+/\\d+/[^<>\"/]+\\?key=\\d+)\"").getMatch(0);
        }
        if (dllink == null) {
            dllink = br.getRegex("(\"|')(https?://[a-z0-9]+\\.d-h\\.st/[A-Za-z0-9]+/\\d+/[^<>\"/]+/[^<>\"/]*?)\\1").getMatch(1);
        }
        return dllink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}