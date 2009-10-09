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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.DownloadLink.AvailableStatus;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "edisk.cz" }, urls = { "http://[\\w\\.]*?edisk\\.cz/stahni/[0-9]+/.+\\.html" }, flags = { 0 })
public class EdiskCz extends PluginForHost {

    public EdiskCz(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://www.edisk.cz/kontakt";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("Tento soubor již neexistuje z následujích")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = Encoding.htmlDecode(br.getRegex("Stažení souboru &quot;(.*?)&quot").getMatch(0));
        String filesize = br.getRegex("Velikost souboru: (.*?)<br").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        link.setName(filename);
        link.setDownloadSize(Regex.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);
        br.setDebug(true);
        String posturl = br.getRegex("Naposledy stáhnut.*?action=\"(http://.*?)\"").getMatch(0);
        Form captchaForm = br.getForm(0);
        if (captchaForm == null || posturl == null) throw new PluginException(LinkStatus.ERROR_FATAL);
        captchaForm.setAction(posturl);
        String code = "";
        for (int i = 0; i < 5; i++) {
            if (!br.containsHTML("Opište text z obrázku")) break;
            String captchaurl0 = br.getRegex("captchaImgWrapper.*?src=\"(.*?)\"").getMatch(0);
            if (captchaurl0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            String captchaurl = "http://www.edisk.cz" + captchaurl0;
            code = getCaptchaCode(captchaurl, downloadLink);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            captchaForm.put("captchaCode", code);
            br.submitForm(captchaForm);
        }

        if (br.containsHTML("Opište text z obrázku")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String[] countDownInfo = br.getRegex("countDown\\(.*?'(.*?)'.*?,.*?'(.*?)'.*?,.*?'(.*?)'.*?,(.*?)").getRow(0);
        // you dont have to wait
        // (25001l, downloadLink);
        // make sure the form have cookies
        Form dlform = br.getForm(0);
        dlform.getInputFields().clear();
        dlform.setAction("/x-download/" + countDownInfo[1]);
        dlform.setMethod(Form.MethodType.POST);
        dlform.put("captchaCode", code);
        dlform.put("type", countDownInfo[0]);
        // System.out.println(dlform);
        br.submitForm(dlform);
        // String dllink =
        // br.getRegex("captchaImgWrapper.*?src=\"(.*?)\"").getMatch(0);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, br.toString().trim(), true, 0);
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
