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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "dl-protect.com" }, urls = { "http://(www\\.)?dl\\-protect\\.com/(en/)?[A-Z0-9]+" }, flags = { 0 })
public class DlPrtcCom extends PluginForDecrypt {

    public DlPrtcCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String CAPTCHATEXT    = ">Copy the code";
    private static final String CAPTCHAFAILED  = ">The security code is incorrect";
    private static final String PASSWORDTEXT   = ">Password :";
    private static final String PASSWORDFAILED = ">The password is incorrect";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        // Prefer English language
        String parameter = param.toString().replaceAll("dl\\-protect\\.com/(en|fr)/", "dl-protect.com/").replace("dl-protect.com/", "dl-protect.com/en/");
        br.getPage(parameter);
        if (br.containsHTML(PASSWORDTEXT) || br.containsHTML(CAPTCHATEXT)) {
            for (int i = 0; i <= 5; i++) {
                Form importantForm = getForm();
                if (importantForm == null) {
                    logger.warning("Decrypter broken for link: " + parameter);
                    return null;
                }
                if (br.containsHTML(PASSWORDTEXT)) {
                    importantForm.put("pwd", getUserInput(null, param));
                }
                if (br.containsHTML(CAPTCHATEXT)) {
                    String captchaLink = getCaptchaLink();
                    captchaLink = "http://www.dl-protect.com" + captchaLink;
                    String code = getCaptchaCode(captchaLink, param);
                    importantForm.put("secure", code);
                }
                br.submitForm(importantForm);
                if (getCaptchaLink() != null || br.containsHTML(CAPTCHAFAILED) || br.containsHTML(PASSWORDFAILED) || br.containsHTML(PASSWORDTEXT)) continue;
                break;
            }
            if (br.containsHTML(CAPTCHAFAILED) && br.containsHTML(CAPTCHAFAILED)) throw new DecrypterException("Wrong captcha and password entered!");
            if (getCaptchaLink() != null || br.containsHTML(CAPTCHAFAILED) || br.containsHTML(CAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
            if (br.containsHTML(PASSWORDTEXT) || br.containsHTML(PASSWORDFAILED)) throw new DecrypterException(DecrypterException.PASSWORD);
        }
        if (br.containsHTML(">Please click on continue to see the content")) {
            Form continueForm = getForm();
            if (continueForm == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            br.submitForm(continueForm);
        }
        System.out.print(br.toString());
        String linktext = br.getRegex("class=\"divlink link\" id=\"slinks\"><a(.*?)valign=\"top\" align=\"right\" width=\"500px\" height=\"280px\"><pre style=\"text\\-align:center\">").getMatch(0);
        if (linktext == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        String[] links = new Regex(linktext, "href=\"([^\"\\']+)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        for (String dl : links)
            decryptedLinks.add(createDownloadlink(dl));
        return decryptedLinks;
    }

    private String getCaptchaLink() {
        String captchaLink = br.getRegex("id=\"captcha\" src=\"(/.*?)\"").getMatch(0);
        if (captchaLink == null) captchaLink = br.getRegex("\"(/captcha\\.php\\?uid=[a-z0-9]+)\"").getMatch(0);
        return captchaLink;
    }

    private Form getForm() {
        Form theForm = br.getFormbyProperty("name", "ccerure");
        if (theForm == null) theForm = br.getForm(0);
        return theForm;
    }
}
