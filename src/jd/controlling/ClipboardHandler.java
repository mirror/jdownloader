//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
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

    public static ClipboardHandler getClipboard() {
        if (INSTANCE == null) {
            new ClipboardHandler();
        }
        return INSTANCE;

    }

    private Clipboard clipboard;

    // private Logger logger;

    /**
     * Der Thread, der den Inhalt der Zwischenablage verteilt
     */
    private DistributeData distributeData = null;

    private boolean enabled = JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false);

    private String olddata;

    private List<?> oldList;

    /**
     */
    private ClipboardHandler() {
        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        INSTANCE = this;
        // logger = JDUtilities.getLogger();
        JDUtilities.getController().addControlListener(this);
        this.setName("ClipboardHandler");
    }

    /**
     * Gibt an ob die clipboard überwachung aktiv ist
     * 
     * @return true/false
     */
    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false);
    }

    @Override
    public void run() {
        enabled = true;
        while (enabled) {
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

                            distributeData = new DistributeData(olddata.trim(), true);
                            distributeData.addControlListener(JDUtilities.getController());
                            distributeData.start();
                        }
                        break;

                    }

                }
                Thread.sleep(500);
            } catch (Exception e2) {
                // e2.printStackTrace();
            }
        }
    }

    /**
     * Schaltet die clipboardüberwachung an/aus
     * 
     * @param enabled
     */
    private void setEnabled(boolean enabled2) {
        enabled = enabled2;
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, enabled2);
        JDUtilities.getConfiguration().save();

        if (enabled && !isAlive()) {
            new ClipboardHandler();
            INSTANCE.start();
        }

    }

    public void toggleActivation() {
        setEnabled(!isEnabled());
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_INIT_COMPLETE && event.getSource() instanceof Main) {
            if (enabled) INSTANCE.start();
            JDUtilities.getController().removeControlListener(this);
        }

    }

}
