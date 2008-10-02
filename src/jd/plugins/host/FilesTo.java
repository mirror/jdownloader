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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

public class FilesTo extends PluginForHost {

    public FilesTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.files.to/content/aup";
    }

    @Override
    public boolean getFileInformation(DownloadLink downloadLink) {
        try {
            br.getPage(downloadLink.getDownloadURL());
            if (!br.containsHTML("Die angeforderte Datei konnte nicht gefunden werden")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("<p>Name: <span id=\"downloadname\">(.*?)</span></p>").getMatch(0)));
                downloadLink.setDownloadSize(Regex.getSize(br.getRegex("<p>Gr&ouml;&szlig;e: (.*? (KB|MB|B))</p>").getMatch(0)));
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        downloadLink.setAvailable(false);
        return false;
    }

    @Override
    public String getFileInformationString(DownloadLink downloadLink) {
        return downloadLink.getName() + " (" + JDUtilities.formatBytesToMB(downloadLink.getDownloadSize()) + ")";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {

        if (!getFileInformation(downloadLink)) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        br.getPage(downloadLink.getDownloadURL());
        String captchaAddress = br.getRegex("<img src=\"(http://www\\.files\\.to/captcha_[\\d]+\\.jpg\\?)").getMatch(0);

        File captchaFile = this.getLocalCaptchaFile(this);
        Browser.download(captchaFile, br.cloneBrowser().openGetConnection(captchaAddress));
        String code = Plugin.getCaptchaCode(captchaFile, this, downloadLink);

        Form captchaForm = br.getForm(0);
        captchaForm.put("txt_ccode", code);
        br.submitForm(captchaForm);

        if (br.containsHTML("Der eingegebene code ist falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);

        br.openDownload(downloadLink, br.getRegex("action\\=\"(http://.*?files\\.to/dl/.*?)\">").getMatch(0), true, 1).startDownload();
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
