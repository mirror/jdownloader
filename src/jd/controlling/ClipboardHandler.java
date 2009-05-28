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
import java.io.File;
import java.util.List;

import jd.Main;
import jd.config.Configuration;
import jd.event.ControlEvent;
import jd.event.ControlListener;
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

    // private Logger logger;

    private boolean enabled = false;

    private String olddata;

    private List<?> oldList;

    private boolean waitFlag;
    private boolean tempdisabled = true;

    /**
     */
    private ClipboardHandler() {
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        JDUtilities.getController().addControlListener(this);
        this.enabled = false;
        this.setName("ClipboardHandler");
        this.start();
    }

    public void setOldData(String data) {
        olddata = data;
    }

    /**
     * Gibt an ob die clipboard überwachung aktiv ist
     * 
     * @return true/false
     */
    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, true);
    }

    // @Override
    public void run() {
        while (true) {
            waitFlag = true;
            synchronized (this) {
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
                    for (DataFlavor element : clipboard.getAvailableDataFlavors()) {

                        if (element.isFlavorJavaFileListType()) {
                            List<?> list = (List<?>) clipboard.getData(element);

                            boolean ch = oldList == null || list.size() != oldList.size();
                            if (!ch) {
                                for (int t = 0; t < list.size(); t++) {
                                    if (!((File) list.get(t)).getAbsolutePath().equals(((File) oldList.get(t)).getAbsolutePath())) {
                                        ch = true;
                                        break;
                                    }

                                }
                            }
                            if (ch) {
                                oldList = list;
                                for (int t = 0; t < list.size(); t++) {
                                    JDUtilities.getController().loadContainerFile((File) list.get(t));
                                }
                            }

                            break;

                        }
                        if (element.isFlavorTextType() && element.getRepresentationClass() == String.class && element.getHumanPresentableName().equals("Unicode String")) {

                            // if (olddata == null) {
                            // olddata = data;
                            // }
                            if (!((String) clipboard.getData(element)).equals(olddata)) {
                                olddata = (String) clipboard.getData(element);

                                new DistributeData(olddata.trim(), true).start();
                            }
                            break;

                        }

                    }
                    Thread.sleep(500);
                } catch (Exception e2) {
                    // JDLogger.exception(e2);
                }
            }
        }
    }

    public void setTempDisabled(boolean v) {
        this.tempdisabled = v;
        if (waitFlag) {
            waitFlag = false;
            synchronized (this) {
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
        if (waitFlag) {
            waitFlag = false;
            synchronized (this) {
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
