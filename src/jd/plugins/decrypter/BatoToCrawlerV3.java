//jDownloader - Downloadmanager
//Copyright (C) 2014  JD-Team support@jdownloader.org
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

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.appwork.storage.TypeRef;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.controller.LazyPlugin;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.nutils.encoding.Encoding;
import jd.parser.html.HTMLSearch;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.hoster.BatoTo;
import jd.plugins.hoster.DirectHTTP;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = {}, urls = {})
@PluginDependencies(dependencies = { BatoTo.class })
/** This crawler is for bato.to website version v3. */
public class BatoToCrawlerV3 extends PluginForDecrypt {
    public BatoToCrawlerV3(PluginWrapper wrapper) {
        super(wrapper);
        /* Prevent server response 503! */
        BatoTo.setRequestLimits();
    }

    @Override
    public LazyPlugin.FEATURE[] getFeatures() {
        return new LazyPlugin.FEATURE[] { LazyPlugin.FEATURE.IMAGE_GALLERY, LazyPlugin.FEATURE.IMAGE_HOST };
    }

    public static List<String[]> getPluginDomains() {
        return BatoTo.getPluginDomains();
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
            ret.add("https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/title/\\d+-[a-z0-9\\-]+(/\\d+-(vol_\\d+-)?ch_\\d+)?");
        }
        return ret.toArray(new String[0]);
    }

    public int getMaxConcurrentProcessingInstances() {
        /* Prevent server response 503! */
        return 1;
    }

    @SuppressWarnings("deprecation")
    @Override
    public ArrayList<DownloadLink> decryptIt(final CryptedLink param, ProgressController progress) throws Exception {
        final ArrayList<DownloadLink> ret = new ArrayList<DownloadLink>();
        br.setFollowRedirects(true);
        br.setCookiesExclusive(true);
        /* Login if possible */
        final PluginForHost hostPlugin = this.getNewPluginForHostInstance(this.getHost());
        final Account acc = AccountController.getInstance().getValidAccount(hostPlugin);
        if (acc != null) {
            ((jd.plugins.hoster.BatoTo) hostPlugin).login(acc, false);
        }
        br.getPage(param.getCryptedUrl());
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        final String urlpath = br._getURL().getPath();
        String title = HTMLSearch.searchMetaTag(br, "og:title");
        if (title != null) {
            title = Encoding.htmlDecode(title);
            title = title.replace(" - Read Free Manga Online at Bato", "");
        }
        final String[] propsStrings = br.getRegex("props=\"([^\"]+)").getColumn(0);
        for (final String propsString : propsStrings) {
            final String json = Encoding.htmlOnlyDecode(propsString);
            final Object parsedObject = restoreFromString(json, TypeRef.OBJECT);
            if (!(parsedObject instanceof Map)) {
                continue;
            }
            final Map<String, Object> map = (Map<String, Object>) parsedObject;
            final List<Object> imageFiles = (List<Object>) map.get("imageFiles");
            if (imageFiles != null) {
                final FilePackage fp = FilePackage.getInstance();
                if (title != null) {
                    fp.setName(title);
                } else {
                    /* Fallback */
                    fp.setName(urlpath);
                }
                final String imageJsonEncoded = imageFiles.get(1).toString();
                final String imageJson = Encoding.htmlOnlyDecode(imageJsonEncoded);
                final List<Object> imagesO = (List<Object>) restoreFromString(imageJson, TypeRef.OBJECT);
                final int padLength = StringUtils.getPadLength(imagesO.size());
                int position = 1;
                for (final Object imageO : imagesO) {
                    final List<Object> imageArray = (List<Object>) imageO;
                    final String urlEncoded = imageArray.get(1).toString();
                    final String url = Encoding.htmlOnlyDecode(urlEncoded);
                    final DownloadLink image = this.createDownloadlink(DirectHTTP.createURLForThisPlugin(url));
                    final String filenameFromURL = Plugin.getFileNameFromURL(new URL(url));
                    if (filenameFromURL != null) {
                        image.setFinalFileName(StringUtils.formatByPadLength(padLength, position) + "_" + filenameFromURL);
                    }
                    image.setAvailable(true);
                    image._setFilePackage(fp);
                    image.setLinkID("batoto://chapter/" + urlpath + "/image/" + position);
                    ret.add(image);
                    position++;
                }
                return ret;
            }
        }
        logger.info("Failed to find images of a single chapter");
        /* No results were found so check if we got a series of which we want to find the URLs to all chapters */
        final String[] chapterurls = br.getRegex("(" + Pattern.quote(urlpath) + "/\\d+-(vol_\\d+-)?ch_\\d+)").getColumn(0);
        if (chapterurls == null || chapterurls.length == 0) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        for (final String chapterurl : chapterurls) {
            ret.add(this.createDownloadlink(br.getURL(chapterurl).toExternalForm()));
        }
        return ret;
    }

    @Override
    public boolean hasCaptcha(CryptedLink link, jd.plugins.Account acc) {
        return false;
    }
}