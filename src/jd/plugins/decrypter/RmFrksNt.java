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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rom-freaks.net" }, urls = { "http://[\\w\\.]*?rom-freaks\\.net/(download-[0-9]+-file-.+|link-[0-9]-[0-9]+-file-.+|.+desc-name.+)\\.html" }, flags = { 0 })
public class RmFrksNt extends PluginForDecrypt {

    public RmFrksNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        parameter = parameter.replaceAll("(/download-|/link-1-)", "/download-");
        br.getPage(parameter);
        if (parameter.contains("desc-name")) {
            String[] thelinks = br.getRegex("\"(link-[0-9]+-[0-9]+-file-.*?\\.html)\"").getColumn(0);
            if (thelinks == null || thelinks.length == 0) return null;
            for (String link : thelinks) {
                decryptedLinks.add(createDownloadlink("http://www.rom-freaks.net/" + link));
            }
        } else {
            String fpName = br.getRegex("<title>Download - NDS ROMs \\- \\d+(.*?)</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("<td width=\"100%\" class=\"aligncenter\" colspan=\"2\">[\t\n\r ]+\\d+ \\- (.*?)</td>").getMatch(0);
            String fastWay = br.getRegex("<p align=\"center\"><b><a href=\"(.*?)\"").getMatch(0);
            if (fastWay == null) fastWay = br.getRegex("<p align=\"center\"><b><a href=\"(.*?)\"").getMatch(0);
            if (fastWay != null) {
                br.getPage("http://www.rom-freaks.net/" + fastWay);
                String finallink = br.getRegex("http-equiv=\"refresh\" content=\"\\d+ url=(http://.*?)\">").getMatch(0);
                if (finallink == null) {
                    logger.warning("finallink is null, tried to go the fastWay, link: " + parameter);
                    return null;
                }
                decryptedLinks.add(createDownloadlink(finallink));
            } else {
                String dlink = br.getRegex("\"(gotodownload\\-\\d+\\.html)\"").getMatch(0);
                if (dlink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                dlink = "http://www.rom-freaks.net/" + dlink;
                br.getPage(dlink);
                /* captcha handling */
                String captchaurl = null;
                if (br.containsHTML("/captcha/go")) {
                    captchaurl = "http://www.rom-freaks.net/captcha/go.php";
                }
                if (captchaurl == null) return null;
                Form captchaForm = br.getFormbyProperty("name", "form1");
                if (captchaForm == null) return null;
                String downlink = null;
                for (int i = 0; i <= 3; i++) {
                    File file = this.getLocalCaptchaFile();
                    Browser.download(file, br.cloneBrowser().openGetConnection("http://www.rom-freaks.net/captcha/go.php"));
                    String code = getCaptchaCode(file, param);
                    if (code == null) continue;
                    String[] codep = code.split(":");
                    Point p = new Point(Integer.parseInt(codep[0]), Integer.parseInt(codep[1]));
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
                        if (fpName != null) {
                            FilePackage fp = FilePackage.getInstance();
                            fp.setName(fpName.trim());
                            fp.addLinks(decryptedLinks);
                        }
                        return decryptedLinks;

                    }
                    if (downlink != null) break;
                }
                if (downlink == null && br.containsHTML("/captcha/go")) throw new DecrypterException(DecrypterException.CAPTCHA);
                if (downlink == null) return null;
                br.getPage(downlink);
                // Last change was here
                String gotu = br.getRegex("\"(http://www\\.rom-freaks\\.net/got.*?_[a-zA-Z0-9]+.*?\\.html)\"").getMatch(0);
                if (gotu == null && br.containsHTML("http-equiv=\"refresh\"")) {
                    logger.warning("Found one non-working link, link = " + dlink);
                }
                if (gotu == null) return null;
                br.getPage(gotu);
                String frameto = br.getRegex("\"(http://www.rom-freaks\\.net/frameto-.*?\\.html)\"").getMatch(0);
                if (frameto == null) return null;
                br.getPage(frameto);
                String[] links0 = br.getRegex("<iframe src=\"(.*?)\"").getColumn(0);
                if (links0 == null || links0.length == 0) {
                    logger.warning("links0 equals null");
                    logger.warning("This link failed: " + parameter);
                    logger.warning(br.toString());
                    return null;
                }
                progress.setRange(links0.length);
                for (String link : links0) {
                    decryptedLinks.add(createDownloadlink(link));
                    progress.increase(1);
                }
            }
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;

    }
}