package jd.controlling.faviconcontroller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.swing.ImageIcon;

import jd.captcha.utils.GifDecoder;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import net.sf.image4j.codec.ico.ICODecoder;

import org.appwork.shutdown.ShutdownController;
import org.appwork.shutdown.ShutdownEvent;
import org.appwork.shutdown.ShutdownRequest;
import org.appwork.storage.config.JsonConfig;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.StringUtils;
import org.appwork.utils.ImageProvider.ImageProvider;
import org.appwork.utils.images.IconIO;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.net.PublicSuffixList;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.updatev2.gui.LAFOptions;

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
                                add(host, requestor);
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
        if (image == null) {
            /* add to queue list */
            image = add(host, requestor);
        }
        return image;
    }

    private static ImageIcon getDefaultIcon(String host, boolean clearAfterGet) {
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
                        final LazyHostPlugin existingHostPlugin = HostPluginController.getInstance().get(host);
                        if (existingHostPlugin != null && ("jd.plugins.hoster.Offline".equals(existingHostPlugin.getClassName()) || "jd.plugins.hoster.JdLog".equals(existingHostPlugin.getClassName()))) {
                            synchronized (LOCK) {
                                QUEUE.remove(host);
                                if (!REFRESHED_ICONS.contains(host) && FAILED_ICONS.get(host) == null) {
                                    FAILED_ICONS.put(host, getDefaultIcon(host, true));
                                }
                            }
                            return;
                        }
                        final List<String> tryHosts = new ArrayList<String>();
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
                        final BufferedImage favicon = downloadFavIcon(tryHosts);
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
                                    ImageIO.write(favicon, "png", fos);
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
            bg = LAFOptions.getInstance().getColorForPanelHeaderBackground();
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        final BufferedImage image = new BufferedImage(w, h, Transparency.TRANSLUCENT);
        final Graphics2D g = image.createGraphics();
        try {
            try {
                final String fontName = ImageProvider.getDrawFontName();
                g.setFont(new Font(fontName, Font.BOLD, size));
                g.getFontMetrics(); // check for missing fonts/headless java
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
                RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 5, 5);
                g.setColor(bg);
                g.fill(roundedRectangle);
                g.setColor(bg.darker());
                g.draw(roundedRectangle);
                g.setColor(fg);
                Rectangle2D bounds = g.getFontMetrics().getStringBounds(dummy, g);
                g.drawString(dummy, (int) (w - bounds.getWidth()) / 2, (int) (-bounds.getY() + (h - bounds.getHeight()) / 2) - (tld == null ? 0 : 1));
                if (tld != null) {
                    g.setFont(new Font(fontName, 0, 6));
                    bounds = g.getFontMetrics().getStringBounds("." + tld, g);
                    g.drawString("." + tld, (int) (w - bounds.getWidth()) - 2, (h) - 2);
                }
                return image;
            } catch (NullPointerException e) {
                // java.lang.NullPointerException
                // at sun.awt.FontConfiguration.getVersion(FontConfiguration.java:1264)
                // at sun.awt.FontConfiguration.readFontConfigFile(FontConfiguration.java:219)
                // at sun.awt.FontConfiguration.init(FontConfiguration.java:107)
                if (Application.isHeadless()) {
                    return IconIO.toBufferedImage(new AbstractIcon(IconKey.ICON_ERROR, 16));
                } else {
                    throw e;
                }
            }
        } finally {
            g.dispose();
        }
    }

    private static BufferedImage download_FavIconIco(String host, LogSource logger) throws IOException {
        final String url = "http://" + host + "/favicon.ico";
        final Browser favBr = new Browser();
        favBr.setLogger(logger);
        favBr.setConnectTimeout(10000);
        favBr.setReadTimeout(10000);
        URLConnectionAdapter con = null;
        BufferedInputStream inputStream = null;
        try {
            /* we first try favicon.ico in root */
            favBr.setFollowRedirects(true);
            favBr.getHeaders().put("Accept-Encoding", null);
            con = favBr.openGetConnection(url);
            if (con.isOK() && !StringUtils.containsIgnoreCase(con.getContentType(), "text")) {
                /* we use bufferedinputstream to reuse it later if needed */
                inputStream = new BufferedInputStream(con.getInputStream());
                inputStream.mark(Integer.MAX_VALUE);
                try {
                    /* try first with iconloader */
                    final List<BufferedImage> ret = ICODecoder.read(inputStream);
                    final BufferedImage img = returnBestImage(ret);
                    if (img != null) {
                        return img;
                    }
                    throw new Throwable("Try again with other ImageLoader");
                } catch (Throwable e) {
                    /* retry with normal image download */
                    inputStream.reset();
                    /* maybe redirect to different icon format? */
                    final BufferedImage img = downloadImage(inputStream);
                    if (img != null && img.getHeight() > 1 && img.getWidth() > 1) {
                        return img;
                    }
                }
            }
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (final Throwable e) {
            }
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    /*
     * dirty hack to count number of unique colors, use only for small images like favicons!
     */
    private static int countColors(BufferedImage image) {
        HashSet<Integer> color = new HashSet<Integer>();
        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {
                color.add(image.getRGB(x, y));
            }
        }
        return color.size();
    }

    private static BufferedImage returnBestImage(List<BufferedImage> images) {
        if (images != null && images.size() > 0) {
            BufferedImage img = null;
            int size = -1;
            int colors = -1;
            for (BufferedImage img2 : images) {
                /*
                 * loop through all available images to find best resolution
                 */
                if (img2 == null) {
                    continue;
                }
                if (img == null || (img2.getHeight() * img2.getWidth()) > size || countColors(img2) > colors) {
                    img = img2;
                    size = img.getHeight() * img.getWidth();
                    colors = countColors(img);
                }
            }
            if (img != null && img.getHeight() > 1 && img.getWidth() > 1) {
                return img;
            }
        }
        return null;
    }

    private static BufferedImage download_FavIconTag(String host, LogSource logger) throws IOException {
        final Browser favBr = new Browser();
        favBr.setLogger(logger);
        favBr.setConnectTimeout(10000);
        favBr.setReadTimeout(10000);
        URLConnectionAdapter con = null;
        BufferedInputStream inputStream = null;
        try {
            favBr.setFollowRedirects(true);
            favBr.getPage("http://" + host);
            String url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=('|\")([^>'\"]*?\\.(ico|png).*?)('|\")").getMatch(4);
            if (StringUtils.isEmpty(url)) {
                url = favBr.getRegex("href=('|\")([^>'\"]*?\\.(ico|png).*?)('|\")[^>]*?rel=('|\")(SHORTCUT )?ICON('|\")").getMatch(1);
            }
            if (StringUtils.isEmpty(url)) {
                /*
                 * workaround for hoster with not complete url, eg rapidshare.com
                 */
                url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=[^>]*?//([^>'\"]*?\\.(ico|png).*?)('|\")").getMatch(3);
                if (!StringUtils.isEmpty(url) && !url.equalsIgnoreCase(host)) {
                    url = "http://" + url;
                }
            }
            if (!StringUtils.isEmpty(url)) {
                /* favicon tag with ico extension */
                favBr.setFollowRedirects(true);
                favBr.getHeaders().put("Accept-Encoding", null);
                con = favBr.openGetConnection(url);
                /* we use bufferedinputstream to reuse it later if needed */
                inputStream = new BufferedInputStream(con.getInputStream());
                inputStream.mark(Integer.MAX_VALUE);
                if (con.isOK() && !StringUtils.containsIgnoreCase(con.getContentType(), "text")) {
                    try {
                        /* try first with iconloader */
                        final List<BufferedImage> ret = ICODecoder.read(inputStream);
                        final BufferedImage img = returnBestImage(ret);
                        if (img != null) {
                            return img;
                        }
                        throw new Throwable("Try again with other ImageLoader");
                    } catch (Throwable e) {
                        /* retry with normal image download */
                        inputStream.reset();
                        /* maybe redirect to different icon format? */
                        final BufferedImage img = downloadImage(inputStream);
                        if (img != null && img.getHeight() > 1 && img.getWidth() > 1) {
                            return img;
                        }
                    }
                }
            }
            return null;
        } finally {
            try {
                inputStream.close();
            } catch (final Throwable e) {
            }
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
        }
    }

    public static BufferedImage downloadFavIcon(List<String> hosts) {
        for (final String host : hosts) {
            final BufferedImage ret = downloadFavIcon(host);
            if (ret != null) {
                return ret;
            }
        }
        return null;
    }

    /**
     * downloads a favicon from the given host, icon must be bigger than 1x1, cause some hosts have fake favicon.ico with 1x1 size
     */
    public static BufferedImage downloadFavIcon(String host) {
        final LogSource logger = LogController.getFastPluginLogger("FavIcons");
        logger.info("Download FavIcon for " + host);
        BufferedImage ret = null;
        try {
            try {
                /* first try to get the FavIcon specified in FavIconTag */
                ret = download_FavIconTag(host, logger);
            } catch (Throwable e) {
            }
            if (ret == null) {
                try {
                    /* fallback to favicon.ico in host root */
                    ret = download_FavIconIco(host, logger);
                } catch (Throwable e) {
                }
            }
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

    private static BufferedImage downloadImage(BufferedInputStream is) {
        try {
            BufferedImage ret = ImageIO.read(is);
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
        }
        return null;
    }
}
