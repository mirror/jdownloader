//jDownloader - Downloadmanager
//Copyright (C) 2015  JD-Team support@jdownloader.org
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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "someimage.com" }, urls = { "https?://someimage\\.com/([^<>\"]+)" }, flags = { 0 })
public class SomeImageCom extends PluginForHost {

    public SomeImageCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://someimage.com/tos";
    }

    private int MAXCHUNKSFORFREE = 1;

    // private String MAINPAGE = "http://someimage.com";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        final String dwnlUrl = downloadLink.getPluginPatternMatcher();

        br.getPage(dwnlUrl);
        if (br.containsHTML("div class='docheader'>404 Image Not Found</div>")) {
            return AvailableStatus.FALSE;
        }

        final String imageId = new Regex(dwnlUrl, "https?://someimage\\.com/([^<>\"]+)").getMatch(0);
        if (imageId == null) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
            return AvailableStatus.UNCHECKABLE;
        }

        String imageInfo[][] = new Regex(br, "<img src='https?://[^<>\"]+\\.someimage\\.com/" + imageId + "\\.([A-z0-9]+)' id='viewimage' align='center' title='([^<>\"]+)' style='cursor:").getMatches();
        if (imageInfo.length == 0) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
            return AvailableStatus.UNCHECKABLE;
        }

        String fileName = imageInfo[0][1];
        String extension = imageInfo[0][0];

        if (fileName == null) {
            fileName = imageId + "." + extension;
        }
        downloadLink.setName(Encoding.htmlDecode(fileName.trim()));

        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        doFree(downloadLink);
    }

    public void doFree(final DownloadLink downloadLink) throws Exception, PluginException {
        final String dwnlUrl = downloadLink.getPluginPatternMatcher();

        final String imageId = new Regex(dwnlUrl, "https?://someimage\\.com/([^<>\"]+)").getMatch(0);

        final String dllink = new Regex(br, "<img src='(https?://[^<>\"]+\\.someimage\\.com/" + imageId + "\\.[A-z0-9]+)' id='viewimage' align='center' title='([^<>\"]+)' style='cursor:").getMatch(0);

        if (imageId == null || dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Can't find final download link!", -1l);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, MAXCHUNKSFORFREE);
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
    public void resetDownloadlink(final DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return false;
    }

}