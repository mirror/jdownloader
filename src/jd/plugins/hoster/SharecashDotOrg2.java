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

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sharecash.org" }, urls = { "http://(www\\.)?(sharecash\\.org/download\\.php\\?(file|id)=\\d+|jafiles\\.net/[A-Za-z0-9]{2,})" }, flags = { 0 })
public class SharecashDotOrg2 extends PluginForHost {

    public SharecashDotOrg2(PluginWrapper wrapper) {
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
            if (downloadLink.getDownloadURL().contains("sharecash.org/")) {
                final String linkID = new Regex(downloadLink.getDownloadURL(), "(\\d+)$").getMatch(0);
                downloadLink.setName(linkID);
                if (br.containsHTML("(>File Does Not Exist|<title>ShareCash\\.Org - Make Money Uploading Files\\! - </title>)") || br.getURL().contains("/doesnt_exist.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String filename = br.getRegex("<td width=\"120\"><strong>(.*?)</strong></td>").getMatch(0);
                if (filename == null) filename = linkID;
                final String filesize = br.getRegex("<strong>File Size:</strong>([^<>\"]*?)<br />").getMatch(0);
                if (filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String md5hash = br.getRegex("<strong>MD5:</strong>([^<>\"]*?)<br />").getMatch(0);
                if (md5hash != null) downloadLink.setMD5Hash(md5hash.trim());
                downloadLink.setFinalFileName(filename.trim());
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
            } else {
                if (br.containsHTML("<title>Download \\.</title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                if (br.getURL().equals("http://jafiles.net/doesnt_exist.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
                String filename = br.getRegex("<strong>Name:</strong> (.*?)<br").getMatch(0);
                if (filename == null) filename = br.getRegex("<title>Download (.*?)\\.</title>").getMatch(0);
                String filesize = br.getRegex("<strong>Size:</strong>(.*?)</p>").getMatch(0);
                if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                String md5hash = br.getRegex("<strong>MD5:</strong>(.*?)</p>").getMatch(0);
                if (md5hash != null) downloadLink.setMD5Hash(md5hash.trim());
                downloadLink.setFinalFileName(filename.trim());
                downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
            }
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.SharecashDotNet.only4premium", ONLY4PREMIUMUSERTEXT));
            return AvailableStatus.TRUE;
        } catch (NullPointerException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
    }

    @Override
    public void handleFree(final DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        try {
            throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
        } catch (final Throwable e) {
            if (e instanceof PluginException) throw (PluginException) e;
        }
        throw new PluginException(LinkStatus.ERROR_FATAL, ONLY4PREMIUMUSERTEXT);
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