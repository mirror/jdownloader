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

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "mylink4u.info" }, urls = { "http://[\\w\\.]*?mylink4u\\.info/[\\w]+" }, flags = { 0 })
public class Mlnk4unf extends PluginForDecrypt {

    public Mlnk4unf(PluginWrapper wrapper) {
        super(wrapper);
        // TODO Auto-generated constructor stub
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(false);
        br.getPage(parameter.toString());
        if (br.getRedirectLocation() == null) if (br.containsHTML("This URL expired!")) return decryptedLinks;
        if (br.containsHTML("This URL is protected!")) {
            Form pw = br.getForm(0);
            String password = null;
            password = Plugin.getUserInput("Password?", parameter);
            pw.put("pass", password);
            br.submitForm(pw);
            decryptedLinks.add(this.createDownloadlink(br.getRedirectLocation()));
        }

        else {
            decryptedLinks.add(this.createDownloadlink(br.getRedirectLocation()));
        }
        return decryptedLinks;
    }

}
