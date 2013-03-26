//jDownloader - Downloadmanager
//Copyright (C) 2010  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.hoster;

import java.io.IOException;

import jd.PluginWrapper;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bitoman.ru" }, urls = { "http://(www\\.)?bitoman\\.ru/download/\\d+\\.html" }, flags = { 2 })
public class BitoManRu extends PluginForHost {

    private static final String WAIT1 = "WAIT1";

    public BitoManRu(PluginWrapper wrapper) {
        super(wrapper);
        setConfigElements();
    }

    @Override
    public String getAGBLink() {
        return "http://www.bitoman.ru/profile/rules.html";
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 3;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String slowDL = br.getRegex("<div class=\"btnspeed\">Да, попробовать\\!</div>[\t\n\r ]+</a>[\t\n\r ]+<a href=\"(/.*?)\"").getMatch(0);
        if (slowDL == null) slowDL = br.getRegex("\"(/download/slow/\\d+/\\d+\\.html)\"").getMatch(0);
        if (slowDL == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage("http://www.bitoman.ru" + Encoding.htmlDecode(slowDL));
        if (!br.containsHTML("/download/get\\.html\\?fid=")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        final boolean waitReconnecttime = getPluginConfig().getBooleanProperty(WAIT1, true);
        int wait = 60;
        String waittime = br.getRegex("download\\.totalTime = (\\d+);").getMatch(0);
        if (waittime != null) wait = Integer.parseInt(waittime);
        if ((wait > 600 && !waitReconnecttime) || wait > 1800) throw new PluginException(LinkStatus.ERROR_IP_BLOCKED, 60 * 60 * 1000l);
        sleep(wait * 1001l, downloadLink);
        final String captchaPage = "http://www.bitoman.ru/download/get.html?fid=" + new Regex(downloadLink.getDownloadURL(), "bitoman\\.ru/download/(\\d+)\\.html").getMatch(0);
        br.getPage(captchaPage);
        boolean failed = true;
        /** Captcha looks easy but you really need more than 3 tries often */
        for (int i = 0; i <= 7; i++) {
            String captchaLink = br.getRegex("method=\"post\"><img id=\"yw0\" src=\"(/.*?)\"").getMatch(0);
            if (captchaLink == null) captchaLink = br.getRegex("\"(/download/captcha\\.html\\?v=[a-z0-9]+)\"").getMatch(0);
            if (captchaLink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            captchaLink = "http://www.bitoman.ru" + Encoding.htmlDecode(captchaLink);
            String code = getCaptchaCode(captchaLink, downloadLink);
            br.postPage(captchaPage, "DownloadCaptchaForm%5Bcaptcha%5D=" + code + "&yt1=%D0%A1%D0%BA%D0%B0%D1%87%D0%B0%D1%82%D1%8C");
            if (br.containsHTML("(class=\"error\" for=\"DownloadCaptchaForm_captcha\"|>Введите код с картинки:<)")) continue;
            failed = false;
            break;
        }
        if (failed) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String dllink = br.getRegex("onclick=\"clearTimeout\\(timer\\);\" href=\"(http://.*?)\"").getMatch(0);
        if (dllink == null) {
            dllink = br.getRegex("setTimeout\\(\"document\\.location\\.href=\\'(http://.*?)\\'\"").getMatch(0);
            if (dllink == null) {
                dllink = br.getRegex("(\\'|\")(http://FN\\d+\\.bitoman\\.ru/download/file/.*?)(\\'|\")").getMatch(0);
            }
        }
        if (dllink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, dllink, false, 1);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        dl.startDownload();
    }

    // do not add @Override here to keep 0.* compatibility
    public boolean hasCaptcha() {
        return true;
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.containsHTML("(Файл удален из\\-за того, что его никто не качал более 30 дней\\.|>Файл не найден\\. Возможно указан неверный идентификатор|<title>Bitoman\\.ru \\- Ошибка</title>)")) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex(">Скачать:</p>\\&nbsp;\\&nbsp;<p class=\"valueNameSize\" title=\"\">(.*?)</p>").getMatch(0);
        if (filename == null) {
            filename = br.getRegex("class=\"download_kachaem\">Качаем: <b>(.*?)</b></div>").getMatch(0);
            if (filename == null) {
                filename = br.getRegex("<title>Bitoman\\.ru \\- (.*?) Скачать быстро, бесплатно, на выской скорости</title>").getMatch(0);
            }
        }
        String filesize = br.getRegex("<font style=\"font\\-size: 18px; position: relative; top: \\-3px;\">\\((.*?)\\)</font").getMatch(0);
        if (filesize == null) filesize = br.getRegex("class=\"nameFile\">Размер:</p>\\&nbsp;<p class=\"valueNameSize\">(.*?)</p><br>").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        filesize = Encoding.htmlDecode(filesize);
        filesize = filesize.replace("Г", "G");
        filesize = filesize.replace("М", "M");
        filesize = filesize.replaceAll("(к|К)", "k");
        filesize = filesize.replaceAll("(Б|б)", "");
        filesize = filesize + "b";
        link.setName(Encoding.htmlDecode(filename.trim()));
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    /** Captcha reload function might be interesting for the future */
    // private String reloadCaptcha() throws IOException {
    // Browser br2 = br.cloneBrowser();
    // br2.getHeaders().put("X-Requested-With", "XMLHttpRequest");
    // br.getPage("http://www.bitoman.ru/download/captcha.html?refresh=1&_=" +
    // System.currentTimeMillis());
    // String captchaLink = br.toString();
    // if (captchaLink.length() > 500 || !captchaLink.startsWith("/")) return
    // null;
    // return captchaLink;
    // }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    private void setConfigElements() {
        final ConfigEntry cond = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, getPluginConfig(), WAIT1, JDL.L("plugins.hoster.bitomanru.waitInsteadOfReconnect", "Wait and download instead of reconnecting if wait time is under 30 minutes")).setDefaultValue(true);
        getConfig().addEntry(cond);
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}