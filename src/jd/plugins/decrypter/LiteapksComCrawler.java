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

import org.appwork.utils.formatter.SizeFormatter;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class LiteapksComCrawler extends PluginForDecrypt {
    public LiteapksComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "liteapks.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([\\w\\-]+\\.html|download/[\\w\\-]+-\\d+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String downloadOverviewPatternStr = "/download/[\\w\\-]+-\\d+";
        if (!param.getCryptedUrl().matches(".+" + downloadOverviewPatternStr) && !br.getURL().matches(".+" + downloadOverviewPatternStr)) {
            final String downloadOverviewUrl = br.getRegex(downloadOverviewPatternStr).getMatch(-1);
            if (downloadOverviewUrl == null) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            br.getPage(downloadOverviewUrl);
        } else {
            final String postedYearsAgoStr = br.getRegex("(\\d+) years ago").getMatch(0);
            if (postedYearsAgoStr != null && Integer.parseInt(postedYearsAgoStr) > 30) {
                /* Dummy page for invalid items */
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
        }
        String title = br.getRegex("<title>([^<]+)</title>").getMatch(0);
        if (title == null) {
            /* Fallback */
            title = br._getURL().getPath().substring(1);
        }
        title = Encoding.htmlDecode(title).trim();
        title = title.replaceFirst("(?i) Download$", "");
        final String[] filesizes = br.getRegex("<span class=\"text-muted d-block ml-auto\"[^>]*>([^<]+)</span>").getColumn(0);
        final String[] links = br.getRegex("(/download/[\\w+\\-]+/\\d+)\"").getColumn(0);
        if (links == null || links.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        int index = 0;
        for (final String singleLink : links) {
            final DownloadLink link = createDownloadlink(br.getURL(singleLink).toString());
            /* Set temporary filename */
            link.setName(br._getURL().getPath() + "_ " + (index + 1) + ".apk");
            link.setAvailable(true);
            if (filesizes != null && links.length == filesizes.length) {
                final String filesizeStr = filesizes[index];
                link.setDownloadSize(SizeFormatter.getSize(filesizeStr));
            }
            ret.add(link);
            index++;
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(title);
        fp.addLinks(ret);
        return ret;
    }
}
