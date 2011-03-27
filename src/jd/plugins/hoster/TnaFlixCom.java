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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "tnaflix.com" }, urls = { "http://[\\w\\.]*?tnaflix\\.com/(view_video\\.php\\?viewkey=[a-z0-9]+|.*?video\\d+)" }, flags = { 2 })
public class TnaFlixCom extends PluginForHost {

    public TnaFlixCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.tnaflix.com/terms.php";
    }

    private static final String FORMAT   = "WAIT1";
    private static final String MP4REGEX = "\"(http://cdn[\\w\\.\\-]*?tnaflix\\.com/tnamp4/[a-z0-9]+/.*?\\.mp4\\?key=[a-z0-9]+)\"";
    private static final String FLVREGEX = "\"(http://cdn[\\w\\.\\-]*?tnaflix\\.com/tnadl/[a-z0-9]+/.*?\\.flv\\?key=[a-z0-9]+)\"";

    private void setConfigElements() {
        ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), FORMAT, JDL.L("plugins.hoster.tnaflixcom.selectformat", "Prefer mp4 videos (download mp4 if available)")).setDefaultValue(false);
        config.addEntry(cond);
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(downloadLink.getDownloadURL());
        if (br.getRedirectLocation() != null) {
            if (br.getRedirectLocation().contains("errormsg=true")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            if (downloadLink.getDownloadURL().contains("viewkey=")) {
                downloadLink.setUrlDownload(br.getRedirectLocation());
                br.getPage(downloadLink.getDownloadURL());
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String filename = br.getRegex("<title>(.*?), Free Porn.*?</title>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("playIcon\">(.*?)</h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("videoDescription\">(.*?)</span>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("description\" content=\"(.*?)Watch Free").getMatch(0);
                }
            }
        }
        if (filename == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filename = filename.trim();
        String format = "flv";
        if (getPluginConfig().getBooleanProperty(FORMAT, false)) format = "mp4";
        if (!filename.endsWith(".")) {
            downloadLink.setName(filename + "." + format);
        } else {
            downloadLink.setName(filename + format);
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        boolean preferMp4 = getPluginConfig().getBooleanProperty(FORMAT, false);
        preferMp4 = true;
        String dllink = null;
        if (preferMp4) {
            logger.info("Prefer mp4 is enabled");
            dllink = br.getRegex(MP4REGEX).getMatch(0);
        } else {
            logger.info("Prefer mp4 is not enabled");
            dllink = br.getRegex(FLVREGEX).getMatch(0);
        }
        if (dllink == null) {
            logger.info("Selected format could not be found, tryng to find a downloadlink for the other format.");
            if (preferMp4) {
                dllink = br.getRegex(FLVREGEX).getMatch(0);
            } else {
                dllink = br.getRegex(MP4REGEX).getMatch(0);
            }
            if (dllink == null) {
                logger.warning("dllink could not be found");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        dllink = dllink.replace("amp;", "");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        String ending = ".flv";
        if (dllink.contains("/tnamp4/")) ending = ".mp4";
        downloadLink.setFinalFileName(downloadLink.getName().replaceAll("(\\.flv|\\.mp4)", ending));
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 18;
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
