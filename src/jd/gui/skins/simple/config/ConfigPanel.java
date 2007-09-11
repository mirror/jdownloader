package jd.gui.skins.simple.config;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Logger;

import javax.swing.JPanel;

import jd.Configuration;
import jd.JDUtilities;
import jd.gui.UIInterface;
import jd.plugins.Plugin;

public abstract class ConfigPanel extends JPanel{
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID = 3383448498625377495L;
   private int row=0;
   private Vector<ConfigEntry> entries= new  Vector<ConfigEntry>();
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
    public void addConfigEntry(ConfigEntry entry){
     
        JDUtilities.addToGridBag(panel, entry, 0, row, 1, 1, 1, 0, insets, GridBagConstraints.BOTH, GridBagConstraints.EAST);
        entries.add(entry);
        row++;
    }
    public void saveConfigEntries(){
       Iterator<ConfigEntry> it = entries.iterator();
        
       while(it.hasNext()){
           ConfigEntry akt=it.next();
           if(akt.getPropertyInstance()!=null&&akt.getPropertyName()!=null)   
           akt.getPropertyInstance().setProperty(akt.getPropertyName(),akt.getText());
           
           
       }
    }
    public void loadConfigEntries(){
        Iterator<ConfigEntry> it = entries.iterator();
         
        while(it.hasNext()){
            ConfigEntry akt=it.next();
       
           if(akt.getPropertyInstance()!=null&&akt.getPropertyName()!=null)        
            akt.setData( akt.getPropertyInstance().getProperty(akt.getPropertyName()));
            
            
        }
     }
  
    
    public abstract void initPanel();
    public abstract void save();
    public abstract void load();
    public abstract String getName();
}
