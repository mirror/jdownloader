//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protectbox.in" }, urls = { "http://[\\w\\.]*?protectbox\\.in/.*" }, flags = { 0 })
public class PrtctBxn extends PluginForDecrypt {

    public PrtctBxn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        FilePackage fp = FilePackage.getInstance();
        br.getPage(parameter);
        if (br.containsHTML("includes/captcha/")) {
            for (int i = 0; i <= 5; i++) {
                Form captchaform = br.getForm(0);
                if (captchaform == null) throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFEKT);
                File file = this.getLocalCaptchaFile();
                Browser.download(file, br.cloneBrowser().openGetConnection("http://www.protectbox.in/includes/captcha/imagecreate.php"));
                int[] p = new jd.captcha.specials.GmdMscCm(file).getResult();
                if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
                captchaform.put("button.x", p[0] + "");
                captchaform.put("button.y", p[1] + "");
                br.submitForm(captchaform);
                if (!br.containsHTML("/includes/captcha/")) break;
            }
        }
        /* Password handling */
        String pass = br.getRegex("<b>Passwort:</b>.*?</td>.*?<td>(.*?)</td>").getMatch(0).trim();
        String fpName = br.getRegex("<b>Ordnername:</b>.*?</td>.*?<td>(.*?)</td>").getMatch(0).trim();
        fp.setName(fpName);
        String[] links = br.getRegex("\"(out\\.php\\?id=.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String link : links) {
            link = "http://www.protectbox.in/" + link;
            br.getPage(link);
            String finallink = br.getRedirectLocation();
            if (finallink == null) throw new DecrypterException(DecrypterException.CAPTCHA);
            DownloadLink dl_link = createDownloadlink(finallink);
            if (pass != null && pass.length() != 0) {
                dl_link.addSourcePluginPassword(pass);
            }
            decryptedLinks.add(dl_link);
            progress.increase(1);
        }
        fp.addLinks(decryptedLinks);
        return decryptedLinks;
    }

    // @Override

}
