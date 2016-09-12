package jd.controlling;

import java.awt.GraphicsEnvironment;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;
import jd.parser.html.HTMLParser;

import org.appwork.exceptions.WTFException;
import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogSource;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.os.CrossSystem.OperatingSystem;
import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferable;
import org.jdownloader.logging.LogController;

import sun.awt.datatransfer.SunClipboard;

public class ClipboardMonitoring {

    public static class HTMLFragment {

        private final String sourceURL;

        public final String getSourceURL() {
            return sourceURL;
        }

        public final String getFragment() {
            return fragment;
        }

        private final String fragment;

        private HTMLFragment(final String sourceURL, final String fragment) {
            this.sourceURL = sourceURL;
            this.fragment = fragment;
        }
    }

    private static class ClipboardHash {
        private final int hash;
        private final int length;

        private ClipboardHash(int hash, int length) {
            this.hash = hash;
            this.length = length;
        }

        private ClipboardHash(String string) {
            if (string == null) {
                this.hash = 0;
                this.length = -1;
            } else {
                this.hash = string.hashCode();
                this.length = string.length();
            }
        }

        private boolean equals(String string) {
            if (string == null) {
                return length == -1 && hash == 0;
            } else {
                return length == string.length() && hash == string.hashCode();
            }
        }

    }

    private static class WindowsClipboardHack {
        Method                  openClipboard    = null;
        Method                  closeClipboard   = null;
        Method                  getClipboardData = null;
        long                    cf_html          = -1;
        final Clipboard         clipboard;
        private final LogSource logger;

        private WindowsClipboardHack(Clipboard clipboard, final LogSource logger) throws Exception {
            this.clipboard = clipboard;
            this.logger = logger;
            try {
                final Field cf_html_field = Class.forName("sun.awt.windows.WDataTransferer").getDeclaredField("CF_HTML");
                if (cf_html_field != null) {
                    cf_html_field.setAccessible(true);
                    cf_html = (Long) cf_html_field.get(null);
                    openClipboard = clipboard.getClass().getDeclaredMethod("openClipboard", new Class[] { SunClipboard.class });
                    openClipboard.setAccessible(true);
                    closeClipboard = clipboard.getClass().getDeclaredMethod("closeClipboard", new Class[] {});
                    closeClipboard.setAccessible(true);
                    getClipboardData = clipboard.getClass().getDeclaredMethod("getClipboardData", new Class[] { long.class });
                    getClipboardData.setAccessible(true);
                } else {
                    throw new WTFException("CF_HTML not available");
                }
            } catch (final Throwable e) {
                throw new Exception(e);
            }
        }

        private String getURLFromCF_HTML() {
            try {
                try {
                    openClipboard.invoke(clipboard, new Object[] { null });
                    final String sstr = new String((byte[]) getClipboardData.invoke(clipboard, new Object[] { cf_html }), "UTF-8");
                    return new Regex(sstr, "SourceURL:([^\r\n]*)").getMatch(0);
                } finally {
                    closeClipboard.invoke(clipboard, new Object[] {});
                }
            } catch (final InvocationTargetException ignore) {
            } catch (final IllegalStateException ignore) {
            } catch (final IOException ignore) {
            } catch (final Throwable e) {
                logger.log(e);
            }
            return null;
        }
    }

    protected static class ClipboardChangeDetector {

        protected volatile int        waitTimeout;
        protected final AtomicBoolean skipChangeFlag;

        protected ClipboardChangeDetector(final AtomicBoolean skipChangeFlag) {
            this.skipChangeFlag = skipChangeFlag;
            waitTimeout = getMinWaitTimeout();
        }

        protected void waitForClipboardChanges() throws InterruptedException {
            while (true) {
                synchronized (this) {
                    this.wait(waitTimeout);
                }
                if (hasChanges()) {
                    return;
                }
            }
        }

        protected int getMinWaitTimeout() {
            return 200;
        }

        protected int getMaxWaitTimeout() {
            return 1000;
        }

