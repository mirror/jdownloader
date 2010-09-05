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

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "indowebster.com" }, urls = { "http://[\\w\\.]*?indowebster\\.com/[^\\s]+\\.html" }, flags = { 0 })
public class Indowebster extends PluginForHost {

    public Indowebster(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.indowebster.com/policy-tos.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Requested file is deleted")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Original name :</b><!--INFOLINKS_ON-->(.*?)<").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<b>Original name:</b>(.*?)</div>").getMatch(0);
            if (filename == null) filename = br.getRegex("<b>Original name: </b><\\!--INFOLINKS_ON-->(.*?)<\\!--INFOLINKS_OFF-->").getMatch(0);
        }
        String filesize = br.getRegex("<b>Size:([ ]+)?</b>(.*?)</div>").getMatch(1);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        downloadLink.setFinalFileName(filename.trim());
        if (filesize != null) downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        String dl_url = br.getRegex("\\&file=(http.*?)\\&logo").getMatch(0);
        if (dl_url == null) {
            String adUrl = br.getRegex("style=\"float:right;\"><a href=\"(.*?)\"").getMatch(0);
            if (adUrl != null) {
                br.getPage("http://www.indowebster.com/" + adUrl);
                Form dlForm = br.getFormbyProperty("name", "form1");
                if (dlForm != null) {
                    logger.info("Sending dlform now...");
                    br.submitForm(dlForm);
                }
            }
        }
        if (dl_url == null) {
            logger.warning("Final link is null!");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        logger.info("Final link = " + dl_url);
        String filename = link.getFinalFileName();
        String ext = filename.substring(filename.lastIndexOf('.'));
        ext = ext.concat(".flv");
        if (dl_url.endsWith(ext)) {
            dl_url = dl_url.replaceFirst(".flv?", "");
        }
        br.setDebug(true);
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dl_url, true, 1);
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
