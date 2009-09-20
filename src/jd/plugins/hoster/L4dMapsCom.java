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
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.parser.Regex;
import jd.plugins.BrowserAdapter;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.JDUtilities;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "l4dmaps.com" }, urls = { "http://[\\w\\.]*?l4dmaps\\.com/(details|mirrors|file-download)\\.php\\?file=[0-9]+" }, flags = { 0 })
public class L4dMapsCom extends PluginForHost {
    private static final String l4dservers = "l4dservers";

    public L4dMapsCom(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
        // TODO Auto-generated constructor stub
    }

    @Override
    public String getAGBLink() {
        return "http://www.l4dmaps.com/terms-of-use.php";
    }

    public void correctDownloadLink(DownloadLink link) {
        link.setUrlDownload(link.getDownloadURL().replaceAll("(details|mirrors|file-download)", "mirrors"));
    }

    
    
    
    private void setConfigElements() {
        String[] servers = new String[] { "1", "2" };
        config.addEntry(new ConfigEntry(ConfigContainer.TYPE_COMBOBOX_INDEX, JDUtilities.getConfiguration(), l4dservers, servers, JDL.L("plugins.host.L4dMapsCom.servers", "Use this server:")).setDefaultValue(0));
    }
    
    public int servers() {
        switch (JDUtilities.getConfiguration().getIntegerProperty(l4dservers, 0)) {
        case 1:
            return 1;
        case 2:
            return 2;
        default:
            return 1;
        }
    }
    
    
    
    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(parameter.getDownloadURL());
        String offlinecheck = br.getRedirectLocation();
        if (offlinecheck != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("color=\".*?\">.*?</font>.*?\\((.*?)\\).*?<font").getMatch(0);
        String filesize = br.getRegex(">Size:</font>(.*?)</strong>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        String server1 = br.getRegex("\">E-Frag #1<.*?\"(http://www\\.l4dmaps\\.com/file-download\\.php\\?file=[0-9]+&entry=[0-9]+)\"").getMatch(0);
//        String server2 = br.getRegex("\">E-Frag #2<.*?\"(http://www\\.l4dmaps\\.com/file-download\\.php\\?file=[0-9]+&entry=[0-9]+)\"").getMatch(0);
        br.getPage(server1);
        String dllink = br.getRegex("begin, <a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dllink = dllink.replace("amp;", "");
        dllink = "http://www.l4dmaps.com/" + dllink;
        br.getPage(dllink);
        dllink = br.getRedirectLocation();
        if (dllink.contains("index.php")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        // You can download up to 3 files simultaniously from one server so if
        // someone knows how to make JD know that it is downloading 3 files from
        // one server you could make jd switch to the other servers so you can
        // download 6 files at the same time
        BrowserAdapter.openDownload(br, link, dllink, true, -3);
        dl.startDownload();
    }

    @Override
    public void reset() {
        // TODO Auto-generated method stub

    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

}
