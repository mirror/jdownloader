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
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "7958.com" }, urls = { "https?://(?!www\\.)[a-z0-9]+\\.7958\\.com/folder\\-\\d+" }, flags = { 0 })
public class SevenNineFiveEightCom extends PluginForDecrypt {

    private String id      = null;
    private String profile = null;

    public SevenNineFiveEightCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        HashSet<String> set = new HashSet<String>();

        String parameter = param.toString();

        id = new Regex(parameter, "(https?://(?!www\\.)[a-z0-9]+\\.7958\\.com(/folder\\-\\d+)?)").getMatch(0);
        profile = new Regex(parameter, "(https?://(?!www\\.)[a-z0-9]+\\.7958\\.com)/").getMatch(0);

        br.getPage(parameter);

        if (br.getRedirectLocation() != null && br.getRedirectLocation().contains("/404.html")) {
            logger.info("Incorrect URL or no longer exists : " + parameter);
            return decryptedLinks;
        }

        String fpName = null;
        // don't mess with this, unless you are feeling brave!
        String[] fpname = br.getRegex("<div class=\"qjwm-foc\">[^\r\n]+<a href=\"" + profile + "\" target=\"_blank\">(.*?)</a>(-<a href=\"" + profile + "/folder-\\d+\">[^<]+</a>){0,2}-?(.*?)</div>").getRow(0);
        if (fpname != null && fpname.length != 0) {
            if (fpname[2] != null && fpname[2].contains("com/folder-")) {
                logger.info("FREAK, this shouldn't be so! : " + br.getURL());
            }
            if ((fpname[2] == null || fpname[2].equals("")) && (fpname[0] != null && fpname[0].length() != 0)) {
                fpName = fpname[0];
            } else if ((fpname[2] != null || !fpname[2].equals("")) && (fpname[0] != null && fpname[0].length() != 0)) {
                fpName = fpname[0] + " - " + fpname[2];
            }
        }
        if (fpName == null || fpName.length() == 0) {
            logger.info("FREAK, this shouldn't happen! : " + br.getURL());
        }

        parsePage(set, decryptedLinks);
        // Buggy -> Disabled
        // parseNextPage(set, decryptedLinks);

        if (fpName != null) {
            FilePackage fp = FilePackage.getInstance();
            fp.setName(fpName.trim());
            fp.addLinks(decryptedLinks);
        }
        return decryptedLinks;
    }

    private void parsePage(HashSet<String> list, ArrayList<DownloadLink> ret) throws PluginException {
        String[] filted = br.getRegex("<tr>[\r\n\t ]+(.*?)</a></td>[\r\n\t ]+</tr>").getColumn(0);
        if (filted == null) {
            logger.info("Erorr within : " + br.getURL());
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (String filter : filted) {
            String link = new Regex(filter, "(" + profile + "/down_\\d+\\.html)").getMatch(0);
            String folder = new Regex(filter, "(" + profile + "/folder\\-\\d+)").getMatch(0);
            if (link == null && folder == null) break;
            String fuid = new Regex(link, "(\\d+)\\.html").getMatch(0);
            if (link != null && !list.contains(link)) {
                final DownloadLink dl = createDownloadlink(link);
                String filename = new Regex(filter, "<td class=\"cb\"><a href=\"" + link + "\"[^>]+>(.*?)</a></td>").getMatch(0);
                String filesize = new Regex(filter, ">(\\d+(\\.\\d+)? ?(KB|MB|GB))</td>").getMatch(0);
                if (filesize != null) dl.setDownloadSize(SizeFormatter.getSize(filesize));
                if (filename != null) {
                    dl.setFinalFileName(filename);
                    dl.setAvailable(true);
                }
                try {
                    dl.setLinkID(fuid);
                } catch (Throwable e) {
                }
                list.add(link);
                ret.add(dl);
            }
            if (folder != null && !list.contains(folder)) {
                list.add(folder);
                ret.add(createDownloadlink(folder));
            }
        }
    }

    private boolean parseNextPage(HashSet<String> list, ArrayList<DownloadLink> ret) throws IOException, PluginException {
        String nextPage = br.getRegex("<a href=\"(folder-\\d+-\\d+)\" class=\"nxt\">下一页</a>").getMatch(0);
        if (nextPage != null) {
            br.getPage(nextPage);
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