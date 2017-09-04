package org.jdownloader.plugins.components.youtube;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map.Entry;

import org.appwork.storage.config.MinTimeWeakReference;
import org.appwork.storage.config.MinTimeWeakReferenceCleanup;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxy;
import org.jdownloader.translate._JDT;

import jd.plugins.AccountRequiredException;
import jd.plugins.DownloadLink;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;

public class ClipDataCache {

    public static final String  THE_DOWNLOAD_IS_NOT_AVAILABLE_IN_YOUR_COUNTRY = "The Download is not available in your country";
    private static final Object LOCK                                          = new Object();

    private static class CachedClipData {

        private YoutubeClipData clipData;
        public List<HTTPProxy>  proxyList;

        public CachedClipData(List<HTTPProxy> proxyListNew, YoutubeClipData youtubeClipData) {
            this.clipData = youtubeClipData;
            proxyList = proxyListNew;
            if (proxyList == null) {
                proxyList = EMPTY;
            }
        }

        private static ArrayList<HTTPProxy> EMPTY = new ArrayList<HTTPProxy>();

        public boolean hasValidProxyList(List<HTTPProxy> proxyListNew) {
            if (proxyListNew == null) {
                proxyListNew = EMPTY;
            }
            // this logic is wrong. just because the size changes, dosn't mean the orginal proxy isn't still within the list!
            // if (proxyListNew.size() != proxyList.size()) {
            // return false;
            // }
            // TODO: confirm this logic is correct, to me it's not!
            // ideally we need to use the same proxy from last cachedata to download with.
            // must check USED vs available, if miss match switch browser proxy selector?
            for (int i = 0; i < proxyList.size(); i++) {
                if (!proxyList.get(i).equals(proxyListNew.get(i))) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final HashMap<String, MinTimeWeakReference<CachedClipData>> CACHE = new HashMap<String, MinTimeWeakReference<CachedClipData>>();

    public static YoutubeClipData get(YoutubeHelper helper, DownloadLink downloadLink) throws Exception {
        String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        CachedClipData ret = getInternal(helper, videoID);
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
            List<HTTPProxy> proxyListNew = helper.getBr().selectProxies(YOUTUBE_URL);
            if (cachedData != null) {
                if (!cachedData.hasValidProxyList(proxyListNew)) {
                    cachedData = null;
                }
                if (cachedData != null && StringUtils.isEmpty(cachedData.clipData.title)) {
                    cachedData = null;
                }
                if (cachedData != null && cachedData.clipData.date == 0) {
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
                    if (lc.contains("is not available in your country")) {
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

    // private static String createCachedKey(String videoID, YoutubeHelper helper) {
    // String ret = videoID + ".";
    // List<HTTPProxy> proxyList;
    // try {
    // proxyList = helper.getBr().selectProxies(YOUTUBE_URL);
    //
    // if (proxyList != null && proxyList.size() > 0) {
    // HTTPProxy proxy = proxyList.get(0);
    // if (proxy != null) {
    // ret += "Proxy." + proxy.getType() + "://" + proxy.getHost() + ":" + proxy.getPort();
    // }
    // }
    // } catch (IOException e) {
    // e.printStackTrace();
    // }
    //
    // return ret;
    // }
    private static CachedClipData getInternal(YoutubeHelper helper, String videoID) throws Exception {
        return getInternal(helper, new YoutubeClipData(videoID));
    }

    public static void clearCache(DownloadLink downloadLink) {
        String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        clearCache(videoID);
    }

    public static void clearCache(String videoID) {
        synchronized (LOCK) {
            CACHE.remove(videoID);
        }
    }

    public static void referenceLink(YoutubeHelper helper, DownloadLink link, YoutubeClipData vid) {
        List<HTTPProxy> proxyListNew = null;
        try {
            proxyListNew = helper.getBr().selectProxies(YOUTUBE_URL);
        } catch (IOException e) {
            e.printStackTrace();
        }
        synchronized (LOCK) {
            String cachedID = vid.videoID;
            for (Entry<String, MinTimeWeakReference<CachedClipData>> es : CACHE.entrySet()) {
                if (StringUtils.equals(es.getKey(), cachedID)) {
                    CachedClipData v = es.getValue().get();
                    if (v != null && v.hasValidProxyList(proxyListNew)) {
                        v.clipData = vid;
                        link.getTempProperties().setProperty("CLIP_DATA_REFERENCE", v);
                        return;
                    }
                }
            }
            MinTimeWeakReference<CachedClipData> ref = new MinTimeWeakReference<CachedClipData>(new CachedClipData(proxyListNew, vid), 1500, cachedID, CLEANUP);
            CACHE.put(cachedID, ref);
        }
    }

    public static boolean hasCache(YoutubeHelper helper, String videoID) {
        synchronized (LOCK) {
            MinTimeWeakReference<CachedClipData> ref = CACHE.get(videoID);
            CachedClipData cachedData = ref == null ? null : ref.get();
            List<HTTPProxy> proxyListNew = null;
            try {
                proxyListNew = helper.getBr().selectProxies(YOUTUBE_URL);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (cachedData != null) {
                if (!cachedData.hasValidProxyList(proxyListNew)) {
                    cachedData = null;
                }
            }
            return cachedData != null;
        }
    }

    public static boolean hasCache(YoutubeHelper helper, DownloadLink downloadLink) {
        String videoID = downloadLink.getStringProperty(YoutubeHelper.YT_ID);
        synchronized (LOCK) {
            String cachedID = videoID;
            MinTimeWeakReference<CachedClipData> ref = CACHE.get(cachedID);
            CachedClipData cachedData = ref == null ? null : ref.get();
            return cachedData != null;
        }
    }
}
