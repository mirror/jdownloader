package jd.controlling;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.captcha.utils.GifDecoder;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.http.URLConnectionAdapter;
import jd.nutils.JDImage;
import jd.utils.JDUtilities;
import net.sf.image4j.codec.ico.ICODecoder;

//final, because the constructor calls Thread.start(),
//see http://findbugs.sourceforge.net/bugDescriptions.html#SC_START_IN_CTOR
public final class FavIconController extends SubConfiguration implements Runnable {

    private static final long                                               serialVersionUID = -1455068138306163872L;
    private static final Object                                             LOCK             = new Object();
    private static final Object                                             WAITLOCK         = new Object();
    private final static LinkedHashMap<String, ArrayList<FavIconRequestor>> queue            = new LinkedHashMap<String, ArrayList<FavIconRequestor>>();
    private ArrayList<String>                                               failed           = new ArrayList<String>();

    public static ImageIcon getFavIcon(String favIconhost, FavIconRequestor requestor, boolean useOriginalHost) {
        String host = useOriginalHost == false ? Browser.getHost(favIconhost) : favIconhost;
        if (host == null) return null;
        synchronized (LOCK) {
            /* check if we already have a favicon? */
            ImageIcon image = JDImage.getImageIcon("favicons/" + host);
            if (image != null) return image;
        }
        /* add to queue list */
        getInstance().add(host, requestor);
        return null;
    }

    private synchronized void add(String host, FavIconRequestor requestor) {
        synchronized (LOCK) {
            /* dont try this host again? */
            if (failed.contains(host)) return;
            /* enqueu this host for favicon loading */
            ArrayList<FavIconRequestor> ret = queue.get(host);
            if (ret == null) {
                ret = new ArrayList<FavIconRequestor>();
                queue.put(host, ret);
            }
            /* add to queue */
            if (requestor != null) ret.add(requestor);
        }
        /* notify our favicon loader */
        synchronized (WAITLOCK) {
            waitFlag = false;
            WAITLOCK.notifyAll();
        }
    }

    private final static FavIconController INSTANCE = new FavIconController();

    public final static FavIconController getInstance() {
        return INSTANCE;
    }

    private FavIconController() {
        super("FavIconController");
        thread = new Thread(this);
        thread.setDaemon(true);
        thread.setName("FavIconLoader");
        failed = getGenericProperty("failedList", new ArrayList<String>());
        Long lastRefresh = getGenericProperty("lastRefresh", Long.valueOf(0));
        if ((System.currentTimeMillis() - lastRefresh) > (1000l * 60 * 60 * 24 * 7)) {
            JDLogger.getLogger().info("FavIcon Refresh Timeout");
            failed.clear();
        }
        thread.start();
    }

    private String  host;
    private boolean waitFlag;
    private Thread  thread;
    private boolean started = false;

