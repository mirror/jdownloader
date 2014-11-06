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

import java.io.File;
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
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "r8link.com" }, urls = { "http://(www\\.)?r8link\\.com/[A-Za-z0-9]+" }, flags = { 0 })
public class R8LinkCom extends PluginForDecrypt {

    public R8LinkCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final String reCaptcha = "api\\.recaptcha\\.net|google\\.com/recaptcha/api/";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML(">NOT FOUND<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }
        final int repeat = 4;
        for (int i = 1; i <= repeat; i++) {
            if (br.containsHTML(reCaptcha)) {
                final PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                final jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                final String[] v = br.getRegex("(\\w)\\.name\\s*=\\s*(\"|')(\\w+)\";\\s*\\1\\.value\\s*=\\s*(\"|')([a-f0-9]+)\\4").getRow(0);
                if (v == null) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                Form r = rc.getForm();
                r.put(v[2], v[4]);
                rc.load();
                final File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                final String c = getCaptchaCode("recaptcha", cf, param);
                r.put("recaptcha_challenge_field", rc.getChallenge());
                r.put("recaptcha_response_field", Encoding.urlEncode(c));
                br.submitForm(r);
                if (br.containsHTML(reCaptcha) && i + 1 != repeat) {
                    continue;
                } else if (br.containsHTML(reCaptcha) && i + 1 == repeat) {
                    throw new Exception(DecrypterException.CAPTCHA);
                } else {
                    break;
                }
            }
        }
        final String finallink = br.getRegex("HTTP-EQUIV='Refresh' CONTENT='\\d+;URL=(http://[^<>\"]*?)'>").getMatch(0);
        if (finallink == null) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        decryptedLinks.add(createDownloadlink(finallink));

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return true;
    }

}