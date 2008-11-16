//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class ShragleCom extends PluginForHost {

    public ShragleCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.shragle.com/index.php?cat=about&p=faq";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Die von Ihnen gewählte Datei wurde nicht gefunden.")) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        String size = br.getRegex("« \\((.*?)\\) runterladen").getMatch(0);
        String name = br.getRegex("»(.*?)«").getMatch(0).trim();
        if (name == null || size == null) { throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND); }
        downloadLink.setName(name.trim());
        downloadLink.setDownloadSize(Regex.getSize(size.replaceAll("Кб", "KB").replaceAll("Mб", "MB")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        Form form = br.getFormbyName("download");
        sleep(10000l, downloadLink);
        br.setFollowRedirects(true);
        /*
         * zum zeitpunkt der implementation waren nur 3 verbindungen gesamt
         * erlaubt
         */
        dl = br.openDownload(downloadLink, form, true, -3);
        HTTPConnection con = dl.getConnection();
        if (!con.isContentDisposition()) {
            con.disconnect();
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, 60 * 60 * 1000l);
        }
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public int getTimegapBetweenConnections() {
        return 800;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}