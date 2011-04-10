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

package jd.plugins;


 import org.jdownloader.translate.*;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;

import jd.DecryptPluginWrapper;
import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.controlling.JDLogger;
import jd.controlling.JDPluginLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.controlling.captcha.CaptchaController;
import jd.event.ControlEvent;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;
import jd.nutils.jobber.JDRunnable;
import jd.nutils.jobber.Jobber;
import jd.utils.locale.JDL;

import org.appwork.utils.Regex;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {
    /**
     * timestamp when user last used the "do not show further captchas" option
     * after canceling a captcha dialog
     */
    private static long                  ALL_BLOCK;
    /**
     * Store the timestamps when user decided to choose the
     * "do not show further captchas of this host" after canceling a captcha
     */
    private static HashMap<String, Long> HOST_BLOCK_MAP = new HashMap<String, Long>();

    public PluginForDecrypt(PluginWrapper wrapper) {
        super(wrapper);
        logger = new JDPluginLogger(wrapper.getHost() + System.currentTimeMillis());

    }

    public void setBrowser(Browser br) {
        this.br = br;
    }

    private CryptedLink                                             curcryptedLink      = null;

    private static HashMap<Class<? extends PluginForDecrypt>, Long> LAST_STARTED_TIME   = new HashMap<Class<? extends PluginForDecrypt>, Long>();
    private static final String                                     JDL_PREFIX          = "jd.plugins.PluginForDecrypt.";
    private Long                                                    WAIT_BETWEEN_STARTS = 0L;

    public synchronized long getLastTimeStarted() {
        if (!LAST_STARTED_TIME.containsKey(this.getClass())) { return 0; }
        return Math.max(0, (LAST_STARTED_TIME.get(this.getClass())));
    }

    public synchronized void putLastTimeStarted(long time) {
        LAST_STARTED_TIME.put(this.getClass(), time);
    }

    public void setStartIntervall(long interval) {
        WAIT_BETWEEN_STARTS = interval;
    }

    @Override
    public DecryptPluginWrapper getWrapper() {
        return (DecryptPluginWrapper) super.getWrapper();
    }

    @Override
    public long getVersion() {
        return this.getWrapper().getVersion();
    }

    public boolean waitForNextStartAllowed(CryptedLink link) throws InterruptedException {
        String temp = link.getProgressController().getStatusText();
        long time = Math.max(0, WAIT_BETWEEN_STARTS - (System.currentTimeMillis() - getLastTimeStarted()));
        if (time > 0) {
            try {
                this.sleep(time, link);
            } catch (InterruptedException e) {
                link.getProgressController().setStatusText(temp);
                throw e;
            }
            link.getProgressController().setStatusText(temp);
            return true;
        } else {
            link.getProgressController().setStatusText(temp);
            return false;
        }
    }

    public void sleep(long i, CryptedLink link) throws InterruptedException {
        String before = link.getProgressController().getStatusText();
        while (i > 0) {
            i -= 1000;
            link.getProgressController().setStatusText(before + " " + JDT._.gui_download_waittime_status2( Formatter.formatSeconds(i / 1000)));
            Thread.sleep(1000);
        }
        link.getProgressController().setStatusText(before);
    }

    /**
     * Diese Methode entschlüsselt Links.
     * 
     * @param cryptedLinks
     *            Ein Vector, mit jeweils einem verschlüsseltem Link. Die
     *            einzelnen verschlüsselten Links werden aufgrund des Patterns
     *            {@link jd.plugins.Plugin#getSupportedLinks()
     *            getSupportedLinks()} herausgefiltert
     * @return Ein Vector mit Klartext-links
     */

    protected DownloadLink createDownloadlink(String link) {
        return new DownloadLink(null, null, getHost(), Encoding.urlDecode(link, true), true);
    }

    @Override
    public final ArrayList<MenuAction> createMenuitems() {
        return null;
    }

    /**
     * Die Methode entschlüsselt einen einzelnen Link.
     */
    public abstract ArrayList<DownloadLink> decryptIt(CryptedLink parameter, ProgressController progress) throws Exception;

    /**
     * Die Methode entschlüsselt einen einzelnen Link. Alle steps werden
     * durchlaufen. Der letzte step muss als parameter einen Vector<String> mit
     * den decoded Links setzen
     * 
     * @param cryptedLink
     *            Ein einzelner verschlüsselter Link
     * 
     * @return Ein Vector mit Klartext-links
     */
    public ArrayList<DownloadLink> decryptLink(CryptedLink cryptedLink) {
        curcryptedLink = cryptedLink;
        ProgressController progress = new ProgressController(JDT._.jd_plugins_PluginForDecrypt_decrypting( getHost(), getLinkName()), null);
        progress.setInitials(getInitials());
        curcryptedLink.setProgressController(progress);
        try {
            while (waitForNextStartAllowed(curcryptedLink)) {
            }
        } catch (InterruptedException e) {
            return new ArrayList<DownloadLink>();
        }
        putLastTimeStarted(System.currentTimeMillis());
        ArrayList<DownloadLink> tmpLinks = null;
        try {
            tmpLinks = decryptIt(curcryptedLink, progress);
        } catch (SocketTimeoutException e2) {
            progress.setStatusText(JDT._.jd_plugins_PluginForDecrypt_error_server());
            progress.setColor(Color.RED);
            progress.doFinalize(15000l);
            return new ArrayList<DownloadLink>();
        } catch (UnknownHostException e) {
            progress.setStatusText(JDT._.jd_plugins_PluginForDecrypt_error_connection());
            progress.setColor(Color.RED);
            progress.doFinalize(15000l);
            return new ArrayList<DownloadLink>();
        } catch (DecrypterException e) {
            e.printStackTrace();
            tmpLinks = new ArrayList<DownloadLink>();
            progress.setStatusText(this.getHost() + ": " + e.getErrorMessage());
            progress.setColor(Color.RED);

            progress.doFinalize(15000l);
        } catch (InterruptedException e2) {
            tmpLinks = new ArrayList<DownloadLink>();
        } catch (Throwable e) {
            progress.doFinalize();
            JDLogger.exception(e);
        }
        if (tmpLinks == null) {
            logger.severe("Decrypter out of date: " + this);
            logger.severe("Decrypter out of date: " + getVersion());
            progress.setStatusText(JDT._.jd_plugins_PluginForDecrypt_error_outOfDate( this.getHost()));

            progress.setColor(Color.RED);
            progress.doFinalize(15000l);
            return new ArrayList<DownloadLink>();
        }

        if (tmpLinks.size() == 0) {
            progress.doFinalize();
            return new ArrayList<DownloadLink>();
        }

        progress.doFinalize();
        return tmpLinks;
    }

    protected String getCaptchaCode(String captchaAddress, CryptedLink param) throws IOException, DecrypterException {
        return getCaptchaCode(getHost(), captchaAddress, param);
    }

    protected String getCaptchaCode(LoadImage li, CryptedLink param) throws IOException, DecrypterException {
        return getCaptchaCode(getHost(), li.file, param);
    }

    protected String getCaptchaCode(String method, String captchaAddress, CryptedLink param) throws IOException, DecrypterException {
        if (captchaAddress == null) {
            logger.severe("Captcha Adresse nicht definiert");
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        File captchaFile = this.getLocalCaptchaFile();
        Browser brc = br.cloneBrowser();
        try {
            brc.getDownload(captchaFile, captchaAddress);
        } catch (Exception e) {
            logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
            throw new DecrypterException(DecrypterException.CAPTCHA);
        }
        // erst im Nachhinein das der Bilddownload nicht gestört wird

        String captchaCode = getCaptchaCode(method, captchaFile, param);
        captchaFile.delete();
        return captchaCode;
    }

    protected String getCaptchaCode(File captchaFile, CryptedLink param) throws DecrypterException {
        return getCaptchaCode(getHost(), captchaFile, param);
    }

    protected String getCaptchaCode(String methodname, File captchaFile, CryptedLink param) throws DecrypterException {
        return getCaptchaCode(methodname, captchaFile, 0, param, null, null);
    }

    /**
     * 
     * @param method
     *            Method name (name of the captcha method)
     * @param file
     *            (imagefile)
     * @param flag
     *            (Flag of UserIO.FLAGS
     * @param link
     *            (CryptedlinkO)
     * @param defaultValue
     *            (suggest this code)
     * @param explain
     *            (Special captcha? needs explaination? then use this parameter)
     * @return
     * @throws DecrypterException
     */
    protected String getCaptchaCode(String method, File file, int flag, CryptedLink link, String defaultValue, String explain) throws DecrypterException {
        if (link.getProgressController() != null) link.getProgressController().setStatusText(JDT._.gui_linkgrabber_waitinguserio2( method));
        String cc = new CaptchaController(this.getInitTime(), getHost(), method, file, defaultValue, explain).getCode(flag);
        if (link.getProgressController() != null) link.getProgressController().setStatusText(null);
        if (cc == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        return cc;
    }

    public ArrayList<DownloadLink> decryptLinks(CryptedLink[] cryptedLinks) {
        if (isAborted(this.getInitTime(), this.getHost())) return new ArrayList<DownloadLink>();
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_ACTIVE, cryptedLinks);
        final ArrayList<DownloadLink> decryptedLinks = new ArrayList<DownloadLink>();
        Jobber decryptJobbers = new Jobber(4);

        class DThread extends Thread implements JDRunnable {
            private CryptedLink      decryptableLink = null;
            private PluginForDecrypt plg             = null;

            public DThread(CryptedLink decryptableLink, PluginForDecrypt plg) {

                this.decryptableLink = decryptableLink;
                this.plg = plg.getWrapper().getNewPluginInstance();
                // DThreads inittime should be derived from master plugin
                this.plg.setInitTime(plg.getInitTime());
                this.plg.setBrowser(new Browser());
            }

            @Override
            public void run() {
                if (isAborted(getInitTime(), getHost())) return;
                if (LinkGrabberController.isFiltered(decryptableLink)) return;
                ArrayList<DownloadLink> links = plg.decryptLink(decryptableLink);
                for (DownloadLink link : links) {
                    link.setBrowserUrl(decryptableLink.getCryptedUrl());
                }
                synchronized (decryptedLinks) {
                    decryptedLinks.addAll(links);
                }
            }

            public void go() throws Exception {
                run();
            }
        }

        for (int b = cryptedLinks.length - 1; b >= 0; b--) {
            if (isAborted(this.getInitTime(), this.getHost())) return new ArrayList<DownloadLink>();
            DThread dthread = new DThread(cryptedLinks[b], getWrapper().getPlugin());
            decryptJobbers.add(dthread);
        }
        int todo = decryptJobbers.getJobsAdded();
        decryptJobbers.start();
        while (decryptJobbers.getJobsFinished() != todo) {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
            }
        }
        decryptJobbers.stop();
        fireControlEvent(ControlEvent.CONTROL_PLUGIN_INACTIVE, decryptedLinks);

        return decryptedLinks;
    }

    /**
     * Sucht in data nach allen passenden links und gibt diese als vektor zurück
     * 
     * @param data
     * @return
     */
    public CryptedLink[] getDecryptableLinks(String data) {
        String[] hits = new Regex(data, getSupportedLinks()).getColumn(-1);
        ArrayList<CryptedLink> chits = new ArrayList<CryptedLink>();
        if (hits != null && hits.length > 0) {

            for (int i = hits.length - 1; i >= 0; i--) {
                String file = hits[i];
                file = file.trim();

                while (file.length() > 0 && file.charAt(0) == '"') {
                    file = file.substring(1);
                }

                while (file.length() > 0 && file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                hits[i] = file;

            }

            for (String hit : hits) {
                chits.add(new CryptedLink(hit));
            }
        }
        return chits.toArray((new CryptedLink[chits.size()]));
    }

    protected void setBrowserExclusive() {
        if (br == null) return;
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
    }

    /**
     * Gibt den namen des internen CryptedLinks zurück
     * 
     * @return encryptedLink
     */
    public String getLinkName() {
        if (curcryptedLink == null) return "";
        try {
            return new URL(curcryptedLink.toString()).toURI().getPath();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * Should be overwridden to specify special initials for the decryption
     * process.
     * 
     * @return a String with a length of 2
     */
    protected String getInitials() {
        return null;
    }

    /**
     * aborts all decrypters for host which have been started until now
     * 
     * @param host
     */
    public static void abortQueuedByHost(String host) {
        synchronized (HOST_BLOCK_MAP) {
            HOST_BLOCK_MAP.put(host, System.currentTimeMillis());
        }

    }

    /**
     * Aborts all decrypter which have been initiated until now
     */
    public static void abortQueued() {
        synchronized (HOST_BLOCK_MAP) {
            ALL_BLOCK = System.currentTimeMillis();
        }
    }

    /**
     * checks if host decrypter has been aborted before initTime
     * 
     * @param initTime
     * @param host
     * @return
     */
    public static boolean isAborted(long initTime, String host) {
        synchronized (HOST_BLOCK_MAP) {
            // block dialog, because the rquest is older than the block
            if (initTime < ALL_BLOCK) return true;
            // bolc host request, becasue the requesting plugin is older than
            // the
            // block
            Long hostBlockTime = HOST_BLOCK_MAP.get(host);

            if (hostBlockTime != null && initTime < hostBlockTime) return true;
            return false;
        }
    }

}