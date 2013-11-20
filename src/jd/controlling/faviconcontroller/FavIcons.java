package jd.controlling.faviconcontroller;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import java.util.logging.Logger;

import javax.imageio.ImageIO;
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
import org.appwork.utils.logging2.LogSource;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;
import org.jdownloader.plugins.controller.host.HostPluginController;
import org.jdownloader.plugins.controller.host.LazyHostPlugin;
import org.jdownloader.updatev2.gui.LAFOptions;

public class FavIcons {

    private static final ThreadPoolExecutor                                      THREAD_POOL;
    private static final AtomicInteger                                           THREADCOUNTER = new AtomicInteger(0);
    private static final Object                                                  LOCK          = new Object();
    private final static LinkedHashMap<String, java.util.List<FavIconRequestor>> QUEUE         = new LinkedHashMap<String, java.util.List<FavIconRequestor>>();
    private static HashMap<String, ImageIcon>                                    FAILED_ICONS  = new HashMap<String, ImageIcon>();
    private static HashMap<String, ImageIcon>                                    DEFAULT_ICONS = new HashMap<String, ImageIcon>();
    private static LogSource                                                     LOGGER;

    private static final FavIconsConfig                                          CONFIG        = JsonConfig.create(FavIconsConfig.class);
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

        long lastRefresh = CONFIG.getLastRefresh();
        /* load failed hosts list */
        ArrayList<String> FAILED_ARRAY_LIST = JsonConfig.create(FavIconsConfig.class).getFailedHosts();
        if (FAILED_ARRAY_LIST == null || (System.currentTimeMillis() - lastRefresh) > (1000l * 60 * 60 * 24 * 7)) {
            /* timeout is over, lets try again the failed ones */
            FAILED_ARRAY_LIST = new ArrayList<String>();
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
                CONFIG.setLastRefresh(System.currentTimeMillis());
                ArrayList<String> failedHosts = null;
                synchronized (LOCK) {
                    failedHosts = new ArrayList<String>(FAILED_ICONS.keySet());
                }
                CONFIG.setFailedHosts(failedHosts);
            }

        });

    }

    public static ImageIcon getFavIcon(String host, FavIconRequestor requestor) {
        if (host == null) return null;
        ImageIcon image = null;
        synchronized (LOCK) {
            /* check if we already have a favicon? */
            if (NewTheme.I().hasIcon("fav/" + host)) {
                image = NewTheme.I().getIcon("fav/" + host, -1);
            }
            if (image == null && NewTheme.I().hasIcon("fav/big." + host)) {
                image = NewTheme.I().getIcon("fav/big." + host, -1);
            }
            if (image != null) {
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
                if (clearAfterGet == false) DEFAULT_ICONS.put(host, ret);
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
            java.util.List<FavIconRequestor> ret = QUEUE.get(host);
            boolean enqueueFavIcon = false;
            if (ret == null) {
                ret = new ArrayList<FavIconRequestor>();
                QUEUE.put(host, ret);
                enqueueFavIcon = true;
            }
            /* add to queue */
            if (requestor != null) ret.add(requestor);
            if (enqueueFavIcon) {
                THREAD_POOL.execute(new Runnable() {

                    public void run() {
                        LazyHostPlugin existingHostPlugin = HostPluginController.getInstance().get(host);
                        if (existingHostPlugin != null && "jd.plugins.hoster.Offline".equals(existingHostPlugin.getClassname())) {
                            synchronized (LOCK) {
                                QUEUE.remove(host);
                                ImageIcon failedIcon = FAILED_ICONS.get(host);
                                if (failedIcon == null) {
                                    failedIcon = getDefaultIcon(host, true);
                                    FAILED_ICONS.put(host, failedIcon);
                                }
                            }
                            return;
                        }

                        String tryHost = host;
                        ArrayList<String> tryHosts = new ArrayList<String>();
                        int index = 0;
                        /* this loop adds every subdomain and the tld to tryHosts and we try to fetch a favIcon in same order */
                        while (true) {
                            tryHosts.add(tryHost);
                            if ((index = tryHost.indexOf(".")) >= 0 && tryHost.length() >= index + 1) {
                                tryHost = tryHost.substring(index + 1);
                                if (tryHost.indexOf(".") >= 0) {
                                    continue;
                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        }
                        BufferedImage favicon = null;
                        for (String tryHost2 : tryHosts) {
                            favicon = downloadFavIcon(tryHost2);
                            if (favicon != null) break;
                        }
                        synchronized (LOCK) {
                            java.util.List<FavIconRequestor> requestors = QUEUE.remove(host);
                            if (favicon == null) {
                                ImageIcon failedIcon = FAILED_ICONS.get(host);
                                if (failedIcon == null) {
                                    failedIcon = getDefaultIcon(host, true);
                                    FAILED_ICONS.put(host, failedIcon);
                                }
                            } else {
                                FileOutputStream fos = null;
                                try {
                                    /* buffer favicon to disk */
                                    File imageFile = Application.getResource(NewTheme.I().getPath() + "/images/fav/" + host + ".png");
                                    FileCreationManager.getInstance().mkdir(imageFile.getParentFile());
                                    fos = new FileOutputStream(imageFile);
                                    ImageIO.write(favicon, "png", fos);
                                    /* load and scale it again */
                                    ImageIcon image = NewTheme.I().getIcon("fav/" + host, -1);
                                    if (image != null && requestors != null) {
                                        /* refresh icons for all queued plugins */
                                        for (FavIconRequestor requestor : requestors) {
                                            requestor.setFavIcon(image);
                                        }
                                    }
                                } catch (Throwable e) {
                                    LogController.getInstance().getLogger("FavIcons").log(e);
                                } finally {
                                    try {
                                        fos.close();
                                    } catch (final Throwable e) {
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
        int w = 16;
        int h = 16;
        int size = 9;
        Color fg = Color.BLACK;
        Color bg = Color.WHITE;
        LOGGER.info("Create Favicon: " + host);
        try {
            bg = LAFOptions.getInstance().getColorForPanelHeaderBackground();
        } catch (final Throwable e) {
            e.printStackTrace();
        }

        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice gd = ge.getDefaultScreenDevice();

        GraphicsConfiguration gc = gd.getDefaultConfiguration();
        final BufferedImage image = gc.createCompatibleImage(w, h, Transparency.BITMASK);

        String tld = Files.getExtension(host);
        if (tld != null) tld = tld.toLowerCase(Locale.ENGLISH);
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
        if (dummy.length() <= 0 || dummy.length() > 2) dummy = host.substring(0, Math.min(host.length(), 2));
        // paint
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setFont(new Font("Arial", Font.BOLD, size));
        RoundRectangle2D roundedRectangle = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, 5, 5);
        g.setColor(bg);
        g.fill(roundedRectangle);
        g.setColor(bg.darker());
        g.draw(roundedRectangle);
        g.setColor(fg);
        Rectangle2D bounds = g.getFontMetrics().getStringBounds(dummy, g);
        g.drawString(dummy, (int) (w - bounds.getWidth()) / 2, (int) (-bounds.getY() + (h - bounds.getHeight()) / 2) - (tld == null ? 0 : 1));
        if (tld != null) {
            g.setFont(new Font("Arial", 0, 6));
            bounds = g.getFontMetrics().getStringBounds("." + tld, g);
            g.drawString("." + tld, (int) (w - bounds.getWidth()) - 2, (int) (h) - 2);
        }
        g.dispose();
        return image;
    }

    private static BufferedImage download_FavIconIco(String host, Logger logger) throws IOException {
        String url = "http://" + host + "/favicon.ico";
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
            if (con.isOK()) {
                /* we use bufferedinputstream to reuse it later if needed */
                inputStream = new BufferedInputStream(con.getInputStream());
                inputStream.mark(Integer.MAX_VALUE);
                try {
                    /* try first with iconloader */
                    List<BufferedImage> ret = ICODecoder.read(inputStream);
                    BufferedImage img = returnBestImage(ret);
                    if (img != null) { return img; }
                    throw new Throwable("Try again with other ImageLoader");
                } catch (Throwable e) {
                    /* retry with normal image download */
                    inputStream.reset();
                    /* maybe redirect to different icon format? */
                    BufferedImage img = downloadImage(inputStream);
                    if (img != null && img.getHeight() > 1 && img.getWidth() > 1) { return img; }
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
                if (img2 == null) continue;
                if (img == null || (img2.getHeight() * img2.getWidth()) > size || countColors(img2) > colors) {
                    img = img2;
                    size = img.getHeight() * img.getWidth();
                    colors = countColors(img);
                }
            }
            if (img != null && img.getHeight() > 1 && img.getWidth() > 1) return img;
        }
        return null;
    }

    private static BufferedImage download_FavIconTag(String host, Logger logger) throws IOException {
        final Browser favBr = new Browser();
        favBr.setLogger(logger);
        favBr.setConnectTimeout(10000);
        favBr.setReadTimeout(10000);
        URLConnectionAdapter con = null;
        BufferedInputStream inputStream = null;
        try {
            favBr.setFollowRedirects(true);
            favBr.getPage("http://" + host);
            String url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=('|\")([^>'\"]*?)('|\")").getMatch(4);
            if (StringUtils.isEmpty(url)) url = favBr.getRegex("href=('|\")([^>'\"]*?)('|\")[^>]*?rel=('|\")(SHORTCUT )?ICON('|\")").getMatch(1);
            if (StringUtils.isEmpty(url)) {
                /*
                 * workaround for hoster with not complete url, eg rapidshare.com
                 */
                url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=[^>]*?//([^>'\"]*?)('|\")").getMatch(3);
                if (!StringUtils.isEmpty(url) && !url.equalsIgnoreCase(host)) url = "http://" + url;
            }
            if (url != null && url.equalsIgnoreCase(host)) url = null;
            if (url == null && "rapidshare.com".equalsIgnoreCase(host)) {
                /*
                 * hardcoded workaround for rapidshare, they use js to build the favicon path
                 */
                url = "http://images3.rapidshare.com/img/favicon.ico";
            }
            if (!StringUtils.isEmpty(url)) {
                /* favicon tag with ico extension */
                favBr.setFollowRedirects(false);
                favBr.getHeaders().put("Accept-Encoding", null);
                con = favBr.openGetConnection(url);
                /* we use bufferedinputstream to reuse it later if needed */
                inputStream = new BufferedInputStream(con.getInputStream());
                inputStream.mark(Integer.MAX_VALUE);
                if (con.isOK()) {
                    try {
                        /* try first with iconloader */
                        List<BufferedImage> ret = ICODecoder.read(inputStream);
                        BufferedImage img = returnBestImage(ret);
                        if (img != null) { return img; }
                        throw new Throwable("Try again with other ImageLoader");
                    } catch (Throwable e) {
                        /* retry with normal image download */
                        inputStream.reset();
                        /* maybe redirect to different icon format? */
                        BufferedImage img = downloadImage(inputStream);
                        if (img != null && img.getHeight() > 1 && img.getWidth() > 1) { return img; }
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

    /**
     * downloads a favicon from the given host, icon must be bigger than 1x1, cause some hosts have fake favicon.ico with 1x1 size
     */
    public static BufferedImage downloadFavIcon(String host) {
        LogSource logger = LogController.getInstance().getLogger("FavIcons");
        logger.setAllowTimeoutFlush(false);
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
                GifDecoder d = new GifDecoder();
                /* reset bufferedinputstream to begin from start */
                is.reset();
                if (d.read(is) == 0) ret = d.getImage();
            }
            return ret;
        } catch (Throwable e) {
        }
        return null;
    }

}
