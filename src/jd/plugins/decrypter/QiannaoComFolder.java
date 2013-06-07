//jDownloader - Downloadmanager
//Copyright (C) 2009  JD-Team support@jdownloader.org
//
//This program is free software: you can redistribute it and/or modify
//it under the terms of the GNU General Public License as published by
//the Free Software Foundation, either version 3 of the License, or
//(at your option) any later version.
//
//This program is distributed in the hope that it will be useful,
//but WITHOUT ANY WARRANTY; without even the implied warranty of
//MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//GNU General Public License for more details.
//
//You should have received a copy of the GNU General Public License
//along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.plugins.decrypter;

import java.util.ArrayList;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "qiannao.com" }, urls = { "http://(www\\.)?qiannao\\.com/space/dir/[^<>\"]*\\.page" }, flags = { 0 })
public class QiannaoComFolder extends PluginForDecrypt {

    public QiannaoComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.getPage(parameter);
        final String[] files = br.getRegex("onClick=\"btn_copy\\(\\'(/space/file/[^<>\"]*?)\\'\\)").getColumn(0);
        final String[] dirs = br.getRegex("<td  style=\"text\\-align:left\">[\t\n\r ]+<a title=\"[^<>\"]*?\" href=\"(/space/dir/[^<>\"]*?)\"").getColumn(0);
        if ((files == null || files.length == 0) && (dirs == null || dirs == null)) {
            if (br.containsHTML("<th>文件名</th>")) {
                logger.info("Link offline: " + parameter);
                return decryptedLinks;
            }
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        if (files != null && files.length != 0) {
            for (final String singleLink : files) {
                final DownloadLink dl = createDownloadlink("http://www.qiannao.com" + singleLink);
                dl.setName(new Regex(singleLink, "/([^<>\"/]*)/\\.page$").getMatch(0));
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
        }
        if (dirs != null && dirs.length != 0) {
            for (final String singleLink : dirs)
                decryptedLinks.add(createDownloadlink("http://www.qiannao.com" + singleLink));
        }
        return decryptedLinks;
    }

}
