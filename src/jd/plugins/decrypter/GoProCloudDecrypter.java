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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import jd.PluginWrapper;
import jd.controlling.AccountController;
import jd.controlling.ProgressController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.parser.Regex;
import jd.plugins.Account;
import jd.plugins.AccountRequiredException;
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

import org.appwork.storage.flexijson.FlexiJSONParser;
import org.appwork.storage.flexijson.FlexiJSonNode;
import org.appwork.storage.flexijson.FlexiUtils;
import org.appwork.storage.flexijson.ParsingError;
import org.appwork.storage.flexijson.mapper.FlexiJSonMapper;
import org.appwork.storage.flexijson.mapper.interfacestorage.InterfaceStorage;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.ConcatIterator;
import org.appwork.utils.DebugMode;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.antiDDoSForDecrypt;
import org.jdownloader.plugins.config.PluginJsonConfig;

@DecrypterPlugin(revision = "$Revision$", interfaceVersion = 3, names = { "gopro.com" }, urls = { "(https?://(?:plus\\.)?gopro\\.com/media-library/[a-zA-Z0-9]+|https?://(?:plus\\.)?gopro\\.com/media-library/?$|https?://(?:www\\.)?gopro\\.com/v/[A-Za-z0-9]+/?(?:[A-Za-z0-9]+)?$)" })
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
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>() {
            @Override
            public boolean add(DownloadLink e) {
                final boolean ret = super.add(e);
                distribute(e);
                return ret;
            }
        };
        final Account account = AccountController.getInstance().getValidAccount("gopro.com");
        if (param.getCryptedUrl().matches("(?i).*gopro\\.com/v/.+")) {
            decryptSharedLinks(decryptedLinks, account, param);
        } else {
            decryptMedialibrary(decryptedLinks, account, param);
        }
        return decryptedLinks;
    }

    private void decryptSharedLinks(List<DownloadLink> decryptedLinks, Account account, CryptedLink param) throws Exception {
        final FlexiJSonMapper mapper = new FlexiJSonMapper();
        final String singleID = new Regex(param.getCryptedUrl(), ".*/v/[^/]+/([^/]+)$").getMatch(0);
        br.getPage(param.getCryptedUrl());
        final String json = br.getRegex("<script>window.__reflectData=(.*)").getMatch(0);
        final FlexiJSONParser parser = new FlexiJSONParser(json).breakAtEndOfObject().ignoreIssues(new HashSet<ParsingError>(FlexiJSONParser.IGNORE_LIST_JS));
        final FlexiJSonNode node = parser.parse();
        final ReflectData resp = mapper.jsonToObject(node, ReflectData.TYPEREF);
        for (Media m : resp.getCollectionMedia()) {
            if (singleID != null && !StringUtils.equals(singleID, m.getId())) {
                continue;
            } else if (isAbort()) {
                return;
            } else {
                scanID(decryptedLinks, account, param, mapper, m.getId(), m, "free", resp.getCollection().getTitle());
            }
        }
    }

    protected void decryptMedialibrary(final ArrayList<DownloadLink> decryptedLinks, final Account account, CryptedLink cryptedLink) throws Exception {
        final FlexiJSonMapper mapper = new FlexiJSonMapper();
        String id = new Regex(cryptedLink.getCryptedUrl(), "(?i).*/media-library/([^/]+)").getMatch(0);
        if (account == null) {
            throw new AccountRequiredException();
        }
        login(this.br, account);
        if (StringUtils.isNotEmpty(id) && !StringUtils.equalsIgnoreCase(id, "links")) {
            scanID(decryptedLinks, account, cryptedLink, mapper, id, null, "premium", null);
        } else {
            int perPage = 100;
            int page = 1;
            Set<GoProType> toCrawl = getTypesToCrawl();
            String list = "";
            HashSet<String> ids = new HashSet<String>();
            for (GoProType e : toCrawl) {
                ids.add(e.apiID);
            }
            list = StringUtils.join(ids, ",");
            long scanUntil = -1;
            final int onlyScanLastXDaysFromLibrary = hostConfig.getOnlyScanLastXDaysFromLibrary();
            if (onlyScanLastXDaysFromLibrary > 0) {
                scanUntil = System.currentTimeMillis() - (onlyScanLastXDaysFromLibrary * 24l * 60 * 60 * 1000l);
            }
            while (!isAbort()) {
                // it is important to update requested fields as we cache Media response
                final Request request = GoProCloud.doAPIRequest(this, account, br, "https://api.gopro.com/media/search?fields=camera_model,captured_at,content_title,content_type,created_at,gopro_user_id,gopro_media,filename,file_size,file_extension,height,fov,id,item_count,moments_count,on_public_profile,orientation,play_as,ready_to_edit,ready_to_view,resolution,source_duration,token,type,width&processing_states=registered,rendering,pretranscoding,transcoding,failure,ready&order_by=captured_at&per_page=" + perPage + "&page=" + page + "&type=" + list.toString());
                final String json = request.getHtmlCode();
                final FlexiJSonNode node = new FlexiJSONParser(json).parse();
                final SearchResponse resp = mapper.jsonToObject(node, SearchResponse.TYPEREF);
                if (StringUtils.isNotEmpty(resp.getError())) {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
                }
                final Media[] media = resp.getEmbedded().getMedia();
                for (Media m : media) {
                    // System.out.println(m.getCaptured_at() + " - " + scanReadable);
                    if (scanUntil > 0 && m.getCaptured_at().getTime() < scanUntil) {
                        return;
                    } else if (isAbort()) {
                        return;
                    } else {
                        scanID(decryptedLinks, account, cryptedLink, mapper, m.getId(), m, "premium", null);
                    }
                }
                if (page >= resp.getPages().getTotal_pages()) {
                    return;
                } else {
                    page++;
                }
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

    @Override
    public CrawledLink convert(DownloadLink link) {
        CrawledLink ret = super.convert(link);
        // if (hostConfig.isKeepLinksInLinkgrabber()) {
        // TODO
        // ret.setSticky(StickyOption.STICKY);
        // }
        return ret;
    }

    protected void scanID(final List<DownloadLink> decryptedLinks, final Account account, CryptedLink cryptedLink, FlexiJSonMapper mapper, String id, Media media, String access, String collectionTitle) throws Exception {
        DownloadLink cacheSource = null;
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
        try {
            final HashSet<String> dupe = new HashSet<String>();
            repeat: for (int i = 0; i < 2; i++) {
                if (media == null) {
                    responseMedia = GoProCloud.getMediaResponse(this, account, br, id, cacheSource);
                    media = mapper.jsonToObject(responseMedia.jsonNode, Media.TYPEREF);
                }
                if (responseMedia == null && media != null) {
                    responseMedia = new FlexiJSonNodeResponse(InterfaceStorage.get(media).backendNode, FlexiUtils.serializeToPrettyJson(media));
                }
                final Set<GoProType> toCrawl = getTypesToCrawl();
                try {
                    if ("Burst".equals(media.getType())) {
                        if (!toCrawl.contains(GoProType.BurstAsZipPackage) && !toCrawl.contains(GoProType.BurstAsSingleImages)) {
                            return;
                        }
                    } else if ("TimeLapse".equals(media.getType())) {
                        if (!toCrawl.contains(GoProType.TimeLapseAsZipPackage) && !toCrawl.contains(GoProType.TimeLapseAsSingleImages)) {
                            return;
                        } else if (!DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                            // could not find any working TimeLapse images/zips
                            return;
                        }
                    } else if (!toCrawl.contains(GoProType.valueOf(media.getType()))) {
                        logger.info("Skipped MediaType: " + media.getType() + " (Disabled in Config)");
                        return;
                    }
                } catch (IllegalArgumentException e) {
                    logger.log(e);
                }
                final FilePackage fp = FilePackage.getInstance();
                fp.setName("GoPro " + new SimpleDateFormat("yyyy-MM-dd").format(media.getCaptured_at()));
                if (collectionTitle != null) {
                    fp.setName("GoPro Collection " + collectionTitle);
                }
                fp.setIgnoreVarious(Boolean.TRUE);
                if ("Burst".equals(media.getType()) || "TimeLapse".equals(media.getType())) {
                    final GoProType singleImageType;
                    final GoProType zipPackageType;
                    if ("Burst".equals(media.getType())) {
                        singleImageType = GoProType.BurstAsSingleImages;
                        zipPackageType = GoProType.BurstAsZipPackage;
                    } else if ("TimeLapse".equals(media.getType())) {
                        singleImageType = GoProType.TimeLapseAsSingleImages;
                        zipPackageType = GoProType.TimeLapseAsZipPackage;
                    } else {
                        throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Media Type:" + media.getType());
                    }
                    final FlexiJSonNodeResponse responseMediaDownload = GoProCloud.getDownloadResponse(this, account, br, id, cacheSource);
                    final Download download = mapper.jsonToObject(responseMediaDownload.jsonNode, Download.TYPEREF);
                    final ConcatIterator<Variation> iter = new ConcatIterator<Variation>(download.getEmbedded().getSidecar_files(), download.getEmbedded().getFiles());
                    for (Variation v : iter) {
                        if (isAbort()) {
                            return;
                        }
                        String variantID = v.getLabel();
                        if ("zip".equals(variantID)) {
                            if (!toCrawl.contains(zipPackageType)) {
                                continue;
                            }
                        } else if (!toCrawl.contains(singleImageType)) {
                            continue;
                        } else if (dupe.contains(v.getUrl())) {
                            continue;
                        } else if ("raw_photo".equals(variantID)) {
                            // https://github.com/gopro/gpr
                            // "type":"gpr"
                            // "label":"raw_photo"
                            // TODO: add support for raw_photo and jpg. right now variantID does not distinguish type (jpg or gpr...)
                            continue;
                        }
                        if (StringUtils.isEmpty(variantID)) {
                            final int digits = (int) (Math.log10(media.getItem_count()) + 1);
                            variantID = StringUtils.fillPre(v.getItem_number() + "", "0", digits);
                        }
                        final DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/" + variantID);
                        link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                        GoProCloud.setCache(link, responseMedia != null ? responseMedia.jsonString : null, responseMediaDownload != null ? responseMediaDownload.jsonString : null);
                        setContentUrl(cryptedLink, id, access, link);
                        link.setAvailable(true);
                        if (!setFileSize(id, v, link, cacheSource)) {
                            if (cacheSource != null && i == 0) {
                                GoProCloud.clearDownloadCache(cacheSource);
                                continue repeat;
                            }
                        }
                        fp.add(link);
                        GoProCloud.setFinalFileName(this, hostConfig, media, link, v);
                        decryptedLinks.add(link);
                        dupe.add(v.getUrl());
                    }
                } else if ("Video".equals(media.getType()) || "TimeLapseVideo".equals(media.getType()) || "BurstVideo".equals(media.getType()) || "MultiClipEdit".equals(media.getType())) {
                    if (hostConfig.isCrawlDownscaledVariants()) {
                        final FlexiJSonNodeResponse responseMediaDownload = GoProCloud.getDownloadResponse(this, account, br, id, cacheSource);
                        final Download download = mapper.jsonToObject(responseMediaDownload.jsonNode, Download.TYPEREF);
                        final ArrayList<GoProVariant> variants = new ArrayList<GoProVariant>();
                        Collections.sort(download.getEmbedded().getVariations(), new Comparator<Variation>() {
                            @Override
                            public int compare(Variation o1, Variation o2) {
                                return CompareUtils.compareInt(o2.getHeight(), o1.getHeight());
                            }
                        });
                        for (Variation v : download.getEmbedded().getVariations()) {
                            if (isAbort()) {
                                return;
                            }
                            final String variantID = GoProCloud.createVideoVariantID(v, media);
                            if (StringUtils.isEmpty(variantID)) {
                                continue;
                            } else if (dupe.contains(Integer.toString(v.getHeight()) + "_" + Integer.toString(v.getItem_number()))) {
                                continue;
                            }
                            if ("concat".equals(v.getLabel())) {
                                final GoProVariant sourceVariant = (new GoProVariant("Highest(" + v.getHeight() + "p" + ")" + " (Merged full length)", variantID + "_" + v.getHeight()));
                                variants.add(sourceVariant);
                            } else if (v.getItem_number() > 0) {
                                final GoProVariant newVariant = (new GoProVariant(v.getHeight() + "p" + " (Part " + v.getItem_number() + "/" + media.getItem_count() + ")", variantID + "_" + v.getHeight()));
                                variants.add(newVariant);
                            } else if (media.getItem_count() > 1 && v.getItem_number() == 0) {
                                final GoProVariant newVariant = (new GoProVariant(v.getHeight() + "p" + " (Merged full length)", variantID + "_" + v.getHeight()));
                                variants.add(newVariant);
                            } else {
                                if (variantID.contains("source")) {
                                    // source, baked_source,...
                                    final GoProVariant sourceVariant = (new GoProVariant("Highest(" + v.getHeight() + "p" + ")", "source"));
                                    variants.add(sourceVariant);
                                }
                                final GoProVariant newVariant = (new GoProVariant(v.getHeight() + "p", variantID + "_" + v.getHeight()));
                                variants.add(newVariant);
                            }
                            if (hostConfig.isAddEachVariantAsExtraLink()) {
                                final DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/" + variantID);
                                link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                                GoProCloud.setCache(link, responseMedia != null ? responseMedia.jsonString : null, responseMediaDownload != null ? responseMediaDownload.jsonString : null);
                                setContentUrl(cryptedLink, id, access, link);
                                GoProCloud.setFinalFileName(this, hostConfig, media, link, v);
                                fp.add(link);
                                link.setAvailable(true);
                                if (!setFileSize(id, v, link, cacheSource)) {
                                    if (cacheSource != null && i == 0) {
                                        GoProCloud.clearDownloadCache(cacheSource);
                                        continue repeat;
                                    }
                                }
                                decryptedLinks.add(link);
                                dupe.add(Integer.toString(v.getHeight()));
                            }
                        }
                        if (!hostConfig.isAddEachVariantAsExtraLink()) {
                            final DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/video");
                            GoProCloud.setCache(link, responseMedia != null ? responseMedia.jsonString : null, responseMediaDownload != null ? responseMediaDownload.jsonString : null);
                            setContentUrl(cryptedLink, id, access, link);
                            link.setFinalFileName(media.getFilename());
                            link.setVariantSupport(variants.size() > 1);
                            fp.add(link);
                            link.setVariants(variants);
                            link.setAvailable(true);
                            GoProVariant activeVariant = variants.get(0);
                            for (Variation v : download.getEmbedded().getVariations()) {
                                String variantID = v.getLabel() + "_" + v.getItem_number();
                                variantID += "_" + v.getHeight();
                                if (StringUtils.equals(variantID, activeVariant.getId())) {
                                    GoProCloud.setFinalFileName(this, hostConfig, media, link, v);
                                    if (media.getFile_size() > 0) {
                                        if (v.getItem_number() == 0) {
                                            link.setDownloadSize(media.getFile_size());
                                        }
                                    }
                                } else if ("source".equals(activeVariant.getId()) && variantID.contains("source")) {
                                    GoProCloud.setFinalFileName(this, hostConfig, media, link, v);
                                    if (media.getFile_size() > 0) {
                                        link.setDownloadSize(media.getFile_size());
                                    }
                                }
                            }
                            link.setVariant(activeVariant);
                            link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, activeVariant));
                            decryptedLinks.add(link);
                        }
                    } else {
                        final DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/source");
                        link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                        setContentUrl(cryptedLink, id, access, link);
                        GoProCloud.setCache(link, responseMedia != null ? responseMedia.jsonString : null, null);
                        link.setAvailable(false);
                        GoProCloud.setFinalFileName(this, hostConfig, media, link, null);
                        fp.add(link);
                        link.setAvailable(true);
                        link.setDownloadSize(media.getFile_size());
                        decryptedLinks.add(link);
                    }
                } else if ("Photo".equals(media.getType())) {
                    final DownloadLink link = createDownloadlink("https://gopro.com/download" + access + "/" + id + "/source");
                    link.setLinkID(jd.plugins.hoster.GoProCloud.createLinkID(link, null));
                    setContentUrl(cryptedLink, id, access, link);
                    GoProCloud.setCache(link, responseMedia != null ? responseMedia.jsonString : null, null);
                    link.setAvailable(true);
                    link.setDownloadSize(media.getFile_size());
                    fp.add(link);
                    GoProCloud.setFinalFileName(this, hostConfig, media, link, null);
                    decryptedLinks.add(link);
                } else {
                    throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT, "Unknown Media Type:" + media.getType());
                }
                return;
            }
        } catch (final PluginException e) {
            if (DebugMode.TRUE_IN_IDE_ELSE_FALSE) {
                throw e;
            } else {
                logger.log(e);
            }
        }
    }

    protected boolean setFileSize(String id, Variation v, DownloadLink link, DownloadLink cacheSource) throws IOException {
        final String linkID = jd.plugins.hoster.GoProCloud.createLinkID(link, null);
        link.setLinkID(linkID);
        if (hostConfig.isLocalMediaCacheEnabled()) {
            synchronized (jd.plugins.hoster.GoProCloud.LINKCACHE) {
                for (Entry<DownloadLink, String> es : jd.plugins.hoster.GoProCloud.LINKCACHE.entrySet()) {
                    final DownloadLink next = es.getKey();
                    if (linkID.equals(next.getLinkID()) && next.getKnownDownloadSize() > 0) {
                        link.setDownloadSize(next.getKnownDownloadSize());
                        break;
                    }
                }
            }
        }
        if (link.getKnownDownloadSize() <= 0 && !hostConfig.isImproveCrawlerSpeedBySkippingSizeCheck()) {
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
