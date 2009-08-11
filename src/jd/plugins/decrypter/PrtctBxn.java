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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;
@DecrypterPlugin(revision = "$Revision: 7139 $", interfaceVersion = 2, names = { "protectbox.in" }, urls = { "http://[\\w\\.]*?protectbox\\.in/.*"}, flags = { 0 })


public class PrtctBxn extends PluginForDecrypt {

    public PrtctBxn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        File file = this.getLocalCaptchaFile();

        Form form = br.getForm(0);
        Browser.download(file, br.cloneBrowser().openGetConnection("http://www.protectbox.in/captcha/imagecreate.php"));
        Point p = UserIO.getInstance().requestClickPositionDialog(file, JDL.L("plugins.decrypt.stealthto.captcha.title", "Captcha"), JDL.L("plugins.decrypt.stealthto.captcha", "Please click on the Circle with a gap"));
        if (p == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        form.remove("x");
        form.remove("y");
        form.put("button.x", p.x + "");
        form.put("button.y", p.y + "");
        br.submitForm(form);
        String[] links = br.getRegex("<td style=.*?<a href=\"(.*?)\"(?= onmouse)").getColumn(0);
        for (String link : links) {
            decryptedLinks.add(createDownloadlink(link.trim()));
        }
        return decryptedLinks;
    }

    // @Override
    

}
