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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "wikifortio.com" }, urls = { "https?://(?:www\\.)?wikifortio\\.com/\\d+/" })
public class WikiFortioCom extends PluginForHost {
    public WikiFortioCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.wikifortio.com/contact/";
    }

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setAllowedResponseCodes(410);
        br.getPage(link.getPluginPatternMatcher());
        final int responsecode = this.br.getHttpConnection().getResponseCode();
        if (responsecode == 404 || responsecode == 410 || br.containsHTML("(?i)(doesn\\'t exist or has expired and is no longer available<|>We are sorry but file \\')")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String filename = br.getRegex("class=\"filename\">File name: <strong>([^<>\"]*?)</strong>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<input type=\"hidden\" name=\"fileName\" value=\"([^<>\"]*?)\"/>").getMatch(0);
        }
        String filesize = br.getRegex("<td>Size:</td>[\t\n\r ]+<td>([^<>\"]*?)\\&nbsp;</td>").getMatch(0);
        if (filename != null) {
            link.setName(Encoding.htmlDecode(filename).trim());
        }
        if (filesize != null) {
            link.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(final DownloadLink link) throws Exception, PluginException {
        requestFileInformation(link);
        if (br.containsHTML("(?i)>Enter password")) {
            throw new PluginException(LinkStatus.ERROR_FATAL, "Password protected files are not yet supported");
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, link.getDownloadURL(), "act=download&fid=" + new Regex(link.getDownloadURL(), "(\\d+)/$").getMatch(0) + "&fileName=" + Encoding.urlEncode(link.getName()), true, -2);
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