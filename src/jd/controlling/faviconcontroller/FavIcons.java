package jd.controlling.faviconcontroller;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import org.appwork.net.protocol.http.HTTPConstants;
import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.PublicSuffixList;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.LazyPlugin;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.updatev2.gui.LAFOptions;

import jd.captcha.utils.GifDecoder;
import jd.http.Browser;
import jd.http.Browser.BrowserException;
import jd.http.Request;
import jd.http.URLConnectionAdapter;
import jd.plugins.PluginForHost;
import net.sf.image4j.codec.ico.ICODecoder;

public class FavIcons {
    private static final ThreadPoolExecutor                                      THREAD_POOL;
    private static final AtomicInteger                                           THREADCOUNTER   = new AtomicInteger(0);
    private static final Object                                                  LOCK            = new Object();
    private static final LinkedHashMap<String, java.util.List<FavIconRequestor>> QUEUE           = new LinkedHashMap<String, java.util.List<FavIconRequestor>>();
    private static final HashMap<String, ImageIcon>                              FAILED_ICONS    = new HashMap<String, ImageIcon>();
    private static final HashSet<String>                                         REFRESHED_ICONS = new HashSet<String>();
    private static final HashMap<String, ImageIcon>                              DEFAULT_ICONS   = new HashMap<String, ImageIcon>();
    private static final LogSource                                               LOGGER;
    private static final FavIconsConfig                                          CONFIG          = JsonConfig.create(FavIconsConfig.class);
    private static final long                                                    REFRESH_TIMEOUT = 1000l * 60 * 60 * 24 * 7;
    private static final long                                                    RETRY_TIMEOUT   = 1000l * 60 * 60 * 24 * 7;
    static {
        LOGGER = LogController.getInstance().getLogger("FavIcons.class");
        int maxThreads = Math.max(CONFIG.getMaxThreads(), 1);
        int keepAlive = Math.max(CONFIG.getThreadKeepAlive(), 100);
        THREAD_POOL = new ThreadPoolExecutor(0, maxThreads, keepAlive, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread t = new Thread(r);
                t.setDaemon(true);
                t.setName("FavIconLoader:" + THREADCOUNTER.incrementAndGet());
                return t;
            }
        }, new ThreadPoolExecutor.AbortPolicy()) {
            @Override
            protected void beforeExecute(Thread t, Runnable r) {
                super.beforeExecute(t, r);
                /*
                 * WORKAROUND for stupid SUN /ORACLE way of "how a threadpool should work" !
                 */
                int working = THREAD_POOL.getActiveCount();
                int active = THREAD_POOL.getPoolSize();
                int max = THREAD_POOL.getMaximumPoolSize();
                if (active < max) {
                    if (working == active) {
                        /*
                         * we can increase max pool size so new threads get started
                         */
                        THREAD_POOL.setCorePoolSize(Math.min(max, active + 1));
                    }
                }
            }
        };
        THREAD_POOL.allowCoreThreadTimeOut(true);
        /* load failed hosts list */
        ArrayList<String> FAILED_ARRAY_LIST = JsonConfig.create(FavIconsConfig.class).getFailedHosts();
        if (FAILED_ARRAY_LIST == null || (System.currentTimeMillis() - CONFIG.getLastRefresh()) > RETRY_TIMEOUT) {
            CONFIG.setLastRefresh(System.currentTimeMillis());
            /* timeout is over, lets try again the failed ones */
            FAILED_ARRAY_LIST = new ArrayList<String>();
            CONFIG.setFailedHosts(FAILED_ARRAY_LIST);
        }
        synchronized (LOCK) {
            for (String host : FAILED_ARRAY_LIST) {
                FAILED_ICONS.put(host, null);
            }
        }
        ShutdownController.getInstance().addShutdownEvent(new ShutdownEvent() {
            @Override
            public String toString() {
                return "ShutdownEvent: Save Favicons";
            }

            @Override
            public void onShutdown(final ShutdownRequest shutdownRequest) {
                final ArrayList<String> failedHosts;
                synchronized (LOCK) {
                    failedHosts = new ArrayList<String>(FAILED_ICONS.keySet());
                }
                CONFIG.setFailedHosts(failedHosts);
            }
        });
    }

    public static Icon getFavIcon(String host, FavIconRequestor requestor) {
        return getFavIcon(host, requestor, true);
    }

    public static Icon getFavIcon(String host, FavIconRequestor requestor, boolean updatePermission) {
        if (host == null) {
            return null;
        }
        Icon image = null;
        synchronized (LOCK) {
            /* check if we already have a favicon? */
            URL url = NewTheme.I().getIconURL("fav/" + host);
            if (url != null) {
                image = new AbstractIcon("fav/" + host, -1);
            }
            if (image == null) {
                url = NewTheme.I().getIconURL("fav/big." + host);
                if (url != null) {
                    image = new AbstractIcon("fav/big." + host, -1);
                }
            }
            if (image != null) {
                try {
                    if (REFRESHED_ICONS.add(host)) {
                        if ("file".equalsIgnoreCase(url.getProtocol())) {
                            final File file = new File(url.toURI());
                            final long lastModified = file.lastModified();
                            if ((lastModified > 0 && System.currentTimeMillis() - lastModified > REFRESH_TIMEOUT) && file.exists()) {
                                file.setLastModified(System.currentTimeMillis());// avoid retry before expired
                                if (updatePermission) {
                                    add(host, requestor);
                                }
                            }
                        }
                    }
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                /* load favIcon from disk */
                return image;
            }
        }
        if (image == null && updatePermission) {
            /* add to queue list */
            image = add(host, requestor);
        }
        return image;
    }

    public static ImageIcon getDefaultIcon(String host, boolean clearAfterGet) {
        ImageIcon ret = null;
        synchronized (LOCK) {
            ret = DEFAULT_ICONS.get(host);
            if (ret == null) {
                ret = new ImageIcon(createDefaultFavIcon(host));
                if (clearAfterGet == false) {
                    DEFAULT_ICONS.put(host, ret);
                }
            } else if (clearAfterGet) {
                DEFAULT_ICONS.remove(host);
            }
        }
        return ret;
    }

    private static ImageIcon add(final String host, FavIconRequestor requestor) {
        ImageIcon icon = null;
        synchronized (LOCK) {
            /* don't try this host again? */
            if (FAILED_ICONS.containsKey(host)) {
                icon = FAILED_ICONS.get(host);
                if (icon == null) {
                    icon = getDefaultIcon(host, true);
                    FAILED_ICONS.put(host, icon);
                }
                return icon;
            } else {
                icon = getDefaultIcon(host, false);
            }
            /* enqueue this host for favicon loading */
            List<FavIconRequestor> ret = QUEUE.get(host);
            boolean enqueueFavIcon = false;
            if (ret == null) {
                ret = new ArrayList<FavIconRequestor>();
                QUEUE.put(host, ret);
                enqueueFavIcon = true;
            }
            /* add to queue */
            if (requestor != null) {
                ret.add(requestor);
            }
            if (enqueueFavIcon) {
                THREAD_POOL.execute(new Runnable() {
                    public void run() {
                        final List<String> tryHosts = new ArrayList<String>();
                        BufferedImage favicon = null;
                        String[] siteSupportedNames = null;
                        final LazyHostPlugin existingHostPlugin = HostPluginController.getInstance().get(host);
                        if (existingHostPlugin != null) {
                            if (existingHostPlugin.hasFeature(LazyPlugin.FEATURE.INTERNAL)) {
                                synchronized (LOCK) {
                                    QUEUE.remove(host);
                                    if (!REFRESHED_ICONS.contains(host) && FAILED_ICONS.get(host) == null) {
                                        FAILED_ICONS.put(host, getDefaultIcon(host, true));
                                    }
                                }
                                return;
                            }
                            if (existingHostPlugin.hasFeature(LazyPlugin.FEATURE.FAVICON)) {
                                final LogSource logger = LogController.getFastPluginLogger("FavIcons");
                                try {
                                    final PluginForHost pluginInstance = existingHostPlugin.newInstance(null, false);
                                    pluginInstance.setLogger(logger);
                                    siteSupportedNames = pluginInstance.siteSupportedNames();
                                    if (siteSupportedNames == null) {
                                        siteSupportedNames = new String[0];
                                    }
                                    final Object result = pluginInstance.getFavIcon(host);
                                    if (result instanceof BufferedImage) {
                                        favicon = (BufferedImage) result;
                                    } else if (result instanceof String) {
                                        tryHosts.add((String) result);
                                    } else if (result instanceof List) {
                                        for (final Object elem : (List) result) {
                                            if (elem instanceof String) {
                                                tryHosts.add((String) elem);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.log(e);
                                } finally {
                                    if (favicon != null) {
                                        logger.clear();
                                    }
                                    logger.close();
                                }
                            }
                        }
                        if (favicon == null) {
                            tryHosts.add(host);
                            if (!host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+")) {
                                final String domain;
                                if (PublicSuffixList.getInstance() != null) {
                                    domain = PublicSuffixList.getInstance().getDomain(host);
                                } else {
                                    domain = null;
                                }
                                String tryHost = host;
                                int index = 0;
                                while (true) {
                                    /* this loop adds every subdomain and the tld to tryHosts and we try to fetch a favIcon in same order */
                                    if ((index = tryHost.indexOf(".")) >= 0 && tryHost.length() >= index + 1) {
                                        tryHost = tryHost.substring(index + 1);
                                        if (domain != null && !tryHost.contains(domain) || tryHost.indexOf('.') == -1) {
                                            break;
                                        }
                                        tryHosts.add(tryHost);
                                    } else {
                                        break;
                                    }
                                }
                            }
                            favicon = downloadFavIcon(tryHosts, siteSupportedNames);
                        }
                        synchronized (LOCK) {
                            final List<FavIconRequestor> requestors = QUEUE.remove(host);
                            if (favicon == null) {
                                if (!REFRESHED_ICONS.contains(host) && FAILED_ICONS.get(host) == null) {
                                    FAILED_ICONS.put(host, getDefaultIcon(host, true));
                                }
                            } else {
                                FileOutputStream fos = null;
                                File outputFile = null;
                                try {
                                    /* buffer favicon to disk */
                                    outputFile = Application.getResource(NewTheme.I().getPath() + "/images/fav/" + host + ".png");
                                    FileCreationManager.getInstance().mkdir(outputFile.getParentFile());
                                    fos = new FileOutputStream(outputFile);
                                    ImageProvider.writeImage(favicon, "png", fos);
                                    outputFile = null;
                                    /* load and scale it again */
                                    if (requestors != null && requestors.size() > 0) {
                                        final Icon image = new AbstractIcon("fav/" + host, -1);
                                        if (image != null) {
                                            /* refresh icons for all queued plugins */
                                            for (final FavIconRequestor requestor : requestors) {
                                                requestor.setFavIcon(image);
                                            }
                                        }
                                    }
                                } catch (Throwable e) {
                                    LogController.getInstance().getLogger("FavIcons").log(e);
                                } finally {
                                    try {
                                        if (fos != null) {
                                            fos.close();
                                        }
                                    } catch (final Throwable e) {
                                    }
                                    if (outputFile != null) {
                                        outputFile.delete();
                                    }
                                }
                            }
                        }
                    }
                });
            }
            return icon;
        }
    }

    /**
     * Creates a dummyHosterIcon
     */
    private static BufferedImage createDefaultFavIcon(String host) {
        final int w = 16;
        final int h = 16;
        int size = 9;
        final Color fg = Color.BLACK;
        Color bg = Color.WHITE;
        LOGGER.info("Create Favicon: " + host);
        try {
            if (!Application.isHeadless()) {
                final Color c = LAFOptions.getInstance().getColorForPanelHeaderBackground();
                if (c != null) {
                    bg = c;
                }
            }
        } catch (final Throwable e) {
            LogController.CL().log(e);
        }
        final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        try {
            try {
                final String fontName = ImageProvider.getDrawFontName();
                g.setFont(new Font(fontName, Font.BOLD, size));
                final FontMetrics fontmetrics = g.getFontMetrics(); // check for missing fonts/headless java
                String tld = Files.getExtension(host);
                if (tld != null) {
                    tld = tld.toLowerCase(Locale.ENGLISH);
                }
                String dummy = host.toUpperCase();
                // remove tld
                try {
                    dummy = dummy.substring(0, dummy.lastIndexOf("."));
                } catch (Throwable t) {
                }
                // clean up
                dummy = dummy.replaceAll("[\\d\\WEIOAJU]", "");
                try {
                    dummy = "" + dummy.charAt(0) + dummy.charAt(dummy.length() / 2);
                } catch (Throwable t) {
                }
                if (dummy.length() <= 0 || dummy.length() > 2) {
                    dummy = host.substring(0, Math.min(host.length(), 2));
                }
                // paint
                // Graphics2D g = image.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setFont(new Font(fontName, Font.BOLD, size));
                final RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 5, 5);
                g.setColor(bg);
                g.fill(roundedRectangle);
                g.setColor(bg.darker());
                g.draw(roundedRectangle);
                g.setColor(fg);
                Rectangle2D bounds = fontmetrics.getStringBounds(dummy, g);
                g.drawString(dummy, (int) (w - bounds.getWidth()) / 2, (int) (-bounds.getY() + (h - bounds.getHeight()) / 2) - (tld == null ? 0 : 1));
                if (tld != null) {
                    g.setFont(new Font(fontName, 0, 6));
                    bounds = fontmetrics.getStringBounds("." + tld, g);
                    g.drawString("." + tld, (int) (w - bounds.getWidth()) - 2, (h) - 2);
                }
                return image;
            } catch (Throwable e) {
                if (ImageProvider.isBuggyFontEnvironment(e)) {
                    return IconIO.toBufferedImage(new AbstractIcon(IconKey.ICON_ERROR, 16));
                } else {
                    throw new RuntimeException(e);
                }
            }
        } finally {
            g.dispose();
        }
    }

    /*
     * dirty hack to count number of unique colors, use only for small images like favicons!
     */
    private static int countColors(BufferedImage image) {
        final HashSet<Integer> color = new HashSet<Integer>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                color.add(image.getRGB(x, y));
            }
        }
        return color.size();
    }

    private static BufferedImage returnBestImage(List<BufferedImage> images) {
        if (images != null && images.size() > 0) {
            BufferedImage ret = null;
            int size = -1;
            int colors = -1;
            for (final BufferedImage img : images) {
                /*
                 * loop through all available images to find best resolution
                 */
                if (img == null) {
                    continue;
                } else if (ret == null || (img.getHeight() * img.getWidth()) > size || countColors(img) > colors) {
                    ret = img;
                    size = ret.getHeight() * ret.getWidth();
                    colors = countColors(ret);
                }
            }
            if (ret != null && ret.getHeight() > 1 && ret.getWidth() > 1) {
                return ret;
            }
        }
        return null;
    }

    private static boolean isSameDomain(final Browser br, String host, String[] siteSupportedNames) throws IOException {
        if (host.matches("(?i)^https?://.+")) {
            host = new URL(host).getHost();
        }
        final URL url = br != null ? br._getURL() : null;
        if (url == null) {
            return false;
        } else if (!StringUtils.containsIgnoreCase(url.getHost(), host)) {
            final String domain = br.getHost();
            if (!StringUtils.containsIgnoreCase(domain, host)) {
                // different domain, check for different ld
                final String domainTld = PublicSuffixList.getInstance().getTopLevelDomain(domain);
                final String hostTld = PublicSuffixList.getInstance().getTopLevelDomain(host);
                final String compareDomain = domain.replaceAll("\\." + domainTld + "$", "");
                final String compareHost = host.replaceAll("\\." + hostTld + "$", "");
                if (StringUtils.equalsIgnoreCase(compareDomain, compareHost)) {
                    return true;
                }
            }
            if (siteSupportedNames == null) {
                final LazyHostPlugin existingHostPlugin = HostPluginController.getInstance().get(host);
                if (existingHostPlugin != null) {
                    final LogSource logger = LogController.getFastPluginLogger("FavIcons");
                    try {
                        final PluginForHost pluginInstance = existingHostPlugin.newInstance(null, false);
                        pluginInstance.setLogger(logger);
                        siteSupportedNames = pluginInstance.siteSupportedNames();
                    } catch (Exception e) {
                        logger.log(e);
                    } finally {
                        logger.close();
                    }
                }
            }
            if (siteSupportedNames != null && siteSupportedNames.length > 0) {
                for (String siteSupportedName : siteSupportedNames) {
                    if (StringUtils.equalsIgnoreCase(siteSupportedName, domain) || StringUtils.equalsIgnoreCase(siteSupportedName, url.getHost())) {
                        return true;
                    }
                }
            }
            return false;
        }
        return true;
    }

    private static boolean isFavIconURL(String host) {
        try {
            if (!host.matches("(?i)^https?://.+")) {
                return false;
            } else {
                final String path = new URL(host).getFile();
                return path.matches("(?i).+\\.(ico|png|svg)$");
            }
        } catch (MalformedURLException e) {
            return false;
        }
    }

    public static BufferedImage download_FavIconTag(final String host, String[] siteSupportedNames, LogInterface logger) throws IOException {
        final Browser favBr = new Browser();
        favBr.setLogger(logger);
        favBr.setConnectTimeout(10000);
        favBr.setReadTimeout(10000);
        final List<String> websites = new ArrayList<String>();
        if (!isFavIconURL(host)) {
            if (host.matches("(?i)^https?://.+")) {
                websites.add(host);
            } else {
                websites.addAll(Arrays.asList(new String[] { "https://" + host, "http://" + host }));
            }
        }
        for (final String website : websites) {
            boolean retryFlag = true;
            int retryCount = 0;
            while (retryFlag) {
                retryFlag = false;
                try {
                    favBr.getPage(website);
                    if (favBr.getRedirectLocation() != null) {
                        favBr.followRedirect(true);
                        if (!isSameDomain(favBr, host, siteSupportedNames)) {
                            throw new IOException("redirect to different domain?" + favBr._getURL().getHost() + "!=" + host);
                        }
                    }
                    final BufferedImage ret = download_FavIconTag(favBr, host, logger);
                    if (ret != null) {
                        return ret;
                    } else {
                        break;
                    }
                } catch (final BrowserException e) {
                    logger.log(e);
                    final Request reg = e.getRequest();
                    if (reg != null && reg.getHttpConnection() != null && reg.getHttpConnection().getResponseCode() == 429 && retryCount == 0) {
                        try {
                            int timeout = 2000;// minimum wait timeout
                            final String retryAfter = reg.getHttpConnection().getHeaderField(HTTPConstants.HEADER_RESPONSE_RETRY_AFTER);
                            if (retryAfter != null && retryAfter.matches("^\\s*\\d+\\s*$")) {
                                timeout = Math.max(timeout, Math.min(Integer.parseInt(retryAfter) * 1000, 20000));
                            }
                            Thread.sleep(timeout);
                            retryFlag = true;
                            retryCount = 1;
                        } catch (InterruptedException ignore) {
                        }
                    }
                } catch (final IOException e) {
                    logger.log(e);
                }
            }
        }
        if (websites.size() == 0) {
            // host is absolute URL
            final BufferedImage ret = download_FavIconTag(favBr, host, logger);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    public static BufferedImage download_FavIconTag(Browser favBr, String host, LogInterface logger) throws IOException {
        final Set<String> favIconURLs = getFavIconURLs(favBr, host, logger);
        for (final String favIconURL : favIconURLs) {
            boolean retryFlag = true;
            int retryCount = 0;
            while (retryFlag) {
                retryFlag = false;
                try {
                    final Browser brc = favBr.cloneBrowser();
                    final BufferedImage ret = download_FavIconTag(brc, favIconURL, host, logger);
                    if (ret != null) {
                        return ret;
                    } else {
                        break;
                    }
                } catch (final BrowserException e) {
                    logger.log(e);
                    final Request reg = e.getRequest();
                    if (reg != null && reg.getHttpConnection() != null && reg.getHttpConnection().getResponseCode() == 429 && retryCount == 0) {
                        try {
                            int timeout = 2000;// minimum wait timeout
                            final String retryAfter = reg.getHttpConnection().getHeaderField(HTTPConstants.HEADER_RESPONSE_RETRY_AFTER);
                            if (retryAfter != null && retryAfter.matches("^\\s*\\d+\\s*$")) {
                                timeout = Math.max(timeout, Math.min(Integer.parseInt(retryAfter) * 1000, 20000));
                            }
                            Thread.sleep(timeout);
                            retryFlag = true;
                            retryCount = 1;
                        } catch (InterruptedException ignore) {
                        }
                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            }
        }
        return null;
    }

    public static Set<String> getFavIconURLs(final Browser favBr, final String host, LogInterface logger) throws IOException {
        final Set<String> ret = new LinkedHashSet<String>();
        if (isFavIconURL(host)) {
            ret.add(host);
        } else {
            final String requestHtml = favBr.toString().replaceAll("(?s)<!--.*?-->", "");
            String urls[] = new Regex(requestHtml, "rel\\s*=\\s*('|\")(SHORTCUT |apple-touch-)?ICON('|\")[^>]*href\\s*=\\s*('|\")([^>'\"]*\\.(ico|png|svg)[^>'\"]*)('|\")").getColumn(4);
            if (urls != null && urls.length > 0) {
                ret.addAll(Arrays.asList(urls));
            }
            urls = new Regex(requestHtml, "href\\s*=\\s*('|\")([^>'\"]*\\.(ico|png|svg)[^>'\"]*)('|\")[^>]*rel\\s*=\\s*('|\")(SHORTCUT |apple-touch-)?ICON('|\")").getColumn(1);
            if (urls != null && urls.length > 0) {
                ret.addAll(Arrays.asList(urls));
            }
            if (ret.size() == 0) {
                /*
                 * workaround for hoster with not complete url, eg rapidshare.com
                 */
                String url = new Regex(requestHtml, "rel\\s*=\\s*('|\")(SHORTCUT |apple-touch-)?ICON('|\")[^>]*href\\s*=\\s*[^>]*//([^>'\"]*\\.(ico|png|svg)[^>'\"]*)('|\")").getMatch(3);
                if (!StringUtils.isEmpty(url) && !url.equalsIgnoreCase(host)) {
                    url = "http://" + url;
                }
                ret.add(url);
            }
            ret.add("/favicon.ico");
        }
        return ret;
    }

    public static BufferedImage download_FavIconTag(final Browser favBr, final String url, final String host, LogInterface logger) throws IOException {
        URLConnectionAdapter con = null;
        byte[] bytes = null;
        try {
            if (!StringUtils.isEmpty(url)) {
                /* favicon tag with ico extension */
                favBr.setFollowRedirects(true);
                favBr.getHeaders().put("Accept-Encoding", null);
                con = favBr.openGetConnection(url);
                /* we use bufferedinputstream to reuse it later if needed */
                bytes = IO.readStream(-1, con.getInputStream());
                if (con.isOK() && !StringUtils.containsIgnoreCase(con.getContentType(), "text")) {
                    try {
                        List<BufferedImage> ret = null;
                        if (bytes[1] == 80 && bytes[2] == 78 && bytes[3] == 71) {
                            final BufferedImage img = downloadImage(con, logger, new ByteArrayInputStream(bytes));
                            if (img != null) {
                                ret = new ArrayList<BufferedImage>();
                                ret.add(img);
                            }
                        } else if (bytes[0] == 71 && bytes[1] == 73 && bytes[2] == 70) {
                            final GifDecoder gifDecoder = new GifDecoder();
                            /* reset bufferedinputstream to begin from start */
                            if (gifDecoder.read(new ByteArrayInputStream(bytes)) == 0) {
                                final BufferedImage img = gifDecoder.getImage();
                                if (img != null) {
                                    ret = new ArrayList<BufferedImage>();
                                    ret.add(img);
                                }
                            }
                        }
                        /* try first with iconloader */
                        if (ret == null) {
                            try {
                                ret = ICODecoder.read(new ByteArrayInputStream(bytes));
                            } catch (final IOException e) {
                                final String max = new Regex(e.getMessage(), "Failed to read image #\\s*(\\d+)").getMatch(0);
                                if (max != null && Integer.parseInt(max) > 1) {
                                    final byte[] copy = bytes.clone();
                                    // TODO: modify icon header to stop at last okay image
                                    ret = ICODecoder.read(new ByteArrayInputStream(copy));
                                } else {
                                    throw e;
                                }
                            }
                        }
                        final BufferedImage img = returnBestImage(ret);
                        if (img != null) {
                            return img;
                        } else {
                            throw new Throwable("Try again with other ImageLoader");
                        }
                    } catch (Throwable e) {
                        /* maybe redirect to different icon format? */
                        final BufferedImage img = downloadImage(con, logger, new ByteArrayInputStream(bytes));
                        if (img != null && img.getHeight() > 1 && img.getWidth() > 1) {
                            return img;
                        } else {
                            logger.log(e);
                        }
                    }
                }
            }
            return null;
        } finally {
            if (con != null) {
                try {
                    con.disconnect();
                } catch (final Throwable e) {
                }
            }
        }
    }

    public static BufferedImage downloadFavIcon(List<String> hosts, String[] siteSupportedNames) {
        for (final String host : hosts) {
            final BufferedImage ret = downloadFavIcon(host, siteSupportedNames);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    @Deprecated
    public static BufferedImage downloadFavIcon(String host) {
        return downloadFavIcon(host, null);
    }

    /**
     * downloads a favicon from the given host, icon must be bigger than 1x1, cause some hosts have fake favicon.ico with 1x1 size
     */
    public static BufferedImage downloadFavIcon(String host, String[] siteSupportedNames) {
        final LogSource logger = LogController.getFastPluginLogger("FavIcons");
        logger.info("Download FavIcon for " + host);
        BufferedImage ret = null;
        try {
            /* first try to get the FavIcon specified in FavIconTag */
            ret = download_FavIconTag(host, siteSupportedNames, logger);
        } catch (Throwable ignore) {
            logger.log(ignore);
        } finally {
            if (ret != null) {
                logger.clear();
            } else {
                logger.severe("Could not download FavIcon for " + host);
            }
            logger.close();
        }
        return ret;
    }

    private static BufferedImage downloadImage(URLConnectionAdapter con, LogInterface logger, InputStream is) {
        try {
            BufferedImage ret = null;
            if (StringUtils.endsWithCaseInsensitive(con.getURL().getPath(), ".svg")) {
                try {
                    final Image img = IconIO.getSvgFactory().getImageFromSVG(is, 32, 32, null);
                    if (img != null) {
                        ret = IconIO.toBufferedImage(img);
                    }
                } catch (IOException e) {
                    logger.log(e);
                }
            }
            if (ret == null) {
                ret = ImageIO.read(is);
            }
            if (ret == null) {
                /* workaround for gif images */
                final GifDecoder d = new GifDecoder();
                /* reset bufferedinputstream to begin from start */
                is.reset();
                if (d.read(is) == 0) {
                    ret = d.getImage();
                }
            }
            return ret;
        } catch (Throwable e) {
            logger.log(e);
        }
        return null;
    }
}
