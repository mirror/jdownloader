//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision: 7387 $", interfaceVersion = 2, names = { "sexvideo.to" }, urls = { "http://[\\w\\.]*(sexvideo\\.to)/.+" }, flags = { 0 })
public class SxVdT extends PluginForDecrypt {

    public SxVdT(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        if (param.getCryptedUrl().toLowerCase().contains("crypt.sexvideo.to")) {
            String captcha;
            String code;
            Form form;
            for (int i = 0; i < 5; i++) {
                captcha = "http://" + br.getHost() + "/" + br.getRegex("id=\"captcha\" SRC=\"([^\"]*)").getMatch(0);
                code = getCaptchaCode(captcha, param);
                form = br.getForm(0);
                form.getInputFieldByType("text").setValue(code);
                br.submitForm(form);
                if(!br.containsHTML("id=\"captcha\""))break;
            }
            if(br.containsHTML("id=\"captcha\""))throw new DecrypterException(DecrypterException.CAPTCHA);

            Form[] links = br.getForms();
            for (Form form2 : links) {
                Browser clone = br.cloneBrowser();
                clone.submitForm(form2);
                String link = clone.getRegex("content=\"5;URL=([^\"]*)").getMatch(0);
                decryptedLinks.add(createDownloadlink(link));
            }
            if(decryptedLinks.size()==0)
            {

                String[] linkar = br.getRegex("<A HREF=\"(dl\\-[^\"]\\.[dlcrsf]{3}\\.html)\" TARGET").getColumn(0);
                for (String string : linkar) {
                    File container = JDUtilities.getResourceFile("container/" + System.currentTimeMillis() + "."+string.replaceFirst("\\.html", "").replaceFirst(".*\\.", ""));
                    if (!container.exists()) container.createNewFile();
                    Browser clone = br.cloneBrowser();
                    clone.getDownload(container, "http://" + br.getHost() + "/" +container);
                    decryptedLinks.addAll(JDUtilities.getController().getContainerLinks(container));

                }
            }
        }
        else
        {
            String[] links = HTMLParser.getHttpLinks(br.toString(), param.getCryptedUrl());
            for (String string : links) {
             if(string.toLowerCase().contains("crypt.sexvideo.to") || string.toLowerCase().contains("linksave"))
                 decryptedLinks.add(createDownloadlink(string));
            }
        }
        return decryptedLinks;
    }

    // @Override

}
