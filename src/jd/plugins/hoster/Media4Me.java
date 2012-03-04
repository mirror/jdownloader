//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "media-4.me" }, urls = { "http://(www\\.)?media\\-4\\.me/((\\?d|download\\.php\\?id)=[A-Z0-9]+|((en|ru|fr|es)/)?file/[0-9]+/)" }, flags = { 0 })
public class Media4Me extends PluginForHost {

    public Media4Me(PluginWrapper wrapper) {
        super(wrapper);
        // this.enablePremium(COOKIE_HOST + "/register.php?g=3");
    }

    @Override
    public String getAGBLink() {
        return COOKIE_HOST + "/rules.php";
    }

    private static final String COOKIE_HOST = "http://media-4.me";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setCookie(COOKIE_HOST, "mfh_mylang", "en");
        br.setCookie(COOKIE_HOST, "yab_mylang", "en");
        br.getPage(parameter.getDownloadURL());
        String newlink = br.getRegex("<p>The document has moved <a href=\"(.*?)\">here</a>\\.</p>").getMatch(0);
        if (newlink != null) {
            logger.info("This link has moved, trying to find and set the new link...");
            newlink = newlink.replaceAll("(\\&amp;|setlang=en)", "");
            parameter.setUrlDownload(newlink);
            br.getPage(newlink);
        }
        if (br.containsHTML("(Your requested file is not found|No file found)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("<b>File name:</b></td>[\r\t\n ]+<td align=([\r\t\n ]+|left width=\\d+px)>(.*?)</td>").getMatch(1);
        if (filename == null) {
            filename = br.getRegex("\"Click (this to report for|Here to Report)(.*?)\"").getMatch(1);
            if (filename == null) {
                filename = br.getRegex("<title>(.*?)</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("<b>(File size|Filesize):</b></td>[\r\t\n ]+<td align=([\r\t\n ]+|(\")?left(\")?)>(.*?)</td>").getMatch(4);
        if (filesize == null) filesize = br.getRegex("<strong>File size</strong></li>[\t\n\r ]+<li class=\"col\\-w50\">([^<>\"\\']+)</li>").getMatch(0);
        if (filename == null || filename.matches("")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setFinalFileName(filename.trim());
        if (filesize != null) parameter.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        this.setBrowserExclusive();
        requestFileInformation(link);
        /** Instead of an errormessage the downloadbutton disappears */
        if (!br.containsHTML("class=\"downloadbtn\"") && !br.containsHTML("flash-mp3-player.net/players/")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        br.getHeaders().put("X-Requested-With", "XMLHttpRequest");
        br.postPage(link.getDownloadURL(), "downloadverify=1&d=1");
        if (br.containsHTML("(The allowed download sessions assigned to your IP is used up:|Please try later, untill your current downloads are completed or)")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED);
        if (br.containsHTML("value=\"Free Users\""))
            br.postPage(link.getDownloadURL(), "Free=Free+Users");
        else if (br.getFormbyProperty("name", "entryform1") != null) br.submitForm(br.getFormbyProperty("name", "entryform1"));
        String finalLink = findLink();
        if (finalLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String waittime = br.getRegex("countdown\\((\\d+)\\)").getMatch(0);
        int wait = 15;
        if (waittime != null) wait = Integer.parseInt(waittime);
        sleep(wait * 1000l, link);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, finalLink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            if (br.containsHTML(">AccessKey is expired, please request new one from the download page")) throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Server error", 5 * 60 * 1000l);
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    private String findLink() throws Exception {
        String finalLink = br.getRegex("(http://.{5,30}getfile\\.php\\?id=\\d+[^\"\\']{10,500})(\"|\\')").getMatch(0);
        if (finalLink == null) {
            String[] sitelinks = HTMLParser.getHttpLinks(br.toString(), null);
            if (sitelinks == null || sitelinks.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            for (String alink : sitelinks) {
                alink = Encoding.htmlDecode(alink);
                if (alink.contains("access_key=") || alink.contains("getfile.php?")) {
                    finalLink = alink;
                    break;
                }
            }
        }
        return finalLink;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasAutoCaptcha() {
        return false;
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return false;
    }

    @Override
    public int getMaxSimultanPremiumDownloadNum() {
        return -1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

}
