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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.pluginUtils.Recaptcha;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "" }, urls = { "http://[\\w\\.]*?music-pirates\\.org/show\\.php\\?id=\\d+" }, flags = { 0 })
public class MscPrtsOrg extends PluginForDecrypt {

    public MscPrtsOrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        String parameter = param.toString();
        br.getPage(parameter);
        String data1 = br.getRegex("<strong>Album</strong>:(.*?)</p>").getMatch(0);
        String data2 = br.getRegex("<strong>Band</strong>:(.*?)</p>").getMatch(0);

        for (int i = 0; i <= 1; i++) {
            Recaptcha rc = new Recaptcha(br);
            rc.parse();
            rc.load();
            File cf = rc.downloadCaptcha(getLocalCaptchaFile());
            String c = getCaptchaCode(cf, param);
            rc.setCode(c);
            if (br.getRedirectLocation() == null) return null;
            if (br.getRedirectLocation().contains("wrongcaptcha")) {
                br.getPage(parameter);
                continue;
            }
            if (br.getRedirectLocation().equals("http://anonym.to?")) throw new DecrypterException(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            break;
        }
        if (br.getURL().equals(parameter)) throw new DecrypterException(DecrypterException.CAPTCHA);
        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        if (data1 != null && data2 != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(data2.trim() + " - " + data1.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
