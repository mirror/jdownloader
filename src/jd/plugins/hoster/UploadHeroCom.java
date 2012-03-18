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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

/**
 * They have a linkchecker but it's only available for registered users (well
 * maybe it also works so) but it doesn't show the filenames of the links:
 * http://uploadhero.com/api/linktester.php postvalues: "linktest=" + links to
 * check
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadhero.com" }, urls = { "http://(www\\.)?uploadhero\\.com/(dl|v)/[A-Za-z0-9]+" }, flags = { 0 })
public class UploadHeroCom extends PluginForHost {

    public UploadHeroCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://uploadhero.com/tos";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("http://(www\\.)?uploadhero\\.com/(dl|v)/", "http://uploadhero.com/dl/"));
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie("http://uploadhero.com/", "lang", "en");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(>The following download is not available on our server|>The file link is invalid|>The uploader has deleted the file|>The file was illegal and was deleted|<title>UploadHero \\- File Sharing made easy\\!</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<div class=\"nom_de_fichier\">([^<>\"/]+)</div>").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>(.*?) \\- UploadHero</title>").getMatch(0);
        String filesize = br.getRegex("<span class=\"noir\">Filesize: </span><strong>([^<>\"\\'/]+)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        final Regex blockRegex = br.getRegex("/lightbox_block_download\\.php\\?min=(\\d+)\\&sec=(\\d+)\"");
        final String blockmin = blockRegex.getMatch(0);
        final String blocksec = blockRegex.getMatch(0);
        if (blockmin != null && blocksec != null) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, ((Integer.parseInt(blockmin) + 5) * 60 + Integer.parseInt(blocksec)) * 1001l);
        final String captchaLink = br.getRegex("\"(/captchadl\\.php\\?[a-z0-9]+)\"").getMatch(0);
        if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String code = getCaptchaCode("http://uploadhero.com" + captchaLink, downloadLink);
        br.getPage("http://www.uploadhero.com/dl/dCYhooWE?code=" + code);
        if (!br.containsHTML("\"dddl\"")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("var magicomfg = \\'<a href=\"(http://[^<>\"]*?)\"").getMatch(0);
        if (dllink == null) dllink = br.getRegex("\"(http://storage\\d+\\.uploadhero\\.com/\\?d=[A-Za-z0-9]+/[^<>\"/]+)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, Encoding.htmlDecode(dllink), false, 1);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}