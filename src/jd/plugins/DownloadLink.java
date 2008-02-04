package jd.plugins;

import java.io.File;
import java.io.Serializable;
import java.util.Vector;
import java.util.logging.Logger;


import jd.config.Configuration;
import jd.controlling.SpeedMeter;
import jd.utils.JDUtilities;

/**
 * Hier werden alle notwendigen Informationen zu einem einzelnen Download
 * festgehalten. Die Informationen werden dann in einer Tabelle dargestellt
 * 
 * @author astaldo
 */
public class DownloadLink implements Serializable,Comparable<DownloadLink> {
    /**
     * Link muß noch bearbeitet werden
     */
    public final static int   STATUS_TODO                          = 0;
    /**
     * Link wurde erfolgreich heruntergeladen
     */
    public final static int   STATUS_DONE                          = 1;
    /**
     * Ein unbekannter Fehler ist aufgetreten
     */
    public final static int   STATUS_ERROR_UNKNOWN                 = 2;
    /**
     * Captcha Text war falsch
     */
    public final static int   STATUS_ERROR_CAPTCHA_WRONG           = 3;
    /**
     * Download Limit wurde erreicht
     */
    public final static int   STATUS_ERROR_DOWNLOAD_LIMIT          = 4;
    /**
     * Der Download ist gelöscht worden (Darf nicht verteilt werden)
     */
    public final static int   STATUS_ERROR_FILE_ABUSED             = 5;
    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int   STATUS_ERROR_FILE_NOT_FOUND          = 6;
    /**
     * Die Datei konnte nicht gefunden werden
     */
    public final static int   STATUS_ERROR_BOT_DETECTED            = 7;
    /**
     * Ein unbekannter Fehler ist aufgetreten. Der Download Soll wiederholt
     * werden
     */
    public final static int   STATUS_ERROR_UNKNOWN_RETRY           = 8;
    /**
     * Es gab Fehler mit dem captchabild (konnte nicht geladn werden)
     */

    public final static int   STATUS_ERROR_CAPTCHA_IMAGEERROR      = 9;
    /**
     * Zeigt an, dass der Download aus unbekannten Gründen warten muss. z.B.
     * weil Die Ip gerade gesperrt ist, oder eine Session id abgelaufen ist
     */
    public final static int   STATUS_ERROR_STATIC_WAITTIME         = 10;
  
    /**
     * zeigt einen Premiumspezifischen fehler an
     */
    public static final int   STATUS_ERROR_PREMIUM                 = 12;
    /**
     * Zeigt an dass der Link fertig geladen wurde
     */
    public static final int   STATUS_DOWNLOAD_FINISHED             = 13;
    /**
     * Zeigt an dass der Link nicht vollständig geladen wurde
     */
    public static final int   STATUS_DOWNLOAD_INCOMPLETE           = 14;
    /**
     * Zeigt an dass der Link gerade heruntergeladen wird
     */
    public static final int   STATUS_DOWNLOAD_IN_PROGRESS          = 15;
    /**
     * Der download ist zur Zeit nicht möglich
     */
    public static final int   STATUS_ERROR_TEMPORARILY_UNAVAILABLE = 16;
    /**
     * Der download ist zur Zeit nicht möglich. Die Auslastung der Server ist zu groß
     */
    public static final int   STATUS_ERROR_TO_MANY_USERS = 17;
    /**
     * das PLugin meldet einen Fehler. Der Fehlerstring kann via Parameter übergeben werden
     */
    public static final int   STATUS_ERROR_PLUGIN_SPECIFIC = 18;
    /**
     * zeigt an, dass nicht genügend Speicherplatz vorhanden ist
     */
    public static final int STATUS_ERROR_NO_FREE_SPACE = 19;
    /**
     * Die angefordete Datei wurde noch nicht fertig upgeloaded
     */
    public static final int STATUS_ERROR_FILE_NOT_UPLOADED = 20;

    
    /**
     * serialVersionUID
     */
    private FilePackage       filePackage;
    private static final long serialVersionUID                     = 1981079856214268373L;
    public static final int STATUS_ERROR_AGB_NOT_SIGNED = 21;
    public static final int STATUS_ERROR_SECURITY = 22;
    public static final int LINKTYPE_NORMAL = 0;
    public static final int LINKTYPE_CONTAINER = 1;
  
