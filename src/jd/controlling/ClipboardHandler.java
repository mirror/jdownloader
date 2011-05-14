//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.controlling;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import jd.Main;
import jd.config.Configuration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.nutils.OSDetector;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;

/**
 * Diese Klasse ist dafür da, zeitverzögert die Zwischenablage zu untersuchen
 * 
 * @author astaldo/JD-Team
 */
public class ClipboardHandler extends Thread implements ControlListener {
    private static ClipboardHandler INSTANCE = null;

    public synchronized static ClipboardHandler getClipboard() {
        if (INSTANCE == null) {
            INSTANCE = new ClipboardHandler();
        }
        return INSTANCE;
    }

    private final Clipboard     clipboard;

    private boolean             enabled        = false;

    private boolean             waitFlag;
    private boolean             tempdisabled   = true;

    private DataFlavor          urlFlavor      = null;

    private DataFlavor          uriListFlavor  = null;
    private final DataFlavor    fileListFlavor = DataFlavor.javaFileListFlavor;
    private final DataFlavor    stringFlavor   = DataFlavor.stringFlavor;
    private DataFlavor          htmlFlavor     = null;

    private Transferable        cur            = null;
    private DataFlavor          lastDataFlavor = null;
    private String              lastString     = null;
    private String              currentString  = null;
    private final static byte[] tmpByteArray   = new byte[0];
    private final Object        LOCK           = new Object();

    /**
     */
    private ClipboardHandler() {
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        JDUtilities.getController().addControlListener(this);
        this.enabled = false;
        initDataFlavors();
        this.setName("ClipboardHandler");
        this.start();
    }

    private void initDataFlavors() {
        try {
            urlFlavor = new DataFlavor("application/x-java-url; class=java.net.URL");
        } catch (final Exception e) {
            JDLogger.exception(e);
            JDLogger.getLogger().info("urlFlavor not supported");
        }
        try {
            uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
        } catch (final Exception e) {
            JDLogger.exception(e);
            JDLogger.getLogger().info("uriListFlavor not supported");
        }
    }

    public void copyTextToClipboard(final String data) {
        /* new thread because we dont want to wait when currently locked */
        new Thread() {
            @Override
            public void run() {
                synchronized (LOCK) {
                    clipboard.setContents(new StringSelection(data), null);
                    lastDataFlavor = stringFlavor;
                    lastString = data;
                }
            }
        }.start();
    }

