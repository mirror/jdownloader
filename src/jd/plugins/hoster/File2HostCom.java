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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

//IMPORTANT: The name of the plugin is CORRECT!
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "f2h.io" }, urls = { "https?://(?:www\\.)?(?:f2h(?:\\.nana\\d+)?\\.co\\.il|f2h\\.io)/((he/)?[a-z0-9]+|[0-9]+)" })
public class File2HostCom extends PluginForHost {
    public File2HostCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "https://f2h.io/";
    }

    @Override
    public void correctDownloadLink(DownloadLink link) throws Exception {
        // enforce https
        link.setPluginPatternMatcher(link.getPluginPatternMatcher().replaceFirst("https?://", "https://").replaceFirst(org.appwork.utils.Regex.escape(Browser.getHost(link.getPluginPatternMatcher(), true)), "f2h.io"));
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
    public String rewriteHost(final String host) {
        if (host == null || host.equalsIgnoreCase("file2host.com")) {
            return this.getHost();
        } else {
            return super.rewriteHost(host);
        }
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws Exception {
        correctDownloadLink(link);
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(link.getPluginPatternMatcher());
        if (br.containsHTML("HTTP-EQUIV=\"Refresh\"")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("class=\"fa fa-download\"")) {
            /* Invalid URL e.g.https://f2h.io/en */
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final Regex info = br.getRegex("<div itemprop=\"name\">\\s*([^<>\"]*?)\\s*</div>\\s*<font dir=\"ltr\"><br>\\s*\\(([^<>\"]*?)\\)\\s*</font>");
        String filename = info.getMatch(0);
        if (filename == null) {
            filename = br.getRegex("itemprop=\"name\">([^<>\"]+)<").getMatch(0);
        }
        String filesize = info.getMatch(1);
        if (filesize == null) {
            filesize = br.getRegex("itemprop=\"contentSize\" content=\"([^<>\"]+)\"").getMatch(0);
        }
        if (filesize == null) {
            /* 2021-10-14 */
            filesize = br.getRegex("</strong></h4>\\s*<span>\\( ([^<>\"\\']+) \\)</span>").getMatch(0);
        }
        if (filename != null) {
            /* Server sometimes sends encoded crap - use html-filename as final filename! */
            link.setFinalFileName(Encoding.htmlDecode(filename.trim()));
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        // now have form
        Form thanks = br.getFormbyActionRegex(".+/thanks/.+");
        if (thanks == null) {
            thanks = br.getForm(0);
        }
        if (thanks == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        br.submitForm(thanks);
        final String dllink = br.getRegex("('|\")((?:(?:https?:)?//[^/]+)?/files/[a-z0-9]+\\|[^<>\"\\']+)\\1").getMatch(1);
        if (dllink == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = new jd.plugins.BrowserAdapter().openDownload(br, link, dllink, true, 0);
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
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}