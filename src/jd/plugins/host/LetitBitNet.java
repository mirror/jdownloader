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

import java.io.File;
import java.io.IOException;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.HTTPConnection;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class LetitBitNet extends PluginForHost {

    public LetitBitNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://letitbit.net/page/terms.php";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        br.getPage(downloadLink.getDownloadURL());
        String filename = null;
        String size = null;
        try {
            filename = br.getXPathElement("/html/body/div[2]/div[3]/div/h1[1]").trim();
            size = br.getXPathElement("/html/body/div[2]/div[3]/div/h1[2]").trim();
        } catch (Exception e) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        if (filename == null || size == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setName(filename.trim());
        downloadLink.setDownloadSize(Regex.getSize(size));
        return true;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        getFileInformation(downloadLink);
        Form info = br.getForm(3);
        String id = info.getVarsMap().get("uid");
        Form down = br.getForm(4);
        if (down == null) throw new PluginException(LinkStatus.ERROR_FATAL, "Your country is blocked by Letitbit");
        HTTPConnection con = br.openGetConnection("http://letitbit.net/cap.php?jpg=" + id + ".jpg");
        File file = this.getLocalCaptchaFile(this);
        Browser.download(file, con);
        down.action = "http://letitbit.net/download3.php";
        down.method = Form.METHOD_POST;
        down.put("frameset", "Download+file");
        String code = Plugin.getCaptchaCode(file, this, downloadLink);
        down.put("cap", code);
        down.put("fix", "1");
        br.setDebug(true);
        br.submitForm(down);
        String url = br.getRegex("link=(.*?)\"").getMatch(0);
        if (url == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        this.sleep(60000, downloadLink);
        dl = br.openDownload(downloadLink, url, true, 1);
        if (dl.getConnection().getResponseCode() == 404) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_RETRY);
        }
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