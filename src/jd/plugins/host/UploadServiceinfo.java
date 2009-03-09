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

import java.io.IOException;
import java.util.HashMap;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class UploadServiceinfo extends PluginForHost {

    public UploadServiceinfo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.uploadservice.info/rules.html";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        this.setBrowserExclusive();

        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (!br.containsHTML("<strong>Die ausgew&auml;hlte Datei existiert nicht!</strong>")) {
            downloadLink.setName(Encoding.htmlDecode(new Regex(br, Pattern.compile("<input type=\"text\" value=\"(.*?)\" /></td>", Pattern.CASE_INSENSITIVE)).getMatch(0)));
            String filesize = null;
            if ((filesize = new Regex(br, "<td style=\"font-weight: bold;\">(\\d+) MB</td>").getMatch(0)) != null) {
                downloadLink.setDownloadSize(new Integer(filesize) * 1024 * 1024);
            }
            return true;
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    @Override
    public String getVersion() {

        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        /* Link holen */
        String url = br.getForms()[0].getAction();
        HashMap<String, String> submitvalues = HTMLParser.getInputHiddenFields(br.toString());
        String postdata = "key=" + Encoding.urlEncode(submitvalues.get("key"));
        postdata = postdata + "&mysubmit=Download";

        /* Zwangswarten, 10seks, kann man auch weglassen */
        sleep(10000, downloadLink);

        /* Datei herunterladen */
        dl = br.openDownload(downloadLink, url, postdata);

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
