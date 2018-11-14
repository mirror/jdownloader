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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision: $", interfaceVersion = 3, names = { "thisav.com" }, urls = { "https?://(?:www\\.)?thisav\\.com/video/\\d+/" })
public class ThisAVCom extends PluginForHost {
    public ThisAVCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String  DEFAULT_EXTENSION = ".mp4";
    private static final boolean FREE_RESUME       = true;
    private static final int     FREE_MAXCHUNKS    = 0;
    private static final int     FREE_MAXDOWNLOADS = -1;
    private String               dllink            = null;

    @Override
    public String getAGBLink() {
        return "https://www.thisav.com/static/terms";
    }

    @Override
    public String getLinkID(final DownloadLink link) {
        final String linkid = new Regex(link.getPluginPatternMatcher(), "(\\d+)$").getMatch(0);
        if (linkid != null) {
            return linkid;
        } else {
            return super.getLinkID(link);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        setBrowserExclusive();
        br.setFollowRedirects(true);
        getPage(link.getPluginPatternMatcher());
        if (br.getHttpConnection().getResponseCode() == 404 || br.getURL().contains("error404")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<title>([^<>\"]*?) - ").getMatch(0);
        if (title == null) {
            title = getLinkID(link);
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        title = Encoding.htmlDecode(title.trim());
        String filename = title;
        if (!filename.endsWith(DEFAULT_EXTENSION)) {
            filename += DEFAULT_EXTENSION;
        }
        filename = encodeUnicode(filename);
        link.setFinalFileName(filename);
        String mpdUrl = br.getRegex("<source src=\"([^\"]+)\"[^>]+label=('|\")HD\\2").getMatch(0);
        if (mpdUrl == null) {
            mpdUrl = br.getRegex("<source src=\"([^\"]+)\"[^>]+label=('|\")SD\\2").getMatch(0);
        }
        if (mpdUrl == null) {
            mpdUrl = br.getRegex("<source src=\"([^\"]+)\"[^>]+dash\\+xml").getMatch(0);
            if (mpdUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        getPage(mpdUrl);
        link.setDownloadSize(getDashFileSize());
        String baseUrl = br.getRegex("<BaseURL>([^<>]+)</").getMatch(0);
        if (baseUrl == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dllink = mpdUrl.replaceFirst("/([^/]+)$", "/" + baseUrl);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, FREE_RESUME, FREE_MAXCHUNKS);
        if (dl.getConnection().getContentType().contains("html")) {
            logger.warning("The final dllink seems not to be a file!");
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private long getDashFileSize() {
        long result = -1;
        String[] mediaRange = br.getRegex("SegmentURL mediaRange=\"\\d+-(\\d+)\"").getColumn(0);
        if (mediaRange != null) {
            result = Long.parseLong(mediaRange[mediaRange.length - 1]);
        }
        return result;
    }

    private void getPage(String page) throws Exception {
        br.getPage(page);
    }

    @Override
    public String encodeUnicode(final String input) {
        if (input != null) {
            String output = input;
            output = output.replace(":", "：");
            output = output.replace("|", "｜");
            output = output.replace("<", "＜");
            output = output.replace(">", "＞");
            output = output.replace("/", "⁄");
            output = output.replace("\\", "∖");
            output = output.replace("*", "＊");
            output = output.replace("?", "？");
            output = output.replace("!", "！");
            output = output.replace("\"", "”");
            return output;
        }
        return null;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return FREE_MAXDOWNLOADS;
    }

    @Override
    public void resetDownloadlink(final DownloadLink link) {
    }
}