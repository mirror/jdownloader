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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ClickPositionDialog;
import jd.http.Browser;
import jd.parser.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ProtectBoxIn extends PluginForDecrypt {

    public ProtectBoxIn(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);

        File file = this.getLocalCaptchaFile(this);

        Form form = br.getForm(1);
        Browser.download(file, br.cloneBrowser().openGetConnection("http://www.protectbox.in/captcha/imagecreate.php"));
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
        String[] links = br.getRegex("Link\\.php\\?url\\=(.*?)\"").getColumn(0);
        for (int i = 0; i < links.length; i++) {
            decryptedLinks.add(createDownloadlink(links[i]));
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }

}