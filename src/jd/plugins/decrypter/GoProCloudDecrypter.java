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

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.CryptedLink;
import jd.plugins.DecrypterPlugin;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.LinkStatus;
import jd.plugins.PluginDependencies;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.gopro.Download;
import jd.plugins.components.gopro.FlexiJSonNodeResponse;
import jd.plugins.components.gopro.GoProConfig;
import jd.plugins.components.gopro.GoProType;
import jd.plugins.components.gopro.GoProVariant;
import jd.plugins.components.gopro.Media;
import jd.plugins.components.gopro.ReflectData;
import jd.plugins.components.gopro.SearchResponse;
import jd.plugins.components.gopro.Variation;
import jd.plugins.hoster.GoProCloud;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.flexijson.FlexiJSONParser;
import org.appwork.storage.flexijson.FlexiJSONParser.ParsingError;
import org.appwork.storage.flexijson.FlexiJSonNode;
import org.appwork.storage.flexijson.FlexiParserException;
import org.appwork.storage.flexijson.mapper.FlexiJSonMapper;
import org.appwork.storage.flexijson.mapper.FlexiMapperException;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.ConcatIterator;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gopro.com" }, urls = { "(https?://plus.gopro.com/media-library/[a-zA-Z0-9]+|https?://plus\\.gopro\\.com/media-library/?$|https?://(?:www\\.)?gopro.com/v/[A-Za-z0-9]+/?(?:[A-Za-z0-9]+)?$)" })
@PluginDependencies(dependencies = { GoProCloud.class })
public class GoProCloudDecrypter extends antiDDoSForDecrypt {
    private GoProConfig hostConfig;

    public GoProCloudDecrypter(PluginWrapper wrapper) {
        super(wrapper);
    }

    public void login(final Browser br, final Account account) throws Exception {
        final PluginForHost plg = this.getNewPluginForHostInstance("gopro.com");
        try {
            ((GoProCloud) plg).login(br, account);
        } catch (PluginException e) {
            handleAccountException(plg, account, e);
            throw e;
        }
    }

    @SuppressWarnings("deprecation")
    public ArrayList<DownloadLink> decryptIt(CryptedLink param, ProgressController progress) throws Exception {
        hostConfig = PluginJsonConfig.get(GoProConfig.class);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        final Account account = AccountController.getInstance().getValidAccount("gopro.com");
        if (param.getCryptedUrl().matches("(?i).*gopro\\.com/v/.+")) {
            decryptSharedLinks(decryptedLinks, account, param);
        } else {
            decryptMedialibrary(decryptedLinks, account, param);
        }
        return decryptedLinks;
    }

    private void decryptSharedLinks(List<DownloadLink> decryptedLinks, Account account, CryptedLink param) throws IOException, FlexiParserException, FlexiMapperException, PluginException {
        FlexiJSonMapper mapper = new FlexiJSonMapper();
        final String singleID = new Regex(param.getCryptedUrl(), ".*/v/[^/]+/([^/]+)$").getMatch(0);
        br.getPage(param.getCryptedUrl());
        String json = br.getRegex("<script>window.__reflectData=(.*)").getMatch(0);
        FlexiJSONParser parser = new FlexiJSONParser(json).breakAtEndOfObject().ignoreIssues(new HashSet<ParsingError>(FlexiJSONParser.IGNORE_LIST_JS));
        FlexiJSonNode node = parser.parse();
        ReflectData resp = mapper.jsonToObject(node, ReflectData.TYPEREF);
        for (Media m : resp.getCollectionMedia()) {
            if (singleID != null && !StringUtils.equals(singleID, m.getId())) {
                continue;
            }
            scanID(decryptedLinks, param, mapper, m.getId(), m, "free", resp.getCollection().getTitle());
        }
    }

    protected void decryptMedialibrary(final ArrayList<DownloadLink> decryptedLinks, final Account account, CryptedLink cryptedLink) throws Exception, IOException, FlexiParserException, FlexiMapperException {
        FlexiJSonMapper mapper = new FlexiJSonMapper();
        String id = new Regex(cryptedLink.getCryptedUrl(), ".*/media-library/([^/]+)").getMatch(0);
        if (account != null) {
            login(this.br, account);
        } else {
            return;
        }
        if (StringUtils.isNotEmpty(id)) {
            scanID(decryptedLinks, cryptedLink, mapper, id, null, "premium", null);
        } else {
            int perPage = 100;
            int page = 1;
            int loginRetries = 0;
            Set<GoProType> toCrawl = getTypesToCrawl();
            String list = "";
            HashSet<String> ids = new HashSet<String>();
            for (GoProType e : toCrawl) {
                ids.add(e.apiID);
            }
            list = StringUtils.join(ids, ",");
            long scanUntil = 0;
            if (hostConfig.getOnlyScanLastXDaysFromLibrary() > 0) {
                scanUntil = System.currentTimeMillis() - hostConfig.getOnlyScanLastXDaysFromLibrary() * 24 * 60 * 60 * 1000l;
            }
            Date scanReadable = new Date(scanUntil);
            while (!isAbort()) {
                br.getHeaders().put("accept", "application/vnd.gopro.jk.media+json; version=2.0.0");
                String json = br.getPage("https://api.gopro.com/media/search?fields=camera_model,captured_at,content_title,content_type,created_at,gopro_user_id,gopro_media,filename,file_size,height,fov,id,item_count,moments_count,on_public_profile,orientation,play_as,ready_to_edit,ready_to_view,resolution,source_duration,token,type,width&processing_states=registered,rendering,pretranscoding,transcoding,failure,ready&order_by=captured_at&per_page=" + perPage + "&page=" + page + "&type=" + list.toString());
                FlexiJSonNode node = new FlexiJSONParser(json).parse();
                SearchResponse resp = mapper.jsonToObject(node, SearchResponse.TYPEREF);
                if (StringUtils.isNotEmpty(resp.getError())) {
                    if (loginRetries > 0) {
                        throw new WTFException();
                    }
                    loginRetries++;
                    br.clearAll();
                    account.removeProperty(GoProCloud.ACCESS_TOKEN);
                    login(this.br, account);
                    continue;
                }
                Media[] media = resp.getEmbedded().getMedia();
                for (Media m : media) {
                    // System.out.println(m.getCaptured_at() + " - " + scanReadable);
                    if (m.getCaptured_at().getTime() < scanUntil) {
                        return;
                    }
                    if (isAbort()) {
                        return;
                    }
                    scanID(decryptedLinks, cryptedLink, mapper, m.getId(), m, "premium", null);
                }
                if (page >= resp.getPages().getTotal_pages()) {
                    break;
                }
                page++;
            }
        }
    }

    public Set<GoProType> getTypesToCrawl() {
        Set<GoProType> toCrawl = hostConfig.getTypesToCrawl();
        if (toCrawl == null || toCrawl.size() == 0) {
            toCrawl = new HashSet<GoProType>(Arrays.asList(GoProType.values()));
        }
        return toCrawl;
    }

    public void finalizeLink(DownloadLink link) {
        super.distribute(link);
    }

    @Override
    public CrawledLink convert(DownloadLink link) {
        CrawledLink ret = super.convert(link);
        if (hostConfig.isKeepLinksInLinkgrabber()) {
            // TODO
            // ret.setSticky(StickyOption.STICKY);
        }
        return ret;
    }

    protected void scanID(final List<DownloadLink> decryptedLinks, CryptedLink cryptedLink, FlexiJSonMapper mapper, String id, Media media, String access, String collectionTitle) throws FlexiParserException, IOException, FlexiMapperException, MalformedURLException, PluginException {
        DownloadLink cacheSource = null;
        FlexiJSonNodeResponse responseMediaDownload = null;
        FlexiJSonNodeResponse responseMedia = null;
        if (hostConfig.isLocalMediaCacheEnabled()) {
            synchronized (jd.plugins.hoster.GoProCloud.LINKCACHE) {
                for (Entry<DownloadLink, String> es : jd.plugins.hoster.GoProCloud.LINKCACHE.entrySet()) {
                    if (es.getValue().equals(id)) {
                        cacheSource = es.getKey();
                        break;
                    }
                }
            }
        }
        repeat: for (int i = 0; i < 2; i++) {
            if (media == null) {
                responseMedia = GoProCloud.getMediaResponse(this, br, id, cacheSource);
                media = mapper.jsonToObject(responseMedia.jsonNode, Media.TYPEREF);
            }
            final FilePackage fp = FilePackage.getInstance();
            fp.setName("GoPro " + new SimpleDateFormat("yyyy-MM-dd").format(media.getCaptured_at()));
            if (collectionTitle != null) {
                fp.setName("GoPro Collection " + collectionTitle);
            }
            HashSet<String> dupe = new HashSet<String>();
            ConcatIterator<Variation> iter;
            Set<GoProType> toCrawl = getTypesToCrawl();
            try {
                if ("Burst".equals(media.getType())) {
                    if (!toCrawl.contains(GoProType.BurstAsZipPackage) && !toCrawl.contains(GoProType.BurstAsSingleImages)) {
                        return;
                    }
                } else if (!toCrawl.contains(GoProType.valueOf(media.getType()))) {
                    return;
                }
            } catch (IllegalArgumentException e) {
                logger.log(e);
            }
            if ("Burst".equals(media.getType())) {
                responseMediaDownload = GoProCloud.getDownloadResponse(this, br, id, cacheSource);
                final Download download = mapper.jsonToObject(responseMediaDownload.jsonNode, Download.TYPEREF);
                iter = new ConcatIterator<Variation>(download.getEmbedded().getSidecar_files(), download.getEmbedded().getFiles());
                for (Variation v : iter) {
                    if (isAbort()) {
                        return;
                    }
                    try {
                        String variantID = v.getLabel();
                        if (StringUtils.isEmpty(variantID)) {
                            int digits = (int) (Math.log10(media.getItem_count()) + 1);
                            variantID = StringUtils.fillPre(v.getItem_number() + "", "0", digits);
                            if (!toCrawl.contains(GoProType.BurstAsSingleImages)) {
                                continue;
                            }
                        }
                        if ("zip".equals(variantID)) {
                            if (!toCrawl.contains(GoProType.BurstAsZipPackage)) {
                                continue;
                            }
                        } else {
                            if (!toCrawl.contains(GoProType.BurstAsSingleImages)) {
                                continue;
                            }
                        }
                        if (!dupe.add(v.getUrl())) {
                            continue;
                        }
                        DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/" + variantID);
                        link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                        GoProCloud.setCache(link, responseMedia.jsonString, responseMediaDownload.jsonString);
                        setContentUrl(cryptedLink, id, access, link);
                        link.setAvailable(true);
                        if (!setFileSize(id, v, link, cacheSource)) {
                            if (cacheSource != null && i == 0) {
                                GoProCloud.clearDownloadCache(cacheSource);
                                continue repeat;
                            }
                        }
                        fp.add(link);
                        GoProCloud.setFinalFileName(hostConfig, media, link, v);
                        finalizeLink(link);
                        decryptedLinks.add(link);
                    } finally {
                    }
                }
            } else if ("Video".equals(media.getType()) || "TimeLapseVideo".equals(media.getType()) || "BurstVideo".equals(media.getType())) {
                if (hostConfig.isCrawlDownscaledVariants()) {
                    responseMediaDownload = GoProCloud.getDownloadResponse(this, br, id, cacheSource);
                    final Download download = mapper.jsonToObject(responseMediaDownload.jsonNode, Download.TYPEREF);
                    ArrayList<GoProVariant> variants = new ArrayList<GoProVariant>();
                    Collections.sort(download.getEmbedded().getVariations(), new Comparator<Variation>() {
                        @Override
                        public int compare(Variation o1, Variation o2) {
                            return CompareUtils.compare(o2.getHeight(), o1.getHeight());
                        }
                    });
                    for (Variation v : download.getEmbedded().getVariations()) {
                        if (isAbort()) {
                            return;
                        }
                        String variantID = v.getLabel();
                        if (StringUtils.isEmpty(variantID)) {
                            continue;
                        }
                        GoProVariant newVariant;
                        if (!dupe.add(v.getHeight() + "")) {
                            continue;
                        }
                        if (hostConfig.isUseOriginalGoProFileNames()) {
                            newVariant = (new GoProVariant(v.getHeight() + "p" + "_" + variantID, variantID + "_" + v.getHeight()));
                            variants.add(newVariant);
                        } else {
                            if ("source".equals(variantID)) {
                                newVariant = (new GoProVariant("Highest(Source)", "source"));
                                variants.add(newVariant);
                            }
                            newVariant = (new GoProVariant(v.getHeight() + "p", v.getHeight() + "p"));
                            variants.add(newVariant);
                        }
                        if (hostConfig.isAddEachVariantAsExtraLink()) {
                            DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/" + variantID);
                            link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                            GoProCloud.setCache(link, responseMedia.jsonString, responseMediaDownload.jsonString);
                            setContentUrl(cryptedLink, id, access, link);
                            GoProCloud.setFinalFileName(hostConfig, media, link, v);
                            fp.add(link);
                            link.setAvailable(true);
                            if (!setFileSize(id, v, link, cacheSource)) {
                                if (cacheSource != null && i == 0) {
                                    GoProCloud.clearDownloadCache(cacheSource);
                                    continue repeat;
                                }
                            }
                            finalizeLink(link);
                            decryptedLinks.add(link);
                        }
                    }
                    if (!hostConfig.isAddEachVariantAsExtraLink()) {
                        DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/video");
                        GoProCloud.setCache(link, responseMedia.jsonString, responseMediaDownload.jsonString);
                        setContentUrl(cryptedLink, id, access, link);
                        link.setFinalFileName(media.getFilename());
                        link.setVariantSupport(true);
                        fp.add(link);
                        link.setVariants(variants);
                        link.setAvailable(true);
                        link.setDownloadSize(media.getFile_size());
                        GoProCloud.setFinalFileName(hostConfig, media, link, download.getEmbedded().getVariations().get(0));
                        link.setVariant(variants.get(0));
                        link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, variants.get(0)));
                        finalizeLink(link);
                        decryptedLinks.add(link);
                    }
                } else {
                    DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/source");
                    link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                    setContentUrl(cryptedLink, id, access, link);
                    GoProCloud.setCache(link, responseMedia.jsonString, responseMediaDownload.jsonString);
                    link.setAvailable(false);
                    GoProCloud.setFinalFileName(hostConfig, media, link, null);
                    // if (hostConfig.isUseOriginalGoProFileNames()) {
                    // link.setFinalFileName(media.getFilename());
                    // } else {
                    // link.setFinalFileName(Files.getFileNameWithoutExtension(media.getFilename()) + "_" + media.getHeight() + "p" + "." +
                    // Files.getExtension(media.getFilename()));
                    // }
                    fp.add(link);
                    link.setAvailable(true);
                    link.setDownloadSize(media.getFile_size());
                    finalizeLink(link);
                    decryptedLinks.add(link);
                }
            } else if ("Photo".equals(media.getType())) {
                try {
                    String variantID = "source";
                    DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/" + variantID);
                    link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                    setContentUrl(cryptedLink, id, access, link);
                    GoProCloud.setCache(link, responseMedia.jsonString, responseMediaDownload.jsonString);
                    link.setAvailable(true);
                    link.setDownloadSize(media.getFile_size());
                    fp.add(link);
                    GoProCloud.setFinalFileName(hostConfig, media, link, null);
                    // if (hostConfig.isUseOriginalGoProFileNames()) {
                    // link.setFinalFileName(media.getFilename());
                    // } else {
                    // link.setFinalFileName(Files.getFileNameWithoutExtension(media.getFilename()) + "_" + media.getHeight() + "p" + "." +
                    // Files.getExtension(media.getFilename()));
                    // }
                    finalizeLink(link);
                    decryptedLinks.add(link);
                } finally {
                }
            } else {
                throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Media Type:" + media.getType());
            }
            return;
        }
    }

    protected boolean setFileSize(String id, Variation v, DownloadLink link, DownloadLink cacheSource) throws IOException {
        link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
        if (hostConfig.isLocalMediaCacheEnabled()) {
            synchronized (jd.plugins.hoster.GoProCloud.LINKCACHE) {
                for (Entry<DownloadLink, String> es : jd.plugins.hoster.GoProCloud.LINKCACHE.entrySet()) {
                    if (es.getKey().getLinkID().equals(link.getLinkID())) {
                        link.setDownloadSize(es.getKey().getDownloadSize());
                        break;
                    }
                }
            }
        }
        if (link.getDownloadSize() <= 0 && !hostConfig.isImproveCrawlerSpeedBySkippingSizeCheck()) {
            URLConnectionAdapter connection = null;
            try {
                connection = br.openHeadConnection(v.getHead());
                if (connection.getResponseCode() == 200) {
                    link.setDownloadSize(connection.getCompleteContentLength());
                } else {
                    br.followConnection(true);
                    return false;
                }
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
            }
        }
        return true;
    }

    protected void setContentUrl(CryptedLink cryptedLink, String id, String access, DownloadLink link) {
        if ("premium".equals(access)) {
            link.setContentUrl("https://plus.gopro.com/media-library/" + id);
        } else {
            String root = new Regex(cryptedLink.getCryptedUrl(), "(^http.*/v/[^/]+)").getMatch(0);
            link.setContentUrl(root + "/" + id);
        }
    }
}
