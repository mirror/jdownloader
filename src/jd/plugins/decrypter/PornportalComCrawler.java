//jDownloader - Downloadmanager
//Copyright (C) 2013  JD-Team support@jdownloader.org
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import org.appwork.utils.StringUtils;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;
import jd.plugins.components.SiteType.SiteTemplate;
import jd.plugins.hoster.PornportalCom;
import jd.utils.JDUtilities;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 2, names = {}, urls = {})
public class PornportalComCrawler extends PluginForDecrypt {
    public PornportalComCrawler(PluginWrapper wrapper) {
        super(wrapper);
    }

    public static List<String[]> getPluginDomains() {
        final List<String[]> ret = new ArrayList<String[]>();
        // each entry in List<String[]> will result in one PluginForDecrypt, Plugin.getHost() will return String[0]->main domain
        ret.add(new String[] { "babes.com" });
        ret.add(new String[] { "brazzers.com" });
        ret.add(new String[] { "digitalplayground.com" });
        ret.add(new String[] { "erito.com" });
        ret.add(new String[] { "fakehub.com" });
        ret.add(new String[] { "mofos.com" });
        ret.add(new String[] { "realitykings.com" });
        ret.add(new String[] { "sexyhub.com" });
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
            // final String annotationName = domains[0];
            /* Premium URLs */
            String pattern = "https?://site-ma\\." + buildHostsPatternPart(domains) + "/(?:trailer|scene|series)/(\\d+)(/[a-z0-9\\-]+)?";
            /* Free URLs */
            pattern += "|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/(?:scene|series)/(\\d+)(/[a-z0-9\\-]+)?";
            // if (annotationName.equals("digitalplayground.com")) {
            // pattern += "|https?://(?:www\\.)?" + buildHostsPatternPart(domains) + "/scene/(\\d+)(/[a-z0-9\\-]+)?";
            // }
            ret.add(pattern);
        }
        return ret.toArray(new String[0]);
    }

    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final String parameter = param.toString();
        /* Login if possible */
        final boolean isLoggedIN = getUserLogin();
        if (!isLoggedIN) {
            /* Anonymous API auth */
            logger.info("No account given --> Trailer download");
            if (!PornportalCom.prepareBrAPI(this, br, null)) {
                logger.info("Getting fresh API data");
                br.getPage("https://site-ma." + Browser.getHost(parameter, false) + "/login");
                if (!PornportalCom.prepareBrAPI(this, br, null)) {
                    logger.warning("Failed to set required API headers");
                    return null;
                }
            }
        }
        final String videoID = new Regex(parameter, "(?:trailer|scene|series)/(\\d+)").getMatch(0);
        if (videoID == null) {
            return null;
        }
        final LinkedHashMap<String, DownloadLink> qualities = crawlVideoAPI(videoID);
        final Iterator<Entry<String, DownloadLink>> iteratorQualities = qualities.entrySet().iterator();
        while (iteratorQualities.hasNext()) {
            decryptedLinks.add(iteratorQualities.next().getValue());
        }
        return decryptedLinks;
    }

    public boolean isProxyRotationEnabledForLinkCrawler() {
        return false;
    }

    private boolean getUserLogin() throws Exception {
        final PluginForHost hostPlugin = JDUtilities.getPluginForHost(this.getHost());
        final Account aa = AccountController.getInstance().getValidAccount(hostPlugin);
        if (aa != null) {
            try {
                ((jd.plugins.hoster.PornportalCom) hostPlugin).login(this.br, aa, false);
                return true;
            } catch (final PluginException e) {
                handleAccountException(aa, e);
            }
        }
        return false;
    }

    private LinkedHashMap<String, DownloadLink> crawlVideoAPI(final String videoID) throws Exception {
        final LinkedHashMap<String, DownloadLink> foundQualities = new LinkedHashMap<String, DownloadLink>();
        String api_base = PluginJSonUtils.getJson(br, "dataApiUrl");
        if (StringUtils.isEmpty(api_base)) {
            /* Fallback to static value e.g. loggedIN --> html containing json API information has not been accessed before */
            api_base = "https://site-api.project1service.com";
        }
        br.getPage(api_base + "/v2/releases/" + videoID);
        /* TODO: Check offline errorhandling */
        if (br.getHttpConnection().getResponseCode() == 404) {
            throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND);
        }
        LinkedHashMap<String, Object> entries = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.jsonToJavaMap(br.toString());
        entries = (LinkedHashMap<String, Object>) entries.get("result");
        final ArrayList<Object> videoObjects = new ArrayList<Object>();
        /* Add current object */
        videoObjects.add(entries);
        /* Look for more objects */
        final Object childrenO = entries.get("children");
        if (childrenO != null) {
            final ArrayList<Object> children = (ArrayList<Object>) entries.get("children");
            videoObjects.addAll(children);
        }
        for (final Object videoO : videoObjects) {
            entries = (LinkedHashMap<String, Object>) videoO;
            // final String type = (String) entries.get("type");
            String title = (String) entries.get("title");
            String description = (String) entries.get("description");
            if (StringUtils.isEmpty(title)) {
                /* Fallback */
                title = videoID;
            } else if (title.equalsIgnoreCase("trailer")) {
                title = videoID + "_trailer";
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName(title);
            if (!StringUtils.isEmpty(description)) {
                fp.setComment(description);
            }
            final String format_filename = "%s_%s.mp4";
            LinkedHashMap<String, Object> files = null;
            try {
                entries = (LinkedHashMap<String, Object>) entries.get("videos");
                files = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "full/files");
                if (files == null) {
                    /* E.g. not logged in --> Trailer */
                    files = (LinkedHashMap<String, Object>) JavaScriptEngineFactory.walkJson(entries, "mediabook/files");
                }
                if (files == null) {
                    /* Skip non-video objects */
                    continue;
                }
            } catch (final Throwable e) {
                /* Skip non-video objects */
                continue;
            }
            final Iterator<Entry<String, Object>> qualities = files.entrySet().iterator();
            while (qualities.hasNext()) {
                final Entry<String, Object> entry = qualities.next();
                LinkedHashMap<String, Object> videoInfo = (LinkedHashMap<String, Object>) entry.getValue();
                String format = (String) videoInfo.get("format");
                final long filesize = JavaScriptEngineFactory.toLong(videoInfo.get("sizeBytes"), 0);
                videoInfo = (LinkedHashMap<String, Object>) videoInfo.get("urls");
                String downloadurl = (String) videoInfo.get("download");
                if (StringUtils.isEmpty(downloadurl)) {
                    /* Fallback to stream-URL */
                    downloadurl = (String) videoInfo.get("view");
                }
                if (StringUtils.isEmpty(downloadurl)) {
                    continue;
                } else if (StringUtils.isEmpty(format) || !format.matches("\\d+p")) {
                    /* Skip invalid entries and hls and dash streams */
                    continue;
                }
                /* E.g. '1080p' --> '1080' */
                format = format.replace("p", "");
                final DownloadLink dl = this.createDownloadlink("directhttp://" + downloadurl);
                dl.setFinalFileName(String.format(format_filename, title, format));
                dl.setProperty("fid", videoID);
                dl.setProperty("quality", format);
                if (filesize > 0) {
                    dl.setDownloadSize(filesize);
                }
                dl.setAvailable(true);
                dl._setFilePackage(fp);
                // if (!loggedin) {
                // dl.setProperty("free_downloadable", true);
                // }
                foundQualities.put(format, dl);
            }
            /* TODO: Quality selection would have to be here */
        }
        return foundQualities;
    }

    public static String getProtocol() {
        return "https://";
    }

    @Override
    public SiteTemplate siteTemplateType() {
        return SiteTemplate.PornPortal;
    }
}