        protected int getWaitTimeoutInc() {
            return 200;
        }

        protected boolean hasChanges() {
            if (skipChangeFlag.get()) {
                waitTimeout = getMinWaitTimeout();
                return false;
            } else {
                waitTimeout = Math.min(waitTimeout + getWaitTimeoutInc(), getMaxWaitTimeout());
                return true;
            }
        }

        protected void slowDown(Throwable e) {
            waitTimeout = 5000;
        }

        protected void restart() {
            waitTimeout = getMinWaitTimeout();
        }
    }

    public static class ClipboardContent {
        private final String content;
        private final String browserURL;

        public String getContent() {
            return content;
        }

        public String getBrowserURL() {
            return browserURL;
        }

        private ClipboardContent(String content, String browserURL) {
            this.content = content;
            this.browserURL = browserURL;
        }
    }

    private static final ClipboardMonitoring INSTANCE            = new ClipboardMonitoring();
    private static DataFlavor                URLFLAVOR           = null;
    private static DataFlavor                URILISTFLAVOR       = null;
    private volatile Thread                  monitoringThread    = null;
    private final Clipboard                  clipboard;
    private final AtomicBoolean              skipChangeDetection = new AtomicBoolean(false);
    private final WindowsClipboardHack       windowsClipboardHack;
    private final static AtomicBoolean       SKIP_FIRST_ROUND    = new AtomicBoolean(true);
    private final static AtomicBoolean       HTML_FLAVOR_ALLOWED = new AtomicBoolean(true);
    private final ClipboardChangeDetector    clipboardChangeDetector;
    private final LogSource                  logger;

    public static boolean isHtmlFlavorAllowed() {
        return HTML_FLAVOR_ALLOWED.get();
    }

    public static void setHtmlFlavorAllowed(final boolean htmlFlavor) {
        ClipboardMonitoring.HTML_FLAVOR_ALLOWED.set(htmlFlavor);
    }

    public static void setSkipFirstRound(final boolean b) {
        SKIP_FIRST_ROUND.set(b);
    }

