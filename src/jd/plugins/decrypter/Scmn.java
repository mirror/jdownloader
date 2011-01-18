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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "scum.in" }, urls = { "http://[\\w\\.]*?scum\\.in/(index\\.php\\?id=\\d+|[0-9]+-.*?\\.html)" }, flags = { 0 })
public class Scmn extends PluginForDecrypt {

    public Scmn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        String id = new Regex(parameter, "scum\\.in/index\\.php\\?id=(\\d+)").getMatch(0);
        if (id == null) id = new Regex(parameter, "scum\\.in/(\\d+)-").getMatch(0);
        br.getPage(parameter);
        String captchaCode = getCaptchaCode("http://scum.in/assets/captcha.php?t=", param);
        br.postPage("http://scum.in/sites/links.callback.php", "id=" + Encoding.htmlDecode(id) + "&captcha=" + captchaCode);
        String links[] = br.getRegex("href=\"(.*?)\"").getColumn(0);
        if (links == null || links.length == 0) return null;
        progress.setRange(links.length);
        for (String element : links) {
            DownloadLink dLink = createDownloadlink(element);
            dLink.addSourcePluginPassword("scum.in");
            decryptedLinks.add(dLink);
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
