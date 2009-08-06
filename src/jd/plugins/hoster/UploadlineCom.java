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
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "uploadline.com" }, urls = { "http://[\\w\\.]*?uploadline\\.com/\\d+/.+" }, flags = { 0 })
public class UploadlineCom extends PluginForHost {

    public UploadlineCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(false);
        String dllink = "";

        // Initial Errorhandling
        if (br.containsHTML("Error happened when generating Download Link")) {
            String error = br.getRegex("err\">(.*?)</font>").getMatch(0);
            if (error != null) logger.severe(error);
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60000 * 60);
        }

        /* variation 1 for small files */
        if (br.containsHTML("for your IP next 24 hours")) {
            if (!br.containsHTML("hours<br><br>\\s+<a\\shref=\"(.*?)\"")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dllink = br.getRegex("hours<br><br>\\s+<a\\shref=\"(.*?)\"").getMatch(0);
        }

        /* variation 2 for big files */
        Form form = br.getForm(0);
        if (form != null && form.hasInputFieldByName("method_free")) {
            form.setAction(downloadLink.getDownloadURL());
            form.remove("method_premium");
            form.put("referer", Encoding.urlEncode(downloadLink.getDownloadURL()));
            br.submitForm(form);
            if (br.containsHTML("You have to wait")) {
                int minutes = 0, seconds = 0, hours = 0;
                String tmphrs = br.getRegex("\\s(\\d+)\\shours?").getMatch(0);
                if (tmphrs != null) hours = Integer.parseInt(tmphrs);
                String tmpmin = br.getRegex("\\s(\\d+)\\sminutes?").getMatch(0);
                if (tmpmin != null) minutes = Integer.parseInt(tmpmin);
                String tmpsec = br.getRegex("\\s(\\d+)\\sseconds?").getMatch(0);
                if (tmpsec != null) seconds = Integer.parseInt(tmpsec);
                int waittime = ((3600 * hours) + (60 * minutes) + seconds + 1) * 1000;
                throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, null, waittime);
            } else {
                form = br.getFormbyProperty("name", "F1");
                if (form == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);

                /* "Captcha Method" */
                String[][] letters = br.getRegex("<span style='position:absolute;padding-left:(\\d+)px;padding-top:\\d+px;'>(\\d)</span>").getMatches();
                if (letters.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
                for (String[] letter : letters) {
                    capMap.put(Integer.parseInt(letter[0]), letter[1]);
                }
                StringBuilder code = new StringBuilder();
                for (String value : capMap.values()) {
                    code.append(value);
                }
                form.setAction(downloadLink.getDownloadURL());
                form.put("code", code.toString());

                int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
                sleep(tt * 1001, downloadLink);
                br.submitForm(form);
                URLConnectionAdapter con2 = br.getHttpConnection();
                dllink = br.getRedirectLocation();
                if (con2.getContentType().contains("html")) {
                    if (br.containsHTML("Download Link Generated")) dllink = br.getRegex("hours<br><br>\\s+<a\\shref=\"(.*?)\">").getMatch(0);
                    if (br.containsHTML("Error happened when generating Download Link")) {
                        con2.disconnect();
                        String error = br.getRegex("err\">(.*?)</font>").getMatch(0);
                        if (error != null) logger.severe(error);
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60000 * 60);
                    }
                }
            }
        }

        if (dllink != null && dllink != "") {
            dl = jd.plugins.BrowserAdapter.openDownload(br,downloadLink, dllink, true, -4);
            dl.startDownload();
        } else
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    // @Override
    public String getAGBLink() {
        return "http://www.uploadline.com/tos.html";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        String filename = null;
        String filesize = null;
        this.setBrowserExclusive();
        br.setCookie("http://www.uploadline.com/", "lang", "english");
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("(No such file)|(No such user exist)|(Link expired)|(File Not Found)|(Error happened)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("available for Premium users only")) {
            logger.warning(JDL.L("plugins.host.uploadlinecom.premiumonly", "Uploadline.com: Files over 1 Gb are available for Premium users only"));
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, "See log");
        }
        if (br.getForm(0) == null) {
            filename = Encoding.htmlDecode(br.getRegex("Filename:\\s<b>(.*?)</b>").getMatch(0));
            filesize = br.getRegex("<br>\\s+Size:\\s(.*?)\\s<small>").getMatch(0);
        } else {
            filename = Encoding.htmlDecode(br.getRegex("You\\shave\\srequested\\s<font\\scolor=\"red\">http://[\\w\\.]*?uploadline\\.com/\\d+/(.*?)</font>").getMatch(0));
            filesize = br.getRegex("</font>\\s\\((.*?)\\)</font>").getMatch(0);
        }
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
