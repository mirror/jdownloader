//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team jdownloader@freenet.de
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.

package jd.gui;

import java.io.File;
import java.util.Vector;

import jd.event.ControlListener;
import jd.event.UIEvent;
import jd.event.UIListener;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForHost;

/**
 * Interface für alle GUIS
 * 
 * @author astaldo
 * 
 */
public interface UIInterface extends ControlListener {
    /**
     * GUI normal, aber im Vordergrund anzeigen
     */
    public static int WINDOW_STATUS_FOREGROUND = 4;
    /**
     * GUI maximieren
     */
    public static int WINDOW_STATUS_MAXIMIZED = 1;
    /**
     * GUI minimieren
     */
    public static int WINDOW_STATUS_MINIMIZED = 0;
    /**
     * GUI normal anzeigen (Defaulteinstellungen)
     */
    public static int WINDOW_STATUS_NORMAL = 3;
    /**
     * GUI in den Tray legen
     */
    public static int WINDOW_STATUS_TRAYED = 2;

    /**
     * Fügt Links zum Linkgrabber hinzu
     * 
     * @param links
     */
    public void addLinksToGrabber(Vector<DownloadLink> links);

    // /**
    // * Legt alle DownloadLinks fest
    // *
    // * @param downloadLinks Alle DownloadLinks
    // */
    // public void setDownloadLinks(Vector<DownloadLink> downloadLinks);

    /**
     * Fügt einen UIListener hinzu
     * 
     * @param listener
     *            Ein neuer UIListener
     */
    public void addUIListener(UIListener listener);

    public void displayMiniWarning(String shortWarn, String longWarn, int showtime);

    /**
     * Verteilt ein UIEvent an alle registrierten Listener
     * 
     * @param pluginEvent
     *            Das UIEvent, daß verteilt werden soll
     */
    public void fireUIEvent(UIEvent pluginEvent);

    /**
     * Der Benutzer soll den Captcha Code eintippen
     * 
     * @param plugin
     *            Das Plugin, daß den Captcha Code benötigt
     * @param captchaAddress
     *            Die Adresse des Captchas
     * @return Der erkannte Text
     */
    public String getCaptchaCodeFromUser(Plugin plugin, File captchaAddress, String def);

    /**
     * Entfernt einen UIListener
     * 
     * @param listener
     *            UIListener, der entfernt werden soll
     */
    public void removeUIListener(UIListener listener);

    /**
     * Minimiert die GUI. als ID können die GUI_STATUS_IDS aus UIInterface,*
     * verwendet werden
     */
    public void setGUIStatus(int id);

    /**
     * Zeigt einen MessageDialog an
     * 
     * @param string
     *            Nachricht, die angezeigt werden soll
     * @return Wahr, falls diese Anfrage bestätigt wurde, andernfalls falsch
     */
    public boolean showConfirmDialog(String string);

    /**
     * Zeigt einen MessageDialog mit Countdown an
     * 
     * @param string
     * @param sec
     */
    public boolean showCountdownConfirmDialog(String string, int sec);

    /**
     * Zeigt einen HTML dialog mit Countdown an
     * 
     * @param string
     * @param message
     * @param toHTML
     * @param url
     * @param helpMsg
     * @param sec
     */
    public int showHelpMessage(String title, String message, boolean toHTML, String url, String helpMsg, int sec);

    /**
     * Zeigt einen HTML dialog an
     * 
     * @param string
     * @param htmlQuestion
     */
    public boolean showHTMLDialog(String title, String htmlQuestion);

    /**
     * Zeigt einen MessageDialog an
     * 
     * @param string
     */
    public void showMessageDialog(String string);

    /**
     * Zeigt einen Textarea dialog an
     * 
     * @param string
     * @param question
     * @param def
     */
    public String showTextAreaDialog(String title, String question, String def);

    /**
     * Zeigt einen Textfield dialog mit zwei Textfields und zwei Labels an
     * 
     * @param string
     * @param questionOne
     * @param questionTwo
     * @param defaultOne
     * @param defaultTwo
     */
    public String[] showTwoTextFieldDialog(String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo);

    /**
     * Zeigt einen Textarea dialog mit zwei Textareas und zwei Labels an
     * 
     * @param string
     * @param questionOne
     * @param questionTwo
     * @param defaultOne
     * @param defaultTwo
     */
    public String[] showTextAreaDialog(String title, String questionOne, String questionTwo, String defaultOne, String defaultTwo);

    /**
     * Zeigt einen Eingabe Dialog an
     * 
     * @param string
     *            Die Nachricht, die angezeigt werden soll
     * @return Der vom Benutzer eingegebene Text
     */
    public String showUserInputDialog(String string);

    /**
     * Zeigt einen Eingabe Dialog an
     * 
     * @param string
     *            Die Nachricht, die angezeigt werden soll
     * @param def
     *            default Wert
     * @return Der vom Benutzer eingegebene Text
     */
    public String showUserInputDialog(String string, String def);

    /**
     * Zeigt die AccountInformationen an
     * 
     * @param pluginForHost
     *            Das HostPlugin für den der Account gilt
     * @param account
     *            Der Account für den die Informationen geholt werden soll
     */
    public void showAccountInformation(PluginForHost pluginForHost, Account account);

}
