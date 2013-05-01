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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1000eb.com" }, urls = { "http://(?!static|upload)([\\w\\-\\.]+)?(1000eb\\.com/[\\w\\-]+\\.htm(\\?p=\\d+)?|[\\w\\-\\.]+1000eb\\.com/)" }, flags = { 0 })
public class OneThousandEbComFolder extends PluginForDecrypt {

    public OneThousandEbComFolder(final PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String INVALIDLINKS = "http://([\\w\\-\\.]+)?1000eb\\.com/(upload|bulletin_detail_\\d+|chance|copyrights|agreements|faq|contactus|aboutus|joinus|reportbadinformation|login|search).*?\\.htm";

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        if (parameter.matches(INVALIDLINKS)) {
            logger.info("Invalid link: " + parameter);
            return decryptedLinks;
        }
        br.setFollowRedirects(true);
        br.getPage(parameter);

        if (br.containsHTML("class=\"noBodyBox\">主人尚未上传文件到当前文件夹 </div>") || br.containsHTML("<b>下载链接不合法</b>")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        int maxCount = 1, minCount = 1;
        String lastPage = br.getRegex("class=\"pager\\-golast\"><a href=\"/([A-Za-z0-9\\-_]+\\.htm)?\\?p=(\\d+)\"").getMatch(1);
        if (lastPage != null) maxCount = Integer.parseInt(lastPage);
        String firstPage = new Regex(parameter, "\\?p=(\\d+)").getMatch(0);
        if (firstPage != null) minCount = Integer.parseInt(firstPage);
        parameter = parameter.replaceAll("\\?p=\\d+", "");
        if (minCount > maxCount) maxCount = minCount;

        String fpName = br.getRegex("\">我的空间</a>\\&nbsp;\\&gt;\\&gt;\\&nbsp;<a href=\"[^<>\"\\']+\" title=\"([^<>\"\\']+)\">").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<meta name=\"keywords\" content=\"([^,\"]+)").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));

        for (int i = minCount; i <= maxCount; i++) {

            logger.info("Decrypting page " + i + " of " + maxCount);

            try {
                if (i > minCount) br.getPage(parameter + "?p=" + i);
            } catch (Throwable e) {
                logger.warning("1000eb.com: " + e.getMessage());
                return decryptedLinks;
            }

            for (final String[] singleLink : br.getRegex("class=\"li\\-name\"><a title=\"[^\"]+\" href=\'(http://1000eb\\.com/[^\']+)\' target=\"_blank\" class=\"ellipsis\">([^<]+)</a></span> <span class=\"li\\-size\">([^<]+)</span>").getMatches()) {
                DownloadLink dLink = createDownloadlink(singleLink[0]);
                dLink.setFinalFileName(singleLink[1].trim());
                try {
                    dLink.setDownloadSize(SizeFormatter.getSize(singleLink[2].replace("M", "MB").replace("K", "KB")));
                } catch (Throwable e) {
                }
                dLink.setAvailable(true);
                fp.add(dLink);
                try {
                    if (isAbort()) return decryptedLinks;
                    distribute(dLink);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dLink);
            }

            sleep(2 * 1000l, param); // prevent a java.out.of.memory error ;-)

            final String mainlink = new Regex(parameter, "(http://(www\\.)?[A-Za-z0-9\\-_]+\\.1000eb\\.com)/?").getMatch(0);
            for (final String folderLink : br.getRegex("class=\"li\\-name\"><a title=\"[^\"]+\" href=\"([^\"]+)\">[^<]+</a><span class=\"list\\-dir\\-files\">").getColumn(0)) {
                DownloadLink dLink = createDownloadlink(mainlink + folderLink);
                fp.add(dLink);
                try {
                    distribute(dLink);
                } catch (final Throwable e) {
                    /* does not exist in 09581 */
                }
                decryptedLinks.add(dLink);
            }

        }

        if (decryptedLinks == null || decryptedLinks.size() == 0) {
            logger.warning("Decrypter broken for link: " + parameter);
            return null;
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}