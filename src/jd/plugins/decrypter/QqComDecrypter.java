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
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
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

        if (br.containsHTML(">分享文件已过期或者链接错误，请确认后重试。<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        final String[][] linkInfo = br.getRegex("qhref=\"qqdl://([^<>\"]*?)\" .*? filesize=\"(\\d+)\" filehash=\"([A-Z0-9]+)\" title=\"([^<>\"]*?)\"").getMatches();
        if (linkInfo == null || linkInfo.length == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        for (final String singleLink[] : linkInfo) {
            final DownloadLink dl = createDownloadlink("http://qqdecrypted.com/" + System.currentTimeMillis() + new Random().nextInt(10000));
            dl.setFinalFileName(Encoding.htmlDecode(singleLink[3].trim()));
            dl.setDownloadSize(Long.parseLong(singleLink[1]));
            dl.setProperty("qhref", singleLink[0]);
            dl.setProperty("filehash", singleLink[2]);
            dl.setProperty("mainlink", parameter);
            dl.setProperty("plainfilename", singleLink[3]);
            dl.setAvailable(true);
            decryptedLinks.add(dl);
        }
        return decryptedLinks;
    }

}
