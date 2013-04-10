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
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

//pspzockerscenes first decrypter hehe
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "streamprotect.net" }, urls = { "http://[\\w\\.]*?streamprotect\\.net/f/[a-z|0-9]+" }, flags = { 0 })
public class StrPrtctNet extends PluginForDecrypt {

    public StrPrtctNet(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("Der angeforderte Ordner konnte nicht gefunden werden")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* File package handling */
        if (br.containsHTML("Sicherheitscode") || br.containsHTML("Passwort")) {
            for (int i = 0; i <= 5; i++) {
                Form captchaForm = br.getForm(0);
                if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                if (br.containsHTML("Sicherheitscode")) {
                    String captchalink = br.getRegex("Sicherheitscode.*?img src=\"(.*?)\"").getMatch(0);
                    String code = getCaptchaCode(captchalink, param);
                    captchaForm.put("txtCaptcha", code);
                }
                if (br.containsHTML("Passwort")) {
                    String passCode = null;
                    passCode = getUserInput(null, param);
                    captchaForm.put("txtPassword", passCode);
                }
                br.submitForm(captchaForm);
                if (br.containsHTML("Passwort ist falsch")) {
                    logger.warning("Wrong password!");
                    continue;
                }
                if (br.containsHTML("Sicherheitscode ist falsch")) continue;
                break;
            }
        }
        if (br.containsHTML("Sicherheitscode ist falsch")) throw new PluginException(LinkStatus.ERROR_CAPTCHA);
        if (br.containsHTML("Passwort ist falsch")) {
            logger.warning("Wrong password!");
            throw new DecrypterException(DecrypterException.PASSWORD);
        }
        String cryptframe = br.getRegex("frameborder=.*?<frame src=\"(.*?)\" name").getMatch(0);
        if (cryptframe == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        br.getPage(cryptframe);
        String[] links = br.getRegex("Hits.*?href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            br.getPage(link);
            String clink0 = br.getRegex("topFrame\" frameborder.*?<frame src=\"(.*?)\" name=\"mainFrame").getMatch(0);
            if (clink0 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            br.getPage(clink0);
            Form ajax = br.getForm(0);
            if (ajax == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            ajax.setAction("http://streamprotect.net/ajax/l2ex.php");
            br.submitForm(ajax);
            String b64 = br.getRegex("\\{\"state\":\"ok\",\"data\":\"(.*?)\"\\}").getMatch(0);
            if (b64 == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            b64 = Encoding.Base64Decode(b64);
            DownloadLink dl = createDownloadlink(b64);
            decryptedLinks.add(dl);
            progress.increase(1);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}