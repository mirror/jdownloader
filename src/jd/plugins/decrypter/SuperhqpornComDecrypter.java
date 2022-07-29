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

import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
public class SuperhqpornComDecrypter extends PornEmbedParser {
    public SuperhqpornComDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.XXX };
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "superhqporn.com" });
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/\\?v=([A-Z0-9]+)");
        }
        return ret.toArray(new String[0]);
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        String externID = null;
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        String parameter = param.toString();
        br.setFollowRedirects(false);
        br.getPage(parameter);
        final DownloadLink main = createDownloadlink(parameter.replace("superhqporn.com/", "superhqporndecrypted.com/"));
        main.setContentUrl(parameter);
        if (br.containsHTML(">Video Not Exists<|>Requested video not exist") || br.getHttpConnection().getResponseCode() == 404) {
            main.setAvailable(false);
            main.setProperty("offline", true);
            decryptedLinks.add(main);
            return decryptedLinks;
        }
        externID = br.getRedirectLocation();
        if (externID != null && !externID.contains("superhqporn.com/")) {
            decryptedLinks.add(createDownloadlink(externID));
            return decryptedLinks;
        } else if (externID != null) {
            br.getPage(externID);
            externID = null;
        }
        decryptedLinks.addAll(findEmbedUrls());
        if (!decryptedLinks.isEmpty()) {
            return decryptedLinks;
        }
        /* No extern id found --> Add main-link */
        decryptedLinks.add(main);
        return decryptedLinks;
    }

    @Override
    protected String getFileTitle(final CryptedLink param, final Browser br) {
        return jd.plugins.hoster.SuperHqPornCom.getFileTitle(br);
    }

    @Override
    protected boolean isOffline(final Browser br) {
        return jd.plugins.hoster.SuperHqPornCom.isOffline(br);
    }

    @Override
    protected boolean returnRedirectToUnsupportedLinkAsResult() {
        return true;
    }

    @Override
    protected boolean assumeSelfhostedContentOnNoResults() {
        return false;
    }
}
