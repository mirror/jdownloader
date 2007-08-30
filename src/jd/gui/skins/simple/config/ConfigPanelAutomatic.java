package jd.gui.skins.simple.config;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashMap;
import java.util.Vector;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.Configuration;
import jd.JDUtilities;
import jd.controlling.interaction.HTTPReconnect;
import jd.controlling.interaction.Interaction;

public class ConfigPanelAutomatic extends JPanel{

    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 7055395315659682282L;

    private Configuration configuration;
    private HashMap<Integer, Vector<Interaction>> interactions;
    private JPanel panel;
    private JComboBox cboDownloadFinished; 
    private JComboBox cboDownloadFinishedAll; 
    private JComboBox cboDownloadFailed; 
    
    public ConfigPanelAutomatic(Configuration configuration){
        this.configuration = configuration;
        this.interactions = configuration.getInteractions();
        this.panel = new JPanel(new GridBagLayout());
        
        JLabel lblDownloadFinished     = new JLabel("Einzelner Download fertiggestellt");
        JLabel lblDonwloadFinishedALL = new JLabel("Alle Downloads fertiggestellt");
        JLabel lblDownloadFailed       = new JLabel("Einzelner Download fehlgeschlagen");
        cboDownloadFinished    = new JComboBox();
        cboDownloadFinishedAll = new JComboBox();
        cboDownloadFailed      = new JComboBox();

        cboDownloadFinished.addItem("-");
        cboDownloadFinishedAll.addItem("-");
        cboDownloadFailed.addItem("-");

        load();
        
        Insets insets = new Insets(1,5,1,5);
        
        int row=0;
        JDUtilities.addToGridBag(panel, lblDownloadFinished,    0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDonwloadFinishedALL, 0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);
        JDUtilities.addToGridBag(panel, lblDownloadFailed,      0, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.EAST);

        row=0;
        JDUtilities.addToGridBag(panel, cboDownloadFinished,    1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDownloadFinishedAll, 1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        JDUtilities.addToGridBag(panel, cboDownloadFailed,      1, row++, 1, 1, 1, 1, insets, GridBagConstraints.NONE, GridBagConstraints.WEST);
        add(panel);
    }
    /**
     * Lädt alle Informationen 
     */
    void load(){
        Vector<Interaction> interaction;
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOAD_FINISHED);
        if(interaction!=null){
            cboDownloadFinished.addItem(interaction.firstElement());
            cboDownloadFinished.setSelectedItem(interaction.firstElement());
        }
        else{
            cboDownloadFinished.addItem(new HTTPReconnect());
        }
        
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL);
        if(interaction!=null){
            cboDownloadFinishedAll.addItem(interaction.firstElement());
            cboDownloadFinishedAll.setSelectedItem(interaction.firstElement());
        }
        else{
            cboDownloadFinishedAll.addItem(new HTTPReconnect());
        }
        
        interaction = interactions.get(Interaction.INTERACTION_DOWNLOAD_FAILED);
        if(interaction!=null){
            cboDownloadFailed.addItem(interaction.firstElement());
            cboDownloadFailed.setSelectedItem(interaction.firstElement());
        }
        else{
            cboDownloadFailed.addItem(new HTTPReconnect());
        }
    }
    
    /**
     * Speichert alle Änderungen auf der Maske
     */
    void save(){
        configuration.setInteractions(interactions);
        if(cboDownloadFinished.getSelectedIndex()>0){
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction)cboDownloadFinished.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOAD_FINISHED,i);
        }
        if(cboDownloadFinishedAll.getSelectedIndex()>0){
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction)cboDownloadFinishedAll.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOADS_FINISHED_ALL,i);
        }
        if(cboDownloadFailed.getSelectedIndex()>0){
            Vector<Interaction> i = new Vector<Interaction>();
            i.add((Interaction)cboDownloadFailed.getSelectedItem());
            interactions.put(Interaction.INTERACTION_DOWNLOAD_FAILED,i);
        }
    }
}
