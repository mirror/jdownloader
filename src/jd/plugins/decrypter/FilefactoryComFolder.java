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

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "https?://(?:www\\.)?filefactory\\.com/((?:folder|f)/[\\w]+|share/fi[a-z0-9,:]+)" })
public class FilefactoryComFolder extends PluginForDecrypt {
    public FilefactoryComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        if (parameter.matches(".+/share/fi.+")) {
            /* 2019-08-17: New type of folder which contains all fileIDs inside URL */
            final String[] fileIDs = new Regex(parameter, "fi:([a-z0-9]+)").getColumn(0);
            for (final String fileid : fileIDs) {
                final String url = "http://www." + this.getHost() + "/file/" + fileid;
                decryptedLinks.add(this.createDownloadlink(url));
            }
        } else {
            br.getHeaders().put("Accept-Language", "en-gb, en;q=0.8");
            br.getPage(parameter + "/?sort=filename&order=ASC&show=100&page=1");
            /* Error handling */
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else if (br.containsHTML("No Files found in this folder")) {
                /* Empty folder */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String fpName = br.getRegex("<h1>Files in <span>(.*?)</span>").getMatch(0);
            int maxPage = 1;
            final String maxPageStr = br.getRegex("data\\-paginator\\-totalPages=\"(\\d+)\"").getMatch(0);
            if (maxPageStr != null) {
                maxPage = Integer.parseInt(maxPageStr);
            }
            for (int i = 1; i <= maxPage; i++) {
                if (i > 1) {
                    br.getPage(parameter + "/?sort=filename&order=ASC&show=100&page=" + i);
                }
                add(decryptedLinks);
            }
            if (!StringUtils.isEmpty(fpName)) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            }
        }
        return decryptedLinks;
    }

    private void add(final ArrayList<DownloadLink> declinks) {
        final String links[] = br.getRegex(Pattern.compile("\"(https?://(?:www\\.)?filefactory\\.com/file/[^<>\"]*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String element : links) {
            declinks.add(createDownloadlink(element));
        }
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}