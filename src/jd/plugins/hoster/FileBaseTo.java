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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(names = { "filebase.to"}, urls ={ "http://[\\w\\.]*?filebase\\.to/files/\\d{1,}/.*"}, flags = {0})
public class FileBaseTo extends PluginForHost {

    public FileBaseTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        // br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        downloadLink.setName(Plugin.extractFileNameFromURL(url).replaceAll("&dl=1", ""));
        br.setDebug(true);
        if (br.containsHTML("eider\\s+nicht\\s+gefunden")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String size = br.getRegex("Dateigr[^:]*:</td>\\s+<td[^>]*>(.*?)</td>").getMatch(0);
        downloadLink.setDownloadSize(Regex.getSize(size));
        return AvailableStatus.TRUE;

    }

    // @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        String formact = downloadLink.getDownloadURL();
        if (br.containsHTML("/captcha/CaptchaImage")) {
            File captchaFile = getLocalCaptchaFile(".png");
            String captchaFileURL = br.getRegex("src=\"(/captcha/CaptchaImage\\.php.*?)\"").getMatch(0);
            String filecid = br.getRegex("cid\"\\s+value=\"(.*?)\"").getMatch(0);
            Browser.download(captchaFile, br.openGetConnection("http://filebase.to" + captchaFileURL));
            String capTxt = getCaptchaCode(captchaFile, downloadLink);
            br.postPage(formact, "uid=" + capTxt + "&cid=" + Encoding.urlEncode(filecid) + "&submit=+++Best%E4tigung+++&session_code=");
            // if captcha error
            if (br.containsHTML("Code wurde falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        }

        String dlAction = br.getRegex("<form action=\"(http.*?)\"").getMatch(0);
        if (dlAction != null) 
        {
            dl = br.openDownload(downloadLink, dlAction, "wait=" + Encoding.urlEncode("Download - " + downloadLink.getName()));
        }
        else
        {
            dlAction = br.getRegex("value=\"(http.*?/download/ticket.*?)\"").getMatch(0);
            if (dlAction == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            dl = br.openDownload(downloadLink, dlAction);
        }
        br.setDebug(true);
        URLConnectionAdapter con = dl.getConnection();
        if (con.getContentType().contains("html")) {
            br.getPage(dlAction);
            if (br.containsHTML("error")) {
                con.disconnect();
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND,JDL.L("plugins.hoster.filebaseto.servererror","Server error"));
            }
            else
            {
                con.disconnect();
                logger.warning("Unsupported error:");
                logger.warning(br.toString());
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT,JDL.L("plugins.hoster.filebaseto.unsupportederror","Unsupported error"));
            }
        }
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
    public void resetDownloadlink(DownloadLink link) {
        // TODO Auto-generated method stub

    }
}
