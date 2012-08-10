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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "netfolder.in" }, urls = { "http://[\\w\\.]*?netfolder\\.in/folder\\.php\\?folder_id\\=[\\w]{7}|http://[\\w\\.]*?netfolder\\.in/[\\w]{7}/.*?" }, flags = { 0 })
public class Ntfldrn extends PluginForDecrypt {

    static private final String patternSupported_1 = "http://[\\w\\.]*?netfolder\\.in/folder\\.php\\?folder_id\\=[a-zA-Z0-9]{7}";
    static private final String patternSupported_2 = "http://[\\w\\.]*?netfolder\\.in/[a-zA-Z0-9]{7}/.*?";

    public Ntfldrn(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.getPage(parameter);
        if (new Regex(parameter, Pattern.compile(patternSupported_2, Pattern.CASE_INSENSITIVE)).matches()) {
            decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
        } else if (new Regex(parameter, Pattern.compile(patternSupported_1, Pattern.CASE_INSENSITIVE)).matches()) {
            String password = "";
            for (int retrycounter = 1; retrycounter <= 5; ++retrycounter) {
                int check = br.getRegex("input type=\"password\" name=\"password\"").count();
                if (check > 0) {
                    password = getUserInput(null, param);
                    br.postPage(parameter, "password=" + password + "&save=Absenden");
                } else {
                    break;
                }
            }

            final String[] links = br.getRegex("href=\"http://netload\\.in/(.*?)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String element : links) {
                decryptedLinks.add(createDownloadlink("http://netload.in/" + element));
            }
        }

        return decryptedLinks;
    }

}
