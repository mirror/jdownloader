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

import java.io.File;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.InputField;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "protect-it.org" }, urls = { "http://[\\w\\.]*?protect-it\\.org//?\\?id=.*" }, flags = { 0 })
public class PrtctTOg extends PluginForDecrypt {

    public PrtctTOg(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        LoadImage li = LoadImage.loadFile("prtctt");
        li.baseUrl = parameter;
        li.load(getHost());
        String[] p = null;
        try {
            p = getCaptchaCode(li, param).split(":");
        } catch (Exception e) {
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        if (p == null || p.length != 2) throw new DecrypterException(DecrypterException.CAPTCHA);
        br = li.br;
        Form form = br.getForm(0);
        InputField pass = form.getInputField("pass");
        if (pass != null) {
            pass.setValue(getUserInput(null, param));

        }
        form.remove("captcha");
        form.put("captcha.x", p[0]);
        form.put("captcha.y", p[1]);
        br.submitForm(form);
        String[] links = br.getRegex("sndReq\\('(.*?)'\\)").getColumn(0);
        for (String link2 : links) {
            Browser br2 = br.cloneBrowser();
            br2.getPage(link2);
            DownloadLink link = this.createDownloadlink(br2.getURL());
            decryptedLinks.add(link);
        }
        String[] container = br.getRegex("document\\.location='(http://protect-it\\.org//?\\?fetchcrypt=.*?)'").getColumn(0);
        if (decryptedLinks.size() == 0 && container != null && container.length > 0) {
            for (String c : container) {
                File file = JDUtilities.getResourceFile("tmp/" + this.getHost() + "/" + System.currentTimeMillis() + "." + c.replaceFirst(".*type=.", ""));
                br.getDownload(file, c);
                if (file != null && file.exists() && file.length() > 100) {
                    decryptedLinks = JDUtilities.getController().getContainerLinks(file);
                    if (decryptedLinks.size() > 0) return decryptedLinks;
                }
            }
            if (decryptedLinks.size() == 0) throw new DecrypterException("Out of date. Try Click'n'Load");
        }
        return decryptedLinks;
    }

    // @Override

}
