package jd.gui;

import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import jd.Configuration;
import jd.event.ControlEvent;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.event.PluginEvent;

public interface UIInterface {
    /**
     * Liefert alle DownloadLinks zurück
     * @return Alle DownloadLinks
     */
    public Vector<DownloadLink> getDownloadLinks();
    /**
     * Liefert die Konfiguraion zurück
     * @return Die Konfiguration
     */
    public Configuration getConfiguration();
    /**
     * Der Benutzer soll den Captcha Code eintippen
     * 
     * @param plugin Das Plugin, daß den Captcha Code benötigt
     * @param captchaAddress Die Adresse des Captchas
     * @return Der erkannte Text
     */
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress);
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
     * Leitet ein PluginEvent weiter
     * 
     * @param event Ein PluginEvent
     */
    public void pluginEvent(PluginEvent event);
    /**
     * Leitet ein ControlEvent weiter
     * 
     * @param event ein ControlEvent
     */
    public void controlEvent(ControlEvent event);
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
