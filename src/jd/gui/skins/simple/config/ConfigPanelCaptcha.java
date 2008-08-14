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

package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;

import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.captcha.JAntiCaptcha;
import jd.config.ConfigContainer;
import jd.config.ConfigEntry;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelCaptcha extends ConfigPanel implements MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return Boolean.class;
            case 1:
                return String.class;
            }
            return String.class;
        }

        public int getColumnCount() {
            return 2;
        }

        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.jac.column.use", "Verwenden");
            case 1:
                return JDLocale.L("gui.config.jac.column.method", "Methode");
            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return methods.length;
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
            case 0:
                return configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[rowIndex].getName(), true);
            case 1:
                return methods[rowIndex].getName() + " : " + (configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[rowIndex].getName(), true) ? JDLocale.L("gui.config.jac.status.auto", "Automatische Erkennung") : JDLocale.L("gui.config.jac.status.noauto", "Manuelle Eingabe"));
            }
            return null;
        }
    }

    private static final long serialVersionUID = 1592765387324291781L;

    private ConfigEntriesPanel cep;

    private Configuration configuration;

    private ConfigContainer container;

    private File[] methods;

    private JTable table;

    public ConfigPanelCaptcha(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        methods = JAntiCaptcha.getMethods("jd/captcha/methods/");
        // logger.info(methods.length + "");

        initPanel();

        load();

    }

    // public void actionPerformed(ActionEvent e) {
    // logger.info(e.getActionCommand());
    // if (e.getActionCommand().equalsIgnoreCase(JDLocale.L(
    // "gui.config.captcha.btn_train", "Captcha Training starten"))) {
    // JDUtilities.runCommand("java", new String[] { "-jar", "-Xmx512m",
    // "JDownloader.jar", "-t" },
    // JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
    //
    // }
    // else if
    // (e.getActionCommand().equalsIgnoreCase(JDLocale.L(
    // "gui.config.captcha.ces.btn_register",
    // "Registrieren"))) {
    // String user =
    // JDUtilities.getGUI().showUserInputDialog(JDLocale.L(
    // "gui.config.captcha.ces.register",
    // "Gewünschter Benutzername?"));
    //
    // if (user != null) {
    // CESClient ces = new CESClient();
    // String pass = ces.register(user);
    // if (pass == null) {
    // JDUtilities.getGUI().showHTMLDialog(JDLocale.L(
    // "gui.config.captcha.ces.register.error",
    // "Fehler aufgetreten"), ces.getStatusText());
    // } else {
    // JDUtilities.getGUI().showHTMLDialog(JDLocale.L(
    // "gui.config.captcha.ces.register.success",
    // "Erfolgreich"),
    // JDLocale.L("gui.config.captcha.ces.register.success.logins", "Deine
    // neuen C.E.S Logins<br><font color='RED'>Logins unbedingt
    // aufschreiben!</font> Verlorene Logins können nicht ersetzt werden") +
    // "<hr><p>" + user + ":" + pass + "</p>");
    // JDUtilities.getSubConfig("JAC").setProperty(CESClient.PARAM_USER,
    // user);
    // JDUtilities.getSubConfig("JAC").setProperty(CESClient.PARAM_PASS,
    // pass);
    // JDUtilities.getSubConfig("JAC").save();
    // JDUtilities.saveConfig();
    // ConfigurationDialog.DIALOG.dispose();
    // ConfigurationDialog.DIALOG.setVisible(false);
    //
    // }
    // }
    //
    // } else if
    // (e.getActionCommand().equalsIgnoreCase(JDLocale.L(
    // "gui.config.captcha.ces.btn_sendmessages",
    // "Nachricht senden"))) {
    // CESClient ces = new CESClient();
    //ces.setLogins(JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.
    // PARAM_USER),
    // JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_PASS));
    // String nick = JOptionPane.showInputDialog(ConfigurationDialog.DIALOG,
    // JDLocale.L("gui.config.captcha.ces.sendMessage.askUser", "Wer soll
    // die Nachricht erhalten?"));
    // String message =
    // TextAreaDialog.showDialog(ConfigurationDialog.DIALOG,
    // JDLocale.L("gui.config.captcha.ces.sendMessage.askMessage",
    // "Nachricht eingeben"),
    // JDLocale.L("gui.config.captcha.ces.sendMessage.askMessage",
    // "Nachricht eingeben"), "");
    //
    // if (nick == null || message == null || nick.trim().length() == 0 ||
    // message.trim().length() == 0 || !ces.sendMessage(nick, message)) {
    // JOptionPane.showMessageDialog(ConfigurationDialog.DIALOG,
    // JDLocale.L("gui.config.captcha.ces.sendMessage.error", "Fehler!
    // Nachricht nicht verschickt."));
    // } else {
    // JOptionPane.showMessageDialog(ConfigurationDialog.DIALOG,
    // String.format(JDLocale.L("gui.config.captcha.ces.sendMessage.success",
    // "Nachricht an %s verschickt"), nick));
    // }
    // } else if
    // (e.getActionCommand().equalsIgnoreCase(JDLocale.L(
    // "gui.config.captcha.ces.btn_stats",
    // "Meine Statistiken"))) {
    //
    // try {
    // save();
    // JLinkButton.openURL("http://dvk.com.ua/rapid/index.php?Nick=" +
    // JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_USER)
    // + "&Pass=" +
    // JDUtilities.getSubConfig("JAC").getStringProperty(CESClient.PARAM_PASS));
    // } catch (MalformedURLException e1) {
    //
    // e1.printStackTrace();
    // }
    // } else if
    // (e.getActionCommand().equalsIgnoreCase(JDLocale.L(
    // "gui.config.captcha.ces.btn_messages",
    // "Meine Nachrichten anzeigen"))) {
    // Object oldMessages =
    // JDUtilities.getSubConfig("JAC").getProperty(CESClient.MESSAGES);
    // HashMap<Integer, ArrayList<String>> savedMessages = null;
    // if (oldMessages != null) {
    // savedMessages = (HashMap<Integer, ArrayList<String>>) oldMessages;
    // } else {
    // JDUtilities.getGUI().showMessageDialog(JDLocale.L(
    // "captcha.ces.message.nomessages",
    // "C.E.S. Keine Nachrichten für dich!"));
    // return;
    // }
    // String html = "<link href=\"http://jdownloader.org/jdccs.css\"
    // rel=\"stylesheet\" type=\"text/css\" />";
    //
    // ArrayList<String> message;
    // int i = 0;
    // for (Iterator<Entry<Integer, ArrayList<String>>> it =
    // savedMessages.entrySet().iterator(); it.hasNext();) {
    // Entry<Integer, ArrayList<String>> next = it.next();
    // message = next.getValue();
    // html += "<br" +
    // String.format(JDLocale.L("captcha.ces.message.bodywithoutstyle",
    // "<div><p>%s Nachricht von %s<hr>%s</p></div>"),
    // Encoding.htmlDecode(message.get(0)),
    // Encoding.htmlDecode(message.get(1)),
    // Encoding.htmlDecode(message.get(2)));
    // i++;
    // }
    // String title =
    // String.format(JDLocale.L("captcha.ces.message.titleoverview", "C.E.S.
    // %s Nachrichten"), i + "");
    // JDUtilities.getGUI().showHTMLDialog(title, html);
    // }
    // else {
    // JDUtilities.runCommand("java", new String[] { "-jar", "-Xmx512m",
    // "JDownloader.jar", "-s" },
    // JDUtilities.getResourceFile(".").getAbsolutePath(), 0);
    //
    // }
    //
    // }

    public String getName() {

        return JDLocale.L("gui.config.jac.name", "jAntiCaptcha");
    }

    public void initPanel() {
        setupContainer();
        setLayout(new BorderLayout());

        this.add(cep = new ConfigEntriesPanel(container, "Captcha"), BorderLayout.NORTH);
        table = new JTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(internalTableModel);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setEditingRow(0);
        table.addMouseListener(this);
        // setPreferredSize(new Dimension(700, 350));

        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {

            case 0:
                column.setMaxWidth(80);
                column.setPreferredWidth(50);
                break;
            case 1:
                column.setPreferredWidth(600);
                break;
            }
        }

        this.add(new JScrollPane(table), BorderLayout.CENTER);

    }

    /**
     * Lädt alle Informationen
     */
    public void load() {
        loadConfigEntries();
    }

    /*
     * private int getSelectedIndex() { return table.getSelectedRow(); }
     */

    public void mouseClicked(MouseEvent e) {

        configuration.setProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[table.getSelectedRow()].getName(), !configuration.getBooleanProperty(Configuration.PARAM_JAC_METHODS + "_" + methods[table.getSelectedRow()].getName(), true));
        table.tableChanged(new TableModelEvent(table.getModel()));
    }

    /*
     * private File getSelectedMethod() { int index = getSelectedIndex(); if
     * (index < 0) return null; return this.methods[index]; }
     */

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
    }

    public void mouseReleased(MouseEvent e) {
    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];
        // PluginForSearch plg;
        // for (int i = 0; i < pluginsForSearch.size(); i++) {
        // plg = pluginsForSearch.elementAt(i);
        // if (plg.getProperties() != null)
        // configuration.setProperty("PluginConfig_" + plg.getPluginName(),
        // plg.getProperties());
        // }
        cep.save();
        saveConfigEntries();

    }

    public void setupContainer() {
        container = new ConfigContainer(this);

        // ConfigContainer jac = new ConfigContainer(this,
        // JDLocale.L("gui.config.captcha.JAC.tab", "jAntiCaptcha"));
        // ConfigContainer ces = new ConfigContainer(this,
        // JDLocale.L("gui.config.captcha.CES.tab", "Captcha Exchange
        // Service"));
        // container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER,
        // jac));
        // container.addEntry(new ConfigEntry(ConfigContainer.TYPE_CONTAINER,
        // ces));

        ConfigEntry ce;
        ConfigEntry conditionEntry;
        // GENERAL

        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getSubConfig("JAC"), Configuration.JAC_SHOW_TIMEOUT, JDLocale.L("gui.config.captcha.train.show_timeout", "Anzeigedauer des Eingabefensters"), 0, 600).setDefaultValue(20);

        container.addEntry(ce);

        // JAC

        ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX, configuration, Configuration.PARAM_CAPTCHA_JAC_DISABLE, JDLocale.L("gui.config.captcha.jac_disable", "Automatische Bilderkennung abschalten")).setDefaultValue(false);

        conditionEntry = ce;
        container.addEntry(ce);

        // ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // JDUtilities.getSubConfig("JAC"), "SHOW_EXTENDED_CAPTCHA",
        // JDLocale.L("gui.config.captcha.extendedCaptcha", "Captchaverarbeitung
        // anzeigen"));
        // ce.setDefaultValue(true);
        //
        // ce.setEnabledCondidtion(conditionEntry, "==", false);
        // jac.addEntry(ce);

        ce = new ConfigEntry(ConfigContainer.TYPE_SPINNER, JDUtilities.getSubConfig("JAC"), Configuration.AUTOTRAIN_ERROR_LEVEL, JDLocale.L("gui.config.captcha.train.level", "Anzeigeschwelle"), 0, 100).setDefaultValue(80);
        ce.setInstantHelp(JDLocale.L("gui.config.captcha.train.level.instanthelp", "http://jdownloader.org/wiki/index.php?title=Captchafenster_abschalten_%26_Anzeigeschwelle"));
        ce.setEnabledCondidtion(conditionEntry, "==", false);
        container.addEntry(ce);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this,
        // JDLocale.L("gui.config.captcha.btn_train", "Captcha Training
        // starten"));
        // jac.addEntry(ce);
        // ce.setEnabledCondidtion(conditionEntry, "==", false);
        // ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this,
        // JDLocale.L("gui.config.captcha.btn_show", "Testbild auswerten"));
        // jac.addEntry(ce);
        // ce.setEnabledCondidtion(conditionEntry, "==", false);
        // CES
        // ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // JDUtilities.getSubConfig("JAC"), Configuration.JAC_USE_CES,
        // JDLocale.L("gui.config.captcha.use_Ces", "CaptchaExchangeServer
        // verwenden(Neustart nötig)"));
        // ce.setDefaultValue(true);
        // conditionEntry = ce;
        // ce.setInstantHelp(JDLocale.L("gui.config.captcha.ces.help",
        // "http://jdownloader.org/wiki/index.php?title=C.E.S_-_Captcha_Exchange_Service"
        // ));

        // ce.setInstantHelp(JDLocale.L(
        // "gui.config.captcha.extendedCaptcha.instanthelp",
        // "http://ns2.km32221.keymachine.de/jdownloader/web/page.php?id=3"));
        // ces.addEntry(ce);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_TEXTFIELD,
        // JDUtilities.getSubConfig("JAC"), CESClient.PARAM_USER,
        // JDLocale.L("gui.config.captcha.ces.user", "C.E.S. Benutzername"));
        // ce.setDefaultValue("JD_" +
        // JDUtilities.getConfiguration().getIntegerProperty(Configuration.CID,
        // (int) System.currentTimeMillis()));
        //
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        // ces.addEntry(ce);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_PASSWORDFIELD,
        // JDUtilities.getSubConfig("JAC"), CESClient.PARAM_PASS,
        // JDLocale.L("gui.config.captcha.ces.pass", "C.E.S. Passwort"));
        //
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        // ces.addEntry(ce);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_CHECKBOX,
        // JDUtilities.getSubConfig("JAC"), CESClient.DO_WARNING,
        // JDLocale.L("gui.config.captcha.useCESPenaltyWarning", "Durch
        // frühzeitige Rückfrage Punktstrafen vermeiden"));
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        // ce.setInstantHelp(JDLocale.L("gui.config.captcha.ces.help",
        // "http://jdownloader.org/wiki/index.php?title=C.E.S_-_Captcha_Exchange_Service"
        // ));
        //
        // ce.setDefaultValue(true);
        // ces.addEntry(ce);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this,
        // JDLocale.L("gui.config.captcha.ces.btn_register", "Registrieren"));
        // ces.addEntry(ce);
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this,
        // JDLocale.L("gui.config.captcha.ces.btn_stats", "Meine Statistiken"));
        // ces.addEntry(ce);
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this,
        // JDLocale.L("gui.config.captcha.ces.btn_messages", "Meine Nachrichten
        // anzeigen"));
        // ces.addEntry(ce);
        // ce.setEnabledCondidtion(conditionEntry, "==", true);
        //
        // ce = new ConfigEntry(ConfigContainer.TYPE_BUTTON, this,
        // JDLocale.L("gui.config.captcha.ces.btn_sendmessages", "Nachricht
        // senden"));
        // ces.addEntry(ce);
        // ce.setEnabledCondidtion(conditionEntry, "==", true);

    }

}
