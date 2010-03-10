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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "moviestube.net" }, urls = { "http://[\\w\\.]*?moviestube\\.net/protection/ordner\\d+\\.html" }, flags = { 0 })
public class MovStbeNt extends PluginForDecrypt {

    public MovStbeNt(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        for (int i = 0; i <= 5; i++) {
            if (!br.containsHTML("image.php")) return null;
            String captchalink = "http://www.moviestube.net/protection/img/image.php";
            String code = getCaptchaCode(captchalink, param);
            br.postPage(parameter, "code=" + code);
            if (!br.containsHTML("image.php")) break;
        }
        // Errorhandling for offline links
        if (br.containsHTML("Fremder, komm du nur herein")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
        // Captcha errorhandling
        if (br.containsHTML("image.php")) throw new DecrypterException(DecrypterException.CAPTCHA);
        String[] redirectLinks = br.getRegex("<div class=\"container\"><a href=\"(.*?)\"").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) redirectLinks = br.getRegex("\"(http://moviestube\\.net/protection/.*?/.*?\\.html)\"").getColumn(0);
        if (redirectLinks == null || redirectLinks.length == 0) return null;
        progress.setRange(redirectLinks.length);
        for (String link : redirectLinks) {
            br.getPage(link);
            String dllink = br.getRedirectLocation();
            if (dllink == null) return null;
            decryptedLinks.add(createDownloadlink(dllink));
            progress.increase(1);
        }

        return decryptedLinks;
    }

}