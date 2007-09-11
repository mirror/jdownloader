package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumn;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.interaction.DummyInteraction;
import jd.controlling.interaction.ExternExecute;
import jd.controlling.interaction.ExternReconnect;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.InteractionTrigger;
import jd.controlling.interaction.JAntiCaptcha;
import jd.controlling.interaction.ManuelCaptcha;
import jd.controlling.interaction.WebUpdate;
import jd.gui.UIInterface;

public class ConfigPanelEventmanager extends ConfigPanel implements ActionListener, MouseListener {

    /**
     * serialVersionUID
     */
    private static final long   serialVersionUID = 7055395315659682282L;

    private Vector<Interaction> interactions;

//    private JList               list;

    private JButton             btnAdd;

    private JButton             btnRemove;

    private JButton             btnEdit;

    private Interaction         currentInteraction;

    private JComboBox           cboTrigger;

    private JTable table;

    public ConfigPanelEventmanager(Configuration configuration, UIInterface uiinterface) {
        super(configuration, uiinterface);
        initPanel();
        
        load();
    
    }

    /**
     * Lädt alle Informationen
     */
    public void load() {
//        setListData();

    }

//    private void setListData() {
//        DefaultListModel model = new DefaultListModel();
//
//        for (int i = 0; i < interactions.size(); i++) {
//            model.add(i, i + ". " + interactions.elementAt(i).getInteractionName() + " (" + interactions.elementAt(i).getTriggerName() + ")");
//        }
//        list.setModel(model);
//    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];

