//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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
import java.io.FileOutputStream;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.components.ClickPositionDialog;
import jd.http.Browser;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class LinkCryptWs extends PluginForDecrypt {

    public LinkCryptWs(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        String containerId = new Regex(parameter, "dir/([a-zA-Z0-9]+)").getMatch(0);

        br.getPage("http://linkcrypt.ws/download.php?id=" + containerId + "&art=dlc");

        logger.finest("Captcha Protected");

        boolean valid = true;
        for (int i = 0; i < 5; ++i) {
            if (br.containsHTML("<b>Bitte klicke auf den offenen Kreis!</b>")) {
                valid = false;
                File file = this.getLocalCaptchaFile(this);
                Form form = br.getForm(0);
                Browser.download(file, br.cloneBrowser().openGetConnection("http://linkcrypt.ws/captx.php"));
                JDUtilities.acquireUserIO_Semaphore();
                ClickPositionDialog d = new ClickPositionDialog(SimpleGUI.CURRENTGUI.getFrame(), file, "Captcha", JDLocale.L("plugins.decrypt.stealthto.captcha", "Please click on the Circle with a gap"), 20, null);
                if (d.abort == true) throw new DecrypterException(DecrypterException.CAPTCHA);
                JDUtilities.releaseUserIO_Semaphore();
                Point p = d.result;
                form.put("x", p.x + "");
                form.put("y", p.y + "");
                br.submitForm(form);
            } else {
                valid = true;
                break;
            }
        }

        if (valid == false) throw new DecrypterException(DecrypterException.CAPTCHA);

        File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + ".dlc");

        /* TODO: Das kann man sicher besser lösen.. bitte mal wer reinschauen */
        FileOutputStream out = new FileOutputStream(container);
        for (int i = 0; i < br.toString().length(); i++) {
            out.write((byte) br.toString().charAt(i));
        }
        out.close();

        decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
        container.delete();

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
