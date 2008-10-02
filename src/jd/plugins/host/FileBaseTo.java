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

import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class FileBaseTo extends PluginForHost {

    public FileBaseTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        try {
            String url = downloadLink.getDownloadURL();
            br.getPage(url);

            downloadLink.setName(Plugin.extractFileNameFromURL(url).replaceAll("&dl=1", ""));
            if (br.containsHTML("Angeforderte Datei herunterladen")) {

                br.getPage(url + "&dl=1");
            }

            if (br.containsHTML("Vielleicht wurde der Eintrag")) {

            return false; }

            String size = br.getRegex("<font style=\"font-size: 9pt;\" face=\"Verdana\">Datei.*?font-size: 9pt\">(.*?)</font>").getMatch(0);
            downloadLink.setDownloadSize(Regex.getSize(size));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        LinkStatus linkStatus = downloadLink.getLinkStatus();

        if (!getFileInformation(downloadLink)) {
            linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
            return;
        }
        Form dl_form = br.getFormbyName("waitform");
        String value = br.getRegex(Pattern.compile("document\\.waitform\\.wait\\.value = \"(.*?)\";", Pattern.CASE_INSENSITIVE)).getMatch(0);

        dl_form.put("wait", value);

        dl = RAFDownload.download(downloadLink, br.createFormRequest(dl_form));
        dl.startDownload();
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}