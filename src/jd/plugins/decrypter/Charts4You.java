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

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "charts4you.org" }, urls = { "http://[\\w\\.]*?charts4you\\.org/\\?id=\\d+"}, flags = { 0 })


public class Charts4You extends PluginForDecrypt {

    public Charts4You(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (br.containsHTML("Es existiert kein Eintrag") || br.containsHTML("We will be back soon")) return decryptedLinks;
        File file = this.getLocalCaptchaFile();
        String name = br.getRegex("Details zum Download von (.*?) \\- Charts4you").getMatch(0);
        String pass = br.getRegex(Pattern.compile("Passwort.*?</td>.*?<input type=\"text\" value=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        Form form = br.getForm(2);
        Browser.download(file, br.cloneBrowser().openGetConnection("captcha/imagecreate.php"));
        Point p = UserIO.getInstance().requestClickPositionDialog(file, JDL.L("plugins.decrypt.stealthto.captcha.title", "Captcha"), JDL.L("plugins.decrypt.stealthto.captcha", "Please click on the Circle with a gap"));
        if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        form.remove("x");
        form.remove("y");
        form.put("button.x", p.x + "");
        form.put("button.y", p.y + "");
        br.submitForm(form);
        String[] links = br.getRegex("out/\\?url=(.*?)\"").getColumn(0);
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        for (String link2 : links) {
            DownloadLink link = this.createDownloadlink(link2);
            if (pass != null) link.addSourcePluginPassword(pass);
            link.setFilePackage(fp);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }

    // @Override
    

}
