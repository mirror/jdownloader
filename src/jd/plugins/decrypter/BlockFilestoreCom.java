//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;

import org.appwork.utils.formatter.SizeFormatter;

/**
 * @author raztoki
 */
@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "blockfilestore.com" }, urls = { "https?://www\\.blockfilestore\\.com/folder/([a-z0-9\\-]+)" }) 
public class BlockFilestoreCom extends PluginForDecrypt {

    public BlockFilestoreCom(PluginWrapper wrapper) {
        super(wrapper);
    }

    private final HashMap<Form, Browser> subdirectories = new HashMap<Form, Browser>();

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final String parameter = param.toString();
        final String uid = new Regex(parameter, getSupportedLinks()).getMatch(0);
        br.getPage(parameter);
        if (br.containsHTML(">This chapter has been removed due to infringement\\.<")) {
            decryptedLinks.add(createOfflinelink(parameter));
            return decryptedLinks;
        }
        // now we need reference to the filename.. we assume there is only one filename in that given directory.
        decryptedLinks.addAll(process(br, parameter, uid));
        for (final Map.Entry<Form, Browser> a : subdirectories.entrySet()) {
            final Browser br2 = a.getValue();
            br2.submitForm(a.getKey());
            decryptedLinks.addAll(process(br2, parameter, uid));
        }

        return decryptedLinks;
    }

    private ArrayList<DownloadLink> process(final Browser br, final String parameter, final String uid) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String fpName = br.getRegex("<a id=\"ContentPlaceHolder1_rptNiveles_lnkGo_0\"[^>]*>(.*?)</a>").getMatch(0);
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(fpName);

        final String[] results = br.getRegex("<a[^>]+>\\s*<div[^>]*>.*?</div>\\s*</a>").getColumn(-1);
        if (results != null) {
            for (String result : results) {
                result = Encoding.htmlDecode(result);
                long r = 0;
                while (r <= 10000) {
                    r = new Random().nextLong();
                }
                final DownloadLink dl = new DownloadLink(null, null, getHost(), Encoding.urlDecode(parameter.replace("blockfilestore.com/", "blockfilestoredecrypted.com/") + "/" + r, true), true);
                final String id = new Regex(result, "href=\"javascript:__doPostBack\\('(.*?)',''\\)\"").getMatch(0);
                // folders
                if (new Regex(result, "/images/folder\\.png").matches()) {
                    final Form f = br.getFormbyAction(uid);
                    if (f == null) {
                        // throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                        // return instead of defect.. could be empty folders or what ever we should always return the results.
                        return decryptedLinks;
                    }
                    f.put("__EVENTTARGET", Encoding.urlEncode(id));
                    f.put(Encoding.urlEncode("ctl00$txtEmaiLogin"), "");
                    f.put(Encoding.urlEncode("ctl00$txtPasswordLogin"), "");
                    f.put(Encoding.urlEncode("ctl00$txtEmaiRecovery"), "");
                    f.remove("ctl00%24btnLogin");
                    f.remove("ctl00%24btnSend");
                    final Browser br2 = br.cloneBrowser();
                    subdirectories.put(f, br2);
                    continue;
                }
                // files
                final String filename = new Regex(result, "<td class=\"seg\">\\s*(.*?)\\s*</td>").getMatch(0);
                final String filesize = new Regex(result, "<td class=\"terc\">\\s*(.*?)\\s*</td>").getMatch(0);
                // we assume there can only be one unique file within a given directory. note: can not use file id is the same across all,
                // its just a reference of directory entry order.. may cause problem if someone decrypts the entire directory and then owner
                // adds more files and directory changes its order (due to alphabetical order).
                dl.setLinkID("blockfilestore://" + uid + "/" + JDHash.getSHA256(filename));
                dl.setProperty("dlName", filename);
                dl.setProperty("fileUID", id);
                dl.setProperty("folderUID", uid);
                if (fpName != null) {
                    fp.add(dl);
                }
                dl.setName(filename);
                dl.setDownloadSize(SizeFormatter.getSize(filesize.replace(",", ".")));
                dl.setAvailableStatus(AvailableStatus.TRUE);
                decryptedLinks.add(dl);
            }
        }
        return decryptedLinks;
    }

    /* NO OVERRIDE!! */
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }

}