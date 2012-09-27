//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "cineon.com" }, urls = { "http://(www\\.)?c1neon\\.com/download\\-[\\w\\-]+\\d+\\.html" }, flags = { 0 })
public class CineonCom extends PluginForDecrypt {

    public CineonCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter);
        String fpName = br.getRegex("title=\'([^\']+)").getMatch(0);
        if (fpName != null) fpName = Encoding.htmlDecode(fpName);
        String allLinks = br.getRegex("var subcats = \\{([^;]+)").getMatch(0);
        if (allLinks != null) {
            allLinks = allLinks.replaceAll("\\\\|\"", "");
            for (String[] s : new Regex(allLinks, "([\\w\\-]+):\\[(\\[.*?\\])\\]").getMatches()) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName((fpName + "@" + s[0]).trim());
                for (String[] ss : new Regex(s[1], "\\[(\\d),\\w+,.*?,(.*?),\\d+\\]").getMatches()) {
                    final DownloadLink dl = createDownloadlink(ss[1].trim());
                    fp.add(dl);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(dl);
                }
            }
            final String[] uploadedLinks = new Regex(allLinks, "\\[\\d+,redirect,ul.ico,(http://(www\\.)?(uploaded|ul)\\.(to|net)/file/[A-Za-z0-9]+)").getColumn(0);
            if (uploadedLinks != null && uploadedLinks.length != 0) {
                FilePackage fp = FilePackage.getInstance();
                fp.setName((fpName + "@uploaded.net"));
                for (final String ullink : uploadedLinks) {
                    final DownloadLink dl = createDownloadlink(ullink);
                    fp.add(dl);
                    try {
                        distribute(dl);
                    } catch (final Throwable e) {
                        /* does not exist in 09581 */
                    }
                    decryptedLinks.add(dl);
                }
            }
        }
        return decryptedLinks;
    }

}