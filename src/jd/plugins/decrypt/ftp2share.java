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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Encoding;
import jd.parser.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

public class ftp2share extends PluginForDecrypt {

    private static final Pattern patternSupported_Folder = Pattern.compile("http://[\\w\\.]*?ftp2share\\.net/folder/[a-zA-Z0-9\\-]+/(.*?)", Pattern.CASE_INSENSITIVE);

    public ftp2share(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        if (parameter.matches(patternSupported_Folder.pattern())) {
            if (!parameter.contains("?system")) {
                parameter = parameter + "?system=*";
            }
            br.getPage(parameter);
        } else {
            br.getPage(parameter);
            Form[] forms = br.getForms();
            if (forms.length > 1) {
                br.submitForm(forms[1]);
            }
        }

        String links[] = br.getRegex(Pattern.compile("<a href=\"javascript\\:go\\('(.*?)'\\)\">", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String element : links) {
            String link = Encoding.Base64Decode(Encoding.filterString(element, "qwertzuiopasdfghjklyxcvbnmMNBVCXYASDFGHJKLPOIUZTREWQ1234567890=/"));
            decryptedLinks.add(createDownloadlink(link));
        }

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}
