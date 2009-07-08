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

import java.io.IOException;

import jd.PluginWrapper;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.HostPlugin;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.utils.locale.JDL;

@HostPlugin(revision="$Revision", interfaceVersion=1, names = { "ifolder.ru"}, urls ={ "http://[\\-\\w\\.]*?ifolder\\.ru/\\d+"}, flags = {0})
public class IfolderRu extends PluginForHost {

    public IfolderRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return ("http://ifolder.ru/agreement");
    }

    @Override
    public String getCoder() {
        return "Void";
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink downloadLink) throws PluginException, IOException, InterruptedException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("<p>Файл номер <b>\\d+</b> удален !!!</p>") || br.containsHTML("<p>Файл номер <b>\\d+</b> не найден !!!</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("Название:\\s+<b>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("Размер:\\s+<b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replace("Мб", "Mb").replace("кб", "Kb")));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        boolean do_download = false;
        requestFileInformation(downloadLink);
        br.setFollowRedirects(true);

        String watchAd = br.getRegex("http://ints\\.ifolder\\.ru/ints/\\?(.*?)\"").getMatch(0);
        if (watchAd != null) {
            downloadLink.getLinkStatus().setStatusText(JDL.L("plugins.hoster.ifolderru.errors.ticketwait", "Waiting for ticket"));
            watchAd = "http://ints.ifolder.ru/ints/?".concat(watchAd);
            br.getPage(watchAd);
            watchAd = br.getRegex("<font size=\"\\+1\"><a href=(.*?)>").getMatch(0);
            if (watchAd == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.getPage(watchAd);
            watchAd = br.getRegex("\"f_top\" src=\"(.*?)\"").getMatch(0);
            if (watchAd == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            br.getPage(watchAd);
            /* Tickettime */
            String ticketTimeS = br.getRegex("delay = (\\d+)").getMatch(0);
            if (ticketTimeS == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            int ticketTime = Integer.parseInt(ticketTimeS) * 1000;
            this.sleep(ticketTime, downloadLink);
            br.getPage(watchAd);
        }
        for (int retry = 1; retry <= 5; retry++) {
            Form captchaForm = br.getFormbyProperty("name", "form1");
            String captchaurl = br.getRegex("(/random/images/.*?)\"").getMatch(0);
            String tag = br.getRegex("tag.value = \"(.*?)\"").getMatch(0);
            String secret = br.getRegex("var\\s+s=\\s+'(.*?)';").getMatch(0).substring(2);
            if (captchaForm == null || captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            if (tag != null && secret != null) {
                captchaForm.put("interstitials_session", tag);
                InputField nv = new InputField(secret, "1");
                captchaForm.addInputField(nv);
            } else
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
            captchaForm.setAction(br.getURL());

            /* Captcha */
            String captchaCode = getCaptchaCode(captchaurl, downloadLink);
            captchaForm.put("confirmed_number", captchaCode);

            br.submitForm(captchaForm);

            String directLink = br.getRegex("id=\"download_file_href\"\\s+href=\"(.*?)\"").getMatch(0);
            if (directLink != null) {
                dl = br.openDownload(downloadLink, directLink, true, -2);
                do_download = true;
                break;
            }
        }
        if (!do_download) {
            throw new PluginException(LinkStatus.ERROR_FATAL, JDL.L("downloadlink.status.error.captcha_wrong", "Captcha wrong"));
        } else
            dl.startDownload();
    }

    @Override
    public void reset() {
    }

   
    /* public String getVersion() {
        return getVersion("$Revision$");
    } */

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        /* Tested up to 10 parallel downloads */
        return 10;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

}
