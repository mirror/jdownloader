//jDownloader - Downloadmanager
//Copyright (C) 2011  JD-Team support@jdownloader.org
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.nutils.encoding.HTMLEntities;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision: 20458 $", interfaceVersion = 2, names = { "7958.com" }, urls = { "^https?://(www\\.)?xddisk\\.com/(space_\\d+\\.html|space\\.php?username=\\d+)$" }, flags = { 0 })
public class XdDiskCom extends PluginForDecrypt {

    private String id = null;

    public XdDiskCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> set = new HashSet<String>();

        // lets rename space.php links to standard format
        id = new Regex(param.toString(), "/(space\\.php\\?user=|space_)(\\d+)").getMatch(1);

        if (id == null) {
            logger.info("Erorr, please report to JDownloader Development Team : " + param.toString());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }

        // no https at this stage
        param.setCryptedUrl("http://xddisk.com/space_" + id + ".html");

        String parameter = param.toString();

        br.getPage(parameter);

        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404.html")) {
            logger.info("Incorrect URL or no longer exists : " + parameter);
            return decryptedLinks;
        }

        String fpName = "Space - " + id;

        parsePage(set, decryptedLinks);
        parseNextPage(set, decryptedLinks);

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(HashSet<String> list, ArrayList<DownloadLink> ret) throws PluginException {
        String[] filted = br.getRegex("(<tr class=\"color\\d+\">.*?</tr>)").getColumn(0);
        if (filted == null) {
            logger.info("Erorr within : " + br.getURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String filter : filted) {
            String link = new Regex(filter, "(https?://(www\\.)?xddisk\\.com/file-\\d+\\.html)").getMatch(0);
            String folder = new Regex(filter, "hahahahadoesn't exist?").getMatch(0);
            if (link == null && folder == null) break;
            String fuid = new Regex(link, "file-(\\d+)").getMatch(0);
            if (link != null && !list.contains(link)) {
                DownloadLink dl = createDownloadlink(link);
                String filename = new Regex(filter, "<a href=\"file-" + fuid + "\\.html\"[^>]+>(.*?)</a>").getMatch(0);
                String filesize = new Regex(filter, ">(\\d+(\\.\\d+)? ?(B|KB?|MB?|GB?))</td>").getMatch(0);
                if (filesize != null) dl.setDownloadSize(SizeFormatter.getSize(filesize));
                if (filename != null) {
                    if (filename.endsWith("...")) filename = filename.replaceFirst("...$", "");
                    dl.setFinalFileName(Encoding.htmlDecode(filename.trim()));
                    dl.setAvailable(true);
                }
                try {
                    dl.setLinkID(fuid);
                } catch (Throwable e) {
                }
                list.add(link);
                ret.add(dl);
            }
            // if (folder != null && !list.contains(folder)) {
            // list.add(folder);
            // ret.add(createDownloadlink(folder));
            // }
        }
    }

    private boolean parseNextPage(HashSet<String> list, ArrayList<DownloadLink> ret) throws IOException, PluginException {
        String nextPage = br.getRegex("<a href=\"(space\\.php\\?username=" + id + "&folder_id=\\d+&amp;pg=\\d+)\" class=\"p_redirect\">&#8250;</a>").getMatch(0);
        if (nextPage != null) {
            br.getPage(HTMLEntities.unhtmlentities(nextPage));
            parsePage(list, ret);
            parseNextPage(list, ret);
            return true;
        }
        return false;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}