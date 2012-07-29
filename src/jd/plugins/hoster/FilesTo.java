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

import jd.PluginWrapper;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "files.to" }, urls = { "http://(www\\.)?files\\.to/get/[0-9]+/[\\w]+" }, flags = { 0 })
public class FilesTo extends PluginForHost {

    public FilesTo(PluginWrapper wrapper) {
        super(wrapper);
    }

    public String getAGBLink() {
        return "http://www.files.to/content/aup";
    }

    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException {
        try {
            br.setCookie("http://files.to/", "lang", "de");
            br.getPage(downloadLink.getDownloadURL());
            if (!br.containsHTML("(Die angeforderte Datei konnte nicht gefunden werden|>Dieser Downloadlink ist ungültig)")) {
                downloadLink.setName(Encoding.htmlDecode(br.getRegex("<p>Name: <span id=\"downloadname\">(.*?)</span></p>").getMatch(0)));
                downloadLink.setDownloadSize(SizeFormatter.getSize(br.getRegex("<p>Gr&ouml;&szlig;e: (.*? (KB|MB|B))</p>").getMatch(0)));
                return AvailableStatus.TRUE;
            }
        } catch (Exception e) {
            logger.log(java.util.logging.Level.SEVERE, "Exception occurred", e);
        }
        throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
    }

    public void handleFree(DownloadLink downloadLink) throws Exception {
        this.requestFileInformation(downloadLink);
        br.getPage(downloadLink.getDownloadURL());
        String captchaAddress = br.getRegex("<img src=\"(http://www\\.files\\.to/captcha_[\\d]+\\.jpg\\?)").getMatch(0);

        String code = getCaptchaCode(captchaAddress, downloadLink);

        Form captchaForm = br.getForm(0);
        captchaForm.put("txt_ccode", code);
        br.submitForm(captchaForm);

        if (br.containsHTML("Der eingegebene code ist falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String url = br.getRegex("<form id=\"dlform\".*?action\\=\"(http://.*?files\\.to/.*?)\">").getMatch(0);
        jd.plugins.BrowserAdapter.openDownload(br, downloadLink, url, true, 1).startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    public void reset() {
    }

    public void resetDownloadlink(DownloadLink link) {
    }

    public void resetPluginGlobals() {
    }

}