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
import java.net.UnknownHostException;
import java.util.HashMap;

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "speedy-share.com" }, urls = { "http://[\\w\\.]*?speedy\\-share\\.com/[\\w]+/(.*)" }, flags = { 0 })
public class Speedy_ShareCom extends PluginForHost {

    private String postdata;

    public Speedy_ShareCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.speedy-share.com/tos.html";
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    /*
     * public String getVersion() {
     * 
     * return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        /* Link holen */
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(br.toString());
        postdata = "act=" + Encoding.urlEncode(submitvalues.get("act"));
        postdata = postdata + "&id=" + Encoding.urlEncode(submitvalues.get("id"));
        postdata = postdata + "&fname=" + Encoding.urlEncode(submitvalues.get("fname"));
        if (br.containsHTML("type=\"password\" name=\"password\"")) {
            String password = Plugin.getUserInput(JDL.L("plugins.hoster.speedysharecom.password", "Enter Password:"), downloadLink);
            if (password != null && !password.equals("")) {
                postdata = postdata + "&password=" + Encoding.urlEncode(password);
            }
        }

        /* Zwangswarten, 30seks */
        sleep(30000, downloadLink);

        /* Datei herunterladen */
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, downloadLink.getDownloadURL(), postdata).startDownload();
    }

    // @Override
    public AvailableStatus requestFileInformation(final DownloadLink downloadLink) throws IOException, PluginException {
        try {
            br.getPage(downloadLink.getDownloadURL());
        } catch (final UnknownHostException e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (!br.containsHTML("File Not Found")) {
            downloadLink.setName(Encoding.htmlDecode(br.getRegex("File Name:</span>(.*?)</span>").getMatch(0)));
            downloadLink.setDownloadSize(SizeFormatter.getSize(br.getRegex("File Size:</span>(.*?)</span>").getMatch(0)));
            return AvailableStatus.TRUE;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
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