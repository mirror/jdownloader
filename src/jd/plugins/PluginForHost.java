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

package jd.plugins;



import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Vector;

import jd.config.Configuration;
import jd.config.MenuItem;
import jd.parser.SimpleMatches;
import jd.plugins.download.DownloadInterface;
import jd.plugins.download.RAFDownload;
import jd.utils.JDUtilities;

/**
 * Dies ist die Oberklasse für alle Plugins, die von einem Anbieter Dateien
 * herunterladen können
 * 
 * @author astaldo
 */
public abstract class PluginForHost extends Plugin {
    private static final String CONFIGNAME = "pluginsForHost";
    private static final String AGB_CHECKED = "AGB_CHECKED";
    public static final String PARAM_MAX_RETRIES = "MAX_RETRIES";
    public static final String PARAM_MAX_ERROR_RETRIES = "MAX_ERROR_RETRIES";
    private static long END_OF_DOWNLOAD_LIMIT = 0;
    // public abstract URLConnection getURLConnection();

    private int retryCount = 0;
    private int retryOnErrorCount = 0;
    private int maxConnections = 50;
    private static int currentConnections = 0;
    protected DownloadInterface dl = null;

    /**
     * Stellt das Plugin in den Ausgangszustand zurück (variablen intialisieren
     * etc)
     */
    public abstract void reset();

    public ArrayList<MenuItem> createMenuitems() {
        return null;
    }

    public String getPluginNameExtension(DownloadLink link) {
        return "";
    }

    /**
     * Führt alle restevorgänge aus und bereitet das Plugin dadurch auf einen
     * Neustart vor. Sollte nicht überschrieben werden
     */
    public final void resetPlugin() {
        this.resetSteps();
        this.reset();
        // this.aborted = false;
    }
    
    public void resetPluginGlobals() {
      
        setEndOfDownloadLimit(0);
    }
    

  

    /**
     * Diese methode führt den Nächsten schritt aus. Der gerade ausgeführte
     * Schritt wir zurückgegeben
     * 
     * @param parameter
     *            Ein Übergabeparameter
     * @return der nächste Schritt oder null, falls alle abgearbeitet wurden
     */
    public PluginStep doNextStep(Object parameter) {

        nextStep(currentStep);

        if (currentStep == null) {
            logger.info(this + " Pluginende erreicht!");
            return null;
        }
        logger.finer("Current Step:  " + currentStep + "/" + steps);
        if (!this.isAGBChecked()) {
            currentStep.setStatus(PluginStep.STATUS_ERROR);
            logger.severe("AGB not signed : " + this.getPluginID());
            ((DownloadLink) parameter).setStatus(DownloadLink.STATUS_ERROR_AGB_NOT_SIGNED);
            return currentStep;
        }
        currentStep = doStep(currentStep, parameter);
        logger.finer("got/return step: " + currentStep + " Linkstatus: " + ((DownloadLink) parameter).getStatus());

        return currentStep;
    }

    public boolean isListOffline() {
        return true;
    }

    public boolean[] checkLinks(DownloadLink[] urls) {
        return null;

    }

