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

import jd.PluginWrapper;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.download.RAFDownload;

public class ShareBaseTo extends PluginForHost {

    private static final Pattern FILEINFO = Pattern.compile("<span class=\"font1\">(.*?) </span>\\((.*?)\\)</td>", Pattern.CASE_INSENSITIVE);

    public ShareBaseTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://sharebase.to/terms/";
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException {

        br.setFollowRedirects(true);
        String[] infos = new Regex(br.getPage(downloadLink.getDownloadURL()), FILEINFO).getRow(0);

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
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());

        String url = downloadLink.getDownloadURL();

        br.getPage(url);
        if (br.containsHTML("Der Download existiert nicht")) {
            logger.severe("ShareBaseTo Error: File not found");
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }

        Form form = br.getFormbyValue("Please Activate Javascript");
        form.setVariable(0, "Download+Now+%21");
        br.submitForm(form);

        if (br.containsHTML("Von deinem Computer ist noch ein Download aktiv.")) {
            logger.severe("ShareBaseTo Error: Too many downloads");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Nur ein Download gleichzeitig!", 5000);
        } else if (br.containsHTML("werden derzeit Wartungsarbeiten vorgenommen")) {
            logger.severe("ShareBaseTo Error: Maintenance");
            throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, "Wartungsarbeiten", 30 * 60 * 1000l);
        }
        String[] wait = br.getRegex("Du musst noch <strong>(\\d*?)min (\\d*?)sec</strong> warten").getRow(0);
        if (wait != null) {
            long waitTime = (Integer.parseInt(wait[0]) * 60 + Integer.parseInt(wait[1])) * 1000l;
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, waitTime);
        }
        HTTPConnection urlConnection = br.openGetConnection(br.getRedirectLocation());      
        RAFDownload.download(downloadLink, urlConnection, false, 1);
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
