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

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Level;

import jd.DecryptPluginWrapper;
import jd.PluginWrapper;
import jd.captcha.easy.load.LoadImage;
import jd.controlling.IOPermission;
import jd.controlling.JDLogger;
import jd.controlling.JDPluginLogger;
import jd.controlling.ProgressController;
import jd.controlling.captcha.CaptchaController;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawlerDistributer;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.nutils.Formatter;
import jd.nutils.encoding.Encoding;

import org.appwork.utils.Regex;
import org.jdownloader.translate._JDT;

/**
 * Dies ist die Oberklasse für alle Plugins, die Links entschlüsseln können
 * 
 * @author astaldo
 */
public abstract class PluginForDecrypt extends Plugin {

    private IOPermission           ioPermission = null;
    private LinkCrawlerDistributer distributer  = null;

    /**
     * @return the distributer
     */
    public LinkCrawlerDistributer getDistributer() {
        return distributer;
    }

    /**
     * @param distributer
     *            the distributer to set
     */
    public void setDistributer(LinkCrawlerDistributer distributer) {
        this.distributer = distributer;
    }

    /**
     * @return the ioPermission
     */
    public IOPermission getIOPermission() {
        return ioPermission;
    }

    /**
     * @param ioPermission
     *            the ioPermission to set
     */
    public void setIOPermission(IOPermission ioPermission) {
        this.ioPermission = ioPermission;
    }

    public PluginForDecrypt(PluginWrapper wrapper) {
        super(wrapper);
        logger = new JDPluginLogger(wrapper.getHost() + System.currentTimeMillis());

    }

    public void setBrowser(Browser br) {
        this.br = br;
    }

    @Override
    public DecryptPluginWrapper getWrapper() {
        return (DecryptPluginWrapper) super.getWrapper();
    }

    @Override
    public long getVersion() {
        return this.getWrapper().getVersion();
    }

    public void sleep(long i, CryptedLink link) throws InterruptedException {
        String before = link.getProgressController().getStatusText();
        while (i > 0) {
            i -= 1000;
            link.getProgressController().setStatusText(before + " " + _JDT._.gui_download_waittime_status2(Formatter.formatSeconds(i / 1000)));
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
    public ArrayList<DownloadLink> decryptLink(CrawledLink source) {
        ProgressController progress = null;
        int progressShow = 0;
        Color color = null;
        try {
            CryptedLink cryptLink = source.getCryptedLink();
            if (cryptLink == null) return null;
            progress = new ProgressController(_JDT._.jd_plugins_PluginForDecrypt_decrypting(getHost()), null);
            progress.setInitials(getInitials());
            cryptLink.setProgressController(progress);
            ArrayList<DownloadLink> tmpLinks = null;
            boolean showException = true;
            try {
                /*
                 * we now lets log into plugin specific loggers with all
                 * verbose/debug on
                 */
                br.setLogger(logger);
                br.setVerbose(true);
                br.setDebug(true);
                /* now we let the decrypter do its magic */
                tmpLinks = decryptIt(cryptLink, progress);
            } catch (DecrypterException e) {
                if (DecrypterException.CAPTCHA.equals(e.getErrorMessage())) {
                    showException = false;
                }
                /*
                 * we got a decrypter exception, clear log and note that
                 * something went wrong
                 */
                progress.setStatusText(this.getHost() + ": " + e.getErrorMessage());
                logger.clear();
                logger.log(Level.SEVERE, "DecrypterException", e);
                color = Color.RED;
                progressShow = 15000;
            } catch (InterruptedException e) {
                /* plugin got interrupted, clear log and note what happened */
                logger.clear();
                logger.log(Level.SEVERE, "Interrupted", e);
                progressShow = 0;
            } catch (Throwable e) {
                /*
                 * damn, something must have gone really really bad, lets keep
                 * the log
                 */
                progress.setStatusText(this.getHost() + ": " + e.getMessage());
                logger.log(Level.SEVERE, "Exception", e);
                color = Color.RED;
                progressShow = 15000;
            }
            if (tmpLinks == null && showException) {
                /*
                 * null as return value? something must have happened, do not
                 * clear log
                 */
                logger.severe("Decrypter out of date: " + this);
                logger.severe("Decrypter out of date: " + getVersion());
                progress.setStatusText(_JDT._.jd_plugins_PluginForDecrypt_error_outOfDate(this.getHost()));
                color = Color.RED;
                progressShow = 15000;

                /* lets forward the log */
                if (logger instanceof JDPluginLogger) {
                    /* make sure we use the right logger */
                    ((JDPluginLogger) logger).logInto(JDLogger.getLogger());
                }
            }
            return tmpLinks;
        } finally {
            try {
                if (progressShow > 0) {
                    if (color != null) {
                        progress.setColor(color);
                    }
                    progress.doFinalize(progressShow);
                } else {
                    progress.doFinalize();
                }
            } catch (Throwable e) {
            }
        }
    }

    /**
     * use this to process decrypted links while the decrypter itself is still
     * running
     * 
     * NOTE: if you use this, please put it in try{}catch(Throwable) as this
     * function is ONLY available in>09581
     * 
     * @param links
     */
    protected void distribute(DownloadLink... links) {
        LinkCrawlerDistributer dist = distributer;
        if (dist == null || links == null || links.length == 0) return;
        dist.distribute(links);
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
        try {
            Browser brc = br.cloneBrowser();
            try {
                brc.getDownload(captchaFile, captchaAddress);
            } catch (Exception e) {
                logger.severe("Captcha Download fehlgeschlagen: " + captchaAddress);
                throw new DecrypterException(DecrypterException.CAPTCHA);
            }
            // erst im Nachhinein das der Bilddownload nicht gestört wird
            String captchaCode = getCaptchaCode(method, captchaFile, param);
            return captchaCode;
        } finally {
            if (captchaFile != null) captchaFile.delete();
        }
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
        if (link.getProgressController() != null) link.getProgressController().setStatusText(_JDT._.gui_linkgrabber_waitinguserio2(method));
        String cc = null;
        try {
            cc = new CaptchaController(ioPermission, getHost(), method, file, defaultValue, explain).getCode(flag);
        } finally {
            if (link.getProgressController() != null) link.getProgressController().setStatusText(null);
        }
        if (cc == null) throw new DecrypterException(DecrypterException.CAPTCHA);
        return cc;
    }

    public ArrayList<CrawledLink> getCrawlableLinks(String data) {
        /*
         * we dont need memory optimization here as downloadlink, crypted link
         * itself take care of this
         */
        String[] hits = new Regex(data, getSupportedLinks()).setMemoryOptimized(false).getColumn(-1);
        ArrayList<CrawledLink> chits = null;
        if (hits != null && hits.length > 0) {
            chits = new ArrayList<CrawledLink>(hits.length);
        } else {
            chits = new ArrayList<CrawledLink>();
        }
        if (hits != null && hits.length > 0) {
            for (String hit : hits) {
                String file = hit;
                file = file.trim();
                /* cut of any unwanted chars */
                while (file.length() > 0 && file.charAt(0) == '"') {
                    file = file.substring(1);
                }
                while (file.length() > 0 && file.charAt(file.length() - 1) == '"') {
                    file = file.substring(0, file.length() - 1);
                }
                file = file.trim();

                CrawledLink cli;
                chits.add(cli = new CrawledLink(new CryptedLink(file)));
                cli.setdPlugin(this);
            }
        }
        return chits;
    }

    protected void setBrowserExclusive() {
        if (br == null) return;
        br.setCookiesExclusive(true);
        br.clearCookies(getHost());
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

}