    /**
     * Gibt an ob die clipboard überwachung aktiv ist
     * 
     * @return true/false
     */
    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
    }

    public DataFlavor getBestFlavor(final Transferable trans) {
        if (trans.isDataFlavorSupported(fileListFlavor)) return fileListFlavor;
        if (urlFlavor != null && trans.isDataFlavorSupported(urlFlavor)) return urlFlavor;
        if (uriListFlavor != null && trans.isDataFlavorSupported(uriListFlavor)) return uriListFlavor;
        /* check for text/html DataFlavor */
        htmlFlavor = null;
        /*
         * for our workaround for
         * https://bugzilla.mozilla.org/show_bug.cgi?id=385421, it would be good
         * if we have utf8 charset
         */
        for (final DataFlavor flav : trans.getTransferDataFlavors()) {
            if (flav.getMimeType().contains("html") && flav.getRepresentationClass().isInstance(tmpByteArray)) {
                /*
                 * we use first hit and search UTF-8
                 */
                if (htmlFlavor != null) {
                    htmlFlavor = flav;
                }
                final String charSet = new Regex(flav.toString(), "charset=(.*?)]").getMatch(0);
                if (charSet != null && charSet.equalsIgnoreCase("UTF-8")) {
                    /* we found utf-8 encoding, so lets use that */
                    htmlFlavor = flav;
                    return htmlFlavor;
                }
            }
        }
        if (htmlFlavor != null) return htmlFlavor;
        if (trans.isDataFlavorSupported(stringFlavor)) return stringFlavor;
        return null;
    }

    @Override
    public void run() {
        while (true) {
            synchronized (this) {
                waitFlag = true;
                while (waitFlag) {
                    try {
                        wait();
                    } catch (final Exception e) {
                        return;
                    }
                }
            }
            while (enabled && !this.tempdisabled) {
                try {
                    /* get current content of clipboard */
                    synchronized (LOCK) {
                        final DataFlavor what = getCurrentClipboardContent();
                        if ((lastDataFlavor == null || lastString == null || lastDataFlavor != what) && currentString != null && what != null) {
                            /* DataFlavor changed so lets parse it */
                            lastDataFlavor = what;
                            if (what == uriListFlavor || what == fileListFlavor) {
                                // url-lists are defined by rfc 2483 as
                                // crlf-delimited
                                final StringTokenizer izer = new StringTokenizer(currentString, "\r\n");
                                final StringBuilder sb = new StringBuilder("");
                                while (izer.hasMoreTokens()) {
                                    /* linux adds file:// */
                                    final URI fi = new URI(izer.nextToken());
                                    // is it better than File f = new File(fi)?
                                    final File f = new File(fi.getPath());
                                    if (f.exists()) {
                                        if (DistributeData.hasContainerPluginFor(fi.getPath())) {
                                            JDController.loadContainerFile(f);
                                        }
                                    } else if (what == uriListFlavor) {
                                        sb.append(fi.getPath());
                                        sb.append("\r\n");
                                    }
                                }
                                /* check if we have unhandled uri */
                                if (sb.length() > 0) {
                                    final String parse = sb.toString();
                                    if (!CNL2.checkText(parse)) {
                                        new DistributeData(parse).start();
                                    }
                                }
                            } else if (what == urlFlavor || what == stringFlavor) {
                                /* parse plaintext content */
                                if (!CNL2.checkText(currentString)) {
                                    System.out.println("parse");
                                    new DistributeData(currentString).start();
                                }
                            } else if (what == htmlFlavor) {
                                /* parse html content, get all links */
                                if (!CNL2.checkText(currentString)) {
                                    new DistributeData(currentString).start();
                                }
                            }
                            lastString = currentString;
                        } else {
                            /*
                             * DataFlavor did not change, so we must check for
                             * changes
                             */
                            if (lastString != null && currentString != null && lastString.equalsIgnoreCase(currentString)) {
                                /* no changes */
                            } else {
                                /*
                                 * reset lastString so we parse clipboard again
                                 */
                                lastString = null;
                            }
                        }
                    }
                } catch (final Exception e2) {
                    // JDLogger.exception(e2);
                }
                try {
                    Thread.sleep(750);
                } catch (final InterruptedException e) {
                }
            }
        }
    }

    public String getCurrentClipboardLinks() {
        synchronized (LOCK) {
            final DataFlavor what = getCurrentClipboardContent();
            if (what == urlFlavor || what == stringFlavor || what == htmlFlavor) {
                return currentString;
            } else
                return "";
        }
    }

    @SuppressWarnings("unchecked")
    public DataFlavor getCurrentClipboardContent() {
        synchronized (LOCK) {
            DataFlavor what = null;
            try {
                cur = clipboard.getContents(null);
                if (cur != null) {
                    what = getBestFlavor(cur);
                    if (what != null) {
                        currentString = null;
                        if (what == fileListFlavor) {
                            /*
                             * fileListFlavors occur in Windows, we use only
                             * those files we have Plugins for
                             */
                            final List<File> list = (List<File>) cur.getTransferData(fileListFlavor);
                            // final ListIterator<File> it =
                            // list.listIterator();
                            // final StringBuilder sb = new StringBuilder("");
                            // while (it.hasNext()) {
                            // final File f = it.next();
                            // if
                            // (DistributeData.hasContainerPluginFor(f.toString()))
                            // {
                            // sb.append(f.toString());
                            // sb.append("\r\n");
                            // }
                            // }
                            final StringBuilder sb = new StringBuilder("");
                            for (final File f : list) {
                                if (DistributeData.hasContainerPluginFor(f.toString())) {
                                    sb.append(f.toString());
                                    sb.append("\r\n");
                                }
                            }
                            currentString = sb.toString();
                        } else if (what == uriListFlavor) {
                            currentString = (String) cur.getTransferData(uriListFlavor);
                        } else if (what == urlFlavor) {
                            currentString = ((URL) cur.getTransferData(urlFlavor)).toString();
                        } else if (what == stringFlavor) {
                            currentString = ((String) cur.getTransferData(stringFlavor));
                        } else if (what == htmlFlavor) {
                            try {
                                final String charSet = new Regex(htmlFlavor.toString(), "charset=(.*?)]").getMatch(0);
                                byte[] html = (byte[]) cur.getTransferData(htmlFlavor);
                                if (OSDetector.isLinux()) {
                                    /*
                                     * workaround for firefox bug
                                     * https://bugzilla .mozilla.org/show_bug
                                     * .cgi?id=385421
                                     */
                                    /* FIXME: not finished yet */
                                    /*
                                     * write check to skip broken first bytes
                                     * and discard 0 bytes if they are in
                                     * intervalls
                                     */
                                    final int htmlLength = html.length;
                                    final byte[] html2 = new byte[htmlLength];

                                    int o = 0;
                                    for (int i = 6; i < htmlLength - 1; i++) {
                                        // System.out.print(html[i] +
                                        // " ");
                                        if (html[i] != 0) {
                                            html2[o++] = html[i];
                                        }
                                    }
                                    html = html2;
                                    currentString = new String(html, "UTF-8");
                                    // System.out.println(currentString);
                                } else {
                                    /* no workaround needed */
                                    if (charSet != null) {
                                        currentString = new String(html, charSet);
                                    } else {
                                        currentString = new String(html);
                                    }
                                }
                            } catch (final Exception e) {
                                // JDLogger.exception(e);
                                /* fallback */
                                if (cur.isDataFlavorSupported(stringFlavor)) {
                                    what = stringFlavor;
                                    currentString = ((String) cur.getTransferData(stringFlavor));
                                }
                            }
                        }
                    }
                }
            } catch (final Exception e) {
                // JDLogger.exception(e);
            }
            return what;
        }
    }

    public void setTempDisabled(final boolean v) {
        this.tempdisabled = v;
        synchronized (this) {
            if (waitFlag) {
                waitFlag = false;
                notify();
            }
        }
    }

    /**
     * Schaltet die clipboardüberwachung an/aus
     * 
     * @param enabled
     */
    public boolean setEnabled(final boolean enabled2) {
        if (enabled == enabled2) return false;
        if (enabled2) {
            JDLogger.getLogger().info("ClipBoard Observation enabled");
        } else {
            JDLogger.getLogger().info("ClipBoard Observation disabled");
        }
        enabled = enabled2;
        final Configuration configuration = JDUtilities.getConfiguration();
        configuration.setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, enabled2);
        configuration.save();
        synchronized (this) {
            if (waitFlag) {
                waitFlag = false;
                notify();
            }
        }
        return true;
    }

    public void toggleActivation() {
        setEnabled(!isEnabled());
    }

    public void controlEvent(final ControlEvent event) {
        if (event.getEventID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getCaller() instanceof Main) {
            setEnabled(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true));
            JDUtilities.getController().removeControlListener(this);
        }
    }

}
