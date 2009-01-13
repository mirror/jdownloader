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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class LinkProtectorCom extends PluginForDecrypt {

    public LinkProtectorCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private String decryptCode(String decryptedLink, int charCode) {
        String result = "";
        try {
            for (int i = 0; i * 4 < decryptedLink.length(); i++) {
                result = (char) (Integer.parseInt(decryptedLink.substring(i * 4, i * 4 + 4)) - charCode) + result;
            }
        } catch (Exception e) {
            result = "";
        }
        return result;
    }

    @Override
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
            } else {
                do_continue = true;
                break;
            }
        }

        if (do_continue == true) {
            String cryptedLink = br.getRegex("write\\(stream\\('(.*?)'\\)").getMatch(0);
            int charCode = Integer.parseInt(br.getRegex("fromCharCode\\(yy\\[i\\]-(.*?)\\)\\;").getMatch(0));
            String decryptedLink = decryptCode(cryptedLink, charCode);
            String link = new Regex(decryptedLink, "<iframe src=\"(.*?)\"").getMatch(0);
            if (link != null) {
                decryptedLinks.add(createDownloadlink(link));
            } else {
                return null;
            }
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
