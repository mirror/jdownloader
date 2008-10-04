package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.LogRecord;

import javax.swing.JButton;
import javax.swing.ScrollPaneConstants;

import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.gui.UIInterface;
import jd.gui.skins.simple.components.MiniLogDialog;
import jd.router.GetRouterInfo;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class SubPanelCLRReconnect extends ConfigPanel implements ActionListener, ControlListener {

    private static final long serialVersionUID = 6710420298517566329L;

    private JButton btnFindIP;

    private GUIConfigEntry ip;

    private MiniLogDialog mld;

    private GUIConfigEntry pass;

    private GUIConfigEntry routerScript;

    private GUIConfigEntry user;

    public SubPanelCLRReconnect(UIInterface uiinterface, Interaction interaction) {
        super(uiinterface);
        // this.configuration = configuration;
        initPanel();
        load();

    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnFindIP) {
            JDUtilities.getController().addControlListener(this);
            mld = new MiniLogDialog(JDLocale.L("gui.config.routeripfinder", "Router IPsearch"));
            mld.getBtnOK().setEnabled(false);
            mld.getProgress().setMaximum(100);
            mld.getProgress().setValue(2);
            // mld.setEnabled(true);
            new Thread() {
                @Override
                public void run() {
                    ip.setData(JDLocale.L("gui.config.routeripfinder.featchIP", "Suche nach RouterIP..."));
                    mld.getProgress().setValue(60);
                    GetRouterInfo rinfo = new GetRouterInfo(null);
                    mld.getProgress().setValue(80);
                    ip.setData(rinfo.getAdress());
                    mld.getProgress().setValue(100);

                    mld.getBtnOK().setEnabled(true);
                    mld.getBtnOK().setText(JDLocale.L("gui.config.routeripfinder.close", "Fenster schließen"));
                }
            }.start();

        }
    }

    public void controlEvent(ControlEvent event) {
        if (event.getID() == ControlEvent.CONTROL_LOG_OCCURED && mld != null && mld.isEnabled()) {
            LogRecord l = (LogRecord) event.getParameter();

            if (l.getSourceClassName().startsWith("jd.router.GetRouterInfo")) {
                mld.setText(JDUtilities.formatSeconds((int) l.getMillis() / 1000) + " : " + l.getMessage() + "\r\n" + mld.getText());
                mld.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                mld.getProgress().setValue(mld.getProgress().getValue() + 1);
            }
        }

    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.clr.name", "Reconnect via CLR");
    }

    @Override
    public void initPanel() {
        GUIConfigEntry ce;
        // ConfigEntry cfg;
        btnFindIP = new JButton(JDLocale.L("gui.config.liveHeader.btnFindIP", "Router IP ermitteln"));

        btnFindIP.addActionListener(this);
        JDUtilities.addToGridBag(panel, btnFindIP, 2, 0, GridBagConstraints.REMAINDER, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        user = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_USER, JDLocale.L("gui.config.liveHeader.user", "Login User (->%%%user%%%)")));
        addGUIConfigEntry(user);
        pass = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_PASS, JDLocale.L("gui.config.liveHeader.password", "Login Passwort (->%%%pass%%%)")));
        addGUIConfigEntry(pass);
        String routerip = JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP, null);

        ip = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IP, JDLocale.L("gui.config.liveHeader.routerIP", "RouterIP (->%%%routerip%%%)")).setDefaultValue(routerip));
        addGUIConfigEntry(ip);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_IPCHECKWAITTIME, JDLocale.L("gui.config.liveHeader.waitTimeForIPCheck", "Wartezeit bis zum ersten IP-Check[sek]"), 0, 600).setDefaultValue(5));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_RETRIES, JDLocale.L("gui.config.liveHeader.retries", "Max. Wiederholungen (-1 = unendlich)"), -1, 20).setDefaultValue(5));
        addGUIConfigEntry(ce);
        ce = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_WAITFORIPCHANGE, JDLocale.L("gui.config.liveHeader.waitForIP", "Auf neue IP warten [sek]"), 0, 600).setDefaultValue(20));
        addGUIConfigEntry(ce);

        routerScript = new GUIConfigEntry(new ConfigEntry(ConfigContainer.TYPE_TEXTAREA, JDUtilities.getConfiguration(), Configuration.PARAM_HTTPSEND_REQUESTS_CLR, JDLocale.L("gui.config.clr.script", "CLR Script")).setInstantHelp("http://wiki.jdownloader.org/index.php?title=CLR"));

        // addGUIConfigEntry(routerScript);

        this.entries.add(routerScript);
        add(panel, BorderLayout.NORTH);
        add(routerScript, BorderLayout.CENTER);
    }

    @Override
    public void load() {
        loadConfigEntries();
    }

    @Override
    public void save() {
        saveConfigEntries();

    }

}
