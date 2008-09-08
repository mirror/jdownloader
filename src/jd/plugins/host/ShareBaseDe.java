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
import java.util.regex.Pattern;

import jd.http.Encoding;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class ShareBaseDe extends PluginForHost {

    private static final String DOWLOAD_RUNNING = "Von deinem Computer ist noch ein Download aktiv";

    private static final Pattern FILEINFO = Pattern.compile("<span class=\"font1\">(.*?) </span>\\((.*?)\\)</td>", Pattern.CASE_INSENSITIVE);

    public ShareBaseDe(String cfgName) {
        super(cfgName);
    }

    @Override
    public String getAGBLink() {
        return "http://sharebase.de/pp.html";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.setFollowRedirects(true);
        String page = br.getPage(downloadLink.getDownloadURL());
        String[] infos = new Regex(page, FILEINFO).getRow(0);

        downloadLink.setName(infos[0].trim());
        downloadLink.setDownloadSize(Regex.getSize(infos[1].trim()));

        return true;

    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        br.setDebug(true);
        br.setFollowRedirects(true);
        String page = br.getPage(downloadLink.getDownloadURL());
        String fileName = Encoding.htmlDecode(new Regex(page, FILEINFO).getMatch(0));

        if (br.containsHTML(DOWLOAD_RUNNING)) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 1000l); }

        // DownloadInfos nicht gefunden? --> Datei nicht vorhanden
        if (fileName == null) {
            logger.severe("download not found");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        }

        Form form = br.getFormbyValue("Please Activate Javascript");
        form.setVariable(0, "Download+Now+%21");
        HTTPConnection urlConnection = br.openFormConnection(form);

        if (!urlConnection.isContentDisposition()) {
            br.followConnection();
            String wait = br.getRegex("Du musst noch <strong>(.*?)</strong> warten!").getMatch(0);
            if (wait != null) { throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, Regex.getMilliSeconds2(wait)); }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        }

        // Download starten
        dl = new RAFDownload(this, downloadLink, urlConnection);
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }

}
