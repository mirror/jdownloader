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

package jd.plugins.decrypter;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rom-freaks.net" }, urls = { "http://[\\w\\.]*?rom-freaks\\.net/(download-[0-9]+-file-[0-9a-zA-Z-_%\\(\\)\\.]+|link-[0-9]-[0-9]+-file-[0-9a-zA-Z-_%\\.]+)\\.html" }, flags = { 0 })
public class RmFrksNt extends PluginForDecrypt {

    public RmFrksNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(true);
        parameter = parameter.replaceAll("(/download-|/link-1-)", "/download-");
        br.getPage(parameter);
        String dlink = br.getRegex("href=\"(link-[0-9]-.*?\\.html)\"").getMatch(0);
        if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        dlink = "http://www.rom-freaks.net/" + dlink;
        String fpName = br.getRegex("class=\"aligncenter\" colspan=\"[0-9]\">(.*?)</td>").getMatch(0).trim();
        fp.setName(fpName);
        br.getPage(dlink);
        /* captcha handling */
        String captchaurl = null;
        if (br.containsHTML("/captcha/go")) {
            captchaurl = "http://www.rom-freaks.net/captcha/go.php";
        }
        if (captchaurl == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        Form captchaForm = br.getFormbyProperty("name", "form1");
        if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        String downlink = null;
        for (int i = 0; i <= 3; i++) {
            File file = this.getLocalCaptchaFile();
            Browser.download(file, br.cloneBrowser().openGetConnection("http://www.rom-freaks.net/captcha/go.php"));
            String code = getCaptchaCode(file, param);
            if (code == null) continue;
            String[] codep = code.split(":");
            Point p = new Point(Integer.parseInt(codep[0]), Integer.parseInt(codep[1]));
            if (p == null)continue;
            captchaForm.put("button.x", p.x + "");
            captchaForm.put("button.y", p.y + "");
            br.submitForm(captchaForm);

            downlink = br.getRegex("\"(http://www\\.rom-freaks\\.net/down-.*?\\.html)\"").getMatch(0);
            String[] links = br.getRegex("<FORM ACTION=\"(.*?)\"").getColumn(0);
            if (links != null && links.length != 0 && downlink == null) {

                progress.setRange(links.length);
                for (String link : links) {
                    decryptedLinks.add(createDownloadlink(link));
                    progress.increase(1);
                }
                fp.addLinks(decryptedLinks);
                return decryptedLinks;

            }
            if (downlink != null) break;
        }
        if (downlink == null && br.containsHTML("/captcha/go")) throw new DecrypterException(DecrypterException.CAPTCHA);
        if (downlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(downlink);
        String gotu = br.getRegex("\"(http://www.rom-freaks\\.net/goto.*?\\.html)\"").getMatch(0);
        if (gotu == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(gotu);
        String frameto = br.getRegex("\"(http://www.rom-freaks\\.net/frameto-.*?\\.html)\"").getMatch(0);
        if (frameto == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        br.getPage(frameto);
        String[] links0 = br.getRegex("<iframe src=\"(.*?)\"").getColumn(0);
        if (links0 == null || links0.length == 0) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
        progress.setRange(links0.length);
        for (String link : links0) {
            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }
}