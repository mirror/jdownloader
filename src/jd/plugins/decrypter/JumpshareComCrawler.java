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
import jd.parser.html.Form;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "jumpshare.com" }, urls = { "https?://(?:www\\.)?(?:jmp\\.sh/(?!v/)[A-Za-z0-9]+|jumpshare\\.com/b/[A-Za-z0-9/]+)" })
public class JumpshareComCrawler extends PluginForDecrypt {
    public JumpshareComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    private static final String TYPE_FOLDER = "https?://(?:www\\.)?jumpshare\\.com/b/([A-Za-z0-9/]+)";
    private static final String TYPE_FILE   = "https?://(?:www\\.)?jumpshare\\.com/v/([A-Za-z0-9]+)";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String parameter = param.getCryptedUrl();
        String path = param.getDownloadLink() != null ? param.getDownloadLink().getRelativeDownloadFolderPath() : null;
        if (parameter.matches(TYPE_FOLDER)) {
            this.br.setFollowRedirects(true);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404 || this.br.containsHTML("Folder Not Found|The folder you are looking for does not exist")) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String folderpathFromURL = new Regex(parameter, "([A-Za-z0-9]+)$").getMatch(0);
            final Form passwordForm = br.getFormbyProperty("id", "folder-unlock-form");
            if (passwordForm != null) {
                logger.warning("Password protected folders are not yet supported");
                throw new DecrypterRetryException(RetryReason.PASSWORD, "UNSUPPORTED_PASSWORD_PROTECTED_FOLDER_" + folderpathFromURL, "Password protected folders of this website aren't supported yet. Contact our support and ask for implementation!");
            }
            String thisFolderTitle = br.getRegex("property=\"og:title\" content=\"([^<>]+)\"").getMatch(0);
            if (thisFolderTitle != null) {
                thisFolderTitle = Encoding.htmlDecode(thisFolderTitle).trim();
                if (path != null) {
                    path += "/" + thisFolderTitle;
                } else {
                    path = thisFolderTitle;
                }
            }
            final String[] fileids = br.getRegex("/v/([A-Za-z0-9]+)").getColumn(0);
            if (fileids != null && fileids.length > 0) {
                for (final String fileid : fileids) {
                    final String url = br.getURL("/v/" + fileid).toExternalForm();
                    /* Contenturl for the user to copy - let's use the same urls they use in browser. */
                    final String url_content = url + "?b=";
                    final DownloadLink dl = createDownloadlink(url);
                    dl.setContentUrl(url_content);
                    if (path != null) {
                        dl.setRelativeDownloadFolderPath(path);
                    }
                    ret.add(dl);
                }
            }
            final String[] subfolderurls = br.getRegex("(/b/" + folderpathFromURL + "/[A-Za-z0-9]+)").getColumn(0);
            if (subfolderurls != null && subfolderurls.length > 0) {
                for (final String subfolderurl : subfolderurls) {
                    final String url = br.getURL(subfolderurl).toExternalForm();
                    final DownloadLink dl = createDownloadlink(url);
                    if (path != null) {
                        dl.setRelativeDownloadFolderPath(path);
                    }
                    ret.add(dl);
                }
            }
            if (ret.isEmpty()) {
                if (br.containsHTML("class=\"content_fluid is-empty\"")) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
            }
            final FilePackage fp = FilePackage.getInstance();
            if (path != null) {
                fp.setName(path);
            } else {
                fp.setName(folderpathFromURL);
            }
            fp.addLinks(ret);
        } else {
            this.br.setFollowRedirects(false);
            br.getPage(parameter);
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            final String finallink = this.br.getRedirectLocation();
            if (finallink == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            /* Validate result */
            if (!finallink.matches(TYPE_FOLDER) && !finallink.matches(TYPE_FILE) && finallink.contains(this.getHost())) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final DownloadLink singlefile = createDownloadlink(finallink);
            if (path != null) {
                singlefile.setRelativeDownloadFolderPath(path);
            }
            ret.add(singlefile);
        }
        return ret;
    }
}
