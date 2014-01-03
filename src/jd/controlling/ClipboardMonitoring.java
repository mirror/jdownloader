package jd.controlling;

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
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcollector.LinkOrigin;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledLinkModifier;

import org.appwork.utils.IO;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.jdownloader.controlling.PasswordUtils;
import org.jdownloader.gui.views.components.packagetable.dragdrop.PackageControllerTableTransferable;
import org.jdownloader.logging.LogController;

import sun.awt.datatransfer.SunClipboard;

public class ClipboardMonitoring {

    private static class WindowsClipboardHack {
        Method    openClipboard    = null;
        Method    closeClipboard   = null;
        Method    getClipboardData = null;
        long      cf_html          = -1;
        Clipboard clipboard        = null;

        private WindowsClipboardHack(Clipboard clipboard) throws Exception {
            try {
                this.clipboard = clipboard;
                cf_html = (Long) Class.forName("sun.awt.windows.WDataTransferer").getDeclaredField("CF_HTML").get(null);
                openClipboard = clipboard.getClass().getDeclaredMethod("openClipboard", new Class[] { SunClipboard.class });
                openClipboard.setAccessible(true);
                closeClipboard = clipboard.getClass().getDeclaredMethod("closeClipboard", new Class[] {});
                closeClipboard.setAccessible(true);
                getClipboardData = clipboard.getClass().getDeclaredMethod("getClipboardData", new Class[] { long.class });
                getClipboardData.setAccessible(true);
            } catch (final Throwable e) {
                throw new Exception(e);
            }
        }

