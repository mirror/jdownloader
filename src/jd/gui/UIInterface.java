//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program  is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSSee the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.


package jd.gui;

import java.io.File;
import java.util.Vector;

import jd.event.ControlEvent;
import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;


/**
 * INterface für alle GUIS
 * @author astaldo
 *
 */
public interface UIInterface extends ControlListener {
    /**
     * GUI minimieren
     */
  public static int WINDOW_STATUS_MINIMIZED=0;
  /**
   * GUI maximieren
   */
  public static int WINDOW_STATUS_MAXIMIZED=1;
  /**
   * GUI in den Tray legen
   */
  public static int WINDOW_STATUS_TRAYED=2;
  /**
   * GUI normal anzeigen (Defaulteinstellungen)
   */
public static int WINDOW_STATUS_NORMAL = 3;
/**
 * GUI normal, aber im Vordergrund anzeigen
 */
public static int WINDOW_STATUS_FOREGROUND = 4;
    /**
     * Der Benutzer soll den Captcha Code eintippen
     * 
     * @param plugin Das Plugin, daß den Captcha Code benötigt
     * @param captchaAddress Die Adresse des Captchas
     * @return Der erkannte Text
     */
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress,String def);
//    /**
//     * Legt alle DownloadLinks fest
//     * 
//     * @param downloadLinks Alle DownloadLinks
//     */
//    public void setDownloadLinks(Vector<DownloadLink> downloadLinks);
 

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
    /**
     * Zeigt einen MessageDialog an
     * @param string
     */
    public void showMessageDialog(String string);
    /**
     * Zeigt einen Eingabe Dialog an
     * @param string Die Nachricht, die angezeigt werden soll
     * @return Der vom Benutzer eingegebene Text
     */
    public String showUserInputDialog(String string);
    /**
     * Zeigt einen MessageDialog an
     * @param string Nachricht, die angezeigt werden soll
     * @return Wahr, falls diese Anfrage bestätigt wurde, andernfalls falsch
     */
    public boolean showConfirmDialog(String string);
    /**
     * Fügt Links zum Linkgrabber hinzu
     * @param links
     */
    public void addLinksToGrabber(Vector<DownloadLink> links);
    /**
     * Zeigt einen Textarea dialog an
     * @param string
     */
    public String showTextAreaDialog(String title,String question,String def);
    
    /**
     * Zeigt einen HTML dialog an
     * @param string
     */
    public boolean showHTMLDialog(String title,String htmlQuestion);
    
    /**
     * Zeigt einen HTML dialog an
     * @param string
     */
    public int showHelpMessage(String title,String message, String url);
    /**
     * Wird aufgeruifen sobald die Initialisierung aller Module komplett ist
     */
    public void onJDInitComplete();

/**
 * Minimiert die GUI. als ID können die GUI_STATUS_IDS aus UIInterface,* verwendet werden
 */
    public void setGUIStatus(int id);
 
}
