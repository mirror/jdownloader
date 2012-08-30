//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.plugins.decrypter;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "drei.bz" }, urls = { "http://(www\\.)?(xxx\\.)?drei\\.bz/(index\\.php)?(\\?id|\\?a=Download\\&dlid)=\\d+" }, flags = { 0 })
public class ThreeBz extends PluginForDecrypt {

    public ThreeBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CAPTCHATEXT = "captcha/imagecreate\\.php";
    private String              correctedBR = "";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String secondLink = null;
        if (parameter.matches("http://(www\\.)?drei\\.bz/(index\\.php)?\\?a=Download&dlid=\\d+")) {
            secondLink = "http://drei.bz/?a=Download&dlid=" + new Regex(parameter, "dlid=(\\d+)").getMatch(0);
        } else if (parameter.matches("http://(www\\.)?(xxx\\.)?drei\\.bz/(index\\.php)?\\?id=\\d+")) {
            secondLink = "http://drei.bz/?a=Download&dlid=" + new Regex(parameter, "drei\\.bz/\\?id=(\\d+)").getMatch(0);
        }
        if (secondLink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        br.getPage(secondLink);
        correctBR();
        final String fpName = new Regex(correctedBR, "div class=\"contenthead_inner\">Download von ([^<>]+)</div>").getMatch(0);
        if (!br.containsHTML(CAPTCHATEXT)) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        boolean failed = true;
        if (correctedBR.contains(CAPTCHATEXT)) {
            for (int i = 0; i <= 3; i++) {
                File file = this.getLocalCaptchaFile();
                Browser.download(file, br.cloneBrowser().openGetConnection("http://drei.bz/captcha/imagecreate.php"));
                Point p = UserIO.getInstance().requestClickPositionDialog(file, "drei.bz", "Click on open Circle");
                /* anticaptcha does not work good enough */
                // int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                if (p == null) continue;
                br.postPage(secondLink, "button.x=" + p.x + "&button.y=" + p.y);
                correctBR();
                if (correctedBR.contains(CAPTCHATEXT) || correctedBR.contains(">Du hast den Captach falsch eingegeben")) continue;
                failed = false;
                break;
            }
            if (failed) throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        String[] links = new Regex(correctedBR, "window\\.open\\(\\'(.*?)\\'").getColumn(0);
        if (links == null || links.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /** Remove HTML code which could break the plugin */
    public void correctBR() throws NumberFormatException, PluginException {
        correctedBR = br.toString();
        ArrayList<String> someStuff = new ArrayList<String>();
        ArrayList<String> regexStuff = new ArrayList<String>();
        regexStuff.add("<\\!(\\-\\-.*?\\-\\-)>");
        for (String aRegex : regexStuff) {
            String lolz[] = br.getRegex(aRegex).getColumn(0);
            if (lolz != null) {
                for (String dingdang : lolz) {
                    someStuff.add(dingdang);
                }
            }
        }
        for (String fun : someStuff) {
            correctedBR = correctedBR.replace(fun, "");
        }
    }
}
