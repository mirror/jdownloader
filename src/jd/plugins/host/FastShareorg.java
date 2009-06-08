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

package jd.plugins.host;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class FastShareorg extends PluginForHost {

    public FastShareorg(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.fastshare.org/discl.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.setCookiesExclusive(true);
            br.clearCookies(getHost());
            br.setFollowRedirects(false);
            String url = downloadLink.getDownloadURL();
            br.getPage(url);
            if (!br.containsHTML("No filename specified or the file has been deleted")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("Wenn sie die Datei \"<b>(.*?)<\\/b>\"").getMatch(0)));
                String filesize = null;
                if ((filesize = br.getRegex("<i>\\((.*)MB\\)</i>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024 * 1024);
                } else if ((filesize = br.getRegex("<i>\\((.*)KB\\)</i>").getMatch(0)) != null) {
                    downloadLink.setDownloadSize((int) Math.round(Double.parseDouble(filesize)) * 1024);
                }
                return AvailableStatus.TRUE;
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    // @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        String url = downloadLink.getDownloadURL();
        requestFileInformation(downloadLink);

        /* Link holen */
        Form form = br.getForm(0);
        br.submitForm(form);
        if ((url = new Regex(br, "Link: <a href=(.*)><b>").getMatch(0)) == null) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }

        /* Zwangswarten, 10seks */
        sleep(10000, downloadLink);

        dl = br.openDownload(downloadLink, url, false, 1);
        dl.startDownload();

    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    // @Override
    public void reset() {
    }

    // @Override
    public void resetPluginGlobals() {
    }

    // @Override
    public void reset_downloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
