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

import java.util.SortedMap;
import java.util.TreeMap;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zomgupload.com" }, urls = { "http://[\\w\\.]*?zomgupload\\.com/.+[/0-9a-zA-Z]+.html" }, flags = { 0 })
public class ZomgUploadCom extends PluginForHost {

    public ZomgUploadCom(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    public String getAGBLink() {
        return "http://www.zomgupload.com/tos.html";
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
        requestFileInformation(link);
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("<Title>ZOMG Upload - Free File Hosting</Title>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form form = br.getForm(0);
        form.remove("method_premium");
        br.submitForm(form);

        form = br.getForm(0);
        // Captcha. no image, just html placed numbers
        String[][] temp = br.getRegex("<span style='position:absolute;padding-left:([0-9]+)px;padding-top:[0-9]px;'>([0-9])</span>").getMatches();
        // COPY FROM BIGGERUPLOADCOM
        /* "Captcha Method" */
        if (temp.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        SortedMap<Integer, String> capMap = new TreeMap<Integer, String>();
        for (String[] letter : temp) {
            capMap.put(Integer.parseInt(letter[0]), letter[1]);
        }
        StringBuilder code = new StringBuilder();
        for (String value : capMap.values()) {
            code.append(value);
        }
        form.put("code", code.toString());
        // Ticket Time
        int tt = Integer.parseInt(br.getRegex("countdown\">(\\d+)</span>").getMatch(0));
        sleep(tt * 1001, link);
        String passCode = null;
        if (br.containsHTML("<br><b>Passwort:</b>")) {
            if (link.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput("Password?", link);
            } else {
                /* gespeicherten PassCode holen */
                passCode = link.getStringProperty("pass", null);
            }
            form.put("password", passCode);
        }
        br.submitForm(form);
        if (br.containsHTML("Wrong captcha")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        if (br.containsHTML("Wrong password")) {
            logger.warning("Wrong password!");
            link.setProperty("pass", null);
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
        if (passCode != null) {
            link.setProperty("pass", passCode);
        }
        String dllink = br.getRegex("#bbb;padding:[0-9]px;\">.*?<a href=\"(.*?)\"").getMatch(0);
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, link, dllink, true, 0);
        dl.startDownload();

    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        this.setBrowserExclusive();
        br.setCookie("http://www.zomgupload.com", "lang", "english");
        br.getPage(parameter.getDownloadURL());
        if (br.containsHTML("No such file with this filename")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("No such user exist")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        if (br.containsHTML("File not found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form[] forms = br.getForms();
        forms[0].remove("method_premium");
        br.submitForm(forms[0]);
        String filename = br.getRegex("<tr><td align=right><b>Filename:</b></td><td nowrap>(.*?)</b></td></tr>").getMatch(0);
        String filesize = br.getRegex("<tr><td align=right><b>Size:</b></td><td>(.*?)<small>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        parameter.setName(filename.trim());
        parameter.setDownloadSize(Regex.getSize(filesize.replaceAll(",", "\\.")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */
    public int getMaxSimultanFreeDownloadNum() {
        return 2;
    }

}
