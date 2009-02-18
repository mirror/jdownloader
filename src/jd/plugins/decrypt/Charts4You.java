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

package jd.plugins.decrypt;

import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ClickPositionDialog;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class Charts4You extends PluginForDecrypt {

    public Charts4You(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        File file = this.getLocalCaptchaFile(this);
        String name = br.getRegex("Details zum Download von (.*?) \\- Charts4you").getMatch(0);
        String pass = br.getRegex(Pattern.compile("Passwort.*?</td>.*?<input type=\"text\" value=\"(.*?)\"", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getMatch(0);
        Form form = br.getForm(2);
        Browser.download(file, br.cloneBrowser().openGetConnection("captcha/imagecreate.php"));
        JDUtilities.acquireUserIO_Semaphore();
        ClickPositionDialog d = new ClickPositionDialog(SimpleGUI.CURRENTGUI.getFrame(), file, "Captcha", JDLocale.L("plugins.decrypt.charts4you.captcha", "Please click on the Circle with a gap"), 20, null);
        JDUtilities.releaseUserIO_Semaphore();
        if (d.abort == true) throw new DecrypterException(DecrypterException.CAPTCHA);
        Point p = d.result;
        form.remove("x");
        form.remove("y");
        form.put("button.x", p.x + "");
        form.put("button.y", p.y + "");
        br.submitForm(form);
        String[] links = br.getRegex("url=(.*?)\"").getColumn(0);
        FilePackage fp = new FilePackage();
        fp.setName(name);
        for (int i = 0; i < links.length; i++) {
            DownloadLink link = this.createDownloadlink(links[i]);
            if (pass != null) link.addSourcePluginPassword(pass);
            link.setFilePackage(fp);
            decryptedLinks.add(link);
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}