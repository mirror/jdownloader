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
import java.util.SortedMap;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.http.URLConnectionAdapter;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "6giga.com" }, urls = { "http://[\\w\\.]*?6giga\\.com/[a-z0-9]+" }, flags = { 0 })
public class SixGigaCom extends PluginForHost {

    public SixGigaCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);

        if (br.containsHTML("You have reached")) {
            int minutes = 0, seconds = 0, hours = 0;
            String tmphrs = br.getRegex("\\s+(\\d+)\\s+hours?").getMatch(0);
            if (tmphrs != null) hours = Integer.parseInt(tmphrs);
            String tmpmin = br.getRegex("\\s+(\\d+)\\s+minutes?").getMatch(0);
            if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
            String tmpsec = br.getRegex("\\s+(\\d+)\\s+seconds?").getMatch(0);
            if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
            int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
        } else {
            Form form = br.getFormbyProperty("name", "F1");
            if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            /* "Captcha Method" */
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

            form.put("code", code.toString());
            form.setAction(downloadLink.getDownloadURL());
            // Ticket Time
            int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
            sleep(tt * 1001, downloadLink);
            br.submitForm(form);
            URLConnectionAdapter con2 = br.getHttpConnection();
            String dllink = br.getRedirectLocation();
            if (con2.getContentType().contains("html")) {
                String error = br.getRegex("class=\"err\">(.*?)</font>").getMatch(0);
                if (error != null) {
                    logger.warning(error);
                    con2.disconnect();
                    if (error.equalsIgnoreCase("Wrong captcha") || error.equalsIgnoreCase("Expired session")) {
                        throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                    } else {
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, error, 10000);
                    }
                }
                if (br.containsHTML("Download Link Generated")) dllink = br.getRegex("padding:7px;\">\\s+<a\\s+href=\"(.*?)\">").getMatch(0);
            }
            if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, true, 0);
            dl.startDownload();
        }
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public String getAGBLink() {
        return "http://6giga.com/tos.html";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setCookie("http://www.6giga.com/", "lang", "english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("No such file")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("Filename:</b></td><td\\s+nowrap>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("\\((\\d+) bytes\\)").getMatch(0);
        if (filesize == null) {
            filesize = br.getRegex("<b>Size:</b></td><td>(.*?)<").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
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
