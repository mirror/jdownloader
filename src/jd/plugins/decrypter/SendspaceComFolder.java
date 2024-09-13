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
import java.util.regex.Pattern;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.http.Request;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.StringUtils;
import org.appwork.utils.formatter.SizeFormatter;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = { "sendspace.com" }, urls = { "https?://(?:www\\.)?sendspace\\.com/(?:folder/[0-9a-zA-Z]+|filegroup/([0-9a-zA-Z%]+))" })
public class SendspaceComFolder extends PluginForDecrypt {
    public SendspaceComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final String contenturl = param.getCryptedUrl().replaceFirst("(?i)http://", "https://");
        br.getPage(contenturl);
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(404 Page Not Found|It has either been moved)")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*This folder is currently empty")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        if (StringUtils.containsIgnoreCase(contenturl, "/filegroup/")) {
            // return pro or file links
            final String[] results = br.getRegex("<div class=\"groupedFile\">.*?</div>\\s*</div>").getColumn(-1);
            if (results != null && results.length > 0) {
                for (final String result : results) {
                    final Regex fs = new Regex(result, "<h4><b>(.*?)</b>\\s*(.*?)</h4>");
                    final String filename = fs.getMatch(0);
                    final String filesize = fs.getMatch(1);
                    final String url = new Regex(result, "<a [^>]*href=(\"|'|)(.*?)\\1").getMatch(1);
                    final DownloadLink dl = createDownloadlink(Request.getLocation(url, br.getRequest()));
                    dl.setName(Encoding.htmlDecode(filename).trim());
                    dl.setDownloadSize(SizeFormatter.getSize(filesize.trim().replaceAll(",", ".")));
                    dl.setAvailable(true);
                    ret.add(dl);
                }
                final String fpName = "Multiple Downloads " + JDHash.getCRC32(Encoding.urlDecode(new Regex(contenturl, this.getSupportedLinks()).getMatch(0), false));
                if (fpName != null) {
                    FilePackage fp = FilePackage.getInstance();
                    fp.setName(fpName.trim());
                    fp.addLinks(ret);
                }
            }
        } else {
            /* "Normal" folder */
            String path = this.getAdoptedCloudFolderStructure();
            String thisFolderTitle = null;
            final String[] h1Fields = br.getRegex("<h1 class=\"droid\"[^>]*>([^<]+)</h1>").getColumn(0);
            if (h1Fields != null && h1Fields.length > 0) {
                thisFolderTitle = h1Fields[h1Fields.length - 1];
            }
            if (thisFolderTitle == null) {
                thisFolderTitle = br.getRegex("rel=\"alternate\" TYPE=\"application/rss\\+xml\" title=\"([^\"]+)").getMatch(0);
            }
            if (thisFolderTitle != null) {
                thisFolderTitle = Encoding.htmlDecode(thisFolderTitle).trim();
            }
            if (path != null) {
                path += "/" + thisFolderTitle;
            } else {
                path = thisFolderTitle;
            }
            final FilePackage fp = FilePackage.getInstance();
            if (path != null) {
                fp.setName(path);
            }
            final PluginForHost hosterplugin = this.getNewPluginForHostInstance(this.getHost());
            final String[] filesizes = br.getRegex("</td>\\s*<td>\\s*(\\d+[^<]+)</td>").getColumn(0);
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            int fileindex = 0;
            for (final String url : urls) {
                if (hosterplugin.canHandle(url)) {
                    final String filename = br.getRegex("title=\"Click to download ([^\"]+)\" target=\"_blank\" href=\"" + Pattern.quote(url)).getMatch(0);
                    final DownloadLink file = this.createDownloadlink(url);
                    if (filename != null) {
                        file.setName(Encoding.htmlDecode(filename).trim());
                    }
                    if (filesizes != null && fileindex < filesizes.length) {
                        final String filesizeStr = filesizes[fileindex];
                        file.setDownloadSize(SizeFormatter.getSize(filesizeStr));
                    }
                    file.setAvailable(true);
                    if (path != null) {
                        file.setRelativeDownloadFolderPath(path);
                    }
                    file._setFilePackage(fp);
                    ret.add(file);
                    fileindex++;
                } else if (new Regex(url, "^" + this.getSupportedLinks().pattern()).patternFind()) {
                    /* Subfolder */
                    final DownloadLink subfolder = this.createDownloadlink(url);
                    if (path != null) {
                        subfolder.setRelativeDownloadFolderPath(path);
                    }
                    ret.add(subfolder);
                }
            }
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}