        private String getURLFromCF_HTML() {
            try {
                try {
                    openClipboard.invoke(clipboard, new Object[] { null });
                    String sstr = new String((byte[]) getClipboardData.invoke(clipboard, new Object[] { cf_html }), "UTF-8");
                    return new Regex(sstr, "SourceURL:([^\r\n]*)").getMatch(0);
                } finally {
                    closeClipboard.invoke(clipboard, new Object[] {});
                }
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
            return null;
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

    private static ClipboardMonitoring  INSTANCE            = new ClipboardMonitoring();
    private static DataFlavor           urlFlavor           = null;
    private static DataFlavor           uriListFlavor       = null;
    private volatile Thread             monitoringThread    = null;
    private Clipboard                   clipboard           = null;
    private volatile boolean            skipChangeDetection = false;
    private static WindowsClipboardHack clipboardHack       = null;
    private static boolean              firstRoundDone      = true;

    public void setFirstRoundDone(boolean b) {
        firstRoundDone = b;
    }

    private boolean ignoreTransferable(Transferable transferable) {
        if (transferable == null) return true;
        try {
            if (transferable.getClass().getName().contains("TransferableProxy")) {
                Field isLocal = transferable.getClass().getDeclaredField("isLocal");
                if (isLocal != null) {
                    isLocal.setAccessible(true);
                    if (Boolean.TRUE.equals(isLocal.getBoolean(transferable))) return true;
                }
            }
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        return false;
    }

    public synchronized void startMonitoring() {
        if (isMonitoring()) return;
        monitoringThread = new Thread() {
            private String oldStringContent = null;
            private String oldHTMLContent   = null;
            private String oldListContent   = null;
            private String lastBrowserUrl   = null;

            @Override
            public void run() {
                final int minWaitTimeout = 200;
                final int maxWaitTimeout = 1000;
                int waitTimeout = minWaitTimeout;
                while (Thread.currentThread() == monitoringThread) {
                    try {
                        synchronized (this) {
                            this.wait(waitTimeout);
                        }
                        if (Thread.currentThread() != monitoringThread) return;
                    } catch (InterruptedException e) {
                        LogController.CL().finer("Interrupted ClipBoard Monitoring Thread");
                        return;
                    }
                    waitTimeout = Math.min(waitTimeout + 200, maxWaitTimeout);
                    if (skipChangeDetection) {
                        waitTimeout = maxWaitTimeout;
                        continue;
                    }
                    try {
                        lastBrowserUrl = null;
                        Transferable currentContent = clipboard.getContents(null);
                        if (ignoreTransferable(currentContent)) continue;
                        if (currentContent.isDataFlavorSupported(PackageControllerTableTransferable.FLAVOR)) {
                            /* we have Package/Children in clipboard, skip them */
                            continue;
                        }
                        String handleThisRound = null;
                        try {
                            /* change detection for List/URI content */
                            String newListContent = getListTransferData(currentContent);
                            try {
                                if (changeDetector(oldListContent, newListContent)) {
                                    handleThisRound = newListContent;
                                } else if (noChangeDetector(oldListContent, newListContent)) {
                                    continue;
                                }
                            } finally {
                                if (!StringUtils.isEmpty(newListContent)) {
                                    oldListContent = newListContent;
                                } else {
                                    oldListContent = null;
                                }
                            }
                        } catch (final Throwable e) {
                        }
                        if (StringUtils.isEmpty(handleThisRound)) {
                            /* change detection for String/HTML content */
                            String newStringContent = getStringTransferData(currentContent);
                            try {
                                if (changeDetector(oldStringContent, newStringContent)) {
                                    /*
                                     * we only use normal String Content to detect a change
                                     */
                                    handleThisRound = newStringContent;
                                    try {
                                        /*
                                         * lets fetch fresh HTML Content if available
                                         */
                                        String htmlContent = getHTMLTransferData(currentContent);
                                        if (htmlContent != null) {
                                            /*
                                             * remember that we had HTML content this round
                                             */
                                            oldHTMLContent = htmlContent;
                                            handleThisRound = handleThisRound + "\r\n" + htmlContent;
                                            lastBrowserUrl = getCurrentBrowserURL(currentContent);
                                        } else {
                                            oldHTMLContent = null;
                                        }
                                    } catch (final Throwable e) {
                                    }
                                } else if (oldHTMLContent != null) {
                                    /*
                                     * no String Content change detected, let's verify if the HTML content hasn't changed
                                     */
                                    try {
                                        /*
                                         * lets fetch fresh HTML Content if available
                                         */
                                        String htmlContent = getHTMLTransferData(currentContent);
                                        if (htmlContent != null) {
                                            /*
                                             * remember that we had HTML content this round
                                             */
                                            if (changeDetector(oldHTMLContent, htmlContent)) {
                                                oldHTMLContent = htmlContent;
                                                handleThisRound = newStringContent + "\r\n" + htmlContent;
                                                lastBrowserUrl = getCurrentBrowserURL(currentContent);
                                            }
                                        } else {
                                            oldHTMLContent = null;
                                        }
                                    } catch (final Throwable e) {
                                    }
                                }
                            } finally {
                                oldStringContent = newStringContent;
                            }
                        }
                        if (!StringUtils.isEmpty(handleThisRound)) {
                            if (firstRoundDone) {
                                waitTimeout = minWaitTimeout;
                                LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.CLIPBOARD, handleThisRound);
                                final HashSet<String> pws = PasswordUtils.getPasswords(handleThisRound);
                                if (pws != null && pws.size() > 0) {
                                    job.setCrawledLinkModifier(new CrawledLinkModifier() {

                                        @Override
                                        public void modifyCrawledLink(CrawledLink link) {
                                            link.getArchiveInfo().getExtractionPasswords().addAll(pws);
                                        }
                                    });
                                }
                                job.setCustomSourceUrl(lastBrowserUrl);
                                LinkCollector.getInstance().addCrawlerJob(job);
                            } else {
                                firstRoundDone = true;
                            }
                        }
                    } catch (final Throwable e) {
                        Log.exception(e);
                    }
                }
            }

        };
        monitoringThread.setName("ClipboardMonitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    public synchronized ClipboardContent getCurrentContent() {
        Transferable currentContent = null;
        try {
            currentContent = clipboard.getContents(null);
        } catch (final Throwable e) {
            return null;
        }
        if (currentContent == null) return null;
        String stringContent = null;
        try {
            stringContent = getStringTransferData(currentContent);
        } catch (final Throwable e) {
        }
        String htmlContent = null;
        String browserUrl = null;
        try {
            /* lets fetch fresh HTML Content if available */
            htmlContent = getHTMLTransferData(currentContent);
            if (htmlContent != null) browserUrl = getCurrentBrowserURL(currentContent);
        } catch (final Throwable e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        if (stringContent != null) sb.append(stringContent);
        if (sb.length() > 0) sb.append("\r\n");
        if (htmlContent != null) sb.append(htmlContent);
        return new ClipboardContent(sb.toString(), browserUrl);
    }

    public synchronized void setCurrentContent(String string) {
        try {
            clipboard.setContents(new StringSelection(string), new ClipboardOwner() {

                public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    skipChangeDetection = false;
                }
            });
            skipChangeDetection = true;
        } catch (final Throwable e) {
            skipChangeDetection = false;
        }
    }

    public synchronized void setCurrentContent(Transferable object) {
        try {
            clipboard.setContents(object, new ClipboardOwner() {

                public void lostOwnership(Clipboard clipboard, Transferable contents) {
                    skipChangeDetection = false;
                }
            });
            skipChangeDetection = true;
        } catch (final Throwable e) {
            skipChangeDetection = false;
        }
    }

    private boolean changeDetector(String oldS, String newS) {
        if (oldS == null && newS != null) return true;
        if (oldS != null && newS != null && !oldS.equalsIgnoreCase(newS)) return true;
        return false;
    }

    private boolean noChangeDetector(String oldS, String newS) {
        if (oldS != null && newS != null && oldS.equalsIgnoreCase(newS)) return true;
        return false;
    }

    public synchronized void stopMonitoring() {
        if (monitoringThread != null) monitoringThread.interrupt();
        monitoringThread = null;
    }

    public synchronized boolean isMonitoring() {
        return monitoringThread != null && monitoringThread.isAlive();
    }

    public ClipboardMonitoring() {
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        if (CrossSystem.isWindows()) {
            try {
                clipboardHack = new WindowsClipboardHack(clipboard);
            } catch (final Throwable e) {
                LogController.CL().log(e);
            }
        }
    }

    static {
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (final Throwable e) {
            LogController.CL().info("urlFlavor not supported");
        }
        try {
            uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
        } catch (final Throwable e) {
            LogController.CL().info("uriListFlavor not supported");
        }
    }

    public static String getHTMLTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        DataFlavor htmlFlavor = null;
        final Class<?> preferClass = byte[].class;
        /*
         * for our workaround for https://bugzilla.mozilla.org/show_bug.cgi?id=385421, it would be good if we have utf8 charset
         */
        for (final DataFlavor flav : transferable.getTransferDataFlavors()) {
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
        byte[] htmlDataBytes = getBytes(null, htmlFlavor, transferable);
        if (htmlDataBytes != null && htmlDataBytes.length != 0) {
            String charSet = new Regex(htmlFlavor.toString(), "charset=(.*?)]").getMatch(0);
            return convertBytes(htmlDataBytes, charSet, true);
        }
        return null;
    }

    private static String convertBytes(byte[] bytes, String charSet, boolean linuxWorkaround) throws UnsupportedEncodingException {
        if (bytes == null || bytes.length == 0) return null;
        if (StringUtils.isEmpty(charSet)) charSet = "UTF-8";
        if (CrossSystem.isLinux()) {
            /*
             * workaround for firefox bug https://bugzilla .mozilla.org/show_bug .cgi?id=385421
             */
            /*
             * write check to skip broken first bytes and discard 0 bytes if they are in intervalls
             */
            int indexOriginal = 0;
            int i = 0;
            if (linuxWorkaround) i = 6;
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
        } else if (urlFlavor != null && transferable.isDataFlavorSupported(urlFlavor)) {
            return true;
        } else if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            return true;
        } else if (uriListFlavor != null && transferable.isDataFlavorSupported(uriListFlavor)) {
            return true;
        } else {
            return false;
        }
    }

    public static void processSupportedTransferData(final Transferable transferable) {
        try {
            String browserUrl = null;
            String htmlContent = null;
            String listContent = getListTransferData(transferable);
            String stringContent = getStringTransferData(transferable);
            if (StringUtils.isNotEmpty(stringContent)) htmlContent = getHTMLTransferData(transferable);
            if (StringUtils.isNotEmpty(htmlContent)) browserUrl = getCurrentBrowserURL(transferable);
            StringBuilder sb = new StringBuilder();
            if (StringUtils.isNotEmpty(listContent)) {
                sb.append("<");
                sb.append(listContent);
                sb.append(">\r\n\r\n");
            }
            if (StringUtils.isNotEmpty(stringContent)) {
                sb.append("<");
                sb.append(stringContent);
                sb.append(">\r\n\r\n");
            }
            if (StringUtils.isNotEmpty(htmlContent)) {
                sb.append("<");
                sb.append(htmlContent);
                sb.append(">");
            }
            String content = sb.toString();
            if (!StringUtils.isEmpty(content)) {
                LinkCollectingJob job = new LinkCollectingJob(LinkOrigin.CLIPBOARD, content);
                job.setCustomSourceUrl(browserUrl);
                final HashSet<String> pws = PasswordUtils.getPasswords(content);
                if (pws != null && pws.size() > 0) {
                    job.setCrawledLinkModifier(new CrawledLinkModifier() {

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
            Object ret = transferable.getTransferData(DataFlavor.stringFlavor);
            if (ret == null) return null;
            return (String) ret;
        }
        return null;
    }

    public static String getURLTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (urlFlavor != null && transferable.isDataFlavorSupported(urlFlavor)) {
            Object ret = transferable.getTransferData(urlFlavor);
            if (ret == null) return null;
            URL url = (URL) ret;
            if (StringUtils.isEmpty(url.getFile())) return null;
            return url.toExternalForm();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String getListTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException, URISyntaxException {
        final StringBuilder sb = new StringBuilder("");
        if (CrossSystem.isMac() == false && transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            /* TODO: temporarily disabled on mac because of reports of syslog flushing for clipboard url stuff */
            Object ret = transferable.getTransferData(DataFlavor.javaFileListFlavor);
            if (ret != null) {
                final List<File> list = (List<File>) ret;
                for (final File f : list) {
                    if (!f.isAbsolute()) continue;
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append("file://" + f.getPath());
                }
            }
        }
        if (uriListFlavor != null && transferable.isDataFlavorSupported(uriListFlavor)) {
            /* url-lists are defined by rfc 2483 as crlf-delimited */
            Object ret = transferable.getTransferData(uriListFlavor);
            if (ret != null) {
                final StringTokenizer izer = new StringTokenizer((String) ret, "\r\n");
                while (izer.hasMoreTokens()) {
                    if (sb.length() > 0) sb.append("\r\n");
                    String next = izer.nextToken();
                    if (StringUtils.isNotEmpty(next)) sb.append(next);
                }
            }
        }
        if (sb.length() == 0) return null;
        return sb.toString();
    }

    /**
     * @return the iNSTANCE
     */
    public static ClipboardMonitoring getINSTANCE() {
        return INSTANCE;
    }

    private static byte[] getBytes(String mimeType, DataFlavor flavor, final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (mimeType == null && flavor == null) return null;
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
        String ret = null;
        if (clipboardHack != null) {
            ret = clipboardHack.getURLFromCF_HTML();
        }
        if (!StringUtils.isEmpty(ret)) return ret;
        byte[] xmozurlprivBytes = getBytes("x-moz-url-priv", null, transferable);
        return convertBytes(xmozurlprivBytes, "UTF-8", false);
    }
}
