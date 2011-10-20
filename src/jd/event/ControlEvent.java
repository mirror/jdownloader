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

package jd.event;

import org.appwork.utils.event.DefaultIntEvent;

/**
 * Diese Klasse realisiert Ereignisse, die zum Steuern des Programmes dienen
 * 
 * @author astaldo
 */
public class ControlEvent extends DefaultIntEvent {

    private static final long serialVersionUID                 = 1639354503246054870L;

    /**
     * Ein PLugin wird beendet. Source: ist jeweils das PLugin Parameter:
     * Decrypter: decrypted Links Vector
     */
    public static final int   CONTROL_PLUGIN_INACTIVE          = 4;

    /**
     * Ein Plugin fängt an zu arbeiten. Source ist das PLugin selbst Parameter:
     * Decrypter: encryptedLinks
     */
    public static final int   CONTROL_PLUGIN_ACTIVE            = 5;

    /**
     * Wird aufgerufen sobald der Downloadvorgang komplett gestoppt ist
     */
    public static final int   CONTROL_DOWNLOAD_STOP            = 6;

    /**
     * Gibt an, dass der Downloadvorgang gestartet wurde
     */
    public static final int   CONTROL_DOWNLOAD_START           = 13;

    /**
     * wird verschickt wenn das Kontextmenü der Downloadlinks geöffnet wird
     * (oder package); soiu7rce: link/packlage parameter:menuitem arraylist
     */
    public static final int   CONTROL_LINKLIST_CONTEXT_MENU    = 22;

    /**
     * Gibt an dass ein plugin, eine INteraction etc. einen Forschritt gemacht
     * haben. Das entsprechende Event wird aus der ProgressController klasse
     * ausgelöst
     */
    public static final int   CONTROL_ON_PROGRESS              = 24;

    /**
     * Wird vom Controller vor dem beeenden des Programms aufgerufen
     */
    public static final int   CONTROL_SYSTEM_EXIT              = 26;

    public static final int   CONTROL_JDPROPERTY_CHANGED       = 27;

    public static final int   CONTROL_INIT_COMPLETE            = 30;

    /**
     * Wird verwendet wenn eine datei verarbeitet wurde.z.B. eine datei entpackt
     * wurde. Andere plugins und addons können dieses event abrufen und
     * entscheiden wie die files weiterverareitet werden sollen. Die files
     * werden als File[] parameter übergeben
     */
    public static final int   CONTROL_ON_FILEOUTPUT            = 33;

    /**
     * Sammelt über DataBox.java daten ein
     */
    public static final int   CONTROL_COLLECT_DATA             = 34;

    /**
     * prepareShutDown is complete
     */
    public static final int   CONTROL_SYSTEM_SHUTDOWN_PREPARED = 261;

    /**
     * Die ID des Ereignisses
     */
    private final int         controlID;

    /**
     * Ein optionaler Parameter
     */
    private final Object      parameter1;

    private final Object      parameter2;

    public ControlEvent(final Object source, final int controlID) {
        this(source, controlID, null);
    }

    public ControlEvent(final Object source, final int controlID, final Object parameter) {
        super(source, controlID);
        this.controlID = controlID;
        this.parameter1 = parameter;
        this.parameter2 = null;
    }

    public ControlEvent(final Object source, final int controlID, final Object parameter1, final Object parameter2) {
        super(source, controlID);
        this.controlID = controlID;
        this.parameter1 = parameter1;
        this.parameter2 = parameter2;
    }

    public Object getParameter() {
        return this.parameter1;
    }

    public Object getParameter2() {
        return this.parameter2;
    }

    @Override
    public String toString() {
        return "[source:" + this.getCaller() + ", controlID:" + this.controlID + ", parameter:" + this.parameter1 + "," + this.parameter2 + "]";
    }

    @Deprecated
    public int getID() {
        return getEventID();
    }
}
