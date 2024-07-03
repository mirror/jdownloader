//jDownloader - Downloadmanager
//Copyright (C) 2017  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
/** Helper plugin to download image hotlinks from photobucket.com without serverside "watermark protection". */
public class PhotobucketComDirectImages extends PluginForHost {
    public PhotobucketComDirectImages(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForHost, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "directimages.photobucket.com" });
        return ret;
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : getPluginDomains()) {
            ret.add("https?://(?:i\\d+|hosting)\\.photobucket\\.com/albums/\\w+/\\w+/[^/]+\\.(jpg|png|gif|webp)");
        }
        return ret.toArray(new String[0]);
    }

    @Override
    public String getAGBLink() {
        return "https://support.photobucket.com/hc/en-us/requests/new";
    }

    @Override
    public boolean isResumeable(final DownloadLink link, final Account account) {
        return false;
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        return requestFileInformation(link, false);
    }

    private AvailableStatus requestFileInformation(final DownloadLink link, final boolean isDownload) throws Exception {
        if (!link.isNameSet()) {
            final String filenameFromURL = Plugin.getFileNameFromURL(new URL(link.getPluginPatternMatcher()));
            if (filenameFromURL != null) {
                link.setName(filenameFromURL);
            }
        }
        this.setBrowserExclusive();
        prepareDownloadHeaders(br, link);
        basicLinkCheck(br.cloneBrowser(), br.createGetRequest(link.getPluginPatternMatcher()), link, link.getName(), null);
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception {
        requestFileInformation(link, true);
        prepareDownloadHeaders(br, link);
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, link.getPluginPatternMatcher(), this.isResumeable(link, null), 1);
        handleConnectionErrors(br, dl.getConnection());
        dl.startDownload();
    }

    private void prepareDownloadHeaders(final Browser br, final DownloadLink link) {
        /* Prevents serverside "Watermark protection" RE https://board.jdownloader.org/showthread.php?t=95053 */
        br.getHeaders().put("Referer", link.getPluginPatternMatcher());
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}