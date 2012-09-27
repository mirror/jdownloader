//jDownloader - Downloadmanager
//Copyright (C) 2012  JD-Team support@jdownloader.org
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
import java.util.Random;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yousendit.com" }, urls = { "http(s)?://(www\\.)?(yousendit\\.com/(?!cms|signup|aboutus|applications|compare).{2,}|rcpt\\.yousendit\\.com/\\d+/[a-z0-9]+)" }, flags = { 0 })
public class YouSendItCom extends PluginForHost {

    private String DLLINK = null;

    public YouSendItCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void correctDownloadLink(DownloadLink link) {
        // Downloadlink must be correct, that's really important for this
        // hoster!
        String addedLink = link.getDownloadURL();
        Regex fileIDs = new Regex(addedLink, "rcpt\\.yousendit\\.com/(\\d+)/([a-z0-9]+)");
        String id0 = fileIDs.getMatch(0);
        if (id0 == null) id0 = new Regex(addedLink, "send_id=(\\d+)\\&").getMatch(0);
        String id1 = fileIDs.getMatch(1);
        if (id1 == null) id1 = new Regex(addedLink, "\\&email=([a-z0-9]+)").getMatch(0);
        if (id0 != null && id1 != null) addedLink = "https://www.yousendit.com/dl?phi_action=app/orchestrateDownload&rurl=https%253A%252F%252Fwww.yousendit.com%252Ftransfer.php%253Faction%253Dbatch_download%2526send_id%253D" + id0 + "%2526email%253D" + id1;
        link.setUrlDownload(addedLink);
    }

    @Override
    public String getAGBLink() {
        return "https://yousendit.com/aboutus/legal/terms-of-service";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        if (link.getDownloadURL().contains("directDownload")) {
            br.getPage(link.getDownloadURL());
            DLLINK = br.getRedirectLocation();
            if (DLLINK == null) {
                logger.warning("DLLINK is null!");
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.setFollowRedirects(false);
            URLConnectionAdapter con = null;
            try {
                con = br.openGetConnection(DLLINK);
                if (!con.getContentType().contains("html")) {
                    link.setDownloadSize(con.getLongContentLength());
                    link.setFinalFileName(getFileNameFromHeader(con));
                } else {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                return AvailableStatus.TRUE;
            } finally {
                try {
                    con.disconnect();
                } catch (Throwable e) {
                }
            }
        } else {
            br.setFollowRedirects(true);
            br.getPage(link.getDownloadURL());
            // File offline
            if (br.containsHTML("Download link is invalid|File Not Available<")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // File expired
            if (br.containsHTML("Sorry, this file has expired and cannot be downloaded")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            // Link invalid
            if (br.containsHTML(">The server returned a 404 response")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            String filename = br.getRegex("style=\"width:390px; display:block; overflow:hidden;\">(.*?)</a>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<span style=\\'color:#1F3CD6;\\'>(.*?)</span><").getMatch(0);
                if (filename == null) {
                    filename = br.getRegex("clsDownloadFileName\">(.*?)</").getMatch(0);
                    if (filename == null) {
                        filename = br.getRegex(">([^<>]+)</a>\\&nbsp\\; <span>").getMatch(0);
                        if (filename == null) {
                            filename = br.getRegex("title=\"([^<>\"]+)\" style=\"w").getMatch(0);
                            if (filename == null) {
                                logger.warning("YouSendIt: Can't find filename, Please report this to the JD Developement team!");
                                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                            }
                        }
                    }
                }
            }
            String filesize = br.getRegex("\">Size:\\&nbsp;<strong>(.*?)</strong>").getMatch(0);
            if (filesize == null) {
                filesize = br.getRegex(";\\'>Size: (.*?)\\&nbsp;").getMatch(0);
                if (filesize == null) {
                    filesize = br.getRegex("\">Size: (.*?)</").getMatch(0);
                    if (filesize == null) {
                        filesize = br.getRegex("(?i)\\(([\\d\\.]+ ?(MB|GB))\\)").getMatch(0);
                        if (filesize == null) {
                            logger.warning("YouSendIt: Can't find filesize, Please report this to the JD Developement team!");
                            logger.warning("YouSendIt: Continuing...");
                        }
                    }
                }
            }
            // Set the final filename here because server sometimes doesn't give
            // us the correct filename
            if (!filename.endsWith("..."))
                link.setFinalFileName(filename.trim());
            else
                link.setName(filename.trim() + new Random().nextInt(1000));
            if (filesize != null) link.setDownloadSize(SizeFormatter.getSize(filesize));
        }
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String dllink = DLLINK;
        // Nearly all regexes are needed because they have lots of different
        // downloadlinks...
        if (dllink == null) {
            dllink = br.getRegex("<div style=\"width:390px;font-size:14px;font-weight:bold\">[\t\r\n ]+<a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("onclick=\\'showDownloadProcessing\\(this\\);\\' style=\\'position:absolute;top:10px;right:10px;\\'>[\t\r\n ]+<a href=\"(.*?)\"").getMatch(0);
                if (dllink == null) {
                    dllink = br.getRegex("\"(directDownload\\?phi_action=app/directDownload\\&fl=[A-Za-z0-9]+(\\&experience=bas)?)\"").getMatch(0);
                    if (dllink == null) {
                        dllink = br.getRegex("<a id=\"download-button\" href=\"(http.*?)\"").getMatch(0);
                        if (dllink == null) {
                            dllink = br.getRegex("\"(http(s)?://(www\\.)?yousendit\\.com/transfer\\.php\\?action=check_download\\&ufid=[a-zA-Z0-9]+\\&key=[a-z0-9]+)\"").getMatch(0);
                        }
                    }
                }
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (!dllink.contains("yousendit.com")) dllink = "http://yousendit.com/" + dllink;
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
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
    public void resetDownloadlink(DownloadLink link) {
    }

}