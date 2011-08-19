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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.DirectHTTP;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "fullhd.bz" }, urls = { "http://(www\\.)?fullhd\\.bz/(v/.+|\\?do=out\\&iden=[a-z0-9]+)" }, flags = { 0 })
public class FuHdBz extends PluginForDecrypt {

    public FuHdBz(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String RECAPTCHATEXT = "(api\\.recaptcha\\.net|google\\.com/recaptcha/api/)";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        if (parameter.contains("fullhd.bz/v/")) {
            String fpName = br.getRegex("<title>Download: (.*?) \\- auf www\\.fullhd\\.bz</title>").getMatch(0);
            if (fpName == null) fpName = br.getRegex("id=\"download_content\"><div class=\"text\"><h2>([^\"\\']+)</h2>").getMatch(0);
            String[] links = br.getRegex("onclick=\"countdl\\(\\'\\d+\\',\\'\\d+\\'\\);\">[^\"\\']+</a></td><td align=\"right\">Downloads: \\d+</td></tr><tr><td align=\"right\"> </td><td><a href=\"(http://.*?)\"").getColumn(0);
            if (links == null || links.length == 0) links = br.getRegex("\"(http://fullhd\\.bz/\\?do=out\\&iden=[a-z0-9]+)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (String dl : links)
                decryptedLinks.add(createDownloadlink(dl));
            if (fpName != null) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName(fpName.trim());
                fp.addLinks(decryptedLinks);
            }
        } else {
            for (int i = 0; i <= 5; i++) {
                PluginForHost recplug = JDUtilities.getPluginForHost("DirectHTTP");
                jd.plugins.hoster.DirectHTTP.Recaptcha rc = ((DirectHTTP) recplug).getReCaptcha(br);
                rc.parse();
                rc.load();
                File cf = rc.downloadCaptcha(getLocalCaptchaFile());
                String c = getCaptchaCode(cf, param);
                rc.setCode(c);
                if (br.containsHTML(RECAPTCHATEXT)) continue;
                break;
            }
            if (br.containsHTML(RECAPTCHATEXT)) throw new DecrypterException(DecrypterException.CAPTCHA);
            String finallink = br.getRedirectLocation();
            if (finallink == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            decryptedLinks.add(createDownloadlink(finallink));
        }
        return decryptedLinks;
    }
}
