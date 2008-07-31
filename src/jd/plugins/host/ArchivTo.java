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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Pattern;

import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.HTTP;
import jd.plugins.HTTPConnection;
import jd.plugins.LinkStatus;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

//http://archiv.to/Get/?System=Download&Hash=FILE4799F3EC23328
// http://archiv.to/?Module=Details&HashID=FILE4799F3EC23328

public class ArchivTo extends PluginForHost {

    static private final String FILENAME = "<td width=\".*\">Original-Dateiname</td>\n	<td width=\".*\">: <a href=\".*\" style=\".*\">(.*?)</a></td>";

    static private final String FILESIZE = "<td width=\".*\">: ([0-9]+) Byte";

    private static final String HOST = "archiv.to";

    static private final Pattern patternSupported = Pattern.compile("http://[\\w\\.]*?archiv\\.to/\\?Module\\=Details\\&HashID\\=.*", Pattern.CASE_INSENSITIVE);

    //

    public ArchivTo() {
        super();

    }

    @Override
    public boolean doBotCheck(File file) {
        return false;
    } // kein BotCheck

    @Override
    public String getAGBLink() {
        return "http://archiv.to/?Module=Policy";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();
            requestInfo = HTTP.getRequest(new URL(url));
            downloadLink.setName(new Regex(requestInfo.getHtmlCode(), FILENAME).getFirstMatch());
            if (!requestInfo.getHtmlCode().contains(":  Bytes (~ 0 MB)")) {
                downloadLink.setDownloadMax(Integer.parseInt(new Regex(requestInfo.getHtmlCode(), FILESIZE).getFirstMatch()));
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public String getHost() {
        return HOST;
    }

    @Override
    public int getMaxSimultanDownloadNum() {
        return Integer.MAX_VALUE;
    }

    @Override
    public String getPluginName() {
        return HOST;
    }

    @Override
    public Pattern getSupportedLinks() {
        return patternSupported;
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getFirstMatch();
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        LinkStatus linkStatus = downloadLink.getLinkStatus();
        try {
            String url = downloadLink.getDownloadURL();

            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL("http://archiv.to/Get/?System=Download&Hash=" + new Regex(url, ".*HashID=(.*)").getFirstMatch()), null, url, true);

            HTTPConnection urlConnection = requestInfo.getConnection();
            if (!getFileInformation(downloadLink)) {
                linkStatus.addStatus(LinkStatus.ERROR_FILE_NOT_FOUND);
                return;
            }
            final long length = downloadLink.getDownloadMax();
            downloadLink.setName(getFileNameFormHeader(urlConnection));

            dl = new RAFDownload(this, downloadLink, urlConnection);
            dl.setFilesize(length);
            dl.startDownload();
            return;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // step.setStatus(PluginStep.STATUS_ERROR);
        linkStatus.addStatus(LinkStatus.ERROR_RETRY);

        return;
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetPluginGlobals() {
    }
}
