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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "game-freaks.net" }, urls = { "http://[\\w\\.]*?game-freaks\\.net/download/[A-Z_]+/.*?d+\\.html" }, flags = { 0 })
public class GmFrksNt extends PluginForDecrypt {

    public GmFrksNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("target=\"_blank\"><B>(.*?)</B>").getMatch(0);
        if (fpName == null) {
            fpName = br.getRegex("<H1><A HREF=\"/\\?id=\\d+\"><B>(.*?)</B></A></H1>").getMatch(0);
            if (fpName == null) {
                fpName = br.getRegex("<title>GAME-FREAKs\\.NET - (.*?)</title>").getMatch(0);
            }
        }
        if (br.containsHTML(">Es existiert kein Eintrag mit der ID")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        boolean failed = true;
        for (int i = 0; i <= 3; i++) {
            Form ppost = br.getFormbyProperty("name", "download_form");
            String captchaUrl = br.getRegex("\"(/gfx/secure/index\\.php\\?captcha=[a-z0-9]+)\"").getMatch(0);
            if (ppost == null || captchaUrl == null) return null;
            captchaUrl = "http://game-freaks.net" + captchaUrl;
            String code = getCaptchaCode("ucms", captchaUrl, param);
            ppost.put("code", code);
            br.submitForm(ppost);
            if (br.containsHTML("Der Sichheitscode wurde falsch eingeben")) {
                br.getPage(parameter);
                continue;
            }
            failed = false;
            break;
        }
        if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        String[] links = br.getRegex("<FORM ACTION=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
