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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.gui.UserIO;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;
import jd.utils.JDUtilities;

public class SnurlCom extends PluginForDecrypt {

    public SnurlCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);

        if (br.containsHTML("<title>301 Moved Permanently</title>")) {
            parameter = br.getRedirectLocation();
            br.getPage(parameter);
        }

        // Password
        if (br.containsHTML("Please enter a passcode to continue to")) {
            Form form = br.getFormbyProperty("name", "pkeyform");
            if (form == null) return null;

            for (int i = 1; i <= 3; i++) {
                String folderPass = UserIO.getInstance().requestInputDialog("File Password");
                form.put("pkey", folderPass);
                br.submitForm(form);
                if (!br.containsHTML("Please enter a passcode to continue to")) break;
            }
            if (br.containsHTML("Please enter a passcode to continue to")) throw new DecrypterException("Wrong Password");
        }

        decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));

        return decryptedLinks;
    }

    @Override
    public String getVersion() {
        return getVersion("$Revision$");
    }
}