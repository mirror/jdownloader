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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "u-file.net" }, urls = { "http://[\\w\\.]*?u-file\\.net/f-[a-z0-9]+" }, flags = { 0 })
public class UFileNet extends PluginForHost {

    public UFileNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.u-file.net/contactus.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        String passCode = null;
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("FILE NOT FOUND")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("Server is now very busy")) {
            logger.info("Server sais it is very busy!");
            return AvailableStatus.UNCHECKABLE;
        }
        if (br.containsHTML("(This file was protected by password|Please enter the password below)")) {
            logger.info("The file seems to be password protected...");
            for (int i = 0; i <= 3; i++) {
                Form pwform = br.getForm(0);
                if (pwform == null) {
                    logger.warning("Pwform is null, link = " + link.getDownloadURL());
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                if (link.getStringProperty("pass", null) == null) {
                    passCode = Plugin.getUserInput("Password?", link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                pwform.put("cpasske", passCode);
                logger.info("Put password \"" + passCode + "\" entered by user in the passform.");
                br.submitForm(pwform);
                if (br.containsHTML("(This file was protected by password|Please enter the password below)")) continue;
                break;
            }
            if (br.containsHTML("(This file was protected by password|Please enter the password below)")) {
                logger.warning("Wrong password, the entered password \"" + passCode + "\" is wrong, retrying...");
                link.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
        }
        String filename = br.getRegex("Click HERE to download(.*?)</a>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("<li class=\"navItemHighlight\"><h2><img src=\"icon/341\\.gif\">(.*?)</h2>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>UfiLe -(.*?)\\(U-fiLe 2010\\)</title>").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("<br>Download Name:(.*?)<br>").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex("/common/images/orange_arrow\\.gif\\\\\"><a href=\\\\\"download\\.php\\?call=.*?\\\" ><b>(.*?)</b><").getMatch(0);
                    }
                }
            }
        }
        String filesize = br.getRegex("<br>File Size:(.*?)<br>").getMatch(0);
        if (filename == null) {
            logger.info("Browsercontent is: " + br.toString());
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        link.setName(filename.trim());
        if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = br.getRegex("\\\\\"(download\\.php\\?call=.*?)\\\\\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dllink = "http://www.u-file.net/" + dllink;
        sleep(16 * 1000l, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        int size = dl.getConnection().getContentLength();
        if (size == 23056508) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error or file offline!");
        if (dl.getConnection().getContentType() != null && dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML("Server is now very busy")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 4;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}