    /**
     * Statustext der von der GUI abgefragt werden kann
     */
    private transient boolean aborted                              = false;
    private transient String  statusText                           = "";
    /**
     * Beschreibung des Downloads
     */
    private String            name;
    /**
     * TODO downloadpath ueber config setzen
     */
    private String            downloadPath                         =  JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_DOWNLOAD_DIRECTORY);
    /**
     * Wird dieser Wert gesetzt, so wird der Download unter diesem Namen (nicht
     * Pfad) abgespeichert.
     */
    private String            staticFileName;
    /**
     * Von hier soll de Download stattfinden
     */
    private String            urlDownload;
    /**
     * Hoster des Downloads
     */
    private String            host;
    /**
     * Containername
     */
    private String            container;
    
    private boolean isMirror=false;
    /**
     * Dateiname des Containers
     */
    private String            containerFile;
    /**
     * Index dieses DownloadLinks innerhalb der Containerdatei
     */
    private int               containerIndex;
    /**
     * Zeigt an, ob dieser Downloadlink aktiviert ist
     */
    private boolean           isEnabled;
    /**
     * Zeigt, ob dieser DownloadLink grad heruntergeladen wird
     */
    private transient boolean inProgress                           = false;
    /**
     * Das Plugin, das für diesen Download zuständig ist
     */
    private transient Plugin plugin;
    /**
     * Falls vorhanden, das Plugin für den Container, aus der dieser Download geladen wurde
     */
    private transient PluginForContainer  pluginForContainer;
    /**
     * Maximum der heruntergeladenen Datei (Dateilänge)
     */
    private long              downloadMax;
    /**
     * Aktuell heruntergeladene Bytes der Datei
     */
    private long              downloadCurrent;
    /**
     * Hierhin soll die Datei gespeichert werden.
     */
    //private String            fileOutput;
    /**
     * Logger für Meldungen
     */
    private static Logger  logger                               = JDUtilities.getLogger();
    /**
     * Status des DownloadLinks
     */
    private int               status                               = STATUS_TODO;
    /**
     * Timestamp bis zu dem die Wartezeit läuft
     */
    private transient long    mustWaitTil                          = 0;
    /**
     * Ursprüngliche Wartezeit
     */
    private transient long    waittime                             = 0;
    /**
     * Lokaler Pfad zum letzten captchafile
     */
    private File                 latestCaptchaFile = null;
    /**
     * Speedmeter zum berechnen des downloadspeeds
     */
    private transient SpeedMeter speedMeter;
    private transient Boolean    available         = null;
   
    private Vector<String> sourcePluginPasswords=null;
    private String sourcePluginComment=null;
	private int maximalspeed = 209715;
    private int linkType=LINKTYPE_NORMAL;
    private transient String tempUrlDownload;
    /**
     * Erzeugt einen neuen DownloadLink
     * 
     * @param plugin Das Plugins, das für diesen Download zuständig ist
     * @param name Bezeichnung des Downloads
     * @param host Anbieter, von dem dieser Download gestartet wird
     * @param urlDownload Die Download URL (Entschlüsselt)
     * @param isEnabled Markiert diesen DownloadLink als aktiviert oder
     *            deaktiviert
     */
    public DownloadLink(Plugin plugin, String name, String host, String urlDownload, boolean isEnabled) {
        this.plugin = plugin;
        this.name = name;
        sourcePluginPasswords=new Vector<String>();
        
        downloadMax=0;
        this.host = host;
        this.isEnabled = isEnabled;
        speedMeter = new SpeedMeter();
        if(urlDownload!=null)
            this.urlDownload = urlDownload;
        else
            urlDownload=null;
        if(name==null)this.name=this.extractFileNameFromURL();
    }

    /**
     * Liefert den Datei Namen dieses Downloads zurück. Wurde der Name mit
     * setStaticFileName(String) festgelegt wird dieser Name zurückgegeben
     * 
     * @return Name des Downloads
     */
    public String getName() {
        if (this.getStaticFileName() == null) return name;
        return this.getStaticFileName();
    }

    /**
     * Gibt den Hoster dieses Links azurück.
     * 
     * @return Der Hoster, auf dem dieser Link verweist
     */
    public String getHost() {
        return host;
    }
    /**
     * Liefert das Plugin zurück, daß diesen DownloadLink handhabt
     * 
     * @return Das Plugin
     */
    public Plugin getPlugin() {
        return plugin;
    }
    public PluginForContainer getPluginForContainer(){
        return pluginForContainer;
    }

    /**
     * Liefert die Datei zurück, in die dieser Download gespeichert werden soll
     * 
     * @return Die Datei zum Abspeichern
     */
    public String getFileOutput() {
        if (getFilePackage() != null && getFilePackage().getDownloadDirectory() != null && getFilePackage().getDownloadDirectory().length() > 0) {
            return new File(new File(getFilePackage().getDownloadDirectory()), getName()).getAbsolutePath();
        }
        else {
            if(downloadPath!=null){
            return new File(new File(downloadPath), getName()).getAbsolutePath();
            }else{
                return this.getDownloadURL();
            }

        }

    }
    /**
     * Gibt zurück ob Dieser Link schon auf verfügbarkeit getestet wurde.+ Diese
     * FUnktion führt keinen!! Check durch. Sie prüft nur ob schon geprüft
     * worden ist. anschießend kann mit isAvailable() die verfügbarkeit
     * überprüft werden
     * 
     * @return Link wurde schon getestet (true) nicht getestet(false)
     */
    public boolean isAvailabilityChecked() {
        return available != null;

    }
    /**
     * Führt einen verfügbarkeitscheck durch. GIbt true zurück wenn der link
     * online ist
     * 
     * @return true/false
     */
    public boolean isAvailable() {
        if (this.available != null) {
            return available;
        }
        available = ((PluginForHost) getPlugin()).getFileInformation(this);
        return available;
    }


 

    /**
     * Liefert die URL zurück, unter der dieser Download stattfinden soll
     * 
     * @return Die Download URL
     */
    public String getDownloadURL() {
   
        if (linkType==LINKTYPE_CONTAINER){
            if(this.tempUrlDownload!=null)return tempUrlDownload;
            String ret;
            if(pluginForContainer!=null){
                ret=pluginForContainer.extractDownloadURL(this);
                if(ret==null){
                    logger.severe(this+" is a containerlink. Container could not be extracted. Is your JD Version up2date?");  
                }
                return ret;
            }
            else{
                logger.severe(this+" is a containerlink, but no plugin could be found");
                return null;
                
            }
               
        }
        return urlDownload;
    }

    /**
     * Liefert die bisher heruntergeladenen Bytes zurück
     * 
     * @return Anzahl der heruntergeladenen Bytes
     */
    public long getDownloadCurrent() {
        return downloadCurrent;
    }

    /**
     * Die Größe der Datei
     * 
     * @return Die Größe der Datei
     */
    public long getDownloadMax() {
        
        return downloadMax;
    }

    /**
     * Liefert den Status dieses Downloads zurück
     * 
     * @return Status des Downloads
     */
    public int getStatus() {
        return status;
    }

    /**
     * Zeigt, ob dieser Download grad in Bearbeitung ist
     * 
     * @return wahr, wenn diese Download grad heruntergeladen wird
     */
    public boolean isInProgress() {
        return inProgress;
    }

    /**
     * Zeigt, ob dieser Download aktiviert ist
     * 
     * @return wahr, falls dieser DownloadLink aktiviert ist
     */
    public boolean isEnabled() {
        return isEnabled;
    }

    /**
     * Setzt nachträglich das Plugin. Wird nur zum Laden der Liste benötigt
     * 
     * @param plugin Das für diesen Download zuständige Plugin
     */
    public void setLoadedPlugin(PluginForHost plugin) {
        this.plugin = plugin;
    }

    /**
     * Verändert den Aktiviert-Status
     * 
     * @param isEnabled Soll dieser DownloadLink aktiviert sein oder nicht
     */
    public void setEnabled(boolean isEnabled) {
        if (!isEnabled) {
            this.setAborted(true);
        }else{
            this.setAborted(false);
        }
        if(isEnabled == true){
            if(host != null && plugin==null){
                logger.severe("Es ist kein passendes HostPlugin geladen");
                return;
            }
            if(container!=null && pluginForContainer == null){
                logger.severe("Es ist kein passendes ContainerPlugin geladen");
                return;
            }
        }
        this.isEnabled = isEnabled;
    }




    /**
     * Setzt die URL, von der heruntergeladen werden soll 
     * 
     * @param urlDownload Die URL von der heruntergeladen werden soll
     */
    public void setUrlDownload(String urlDownload) {
        if(this.linkType==LINKTYPE_CONTAINER){
            this.tempUrlDownload=urlDownload;
            this.urlDownload=null;
            return;
        }
        this.urlDownload = urlDownload;
    }

    /**
     * Kennzeichnet den Download als in Bearbeitung oder nicht
     * 
     * @param inProgress wahr, wenn die Datei als in Bearbeitung gekennzeichnet
     *            werden soll
     */
    public void setInProgress(boolean inProgress) {
        this.inProgress = inProgress;
    }

    /**
     * Setzt den Status des Downloads
     * 
     * @param status Der neue Status des Downloads
     */
    public void setStatus(int status) {
        this.status = status;
        if(status!=STATUS_DOWNLOAD_IN_PROGRESS)
        	speedMeter=null; //wird von gc erfasst

    }

    /**
     * Setzt die Anzahl der heruntergeladenen Bytes fest und aktualisiert die
     * Fortschrittsanzeige
     * 
     * @param downloadedCurrent Anzahl der heruntergeladenen Bytes
     */
    public void setDownloadCurrent(long downloadedCurrent) {
        this.downloadCurrent = downloadedCurrent;
    }

    /**
     * Setzt die Größe der herunterzuladenden Datei, und aktualisiert die
     * Fortschrittsanzeige
     * 
     * @param downloadMax Die Größe der Datei
     */
    public void setDownloadMax(int downloadMax) {
        this.downloadMax = downloadMax;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DownloadLink && this.getDownloadURL()!=null && ((DownloadLink) obj).getDownloadURL() != null)
            return this.getDownloadURL().equals(((DownloadLink) obj).getDownloadURL());
        else
            return super.equals(obj);
    }

    /**
     * Setzt den Downloadpfad neu
     * 
     * @param downloadPath der neue downloadPfad
     */
    public void setDownloadPath(String downloadPath) {
        this.downloadPath = downloadPath;

    }

    /**
     * Setzt den Namen des Downloads neu
     * 
     * @param name Neuer Name des Downloads
     */
    public void setName(String name) {
        if(name!=null){
        this.name = name;
        }

    }

    /**
     * Setzt den Statustext der in der GUI angezeigt werden kann
     * 
     * @param text
     */
    public void setStatusText(String text) {
        statusText = text;
    }

    /**
     * Erstellt den Statustext, fügt eine eventl Wartezeit hzin und gibt diesen
     * Statusstrin (bevorzugt an die GUI) zurück
     * 
     * @return Statusstring mit eventl Wartezeit
     */

    public String getStatusText() {

        int speed;
        if (statusText == null){
            if (this.isAvailabilityChecked()) {
                return this.isAvailable()?"":"[OFFLINE]";
            }
            return "";
        }
        if (getRemainingWaittime() > 0) {
            return this.statusText + "Warten: (" + JDUtilities.formatSeconds((int) (getRemainingWaittime() / 1000)) + "sek)";
        }
        if (this.isInProgress() && (speed = getDownloadSpeed()) > 0) {
            long remainingBytes = this.getDownloadMax() - this.getDownloadCurrent();
            long eta = remainingBytes / speed;

            return "ETA " + JDUtilities.formatSeconds((int) eta) + " @ " + (speed / 1024) + "kb/s.";
        }

        if (!this.isEnabled()) {
            return "deaktiviert";
        }
        
        if (this.isAvailabilityChecked()) {
            return this.isAvailable()?statusText:"[OFFLINE] "+statusText;
        }
        return this.statusText;

    }

    /**
     * Setzt alle DownloadWErte zurück
     */
    public void reset() {

        downloadMax = 0;

        downloadCurrent = 0;
        aborted=false;
    }

    /**
     * Gibt nur den Dateinamen aus der URL extrahiert zurück. Um auf den
     * dateinamen zuzugreifen sollte bis auf Ausnamen immer
     * DownloadLink.getName() verwendet werden
     * 
     * @return Datename des Downloads.
     */
    public String extractFileNameFromURL() {
        int index = Math.max(this.getDownloadURL().lastIndexOf("/"), this.getDownloadURL().lastIndexOf("\\"));
        return this.getDownloadURL().substring(index + 1);
    }

    /**
     * Setzt die zeit in ms ab der die Wartezeit vorbei ist.
     * 
     * @param l
     */
    public void setEndOfWaittime(long l) {
        this.mustWaitTil = l;
        waittime = l - System.currentTimeMillis();
        if(waittime<=0){
            ((PluginForHost)this.getPlugin()).resetPlugin();
        }

    }

    /**
     * Gibt die wartezeit des Downloads zurück
     * 
     * @return Totale Wartezeit
     */
    public int getWaitTime() {
        return (int) waittime;
    }

    /**
     * Gibt die Verbleibende Wartezeit zurück
     * 
     * @return verbleibende wartezeit
     */
    public long getRemainingWaittime() {
        return Math.max(0, this.mustWaitTil - System.currentTimeMillis());
    }
    /**
     * Gibt zurück ob dieser download gerade auf einen reconnect wartet
     * @return true/False
     */
    public boolean waitsForReconnect(){
        return getRemainingWaittime()>0;
    }

    /**
     * Speichert das zuletzt geladene captchabild für diesen link
     * 
     * @param dest
     */
    public void setLatestCaptchaFile(File dest) {
        this.latestCaptchaFile = dest;

    }

    /**
     * Gibt den Pfad zum zuletzt gespeichertem Captchabild für diesen download
     * zurück
     * 
     * @return captcha pfad
     */
    public File getLatestCaptchaFile() {
        return latestCaptchaFile;
    }

    /**
     * Über diese funktion kann das plugin den link benachrichten dass neue
     * bytes geladen worden sind. dadurchw ird der interne speedmesser
     * aktualisiert
     * 
     * @param bytes
     */
    public void addBytes(long bytes, long difftime) {

        this.getSpeedMeter().addValue(bytes, difftime);

    }

    /**
     * Gibt den internen Speedmeter zurück
     * 
     * @return Speedmeter
     */
    public SpeedMeter getSpeedMeter() {
        if (speedMeter == null) {
            speedMeter = new SpeedMeter();
        }
        return speedMeter;
    }

    /**
     * Gibt die aktuelle Downloadgeschwindigkeit in bytes/sekunde zurück
     * 
     * @return Downloadgeschwindigkeit in bytes/sekunde
     */
    public int getDownloadSpeed() {
        if (getStatus() != DownloadLink.STATUS_DOWNLOAD_IN_PROGRESS) return 0;
        return getSpeedMeter().getSpeed();
    }

    /**
     * @return true falls der download abgebrochen wurde
     */
    public boolean isAborted() {
        return aborted;
    }

    /**
     * kann mit setAborted(true) den Download abbrechen
     * 
     * @param aborted
     */
    public void setAborted(boolean aborted) {
        this.aborted = aborted;
    }

    /**
     * Gibt das Filepacket des Links zurück. Kann auch null sein!! (Gui
     * abhängig)
     * 
     * @return Filepackage
     */
    public FilePackage getFilePackage() {
  
        return filePackage;
    }

    /**
     * Setzt das Filepackage für diesen download
     * 
     * @param filePackage
     */
    public void setFilePackage(FilePackage filePackage) {
        this.filePackage = filePackage;
    }

    /**
     * Diese methhode Frag das eigene Plugin welche Informationen über die File
     * bereit gestellt werden. Der String eignet Sich zur darstellung in der UI
     * 
     * @return STring
     */
    public String toString() {
        return this.getFileOutput();
    }

    /**
     * Gibt den Darstellbaren Dateinamen zurück. Dabei handelt es sich nicht
     * zwangsläufig um einen Valid-Filename. Dieser String eignet sich zur
     * darstellung des link und kann zusatzinformationen wie dateigröße oder
     * verfügbarkeit haben Diese Zusatzinformationen liefert das zugehörige
     * Plugin ACHTUNG: Weil der Dateiname kein zuverlässiger Dateiname sein muss
     * darf diese FUnktion nicht verwendet werden um eine datei zu benennen.
     * 
     * @return Erweiterter "Dateiname"
     */
    public String getFileInfomationString() {
        if (getPlugin() instanceof PluginForHost) {
            return ((PluginForHost) getPlugin()).getFileInformationString(this);
        }
        else {
            return getName();
        }
    }

    /**
     * setzt den Statischen Dateinamen. Ist dieser wert != null, sow ird er zum
     * Speichern der Datei verwendet. ist er ==null, so wird der dateiName im
     * Plugin automatisch ermittelt. ACHTUNG: Diese Funktion sollte nicht ! von
     * den Plugins verwendet werden. Sie dient dazu der Gui die Möglichkeit zu
     * geben unabhängig von den Plugins einen Downloadnamen festzulegen.
     * userinputs>automatische erkenung Plugins solten setName(String) verwenden
     * um den Speichernamen anzugeben.
     * 
     */
    public void setStaticFileName(String staticFileName) {
        this.staticFileName = staticFileName;
    }

    /**
     * Gibt den Statischen Downloadnamen zurück. Wird null zurückgegeben, so
     * wird der dateiname von den jeweiligen plugins automatisch ermittelt.
     * @return Statischer Dateiname
     */
    public String getStaticFileName() {
        return staticFileName;
    }
    public void setLoadedPluginForContainer(PluginForContainer pluginForContainer) {
        this.pluginForContainer = pluginForContainer;
        container = pluginForContainer.getHost();
    }
    public String getContainer() {
        return container;
    }
    public String getContainerFile() {
        return containerFile;
    }
    public void setContainerFile(String containerFile) {
        this.containerFile = containerFile;
    }
    public int getContainerIndex() {
        return containerIndex;
    }
    public void setContainerIndex(int containerIndex) {
        this.containerIndex = containerIndex;
    }

    public int compareTo(DownloadLink o) {
        return extractFileNameFromURL().compareTo(o.extractFileNameFromURL());
    }



