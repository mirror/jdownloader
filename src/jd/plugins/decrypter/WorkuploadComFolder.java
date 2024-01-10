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
import jd.http.Browser;
import jd.http.requests.GetRequest;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterException;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.WorkuploadCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "workupload.com" }, urls = { "https?://(?:www\\.)?workupload\\.com/archive/([A-Za-z0-9]+)" })
public class WorkuploadComFolder extends PluginForDecrypt {
    public WorkuploadComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        WorkuploadCom.prepBR(br);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        final String folderID = new Regex(contenturl, this.getSupportedLinks()).getMatch(0);
        final WorkuploadCom hosterplugin = (WorkuploadCom) this.getNewPluginForHostInstance(this.getHost());
        hosterplugin.getPage(br, new GetRequest(contenturl));
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String passCode = null;
        Form pwform = this.getPasswordForm(br);
        if (pwform != null) {
            boolean success = false;
            for (int i = 0; i <= 3; i++) {
                passCode = getUserInput("Password?", param);
                pwform.put("passwordprotected_archive%5Bpassword%5D", Encoding.urlEncode(passCode));
                br.submitForm(pwform);
                pwform = this.getPasswordForm(br);
                if (pwform == null) {
                    success = true;
                    break;
                }
            }
            if (!success) {
                throw new DecrypterException(DecrypterException.PASSWORD);
            }
        }
        String foldername = br.getRegex("<td>Archivname[^<]*</td><td>([^<]+)").getMatch(0);
        final String[] htmls = br.getRegex("<div class=\"frame\">.*?class=\"filedownload\"").getColumn(-1);
        if (htmls == null || htmls.length == 0) {
            if (br.containsHTML(folderID + "\\s*0\\.00 B")) {
                /**
                 * 2023-12-01: Broken/offline item e.g. https://workupload.com/archive/2WvJvPpS </br>
                 * -> Folder with single 0kb file linking to itself
                 */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        for (final String singleHTML : htmls) {
            final String url = new Regex(singleHTML, "(/file/[^\"]+)").getMatch(0);
            String filename = new Regex(singleHTML, "class=\"filename[^\"]*?\">\\s*?<p>([^<>\"]+)<").getMatch(0);
            if (filename == null) {
                filename = new Regex(singleHTML, "class=\"filecontent\"[^>]*data-content=\"([^\"]+)\"").getMatch(0);
            }
            final String filesize = new Regex(singleHTML, "class=\"filesize[^\"]*?\">([^<>\"]+)<").getMatch(0);
            if (url == null) {
                logger.warning("Skipping invalid html snippet: " + singleHTML);
                continue;
            }
            final DownloadLink dl = createDownloadlink(br.getURL(url).toExternalForm());
            if (filename != null) {
                dl.setName(filename);
            } else {
                logger.warning("Filename regex failed");
            }
            if (filesize != null) {
                dl.setDownloadSize(SizeFormatter.getSize(filesize));
            } else {
                logger.warning("Filesize regex failed");
            }
            if (passCode != null) {
                /* All single files are protected with the same password. */
                dl.setDownloadPassword(passCode, true);
            }
            dl.setAvailable(true);
            ret.add(dl);
        }
        final FilePackage fp = FilePackage.getInstance();
        if (foldername != null) {
            fp.setName(Encoding.htmlDecode(foldername).trim());
        }
        fp.addLinks(ret);
        return ret;
    }

    private Form getPasswordForm(final Browser br) {
        return br.getFormbyProperty("name", "passwordprotected_archive");
    }
}
