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
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mekashare.com" }, urls = { "http://[\\w\\.]*?(mekashare|literack)\\.com/\\d+/.+" }, flags = { 0 })
public class MekaShareCom extends PluginForHost {

    public MekaShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mekashare.com/terms/";
    }

    public void correctDownloadLink(DownloadLink link) {
        String thelink = link.getDownloadURL();
        // We only use mekashare links!
        thelink = thelink.replace("literack.com", "mekashare.com");
        String fileid = new Regex(thelink, "mekashare\\.com/(\\d+/.+)").getMatch(0);
        link.setUrlDownload("http://mekashare.com/" + fileid);
    }

    // Old availablecheck for single files
    // @Override
    // public AvailableStatus requestFileInformation(DownloadLink link) throws
    // IOException, PluginException {
    // this.setBrowserExclusive();
    // String checklinks = "http://mekashare.com/check/";
    // br.getPage(checklinks);
    // br.postPage(checklinks, "links=" + link.getDownloadURL());
    // // if (br.containsHTML("you have requested could not be found")) throw
    // // new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // if (br.containsHTML(">Inexistent<")) throw new
    // PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    // String filename = new Regex(link.getDownloadURL(),
    // "mekashare\\.com/\\d+/(.+)").getMatch(0);
    // String filesize =
    // br.getRegex("width=\"112\" align=\"left\" style=\"padding:5px\">(.*?)</td>").getMatch(0);
    // if (filename == null || filename == null) throw new
    // PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
    // filesize = filesize.replace("i", "");
    // link.setName(filename.trim());
    // link.setDownloadSize(Regex.getSize(filesize));
    // return AvailableStatus.TRUE;
    // }
    public boolean checkLinks(DownloadLink[] urls) {
        if (urls == null || urls.length == 0) { return false; }
        try {
            ArrayList<DownloadLink> links = new ArrayList<DownloadLink>();
            int index = 0;
            StringBuilder sb = new StringBuilder();
            while (true) {
                sb.delete(0, sb.capacity());
                sb.append("links=");
                links.clear();
                while (true) {
                    /* we test 500 links at once */
                    if (index == urls.length || links.size() > 500) break;
                    links.add(urls[index]);
                    index++;
                }
                br.getPage("http://mekashare.com/check/");
                int c = 0;
                for (DownloadLink dl : links) {
                    /*
                     * append fake filename, because api will not report
                     * anything else
                     */
                    if (c > 0) sb.append("%0D%0A");
                    sb.append(Encoding.urlEncode(dl.getDownloadURL()));
                    c++;
                }
                br.postPage("http://mekashare.com/check/", sb.toString());
                for (DownloadLink dl : links) {
                    String regexForThisLink = "(<a href=\"" + dl.getDownloadURL() + "\">" + dl.getDownloadURL() + "</a></td>.*?<td width=\"\\d+\" align=\"left\" style=\"padding:5px\">.*?</td>.*?<td width=\"\\d+\" align=\"left\" style=\"padding:5px\">.*?</td>)";
                    String regexForThisLink2 = "(style=\"padding:5px\">" + dl.getDownloadURL() + "</td>.*?<td width=\"\\d+\" align=\"left\" style=\"padding:5px\">.*?</td>.*?<td width=\"\\d+\" align=\"left\" style=\"padding:5px\">.*?</td>)";
                    String theData = br.getRegex(regexForThisLink).getMatch(0);
                    if (theData == null) theData = br.getRegex(regexForThisLink2).getMatch(0);
                    if (theData == null) {
                        logger.warning("Mekashare.com availablecheck is broken!");
                        return false;
                    }
                    theData = theData.replace("style=\"padding:5px\">" + dl.getDownloadURL() + "</td>", "");
                    String infos[][] = new Regex(theData, ".*?style=\"padding:5px\">(.*?)</td>.*?style=\"padding:5px\">(.*?)</td>").getMatches();
                    String filesize = null;
                    String status = null;
                    for (String[] info : infos) {
                        filesize = info[0].toString();
                        status = info[1].toString();
                    }
                    String filename = new Regex(dl.getDownloadURL(), "mekashare\\.com/\\d+/(.+)").getMatch(0);
                    if (!status.matches("Available")) {
                        dl.setAvailable(false);
                    } else if (filename == null || filesize == null) {
                        logger.warning("Mekashare.com availablecheck is broken!");
                        dl.setAvailable(false);
                    } else {
                        dl.setAvailable(true);
                    }
                    if (filename != null) dl.setName(filename);
                    if (filesize != null) {
                        filesize = filesize.replace("i", "");
                        dl.setDownloadSize(Regex.getSize(filesize));
                    }
                }
                if (index == urls.length) break;
            }
        } catch (Exception e) {
            return false;
        }
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException {
        checkLinks(new DownloadLink[] { downloadLink });
        if (!downloadLink.isAvailabilityStatusChecked()) {
            downloadLink.setAvailableStatus(AvailableStatus.UNCHECKABLE);
        }
        return downloadLink.getAvailableStatus();
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        // Ticket Time
        String ttt = br.getRegex("var t =.*?(\\d+);").getMatch(0);
        int tt = 60;
        if (ttt != null) {
            logger.info("Waittime detected, waiting " + ttt + " seconds from now on...");
            tt = Integer.parseInt(ttt);
        }
        if (tt > 101) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, tt * 1001l);
        sleep(tt * 1001, downloadLink);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), "", false, 1);
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
        return 1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}