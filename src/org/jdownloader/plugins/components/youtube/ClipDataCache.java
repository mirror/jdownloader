package org.jdownloader.plugins.components.youtube;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

public class ClipDataCache {
    public static final String  THE_DOWNLOAD_IS_NOT_AVAILABLE_IN_YOUR_COUNTRY = "The Download is not available in your country";
    private static final Object LOCK                                          = new Object();

    private static class CachedClipData {
        private volatile YoutubeClipData clipData = null;
        private final List<HTTPProxy>    proxyList;

        public CachedClipData(List<HTTPProxy> proxyListNew, YoutubeClipData youtubeClipData) {
            this.clipData = youtubeClipData;
            proxyList = proxyListNew;
        }

        protected void setYoutubeClipData(YoutubeClipData clipData) {
            this.clipData = clipData;
        }

        public boolean hasValidProxyList(List<HTTPProxy> validateList) {
            if (proxyList != null && validateList != null) {
                for (final HTTPProxy proxy : proxyList) {
                    if (!validateList.contains(proxy)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }
    }

    private static final HashMap<String, MinTimeWeakReference<CachedClipData>> CACHE = new HashMap<String, MinTimeWeakReference<CachedClipData>>();

    public static YoutubeClipData get(YoutubeHelper helper, DownloadLink downloadLink) throws Exception {
        final String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        final CachedClipData ret = getInternal(helper, videoID);
        ret.clipData.copyToDownloadLink(downloadLink);
        // put a reference to the link. if we remove all links with the ref, the cache will cleanup it self
        downloadLink.getTempProperties().setProperty("CLIP_DATA_REFERENCE", ret);
        return ret.clipData;
    }

    public static YoutubeClipData get(YoutubeHelper helper, String videoID) throws Exception {
        return getInternal(helper, videoID).clipData;
    }

    private static MinTimeWeakReferenceCleanup CLEANUP = new MinTimeWeakReferenceCleanup() {
                                                           @Override
                                                           public void onMinTimeWeakReferenceCleanup(MinTimeWeakReference<?> minTimeWeakReference) {
                                                               synchronized (LOCK) {
                                                                   CACHE.remove(minTimeWeakReference.getID());
                                                               }
                                                           }
                                                       };

    private static CachedClipData getInternal(YoutubeHelper helper, YoutubeClipData vid) throws Exception {
        synchronized (LOCK) {
            String cachedID = vid.videoID;
            MinTimeWeakReference<CachedClipData> ref = CACHE.get(cachedID);
            CachedClipData cachedData = ref == null ? null : ref.get();
            final List<HTTPProxy> proxyListNew = helper.getBr().selectProxies(YOUTUBE_URL);
            if (cachedData != null) {
                if (!cachedData.hasValidProxyList(proxyListNew)) {
                    cachedData = null;
                } else if (StringUtils.isEmpty(cachedData.clipData.title)) {
                    cachedData = null;
                } else if (cachedData.clipData.date == 0) {
                    cachedData = null;
                }
            }
            if (cachedData == null) {
                cachedData = new CachedClipData(proxyListNew, vid);
                helper.loadVideo(cachedData.clipData);
                ref = new MinTimeWeakReference<CachedClipData>(cachedData, 15000, cachedID, CLEANUP);
                CACHE.put(cachedID, ref);
            }
            if (cachedData.clipData.streams == null || StringUtils.isNotEmpty(cachedData.clipData.error)) {
                if (StringUtils.equalsIgnoreCase(cachedData.clipData.error, "This video is unavailable.") || StringUtils.equalsIgnoreCase(cachedData.clipData.error, "This video is not available.")) {
                    // this is not region issue, its just not available.
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, cachedData.clipData.error);
                }
                if (StringUtils.containsIgnoreCase(cachedData.clipData.error, "This video has been removed")) {
                    throw new PluginException(LinkStatus.ERROR_FILE_NOT_FOUND, cachedData.clipData.error);
                }
                // private video.. login is required! assumption that account hasn't been used.. or wrong account has been used...
                if (StringUtils.containsIgnoreCase(cachedData.clipData.error, "This Video is Private")) {
                    if (helper.getLoggedIn()) {
                        // wrong account used?? try next??
                        // TODO: confirm with jiaz that this this type of exception will try the next account
                    }
                    throw new AccountRequiredException(cachedData.clipData.error); // .localizedMessage(_JDT.T.AccountRequiredException_createCandidateResult());
                }
                if (cachedData.clipData.error != null) {
                    String lc = cachedData.clipData.error.toLowerCase(Locale.ENGLISH);
                    if (lc.contains("is not available in your country") || lc.contains("geo blocked due to copyright grounds")) {
                        // 18.04.2016
                        // „Unfortunately, this video is not available in Germany because it may contain music for which GEMA has not
                        // granted the respective music rights.”
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, THE_DOWNLOAD_IS_NOT_AVAILABLE_IN_YOUR_COUNTRY).localizedMessage(_JDT.T.CountryIPBlockException_createCandidateResult());
                    }
                    if (lc.contains("content is not available in")) {
                        // „Unfortunately, this UMG-music-content is not available in Germany because GEMA has not granted the
                        // respective music publishing rights.”
                        // 18.04.2016
                        throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, THE_DOWNLOAD_IS_NOT_AVAILABLE_IN_YOUR_COUNTRY).localizedMessage(_JDT.T.CountryIPBlockException_createCandidateResult());
                    }
                }
                throw new PluginException(LinkStatus.ERROR_TEMPORARILY_UNAVAILABLE, cachedData.clipData.error);
            }
            return cachedData;
        }
    }