    /**
     * Hier werden Treffer für Downloadlinks dieses Anbieters in diesem Text
     * gesucht. Gefundene Links werden dann in einem Vector zurückgeliefert
     * 
     * @param data
     *            Ein Text mit beliebig vielen Downloadlinks dieses Anbieters
     * @return Ein Vector mit den gefundenen Downloadlinks
     */
    public Vector<DownloadLink> getDownloadLinks(String data, FilePackage fp) {

        Vector<DownloadLink> links = null;

        Vector<String> hits = SimpleMatches.getMatches(data, getSupportedLinks());
        if (hits != null && hits.size() > 0) {
            links = new Vector<DownloadLink>();
            for (int i = 0; i < hits.size(); i++) {
                String file = hits.get(i);

                while (file.charAt(0) == '"')
                    file = file.substring(1);
                while (file.charAt(file.length() - 1) == '"')
                    file = file.substring(0, file.length() - 1);

                try {
                    // Zwecks Multidownload braucht jeder Link seine eigene
                    // Plugininstanz
                    PluginForHost plg = this.getClass().newInstance();

                    DownloadLink link = new DownloadLink(plg, file.substring(file.lastIndexOf("/") + 1, file.length()), getHost(), file, true);
                    links.add(link);
                    if (fp != null) link.setFilePackage(fp);
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return links;
    }

    /**
     * Holt Informationen zu einem Link. z.B. dateigröße, Dateiname,
     * verfügbarkeit etc.
     * 
     * @param parameter
     * @return true/false je nach dem ob die Datei noch online ist (verfügbar)
     */
    public abstract boolean getFileInformation(DownloadLink parameter);

    /**
     * Gibt einen String mit den Dateiinformationen zurück. Die Defaultfunktion
     * gibt nur den dateinamen zurück. Allerdings Sollte diese Funktion
     * überschrieben werden. So kann ein Plugin zusatzinfos zu seinen Links
     * anzeigen (Nach dem aufruf von getFileInformation()
     * 
     * @param parameter
     * @return
     */
    public String getFileInformationString(DownloadLink parameter) {
        return parameter.getName();
    }

    /**
     * Diese Funktion verarbeitet jeden Schritt des Plugins.
     * 
     * @param step
     * @param parameter
     * @return
     */
    public abstract PluginStep doStep(PluginStep step, DownloadLink parameter) throws Exception;

    public abstract int getMaxSimultanDownloadNum();

    public abstract String getAGBLink();

    public boolean isAGBChecked() {
        if (!this.getProperties().hasProperty(AGB_CHECKED)) {
            getProperties().setProperty(AGB_CHECKED, JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGBS_CHECKED_" + this.getPluginID(), false) || JDUtilities.getSubConfig(CONFIGNAME).getBooleanProperty("AGB_CHECKED_" + this.getHost(), false));
            getProperties().save();
        }
        return getProperties().getBooleanProperty(AGB_CHECKED, false);
    }

    public void setAGBChecked(boolean value) {
        getProperties().setProperty(AGB_CHECKED, value);
        getProperties().save();
    }

    // public void abort(){
    // super.abort();
    // if(this.getDownloadInstance()!=null){
    // this.getDownloadInstance().abort();
    // }
    // }
    private DownloadInterface getDownloadInstance() {
        // TODO Auto-generated method stub
        return this.dl;
    }

    public int getMaxRetries() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_RETRIES, 3);
    }

    public int getMaxRetriesOnError() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(PARAM_MAX_ERROR_RETRIES, 0);
    }

    /**
     * Delegiert den doStep Call mit einem Downloadlink als Parameter weiter an
     * die Plugins. Und fängt übrige Exceptions ab.
     * 
     * @param parameter
     *            Downloadlink
     */
    public PluginStep doStep(PluginStep step, Object parameter) {

        try {
            PluginStep ret = doStep(step, (DownloadLink) parameter);
            logger.finer("got/return step: " + step + " Linkstatus: " + ((DownloadLink) parameter).getStatus());
            return ret;
            // if(ret==null){
            // return step;
            // }else{
            // return ret;
            // }
        } catch (Exception e) {
            e.printStackTrace();
            step.setStatus(PluginStep.STATUS_ERROR);
            ((DownloadLink) parameter).setStatus(DownloadLink.STATUS_ERROR_PLUGIN_SPECIFIC);
            step.setParameter(e.getLocalizedMessage());
            logger.finer("got/return 2 step: " + step + " Linkstatus: " + ((DownloadLink) parameter).getStatus());

            return step;
        }
    }

    /**
     * Kann im Downloadschritt verwendet werden um einen einfachen Download
     * vorzubereiten
     * 
     * @param downloadLink
     * @param step
     * @param url
     * @param cookie
     * @param redirect
     * @return
     */
    protected boolean defaultDownloadStep(DownloadLink downloadLink, PluginStep step, String url, String cookie, boolean redirect) {
        try {
            requestInfo = HTTP.getRequestWithoutHtmlCode(new URL(url), cookie, null, redirect);

            int length = requestInfo.getConnection().getContentLength();
            downloadLink.setDownloadMax(length);
            logger.finer("Filename: " + getFileNameFormHeader(requestInfo.getConnection()));

            downloadLink.setName(getFileNameFormHeader(requestInfo.getConnection()));
            dl = new RAFDownload(this, downloadLink, requestInfo.getConnection());
            if (!dl.startDownload() && step.getStatus() != PluginStep.STATUS_ERROR && step.getStatus() != PluginStep.STATUS_TODO) {
                downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);

                step.setStatus(PluginStep.STATUS_ERROR);

            }
            return true;
        } catch (MalformedURLException e) {

            e.printStackTrace();
        } catch (IOException e) {

            e.printStackTrace();
        }

        step.setStatus(PluginStep.STATUS_ERROR);
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_UNKNOWN);
        return false;

    }

    /**
     * Wird nicht gebraucht muss aber implementiert werden.
     */
    @Override
    public String getLinkName() {

        return null;
    }

    /**
     * Gibt zurück wie lange nach einem erkanntem Bot gewartet werden muss. Bei
     * -1 wird ein reconnect durchgeführt
     * 
     * @return
     */
    public long getBotWaittime() {

        return -1;
    }

    public void clean() {
        this.requestInfo = null;
        this.request = null;
        this.dl = null;

        super.clean();
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
        this.maxConnections = maxConnections;
    }

    public int getCurrentConnections() {
        return currentConnections;
    }

    public synchronized void setCurrentConnections(int currentConnections) {
        this.currentConnections = currentConnections;
    }

    public int getChunksPerFile() {
        return JDUtilities.getSubConfig("DOWNLOAD").getIntegerProperty(Configuration.PARAM_DOWNLOAD_MAX_CHUNKS, 3);
    }

    public int getFreeConnections() {
        return Math.max(1, maxConnections - currentConnections);
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getRetryOnErrorCount() {
        return retryOnErrorCount;
    }

    public void setRetryOnErrorCount(int retryOnErrorcount) {
        this.retryOnErrorCount = retryOnErrorcount;
    }

    public static long getEndOfDownloadLimit() {
        return END_OF_DOWNLOAD_LIMIT;
    }

    public static void setEndOfDownloadLimit(long end_of_download_limit) {
        END_OF_DOWNLOAD_LIMIT = end_of_download_limit;
    }

    public static void setDownloadLimitTime(long downloadlimit) {
        END_OF_DOWNLOAD_LIMIT = System.currentTimeMillis() + downloadlimit;
    }

    public static long getRemainingWaittime() {
        return Math.max(0, END_OF_DOWNLOAD_LIMIT - System.currentTimeMillis());
    }

    public PluginStep handleDownloadLimit(PluginStep step, DownloadLink downloadLink) {
        long waitTime = getRemainingWaittime();
        logger.finer("wait (intern) " + waitTime + " minutes");
        downloadLink.setStatus(DownloadLink.STATUS_ERROR_DOWNLOAD_LIMIT);
        step.setStatus(PluginStep.STATUS_ERROR);
        logger.info(" Waittime(intern) set to " + step + " : " + waitTime);
        step.setParameter((long) waitTime);
        return step;
    }
}
