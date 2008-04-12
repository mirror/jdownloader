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

package jd.event;

import java.awt.AWTEvent;

/**
 * Diese Klasse realisiert Ereignisse, die zum Steuern des Programmes dienen
 * 
 * @author astaldo
 */
public class ControlEvent extends AWTEvent {
    /**
     * serialVersionUID
     */
    private static final long serialVersionUID                                    = 1639354503246054870L;

    /**
     * Alle Downloads wurden bearbeitet. Und der download wird angehalten
     */
    public final static int   CONTROL_ALL_DOWNLOADS_FINISHED                      = 1;

    /**
     * Die Daten von einem oder mehreren Downloadlinks wurden verändert. Dazu zählen auch Statusids,Fortschritt usw..
     */
    public final static int   CONTROL_DOWNLOADLINK_DATA_CHANGED                     = 2;

    /**
     * Das Verteilen des Inhalts der Zwischenablage ist abgeschlossen Als
     * Parameter wird hier ein Vector mit DownloadLinks übergeben, die
     * herausgearbeitet wurden
     */
    public final static int   CONTROL_DISTRIBUTE_FINISHED                         = 3;
    /**
     * Ein PLugin wird beendet. 
     * Source: ist jeweils das PLugin
     * Parameter: Decrypter: decrypted Links Vector
     * 
     */
  
    public final static int   CONTROL_PLUGIN_INACTIVE                     = 4;

    /**
     * Ein Plugin fängt an zu arbeiten.
     * Source ist das PLugin selbst
     * Parameter: Decrypter: encryptedLinks
     */
    public final static int   CONTROL_PLUGIN_ACTIVE                       = 5;
    
    /**
     * Wird aufgerufen sobald der Downloadvorgang komplett gestoppt ist
     */
    public static final int CONTROL_DOWNLOAD_STOP = 6;
    
    /**
     * Es wird eine ArrayList mit den veränderten DownloadLinks als parameter erwartet
     */
    public final static int   CONTROL_DOWNLOADLINKS_CHANGED                    = 7;
    /**
     * Gibt an dass ein captcha geladen wurde. der Fiel-Pfad zum captcha wir als
     * parameter erwartet. Source ist das ausführende Plugin
     */
    public static final int   CONTROL_CAPTCHA_LOADED                              = 12;

    /**
     * Gibt an, dass der Downloadvorgang gestartet wurde
     */
    public static final int   CONTROL_DOWNLOAD_START                          = 13;



    /**
     * Gibt an dass das Abbrechen der Downloads eingeleitet wurde
     */
    public static final int   CONTROL_DOWNLOAD_TERMINATION_ACTIVE                 = 17;

    /**
     * Gibt an dass der Download nun komplett abgebrochen wurde.
     */
    public static final int   CONTROL_DOWNLOAD_TERMINATION_INACTIVE               = 18;
    
    







  



    /**
     * Gibt an dass ein plugin, eine INteraction etc. einen Forschritt gemacht
     * haben. Das entsprechende Event wird aus der ProgressController klasse
     * ausgelöst
     */
    public static final int   CONTROL_ON_PROGRESS                                 = 24;
/**
 * Wird bei Strukturänderungen der Linkliste aufgerufen
 */
    public static final int   CONTROL_LINKLIST_STRUCTURE_CHANGED                            = 25;

    /**
     * Wird vom Controller vor dem beeenden des Programms aufgerufen
     */
    public static final int CONTROL_SYSTEM_EXIT = 26;



    

    /**
     * Die ID des Ereignisses
     */
    private int               controlID;

    /**
     * Ein optionaler Parameter
     */
    private Object            parameter;

    public ControlEvent(Object source, int controlID) {
        this(source, controlID, null);
    }

    public ControlEvent(Object source, int controlID, Object parameter) {
        super(source, controlID);
        this.controlID = controlID;
        this.parameter = parameter;
    }

    public int getID() {
        return controlID;
    }

    public Object getParameter() {
        return parameter;
    }
}
