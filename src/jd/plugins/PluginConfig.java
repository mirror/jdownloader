package jd.plugins;

import java.util.Vector;

/**
 * Diese Klasse speichert die GUI-Dialog INformationen für PLugins
 * @TODO: Diese oder eine gleichartige Klasse sollte global verwendet werden um Core-elementen und PLugins die möglichkeit zu geben Config wünsche an die GUI zu geben
 * @author coalado
 *
 */
public  class PluginConfig {
    public static final int TYPE_SPINNER =8;
    public static final int TYPE_BROWSEFILE = 7;
    public static final int TYPE_SEPERATOR  = 6;
    public static final int TYPE_RADIOFIELD  = 5;
    public static final int TYPE_LABEL  = 4;
    public static final int TYPE_CHECKBOX  = 3;

    public static final int TYPE_BUTTON    = 2;

    public static final int TYPE_COMBOBOX  = 1;

    public static final int TYPE_TEXTFIELD = 0;
  
    @SuppressWarnings("unused")
    private Plugin plugin;
    private Vector<PluginConfigEntry> content=new Vector<PluginConfigEntry>();
  public PluginConfig(Plugin plugin){
      this.plugin=plugin;
  }
  public void addEntry(PluginConfigEntry entry){
      content.add(entry);
  }
  public PluginConfigEntry getEntryAt(int i){
      if(content.size()<=i)return null;
      return content.elementAt(i);
  }
  public Vector<PluginConfigEntry> getEntries(){
      return content;
  }
  

}