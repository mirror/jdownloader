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
import java.util.SortedMap;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "file2box.com" }, urls = { "http://[\\w\\.]*?file2box\\.com/[a-z0-9]{12}" }, flags = { 0 })
public class File2BoxCom extends PluginForHost {

    public File2BoxCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.file2box.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.file2box.com", "lang", "english");
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:.*?nowrap>(.*?)</td>").getMatch(0);
        String filesize = br.getRegex("\\(([0-9]+ bytes)\\)").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        if (br.containsHTML("You have to wait")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        }
        if (br.containsHTML("You have reached the download-limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        br.setFollowRedirects(false);
        Form DLForm = br.getFormbyProperty("name", "F1");
        if (DLForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String passCode = null;
        if (br.containsHTML("name=\"password\"")) {
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", downloadLink);
            } else {
                /* gespeicherten PassCode holen */
                passCode = downloadLink.getStringProperty("pass", null);
            }
            DLForm.put("password", passCode);
        }
        // Captcha method by
        String[][] letters = br.getRegex("<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
        if (letters.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
        for (String[] letter : letters) {
            capMap.put(Integer.parseInt(letter[0]), letter[1]);
        }
        StringBuilder code = new StringBuilder();
        for (String value : capMap.values()) {
            code.append(value);
        }
        DLForm.put("code", code.toString());
        // Ticket Time
        String ttt = br.getRegex("countdown\">.*?(\\d+).*?</span>").getMatch(0);
        if (ttt != null) {
            int tt = Integer.parseInt(ttt);
            sleep(tt * 1001, downloadLink);
        }
        br.submitForm(DLForm);
        String dllink = br.getRedirectLocation();
        URLConnectionAdapter con2 = br.getHttpConnection();
        if (con2.getContentType().contains("html")) {
            if (br.containsHTML("You have to wait")) {
                int minutes = 0, seconds = 0, hours = 0;
                String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            }
            if (br.containsHTML("You have reached the download-limit")) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
            if (br.containsHTML("name=\"password\"")) {
                logger.warning("Wrong password!");
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_RETRY);
            }
            if (br.containsHTML("Wrong captcha")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            if (dllink == null) dllink = br.getRegex("><b><a href=\"(.*?)\"").getMatch(0);
            if (dllink == null) dllink = br.getRegex("\"(http://serv[0-9]+\\.file2box\\.com/files/.*?/.*?/.*?)\"").getMatch(0);

        }
        if (passCode != null) {
            downloadLink.setProperty("pass", passCode);
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
        if (!(dl.getConnection().isContentDisposition())) {
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
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}