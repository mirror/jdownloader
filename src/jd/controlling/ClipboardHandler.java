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
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import jd.Main;
import jd.config.Configuration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
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
    private boolean clipboardchanged = true;
    private int wait = 500;

    private ClipboardOwner clipboardwatch = new ClipboardOwner() {
        public void lostOwnership(Clipboard clipboard, Transferable contents) {
            /* notifies us when clipboard content changed */
            clipboardchanged = true;
        }
    };

    /**
     */
    private ClipboardHandler() {
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        JDUtilities.getController().addControlListener(this);
        this.enabled = false;
        this.setName("ClipboardHandler");
        /* set clipboardchanged to true, so we check it at least once */
        clipboardchanged = true;
        this.start();
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

    @SuppressWarnings("unchecked")
    public void run() {
        Transferable cur = null;
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
                while (clipboardchanged && enabled && !this.tempdisabled) {
                    try {
                        /* get current content of clipboard */
                        cur = clipboard.getContents(null);
                        if (cur != null) {
                            try {
                                if (cur.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                    /*
                                     * we have text in clipboard, also can be
                                     * files in linux
                                     */
                                    String text = (String) cur.getTransferData(DataFlavor.stringFlavor);
                                    if (!text.equals(oldText)) {
                                        /* content changed */
                                        oldText = text;
                                        String files[] = Regex.getLines(text);
                                        boolean isTextContent = true;
                                        if (files.length > 0) {
                                            /*
                                             * workaround for files under linux
                                             */
                                            if (new File(files[0].trim()).exists()) {
                                                isTextContent = false;
                                                for (String file : files) {
                                                    JDUtilities.getController().loadContainerFile(new File(file.trim()));
                                                }
                                            }
                                        }
                                        /* parsing clipboard for Links */
                                        if (isTextContent) new DistributeData(text.trim()).start();
                                    }
                                    /*
                                     * now let the clipboardwatcher check for
                                     * changes
                                     */
                                    clipboardchanged = false;
                                    clipboard.setContents(cur, clipboardwatch);
                                    wait = 500;
                                } else if (cur.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                                    /* we have files in cliploard */
                                    List<File> files = (List<File>) cur.getTransferData(DataFlavor.javaFileListFlavor);
                                    /* check for changes */
                                    boolean fileschanged = (oldFiles == null) || oldFiles.size() != files.size();
                                    if (!fileschanged) {
                                        /* second check for changes */
                                        for (File file : oldFiles) {
                                            boolean found = false;
                                            for (File file2 : files) {
                                                if (file2.getAbsolutePath().equals(file.getAbsolutePath())) {
                                                    found = true;
                                                    break;
                                                }
                                            }
                                            if (!found) {
                                                fileschanged = true;
                                                break;
                                            }
                                        }
                                    }
                                    if (fileschanged) {
                                        /* changes detected, add files */
                                        oldFiles = files;
                                        for (File file : files) {
                                            JDUtilities.getController().loadContainerFile(file);
                                        }
                                    }
                                    /*
                                     * cannot install clipboardwatcher for
                                     * files, because else windows cut paste
                                     * will no longer work, increase waittime
                                     */
                                    wait = 2000;
                                }
                            } catch (Exception e) {
                                /*
                                 * an error occurred, lets check clipboard again
                                 * to be sure
                                 */
                                clipboardchanged = true;
                                wait = 1000;
                            }
                        } else {
                            /*
                             * an error occurred, lets check clipboard again to
                             * be sure
                             */
                            clipboardchanged = true;
                            wait = 1000;
                        }
                    } catch (Exception e2) {
                    }
                    /* wait in case of fileselection */
                    try {
                        Thread.sleep(wait);
                    } catch (Exception e) {
                    }
                }
                try {
                    Thread.sleep(wait);
                } catch (Exception e) {
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
