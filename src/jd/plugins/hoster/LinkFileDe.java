//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
//    Copyright (C) 2009  zdolny jupik@10g.pl
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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

/**
 * @author zdolny fixes by djuzi
 */
@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "linkfile.de" }, urls = { "http://[\\w\\.]*?linkfile.de/download-[\\w]+\\.php" }, flags = { 0 })
public class LinkFileDe extends PluginForHost {

    public LinkFileDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.linkfile.de/disclaimer.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws IOException, InterruptedException, PluginException {
        String url = downloadLink.getDownloadURL();
        br.getPage(url);
        if (br.containsHTML("Diese Datei ist nicht mehr verf")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String size = br.getRegex("<tbody><tr><td[^>]*>\\s+(.*?)\\s+&nbsp;").getMatch(0);
        if (size == null) size = br.getRegex("&nbsp; \\((.*?)\\)").getMatch(0);
        String name = br.getRegex("Datei: <b>(.*?)</b>").getMatch(0);
        if (size == null || name == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        downloadLink.setDownloadSize(Regex.getSize(size));
        downloadLink.setName(name.trim());
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);

        String code = getCaptchaCode(br.getBaseURL() + "captcha.php", downloadLink);

        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        captchaForm.put("captcha", code);

        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, captchaForm, true, 1);
        if (!dl.getConnection().isContentDisposition()) {
            dl.getConnection().disconnect();
            throw new PluginException(LinkStatus.ERROR_CAPTCHA, JDL.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
        }
        dl.setResume(true);
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

}