    private boolean ignoreTransferable(Transferable transferable) {
        if (transferable == null) {
            return true;
        }
        if (transferable.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
            /* we have Package/Children in clipboard, skip them */
            return true;
        }
        try {
            if (transferable.getClass().getName().contains("TransferableProxy")) {
                Field isLocal = transferable.getClass().getDeclaredField("isLocal");
                if (isLocal != null) {
                    isLocal.setAccessible(true);
                    if (Boolean.TRUE.equals(isLocal.getBoolean(transferable))) {
                        return true;
                    }
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public static Transferable getTransferable() {
        return getINSTANCE().clipboard.getContents(null);
    }

    public synchronized void startMonitoring() {
        if (clipboard != null) {
            if (isMonitoring()) {
                return;
            }
            monitoringThread = new Thread() {
                private final AtomicLong roundIndex       = new AtomicLong(0);
                private ClipboardHash    oldStringContent = new ClipboardHash(0, -1);
                private ClipboardHash    oldListContent   = new ClipboardHash(0, -1);
                private ClipboardHash    oldHTMLFragment  = new ClipboardHash(0, -1);

                @Override
                public void run() {
                    while (Thread.currentThread() == monitoringThread) {
                        try {
                            clipboardChangeDetector.waitForClipboardChanges();
                            if (Thread.currentThread() != monitoringThread) {
                                return;
                            }
                        } catch (InterruptedException e) {
                            logger.finer("Interrupted ClipBoard Monitoring Thread");
                            return;
                        }
                        try {
                            final Transferable currentContent = clipboard.getContents(null);
                            if (ignoreTransferable(currentContent)) {
                                continue;
                            }
                            String handleThisRound = null;
                            boolean macOSWorkaroundNeeded = false;
                            try {
                                /* change detection for List/URI content */
                                final String newListContent = getListTransferData(currentContent);
                                try {
                                    if (!oldListContent.equals(newListContent)) {
                                        handleThisRound = newListContent;
                                    }
                                } finally {
                                    oldListContent = new ClipboardHash(newListContent);
                                }
                            } catch (final Throwable e) {
                                if (CrossSystem.isMac() && e instanceof IOException) {
                                    /**
                                     * Couldn't get a file system path for a URL: file:///.file/id=6571367.1715588 2014-02-28 09:52:10.362
                                     * java[1637:507] Looked for URLs on the pasteboard, but found none.
                                     **/
                                    macOSWorkaroundNeeded = true;
                                }
                            }
                            String browserURL = null;
                            final boolean htmlFlavorAllowed = isHtmlFlavorAllowed();
                            if (StringUtils.isEmpty(handleThisRound)) {
                                /* change detection for String/HTML content */
                                final String newStringContent = getStringTransferData(currentContent);
                                try {
                                    if (!oldStringContent.equals(newStringContent)) {
                                        /*
                                         * we only use normal String Content to detect a change
                                         */
                                        handleThisRound = newStringContent;
                                        try {
                                            /*
                                             * lets fetch fresh HTML Content if available
                                             */
                                            final HTMLFragment htmlFragment = getHTMLFragment(currentContent);
                                            if (htmlFragment != null) {
                                                /*
                                                 * remember that we had HTML content this round
                                                 */
                                                oldHTMLFragment = new ClipboardHash(htmlFragment.getFragment());
                                                if (htmlFlavorAllowed) {
                                                    handleThisRound = handleThisRound + "\r\n" + htmlFragment.getFragment();
                                                }
                                                browserURL = htmlFragment.getSourceURL();
                                            } else {
                                                oldHTMLFragment = new ClipboardHash(null);
                                            }
                                        } catch (final Throwable e) {
                                        }
                                    } else {
                                        /*
                                         * no String Content change detected, let's verify if the HTML content hasn't changed
                                         */
                                        try {
                                            /*
                                             * lets fetch fresh HTML Content if available
                                             */
                                            final HTMLFragment htmlFragment = getHTMLFragment(currentContent);
                                            if (htmlFragment != null) {
                                                /*
                                                 * remember that we had HTML content this round
                                                 */
                                                if (!oldHTMLFragment.equals(htmlFragment)) {
                                                    oldHTMLFragment = new ClipboardHash(htmlFragment.getFragment());
                                                    if (htmlFlavorAllowed) {
                                                        handleThisRound = newStringContent + "\r\n" + htmlFragment.getFragment();
                                                    }
                                                    browserURL = htmlFragment.getSourceURL();
                                                }
                                            } else {
                                                oldHTMLFragment = new ClipboardHash(null);
                                            }
                                        } catch (final Throwable e) {
                                        }
                                    }
                                } finally {
                                    oldStringContent = new ClipboardHash(newStringContent);
                                }
                            }
                            if (!StringUtils.isEmpty(handleThisRound)) {
                                final long round = roundIndex.getAndIncrement();
                                if (round > 0 || !SKIP_FIRST_ROUND.get()) {
                                    clipboardChangeDetector.restart();
                                    final LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.CLIPBOARD.getLinkOriginDetails(), handleThisRound);
                                    final HashSet<String> pws = PasswordUtils.getPasswords(handleThisRound);
                                    if (pws != null && pws.size() > 0) {
                                        job.setCrawledLinkModifierPrePackagizer(new CrawledLinkModifier() {

                                            @Override
                                            public void modifyCrawledLink(CrawledLink link) {
                                                link.getArchiveInfo().getExtractionPasswords().addAll(pws);
                                            }
                                        });
                                    }
                                    job.setCustomSourceUrl(browserURL);
                                    LinkCollector.getInstance().addCrawlerJob(job);
                                }
                            }
                            if (macOSWorkaroundNeeded) {
                                setCurrentContent(handleThisRound);
                            }
                        } catch (final Throwable e) {
                            clipboardChangeDetector.slowDown(e);
                            final String message = e.getMessage();
                            if (!StringUtils.containsIgnoreCase(message, "cannot open system clipboard") && !StringUtils.containsIgnoreCase(message, "Owner failed to convert data") && !StringUtils.containsIgnoreCase(message, "Owner timed out") && !StringUtils.containsIgnoreCase(message, "system clipboard data unavailable")) {
                                if (message != null) {
                                    logger.severe(message);
                                }
                                logger.log(e);
                            }
                        }
                    }
                }

            };
            monitoringThread.setName("ClipboardMonitor");
            monitoringThread.setDaemon(true);
            monitoringThread.start();
        }
    }

    public synchronized ClipboardContent getCurrentContent(Transferable currentContent) {
        if (currentContent != null) {
            String stringContent = null;
            try {
                stringContent = getStringTransferData(currentContent);
            } catch (final Throwable e) {
            }
            HTMLFragment htmlFragment = null;
            try {
                /* lets fetch fresh HTML Content if available */
                htmlFragment = getHTMLFragment(currentContent);
            } catch (final Throwable e) {
                e.printStackTrace();
            }
            final StringBuilder sb = new StringBuilder();
            if (stringContent != null) {
                sb.append("<");
                sb.append(stringContent);
                sb.append(">");
            }
            if (isHtmlFlavorAllowed() && htmlFragment != null && StringUtils.isNotEmpty(htmlFragment.getFragment())) {
                if (sb.length() > 0) {
                    sb.append("\r\n");
                }
                sb.append("<");
                sb.append(htmlFragment.getFragment());
                sb.append(">");
            }
            if (sb.length() > 0) {
                if (htmlFragment != null) {
                    return new ClipboardContent(sb.toString(), htmlFragment.getSourceURL());
                } else {
                    return new ClipboardContent(sb.toString(), null);
                }
            }
        }
        return null;
    }

    public synchronized ClipboardContent getCurrentContent() {
        if (clipboard != null) {
            try {
                final Transferable currentContent = clipboard.getContents(null);
                return getCurrentContent(currentContent);
            } catch (final Throwable e) {
            }
        }
        return null;
    }

    public synchronized void setCurrentContent(String string) {
        if (clipboard != null) {
            try {
                clipboard.setContents(new StringSelection(string), new ClipboardOwner() {

                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                        skipChangeDetection.set(false);
                    }
                });
                skipChangeDetection.set(true);
            } catch (final Throwable e) {
                skipChangeDetection.set(false);
            }
        }
    }

    public synchronized void setCurrentContent(Transferable object) {
        if (clipboard != null) {
            try {
                clipboard.setContents(object, new ClipboardOwner() {

                    public void lostOwnership(Clipboard clipboard, Transferable contents) {
                        skipChangeDetection.set(false);
                    }
                });
                skipChangeDetection.set(true);
            } catch (final Throwable e) {
                skipChangeDetection.set(false);
            }
        }
    }

    public synchronized void stopMonitoring() {
        if (monitoringThread != null) {
            monitoringThread.interrupt();
        }
        monitoringThread = null;
    }

    public synchronized boolean isMonitoring() {
        return monitoringThread != null && monitoringThread.isAlive();
    }

    public ClipboardMonitoring() {
        logger = LogController.CL(ClipboardMonitoring.class);
        Clipboard lclipboard = null;
        WindowsClipboardHack lclipboardHack = null;
        ClipboardChangeDetector clipboardChangeDetector = null;
        try {
            if (!GraphicsEnvironment.isHeadless()) {
                lclipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                if (lclipboard != null && CrossSystem.isWindows()) {
                    try {
                        lclipboardHack = new WindowsClipboardHack(lclipboard, logger);
                    } catch (final Throwable th) {
                        logger.log(th);
                    }
                    try {
                        if (CrossSystem.getOS().isMinimum(OperatingSystem.WINDOWS_XP)) {
                            clipboardChangeDetector = new WindowsClipboardChangeDetector(skipChangeDetection, logger);
                        }
                    } catch (final Throwable th) {
                        logger.log(th);
                    }
                }
            }
        } catch (final Throwable th) {
            logger.log(th);
        }
        clipboard = lclipboard;
        windowsClipboardHack = lclipboardHack;
        if (clipboard != null) {
            if (clipboardChangeDetector == null) {
                this.clipboardChangeDetector = new ClipboardChangeDetector(skipChangeDetection);
            } else {
                this.clipboardChangeDetector = clipboardChangeDetector;
            }
        } else {
            this.clipboardChangeDetector = null;
        }
    }

    static {
        try {
            URLFLAVOR = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (final Throwable e) {
            LogController.CL().info("urlFlavor not supported");
        }
        try {
            URILISTFLAVOR = new DataFlavor("text/uri-list; class=java.lang.String");
        } catch (final Throwable e) {
            LogController.CL().info("uriListFlavor not supported");
        }
    }

    @Deprecated
    public static String getHTMLTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        final HTMLFragment ret = getHTMLFragment(transferable);
        if (ret != null) {
            return ret.getFragment();
        } else {
            return null;
        }
    }

    public static HTMLFragment getHTMLFragment(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        DataFlavor htmlFlavor = null;
        final Class<?> preferClass = byte[].class;
        /*
         * for our workaround for https://bugzilla.mozilla.org/show_bug.cgi?id=385421, it would be good if we have utf8 charset
         */
        final DataFlavor[] flavors = transferable.getTransferDataFlavors();
        for (final DataFlavor flav : flavors) {
            if (flav.getMimeType().contains("html") && flav.getRepresentationClass().isAssignableFrom(preferClass)) {
                /*
                 * we use first hit and search UTF-8
                 */
                if (htmlFlavor == null) {
                    htmlFlavor = flav;
                }
                final String charSet = new Regex(flav.toString(), "charset=(.*?)]").getMatch(0);
                if ("UTF-8".equalsIgnoreCase(charSet)) {
                    /* we found utf-8 encoding, so lets use that */
                    htmlFlavor = flav;
                    break;
                }
            }
        }
        final byte[] htmlDataBytes = getBytes(null, htmlFlavor, transferable);
        if (htmlDataBytes != null && htmlDataBytes.length != 0) {
            final String charSet = new Regex(htmlFlavor.toString(), "charset=(.*?)]").getMatch(0);
            final String result = convertBytes(htmlDataBytes, charSet, true);
            if (CrossSystem.isWindows()) {
                final String sourceURL = new Regex(result, "EndFragment:\\d+[\r\n]*SourceURL:(.*?)(\r|\n)").getMatch(0);
                final String fragment = new Regex(result, "<!--StartFragment-->(.*?)<!--EndFragment-->").getMatch(0);
                if (fragment != null) {
                    if (!StringUtils.isEmpty(sourceURL) && HTMLParser.getProtocol(sourceURL) != null) {
                        return new HTMLFragment(sourceURL, fragment);
                    }
                    final String browserURL = getCurrentBrowserURL(transferable, result);
                    return new HTMLFragment(browserURL, fragment);
                }
            }
            final String browserURL = getCurrentBrowserURL(transferable, result);
            return new HTMLFragment(browserURL, result);
        }
        return null;
    }

    private static String convertBytes(byte[] bytes, String charSet, boolean linuxWorkaround) throws UnsupportedEncodingException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        if (StringUtils.isEmpty(charSet)) {
            charSet = "UTF-8";
        }
        if (CrossSystem.isUnix()) {
            /*
             * workaround for firefox bug https://bugzilla .mozilla.org/show_bug .cgi?id=385421
             */
            /*
             * write check to skip broken first bytes and discard 0 bytes if they are in intervalls
             */
            int indexOriginal = 0;
            int i = 0;
            if (linuxWorkaround) {
                i = 6;
            }
            for (; i < bytes.length - 1; i++) {
                if (bytes[i] != 0) {
                    /* copy byte */
                    bytes[indexOriginal++] = bytes[i];
                }
            }
            return new String(bytes, 0, indexOriginal, charSet);
        } else {
            return new String(bytes, charSet);
        }
    }

    public static boolean hasSupportedTransferData(final Transferable transferable) {
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            /*
             * string and html always come together, so no need to check for html
             */
            return true;
        } else if (URLFLAVOR != null && transferable.isDataFlavorSupported(URLFLAVOR)) {
            return true;
        } else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return true;
        } else if (URILISTFLAVOR != null && transferable.isDataFlavorSupported(URILISTFLAVOR)) {
            return true;
        } else {
            return false;
        }
    }

    public static void processSupportedTransferData(final Transferable transferable, LinkOrigin origin) {
        try {
            final String listContent = getListTransferData(transferable);
            final String stringContent = getStringTransferData(transferable);
            final HTMLFragment htmlFragment;
            if (StringUtils.isNotEmpty(stringContent)) {
                htmlFragment = getHTMLFragment(transferable);
            } else {
                htmlFragment = null;
            }
            final String sourceURL;
            if (htmlFragment != null && StringUtils.isNotEmpty(htmlFragment.getSourceURL())) {
                sourceURL = htmlFragment.getSourceURL();
            } else {
                if (htmlFragment == null) {
                    sourceURL = getCurrentBrowserURL(transferable, null);
                } else {
                    sourceURL = getCurrentBrowserURL(transferable, htmlFragment.getFragment());
                }
            }
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotEmpty(listContent)) {
                sb.append("<");
                sb.append(listContent);
                sb.append(">\r\n\r\n");
            }
            if (StringUtils.isEmpty(listContent) && StringUtils.isNotEmpty(stringContent)) {
                sb.append("<");
                sb.append(stringContent);
                sb.append(">\r\n\r\n");
            }
            if (isHtmlFlavorAllowed() && htmlFragment != null && StringUtils.isNotEmpty(htmlFragment.getFragment())) {
                sb.append("<");
                sb.append(htmlFragment.getFragment());
                sb.append(">");
            }
            final String content = sb.toString();
            if (!StringUtils.isEmpty(content)) {
                final LinkCollectingJob job = new LinkCollectingJob(origin.getLinkOriginDetails(), content);
                job.setCustomSourceUrl(sourceURL);
                final HashSet<String> pws = PasswordUtils.getPasswords(content);
                if (pws != null && pws.size() > 0) {
                    job.setCrawledLinkModifierPrePackagizer(new CrawledLinkModifier() {

                        @Override
                        public void modifyCrawledLink(CrawledLink link) {
                            link.getArchiveInfo().getExtractionPasswords().addAll(pws);
                        }
                    });
                }
                LinkCollector.getInstance().addCrawlerJob(job);
            }
        } catch (final Throwable e) {
        }
    }

    public static String getStringTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
            final Object ret = transferable.getTransferData(DataFlavor.stringFlavor);
            if (ret == null) {
                return null;
            }
            return (String) ret;
        }
        return null;
    }

    public static String getURLTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (URLFLAVOR != null && transferable.isDataFlavorSupported(URLFLAVOR)) {
            Object ret = transferable.getTransferData(URLFLAVOR);
            if (ret == null) {
                return null;
            }
            final URL url = (URL) ret;
            if (StringUtils.isEmpty(url.getFile())) {
                return null;
            }
            return url.toExternalForm();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String getListTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException, URISyntaxException {
        final StringBuilder sb = new StringBuilder("");
        if (URILISTFLAVOR != null && transferable.isDataFlavorSupported(URILISTFLAVOR)) {
            /* url-lists are defined by rfc 2483 as crlf-delimited */
            final Object ret = transferable.getTransferData(URILISTFLAVOR);
            if (ret != null) {
                final StringTokenizer izer = new StringTokenizer((String) ret, "\r\n");
                while (izer.hasMoreTokens()) {
                    if (sb.length() > 0) {
                        sb.append("\r\n");
                    }
                    final String next = izer.nextToken();
                    if (StringUtils.isNotEmpty(next)) {
                        sb.append(next.replaceFirst("file:///", "file:/"));
                    }
                }
            }
            if (sb.length() > 0) {
                return sb.toString();
            }
        }
        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            final Object ret = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            if (ret != null) {
                final List<File> list = (List<File>) ret;
                for (final File f : list) {
                    if (!f.isAbsolute()) {
                        continue;
                    }
                    if (sb.length() > 0) {
                        sb.append("\r\n");
                    }
                    sb.append(f.toURI().toString());
                }
            }
        }
        if (sb.length() == 0) {
            return null;
        }
        return sb.toString();
    }

    /**
     * @return the iNSTANCE
     */
    public static ClipboardMonitoring getINSTANCE() {
        return INSTANCE;
    }

    private static byte[] getBytes(String mimeType, DataFlavor flavor, final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (mimeType == null && flavor == null) {
            return null;
        }
        final Class<?> preferClass = byte[].class;
        /*
         * for our workaround for https://bugzilla.mozilla.org/show_bug.cgi?id=385421, it would be good if we have utf8 charset
         */
        if (flavor == null) {
            for (final DataFlavor flav : transferable.getTransferDataFlavors()) {
                if (flav.getMimeType().contains(mimeType) && flav.getRepresentationClass().isAssignableFrom(preferClass)) {
                    flavor = flav;
                    break;
                }
            }
        }
        if (flavor != null) {
            byte[] htmlBytes = null;
            /* this can throw exception on some java versions when content is >256kb */
            if (flavor.getRepresentationClass().isAssignableFrom(byte[].class)) {
                htmlBytes = (byte[]) transferable.getTransferData(flavor);
            } else if (flavor.getRepresentationClass().isAssignableFrom(InputStream.class)) {
                InputStream is = null;
                try {
                    is = (InputStream) transferable.getTransferData(flavor);
                    htmlBytes = IO.readStream(-1, is);
                } finally {
                    try {
                        is.close();
                    } catch (final Throwable ignore) {
                    }
                }
            }
            return htmlBytes;
        }
        return null;
    }

    public static String getCurrentBrowserURL(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        return getCurrentBrowserURL(transferable, null);
    }

    public static String getCurrentBrowserURL(final Transferable transferable, final String htmlFlavor) throws UnsupportedFlavorException, IOException {
        if (ClipboardMonitoring.getINSTANCE().windowsClipboardHack != null) {
            final String ret = ClipboardMonitoring.getINSTANCE().windowsClipboardHack.getURLFromCF_HTML();
            if (!StringUtils.isEmpty(ret) && HTMLParser.getProtocol(ret) != null) {
                return ret;
            }
        }
        final String ret = getBrowserMime("x-moz-url-priv", transferable);
        if (!StringUtils.isEmpty(ret) && HTMLParser.getProtocol(ret) != null) {
            return ret;
        }
        if (htmlFlavor != null) {
            String viewSource = new Regex(htmlFlavor, "<a href=\"view-source:(https?://.*?)\"").getMatch(0);
            if (!StringUtils.isEmpty(viewSource) && HTMLParser.getProtocol(viewSource) != null) {
                return viewSource;
            }
            viewSource = new Regex(htmlFlavor, "EndFragment:\\d+[\r\n]*SourceURL:(.*?)(\r|\n)").getMatch(0);
            if (!StringUtils.isEmpty(viewSource) && HTMLParser.getProtocol(viewSource) != null) {
                return viewSource;
            }
        }
        return null;
    }

    public static String getBrowserMime(final String mime, final Transferable transferable) throws UnsupportedFlavorException, IOException {
        final byte[] xmozurlprivBytes = getBytes(mime, null, transferable);
        return convertBytes(xmozurlprivBytes, "UTF-8", false);
    }
}
