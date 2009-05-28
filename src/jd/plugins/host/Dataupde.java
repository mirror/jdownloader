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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

public class Dataupde extends PluginForHost {
    public Dataupde(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    // @Override
    public String getAGBLink() {
        return "http://www.dataup.de/agb";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.setCookie("http://www.dataup.de/", "language", "en");
            br.setFollowRedirects(false);
            br.getPage(downloadLink.getDownloadURL());

            if (br.getRedirectLocation() != null) br.getPage(br.getRedirectLocation());
            String dlurl = br.getRegex("action=\"(http://dataup\\.de/\\d+/dl/.+)\"\\smethod").getMatch(0);
            if (dlurl != null) br.getPage(dlurl);
            if (!br.containsHTML("no download at this link")) {
                String filename = br.getRegex("helvetica;\"><b>(.*?)</b></div>").getMatch(0);
                if (filename == null) filename = br.getRegex("helvetica;\">(.*?)</div>").getMatch(0);
                String filesizeString = br.getRegex("<label>Size:\\s(.*?)</label><br\\s/>").getMatch(0);
                downloadLink.setDownloadSize(Regex.getSize(filesizeString));
                downloadLink.setName(filename);
                return AvailableStatus.TRUE;
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occured", e);
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
        requestFileInformation(downloadLink);

        /* 10 seks warten, kann weggelassen werden */
        // this.sleep(10000, downloadLink);
        /* DownloadLink holen */
        Form form = br.getForms()[2];

        dl = br.openDownload(downloadLink, form, true, 1);

        if (dl.getConnection().getLongContentLength() == 0) {
            linkStatus.addStatus(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE);
            linkStatus.setValue(20 * 60 * 1000l);
            return;
        }
        /* DownloadLimit? */
        if (!dl.getConnection().isContentDisposition()) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 180000);

        dl.startDownload();
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
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
