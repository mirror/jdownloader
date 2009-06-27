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
import java.util.logging.Level;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class CryptBoxCC extends PluginForDecrypt {

    private static final String CAPTCHA_PATTERN = "<img src=\\'\\.\\./captcha/captcha\\.php\\'>";
    private static final String FRAME_SRC_PATTERN = "<frame src=[\\'\\\"](.*?)[\\'\\\"]>";
    private static final String SINGLE_LINK_PATTERN = "<a href=\\'\\.\\./(link/[0-9]+)";
    private static final String TOPLEVEL_URL_PATTERN = "(http://[\\w\\.]*?cryptbox\\.cc/).*";
    private static final String FOLDER_NOT_FOUND_PATTERN = "<h3>Fehler!</h3>Dieser Ordner ex[ei]stiert nicht.";

    public CryptBoxCC(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

        br.getPage(parameter.toString());

        if (br.containsHTML(FOLDER_NOT_FOUND_PATTERN)) {
            logger.fine("No such folder.");
            return decryptedLinks;
        }

        if (br.containsHTML(CAPTCHA_PATTERN)) {
            Form form = br.getForm(0);
            if (form == null) {
                logger.log(Level.SEVERE, "Could not find captcha form.");
                return null;
            }
            form.put("captcha_action", "FALSE");
            br.submitForm(form);
        }

        Regex regex = new Regex(br, SINGLE_LINK_PATTERN);
        if (!regex.matches()) {
            logger.log(Level.SEVERE, "Could not find any links.");
            return null;
        }

        String baseUrl = new Regex(parameter.toString(), TOPLEVEL_URL_PATTERN).getMatch(0);
        if (baseUrl == null) return null;

        String[] links = regex.getColumn(0);
        if (links == null) return null;

        for (String link : links) {
            br.getPage(baseUrl + link);
            String requestLink = br.getRegex(FRAME_SRC_PATTERN).getMatch(0);
            if (requestLink == null) continue;

            br.getPage(requestLink);
            String decryptedLink = br.getRegex(FRAME_SRC_PATTERN).getMatch(0);
            if (decryptedLink == null) continue;
            decryptedLinks.add(createDownloadlink(decryptedLink));
        }
        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return "$Revision$";
    }

}
