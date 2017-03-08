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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jd.controlling.linkcollector.LinkOriginDetails;
import jd.controlling.linkcrawler.CrawledLink;
import jd.nutils.Formatter;

import org.appwork.storage.config.JsonConfig;
import org.appwork.uio.CloseReason;
import org.appwork.uio.ConfirmDialogInterface;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.Files;
import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.controlling.FileCreationManager;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.logging.LogController;
import org.jdownloader.settings.GeneralSettings;
import org.jdownloader.settings.GeneralSettings.DeleteContainerAction;
import org.jdownloader.translate._JDT;

/**
 * Dies ist die Oberklasse für alle Plugins, die Containerdateien nutzen können
 *
 * @author astaldo/JD-Team
 */

public abstract class PluginsC {

    private final Pattern  pattern;

    private final String   name;

    private final long     version;

    protected LogInterface logger = LogController.TRASH;

    public LogInterface getLogger() {
        return logger;
    }

    public void setLogger(LogInterface logger) {
        if (logger == null) {
            logger = LogController.TRASH;
        }
        this.logger = logger;
    }

    public PluginsC(String name, String pattern, String rev) {
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.matcher = this.pattern.matcher("");
        this.name = name;
        long version = -1;
        try {
            version = Formatter.getRevision(rev);
        } catch (Throwable e) {
            logger.log(e);
            version = -1;
        }
        this.version = version;
    }

    public abstract PluginsC newPluginInstance();

    protected ArrayList<CrawledLink> cls             = new ArrayList<CrawledLink>();

    protected String                 md5;
    protected byte[]                 k;

    private boolean                  askFileDeletion = true;
    private final Matcher            matcher;

    public abstract ContainerStatus callDecryption(File file) throws Exception;

    // @Override
    public synchronized boolean canHandle(final String data) {
        if (data != null) {
            synchronized (matcher) {
                try {
                    return matcher.reset(data).find();
                } finally {
                    matcher.reset("");
                }
            }
        }
        return false;
    }

    public String createContainerString(ArrayList<DownloadLink> downloadLinks) {
        return null;
    }

    public Pattern getSupportedLinks() {
        return pattern;
    }

    public String getName() {
        return name;
    }

    public long getVersion() {
        return version;
    }

    /* hide links by default */
    public boolean hideLinks() {
        return true;
    }

    public abstract String[] encrypt(String plain);

    /**
     * Liefert alle in der Containerdatei enthaltenen Dateien als DownloadLinks zurück.
     *
     * @param filename
     *            Die Containerdatei
     * @return Ein ArrayList mit DownloadLinks
     */
    public ArrayList<CrawledLink> getContainedDownloadlinks() {
        return cls == null ? new ArrayList<CrawledLink>() : cls;
    }

    protected boolean askFileDeletion() {
        return askFileDeletion;
    }

    public synchronized void initContainer(final CrawledLink source, final File file, final byte[] key) throws IOException {
        if (cls == null || cls.size() == 0) {
            logger.info("Init Container");
            if (key != null) {
                k = key;
            }
            try {
                final ContainerStatus cs = callDecryption(file);
                if (cs != null && cs.isStatus(ContainerStatus.STATUS_FINISHED) && isDeleteContainer(source, file)) {
                    deleteContainer(source, file);
                }
            } catch (Throwable e) {
                logger.log(e);
            }
        }
    }

