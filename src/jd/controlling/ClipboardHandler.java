package jd.controlling;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.io.File;
import java.util.List;
import java.util.logging.Logger;

import jd.config.Configuration;
import jd.utils.JDUtilities;

/**
 * Diese Klasse ist dafür da, zeitverzögert die Zwischenablage zu untersuchen
 * 
 * @author astaldo/coalado
 */
public class ClipboardHandler extends Thread {
    /**
     * Der Thread, der den Inhalt der Zwischenablage verteilt
     */
    private DistributeData distributeData = null;

    private boolean enabled = true;

    private Clipboard clipboard;

    private Logger logger;

    private List oldList;

    private Thread saveConfig = null;

    private String olddata;

    /**
     */
    public ClipboardHandler() {

        clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();

        logger = JDUtilities.getLogger();
        this.start();
    }

    /**
     * Gibt an ob die clipboard überwachung aktiv ist
     * 
     * @return true/false
     */
    public boolean isEnabled() {
        return JDUtilities.getConfiguration().getBooleanProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, false);
    }

    public void toggleActivation() {
        setEnabled(!isEnabled());
    }

    /**
     * Schaltet die clipboardüberwachung an/aus
     * 
     * @param enabled
     */
    private void setEnabled(boolean enabled) {
        this.enabled = enabled;
        JDUtilities.getConfiguration().setProperty(Configuration.PARAM_CLIPBOARD_ALWAYS_ACTIVE, enabled);
        if (saveConfig != null) {
            while (saveConfig.isAlive()) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
        saveConfig = new Thread(new Runnable() {

            public void run() {
                JDUtilities.saveConfig();
            }
        });
        saveConfig.start();

        if (enabled && !this.isAlive())
            JDUtilities.getController().setClipboard(new ClipboardHandler());
    }

    public void run() {
        enabled = isEnabled();
        while (enabled) {
            try {
                DataFlavor[] flavors = clipboard.getAvailableDataFlavors();
                for (int i = 0; i < flavors.length; i++) {

                    if (flavors[i].isFlavorJavaFileListType()) {
                        List list = (List) clipboard.getData(flavors[i]);
                        // Prüfen ob es eine neue Liste ist
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
                        // ((List)clipboard.getData(flavors[i])).
                        break;

                    }
                    if (flavors[i].isFlavorTextType() && flavors[i].getRepresentationClass() == String.class && flavors[i].getHumanPresentableName().equals("Unicode String")) {
                        String data = (String) clipboard.getData(flavors[i]);

                        data = data.trim();
                        if (!data.equals(olddata)) {
                            olddata = data;
                            logger.info(data.length() + " - " + olddata.length());
                            logger.info("|" + data + "|" + " - " + "|" + olddata + "|");
                            distributeData = new DistributeData(data);
                            distributeData.addControlListener(JDUtilities.getController());
                            distributeData.start();
                        }
                        break;

                    }

                }
                Thread.sleep(100);
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

}
