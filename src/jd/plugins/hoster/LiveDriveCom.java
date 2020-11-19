//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

//This plugin only takes decrypted links from the livedrive decrypter
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "livedrive.com" }, urls = { "https?://[a-z0-9]+\\.livedrivedecrypted\\.com/item/([a-f0-9]{32})" })
public class LiveDriveCom extends PluginForHost {
    public LiveDriveCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replace("livedrivedecrypted.com/", "livedrive.com/"));
    }

    @Override
    public String getAGBLink() {
        return "http://www.livedrive.com/terms-of-use";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
        return new Regex(link.getPluginPatternMatcher(), ".*/item/(.+)").getMatch(0);
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        /* 2017-05-04: https is forced now */
        br.getPage(link.getPluginPatternMatcher().replace("http://", "https://"));
        final String redirect = br.getRegex("window\\.top\\.location\\.href = '(https?://[^<>\"\\']+)';").getMatch(0);
        if (redirect != null) {
            if (!redirect.contains(getFID(link))) {
                /* E.g. redirect to mainpage */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.getPage(redirect);
        }
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("class=\"nofilesfound\"")) {
            /* 2020-11-19: E.g. <div class="nofilesfound"><h2>Shared file not available</h2></div> */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.getURL().contains(getFID(link))) {
            /* E.g. redirect to mainpage */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("<div id=\"Preview\">[\t\n\r ]+<img src=\"/Content/Images/filetypes/180x230/[^<>\"/]*?\" alt=\"([^<>\"]*?)\"").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("id=\"PageTitle\" class=\"Hidden\">([^<>\"]*?)</div>").getMatch(0);
        }
        if (filename == null) {
            /* 2017-05-04 */
            filename = br.getRegex("class=\"file\\-details\">\\s*?<h2>([^<>\"]+)</h2>").getMatch(0);
        }
        if (filename == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        link.setName(Encoding.htmlDecode(filename.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        final String nodeID = new Regex(link.getDownloadURL(), "([a-z0-9]{32})$").getMatch(0);
        String liveDriveUrlUserPart = new Regex(link.getDownloadURL(), "(.*?)\\.livedrive\\.com").getMatch(0);
        liveDriveUrlUserPart = liveDriveUrlUserPart.replaceAll("(https?://|www\\.)", "");
        final String aid = br.getRegex("DownloadSharedFile\\(\\'" + nodeID + "\\',\\'(\\d+)\\'\\)").getMatch(0);
        if (aid == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String dllink = "https://" + liveDriveUrlUserPart + ".livedrive.com/IO/DownloadSharedFile?NodeID=" + nodeID + "&AID=" + aid;
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            try {
                br.followConnection(true);
            } catch (final IOException e) {
                logger.log(e);
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}