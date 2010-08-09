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

import java.io.IOException;
import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "rapidshare.com" }, urls = { "http://[\\w\\.]*?rapidshare.com/users/.+" }, flags = { 0 })
public class RpdshrCmFldr extends PluginForDecrypt {

    private ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();

    public RpdshrCmFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String PASSWORDTEXT = "input type=\"password\" name=\"password\"";

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String parameter = param.toString();

        String page = br.getPage(parameter);
        String password = "";

        for (int retry = 1; retry < 5; retry++) {
            if (page.contains(PASSWORDTEXT)) {
                password = this.getPluginConfig().getStringProperty("PASSWORD", null);
                if (password == null) password = getUserInput(null, param);
                page = br.postPage(parameter, "password=" + password);
                if (br.containsHTML(PASSWORDTEXT)) {
                    getPluginConfig().setProperty("PASSWORD", null);
                    getPluginConfig().save();
                } else {
                    // Save actual password if it is valid
                    getPluginConfig().setProperty("PASSWORD", password);
                    getPluginConfig().save();
                }
            } else {
                break;
            }
        }

        getLinks(parameter, password, page);

        return decryptedLinks;
    }

    private void getLinks(String para, String password, String source) throws IOException {
        String[] folders = new Regex(source, "font\\-size:12pt\\;\" href=\"javascript:folderoeffnen\\('(\\d+?)'\\);").getColumn(0);
        String[] links = new Regex(source, "<a style=\"font-size:12pt;\" target=\"_blank\" href=\"http://rapidshare.com/files/(.*?)\">").getColumn(0);
        for (String element : folders) {
            getLinks(para, password, br.postPage(para, "password=" + password + "&subpassword=&browse=ID%3D" + element));
        }

        for (String element : links) {
            decryptedLinks.add(createDownloadlink("http://rapidshare.com/files/" + element));
        }
    }

    // @Override

}