    protected void deleteContainer(final CrawledLink source, final File file) {
        try {
            if (askFileDeletion() == false) {
                FileCreationManager.getInstance().delete(file, null);
            } else if (cls.size() > 0 && askFileDeletion()) {
                switch (JsonConfig.create(GeneralSettings.class).getDeleteContainerFilesAfterAddingThemAction()) {
                case ASK_FOR_DELETE:
                    final ConfirmDialog d = new ConfirmDialog(0, _JDT.T.AddContainerAction_delete_container_title(), _JDT.T.AddContainerAction_delete_container_msg(file.toString()), new AbstractIcon(IconKey.ICON_HELP, 32), _GUI.T.lit_yes(), _GUI.T.lit_no()) {
                        @Override
                        public String getDontShowAgainKey() {
                            return null;
                        }
                    };
                    final ConfirmDialogInterface s = UIOManager.I().show(ConfirmDialogInterface.class, d);
                    s.throwCloseExceptions();
                    if (s.getCloseReason() == CloseReason.OK) {
                        FileCreationManager.getInstance().delete(file, null);
                        if (s.isDontShowAgainSelected()) {
                            JsonConfig.create(GeneralSettings.class).setDeleteContainerFilesAfterAddingThemAction(DeleteContainerAction.DELETE);
                        }
                    } else {
                        if (s.isDontShowAgainSelected()) {
                            JsonConfig.create(GeneralSettings.class).setDeleteContainerFilesAfterAddingThemAction(DeleteContainerAction.DONT_DELETE);
                        }
                    }
                    break;
                case DELETE:
                    FileCreationManager.getInstance().delete(file, null);
                    break;
                case DONT_DELETE:

                }
            }
        } catch (DialogNoAnswerException e) {
            logger.log(e);
        }
    }

    protected boolean isDeleteContainer(final CrawledLink link, File file) {
        final String tmp = Application.getTempResource("").getAbsolutePath();
        final String rel = Files.getRelativePath(tmp, file.getAbsolutePath());
        if (rel == null) {
            final LinkOriginDetails origin = link.getOrigin();
            if (origin != null) {
                switch (origin.getOrigin()) {
                case DRAG_DROP_ACTION:
                case PASTE_LINKS_ACTION:
                case EXTENSION:
                    return false;
                default:
                    break;
                }
            }
            return true;
        } else {
            final CrawledLink origin = link.getOriginLink();
            logger.fine("Do not ask - just delete: " + origin.getURL());
            askFileDeletion = false;
            return true;
        }
    }

    private CrawledLink currentLink = null;

    public CrawledLink getCurrentLink() {
        return currentLink;
    }

    public void setCurrentLink(CrawledLink currentLink) {
        this.currentLink = currentLink;
    }

    public ArrayList<CrawledLink> decryptContainer(final CrawledLink source) {
        if (source.getURL() == null) {
            return null;
        }
        ArrayList<CrawledLink> retLinks = null;
        boolean showException = true;
        try {
            setCurrentLink(source);
            /* extract filename from url */
            final String sourceURL = new Regex(source.getURL(), "(file:/.+)").getMatch(0);
            if (sourceURL != null) {
                // workaround for authorities in file uris
                final String currentURI = sourceURL.replaceFirst("file:///?", "file:///");
                final File file = new File(new URI(currentURI));
                if (file != null && file.exists() && file.isFile()) {
                    final CrawledLink origin = source.getOriginLink();
                    if (origin != null && !StringUtils.containsIgnoreCase(origin.getURL(), "file:/")) {
                        askFileDeletion = false;
                    } else if (origin != null) {
                        final String originURL = new Regex(origin.getURL(), "(file:/.+)").getMatch(0);
                        if (originURL != null && !sourceURL.equalsIgnoreCase(originURL)) {
                            logger.fine("Do not ask - just delete: " + origin.getURL());
                            askFileDeletion = false;
                        }
                    }
                    initContainer(source, file, null);
                    retLinks = getContainedDownloadlinks();
                }
            } else {
                throw new Throwable("Invalid Container: " + source.getURL());
            }
        } catch (Throwable e) {
            /*
             * damn, something must have gone really really bad, lets keep the log
             */

            logger.log(e);
        }
        if (retLinks == null && showException) {
            /*
             * null as return value? something must have happened, do not clear log
             */
            logger.severe("ContainerPlugin out of date: " + this + " :" + getVersion());
        }
        return retLinks;
    }
}