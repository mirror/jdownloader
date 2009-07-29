//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.http.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mega.1280.com" }, urls = { "http://[\\w\\.]*?mega\\.1280\\.com/file/[0-9|A-Z]+" }, flags = { 0 })
public class Mega1280Com extends PluginForHost {

    public Mega1280Com(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://mega.1280.com/terms.php";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("upload.php")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("<td class=\"clr03\" colspan=\"3\"><strong>(.*?)</strong></td>").getMatch(0));
        String filesize = br.getRegex("<td class=\"clr07\" width=\"50%\" valign=\"top\">(.*?)</td>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        // Link zum Captcha (kann bei anderen Hostern auch mit ID sein)
        String captchaurl = "http://mega.1280.com/security_code.php";
        String code = getCaptchaCode(captchaurl, downloadLink);
        Form captchaForm = br.getForm(0);
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        // Captcha Usereingabe in die Form einfügen
        captchaForm.put("code_security", code);
        br.submitForm(captchaForm);
        if (br.containsHTML("frm_download")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        
        //setzt den downloadlink zusammen (damit wird die Wartezeit umgangen)
        String dllink0 = br.getRegex("<div id=\"hddomainname\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String dllink1 = br.getRegex("<div id=\"hdfolder\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String dllink2 = br.getRegex("<div id=\"hdcode\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String dllink3 = br.getRegex("<div id=\"hdfilename\" style=\"display:none\">(.*?)</div>").getMatch(0);
        String downloadURL = dllink0 + dllink1 + dllink2 + "/" + dllink3 ;
        dl = br.openDownload(downloadLink, downloadURL, true, 1);
        
        
        dl.startDownload();
    }

    @Override
    public void reset() {
    }

    // @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 20;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
