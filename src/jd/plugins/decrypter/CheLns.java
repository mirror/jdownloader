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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 23948 $", interfaceVersion = 2, names = { "cacheliens.com" }, urls = { "http://(www\\.)?cacheliens\\.com/(mylink|linkcheck|linkidwoc)\\.php\\?linkid=[a-z0-9]+" }, flags = { 0 })
public class CheLns extends PluginForDecrypt {

    public CheLns(PluginWrapper wrapper) {
        super(wrapper);
    }

    // All similar: IleProtectCom, ExtremeProtectCom, TopProtectNet
    /* DecrypterScript_linkid=_linkcheck.php */
    private final String RECAPTCHATEXT  = "api\\.recaptcha\\.net";
    private final String RECAPTCHATEXT2 = "google\\.com/recaptcha/api/challenge";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString().replaceAll("linkidwoc|mylink", "linkcheck");
        br.getPage(parameter);
        Browser obr = br.cloneBrowser();
        String fpName = null;
        boolean noCaptcha = true;
        for (int i = 0; i != 4; i++) {
            Form f1 = br.getFormbyProperty("name", "linkprotect");
            if (f1 == null) {
                logger.warning("Plugin Defect: " + parameter);
                return null;
            }
            PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
            jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((jd.plugins.hoster.DirectHTTP) recplug).getReCaptcha(br);
            rc.parse();
            rc.load();
            f1.put("recaptcha_challenge_field", rc.getChallenge());
            // try without captcha
            if (noCaptcha) {
                f1.put("recaptcha_response_field", "");
            } else {
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode(cf, param);
                f1.put("recaptcha_response_field", Encoding.urlEncode(c));
            }
            br.submitForm(f1);
            if (br.containsHTML("(The security code is <font color=\\'red\\'>incorrect</font>|The CAPTCHA wasn\\'t entered correctly)")) {
                if (i == 3)
                    throw new DecrypterException(DecrypterException.CAPTCHA);
                else if (noCaptcha && i == 1) noCaptcha = false;
                br = obr.cloneBrowser();
                continue;
            }
            Form f2 = br.getFormbyProperty("name", "linkprotect");
            if (f2 == null) {
                logger.warning("Plugin Defect: " + parameter);
                return null;
            }
            br.submitForm(f2);

            if (br.containsHTML("<a href= target=_blank></a>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            fpName = br.getRegex("Titre:.*?</td>.*?<td[^>]+>(.*?)</td>").getMatch(0);
            String[] links = br.getRegex("target=_blank>(.*?)</a>").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
            if (!decryptedLinks.isEmpty())
                break;
            else if (i == 3) return null;
        }
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}