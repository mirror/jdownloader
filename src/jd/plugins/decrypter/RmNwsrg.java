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

import java.awt.Color;
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
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.Regex;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rom-news.org" }, urls = { "http://[\\w\\.]*?download\\.rom-news\\.org/[\\w]+" }, flags = { 0 })
public class RmNwsrg extends PluginForDecrypt {

    public RmNwsrg(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (br.getRedirectLocation() != null) {
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            return decryptedLinks;
        }
        for (int i = 1; i <= 5; i++) {
            File file = this.getLocalCaptchaFile();
            Regex colorhex = br.getRegex(br.getRegex("<h2>.*?class=\"(.*?)\".*?</h2>").getMatch(0) + " . color: .(..)(..)(..); .");
            Color color = new Color(Integer.parseInt(colorhex.getMatch(0), 16), Integer.parseInt(colorhex.getMatch(1), 16), Integer.parseInt(colorhex.getMatch(2), 16));
            String cap = br.getRegex("\"image\" src=\"(.*?png.*?)\"").getMatch(0);
            Form form = br.getForm(0);
            Browser.download(file, br.cloneBrowser().openGetConnection(cap));
            int[] p = new jd.captcha.specials.RmNwsrg(file, color).getResult();
            if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
            form.remove("x");
            form.remove("y");
            form.put("name.x", p[0] + "");
            form.put("name.y", p[1] + "");
            br.submitForm(form);
            if (br.getRedirectLocation() != null) break;
        }

        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));

        return decryptedLinks;
    }

}
