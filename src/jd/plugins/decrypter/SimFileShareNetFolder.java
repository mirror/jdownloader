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
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.SimFileShareNet;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SimFileShareNetFolder extends PluginForDecrypt {
    public SimFileShareNetFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        return SimFileShareNet.getPluginDomains();
    }

    public static String[] getAnnotationNames() {
        return buildAnnotationNames(getPluginDomains());
    }

    @Override
    public String[] siteSupportedNames() {
        return buildSupportedNames(getPluginDomains());
    }

    public static String[] getAnnotationUrls() {
        return buildAnnotationUrls(getPluginDomains());
    }

    public static String[] buildAnnotationUrls(final List<String[]> pluginDomains) {
        final List<String> ret = new ArrayList<String>();
        for (final String[] domains : pluginDomains) {
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/folder/(\\d+)/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.getPage(param.getCryptedUrl());
        final String folderID = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (br.containsHTML(">\\s*No files in this folder")) {
            throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
        }
        final String parentFolderurl = br.getRegex(">\\s*Back to parent:\\s*</strong> <a href=\"(/folder/\\d+/?)\"").getMatch(0);
        String currentFolderName = br.getRegex("<h4>Folder: ([^<]+)</h4>").getMatch(0);
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        if (subfolderPath == null) {
            /* No existing path available -> Use current folder name as root */
            subfolderPath = currentFolderName;
        } else {
            /* Append name of current folder to existing path */
            if (subfolderPath != null) {
                subfolderPath += "/" + currentFolderName;
            }
        }
        if (currentFolderName != null) {
            currentFolderName = Encoding.htmlDecode(currentFolderName).trim();
        }
        FilePackage fp = null;
        if (subfolderPath != null) {
            fp = FilePackage.getInstance();
            fp.setName(subfolderPath);
        }
        final String[] fileurls = br.getRegex("(/download/\\d+/?)").getColumn(0);
        if (fileurls != null && fileurls.length > 0) {
            for (final String fileurl : fileurls) {
                final Regex fileinfo = br.getRegex(Pattern.quote(fileurl) + "\">([^<]+)</a>\\s*</td>\\s*<td>([^<]+)</td>");
                final DownloadLink file = createDownloadlink(br.getURL(fileurl).toExternalForm());
                if (fileinfo.patternFind()) {
                    file.setName(Encoding.htmlDecode(fileinfo.getMatch(0)));
                    file.setDownloadSize(SizeFormatter.getSize(fileinfo.getMatch(1)));
                }
                file.setAvailable(true);
                if (subfolderPath != null) {
                    file.setRelativeDownloadFolderPath(subfolderPath);
                }
                if (fp != null) {
                    file._setFilePackage(fp);
                }
                ret.add(file);
            }
        }
        final String[] subfolderurls = br.getRegex("(/folder/\\d+/?)").getColumn(0);
        if (subfolderurls != null && subfolderurls.length > 0) {
            for (final String subfolderurl : subfolderurls) {
                if (subfolderurl.contains(folderID)) {
                    /* This is the folder we are currently crawling -> Do not return it again! */
                    continue;
                } else if (parentFolderurl != null && subfolderurl.contains(parentFolderurl)) {
                    /* Do not add parent folder (only go deeper into the folder structure, not higher up). */
                    continue;
                }
                final DownloadLink subfolder = createDownloadlink(br.getURL(subfolderurl).toExternalForm());
                if (subfolderPath != null) {
                    subfolder.setRelativeDownloadFolderPath(subfolderPath);
                }
                ret.add(subfolder);
            }
        }
        if (ret.isEmpty()) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        return ret;
    }

    @Override
    public int getMaxConcurrentProcessingInstances() {
        return 1;
    }
}
