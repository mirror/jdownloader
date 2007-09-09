package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

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

    private JList               list;

    private JButton             btnAdd;

    private JButton             btnRemove;

    private JButton             btnEdit;

    private Interaction         currentInteraction;

    private JComboBox           cboTrigger;

    public ConfigPanelEventmanager(Configuration configuration, UIInterface uiinterface) {
        super(configuration, uiinterface);
        initPanel();
        
        load();
    
    }

    /**
     * Lädt alle Informationen
     */
    public void load() {
        setListData();

    }

    private void setListData() {
        DefaultListModel model = new DefaultListModel();

        for (int i = 0; i < interactions.size(); i++) {
            model.add(i, i + ". " + interactions.elementAt(i).getInteractionName() + " (" + interactions.elementAt(i).getTriggerName() + ")");
        }
        list.setModel(model);
    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    public void save() {
        // Interaction[] tmp= new Interaction[interactions.size()];

        configuration.setInteractions(interactions.toArray(new Interaction[] {}));

    }

    @Override
    public void initPanel() {

        this.interactions = new Vector<Interaction>();
        Interaction[] tmp = configuration.getInteractions();

        if (tmp != null) {
            for (int i = 0; i < tmp.length; i++) {

                if (tmp[i] != null) {
                    interactions.add(tmp[i]);
                }
            }
        }
        list = new JList();
        list.addMouseListener(this);
        JScrollPane scrollpane = new JScrollPane(list);

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
        return list.getSelectedIndex();
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
        panel.add(config, BorderLayout.CENTER);
        ConfigurationPopup pop = new ConfigurationPopup(new JFrame(), config, panel, uiinterface, configuration);
        pop.setVisible(true);
    }

    private Interaction getSelectedInteraction() {
        int index = getSelectedInteractionIndex();
        if (index < 0) return null;
        return interactions.elementAt(index);
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {

            Interaction[] interacts = getInteractionArray();
            Interaction interaction = (Interaction) JOptionPane.showInputDialog(this, "Aktion auswählen", "Aktion auswählen", JOptionPane.QUESTION_MESSAGE, null, interacts, null);

            if (interaction == null) return;
            InteractionTrigger[] events = InteractionTrigger.getAllTrigger();
            InteractionTrigger event = (InteractionTrigger) JOptionPane.showInputDialog(this, "Event auswählen", "Event auswählen", JOptionPane.QUESTION_MESSAGE, null, events, null);
            if (event == null) return;
            interaction.setTrigger(event);
            interactions.add(interaction);
            setListData();
        }
        if (e.getSource() == btnRemove) {
            int index = getSelectedInteractionIndex();
            if (index >= 0) {
                interactions.remove(index);
                setListData();
            }
        }

        if (e.getSource() == this.cboTrigger) {
            Interaction interaction = currentInteraction;

            if (interaction == null) return;
            logger.info(((InteractionTrigger) cboTrigger.getSelectedItem()).toString());
            interaction.setTrigger((InteractionTrigger) cboTrigger.getSelectedItem());
            setListData();

        }
        if (e.getSource() == btnEdit) {
            editEntry();

        }

    }

    private void editEntry() {
        Interaction interaction = getSelectedInteraction();

        if (interaction != null) {

            logger.info("Config " + interaction.getInteractionName());
            if (interaction instanceof DummyInteraction) {
                openPopupPanel(new ConfigPanelInteractionDummy(configuration, uiinterface,(DummyInteraction)interaction));
                
            }
            else if (interaction instanceof ExternReconnect) {
                openPopupPanel(new ConfigPanelInteractionExternReconnect(configuration, uiinterface));
                
            }
            else if (interaction instanceof HTTPReconnect) {
                openPopupPanel(new ConfigPanelInteractionHTTPReconnect(configuration, uiinterface));
            }
            else if (interaction instanceof WebUpdate) {

            }
            else if (interaction instanceof ExternExecute) {
                openPopupPanel(new ConfigPanelInteractionExternCommand(configuration, uiinterface,(ExternExecute)interaction));
                
            }
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
}
