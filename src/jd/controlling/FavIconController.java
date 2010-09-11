package jd.controlling;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import jd.captcha.utils.GifDecoder;
import jd.config.SubConfiguration;
import jd.http.Browser;
import jd.nutils.JDImage;
import jd.utils.JDUtilities;
import net.sf.image4j.codec.ico.ICODecoder;

public class FavIconController extends SubConfiguration implements Runnable {

    private static final long serialVersionUID = -1455068138306163872L;
    private static final Object LOCK = new Object();
    private final static HashMap<String, ArrayList<FavIconRequestor>> queue = new HashMap<String, ArrayList<FavIconRequestor>>();
    private ArrayList<String> failed = new ArrayList<String>();

    public static ImageIcon getFavIcon(String favIconhost, FavIconRequestor requestor, boolean useOriginalHost) {
        String host = useOriginalHost == false ? Browser.getHost(favIconhost) : favIconhost;
        if (host == null || requestor == null) return null;
        synchronized (LOCK) {
            /* check if we already have a favicon? */
            Image image = JDImage.getImage("favicons/" + host);
            if (image != null) return new ImageIcon(image);
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
            ret.add(requestor);
        }
        /* notify our favicon loader */
        synchronized (this) {
            if (waitFlag) {
                waitFlag = false;
                notify();
            }
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
        Long lastRefresh = getGenericProperty("lastRefresh", new Long(0));
        if ((System.currentTimeMillis() - lastRefresh) > (1000l * 60 * 60 * 24 * 7)) {
            JDLogger.getLogger().info("FavIcon Refresh Timeout");
            failed.clear();
        }
        thread.start();
    }

    private String host;
    private boolean waitFlag;
    private Thread thread;
    private boolean started = false;

    public void run() {
        synchronized (this) {
            if (started) return;
            started = true;
        }
        while (true) {
            synchronized (this) {
                while (waitFlag) {
                    try {
                        wait();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
            /* Download FavIcon */
            BufferedImage favicon = downloadFavIcon(host);
            JDLogger.getLogger().severe("downloading favicon from: " + host + (favicon == null ? " failed!" : " ok!"));
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
                        Image image = JDImage.getImage("favicons/" + host);
                        if (image != null) {
                            /* refresh icons for all queued plugins */
                            for (FavIconRequestor requestor : requestors) {
                                requestor.setFavIcon(new ImageIcon(image));
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
     * downloads a favicon from the given host
     */
    public BufferedImage downloadFavIcon(String host) {
        String url = "http://" + host + "/favicon.ico";
        final Browser favBr = new Browser();
        favBr.setConnectTimeout(10000);
        favBr.setReadTimeout(10000);
        try {
            /* we first try favicon.ico in root */
            favBr.setFollowRedirects(true);
            favBr.openGetConnection(url);
            favBr.getHeaders().put("Accept-Encoding", "");
            if (favBr.getHttpConnection().isOK()) {
                try {
                    List<BufferedImage> ret = ICODecoder.read(favBr.getHttpConnection().getInputStream());
                    if (ret.size() > 0) return ret.get(0);
                } catch (Throwable e) {
                    // JDLogger.exception(e);
                    /* maybe redirect to different icon format? */
                    BufferedImage ret = downloadImage(favBr.getHttpConnection().getURL().toString());
                    if (ret != null) return ret;
                }
            }
            /* now we look for a favicon tag */
            favBr.getHttpConnection().disconnect();
            favBr.getPage("http://" + host);
            url = favBr.getRegex("rel=('|\")(SHORTCUT )?ICON('|\")[^>]*?href=('|\")([^>'\"]*?)('|\")").getMatch(4);
            if (url == null) url = favBr.getRegex("href=('|\")([^>'\"]*?)('|\")[^>]*?rel=('|\")(SHORTCUT )?ICON('|\")").getMatch(1);
            if (url != null) {
                /* favicon tag with ico extension */
                favBr.getHeaders().put("Accept-Encoding", "");
                favBr.openGetConnection(url);
                if (favBr.getHttpConnection().isOK()) {
                    try {
                        List<BufferedImage> ret = ICODecoder.read(favBr.getHttpConnection().getInputStream());
                        if (ret.size() > 0) return ret.get(0);
                    } catch (Throwable e) {
                        // JDLogger.exception(e);
                        /* maybe not icon format? */
                        BufferedImage ret = downloadImage(favBr.getHttpConnection().getURL().toString());
                        if (ret != null) return ret;
                    }
                }
            }
        } catch (Throwable e) {
            // JDLogger.exception(e);
        } finally {
            try {
                favBr.getHttpConnection().disconnect();
            } catch (Exception e) {
            }
        }
        return null;
    }

    private BufferedImage downloadImage(String url) {
        File tmp = JDUtilities.getResourceFile("tmp/" + host, true);
        if (tmp.exists()) tmp.delete();
        tmp.deleteOnExit();
        try {
            Browser.download(tmp, url);
            BufferedImage ret = ImageIO.read(tmp);
            if (ret == null) {
                /* workaround for gif images */
                GifDecoder d = new GifDecoder();
                BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmp));
                if (d.read(in) == 0) ret = d.getImage();
            }
            return ret;
        } catch (Throwable e) {
        }
        return null;
    }

    public void saveSyncnonThread() {
        final String id = JDController.requestDelayExit("faviconcontroller");
        synchronized (LOCK) {
            setProperty("lastRefresh", new Long(System.currentTimeMillis()));
            save();
        }
        JDController.releaseDelayExit(id);
    }

}
