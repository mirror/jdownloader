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

import org.appwork.utils.formatter.SizeFormatter;

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

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MajorgeeksComCrawler extends PluginForDecrypt {
    public MajorgeeksComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "majorgeeks.com" });
        return ret;
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/files/details/([^/]+)\\.html");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        } else if (!br.containsHTML("report_a_bad_link\\.html") && !br.containsHTML("/SoftwareApplication")) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String titleSlug = new Regex(param.getCryptedUrl(), this.getSupportedLinks()).getMatch(0);
        final String filesizeStr = br.getRegex("itemprop=\"fileSize\" content=\"([^<>\"]+)\"").getMatch(0);
        final Long filesize = filesizeStr != null ? SizeFormatter.getSize(filesizeStr) : null;
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(titleSlug);
        final String[] links = br.getRegex("(mg/get/[^\",]+,\\d+\\.html)").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String singleLink : links) {
            final String fullURL = br.getURL("/" + singleLink).toString();
            final String thisSoftwareVariantTitle = br.getRegex("(?i)" + Pattern.quote(singleLink) + "\"><strong>Download([^<]+)</strong>").getMatch(0);
            if (thisSoftwareVariantTitle == null && singleLink.endsWith(",1.html")) {
                logger.info("Skipping invalid URL: " + fullURL);
                continue;
            }
            final DownloadLink link = createDownloadlink(fullURL);
            final String slug = singleLink.substring(singleLink.lastIndexOf("/")).replace("-html", "");
            if (thisSoftwareVariantTitle != null) {
                link.setName(slug + "_" + thisSoftwareVariantTitle);
            } else {
                link.setName(slug);
            }
            if (filesize != null) {
                link.setDownloadSize(filesize.longValue());
            }
            link.setAvailable(true);
            link._setFilePackage(fp);
            ret.add(link);
        }
        return ret;
    }
}
