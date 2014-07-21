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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "yobt.com" }, urls = { "http://(www\\.)?yobt\\.com/content/\\d+/.*\\.html" }, flags = { 0 })
public class YobtComDecrypter extends PluginForDecrypt {

    public YobtComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);
        if (!br.getURL().contains("yobt.com/content/") || br.getHttpConnection().getResponseCode() == 404) {
            final DownloadLink offline = createDownloadlink(parameter.replace("yobt.com/", "yobtdecrypted.com/"));
            offline.setFinalFileName(new Regex(parameter, "content(/\\d+/.*)\\.html").getMatch(0));
            offline.setAvailable(false);
            offline.setProperty("offline", true);
            decryptedLinks.add(offline);
            return decryptedLinks;
        }
        final String lid = new Regex(parameter, "content/(\\d+)/").getMatch(0);
        String title = br.getRegex("<title>(.*?)</title>").getMatch(0);
        if (title == null) {
            title = lid;
        }
        title = Encoding.htmlDecode(title).trim();
        if (br.containsHTML("class=\"player\"")) {
            final DownloadLink fina = createDownloadlink(parameter.replace("yobt.com/", "yobtdecrypted.com/"));
            fina.setName(title + ".mp4");
            fina.setAvailable(true);
            decryptedLinks.add(fina);
        } else {
            final String[] links = br.getRegex("\"(http://[a-z0-9]+\\.yobt.com/[A-Za-z0-9/]+/img_thumb/\\d+_\\d+x\\d+\\.jpg)\"").getColumn(0);
            if (links == null || links.length == 0) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            for (final String singleLink : links) {
                final Regex lregex = new Regex(singleLink, "(http://[a-z0-9]+\\.yobt.com/[A-Za-z0-9/]+/)img_thumb/(\\d+)_\\d+x\\d+\\.jpg");
                final String finallink = lregex.getMatch(0) + "img/" + lregex.getMatch(1) + ".jpg";
                final DownloadLink dl = createDownloadlink("directhttp://" + finallink);
                dl.setAvailable(true);
                decryptedLinks.add(dl);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            fp.addLinks(decryptedLinks);
        }

        return decryptedLinks;
    }

}
