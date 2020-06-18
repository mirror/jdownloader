//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.jdownloader.controlling.filter.CompiledFiletypeFilter;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "opensubtitles.org" }, urls = { "https?://(?:www\\.)?opensubtitles\\.org/[a-z]{2}/subtitles/(\\d+)" })
public class OpenSubtitlesOrg extends PluginForHost {
    public OpenSubtitlesOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://2pu.net/v/opensubtitles";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload("https://www.opensubtitles.org/en/subtitles/" + getFID(link));
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
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        link.setMimeHint(CompiledFiletypeFilter.ArchiveExtensions.ZIP);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(this.getHost(), "weblang", "en");
        br.getPage(link.getPluginPatternMatcher());
        if (!br.getURL().contains(this.getFID(link))) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("/en/download/sub/\\d+\"><span itemprop=\"name\">([^<>\"]*?)</span>").getMatch(0);
        if (filename != null) {
            filename = this.getFID(link) + "_" + Encoding.htmlDecode(filename.trim()).replace("\"", "'");
        } else if (filename == null) {
            filename = this.getFID(link);
        }
        filename += ".zip";
        /* 2020-06-18: Do not set final filename here! Use content-disposition final-filename! */
        link.setName(filename);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        // Resume and chunks disabled, not needed for such small files & can't
        // test
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, "https://dl.opensubtitles.org/en/download/sub/" + new Regex(link.getDownloadURL(), "(\\d+)$").getMatch(0), false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
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
    public void resetDownloadlink(DownloadLink link) {
    }
}