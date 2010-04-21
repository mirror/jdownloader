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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "hideurl.biz" }, urls = { "http://[\\w\\.]*?hideurl\\.biz/[\\w]+" }, flags = { 0 })
public class Hdrlbz extends PluginForDecrypt {

    public Hdrlbz(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();

        br.setFollowRedirects(false);
        br.getPage(parameter);
        String links[] = br.getRegex(Pattern.compile("value=\".*?Download.*?\" onclick=\"openlink\\('(.*?)','.*?'\\);\"", Pattern.CASE_INSENSITIVE)).getColumn(0);

        progress.setRange(links.length);
        for (String element : links) {
            br.getPage(element);
            if (br.getRedirectLocation() != null) {
                decryptedLinks.add(createDownloadlink(br.getRedirectLocation()));
            } else {
                String link = br.getRegex(Pattern.compile("action=\"(.*?)\"", Pattern.CASE_INSENSITIVE)).getMatch(0);
                if (link != null) decryptedLinks.add(createDownloadlink(link));
            }
            progress.increase(1);
        }

        return decryptedLinks;
    }

}
