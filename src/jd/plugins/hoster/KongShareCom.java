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
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "kongshare.com" }, urls = { "http://[\\w\\.]*?kongshare\\.com/DownloadExtra\\.aspx\\?d=[a-zA-Z0-9]+-[a-zA-Z0-9-.]+" }, flags = { 0 })
public class KongShareCom extends PluginForHost {

    public KongShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://kongshare.com/AboutExtra.aspx";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("File does not exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("LabelFileName\">(.*?)</span>").getMatch(0);
        if (filename == null) filename = br.getRegex("action=\"DownloadExtra\\.aspx\\?d=.*?-(.*?)\"").getMatch(0);
        if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String viewstate = br.getRegex("id=\"__VIEWSTATE\" value=\"(.*?)\"").getMatch(0);
        String previouspage = br.getRegex("id=\"__PREVIOUSPAGE\" value=\"(.*?)\"").getMatch(0);
        String eventvalidation = br.getRegex("id=\"__EVENTVALIDATION\" value=\"(.*?)\"").getMatch(0);
        if (viewstate == null || previouspage == null || eventvalidation == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        Form dlform = new Form();
        dlform.setMethod(Form.MethodType.POST);
        dlform.setAction(downloadLink.getDownloadURL().replace("DownloadExtra.aspx", "Default.aspx"));
        dlform.put("__EVENTTARGET", "");
        dlform.put("__EVENTARGUMENT", "");
        dlform.put("__VIEWSTATE", viewstate);
        dlform.put("ctl00$ContentPlaceHolder1$Button1", "here");
        dlform.put("ctl00$ContentPlaceHolder1$Phonenr1", "+49");
        dlform.put("ctl00$ContentPlaceHolder1$CodeNumberTextBox", "");
        dlform.put("__PREVIOUSPAGE", previouspage);
        dlform.put("__EVENTVALIDATION", eventvalidation);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dlform, false, 1);
        if (!(dl.getConnection().isContentDisposition())) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
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
