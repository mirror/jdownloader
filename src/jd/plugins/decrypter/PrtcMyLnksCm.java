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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-my-links.com" }, urls = { "http://[\\w\\.]*?protect-my-links\\.com/\\?id=[a-z0-9]+" }, flags = { 0 })
public class PrtcMyLnksCm extends PluginForDecrypt {

    public PrtcMyLnksCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        boolean decrypterBroken = true;
        if (decrypterBroken) return null;

        /* Error handling */
        if (br.containsHTML("This data has been removed by the owner")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* File package handling */
        for (int i = 0; i <= 5; i++) {
            Form captchaForm = br.getForm(1);
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String passCode = null;
            String captchalink0 = br.getRegex("src=\"(mUSystem.*?)\"").getMatch(0);
            String captchalink = "http://protect-my-links.com/" + captchalink0;
            if (captchalink0.contains("null")) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchalink, param);
            captchaForm.put("captcha", code);

            if (br.containsHTML("Password :")) {
                passCode = Plugin.getUserInput("Password?", param);
                captchaForm.put("passwd", passCode);
            }
            br.submitForm(captchaForm);
            if (br.containsHTML("Captcha is not valid") || br.containsHTML("Password is not valid")) continue;
            break;
        }
        if (br.containsHTML("Captcha is not valid")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        String fpName = br.getRegex("h1 class=\"pmclass\">(.*?)</h1></td>").getMatch(0).trim();
        fp.setName(fpName);
        String[] links = br.getRegex("><a href='(/\\?p=.*?)'").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String psp : links) {
            br.getPage("http://protect-my-links.com" + psp);
            String c = br.getRegex("javascript>c=\"(.*?)\";").getMatch(0);
            String d = "";
            for (int i = 0; i < c.length(); i++) {
                if (i % 3 == 0) {
                    d += "%";
                } else {
                    d += c.charAt(i);
                }
            }
            d = Encoding.htmlDecode(d);
            // Java script continues here ;)
            String finallink = "";
            if (finallink == null) return null;
            decryptedLinks.add(createDownloadlink(finallink));
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
