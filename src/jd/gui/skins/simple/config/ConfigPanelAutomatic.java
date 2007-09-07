package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.interaction.DummyInteraction;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;
import jd.controlling.interaction.WebUpdate;

public class ConfigPanelAutomatic extends JPanel {

    /**
     * serialVersionUID
     */
    private static final long                     serialVersionUID = 7055395315659682282L;

    private Configuration                         configuration;
    private HashMap<Integer, Vector<Interaction>> interactions;
    private JPanel                                panel;
    private JComboBox                             cboDownloadFinished;
    private JComboBox                             cboDownloadFinishedAll;
    private JComboBox                             cboDownloadFailed;
    private JComboBox                             cboDownloadWaittime;
    private JComboBox                             cboDownloadBot;
    private JComboBox                             cboAppStart;
    public ConfigPanelAutomatic(Configuration configuration) {
        this.configuration = configuration;
        this.interactions = configuration.getInteractions();
        this.panel = new JPanel(new GridBagLayout());

        JLabel lblDownloadFinished = new JLabel("Einzelner Download fertiggestellt");
        JLabel lblDonwloadFinishedALL = new JLabel("Alle Downloads fertiggestellt");
        JLabel lblDownloadFailed = new JLabel("Einzelner Download fehlgeschlagen");
        JLabel lblDownloadWaittime = new JLabel("Einzelner Download hat Wartezeit");
        JLabel lblDownloadBot = new JLabel("Bot erkannt");
        JLabel lblAppStart = new JLabel("Programmstart");
       
        cboDownloadFinished = new JComboBox();
        cboDownloadFinishedAll = new JComboBox();
        cboDownloadFailed = new JComboBox();
        cboDownloadWaittime = new JComboBox();
        cboDownloadBot = new JComboBox();
        cboAppStart = new JComboBox();
        
        cboDownloadFinished.addItem("-");
        cboDownloadFinishedAll.addItem("-");
        cboDownloadFailed.addItem("-");
        cboDownloadWaittime.addItem("-");
        cboDownloadBot.addItem("-");
        cboAppStart.addItem("-");
        load();

        Insets insets = new Insets(1, 5, 1, 5);

        int row = 0;
        JDUtilities.addToGridBag(panel, lblDownloadFinished, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDonwloadFinishedALL, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDownloadFailed, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDownloadWaittime, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDownloadBot, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblAppStart, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);

        row = 0;
        JDUtilities.addToGridBag(panel, cboDownloadFinished, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDownloadFinishedAll, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDownloadFailed, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDownloadWaittime, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDownloadBot, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboAppStart, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);

        add(panel);
    }

    /**
     * Lädt alle Informationen
     */
    void load() {
        Vector<Interaction> interaction;
        Hashtable<String, Interaction> availableInteractions;

        availableInteractions = getAllInteractions();
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOAD_FINISHED);
        if (interaction != null) {
            availableInteractions.put(interaction.firstElement().getInteractionName(), interaction.firstElement());
            fillComboBox(cboDownloadFinished, availableInteractions, interaction.firstElement());

        } else {
            fillComboBox(cboDownloadFinished, availableInteractions, null);
        }
        availableInteractions = getAllInteractions();
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL);
        if (interaction != null) {
            availableInteractions.put(interaction.firstElement().getInteractionName(), interaction.firstElement());
            fillComboBox(cboDownloadFinishedAll, availableInteractions, interaction.firstElement());
        } else {
            fillComboBox(cboDownloadFinishedAll, availableInteractions, null);
        }
        availableInteractions = getAllInteractions();
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOAD_FAILED);
        if (interaction != null) {
            availableInteractions.put(interaction.firstElement().getInteractionName(), interaction.firstElement());
            fillComboBox(cboDownloadFailed, availableInteractions, interaction.firstElement());
        } else {
            fillComboBox(cboDownloadFailed, availableInteractions, null);
        }
        availableInteractions = getAllInteractions();
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOAD_WAITTIME);
        if (interaction != null) {
            availableInteractions.put(interaction.firstElement().getInteractionName(), interaction.firstElement());
            fillComboBox(cboDownloadWaittime, availableInteractions, interaction.firstElement());
        } else {
            fillComboBox(cboDownloadWaittime, availableInteractions, null);
        }
        
        availableInteractions = getAllInteractions();
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOAD_BOT_DETECTED);
        if (interaction != null) {
            availableInteractions.put(interaction.firstElement().getInteractionName(), interaction.firstElement());
            fillComboBox(cboDownloadBot, availableInteractions, interaction.firstElement());
        } else {
            fillComboBox(cboDownloadBot, availableInteractions, null);
        }
        
        availableInteractions = getAllInteractions();
        interaction = interactions.get(Interaction.INTERACTION_APPSTART);
        if (interaction != null) {
            availableInteractions.put(interaction.firstElement().getInteractionName(), interaction.firstElement());
            fillComboBox(cboAppStart, availableInteractions, interaction.firstElement());
        } else {
            fillComboBox(cboAppStart, availableInteractions, null);
        }


    }

    private void fillComboBox(JComboBox cbo, Hashtable<String, Interaction> availableInteractions, Interaction selected) {
        Enumeration<String> en = availableInteractions.keys();
        Interaction tmp;
        while (en.hasMoreElements()) {
            tmp = ((Interaction) availableInteractions.get(en.nextElement()));
            cbo.addItem(tmp);
        }

        if (selected != null)
            cbo.setSelectedItem(selected);

    }

    private Hashtable<String, Interaction> getAllInteractions() {
        Hashtable<String, Interaction> it = new Hashtable<String, Interaction>();
        Interaction tmp;

        // Hier müssen alle möglichen Interactions eingetragen werden
        it.put((tmp = new HTTPReconnect()).getInteractionName(), tmp);
        it.put((tmp = new WebUpdate()).getInteractionName(), tmp);
        it.put((tmp = new DummyInteraction()).getInteractionName(), tmp);
        return it;

    }

    /**
     * Speichert alle Änderungen auf der Maske
     */
    void save() {
        configuration.setInteractions(interactions);
        if (cboDownloadFinished.getSelectedIndex() > 0) {
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction) cboDownloadFinished.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOAD_FINISHED, i);
        } else
            interactions.remove(Interaction.INTERACTION_DOWNLOAD_FINISHED);
        if (cboDownloadFinishedAll.getSelectedIndex() > 0) {
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction) cboDownloadFinishedAll.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL, i);
        } else
            interactions.remove(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL);
        if (cboDownloadFailed.getSelectedIndex() > 0) {
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction) cboDownloadFailed.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOAD_FAILED, i);
        } else
            interactions.remove(Interaction.INTERACTION_DOWNLOAD_FAILED);

        if (cboDownloadWaittime.getSelectedIndex() > 0) {
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction) cboDownloadWaittime.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOAD_WAITTIME, i);
        } else
            interactions.remove(Interaction.INTERACTION_DOWNLOAD_WAITTIME);
        
        if (cboDownloadBot.getSelectedIndex() > 0) {
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction) cboDownloadBot.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOAD_BOT_DETECTED, i);
        } else
            interactions.remove(Interaction.INTERACTION_DOWNLOAD_BOT_DETECTED);

        
        if (cboAppStart.getSelectedIndex() > 0) {
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction) cboAppStart.getSelectedItem());
            interactions.put(Interaction.INTERACTION_APPSTART, i);
        } else
            interactions.remove(Interaction.INTERACTION_APPSTART);

    }
}
