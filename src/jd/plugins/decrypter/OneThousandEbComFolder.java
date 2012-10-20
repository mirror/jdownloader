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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "1000eb.com" }, urls = { "http://(www\\.)?([A-Za-z0-9\\-_]+\\.)?1000eb\\.com/[A-Za-z0-9\\-_]+\\.htm?" }, flags = { 0 })
public class OneThousandEbComFolder extends PluginForDecrypt {

    public OneThousandEbComFolder(final PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        ArrayList<String> pages = new ArrayList<String>();
        pages.add("1");
        String parameter = param.toString();
        br.getPage(parameter);

        String fpName = br.getRegex("\">我的空间</a>\\&nbsp;\\&gt;\\&gt;\\&nbsp;<a href=\"[^<>\"\\']+\" title=\"([^<>\"\\']+)\">").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<meta name=\"keywords\" content=\"([^,\"]+)").getMatch(0);

        int maxCount = 1;
        String lastPage = br.getRegex("class=\"pager\\-golast\"><a href=\"/[A-Za-z0-9\\-_]+\\.htm\\?p=(\\d+)\"").getMatch(0);
        if (lastPage != null) maxCount = Integer.parseInt(lastPage);
        for (int i = 1; i <= maxCount; i++) {
            logger.info("Decrypting page " + i + " of " + maxCount);
            if (i > 1) br.getPage(parameter + "?p=" + i);
            final String[] links = br.getRegex("class=\"li\\-name\"><a title=\"[^<>\"\\']+\" href=\\'(http://1000eb\\.com/[a-z0-9]+)\\'").getColumn(0);
            final String[] folderLinks = br.getRegex("\"(/mydirectory[^<>\"]*?)\"").getColumn(0);
            if ((links == null || links.length == 0) && (folderLinks == null || folderLinks.length == 0)) {
                logger.warning("Decrypter broken for link: " + parameter);
                return null;
            }
            if (links != null && links.length > 0) {
                for (final String singleLink : links)
                    decryptedLinks.add(createDownloadlink(singleLink));
            }
            if (folderLinks != null && folderLinks.length > 0) {
                final String mainlink = new Regex(parameter, "(http://(www\\.)?[A-Za-z0-9\\-_]+\\.1000eb\\.com)").getMatch(0);
                for (final String folderLink : folderLinks)
                    decryptedLinks.add(createDownloadlink(mainlink + folderLink));
            }
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

}
