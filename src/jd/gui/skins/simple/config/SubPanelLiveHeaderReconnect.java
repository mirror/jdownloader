package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JOptionPane;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.HTTPLiveHeader;
import jd.controlling.interaction.Interaction;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
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
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_BUTTON, this, JDLocale.L("gui.config.liveHeader.selectRouter", "Router auswählen")));
        addGUIConfigEntry(ce);

        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.liveHeader.user", "Login User (->%%%user%%%)")));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.liveHeader.password", "Login Passwort (->%%%pass%%%)")));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.liveHeader.routerIP", "RouterIP (->%%%routerip%%%)")));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, JDLocale.L("gui.config.liveHeader.waitTimeForIPCheck", "Wartezeit bis zum ersten IP-Check[sek]"), 0, 600).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_RETRIES, JDLocale.L("gui.config.liveHeader.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5).setExpertEntry(true));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, JDLocale.L("gui.config.liveHeader.waitForIP", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20).setExpertEntry(true));

        routerScript = new GUIConfigEntry(cfg = new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS, JDLocale.L("gui.config.liveHeader.script", "HTTP Script")));
        addGUIConfigEntry(routerScript);

        add(panel, BorderLayout.CENTER);

    }

    @Override
    public void load() {
        this.loadConfigEntries();
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.liveHeader.name", "Reconnect via LiveHeader");
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

        String selected = (String) JOptionPane.showInputDialog(this, JDLocale.L("gui.config.liveHeader.dialog.selectRouter", "Bitte wähle deinen Router aus"), JDLocale.L("gui.config.liveHeader.dialog.importRouter", "Router importieren"), JOptionPane.INFORMATION_MESSAGE, null, d, null);
        if (selected != null) {

            int id = Integer.parseInt(selected.split("\\.")[0]);
            String[] data = scripts.get(id);
            if (data[2].toLowerCase().indexOf("curl") >= 0) {
                JDUtilities.getGUI().showMessageDialog(JDLocale.L("gui.config.liveHeader.warning.noCURLConvert", "JD could not convert this curl-batch to a Live-Header Script. Please consult your JD-Support Team!"));
            }
            routerScript.setData(data[2]);

        }

    }

}
