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

import jd.utils.JDLocale;

import jd.http.HTTPConnection;

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
import jd.utils.JDUtilities;

public class ProtectorIT extends PluginForDecrypt {

    public ProtectorIT(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches("http://[\\w.]*?protect-it.org//?\\?de=.*")) {
            br.openGetConnection(parameter);
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            return decryptedLinks;
        }
        if (!parameter.matches("http://[\\w.]*?protect-it.org//?\\?id=.*")) throw new DecrypterException(JDLocale.L("downloadlink.status.error.file_not_found", "File not found"));
        br.getPage(parameter);
        if (br.toString().contains("Diesen Link gibt es nicht")) throw new DecrypterException(JDLocale.L("downloadlink.status.error.file_not_found", "File not found"));
        boolean valid = true;
        for (int i = 0; i < 5; ++i) {
            if (br.toString().contains("name=\"captcha\" alt=\"Captcha\"")) {
                valid = false;
                File file = this.getLocalCaptchaFile(this);
                Form form = br.getForm(0);

                Browser.download(file, br.cloneBrowser().openGetConnection("http://" + br.getHost() + br.getRegex("<input type=\"image\" src=\"\\.([^\"]*)\" name=\"captcha\" alt=\"Captcha\">").getMatch(0)));
                JDUtilities.acquireUserIO_Semaphore();
                ClickPositionDialog d = new ClickPositionDialog(SimpleGUI.CURRENTGUI.getFrame(), file, "Captcha", "", 20, null);
                if (d.abort == true) throw new DecrypterException(DecrypterException.CAPTCHA);
                JDUtilities.releaseUserIO_Semaphore();
                Point p = d.result;
                form.remove("captcha");
                form.put("captcha.x", p.x + "");
                form.put("captcha.y", p.y + "");
                br.submitForm(form);
                String captchawrong = br.getRegex("<a href=\"([^\"]*)\">Neuer Versuch</a></center>").getMatch(0);
                if (captchawrong != null) br.getPage(captchawrong);
            } else {
                valid = true;
                break;
            }
        }
        if (valid == false) throw new DecrypterException(DecrypterException.CAPTCHA);
        String containerlink = br.getRegex("(http://protect-it.org/\\?fetchcrypt=[^\"']*)[\"']").getMatch(0);
        containerlink = null;
        if (containerlink != null) {
            try {
                HTTPConnection con = br.openGetConnection(containerlink);
                File container = JDUtilities.getResourceFile("container/" + getFileNameFormHeader(con));
                Browser.download(container, con);
                decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));
                container.delete();
            } catch (Exception e) {
                // TODO: handle exception
            }

        }
        if (decryptedLinks.size() == 0) {
            String[] matches = br.getRegex("(http://protect-it.org//?\\?de=[^\"']*)[\"']").getColumn(0);
            for (String string : matches) {
                br.openGetConnection(string);
                decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));

            }
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision: 4360 $");
    }
}
