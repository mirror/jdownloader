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

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "megashare.com" }, urls = { "http://[\\w\\.]*?megashare\\.com/[0-9]+" }, flags = { 0 })
public class MegaShareCom extends PluginForHost {

    public MegaShareCom(PluginWrapper wrapper) {
        super(wrapper);
        this.setStartIntervall(2000l);
    }

    // @Override
    public String getAGBLink() {
        return "http://www.megashare.com/tos.php";
    }

    // @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws Exception {
        this.setBrowserExclusive();
        br.setFollowRedirects(true);
        br.setDebug(true);
        br.getPage(downloadLink.getDownloadURL());
        if (br.containsHTML("Not Found")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        Form submit = br.getForm(0);
        submit.setPreferredSubmit(2);
        br.submitForm(submit);
        if (br.containsHTML("This File has been DELETED")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        return AvailableStatus.TRUE;
    }

    // @Override
    /*
     * public String getVersion() { return getVersion("$Revision$"); }
     */

    // @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        requestFileInformation(downloadLink);
        Form form = br.getForm(0);

        File captchaFile = getLocalCaptchaFile();
        int i = 15;
        while (i-- > 0) {
            try {
                Browser.download(captchaFile, br.cloneBrowser().openGetConnection("http://megashare.com/security.php?" + Math.random()));
            } catch (Exception e) {
                throw new PluginException(LinkStatus.ERROR_CAPTCHA);
            }

            String hash = JDHash.getMD5(captchaFile);
            // Seems to be a captchaerror (captcahs without any letters)
            if (hash.equals("eb92a5ddf69784ee2de24bca0c6299d4")) {
                continue;
            } else {
                break;
            }
        }
        String captchaCode = getCaptchaCode(captchaFile, downloadLink);

        form.remove("accel");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        form.remove("yesss");
        
        form.put("captcha_code", captchaCode);
        form.put("yesss", "Download");
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, form, true, 1);
        if(!dl.getConnection().isContentDisposition()){
            String page = br.loadConnection(dl.getConnection());
            if(page.contains("Invalid Captcha Value")){
                throw new PluginException(LinkStatus.ERROR_CAPTCHA); 
            }
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
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
