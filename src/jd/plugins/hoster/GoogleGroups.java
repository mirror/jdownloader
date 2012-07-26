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
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import jd.PluginWrapper;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "googlegroups.com" }, urls = { "http://[\\w\\.]*?googlegroups.com/web/.*" }, flags = { 0 })
public class GoogleGroups extends PluginForHost {

    public GoogleGroups(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    public boolean checkLinks(DownloadLink[] urls) {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        HashMap<String, ArrayList<DownloadLink>> map = new HashMap<String, ArrayList<DownloadLink>>();
        for (DownloadLink downloadLink : urls) {
            String subd = downloadLink.getDownloadURL().substring(7, downloadLink.getDownloadURL().indexOf(".googlegroups.com"));
            ArrayList<DownloadLink> link = map.get(subd);
            if (link != null)
                link.add(downloadLink);
            else {
                link = new ArrayList<DownloadLink>(1);
                link.add(downloadLink);
                map.put(subd, link);
            }
        }
        for (DownloadLink l : urls)
            l.setAvailable(false);
        for (Entry<String, ArrayList<DownloadLink>> entry : map.entrySet()) {
            try {
                br.getPage("http://groups.google.com/group/" + entry.getKey() + "/files");
                String[][] infos = br.getRegex("<td class=\"namecol\">.*?<a.*?href=\"(.*?)\">(.*?)</a>.*?<td class=\"sizecol\">(.*?)</td>").getMatches();
                for (DownloadLink downloadLink : entry.getValue()) {
                    String na = downloadLink.getDownloadURL().replaceFirst("\\?gda=.*", "");
                    na = na.replaceFirst("googlegroups.com/web/.*", "googlegroups.com/web/") + URLEncoder.encode(na.replaceFirst("http://.*?\\.googlegroups.com/web/", ""), "UTF-8");
                    for (String[] strings : infos) {
                        if (strings[0].contains(na) || downloadLink.getName().equals(strings[1])) {

                            downloadLink.setAvailable(true);
                            downloadLink.setFinalFileName(strings[1]);
                            downloadLink.setDownloadSize(SizeFormatter.getSize(strings[2]));
                            break;
                        }
                    }
                    if (downloadLink.getDownloadSize() < 1) downloadLink.setName(downloadLink.getDownloadURL().replaceFirst("\\?gda=.*", "").replaceFirst("googlegroups.com/web/", ""));
                }
            } catch (IOException e) {
                // TODO Auto-generated catch block
                logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
                return false;
            }
        }
        return true;
    }

    // @Override
    public String getAGBLink() {
        return "http://groups.google.com/intl/de/googlegroups/terms_of_service3.html";
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        // .googlegroups.com/web/
        String na = downloadLink.getDownloadURL().replaceFirst("\\?gda=.*", "");
        na = na.replaceFirst("googlegroups.com/web/.*", "googlegroups.com/web/") + URLEncoder.encode(na.replaceFirst("http://.*?\\.googlegroups.com/web/", ""), "UTF-8");
        br.setFollowRedirects(true);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, na);
        try {
            ((RAFDownload) dl).setFilesizeCheck(false);
        } catch (final Throwable e) {
        }
        dl.startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (downloadLink.getAvailableStatus() == AvailableStatus.FALSE) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return downloadLink.getAvailableStatus();
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    // @Override
    public void resetPluginGlobals() {
    }
}