        configuration.setInteractions(interactions);

    }

    @Override
    public void initPanel() {
        setLayout(new BorderLayout());
        table = new JTable();
        InternalTableModel internalTableModel=new InternalTableModel();
        table.setModel(new InternalTableModel());
        this.setPreferredSize(new Dimension(700,350));
 // table.getColumn(table.getColumnName(1)).setCellRenderer(new ComboBoxRenderer());
        TableColumn column = null;
        for (int c = 0; c < internalTableModel.getColumnCount(); c++) {
            column = table.getColumnModel().getColumn(c);
            switch(c){
               
                case 0:     column.setPreferredWidth(150); break;
                case 1:    column.setPreferredWidth(150);  break;
                case 2:    column.setPreferredWidth(450);  break;
               
                
            }
        }
    
     
        this.interactions = new Vector<Interaction>();
        Vector<Interaction> tmp = configuration.getInteractions();

        if (tmp != null) {
            for (int i = 0; i < tmp.size(); i++) {

                if (tmp.get(i) != null) {
                    interactions.add(tmp.get(i));
                }
            }
        }
        
       
       
       
//         add(scrollPane);
//        list = new JList();
        table.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(table);
        scrollpane.setPreferredSize(new Dimension(400,200));
        btnAdd = new JButton("+");
        btnRemove = new JButton("-");
        btnEdit = new JButton("Einstellungen");
        btnAdd.addActionListener(this);
        btnRemove.addActionListener(this);
        btnEdit.addActionListener(this);
        JDUtilities.addToGridBag(panel, scrollpane, 0, 0, 3, 1, 1, 1, insets, GridBagConstraints.BOTH, GridBagConstraints.CENTER);

        JDUtilities.addToGridBag(panel, btnAdd, 0, 1, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnRemove, 1, 1, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, btnEdit, 2, 1, 1, 1, 0, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        // JDUtilities.addToGridBag(this, panel,0, 0, 1, 1, 1, 1, insets,
        // GridBagConstraints.BOTH, GridBagConstraints.WEST);
        add(panel, BorderLayout.CENTER);

    }

    private int getSelectedInteractionIndex() {
return table.getSelectedRow();
    }

    @Override
    public String getName() {

        return "Eventmanager";
    }

    private Interaction[] getInteractionArray() {
        Interaction[] interacts = new Interaction[] { new DummyInteraction(), new ExternExecute(), new ExternReconnect(), new HTTPReconnect(), new WebUpdate(),new JAntiCaptcha(),new ManuelCaptcha() };
        return interacts;
    }

    private void openPopupPanel(ConfigPanel config) {
        JPanel panel = new JPanel(new BorderLayout());

        InteractionTrigger[] triggers = InteractionTrigger.getAllTrigger();

        Interaction interaction = this.getSelectedInteraction();
        currentInteraction = interaction;
        if (interaction == null) return;
        InteractionTrigger trigger = interaction.getTrigger();

        int indexT = 0;
        int i;

        for (i = 0; i < triggers.length; i++) {
            if (triggers[i].getID() == trigger.getID()) {
                indexT = i;
                break;
            }
        }

        cboTrigger = new JComboBox(triggers);
        cboTrigger.addActionListener(this);
        cboTrigger.setSelectedIndex(indexT);
        JPanel topPanel = new JPanel();
        topPanel.add(new JLabel("Trigger Event"));
        topPanel.add(cboTrigger);
        panel.add(topPanel, BorderLayout.NORTH);
       if(config!=null) panel.add(config, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(new JFrame(), config, panel, uiinterface, configuration);
        pop.setLocation(JDUtilities.getCenterOfComponent(this, pop));
        pop.setVisible(true);
    }

    private Interaction getSelectedInteraction() {
        int index = getSelectedInteractionIndex();
        if (index < 0) return null;
        return interactions.elementAt(index);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            InteractionTrigger[] events = InteractionTrigger.getAllTrigger();
            InteractionTrigger event = (InteractionTrigger) JOptionPane.showInputDialog(this, "Trigger auswählen", "Wann soll eine Aktion ausgeführt werden?", JOptionPane.QUESTION_MESSAGE, null, events, null);
            if (event == null) return;
            
            
            Interaction[] interacts = getInteractionArray();
            Interaction interaction = (Interaction) JOptionPane.showInputDialog(this, "Aktion auswählen für \""+event.getName()+"\"", "Welche Aktion soll ausgeführt werden?", JOptionPane.QUESTION_MESSAGE, null, interacts, null);

            if (interaction == null) return;
           
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

        if (e.getSource() == this.cboTrigger) {
            Interaction interaction = currentInteraction;

            if (interaction == null) return;
           
            interaction.setTrigger((InteractionTrigger) cboTrigger.getSelectedItem());
            table.tableChanged(new TableModelEvent(table.getModel()));

        }
        if (e.getSource() == btnEdit) {
            editEntry();

        }

    }

    private void editEntry() {
        Interaction interaction = getSelectedInteraction();

        if (interaction != null) {

        
            if (interaction instanceof DummyInteraction) {
                openPopupPanel(new ConfigPanelInteractionDummy(configuration, uiinterface,(DummyInteraction)interaction));
                return;
            }
            else if (interaction instanceof ExternReconnect) {
                openPopupPanel(new ConfigPanelInteractionExternReconnect(configuration, uiinterface));
                return;
                
            }
            else if (interaction instanceof HTTPReconnect) {
                openPopupPanel(new ConfigPanelInteractionHTTPReconnect(configuration, uiinterface));
                return;
            }
            else if (interaction instanceof WebUpdate) {

            }
            else if (interaction instanceof ExternExecute) {
                openPopupPanel(new ConfigPanelInteractionExternCommand(configuration, uiinterface,(ExternExecute)interaction));
                return; 
            }
            
            openPopupPanel(null);
        }

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
    private class InternalTableModel extends AbstractTableModel{

      
   
        /**
         * 
         */
        private static final long serialVersionUID = 1155282457354673850L;
        public Class<?> getColumnClass(int columnIndex) {
            switch(columnIndex){
                case 0: return String.class;
                case 1: return String.class;
               
          
            }
            return String.class;
        }
        public int getColumnCount() {
            return 3;
        }
        public int getRowCount() {
            return interactions.size();
        }
        public Object getValueAt(int rowIndex, int columnIndex) {
           

           
            switch(columnIndex){
                case 0: return interactions.elementAt(rowIndex).getInteractionName();
                case 1: 
                    return interactions.elementAt(rowIndex).getTrigger().getName();
                case 2: 
                    return interactions.elementAt(rowIndex).getTrigger().getDescription();
                  
                    
                 
               
              
            }
            return null;
        }
        public String getColumnName(int column) {
            switch(column){
                case 0: return "Aktion";
                case 1: return "Trigger";
                case 2: return "Triggerbeschreibung";
           
            }
            return super.getColumnName(column);
        }
    }
  
    
}
