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

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.MissavCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class MissavComCrawler extends PluginForDecrypt {
    public MissavComCrawler(PluginWrapper wrapper) {
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
        ret.add(new String[] { "missav.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/[a-z]{2}/([A-Za-z0-9\\-]+)");
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final MissavCom hosterplugin = (MissavCom) this.getNewPluginForHostInstance(this.getHost());
        final DownloadLink selfhostedVideo = this.createDownloadlink(param.getCryptedUrl());
        selfhostedVideo.setDefaultPlugin(hosterplugin);
        hosterplugin.requestFileInformation(selfhostedVideo);
        /* Prevent hosterplugin from checking this link again. No Exception during check = Item is online. */
        selfhostedVideo.setAvailable(true);
        ret.add(selfhostedVideo);
        /* Search- and add external downloadlinks */
        final String[] links = br.getRegex("<a href=\"(https?://[^\"]+)\" target=\"_blank\" rel=\"nofollow noopener\"").getColumn(0);
        if (links != null && links.length > 0) {
            for (final String singleLink : links) {
                ret.add(createDownloadlink(singleLink));
            }
        }
        final FilePackage fp = FilePackage.getInstance();
        fp.setName(selfhostedVideo.getName());
        fp.addLinks(ret);
        return ret;
    }
}
