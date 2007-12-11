package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;
import java.util.logging.Level;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.event.UIEvent;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.BrowseFile;
import jd.router.RouterData;
import jd.router.RouterParser;
import jd.utils.JDUtilities;

class SubPanelLiveHeaderReconnect extends ConfigPanel implements ActionListener {
    private Configuration  configuration;

    private HTTPLiveHeader lh;

    private GUIConfigEntry routerScript;

    public SubPanelLiveHeaderReconnect(UIInterface uiinterface, Interaction interaction) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();
        this.lh = (HTTPLiveHeader) interaction;
        load();
    }

    public void save() {
        this.saveConfigEntries();

    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;

        ConfigEntry cfg;
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, "Router auswählen"));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, "Login User (->%%%user%%%)"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, "Login Passwort (->%%%pass%%%)"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IP, "RouterIP (->%%%routerip%%%)"));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, "Wartezeit bis zum ersten IP-Check[sek]", 0, 600).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_RETRIES, "Max. Wiederholungen (-1 = unendlich)", -1, 20).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, "Auf neue IP warten [sek]", 0, 600).setDefaultValue(20).setExpertEntry(true));

        routerScript = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS, "HTTP Script"));
        addGUIConfigEntry(routerScript);

        add(panel, BorderLayout.CENTER);

    }

    @Override
    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public String getName() {
        return "Reconnect via LiveHeader";
    }

    public void actionPerformed(ActionEvent e) {
        Vector<String[]> scripts = lh.getLHScripts();

        Collections.sort(scripts, new Comparator<Object>() {
            public int compare(Object a, Object b) {
                String[] aa = (String[]) a;
                String[] bb = (String[]) b;

                if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) > 0) {
                    return 1;
                }
                else if ((aa[0] + " " + aa[1]).compareToIgnoreCase((bb[0] + " " + bb[1])) < 0) {
                    return -1;
                }
                else {
                    return 0;
                }

            }

        });

        HashMap<String, Boolean> ch = new HashMap<String, Boolean>();
        for (int i = scripts.size() - 1; i >= 0; i--) {
            if (ch.containsKey(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2])) {
                scripts.remove(i);
            }
            else {

                ch.put(scripts.get(i)[0] + scripts.get(i)[1] + scripts.get(i)[2], true);
            }
        }
        ch.clear();
        String[] d = new String[scripts.size()];
        for (int i = 0; i < d.length; i++) {
            d[i] = i + ". " + JDUtilities.htmlDecode(scripts.get(i)[0] + " : " + scripts.get(i)[1]);
        }

        String selected = (String) JOptionPane.showInputDialog(this, "Bitte wähle deinen Router aus", "Router importieren", JOptionPane.INFORMATION_MESSAGE, null, d, null);
        if (selected != null) {
         
            int id = Integer.parseInt(selected.split("\\.")[0]);
            String[] data = scripts.get(id);
            if(data[2].toLowerCase().indexOf("curl")>=0){
                JDUtilities.getGUI().showMessageDialog("JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!");
            }
            routerScript.setData(data[2]);

        }

    }

}
