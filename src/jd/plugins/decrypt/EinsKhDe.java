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

import java.util.ArrayList;
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.http.Encoding;
import jd.parser.Form;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class EinsKhDe extends PluginForDecrypt {

    final static private Pattern patternSupported_File = Pattern.compile("http://[\\w\\.]*?1kh\\.de/[0-9]+", Pattern.CASE_INSENSITIVE);

    // final static private Pattern patternSupported_Folder =
    // Pattern.compile("http://[\\w\\.]*?1kh\\.de/f/[0-9/]+",
    // Pattern.CASE_INSENSITIVE);

    public EinsKhDe(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        Form[] forms = br.getForms();
        if (Regex.matches(parameter, patternSupported_File)) {
            /* Einzelne Datei */

            String[] links = br.getRegex("<iframe name=\"pagetext\" height=\".*?\" frameborder=\"no\" width=\"100%\" src=\"(.*?)\"></iframe>").getColumn(0);
            progress.setRange(links.length);

            for (String element : links) {
                decryptedLinks.add(createDownloadlink(Encoding.htmlDecode(element)));
                progress.increase(1);
            }
        } else {
            /* ganzer Ordner */
            boolean valid = true;
            if (forms != null && forms.length > 0 && forms[0].getVars().containsKey("Password")) {
                valid = false;
                /* Ordner ist Passwort geschützt */
                for (int retrycounter = 1; retrycounter <= 5; retrycounter++) {
                    String password = JDUtilities.getGUI().showUserInputDialog("Ordnerpasswort?");

                    if (password == null) {
                        /* Auf "Abbruch" geklickt */
                        throw new DecrypterException("No Valid PW");
                    }

                    forms[0].put("Password", password);
                    br.submitForm(forms[0]);

                    if (!br.containsHTML("Das eingegebene Passwort ist falsch!")) {
                        valid = true;
                        break;
                    }
                }
            }
            if (valid == false) throw new DecrypterException("No Valid PW");
            String[] links = br.getRegex("<div class=\"Block3\" ><a id=\"DownloadLink_(\\d+)\"").getColumn(0);
            progress.setRange(links.length);

            for (String element : links) {
                decryptedLinks.add(createDownloadlink("http://1kh.de/" + element));
                progress.increase(1);
            }

        }

        return decryptedLinks;
    }

    @Override
    public String getCoder() {
        return "JD-Team";
    }

    @Override
    public String getVersion() {
        String ret = new Regex("$Revision$", "\\$Revision: ([\\d]*?) \\$").getMatch(0);
        return ret == null ? "0.0" : ret;
    }
}