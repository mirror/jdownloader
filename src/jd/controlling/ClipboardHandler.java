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
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.StringTokenizer;

import jd.Main;
import jd.config.Configuration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.nutils.OSDetector;
import jd.parser.Regex;
import jd.utils.JDUtilities;

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

    private Clipboard clipboard;

    private boolean enabled = false;

    private String oldText = null;

    private List<File> oldFiles = null;

    private boolean waitFlag;
    private boolean tempdisabled = true;

    private DataFlavor urlFlavor = null;

    private DataFlavor uriListFlavor = null;
    private DataFlavor stringFlavor = DataFlavor.stringFlavor;
    private DataFlavor htmlFlavor = null;
    private final static byte[] tmpByteArray = new byte[0];

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
        } catch (Exception e) {
            JDLogger.exception(e);
            JDLogger.getLogger().info("urlFlavor not supported");
        }
        try {
            uriListFlavor = new DataFlavor("text/uri-list; class=java.lang.String");
        } catch (Exception e) {
            JDLogger.exception(e);
            JDLogger.getLogger().info("uriListFlavor not supported");
        }
    }

    public void setOldText(String data) {
        oldText = data;
    }

    /**
     * Gibt an ob die clipboard überwachung aktiv ist
     * 
     * @return true/false
     */
    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
    }

    public DataFlavor getBestFlavor(Transferable trans) {
        if (urlFlavor != null && trans.isDataFlavorSupported(urlFlavor)) return urlFlavor;
        if (uriListFlavor != null && trans.isDataFlavorSupported(uriListFlavor)) return uriListFlavor;
        /* check for text/html DataFlavor */
        htmlFlavor = null;
        if (OSDetector.isLinux()) {
            /*
             * for our workaround for
             * https://bugzilla.mozilla.org/show_bug.cgi?id=385421, we need utf8
             * charset
             */
            for (DataFlavor flav : trans.getTransferDataFlavors()) {
                if (flav.getMimeType().contains("html") && flav.getRepresentationClass().isInstance(tmpByteArray)) {
                    String charSet = new Regex(flav.toString(), "charset=(.*?)]").getMatch(0);
                    if (charSet != null && charSet.equalsIgnoreCase("UTF-8")) {
                        htmlFlavor = flav;
                        return htmlFlavor;
                    }
                }
            }
        } else {
            for (DataFlavor flav : trans.getTransferDataFlavors()) {
                if (flav.getMimeType().contains("html") && flav.getRepresentationClass().isInstance(tmpByteArray)) {
                    htmlFlavor = flav;
                    return htmlFlavor;
                }
            }
        }
        if (trans.isDataFlavorSupported(stringFlavor)) return stringFlavor;
        return null;
    }

    @SuppressWarnings("unchecked")
    public void run() {
        Transferable cur = null;
        DataFlavor lastDataFlavor = null;
        String lastString = null;
        while (true) {
            synchronized (this) {
                waitFlag = true;
                while (waitFlag) {
                    try {
                        wait();
                    } catch (Exception e) {
                        return;
                    }
                }
            }
            while (enabled && !this.tempdisabled) {
                try {
                    /* get current content of clipboard */
                    cur = clipboard.getContents(null);
                    if (cur != null) {
                        DataFlavor what = getBestFlavor(cur);
                        if (what != null) {
                            lastString = null;
                            if (what == uriListFlavor) {
                                // url-lists are defined by rfc 2483 as
                                // crlf-delimited
                                lastString = (String) cur.getTransferData(uriListFlavor);
                            } else if (what == urlFlavor) {
                                lastString = ((URL) cur.getTransferData(urlFlavor)).toString();
                            } else if (what == stringFlavor) {
                                lastString = ((String) cur.getTransferData(stringFlavor));
                            } else if (what == htmlFlavor) {
                                try {
                                    String charSet = new Regex(htmlFlavor.toString(), "charset=(.*?)]").getMatch(0);
                                    System.out.println("charset " + charSet);
                                    byte[] html = (byte[]) cur.getTransferData(htmlFlavor);
                                    if (OSDetector.isLinux()) {
                                        /*
                                         * workaround for firefox bug
                                         * https://bugzilla
                                         * .mozilla.org/show_bug.cgi?id=385421
                                         */
                                        byte[] html2 = new byte[html.length];

                                        int o = 0;
                                        for (int i = 6; i < html.length - 1; i++) {
                                            System.out.print(html[i] + " ");
                                            if (i % 2 == 0 || true) html2[o++] = html[i];
                                        }
                                        html = html2;
                                        lastString = new String(html, "UTF-8");
                                    } else {
                                        if (charSet != null) {
                                            lastString = new String(html, charSet);
                                        } else {
                                            lastString = new String(html);
                                        }
                                    }
                                    System.out.println(lastString);
                                    byte[] oo = new String("hü").getBytes("UTF-8");
                                    for (byte z : oo) {
                                        System.out.print(z + " ");
                                    }
                                    System.out.println("");
                                } catch (Exception e) {
                                    JDLogger.exception(e);
                                    /* fallback */
                                    if (cur.isDataFlavorSupported(stringFlavor)) lastString = ((String) cur.getTransferData(stringFlavor));
                                }
                            }

                            if (lastDataFlavor == null || lastDataFlavor != what) {
                                /* DataFlavor changed so lets parse it */
                                lastDataFlavor = what;
                                if (what == uriListFlavor) {
                                    StringTokenizer izer = new StringTokenizer(lastString, "\r\n");
                                    while (izer.hasMoreTokens()) {
                                        String uri = izer.nextToken().replaceFirst("file://", "");
                                        if (new File(uri).exists()) {
                                            if (DistributeData.hasContainerPluginFor(uri)) JDController.getInstance().loadContainerFile(new File(uri));
                                        }
                                    }
                                } else if (what == urlFlavor || what == stringFlavor) {
                                    if (lastString != null) {
                                        if (!CNL2.checkText(lastString)) new DistributeData(lastString).start();
                                    }
                                }
                            } else {
                                /*
                                 * DataFlavor did not change, so we must check
                                 * for changes
                                 */
                            }
                        }
                    }
                } catch (Exception e2) {
                    JDLogger.exception(e2);
                }
                try {
                    Thread.sleep(750);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void setTempDisabled(boolean v) {
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
    public boolean setEnabled(boolean enabled2) {
        if (enabled == enabled2) return false;
        if (enabled2) {
            JDLogger.getLogger().info("ClipBoard Observation enabled");
        } else {
            JDLogger.getLogger().info("ClipBoard Observation disabled");
        }
        enabled = enabled2;
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, enabled2);
        JDUtilities.getConfiguration().save();
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

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            setEnabled(JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true));
            JDUtilities.getController().removeControlListener(this);
        }

    }

}
