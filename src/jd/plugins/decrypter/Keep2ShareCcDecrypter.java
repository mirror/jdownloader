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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "keep2share.cc" }, urls = { "https?://((www|new)\\.)?(keep2share|k2s|k2share|keep2s|keep2)\\.cc/file/(info/)?[a-z0-9]+" })
public class Keep2ShareCcDecrypter extends PluginForDecrypt {

    public Keep2ShareCcDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final PluginForHost plugin = JDUtilities.getPluginForHost("keep2share.cc");
        if (plugin == null) {
            throw new IllegalStateException("keep2share plugin not found!");
        }
        br = ((jd.plugins.hoster.Keep2ShareCc) plugin).newWebBrowser();
        // set cross browser support
        ((jd.plugins.hoster.Keep2ShareCc) plugin).setBrowser(br);
        // // corrections
        // final String uid = ((jd.plugins.hoster.Keep2ShareCc) plugin).getFUID(param.toString());
        // final String host = ((jd.plugins.hoster.Keep2ShareCc) plugin).MAINPAGE.replaceFirst("^https?://",
        // ((jd.plugins.hoster.Keep2ShareCc) plugin).getProtocol());
        // if (uid == null || host == null) {
        // logger.warning("Decrypter broken for link: " + param.toString());
        // return null;
        // }
        // final String parameter = host + "/file/" + uid;

        // DO NOT AUTO CORRECT, links redirect to there default server
        final String parameter = param.toString().replaceFirst("https?://", ((jd.plugins.hoster.Keep2ShareCc) plugin).getProtocol());
        br.setFollowRedirects(true);
        ((jd.plugins.hoster.Keep2ShareCc) plugin).getPage(parameter);
        ((jd.plugins.hoster.Keep2ShareCc) plugin).followRedirectNew(br);
        // empty folder
        if (br.containsHTML("<span class=\"empty\">No results found\\.\\s*</span>")) {
            return decryptedLinks;
        }
        // Check if we have a single link or a folder
        if (br.containsHTML("class=\"summary\"")) {
            // folder
            final String fpName = br.getRegex("<title>([^<>\"]*?)</title>").getMatch(0);
            final FilePackage fp;
            if (fpName != null) {
                fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName.trim()));
                fp.addLinks(decryptedLinks);
            } else {
                fp = null;
            }
            int pageIndex = 1;
            while (!isAbort()) {
                final String[] links = br.getRegex("target=\"_blank\" href=\"([^\"]+)?(/file/[a-z0-9]+)").getColumn(1);
                if (links == null || links.length == 0) {
                    if (decryptedLinks.isEmpty()) {
                        logger.warning("Decrypter broken for link: " + parameter);
                        return null;
                    } else {
                        break;
                    }
                }
                for (final String link : links) {
                    final DownloadLink singlelink = createDownloadlink(link);
                    final String name = br.getRegex("target=\"_blank\" href=\"([^\"]+)?" + link + ".*?\">(.*?)</a>").getMatch(1);
                    final String size = br.getRegex("target=\"_blank\" href=\"([^\"]+)?" + link + ".*?\">.*?>\\[.*?([0-9\\.GKMB ]+?) \\]<").getMatch(1);
                    if (name != null) {
                        singlelink.setName(name);
                        singlelink.setAvailableStatus(AvailableStatus.TRUE);
                    }
                    if (size != null) {
                        singlelink.setDownloadSize(SizeFormatter.getSize(size.trim()));
                    }
                    if (fp != null) {
                        fp.add(singlelink);
                    }
                    decryptedLinks.add(singlelink);
                    distribute(singlelink);
                }
                sleep(250, param);
                ((jd.plugins.hoster.Keep2ShareCc) plugin).getPage("?UserFile_page=" + (pageIndex++));
            }
        } else {
            // single link
            final DownloadLink singlink = createDownloadlink(br.getURL());
            final String filename = ((jd.plugins.hoster.Keep2ShareCc) plugin).getFileName();
            final String filesize = ((jd.plugins.hoster.Keep2ShareCc) plugin).getFileSize();
            if (filename != null) {
                singlink.setName(Encoding.htmlDecode(filename.trim()));
            }
            if (filesize != null) {
                singlink.setDownloadSize(SizeFormatter.getSize(filesize.trim()));
            }
            if (filename == null) {
                singlink.setAvailableStatus(AvailableStatus.FALSE);
            } else if (filename.contains("...")) {
                // has to be null (to trigger linkchecking within hoster plugin to remove ...), availablestatus.unchecked will re-enter
                // decrypter
                singlink.setAvailableStatus(null);
            } else {
                // prevent wasteful double linkchecks.
                singlink.setAvailableStatus(AvailableStatus.TRUE);
            }
            if (br.containsHTML("Downloading blocked due to")) {
                throw new PluginException(LinkStatus.ERROR_HOSTER_TEMPORARILY_UNAVAILABLE, "Downloading blocked: No JD bug, please contact the keep2share support", 10 * 60 * 1000l);
            }
            // you can set filename for offline links! handling should come here!
            if (br.containsHTML("Sorry, an error occurred while processing your request|File not found or deleted|>Sorry, this file is blocked or deleted\\.</h5>|class=\"empty\"|>Displaying 1")) {
                singlink.setAvailableStatus(AvailableStatus.FALSE);
            }
            decryptedLinks.add(singlink);
        }
        return decryptedLinks;
    }
}
