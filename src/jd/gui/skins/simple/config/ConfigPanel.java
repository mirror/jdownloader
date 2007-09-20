package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jd.JDUtilities;
import jd.config.Configuration;
import jd.gui.UIInterface;
import jd.plugins.Plugin;

public abstract class ConfigPanel extends JPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
   private int row=0;
   private Vector<GUIConfigEntry> entries= new  Vector<GUIConfigEntry>();
    protected UIInterface uiinterface;
    protected JPanel panel;    
    protected Logger            logger           = Plugin.getLogger();
    protected Configuration configuration;
    protected  Insets insets = new Insets(1,5,1,5);
    ConfigPanel(Configuration configuration, UIInterface uiinterface){
        this.configuration = configuration;
        this.setLayout(new BorderLayout());
        panel = new JPanel(new GridBagLayout());
        this.uiinterface=uiinterface;      
       
    }
    public void addGUIConfigEntry(GUIConfigEntry entry){
     
        JDUtilities.addToGridBag(panel, entry, 0, row, 1, 1, 1, 0, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        entries.add(entry);
        row++;
    }
    public void saveConfigEntries(){
       Iterator<GUIConfigEntry> it = entries.iterator();
        
       while(it.hasNext()){
           GUIConfigEntry akt=it.next();
           if(akt.getConfigEntry().getPropertyInstance()!=null&&akt.getConfigEntry().getPropertyName()!=null)   
           akt.getConfigEntry().getPropertyInstance().setProperty(akt.getConfigEntry().getPropertyName(),akt.getText());
           
           
       }
    }
    public void loadConfigEntries(){
        Iterator<GUIConfigEntry> it = entries.iterator();
         
        while(it.hasNext()){
            GUIConfigEntry akt=it.next();
       
           if(akt.getConfigEntry().getPropertyInstance()!=null&&akt.getConfigEntry().getPropertyName()!=null)        
            akt.setData( akt.getConfigEntry().getPropertyInstance().getProperty(akt.getConfigEntry().getPropertyName()));
            
            
        }
     }
  
    
    public abstract void initPanel();
    public abstract void save();
    public abstract void load();
    public abstract String getName();
}
