package jd.controlling;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.List;

import org.appwork.utils.Regex;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;

public class ClipboardMonitoring {

    private static DataFlavor urlFlavor        = null;
    private static DataFlavor uriListFlavor    = null;
    private volatile Thread   monitoringThread = null;
    private Clipboard         clipboard        = null;

    public synchronized void startMonitoring() {
        monitoringThread = new Thread() {
            private String oldStringContent = null;

            @Override
            public void run() {
                while (Thread.currentThread() == monitoringThread) {
                    try {
                        synchronized (this) {
                            this.wait(750);
                        }
                    } catch (InterruptedException e) {
                        return;
                    }
                    try {
                        Transferable currentContent = clipboard.getContents(null);
                        if (currentContent == null) continue;
                        String newStringContent = getStringTransferData(currentContent);
                        if (changeDetector(oldStringContent, newStringContent)) {
                            String htmlContent = getHTMLTransferData(currentContent);
                            if (htmlContent != null) {
                                System.out.println("NEW(STRING+HTML):" + newStringContent + htmlContent);
                            }
                            System.out.println("NEW(STRING ONLY):" + newStringContent);
                        } else {
                            System.out.println("UNCHANGED");
                        }
                        oldStringContent = newStringContent;
                    } catch (final Throwable e) {
                    }
                }
            }

        };
        monitoringThread.setName("ClipboardMonitor");
        monitoringThread.setDaemon(true);
        monitoringThread.start();
    }

    private boolean changeDetector(String oldS, String newS) {
        if (oldS == null && newS != null) return true;
        if (oldS != null && newS != null && !oldS.equalsIgnoreCase(newS)) return true;
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
    }

    public static void main(String[] args) throws InterruptedException {
        ClipboardMonitoring cm = new ClipboardMonitoring();
        cm.startMonitoring();
        Thread.sleep(90000);
    }

    static {
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (final Throwable e) {
            Log.L.info("urlFlavor not supported");
        }
        try {
            uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
        } catch (final Throwable e) {
            Log.L.info("uriListFlavor not supported");
        }
    }

    public static String getHTMLTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        DataFlavor htmlFlavor = null;
        /*
         * for our workaround for
         * https://bugzilla.mozilla.org/show_bug.cgi?id=385421, it would be good
         * if we have utf8 charset
         */
        for (final DataFlavor flav : transferable.getTransferDataFlavors()) {
            if (flav.getMimeType().contains("html") && flav.getRepresentationClass().isAssignableFrom(byte[].class)) {
                /*
                 * we use first hit and search UTF-8
                 */
                if (htmlFlavor != null) {
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
        if (htmlFlavor != null) {
            /* we found a flavor for html content, lets fetch its content */
            final String charSet = new Regex(htmlFlavor.toString(), "charset=(.*?)]").getMatch(0);
            byte[] htmlBytes = (byte[]) transferable.getTransferData(htmlFlavor);
            if (CrossSystem.isLinux()) {
                /*
                 * workaround for firefox bug https://bugzilla
                 * .mozilla.org/show_bug .cgi?id=385421
                 */
                /*
                 * write check to skip broken first bytes and discard 0 bytes if
                 * they are in intervalls
                 */
                byte[] htmlBytes2 = new byte[htmlBytes.length];
                int indexOriginal = 0;
                int maxLength = (htmlBytes.length - 1);
                for (int i = 6; i < maxLength; i++) {
                    if (htmlBytes[i] != 0) {
                        /* copy byte */
                        htmlBytes2[indexOriginal++] = htmlBytes[i];
                    } else if (htmlBytes[i] == 0) {
                        /* byte is zero, lets workaround the bug */
                        if (i + 2 < maxLength && htmlBytes[i + 2] != 0) {
                            htmlBytes2[indexOriginal++] = htmlBytes[i];
                            for (int ii = i + 1; ii < maxLength; ii++) {
                                i++;
                                if (htmlBytes[ii] != 0) {
                                    htmlBytes2[indexOriginal++] = htmlBytes[ii];
                                } else {
                                    break;
                                }
                            }
                        }
                    }
                }
                String ret = null;
                if (charSet != null) {
                    ret = new String(htmlBytes2, 0, indexOriginal, charSet);
                } else {
                    ret = new String(htmlBytes2, 0, indexOriginal, "UTF-8");
                }
                return ret;
            } else {
                /* no workaround needed */
                if (charSet != null) {
                    return new String(htmlBytes, charSet);
                } else {
                    return new String(htmlBytes);
                }
            }
        }
        return null;
    }

    public static String getStringTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) { return (String) transferable.getTransferData(DataFlavor.stringFlavor); }
        return null;
    }

    public static String getURLTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (urlFlavor != null && transferable.isDataFlavorSupported(urlFlavor)) { return ((URL) transferable.getTransferData(urlFlavor)).toString(); }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static String getListTransferData(final Transferable transferable) throws UnsupportedFlavorException, IOException {
        if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
            final List<File> list = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
            final StringBuilder sb = new StringBuilder("");
            for (final File f : list) {
                if (sb.length() > 0) sb.append("\r\n");
                sb.append("file://" + f.getPath());
            }
            return sb.toString();
        } else if (uriListFlavor != null && transferable.isDataFlavorSupported(uriListFlavor)) { return (String) transferable.getTransferData(uriListFlavor); }
        return null;
    }
}
