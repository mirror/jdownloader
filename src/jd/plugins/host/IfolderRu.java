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
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.JDLocale;

public class IfolderRu extends PluginForHost {

    public IfolderRu(PluginWrapper wrapper) {
        super(wrapper);
    }

    //@Override
    public String getAGBLink() {
        return ("http://ifolder.ru/agreement");
    }

    //@Override
    public boolean getFileInformation(DownloadLink downloadLink) throws PluginException, IOException, InterruptedException {
        this.setBrowserExclusive();
        br.getPage(downloadLink.getDownloadURL());

        if (br.containsHTML("<p>Файл номер <b>\\d+</b> удален !!!</p>") || br.containsHTML("<p>Файл номер <b>\\d+</b> не найден !!!</p>")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);

        String filename = br.getRegex("Название:\\s+<b>(.*?)</b>").getMatch(0);
        String filesize = br.getRegex("Размер:\\s+<b>(.*?)</b>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        downloadLink.setName(filename);
        downloadLink.setDownloadSize(Regex.getSize(filesize.replace("Мб", "Mb").replace("кб", "Kb")));
        return true;
    }

    //@Override
    public void handleFree(DownloadLink downloadLink) throws Exception {
        boolean do_download = false;
        getFileInformation(downloadLink);
        br.setFollowRedirects(true);

        String watchAd = br.getRegex("http://ints\\.ifolder\\.ru/ints/\\?(.*?)\"").getMatch(0);
        if (watchAd != null) {
            downloadLink.getLinkStatus().setStatusText(JDLocale.L("plugins.hoster.ifolderru.errors.ticketwait", "Waiting for ticket"));
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
            URLConnectionAdapter con = br.openGetConnection(captchaurl);
            File file = this.getLocalCaptchaFile(this);
            Browser.download(file, con);
            String captchaCode = getCaptchaCode(file, downloadLink);
            captchaForm.put("confirmed_number", captchaCode);

            br.submitForm(captchaForm);

            String directLink = br.getRegex("Ссылка для скачивания файла:<br><br><a href=\"(.+?)\"").getMatch(0);
            if (directLink != null) {
                dl = br.openDownload(downloadLink, directLink);
                dl.setResume(true);
                do_download = true;
                break;
            }
        }
        if (do_download) {
            dl.startDownload();
        } else
            throw new PluginException(LinkStatus.ERROR_CAPTCHA);
    }

    //@Override
    public void reset() {
    }

    //@Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

    //@Override
    public int getMaxSimultanFreeDownloadNum() {
        /* Tested up to 10 parallel downloads */
        return 10;
    }

    //@Override
    public void reset_downloadlink(DownloadLink link) {
    }

}
