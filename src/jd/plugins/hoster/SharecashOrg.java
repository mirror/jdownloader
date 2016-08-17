//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharecash.org" }, urls = { "https?://(?:www\\.)?(?:sharecash\\.org/download\\.php\\?(file|id)=\\d+|jafiles\\.net/[A-Za-z0-9]{2,})" }) 
public class SharecashOrg extends PluginForHost {

    public SharecashOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://sharecash.org/tos.php";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    private static final String ONLY4PREMIUMUSERTEXT = "This file is only downloadable for premium users!";

    @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws PluginException, IOException {
        this.setBrowserExclusive();
        try {
            br.setFollowRedirects(true);
            br.getPage(downloadLink.getDownloadURL());
            String filename = null, filesize = null, md5 = null;
            if (br.getURL().contains("sharecash.org/")) {
                final String linkID = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
                downloadLink.setName(linkID);
                if (isOffline()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = br.getRegex("<td width=\"120\"><strong>(.*?)</strong></td>").getMatch(0);
                if (filename == null) {
                    filename = linkID;
                }
                filesize = br.getRegex("<strong>File Size:</strong>([^<>\"]*?)<br />").getMatch(0);
                if (filesize == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                md5 = br.getRegex("<strong>MD5:</strong>([^<>\"]*?)<br />").getMatch(0);
            } else {
                final String linkID = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
                downloadLink.setName(linkID);

                if (isOffline()) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                }
                filename = br.getRegex("<td width=\"120\"><strong>(.*?)</strong></td>").getMatch(0);
                if (filename == null) {
                    /*
                     * 2016-07-22: Filename is usually not given as they have a php script which makes an image to display the filename
                     * e.g.: http://downloadwho.com/filename.php?f=linkID&t=CURRTIMESTAMP&h=blabla&d=3
                     */
                    filename = linkID;
                }
                filesize = br.getRegex("=\"fileSize\">[^<>]+</span>([^<>\"]+)</span>").getMatch(0);
                md5 = br.getRegex("=\"md5Value\">[^<>]+</span>([^<>\"]+)</span>").getMatch(0);
            }
            downloadLink.setFinalFileName(filename.trim());
            if (filesize != null) {
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            if (md5 != null) {
                downloadLink.setMD5Hash(md5.trim());
            }
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SharecashDotNet.only4premium", ONLY4PREMIUMUSERTEXT));
            return AvailableStatus.TRUE;
        } catch (NullPointerException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    private boolean isOffline() {
        if (br.containsHTML("(>File Does Not Exist|<title>ShareCash\\.Org \\- Make Money Uploading Files\\! \\- </title>)") || br.getURL().contains("/doesnt_exist.php")) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* 2016-07-22: Survey crap - usually not downloadable! */
        throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public void resetPluginGlobals() {
    }
}