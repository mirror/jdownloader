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
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "filefactory.com" }, urls = { "http://[\\w\\.]*?filefactory\\.com/folder/[\\w]+" }, flags = { 0 })
public class FlFctrFldr extends PluginForDecrypt {

    public FlFctrFldr(PluginWrapper wrapper) {
        super(wrapper);
    }

    // @Override
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.getPage(parameter + "/?sort=filename&order=ASC&show=100&page=1");
        if (br.getRedirectLocation() != null) {
            /* to follow domain redirects */
            br.getPage(br.getRedirectLocation());
        }
        /* Error handling */
        if (br.containsHTML("No Files found in this folder")) {
            logger.warning("The requested document was not found on this server.");
            logger.warning(JDL.L("plugins.decrypt.errormsg.unavailable", "Perhaps wrong URL or the download is not available anymore."));
            return new ArrayList<DownloadLink>();
        }

        final String fpName = br.getRegex("<h1>Files in <span>\"([^<>\"]*?)\"</span>").getMatch(0);

        int maxPagenum = 1;
        final String maxPage = br.getRegex("data\\-paginator\\-totalPages=\"(\\d+)\"").getMatch(0);
        if (maxPage != null) maxPagenum = Integer.parseInt(maxPage);
        for (int i = 1; i <= maxPagenum; i++) {
            if (i > 1) br.getPage(parameter + "/?sort=filename&order=ASC&show=100&page=" + i);
            add(decryptedLinks);
        }
        if (fpName != null) {
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName.trim()));
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void add(ArrayList<DownloadLink> declinks) {
        final String links[] = br.getRegex(Pattern.compile("\"(https?://(www\\.)?filefactory\\.com/file/[^<>\"]*?)\"", Pattern.CASE_INSENSITIVE)).getColumn(0);
        for (String element : links) {
            declinks.add(createDownloadlink(element));
        }
    }

    // @Override

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}