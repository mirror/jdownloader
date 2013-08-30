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
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "qq.com" }, urls = { "http://(www\\.)?(fenxiang\\.qq\\.com/(share/index\\.php/share/share_c/index/|x/)[A-Za-z0-9\\-_~]+|urlxf\\.qq\\.com/\\?[A-Za-z0-9]+)" }, flags = { 0 })
public class QqComDecrypter extends PluginForDecrypt {

    public QqComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);

        if (parameter.matches("http://(www\\.)?urlxf\\.qq\\.com/\\?[A-Za-z0-9]+")) {
            final String redirect = br.getRegex("window.location=\"(http[^\"]+)").getMatch(0);
            if (redirect == null) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (redirect.matches("http://(www\\.)?fenxiang\\.qq\\.com/share/index\\.php/share/share_c/index/[A-Za-z0_9]+")) {
                decryptedLinks.add(createDownloadlink(redirect));
                return decryptedLinks;
            }
            parameter = redirect;
            br.getPage(parameter);
        }

        if (br.containsHTML(">很抱歉，此资源已被删除或包含敏感信息不能查看啦<")) {
            final DownloadLink offline = createDownloadlink("http://qqdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            offline.setName(new Regex(parameter, "([A-Za-z0-9\\-_~]+)$").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }

        final String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);
        final String[] tableEntries = br.getRegex("<td class=\"td_c\">(.*?)</td>").getColumn(0);
        if (tableEntries == null || tableEntries.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String tableEntry : tableEntries) {
            final String qhref = new Regex(tableEntry, "qhref=\"(qqdl://[^<>\"]*?)\"").getMatch(0);
            final String filehash = new Regex(tableEntry, "filehash=\"([^<>\"]*?)\"").getMatch(0);
            final String filesize = new Regex(tableEntry, "filesize=\"([^<>\"]*?)\"").getMatch(0);
            final String title = new Regex(tableEntry, "title=\"([^<>\"]*?)\"").getMatch(0);
            final DownloadLink dl = createDownloadlink("http://qqdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setFinalFileName(Encoding.htmlDecode(title.trim()));
            dl.setDownloadSize(Long.parseLong(filesize));
            dl.setProperty("qhref", qhref);
            dl.setProperty("filehash", filehash);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plainfilename", title);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}