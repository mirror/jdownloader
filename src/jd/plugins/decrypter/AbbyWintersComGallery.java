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
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.hoster.AbbyWintersCom;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { AbbyWintersCom.class })
public class AbbyWintersComGallery extends PluginForDecrypt {
    public AbbyWintersComGallery(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        return AbbyWintersCom.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([\\w\\-]+)/([\\w\\-]+)/([\\w\\-]+)/images/stills");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_IMAGES = "https?://[^/]+/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+/images/stills";
    private final String TYPE_MIXED  = "https?://[^/]+/[\\w\\-]+/[\\w\\-]+/[\\w\\-]+$";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        final AbbyWintersCom hosterPlugin = (AbbyWintersCom) this.getNewPluginForHostInstance(this.getHost());
        final Regex imageUrl = new Regex(param.getCryptedUrl(), TYPE_IMAGES);
        if (imageUrl.matches()) {
            br.getPage(param.getCryptedUrl());
            if (br.getHttpConnection().getResponseCode() == 404) {
                throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
            }
            String fpName = br.getRegex("").getMatch(0);
            final String[] imgurls = br.getRegex("class=\"lazyload\" data-src=\"(https?://[^\"]+)").getColumn(0);
            if (imgurls == null || imgurls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            for (final String imageurl : imgurls) {
                final DownloadLink link = createDownloadlink(imageurl);
                link.setAvailable(true);
                ret.add(link);
            }
            if (fpName != null) {
                final FilePackage fp = FilePackage.getInstance();
                fp.setName(Encoding.htmlDecode(fpName).trim());
                fp.addLinks(ret);
            }
        }
        return ret;
    }
}
