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

import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.config.SolidFilesComConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Request;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DecrypterRetryException;
import jd.plugins.DecrypterRetryException.RetryReason;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.SolidFilesCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
@PluginDependencies(dependencies = { SolidFilesCom.class })
public class SolidFilesComFolder extends PluginForDecrypt {
    public SolidFilesComFolder(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return SolidFilesCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:folder|v)/[A-Za-z0-9]+/?");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.openGetConnection(param.getCryptedUrl());
        if (this.looksLikeDownloadableContent(br.getHttpConnection())) {
            br.getHttpConnection().disconnect();
            // direct downloadable
            final DownloadLink dl = createDownloadlink(param.getCryptedUrl());
            dl.setProperty(SolidFilesCom.PROPERTY_DIRECT_DOWNLOAD, true);
            final String fileName = getFileNameFromConnection(br.getHttpConnection());
            if (fileName != null) {
                dl.setFinalFileName(fileName);
            }
            dl.setVerifiedFileSize(br.getHttpConnection().getContentLength());
            dl.setAvailable(true);
            ret.add(dl);
            return ret;
        }
        br.followConnection();
        if (br.getHttpConnection().getResponseCode() == 404 || br.containsHTML("(?i)>\\s*Not found\\s*<|>\\s*We couldn't find the file you requested|>\\s*This folder is empty\\.<|This file/folder has been disabled")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String currentFolderTitle = br.getRegex("(?i)<title>([^<>\"]*?)(?:-|\\|) Solidfiles</title>").getMatch(0);
        if (currentFolderTitle != null) {
            currentFolderTitle = br.getRegex("<h1 class=\"node-name\">([^<]+)</h1>").getMatch(0);
        }
        if (currentFolderTitle == null) {
            /* Final fallback */
            currentFolderTitle = new Regex(param.getCryptedUrl(), "([a-z0-9]+)/$").getMatch(0);
        }
        currentFolderTitle = Encoding.htmlDecode(currentFolderTitle).trim();
        String subfolderPath = this.getAdoptedCloudFolderStructure();
        if (subfolderPath == null) {
            subfolderPath = currentFolderTitle;
        } else {
            subfolderPath += "/" + currentFolderTitle;
        }
        String filelist = br.getRegex("<ul>(.+?)</ul>").getMatch(0);
        String[] finfos = new Regex(filelist, "(<a href=(?:'|\"|).*?</a>)").getColumn(0);
        if (finfos == null || finfos.length == 0) {
            if (br.containsHTML("id=\"file-list\"")) {
                logger.info("Empty folder: " + param.getCryptedUrl());
                if (!StringUtils.isEmpty(subfolderPath)) {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER, subfolderPath);
                } else {
                    throw new DecrypterRetryException(RetryReason.EMPTY_FOLDER);
                }
            } else {
                // single file-let hoster plugin handle this
                ret.add(this.createDownloadlink(param.getCryptedUrl()));
                return ret;
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(subfolderPath);
        for (final String finfo : finfos) {
            final Regex urlfilename = new Regex(finfo, "<a href=(\"|')(/(?:d|v|folder)/.*?)\\1.*?>([^<>]+)</a>");
            String url = urlfilename.getMatch(1);
            String title = urlfilename.getMatch(2);
            // final String filesize = new Regex(finfo, "(\\d+(?:\\.\\d+)? ?(bytes|KB|MB|GB))").getMatch(0);
            if (url == null || title == null) {
                logger.info("finfo: " + finfo);
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            url = Request.getLocation(url, br.getRequest());
            if (url.contains("/folder/")) {
                if (PluginJsonConfig.get(SolidFilesComConfig.class).isFolderCrawlerCrawlSubfolders()) {
                    final DownloadLink folder = createDownloadlink(url);
                    folder.setRelativeDownloadFolderPath(subfolderPath);
                    ret.add(folder);
                }
            } else {
                final DownloadLink file = createDownloadlink(url);
                title = Encoding.htmlDecode(title).trim();
                file.setName(title.replace(" ", "_"));
                // dl.setDownloadSize(SizeFormatter.getSize(filesize));
                file.setAvailable(true);
                file.setRelativeDownloadFolderPath(subfolderPath);
                file._setFilePackage(fp);
                ret.add(file);
            }
        }
        return ret;
    }

    public boolean hasCaptcha(final CryptedLink link, final jd.plugins.Account acc) {
        return false;
    }
}