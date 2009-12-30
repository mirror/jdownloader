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
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "crypted.biz" }, urls = { "http://[\\w\\.]*?crypted\\.biz/(folder\\.php\\?action=show\\&folder_id=|dl/|file/)[0-9a-z]+" }, flags = { 0 })
public class CryptdBz extends PluginForDecrypt {

    public CryptdBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> linksList = new ArrayList<String>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("<b>Password:</b>")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return null;
        }

        /* Single links handling */
        if (parameter.contains(".biz/file")) {
            String b64 = br.getRegex("var versch = '(.*?)';").getMatch(0);
            if (b64 == null) return null;
            b64 = Encoding.Base64Decode(b64);
            String finallink = new Regex(b64, "src=\"(.*?)\"").getMatch(0);
            if (finallink == null) return null;
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            return decryptedLinks;
        }

        /* File package handling */
        if (br.containsHTML("name=\"captcha")) {
            for (int i = 0; i <= 1; i++) {
                Form captchaForm = br.getFormbyKey("captcha");
                String code = br.getRegex("name=\"captcha_2\" value=\"(.*?)\"").getMatch(0);
                if (captchaForm == null || code == null) return null;
                captchaForm.put("captcha", code);
                br.submitForm(captchaForm);
                if (br.containsHTML("The captcha-code you have entered is wrong")) continue;
                break;
            }
        }
        if (br.containsHTML("The captcha-code you have entered is wrong")) throw new DecrypterException(DecrypterException.CAPTCHA);
        System.out.println("BROWSER: " + br.toString());
        String[] pageLinks = br.getRegex("<a href=\"(http://crypted\\.biz/folder\\.php\\?action=show.+?)\">[0-9]+</a>&nbsp;&nbsp;").getColumn(0);
        for (int i = 0; i < pageLinks.length; i++) {
            br.getPage(pageLinks[i].replace("&amp;", "&"));
            String[] links = br.getRegex("window\\.open\\('(.*?)'\\)").getColumn(0);
            if (links == null || links.length == 0) return null;
            for (String link : links) {
                linksList.add(link);
            }
        }
        progress.setRange(linksList.size());
        for (String link : linksList) {
            br.getPage(link);
            String b64 = br.getRegex("var versch = '(.*?)';").getMatch(0);
            if (b64 == null) return null;
            b64 = Encoding.Base64Decode(b64);
            String finallink = new Regex(b64, "src=\"(.*?)\"").getMatch(0);
            if (finallink == null) return null;
            DownloadLink dl = createDownloadlink(finallink);
            decryptedLinks.add(dl);
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
