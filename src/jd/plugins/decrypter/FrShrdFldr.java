//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.utils.locale.JDL;

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "http://[\\w\\.]*?4shared(-china)?\\.com/dir/.+" }, flags = { 0 })
public class FrShrdFldr extends PluginForDecrypt {

    private final static double RANDOM = Math.random();

    public FrShrdFldr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        br.setFollowRedirects(true);
        br.setCookie(getHost(), "4langcookie", "en");
        String pass = "";

        // check for password
        br.getPage(parameter);
        if (br.containsHTML("enter a password to access")) {
            final Form form = br.getFormbyProperty("name", "theForm");
            if (form == null) { throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT); }
            for (int retry = 5; retry > 0; retry--) {
                pass = Plugin.getUserInput(null, param);
                form.put("userPass2", pass);
                br.submitForm(form);
                if (!br.containsHTML("enter a password to access")) {
                    break;
                } else {
                    if (retry == 1) {
                        logger.severe("Wrong Password!");
                        throw new DecrypterException("Wrong Password!");
                    }
                }
            }
        }

        final String script = br.getRegex("src=\"(/account/homeScript.*?)\"").getMatch(0);
        final String sId = new Regex(script, "^.+sId=(\\w+)").getMatch(0);
        if (script == null || sId == null) { return null; }

        br.cloneBrowser().getPage("http://" + br.getHost() + script);
        final String burl = "/account/changedir.jsp?sId=" + sId;

        String[] subDir = br.getRegex("javascript:changeDirLeft\\((\\d+)\\)").getColumn(0);
        int result = 4;
        // allow choice scan only root directory or all subdirectories too
        if (subDir.length > 1 && br.containsHTML("ml_file_")) {
            result = UserIO.getInstance().requestConfirmDialog(UserIO.STYLE_LARGE | UserIO.NO_COUNTDOWN, JDL.L("jd.plugins.decrypter.frshrdfldr.dir.title", "Directory scan"), JDL.L("jd.plugins.decrypter.frshrdfldr.dir.message", "Do you want scan all subdirectory or only files in this directory?"), null, JDL.L("jd.plugins.decrypter.frshrdfldr.dir.ok", "Subdirectory"), JDL.L("jd.plugins.decrypter.frshrdfldr.dir.cancel", "File"));
        }

        if (result == 4 && br.containsHTML("ml_file_")) {
            subDir = new String[] { subDir[0] };
        }
        scan(decryptedLinks, pass, burl, subDir, progress);

        if (decryptedLinks.size() == 0) {
            try {
                logger.warning("Decrypter out of date for link: " + parameter);
            } catch (final Throwable e) {
                /* not available in public 0.9580 */
            }
            return null;
        }
        return decryptedLinks;
    }

    private void scan(final ArrayList<DownloadLink> decryptedLinks, final String pass, final String burl, final String[] subDir, final ProgressController progress) throws Exception {
        final String[] pages = br.getRegex("javascript:pagerShowFiles\\((\\d+)\\);").getColumn(0);
        String url;

        if (subDir.length > 1) {
            progress.setRange(subDir.length);
        } else {
            progress.setRange(pages.length);
        }

        // scan all
        int j = 1;
        do {
            if (subDir.length > 1) {
                progress.increase(1);
            }
            String name = br.getRegex("<.*?>4shared - (.*?) - free").getMatch(0);
            if (name == null) {
                name = "4shared - Folder";
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(name);
            // change to listview
            if (br.containsHTML("(fbContainer|thumbdiv)")) {
                br.getPage("http://" + br.getHost() + burl + "&ajax=true&changedir=" + subDir[j - 1] + "&homemode=8&viewMode=1&random=" + RANDOM);
            }
            // scan pages
            int i = 0;
            do {
                if (subDir.length == 1) {
                    progress.increase(1);
                }
                final String[] links = br.getRegex("ml_file(.*?)title").getColumn(0);
                // scan page
                for (String dl : links) {
                    final String[] dlTmp = dl.split("\n");
                    dl = new Regex(dlTmp[1], "href=\"javascript:openNewWindow\\('(.*?)'").getMatch(0);
                    final String dlName = new Regex(dlTmp[2], "/>(.*?)</a>").getMatch(0);
                    final String dlSize = new Regex(dlTmp[5], ">(.*?)</td>").getMatch(0);
                    if (dl == null | dlName == null | dlSize == null) {
                        continue;
                    }
                    final DownloadLink dlink = createDownloadlink(dl.replace("https", "http"));
                    if (pass.length() != 0) {
                        dlink.setProperty("pass", pass);
                    }
                    dlink.setName(Encoding.htmlDecode(dlName));
                    dlink.setDownloadSize(SizeFormatter.getSize(dlSize.replace(",", "")));
                    dlink.setAvailable(true);
                    dlink.setFilePackage(fp);
                    decryptedLinks.add(dlink);
                }
                if (i < pages.length - 1) {
                    url = "http://" + br.getHost() + burl + "&ajax=true&firstFileToShow=" + pages[i] + "&sortsMode=NAME&sortsAsc=&random=" + RANDOM;
                    br.getPage(url);
                }
                i++;
            } while (i <= pages.length);
            if (j < subDir.length) {
                url = "http://" + br.getHost() + burl + "&refreshAfterUnzip=false&ajax=true&changedir=" + subDir[j] + "&random=" + RANDOM;
                br.getPage(url);
            }
            j++;
        } while (j <= subDir.length);
    }
}
