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
import jd.parser.html.Form;
import jd.parser.html.Form.MethodType;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "link-protector.com" }, urls = { "http://[\\w\\.]*?link-protector\\.com(/(x-)?\\d+)?" }, flags = { 0 })
public class LnkPrtctrCm extends PluginForDecrypt {

    public LnkPrtctrCm(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        if (br.containsHTML("red>Bad Referrer!")) {
            String ref = br.getRegex("You could get this File only from t.*?<br><a.*?><b>(.*?)</b>").getMatch(0);
            if (ref == null) return null;
            br.getPage(ref);
            br.getPage(parameter);
        }
        boolean do_continue = false;
        for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
            if (br.containsHTML("<h1>PASSWORD PROTECTED LINK</h1>") || br.containsHTML("Incorrect Password")) {
                String passCode = getUserInput(null, param);
                Form pwForm = br.getForm(0);
                pwForm.put("u_password", passCode);
                br.submitForm(pwForm);
                System.out.print(br.toString());
            } else {
                do_continue = true;
                break;
            }
        }
        String link = null;
        if (do_continue == true) {
            link = br.getRegex("onClick=\"window.location='(.*?)'\" style=").getMatch(0);
            if (link == null) {
                link = br.getRegex("<form action=\"(.*?)\"").getMatch(0);
                if (link == null) link = br.getRegex("window\\.location = \"(.*?)\"").getMatch(0);
            }
            if (link == null) link = br.getRegex("METHOD=\"LINK\" ACTION=\"(http[^<>\"]*?)\"").getMatch(0);
            if (link == null || link.length() < 10) {
                Form form = br.getForm(0);
                if (form != null) {
                    form.setMethod(MethodType.GET);
                    br.setFollowRedirects(false);
                    br.submitForm(form);
                }
                link = br.getRegex("frame name=\"protected\" src=\"(.*?)\"").getMatch(0);
            }
        }
        if (link != null && link.length() > 10) {
            decryptedLinks.add(createDownloadlink(link));
        } else {
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}