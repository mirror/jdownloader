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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "iload.to" }, urls = { "http://iload\\.to/go/\\d+/|http://iload\\.to/(view|title|release)/.*?/" }, flags = { 0 })
public class LdT extends PluginForDecrypt {

    private String patternSupported_Info = "http://iload\\.to/(view|title|release)/.*?/";

    public LdT(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setDebug(true);
        if (parameter.matches(patternSupported_Info)) {
            br.getPage(parameter);
            String password = br.getRegex("Passwort:</td><td><input.*?value='(.*?)'").getMatch(0);
            String links_page[] = br.getRegex(Pattern.compile("<div class='Link'>(.*?)target='_blank'.*?div class='Clicks'", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)).getColumn(0);
            if (links_page == null) return null;
            for (String link : links_page) {
                String dllink = new Regex(link, Pattern.compile("href='/go/(\\d+)/'", Pattern.CASE_INSENSITIVE)).getMatch(0);
                DownloadLink dl_link = createDownloadlink("http://iload.to/go/" + dllink + "/");
                dl_link.addSourcePluginPassword(password);
                decryptedLinks.add(dl_link);
            }
        } else {
            br.getPage(parameter);
            DownloadLink dl;
            decryptedLinks.add(dl = createDownloadlink(br.getRedirectLocation()));
            dl.setUrlDownload(br.getRedirectLocation());
        }
        return decryptedLinks;
    }

    // @Override

}
