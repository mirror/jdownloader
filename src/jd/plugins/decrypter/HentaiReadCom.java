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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "hentairead.com" }, urls = { "https?://(www\\.)?hentairead.com/hentai/[^/?]+" })
public class HentaiReadCom extends PluginForDecrypt {
    public HentaiReadCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString().replace("http:", "https:");
        br.getPage(parameter + "/1/");
        br.getPage(br.getRegex("<a[^>]+href\\s*=\\s*\"([^\"]+)\"[^>]+class\\s*=\\s*\"[^\"]*btn-read-now").getMatch(0));
        if (br.getHttpConnection().getResponseCode() == 404) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        String fpName = br.getRegex("property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\"([^\"]+)\\s+-\\s+Read hentai Doujinshi online for free").getMatch(0);
        String pageListText = br.getRegex("<select[^>]+id\\s*=\\s*\\\"single-pager\\\"[^>]+>([^$]+)</select>").getMatch(0);
        if (StringUtils.isNotEmpty(pageListText)) {
            if (pageListText.contains("</select>")) {
                pageListText = pageListText.substring(0, pageListText.indexOf("</select>"));
            }
            String[] pageURLs = new Regex(pageListText, "<option[^>]+value\\s*=\\s*\"[^\"]+\"[^>]+data-redirect\\s*=\\s*\"([^\"]+)").getColumn(0);
            String title = fpName;
            if (StringUtils.isEmpty(title)) {
                title = br.getRegex("property\\s*=\\s*\"og:title\"[^>]+content\\s*=\\s*\\\"(?:Reading\\s+)?([^\\\"]+)\\s+-\\s+[\\w\\s]+online for free").getMatch(0);
                if (StringUtils.isNotEmpty(title)) {
                    fpName = title;
                }
            }
            if (StringUtils.isEmpty(title)) {
                logger.warning("Could not retrieve title for " + parameter);
                return null;
            } else {
                title = Encoding.htmlDecode(title.trim());
            }
            if (pageURLs == null || pageURLs.length == 0) {
                logger.warning("Could not retrieve pages for " + parameter);
                return null;
            } else {
                final FilePackage fp = FilePackage.getInstance();
                fp.setProperty(LinkCrawler.PACKAGE_ALLOW_MERGE, true);
                if (fpName != null) {
                    fp.setName(Encoding.htmlDecode(fpName.trim()));
                }
                final DecimalFormat df = new DecimalFormat(String.valueOf(pageURLs.length).replaceAll("\\d", "0"));
                int pageNumber = 1;
                Browser br2 = br.cloneBrowser();
                for (String pageURL : pageURLs) {
                    br2.getPage(Encoding.htmlDecode(pageURL));
                    String imageURL = br2.getRegex("id\\s*=\\s*\"image-[^\"]+\"[^>]+data-src\\s*=\\s*\"([^\"]+)").getMatch(0);
                    final DownloadLink dl = createDownloadlink("directhttp://" + imageURL);
                    final String extension = getFileNameExtensionFromURL(Encoding.htmlDecode(imageURL));
                    String filename = title + "_" + df.format(pageNumber) + extension;
                    dl.setFinalFileName(filename);
                    dl.setAvailable(true);
                    fp.add(dl);
                    distribute(dl);
                    decryptedLinks.add(dl);
                    pageNumber++;
                }
            }
        }
        return decryptedLinks;
    }
}
