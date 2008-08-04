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
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelEventmanager extends ConfigPanel implements ActionListener, MouseListener {

    private class InternalTableModel extends AbstractTableModel {

        /**
         * 
         */
        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            switch (columnIndex) {
            case 0:
                return String.class;
            case 1:
                return String.class;

            }
            return String.class;
        }

        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
            case 0:
                return JDLocale.L("gui.config.eventmanager.column.action", "Aktion");
            case 1:
                return JDLocale.L("gui.config.eventmanager.column.trigger", "Trigger");
            case 2:
                return JDLocale.L("gui.config.eventmanager.column.triggerDesc", "Triggerbeschreibung");

            }
            return super.getColumnName(column);
        }

        public int getRowCount() {
            return interactions.size();
        }

        public Object getValueAt(int rowIndex, int columnIndex) {

            switch (columnIndex) {
            case 0:
                return interactions.elementAt(rowIndex).getInteractionName();
            case 1:
                return interactions.elementAt(rowIndex).getTrigger().getName();
            case 2:
                return interactions.elementAt(rowIndex).getTrigger().getDescription();

            }
            return null;
        }
    }

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7055395315659682282L;

    // private JList list;

    private JButton btnAdd;

    private JButton btnBottom;

    private JButton btnEdit;

    private JButton btnRemove;

    private JButton btnTop;
    private JButton btnTrigger;

    // private JLabel cboTrigger;
    private Configuration configuration;

    private Interaction currentInteraction;

    private Vector<Interaction> interactions;

    private JLabel lblTrigger;

    private JTable table;

    public ConfigPanelEventmanager(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        initPanel();

        load();

    }

    // private void setListData() {
    // DefaultListModel model = new DefaultListModel();
    //
    // for (int i = 0; i < interactions.size(); i++) {
    // model.add(i, i + ". " + interactions.elementAt(i).getInteractionName() +
    // " ("
    // + interactions.elementAt(i).getTriggerName() + ")");
    // }
    // list.setModel(model);
    // }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnTop) {
            Interaction interaction = getSelectedInteraction();
            int currentIndex = interactions.indexOf(interaction);
            interactions.remove(currentIndex);
            int nextIndex = Math.max(0, currentIndex - 1);
            interactions.insertElementAt(interaction, nextIndex);
            table.tableChanged(new TableModelEvent(table.getModel()));
            table.getSelectionModel().addSelectionInterval(nextIndex, nextIndex);

        }
        if (e.getSource() == btnBottom) {
            Interaction interaction = getSelectedInteraction();
            int currentIndex = interactions.indexOf(interaction);
            interactions.remove(currentIndex);
            int nextIndex = Math.min(interactions.size(), currentIndex + 1);
            interactions.insertElementAt(interaction, nextIndex);
            table.tableChanged(new TableModelEvent(table.getModel()));
            table.getSelectionModel().addSelectionInterval(nextIndex, nextIndex);

        }
        if (e.getSource() == btnAdd) {
            InteractionTrigger[] events = InteractionTrigger.getAllTrigger();
            InteractionTrigger event = (InteractionTrigger) JOptionPane.showInputDialog(this, JDLocale.L("gui.config.eventmanager.new.selectTrigger.title", "Trigger auswählen"), JDLocale.L("gui.config.eventmanager.new.selectTrigger.desc", "Wann soll eine Aktion ausgeführt werden?"), JOptionPane.QUESTION_MESSAGE, null, events, null);
            if (event == null) { return; }

            Interaction[] interacts = getInteractionArray();
            Interaction interaction = (Interaction) JOptionPane.showInputDialog(this, JDUtilities.sprintf(JDLocale.L("gui.config.eventmanager.new.selectAction.title", "Aktion auswählen für \"%s\""), new String[] { event.getName() }), JDLocale.L("gui.config.eventmanager.new.selectAction.desc", "Welche Aktion soll ausgeführt werden?"), JOptionPane.QUESTION_MESSAGE, null, interacts, null);

            if (interaction == null) { return; }

            interaction.setTrigger(event);
            interactions.add(interaction);
            table.tableChanged(new TableModelEvent(table.getModel()));
        }
        if (e.getSource() == btnRemove) {
            int index = getSelectedInteractionIndex();
            if (index >= 0) {
                interactions.remove(index);
                table.tableChanged(new TableModelEvent(table.getModel()));
            }
        }

        if (e.getSource() == btnTrigger) {
            Interaction interaction = currentInteraction;
            InteractionTrigger[] events = InteractionTrigger.getAllTrigger();

            InteractionTrigger event = (InteractionTrigger) JOptionPane.showInputDialog(this, JDLocale.L("gui.config.eventmanager.new.selectTrigger.title", "Trigger auswählen"), JDLocale.L("gui.config.eventmanager.new.selectTrigger.desc", "Wann soll eine Aktion ausgeführt werden?"), JOptionPane.QUESTION_MESSAGE, null, events, currentInteraction);
            if (event == null) { return; }

            interaction.setTrigger(event);
            lblTrigger.setText(event + "");
        }
        if (e.getSource() == btnEdit) {
            editEntry();

        }

    }

    private void editEntry() {
        Interaction interaction = getSelectedInteraction();

        if (interaction != null) {

            logger.info(interaction.getConfig().getEntries() + " _ ");
            if (interaction.getConfig().getEntries().size() > 0) {
                openPopupPanel(new ConfigPanelDefault(uiinterface, interaction.getConfig()));
            }

        }

    }

    private Interaction[] getInteractionArray() {
        Interaction[] interacts = Interaction.getInteractionList();
        return interacts;
    }

    @Override
    public String getName() {

        return JDLocale.L("gui.config.eventmanager.name", "Eventmanager");
    }

    private Interaction getSelectedInteraction() {
        int index = getSelectedInteractionIndex();
        if (index < 0) { return null; }
        return interactions.elementAt(index);
    }

    private int getSelectedInteractionIndex() {
        return table.getSelectedRow();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void initPanel() {
        setLayout(new BorderLayout());
        table = new JTable();
        table.getTableHeader().setPreferredSize(new Dimension(-1, 25));
        InternalTableModel internalTableModel = new InternalTableModel();
        table.setModel(new InternalTableModel());
        setPreferredSize(new Dimension(700, 350));
        // table.getColumn(table.getColumnName(1)).setCellRenderer(new
        // ComboBoxRenderer());
        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch (c) {

            case 0:
                column.setPreferredWidth(150);
                break;
            case 1:
                column.setPreferredWidth(150);
                break;
            case 2:
                column.setPreferredWidth(450);
                break;

            }
        }

        interactions = new Vector<Interaction>();

        Vector<Interaction> tmp = (Vector<Interaction>) JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());

        if (tmp != null) {
            for (int i = 0; i < tmp.size(); i++) {

                if (tmp.get(i) != null) {
                    interactions.add(tmp.get(i));
                }
            }
        }

        // add(scrollPane);
        // list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));
        btnAdd = new JButton(JDLocale.L("gui.config.eventmanager.btn_add", "+"));
        btnRemove = new JButton(JDLocale.L("gui.config.eventmanager.btn_remove", "-"));
        btnEdit = new JButton(JDLocale.L("gui.config.eventmanager.btn_settings", "Einstellungen"));
        btnTop = new JButton(JDLocale.L("gui.config.eventmanager.btn_up", "nach oben!"));
        btnBottom = new JButton(JDLocale.L("gui.config.eventmanager.btn_down", "nach unten!"));
        btnTop.addActionListener(this);
        btnBottom.addActionListener(this);
        btnAdd.addActionListener(this);
        btnRemove.addActionListener(this);
        btnEdit.addActionListener(this);
        // JDUtilities.ahddToGridBag(panel, scrollpane, 0, 0, 5, 1, 1, 1,
        // insets,
        // GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        // JDUtilities.addToGridBag(panel, scrollpane,
        // GridBagConstraints.RELATIVE,
        // GridBagConstraints.RELATIVE, GridBagConstraints.REMAINDER, 1, 1, 1,
        // insets,
        // GridBagConstraints.BOTH, GridBagConstraints.CENTER);
        //
        // JDUtilities.addToGridBag(panel, btnAdd, 0, 1, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, btnRemove, 1, 1, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, btnEdit, 2, 1, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, btnTop, 3, 1, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);
        // JDUtilities.addToGridBag(panel, btnBottom, 4, 1, 1, 1, 0, 1, insets,
        // GridBagConstraints.NONE, GridBagConstraints.WEST);

        JPanel contentPanel = new JPanel(new BorderLayout(5, 5));
        int n = 5;
        contentPanel.setBorder(new EmptyBorder(0, n, 0, n));
        contentPanel.add(scrollpane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(5, 5, FlowLayout.LEFT));
        buttonPanel.add(btnAdd);
        buttonPanel.add(btnRemove);
        buttonPanel.add(btnEdit);
        buttonPanel.add(btnTop);
        buttonPanel.add(btnBottom);

        contentPanel.add(buttonPanel, BorderLayout.SOUTH);
        add(contentPanel, BorderLayout.CENTER);
        // add(buttonPanel, BorderLayout.SOUTH);
        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);

    }

    /**
     * Lädt alle Informationen
     */
    @Override
    public void load() {
        // setListData();

    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
            editEntry();
        }

    }

    public void mouseEntered(MouseEvent e) {

    }

    public void mouseExited(MouseEvent e) {

    }

    public void mousePressed(MouseEvent e) {

    }

    public void mouseReleased(MouseEvent e) {

    }

    private void openPopupPanel(ConfigPanel config) {
        JPanel panel = new JPanel(new GridBagLayout());

        InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();

        Interaction interaction = getSelectedInteraction();
        currentInteraction = interaction;
        if (interaction == null) { return; }
        InteractionTrigger trigger = interaction.getTrigger();

        int i;

        for (i = 0; i < triggers.length; i++) {
            if (triggers[i].getID() == trigger.getID()) {
                break;
            }
        }

        lblTrigger = new JLabel();
        btnTrigger = new JButton(JDLocale.L("gui.config.eventmanager.trigger.btn", "Trigger ändern"));
        btnTrigger.addActionListener(this);
        lblTrigger.setText(trigger + "");
        // cboTrigger.setSelectedIndex(indexT);
        // JPanel topPanel = new JPanel();
        // topPanel.add(new
        // JLabel(JDLocale.L("gui.config.eventmanager.trigger","Trigger
        // Event")));
        // topPanel.add(cboTrigger);
        JDUtilities.addToGridBag(panel, btnTrigger, 0, 0, 1, 1, 0, 0, new Insets(5, 10, 5, 5), GridBagConstraints.NONE, GridBagConstraints.WEST);

        JDUtilities.addToGridBag(panel, lblTrigger, 1, 0, 1, 1, 1, 0, new Insets(5, 0, 5, 5), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, new JSeparator(), 0, 1, 2, 1, 1, 0, new Insets(0, 0, 0, 0), GridBagConstraints.HORIZONTAL, GridBagConstraints.WEST);

        // panel.add(topPanel, BorderLayout.WEST);
        if (config != null) {
            JDUtilities.addToGridBag(panel, config, 0, 2, 2, 1, 1, 1, null, GridBagConstraints.BOTH, GridBagConstraints.NORTHWEST);

            // panel.add(config, BorderLayout.CENTER);
        }
        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel, uiinterface, configuration);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    @Override
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];
        JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).setProperty(Configuration.PARAM_INTERACTIONS, interactions);

        JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS).save();

    }

}
