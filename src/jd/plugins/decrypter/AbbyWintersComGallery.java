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
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.Regex;
import jd.parser.html.HTMLParser;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
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

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.XXX };
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/([\\w\\-]+/[\\w\\-]+/[\\w\\-]+/images/stills|[\\w\\-]+/[\\w\\-]+/[\\w\\-]+)$");
        }
        return ret.toArray(new String[0]);
    }

    private final String TYPE_IMAGES = "https?://[^/]+/([\\w\\-]+)/([\\w\\-]+)/([\\w\\-]+)/images/stills";
    private final String TYPE_MIXED  = "https?://[^/]+/([\\w\\-]+)/([\\w\\-]+)/([\\w\\-]+)$";

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        br.setFollowRedirects(true);
        final Account account = AccountController.getInstance().getValidAccount(this.getHost());
        return decryptIt(param, account);
    }

    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, final Account account) throws Exception {
        if (account == null) {
            throw new AccountRequiredException();
        }
        final AbbyWintersCom hosterPlugin = (AbbyWintersCom) this.getNewPluginForHostInstance(this.getHost());
        hosterPlugin.login(account, param.getCryptedUrl(), true);
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        final Regex imageUrlRegex = new Regex(param.getCryptedUrl(), TYPE_IMAGES);
        if (imageUrlRegex.matches()) {
            final String fpName = (imageUrlRegex.getMatch(0) + " - " + imageUrlRegex.getMatch(1) + " - " + imageUrlRegex.getMatch(2)).replace("_", " ");
            final String[] imgurls = br.getRegex("class=\"card-thumb clearfix\" href=\"(https?://[^\"]+)").getColumn(0);
            if (imgurls == null || imgurls.length == 0) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(Encoding.htmlDecode(fpName).trim());
            int position = 0;
            for (String imageurl : imgurls) {
                imageurl = Encoding.htmlDecode(imageurl);
                final DownloadLink link = new DownloadLink(hosterPlugin, Plugin.getFileNameFromURL(imageurl), this.getHost(), imageurl, true);
                link._setFilePackage(fp);
                link.setAvailable(true);
                link.setLinkID(this.getHost() + "://image/" + br._getURL().getPath() + position);
                link.setProperty(AbbyWintersCom.PROPERTY_MAINLINK, param.getCryptedUrl());
                link.setProperty(AbbyWintersCom.PROPERTY_IMAGE_POSITION, position);
                ret.add(link);
                position++;
            }
        } else {
            final String[] urls = HTMLParser.getHttpLinks(br.getRequest().getHtmlCode(), br.getURL());
            for (final String url : urls) {
                final Regex videoRegex = new Regex(url, AbbyWintersCom.PATTERN_VIDEO);
                if (videoRegex.matches()) {
                    final DownloadLink video = this.createDownloadlink(url);
                    video.setName((videoRegex.getMatch(0) + " - " + videoRegex.getMatch(1) + " - " + videoRegex.getMatch(2)).replace("_", " ") + ".mp4");
                    video.setAvailable(true);
                    ret.add(video);
                } else if (url.matches(TYPE_IMAGES)) {
                    ret.add(this.createDownloadlink(url));
                }
            }
            if (ret.isEmpty()) {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
            }
        }
        return ret;
    }
}
