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
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4share.ws" }, urls = { "http://(www\\.)?4share\\.ws/file/[a-z0-9]+/.*?\\.html" }, flags = { 0 })
public class FourShareWs extends PluginForHost {

    public FourShareWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://4share.ws/terms";
    }

    private static final String PASSWORDPROTECTED = "(File Is Protected|Enter password to continue)";
    private static final String PASSWORDWRONG     = ">You entered a wrong password";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        // They have a linkchecker but it doesn't show the filesize:
        // http://4share.ws/filechecker
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("(Not Found \\(  \\)|<title>4Share\\.WS</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Regex fileInfo = br.getRegex("image/ext/.*?\" class=\"middle\" alt=\"\" /> (.*?) \\( (.*?) \\)</h2><");
        String filename = br.getRegex("<title>(.*?) \\- Download \\- 4Share\\.WS</title>").getMatch(0);
        if (filename == null) filename = fileInfo.getMatch(0);
        String filesize = fileInfo.getMatch(1);
        if (filename == null || filesize == null || filesize.trim().equals("Byte")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        if (br.containsHTML("(You are already downloading a file from|To download another file you have to wait until current download process is finished|In case you are not downloading anything and got this message, then you are using a proxy-server or a shared IP-address)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 10 * 60 * 1000l);
        String id = new Regex(link.getDownloadURL(), "4share\\.ws/file/(.*?)/").getMatch(0);
        String passCode = null;
        if (br.containsHTML(PASSWORDPROTECTED)) {
            for (int i = 0; i <= 3; i++) {
                if (link.getStringProperty("pass", null) == null) {
                    passCode = getUserInput(null, link);
                } else {
                    /* gespeicherten PassCode holen */
                    passCode = link.getStringProperty("pass", null);
                }
                logger.info("Put password \"" + passCode + "\" entered by user in the DLForm.");
                br.postPage(link.getDownloadURL(), "id=" + id + "&fpwd=" + passCode);
                if (br.containsHTML(PASSWORDWRONG) || br.containsHTML(PASSWORDPROTECTED)) {
                    logger.info("Wrong password entered, retrying...");
                    br.getPage(link.getDownloadURL());
                    continue;
                }
                break;
            }
            if (br.containsHTML(PASSWORDWRONG) || br.containsHTML(PASSWORDPROTECTED)) {
                logger.info("Too many wrong passwords were entered, retrying...");
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (passCode != null) {
                link.setProperty("pass", passCode);
            }
        }
        br.postPage(link.getDownloadURL(), "id=" + id + "&code=down");
        String dllink = br.getRedirectLocation();
        if (dllink == null) {
            if (br.containsHTML(PASSWORDPROTECTED)) throw new PluginException(LinkStatus.ERROR_FATAL, "Server error");
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        if ((dl.getConnection().getContentType().contains("html"))) {
            br.followConnection();
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

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 5;
    }

}
