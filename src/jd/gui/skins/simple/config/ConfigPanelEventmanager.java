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
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
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
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.config.Configuration;
import jd.config.SubConfiguration;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.gui.UIInterface;
import jd.utils.JDLocale;
import jd.utils.JDUtilities;

public class ConfigPanelEventmanager extends ConfigPanel implements ActionListener, MouseListener, DropTargetListener {

    private class InternalTableModel extends AbstractTableModel {

        private static final long serialVersionUID = 1155282457354673850L;

        @Override
        public Class<?> getColumnClass(int columnIndex) {
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

    private static final long serialVersionUID = 7055395315659682282L;

    private JButton btnAdd;

    private JButton btnEdit;

    private JButton btnRemove;

    private JButton btnTrigger;

    @SuppressWarnings("unused")
    private Configuration configuration;

    private SubConfiguration subConfig = JDUtilities.getSubConfig(Configuration.CONFIG_INTERACTIONS);

    private Interaction currentInteraction;

    private Interaction draggedInteraction;

    private Vector<Interaction> interactions;

    private JLabel lblTrigger;

    private JTable table;

    private InternalTableModel tableModel;

    @SuppressWarnings("unchecked")
    public ConfigPanelEventmanager(Configuration configuration, UIInterface uiinterface) {
        super(uiinterface);
        this.configuration = configuration;
        interactions = (Vector<Interaction>) subConfig.getProperty(Configuration.PARAM_INTERACTIONS, new Vector<Interaction>());
        initPanel();
        load();
    }

    public void actionPerformed(ActionEvent e) {

        if (e.getSource() == btnAdd) {
            InteractionTrigger event = (InteractionTrigger) JOptionPane.showInputDialog(this, JDLocale.L("gui.config.eventmanager.new.selectTrigger.title", "Trigger auswählen"), JDLocale.L("gui.config.eventmanager.new.selectTrigger.desc", "Wann soll eine Aktion ausgeführt werden?"), JOptionPane.QUESTION_MESSAGE, null, InteractionTrigger.getAllTrigger(), null);
            if (event == null) return;

            Interaction interaction = (Interaction) JOptionPane.showInputDialog(this, JDLocale.LF("gui.config.eventmanager.new.selectAction.title", "Aktion auswählen für \"%s\"", event.getName()), JDLocale.L("gui.config.eventmanager.new.selectAction.desc", "Welche Aktion soll ausgeführt werden?"), JOptionPane.QUESTION_MESSAGE, null, Interaction.getInteractionList(), null);
            if (interaction == null) return;

            interaction.setTrigger(event);
            interactions.add(interaction);
            int newRow = interactions.size() - 1;
            tableModel.fireTableRowsInserted(newRow, newRow);
            table.getSelectionModel().setSelectionInterval(newRow, newRow);
        } else if (e.getSource() == btnRemove) {
            int index = table.getSelectedRow();
            if (index < 0) return;
            interactions.remove(index);
            tableModel.fireTableRowsDeleted(index, index);
            int newRow = Math.min(index, interactions.size() - 1);
            table.getSelectionModel().setSelectionInterval(newRow, newRow);
        } else if (e.getSource() == btnTrigger) {
            InteractionTrigger event = (InteractionTrigger) JOptionPane.showInputDialog(this, JDLocale.L("gui.config.eventmanager.new.selectTrigger.title", "Trigger auswählen"), JDLocale.L("gui.config.eventmanager.new.selectTrigger.desc", "Wann soll eine Aktion ausgeführt werden?"), JOptionPane.QUESTION_MESSAGE, null, InteractionTrigger.getAllTrigger(), currentInteraction.getTrigger());
            if (event == null) return;

            currentInteraction.setTrigger(event);
            lblTrigger.setText(event + "");
        } else if (e.getSource() == btnEdit) {
            editEntry();
        }

    }

    public void dragEnter(DropTargetDragEvent e) {
        draggedInteraction = interactions.get(table.getSelectedRow());
    }

    public void dragExit(DropTargetEvent dte) {
    }

    public void dragOver(DropTargetDragEvent e) {
        int oldId = interactions.indexOf(draggedInteraction);
        int id = table.rowAtPoint(e.getLocation());
        interactions.remove(draggedInteraction);
        interactions.add(id, draggedInteraction);
        tableModel.fireTableRowsUpdated(Math.min(oldId, id), Math.max(oldId, id));
        table.getSelectionModel().setSelectionInterval(id, id);
    }

    public void drop(DropTargetDropEvent e) {
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
    }

    private void editEntry() {
        currentInteraction = interactions.elementAt(table.getSelectedRow());
        ConfigPanel config = new ConfigEntriesPanel(currentInteraction.getConfig());

        JPanel tpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 5));
        tpanel.add(btnTrigger = new JButton(JDLocale.L("gui.config.eventmanager.trigger.btn", "Trigger ändern")));
        tpanel.add(new JLabel(currentInteraction.getTrigger().toString()));

        btnTrigger.addActionListener(this);

        JPanel npanel = new JPanel(new BorderLayout());
        npanel.add(tpanel);
        npanel.add(new JSeparator(), BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(npanel, BorderLayout.NORTH);
        if (config != null) panel.add(config);

        ConfigurationPopup pop = new ConfigurationPopup(JDUtilities.getParentFrame(this), config, panel);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
    }

    @Override
    public String getName() {
        return JDLocale.L("gui.config.eventmanager.name", "Eventmanager");
    }

    @Override
    public void initPanel() {
        this.setLayout(new BorderLayout());
        this.setPreferredSize(new Dimension(700, 350));

        tableModel = new InternalTableModel();
        table = new JTable(tableModel);
        table.addMouseListener(this);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                btnEdit.setEnabled((table.getSelectedRow() >= 0) && interactions.get(table.getSelectedRow()).getConfig().getEntries().size() != 0);
            }
        });
        table.setDragEnabled(true);
        new DropTarget(table, this);

        TableColumn column = null;
        for (int c = 0; c < tableModel.getColumnCount(); c++) {
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

        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400, 200));

        JPanel bpanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 2));
        bpanel.add(btnAdd = new JButton(JDLocale.L("gui.config.eventmanager.btn_add", "+")));
        bpanel.add(btnRemove = new JButton(JDLocale.L("gui.config.eventmanager.btn_remove", "-")));
        bpanel.add(btnEdit = new JButton(JDLocale.L("gui.config.eventmanager.btn_settings", "Einstellungen")));

        btnEdit.setEnabled(false);

        btnAdd.addActionListener(this);
        btnRemove.addActionListener(this);
        btnEdit.addActionListener(this);

        this.add(scrollpane);
        this.add(bpanel, BorderLayout.SOUTH);
    }

    @Override
    public void load() {
    }

    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1 && interactions.get(table.getSelectedRow()).getConfig().getEntries().size() != 0) {
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

    @Override
    public void save() {
        subConfig.setProperty(Configuration.PARAM_INTERACTIONS, interactions);
        subConfig.save();
    }

}
