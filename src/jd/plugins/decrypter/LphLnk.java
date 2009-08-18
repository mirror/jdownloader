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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "alpha-link.eu" }, urls = { "http://[\\w\\.]*?alpha\\-link\\.eu/\\?id=[a-fA-F0-9]+" }, flags = { 0 })
public class LphLnk extends PluginForDecrypt {

    public LphLnk(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        Form form = br.getForm(1);
        for (int i = 0; i <= 5; i++) {
            String code = getCaptchaCode("http://alpha-link.eu/captcha/captcha.php", param);
            form.put("captcha", code);
            form.setAction(parameter);
            br.submitForm(form);
            if (!br.containsHTML("(Code ist falsch)|(kein Code eingegeben)")) break;
        }
        form = br.getForm(1);
        String[] ids = br.getRegex("class='btn' name='id' value='(\\d+)'").getColumn(0);
        if (ids.length == 0) return null;
        progress.setRange(ids.length);
        for (String id : ids) {
            form.put("id", id);
            br.submitForm(form);

            String codedLink = br.getRegex("src=.\"(.*?).\"").getMatch(0);
            if (codedLink == null) return null;
            String link = Encoding.htmlDecode(codedLink);

            decryptedLinks.add(createDownloadlink(link));
            progress.increase(1);
        }
        return decryptedLinks;
    }

    // @Override

}