    public void run() {
        synchronized (this) {
            if (started) return;
            started = true;
        }
        while (true) {
            synchronized (WAITLOCK) {
                while (waitFlag) {
                    try {
                        WAITLOCK.wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                synchronized (LOCK) {
                    /* check if have something to do */
                    if (!queue.isEmpty()) {
                        Iterator<String> it = queue.keySet().iterator();
                        if (it.hasNext()) {
                            host = it.next();
                        } else {
                            host = null;
                        }
                    } else {
                        waitFlag = true;
                        host = null;
                    }
                }
                if (host == null || waitFlag) continue;
            }
            /* Download FavIcon */
            BufferedImage favicon = downloadFavIcon(host);
            // JDLogger.getLogger().severe("downloading favicon from: " + host +
            // (favicon == null ? " failed!" : " ok!"));
            synchronized (LOCK) {
                ArrayList<FavIconRequestor> requestors = queue.remove(host);
                if (favicon == null) {
                    /* favicon loader failed, add to failed list */
                    if (!failed.contains(host)) failed.add(host);
                } else {
                    try {
                        /* buffer favicon to disk */
                        File imageFile = JDUtilities.getResourceFile("jd/img/favicons/" + host + ".png", true);
                        ImageIO.write(favicon, "png", imageFile);
                        /* load and scale it again */
                        ImageIcon image = JDImage.getImageIcon("favicons/" + host);
                        if (image != null && requestors != null) {
                            /* refresh icons for all queued plugins */
                            for (FavIconRequestor requestor : requestors) {
                                requestor.setFavIcon(image);
                            }
                        }
                    } catch (Exception e) {
                        JDLogger.exception(e);
                    }
                }
            }
        }
    }

    /**
     * downloads a favicon from the given host, icon must be bigger than 1x1,
     * cause some hosts have fake favicon.ico with 1x1 size
     */
    public BufferedImage downloadFavIcon(String host) {
        String url = "http://" + host + "/favicon.ico";
        final Browser favBr = new Browser();
        favBr.setLogger(JDPluginLogger.Trash);
        favBr.setConnectTimeout(10000);
        favBr.setReadTimeout(10000);
        URLConnectionAdapter con = null;
        BufferedInputStream inputStream = null;
        try {
            /* we first try favicon.ico in root */
            favBr.setFollowRedirects(true);
            favBr.getHeaders().put("Accept-Encoding", "");
            con = favBr.openGetConnection(url);
            if (con.isOK()) {
                /* we use bufferedinputstream to reuse it later if needed */
                inputStream = new BufferedInputStream(con.getInputStream());
                inputStream.mark(Integer.MAX_VALUE);
                try {
                    /* try first with iconloader */
                    List<BufferedImage> ret = ICODecoder.read(inputStream);
                    if (ret.size() > 0) {
                        BufferedImage img = ret.get(0);
                        if (img != null && img.getHeight() > 1 && img.getWidth() > 1) return img;
                    }
                    throw new Throwable("Try again with other ImageLoader");
                } catch (Throwable e) {
                    /* retry with normal image download */
                    inputStream.reset();
                    /* maybe redirect to different icon format? */
                    BufferedImage img = downloadImage(inputStream);
                    if (img != null && img.getHeight() > 1 && img.getWidth() > 1) return img;
                }
            }
            /* now we look for a favicon tag */
            try {
                inputStream.close();
            } catch (final Throwable e) {
            }
            try {
                con.disconnect();
            } catch (final Throwable e) {
            }
            favBr.getPage("http://" + host);
            url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=('|\")([^>'\"]*?)('|\")").getMatch(4);
            if (url == null || url.length() == 0) url = favBr.getRegex("href=('|\")([^>'\"]*?)('|\")[^>]*?rel=('|\")(SHORTCUT )?ICON('|\")").getMatch(1);
            if (url == null || url.length() == 0) {
                /*
                 * workaround for hoster with not complete url, eg
                 * rapidshare.com
                 */
                url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=.*?//([^>'\"]*?)('|\")").getMatch(3);
                if (url != null && url.length() > 0) url = "http://" + url;
            }
            if (url != null && url.length() > 0) {
                /* favicon tag with ico extension */
                favBr.getHeaders().put("Accept-Encoding", "");
                con = favBr.openGetConnection(url);
                /* we use bufferedinputstream to reuse it later if needed */
                inputStream = new BufferedInputStream(con.getInputStream());
                inputStream.mark(Integer.MAX_VALUE);
                if (con.isOK()) {
                    try {
                        /* try first with iconloader */
                        List<BufferedImage> ret = ICODecoder.read(inputStream);
                        if (ret.size() > 0) {
                            BufferedImage img = ret.get(0);
                            if (img != null && img.getHeight() > 1 && img.getWidth() > 1) return img;
                        }
                        throw new Throwable("Try again with other ImageLoader");
                    } catch (Throwable e) {
                        /* retry with normal image download */
                        inputStream.reset();
                        /* maybe redirect to different icon format? */
                        BufferedImage img = downloadImage(inputStream);
                        if (img != null && img.getHeight() > 1 && img.getWidth() > 1) return img;
                    }
                }
            }
        } catch (Throwable e) {
            // JDLogger.exception(e);
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
        return null;
    }

    private BufferedImage downloadImage(BufferedInputStream is) {
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

    public void saveSyncnonThread() {
        final String id = JDController.requestDelayExit("faviconcontroller");
        synchronized (LOCK) {
            setProperty("lastRefresh", Long.valueOf(System.currentTimeMillis()));
            save();
        }
        JDController.releaseDelayExit(id);
    }

    public void resetFailedList() {
        synchronized (LOCK) {
            failed.clear();
            setProperty("lastRefresh", Long.valueOf(0));
            save();
        }
    }

}
