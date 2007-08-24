package jd.gui;

import java.util.Vector;
import java.util.logging.Logger;

import jd.Configuration;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;

public interface UIInterface {
    /**
     * Liefert alle DownloadLinks zurück
     * @return
     */
    public Vector<DownloadLink> getDownloadLinks();
    /**
     * Liefert die Konfiguraion zurück
     * @return
     */
    public Configuration getConfiguration();
    /**
     * Der Benutzer soll den Captcha Code eintippen
     * 
     * @param plugin Das Plugin, daß den Captcha Code benötigt
     * @param captchaAddress Die Adresse des Captchas
     * @return Der erkannte Text
     */
    public String getCaptchaCodeFromUser(Plugin plugin, String captchaAddress);
    /**
     * Setzt den Logger
     * 
     * @param logger Der neue Logger
     */
    public void setLogger       (Logger logger);
    /**
     * Legt die neue Konfiguration fest
     * 
     * @param configuration Die neue Konfiguration
     */
    public void setConfiguration(Configuration configuration);
    /**
     * Legt alle DownloadLinks fest
     * 
     * @param downloadLinks Alle DownloadLinks
     */
    public void setDownloadLinks(Vector<DownloadLink> downloadLinks);
    /**
     * Zeigt an, daß ein bestimmtes Plugin in/aktiv ist
     * 
     * @param plugin Das Plugin, daß in/aktiv ist
     * @param isActive Zeigt ob das Plugin inaktiv oder aktiv ist
     */
    public void setPluginActive(Plugin plugin, boolean isActive);
    /**
     * Zeigt, daß sich Daten eines Plugins geändert haben
     * 
     * @param plugin Das Plugin, dessen Daten sich geändert haben oder null
     */
    public void pluginDataChanged(Plugin plugin);
    /**
     * Leitet ein PluginEvent weiter
     * 
     * @param event Ein PluginEvent
     */
    public void pluginEvent(PluginEvent event);
    /**
     * Fügt einen UIListener hinzu
     * 
     * @param listener Ein neuer UIListener
     */
    public void addUIListener(UIListener listener);
    /**
     * Entfernt einen UIListener
     * 
     * @param listener UIListener, der entfernt werden soll
     */
    public void removeUIListener(UIListener listener);
    /**
     * Verteilt ein UIEvent an alle registrierten Listener
     * 
     * @param pluginEvent Das UIEvent, daß verteilt werden soll
     */
    public void fireUIEvent(UIEvent pluginEvent);

}
