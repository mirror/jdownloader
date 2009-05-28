//    jDownloader - Downloadmanager
//    Copyright (C) 2008  JD-Team support@jdownloader.org
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

import java.util.ArrayList;

import jd.event.ControlListener;
import jd.plugins.Account;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

/**
 * Interface für alle GUIs
 * 
 * @author astaldo
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
     * Fügt Links zum Linkgrabber hinzu
     * 
     * @param links
     */
    public void addLinksToGrabber(ArrayList<DownloadLink> links, boolean hideGrabber);

    public void displayMiniWarning(String shortWarn, String longWarn);

    /**
     * 
     * Minimiert die GUI. als ID können die GUI_STATUS_IDS aus UIInterface,*
     * verwendet werden
     */
    public void setFrameStatus(int id);

    /**
     * Zeigt einen MessageDialog an
     * 
     * @param string
     *            Nachricht, die angezeigt werden soll
     * @return Wahr, falls diese Anfrage bestätigt wurde, andernfalls falsch
     */
    public boolean showConfirmDialog(String string);

    public boolean showConfirmDialog(String string, String title);

    public String[] showLoginDialog(String title, String defaultUser, String defaultPassword, String errorMessage);

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
     * Dieser InputDialog hat einen Timeout
     * 
     * @param message
     * @param def
     * @return
     */
    public String showCountdownUserInputDialog(String message, String def);

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