public Vector<String> getSourcePluginPasswords() {
    return sourcePluginPasswords;
}

public DownloadLink setSourcePluginPasswords(Vector<String> sourcePluginPassword) {
    this.sourcePluginPasswords = sourcePluginPassword;
    return this;
}
public DownloadLink addSourcePluginPassword(String sourcePluginPassword) {
    
    if(this.sourcePluginPasswords.indexOf(sourcePluginPassword)<0 &&sourcePluginPassword!=null&&sourcePluginPassword.trim().length()>0){
    this.sourcePluginPasswords.add(sourcePluginPassword);
    }
    
    return this;
}
public String getSourcePluginPassword(){
    if(sourcePluginPasswords.size()==0)return null;
    if(sourcePluginPasswords.size()==1)return sourcePluginPasswords.get(0);
   String ret="{";
   for( int i=0; i<sourcePluginPasswords.size();i++){
       if(sourcePluginPasswords.get(i).trim().length()>0){
       ret+="\""+sourcePluginPasswords.get(i)+"\"";
           if(i<sourcePluginPasswords.size()-1){
               ret+=", ";
           }
       }
   }
   ret+="}";
   return ret;
}

public String getSourcePluginComment() {
    return sourcePluginComment;
}

public DownloadLink setSourcePluginComment(String sourcePluginComment) {
    this.sourcePluginComment = sourcePluginComment;
    return this;
}

public void addSourcePluginPasswords(Vector<String> sourcePluginPasswords) {
    for(int i=0; i<sourcePluginPasswords.size();i++){
       this.addSourcePluginPassword(sourcePluginPasswords.get(i));
    }
    
}

public boolean isMirror() {
    return isMirror;
}

public void setMirror(boolean isMirror) {
    this.isMirror = isMirror;
}

public int getMaximalspeed() {
	return maximalspeed;
}

public void setMaximalspeed(int maximalspeed) {
	int diff = this.maximalspeed-maximalspeed;
	if(diff>500 || diff<500)
	this.maximalspeed = maximalspeed/40;
}

public void setLinkType(int linktypeContainer) {
    if(linkType==LINKTYPE_CONTAINER){
        logger.severe("You are not allowd to Change the Linktype of "+this);
        return;
    }
    if(linktypeContainer==LINKTYPE_CONTAINER&&this.urlDownload!=null){
        logger.severe("This link already has a value for urlDownload");
        urlDownload=null;
    }
    this.linkType=linktypeContainer;
    
}
}
