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
import java.util.Collections;
import java.util.List;

import org.appwork.utils.StringUtils;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class WCOForever extends PluginForDecrypt {
    public WCOForever(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public Browser createNewBrowserInstance() {
        final Browser br = super.createNewBrowserInstance();
        br.setFollowRedirects(true);
        return br;
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "wcoforever.tv", "wcoforever.net", "wcoforever.com" });
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
            ret.add("https?://(?:www[0-9]*\\.)?" + buildHostsPatternPart(domains) + "/(?:anime/)?.+$");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        String fpName = br.getRegex("<title>(?:Watch\\s+)?([^<]+)Online\\s+-\\s+").getMatch(0);
        if (StringUtils.isEmpty(fpName)) {
            fpName = br.getRegex("<title>\\s*([^<]+)\\s+\\w+\\s+[DSds]ubbed\\s*(?:\\s+-\\s+|<)").getMatch(0);
        }
        final ArrayList<String> links = new ArrayList<String>();
        Collections.addAll(links, br.getRegex("<div[^>]*class\\s*=\\s*\"[^\"]*cat-eps[^\"]*\"[^>]*>\\s*<a[^>]*href\\s*=\\s*\"([^\"]+)\"").getColumn(0));
        Collections.addAll(links, br.getRegex("(?i)Is the video too slow[^<]+<a[^>]+href\\s*=\\s*\"([^\"]+)\"").getColumn(0));
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        for (String link : links) {
            if (StringUtils.isEmpty(link)) {
                continue;
            }
            link = Encoding.htmlDecode(link).replaceAll("^//", "https://");
            ret.add(createDownloadlink(link));
        }
        final FilePackage fp = FilePackage.getInstance();
        if (fpName != null) {
            fp.setName(Encoding.htmlDecode(fpName).trim());
        } else {
            /* Fallback */
            fp.setName(br._getURL().getPath());
        }
        fp.addLinks(ret);
        return ret;
    }
}