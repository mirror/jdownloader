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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "junocloud.me" }, urls = { "http://(www\\.)?junocloud\\.me/folders/\\d+/[^<>\"/]+" }, flags = { 0 })
public class JunoCloudMeFolder extends PluginForDecrypt {

    public JunoCloudMeFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        this.setBrowserExclusive();
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(true);
        br.getPage(parameter);

        if (br.containsHTML(">No such user exist<")) {
            logger.info("Link offline: " + parameter);
            return decryptedLinks;
        }

        final String user = br.getRegex("usr_login=([^<>\"]*?)\\&amp;").getMatch(0);
        final String fid = new Regex(parameter, "folders/(\\d+)/").getMatch(0);
        final String fpName = br.getRegex("<h1>([^<>\"]*?)</h1>").getMatch(0);

        if (fpName == null) {
            logger.warning("Decrypter broken for link:" + parameter);
            return null;
        }

        int maxPage = 1;
        final String[] pages = br.getRegex("op=user_public\\&amp;page=(\\d+)\\'").getColumn(0);
        for (final String page : pages) {
            final int curpage = Integer.parseInt(page);
            if (curpage > maxPage) {
                maxPage = curpage;
            }
        }

        for (int i = 1; i <= maxPage; i++) {
            if (i > 1) {
                br.getPage("http://junocloud.me/?fld_name=&fld_id=" + fid + "&fld=&op=user_public&usr_login=" + user + "&page=" + i);
            }
            final String[][] info = br.getRegex("class=\"link\"><a href=\"(http://(www\\.)?junocloud\\.me/[a-z0-9]{12}/[^<>\"/]*?)\" target=\"_blank\">([^<>\"/]*?)</a> \\- ([^<>\"]*?)</div>").getMatches();
            for (final String[] finfo : info) {
                final String url = finfo[0];
                final String fname = finfo[2];
                final String fsize = finfo[3];
                final DownloadLink fina = createDownloadlink(url);
                fina.setName(Encoding.htmlDecode(fname).trim());
                fina.setDownloadSize(SizeFormatter.getSize(fsize));
                fina.setAvailable(true);
                decryptedLinks.add(fina);
            }
            if (user == null) {
                /* Username is needed to decrypt multiple pages */
                break;
            }
        }

        if (decryptedLinks.size() == 0) {
            logger.info("Link offline (folder empty): " + parameter);
            return decryptedLinks;
        }

        final FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName));
        fp.addLinks(decryptedLinks);

        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}