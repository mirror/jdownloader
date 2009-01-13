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
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

public class FileBaseTo extends PluginForHost {

    public FileBaseTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://filebase.to/tos/";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) throws IOException, PluginException {
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        downloadLink.setName(Plugin.extractFileNameFromURL(url).replaceAll("&dl=1", ""));
        if (br.containsHTML("Angeforderte Datei herunterladen")) {
            br.getPage(url + "&dl=1");
        }

        if (br.containsHTML("Vielleicht wurde der Eintrag")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String size = br.getRegex("<font style=\"font-size: 9pt;\" face=\"Verdana\">Datei.*?font-size: 9pt\">(.*?)</font>").getMatch(0);
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

        String url = downloadLink.getDownloadURL() + "&dl=1";
        br.getPage(url);
        Form caform = null;
        br.setFollowRedirects(true);
        int i = 5;
        while ((caform = br.getFormbyValue("Ok!")) != null) {
            if (i-- <= 0) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            File captchaFile = Plugin.getLocalCaptchaFile(this, ".gif");
            Browser.download(captchaFile, br.openGetConnection("http://filebase.to" + br.getRegex("<img src=\"(/captcha/CaptchaImage\\.php.*?)\" alt=\"\">").getMatch(0)));
            String capTxt = Plugin.getCaptchaCode(this, "datenklo.net", captchaFile, false, downloadLink);
            caform.put("uid", capTxt);
            caform.action = url;
            br.submitForm(caform);

        }

        Form dlForm = br.getFormbyName("waitform");
        String value = br.getRegex("document\\.waitform\\.wait\\.value = \"(.*?)\";").getMatch(0);

        dlForm.put("wait", value);
        br.openDownload(downloadLink, dlForm, true, 1).startDownload();

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