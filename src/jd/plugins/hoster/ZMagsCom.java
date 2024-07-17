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
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zmags.com" }, urls = { "https?://(?:www\\.)?viewer\\.zmags\\.com/publication/([a-z0-9]+)" })
public class ZMagsCom extends PluginForHost {
    public ZMagsCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.zmags.com/about/contact";
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

    private static final String PROPERTY_TITLE = "title";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        final String extDefault = ".pdf";
        if (!link.isNameSet()) {
            link.setName(this.getFID(link) + extDefault);
        }
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        URLConnectionAdapter con = null;
        try {
            con = br.openGetConnection(link.getPluginPatternMatcher());
            if (con.getResponseCode() == 400) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            br.followConnection();
        } finally {
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
        if (br.getHttpConnection().getResponseCode() == 403) {
            final String baseErrortext = "Server error 403";
            final String betterErrormessage = br.getRegex("<h1>([^<]+)</h1>").getMatch(0);
            if (betterErrormessage != null) {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, baseErrortext + ": " + Encoding.htmlDecode(betterErrormessage).trim());
            } else {
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, baseErrortext);
            }
        } else if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(>\\s*Publication not found\\s*<|>\\s*The publication you are trying to view does not exist or may have been deleted|Please check the URL and re\\-enter it in the address line of your browser)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String title = br.getRegex("<meta property=\"og:title\" content=\"(.*?)\"/>").getMatch(0);
        if (title == null) {
            title = br.getRegex("<meta name=\"title\" content=\"(.*?)\" />").getMatch(0);
            if (title == null) {
                title = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
        }
        if (title == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        /** Server often sends us wrong/same filenames */
        if (title != null) {
            title = Encoding.htmlDecode(title.trim());
            link.setProperty(PROPERTY_TITLE, title);
            /* 2024-07-17: Set final filename here because website will respond with filename "pages.pdf" via Content-Disposition header. */
            link.setFinalFileName(title + extDefault);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, "http://viewer.zmags.com/services/DownloadPDF", "publicationID=" + new Regex(link.getDownloadURL(), "zmags\\.com/publication/(.+)").getMatch(0) + "&selectedPages=all", false, 1);
        if (!this.looksLikeDownloadableContent(dl.getConnection())) {
            br.followConnection(true);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}