    private static URL YOUTUBE_URL;
    static {
        try {
            YOUTUBE_URL = new URL("http://youtube.com");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    private static CachedClipData getInternal(YoutubeHelper helper, String videoID) throws Exception {
        return getInternal(helper, new YoutubeClipData(videoID));
    }

    public static void clearCache(DownloadLink downloadLink) {
        final String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        clearCache(videoID);
    }

    public static void clearCache(String videoID) {
        synchronized (LOCK) {
            CACHE.remove(videoID);
        }
    }

    public static void referenceLink(YoutubeHelper helper, DownloadLink link, YoutubeClipData vid) {
        synchronized (LOCK) {
            final String cachedID = vid.videoID;
            final MinTimeWeakReference<CachedClipData> cache = CACHE.get(cachedID);
            List<HTTPProxy> proxyListNew = null;
            try {
                proxyListNew = helper.getBr().selectProxies(YOUTUBE_URL);
            } catch (IOException e) {
                e.printStackTrace();
            }
            CachedClipData data = null;
            if (cache != null && (data = cache.get()) != null && data.hasValidProxyList(proxyListNew)) {
                data.setYoutubeClipData(vid);
                link.getTempProperties().setProperty("CLIP_DATA_REFERENCE", data);
            } else {
                data = new CachedClipData(proxyListNew, vid);
                link.getTempProperties().setProperty("CLIP_DATA_REFERENCE", data);
                CACHE.put(cachedID, new MinTimeWeakReference<CachedClipData>(data, 1500, cachedID, CLEANUP));
            }
        }
    }

    public static boolean hasCache(YoutubeHelper helper, String videoID) {
        synchronized (LOCK) {
            final MinTimeWeakReference<CachedClipData> ref = CACHE.get(videoID);
            final CachedClipData cachedData = ref == null ? null : ref.get();
            if (cachedData != null) {
                try {
                    final List<HTTPProxy> proxyListNew = helper.getBr().selectProxies(YOUTUBE_URL);
                    return cachedData.hasValidProxyList(proxyListNew);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return false;
        }
    }

    public static boolean hasCache(YoutubeHelper helper, DownloadLink downloadLink) {
        final String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        return hasCache(helper, videoID);
    }
}
