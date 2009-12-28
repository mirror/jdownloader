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
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "bomb-mp3.com" }, urls = { "http://[\\w\\.]*?bomb-mp3\\.com/download\\.php\\?mp3_id=[0-9]+&title=[a-zA-Z+]" }, flags = { 0 })
public class BMp3Cm extends PluginForDecrypt {

    public BMp3Cm(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        /* Error handling */
        if (br.containsHTML("was removed due to broken link or respective copyright owner")) {
            logger.warning("Wrong link");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        /* Decrypt part */
        for (int i = 0; i <= 1; i++) {
            Form captchaForm = br.getFormByKey("captcha");
            if (captchaForm == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String captchalink = null;
            if (br.containsHTML("image.php")) {
                captchalink = "http://www.bomb-mp3.com/image.php";
            }
            if (captchalink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            String code = getCaptchaCode(captchalink, param);
            captchaForm.put("captcha", code);
            br.submitForm(captchaForm);
            if (br.containsHTML(">Wrong code<")) continue;
            break;
        }
        if (br.containsHTML(">Wrong code<")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String finallink = br.getRegex("size:.*?[0-9]+px;\"><a href=\"((http://|ftp://).*?)\"").getMatch(0);
        if (finallink == null) {
            finallink = br.getRegex("file=((http://|ftp://).*?)&amp;").getMatch(0);
        }
        if (finallink == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        String fpname = br.getRegex("Song name:.*?<strong>(.*?)</strong>").getMatch(0).trim();
        if (fpname == null) {
            fpname = br.getRegex("<title>(.*?)</title>").getMatch(0).trim();
        }
        if (fpname != null) {
            fp.setName(fpname);
        }
        finallink = "directhttp://" + finallink;
        decryptedLinks.add(createDownloadlink(finallink));
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

}
