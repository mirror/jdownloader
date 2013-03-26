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
import jd.config.Property;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.encoding.Encoding;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;

import org.appwork.utils.formatter.SizeFormatter;

@HostPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "zaycev.net" }, urls = { "http://(www\\.)?zaycev\\.net/pages/[0-9]+/[0-9]+\\.shtml" }, flags = { 0 })
public class ZaycevNet extends PluginForHost {

    public ZaycevNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public String getAGBLink() {
        return "http://zaycev.net/";
    }

    private static final String CAPTCHATEXT = "/captcha/";

    @Override
    public AvailableStatus requestFileInformation(DownloadLink link) throws IOException, PluginException {
        this.setBrowserExclusive();
        br.setFollowRedirects(false);
        br.getPage(link.getDownloadURL());
        if (br.getRedirectLocation() != null) throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        String filename = br.getRegex("windowTitle: \\'Добавление комментария ([^<>\"/]*?)\\'").getMatch(0);
        if (filename == null) filename = br.getRegex("<title>([^<>/\"]*?) скачать бесплатно mp3 download скачать без регистрации").getMatch(0);
        String filesize = br.getRegex("class=\"track\\-file\\-info__size\">Размер: ([^<>\"]*?)<meta").getMatch(0);
        if (filename == null || filesize == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        link.setName(Encoding.htmlDecode(filename.trim()) + ".mp3");
        link.setDownloadSize(SizeFormatter.getSize(filesize));
        return AvailableStatus.TRUE;
    }

    @Override
    public void handleFree(DownloadLink downloadLink) throws Exception, PluginException {
        requestFileInformation(downloadLink);
        String finallink = checkDirectLink(downloadLink, "savedlink");
        if (finallink == null) {
            String cryptedlink = br.getRegex("\"(/download\\.php\\?id=\\d+\\&ass=[^<>/\"]*?\\.mp3)\"").getMatch(0);
            if (cryptedlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            cryptedlink = "http://zaycev.net" + cryptedlink;
            br.getPage(cryptedlink);
            finallink = getDllink();
            if (finallink == null) {
                if (br.containsHTML(CAPTCHATEXT)) {
                    for (int i = 0; i <= 5; i++) {
                        // Captcha handling
                        String captchaID = getCaptchaID();
                        if (captchaID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        String code = getCaptchaCode("http://www.zaycev.net/captcha/" + captchaID + "/", downloadLink);
                        String captchapage = cryptedlink + "&captchaId=" + captchaID + "&text_check=" + code + "&ok=%F1%EA%E0%F7%E0%F2%FC";
                        br.getPage(captchapage);
                        if (br.containsHTML(CAPTCHATEXT)) continue;
                        break;
                    }
                    if (br.containsHTML(CAPTCHATEXT)) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
                } else {
                    String code = br.getRegex("<label>Ваш IP</label><span class=\"readonly\">[0-9\\.]+</span></div><input value=\"(.*?)\"").getMatch(0);
                    String captchaID = getCaptchaID();
                    if (code == null || captchaID == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                    String captchapage = cryptedlink + "&captchaId=" + captchaID + "&text_check=" + code + "&ok=%F1%EA%E0%F7%E0%F2%FC";
                    br.getPage(captchapage);
                }
                finallink = getDllink();
            }
        }
        dl = jd.plugins.BrowserAdapter.openDownload(br, downloadLink, finallink, true, 0);
        if (dl.getConnection().getContentType().contains("html")) {
            br.followConnection();
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        downloadLink.setProperty("savedlink", finallink);
        dl.startDownload();
    }

    private String getCaptchaID() {
        String captchaID = br.getRegex("name=\"id\" type=\"hidden\"/><input value=\"(\\d+)\"").getMatch(0);
        if (captchaID == null) captchaID = br.getRegex("\"/captcha/(\\d+)").getMatch(0);
        return captchaID;
    }

    private String checkDirectLink(DownloadLink downloadLink, String property) {
        String dllink = downloadLink.getStringProperty(property);
        if (dllink != null) {
            try {
                Browser br2 = br.cloneBrowser();
                URLConnectionAdapter con = br2.openGetConnection(dllink);
                if (con.getContentType().contains("html") || con.getLongContentLength() == -1) {
                    downloadLink.setProperty(property, Property.NULL);
                    dllink = null;
                }
                con.disconnect();
            } catch (Exception e) {
                downloadLink.setProperty(property, Property.NULL);
                dllink = null;
            }
        }
        return dllink;
    }

    private String getDllink() {
        String finallink = br.getRegex("\\{REFRESH: \\{url: \"(http://dl\\.zaycev\\.net/[^<>\"]*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("то нажмите на эту <a href=\\'(http.*?)\\'").getMatch(0);
            if (finallink == null) finallink = br.getRegex("\"(http://dl\\.zaycev\\.net/[a-z0-9\\-]+/\\d+/\\d+/.*?)\"").getMatch(0);
        }
        return finallink;
    }

    @Override
    public void reset() {
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return -1;
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }

    /* NO OVERRIDE!! We need to stay 0.9*compatible */
    public boolean hasCaptcha(DownloadLink link, jd.plugins.Account acc) {
        return true;
    }
}