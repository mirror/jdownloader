//    jDownloader - Downloadmanager
//    Copyright (C) 2012  JD-Team support@jdownloader.org
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

import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "4shared.com" }, urls = { "https?://(www\\.)?4shared(\\-china)?\\.com/(dir|folder|minifolder)/[^\"' /]+(/[^\"' ]+\\?sID=[a-zA-z0-9]{16})?" }, flags = { 0 })
public class FrShrdFldr extends PluginForDecrypt {

    public FrShrdFldr(final PluginWrapper wrapper) {
        super(wrapper);
    }

    /**
     * TODO: Implement API: http://www.4shared.com/developer/ 19.12.12: Their support never responded so we don't know how to use the API...
     * */
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String uid = new Regex(param.toString(), "\\.com/(dir|folder|minifolder)/([^/#]+)").getMatch(1);
        String parameter = new Regex(param.toString(), "(https?://(www\\.)?4shared(\\-china)?\\.com/)").getMatch(0);
        parameter = parameter + "folder/" + uid;
        br.setFollowRedirects(true);
        try {
            br.setCookie(this.getHost(), "4langcookie", "en");
        } catch (final Throwable e) {
        }
        String pass = "";

        // check the folder/ page for password stuff and validity of url
        br.getPage(parameter);

        // **needs checking**, all new html most likely needs fixing
        if (br.containsHTML("The file link that you requested is not valid")) return decryptedLinks;

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

        String fpName = br.getRegex("<title>4shared folder \\- (.*?)[\r\n\t ]+</title>").getMatch(0);
        if (fpName == null) fpName = br.getRegex("<h1 id=\"folderNameText\">(.*?)[\r\n\t ]+<h1>").getMatch(0);
        if (fpName == null) fpName = "4Shared - Folder";

        FilePackage fp = FilePackage.getInstance();
        fp.setName(Encoding.htmlDecode(fpName.trim()));
        int currentFirstLink = 0;
        String[] filter = null;
        do {
            // get minifolder page, contains all files and subfolders in one page
            br.getPage(parameter.replace(".com/folder", ".com/minifolder") + "?firstFileToShow=" + currentFirstLink);
            filter = br.getRegex("<tr valign=\"top\">[\r\n\t ]+<td width=\"\\d+\"(.*?)</td>[\r\n\t ]+</tr>").getColumn(0);
            if (filter == null) {
                logger.warning("Couldn't filter /minifolder/ page!");
                if (decryptedLinks.size() > 0) {
                    // might be caused because of filter.lenght = 99
                    logger.warning("Possible empty page");
                    break;
                } else {
                    logger.warning("Possible error");
                    return null;
                }
            }
            if (filter != null && filter.length > 0) {
                for (final String entry : filter) {
                    final String dllink = new Regex(entry, "\"(.*?4shared.com/[^\"]+\\.html)").getMatch(0);
                    if (dllink == null) {
                        logger.warning("Couldn't find dllink!");
                        continue;
                    }
                    final DownloadLink dlink = createDownloadlink(dllink.replace("https", "http"));
                    if (pass.length() != 0) {
                        dlink.setProperty("pass", pass);
                    }
                    String fileName = new Regex(entry, "[^\\,]+, [\\d,]+ [^\"]+\">([^\"']+)</a>").getMatch(0);
                    final String fileSize = new Regex(entry, "[^\\,]+, ([\\d,]+ [^\"]+)").getMatch(0);

                    if (fileName != null) {
                        fileName = fileName.replace("<wbr>", "");
                        dlink.setName(Encoding.htmlDecode(fileName));
                    }
                    if (fileSize != null) dlink.setDownloadSize(SizeFormatter.getSize(fileSize.replace(",", "")));
                    dlink.setAvailable(true);
                    fp.add(dlink);
                    decryptedLinks.add(dlink);
                }
            }

            // lets just add them back into the decrypter...
            if (filter != null && filter.length > 0) {
                for (String entry : filter) {
                    // sync folders share same uid but have ?sID=UID at the end, but this is done by JS from the main /folder/uid page...
                    String subDir = new Regex(entry, "\"(https?://(www\\.)?4shared(\\-china)?\\.com/(dir|folder)/[^\"' ]+/[^\"' ]+(\\?sID=[a-zA-z0-9]{16}))\"").getMatch(0);
                    // prevent the UID from showing up in another url format structure
                    if (subDir != null) {
                        if (subDir.contains("?sID=") || !new Regex(subDir, "\\.com/(folder|dir)/([^/]+)").getMatch(1).equals(uid)) {
                            decryptedLinks.add(createDownloadlink(subDir));
                        }
                    }
                }
            }
            if (currentFirstLink >= 10000) {
                logger.info("Fail safe triggered, quitting...");
                break;
            }

            currentFirstLink += 100;
            // they only show 99 results on the first page now.. 11/02/2013
        } while (filter != null && (filter.length == 100 | filter.length == 99));
        if (decryptedLinks.size() == 0) {
            logger.warning("Possible empty folder, or plugin out of date for link: " + parameter);
        }
        return decryptedLinks;
    }
}
