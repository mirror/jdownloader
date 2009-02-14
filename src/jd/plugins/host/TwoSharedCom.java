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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class TwoSharedCom extends PluginForHost {

    public TwoSharedCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.2shared.com/terms.jsp";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException {
        br.setCookiesExclusive(true);
        br.getPage(downloadLink.getDownloadURL());
        Form pwform = br.getForm(0);
        if (pwform != null) {
            String filename = br.getRegex("<td class=\"header\" align=\"center\">Download (.*?)</td>").getMatch(0);
            if (filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            downloadLink.setName(filename.trim());
            return true;
        }
        String filesize = br.getRegex(Pattern.compile("<span class=.*?>File size:</span>(.*?)&nbsp; &nbsp;", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        String filename = br.getRegex("<title>2shared - download(.*?)</title>").getMatch(0);
        if (filesize == null || filename == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(filesize.trim().replaceAll(",|\\.", "")));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        /* Nochmals das File überprüfen */
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);
        Form pwform = br.getForm(0);
        if (pwform != null) {
            String passCode;
            if (downloadLink.getStringProperty("pass", null) == null) {
                passCode = Plugin.getUserInput(null, downloadLink);
            } else {
                passCode = downloadLink.getStringProperty("pass", null);
            }
            pwform.put("userPass2", passCode);
            br.submitForm(pwform);
            if (br.containsHTML("passError\\(\\);")) {
                downloadLink.setProperty("pass", null);
                throw new PluginException(LinkStatus.ERROR_CAPTCHA, "PW wrong");
            } else {
                downloadLink.setProperty("pass", passCode);
            }
        }
        String link = br.getRegex(Pattern.compile("function startDownload.*?window.location = \"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        if (link == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        dl = br.openDownload(downloadLink, link);
        if (dl.getConnection().getContentType().contains("text")) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, "Session limit reached", 10 * 60 * 1000l);
        }
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
