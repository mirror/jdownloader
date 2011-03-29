package org.jdownloader.extensions.lecturnity;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import jd.HostPluginWrapper;
import jd.config.ConfigContainer;
import jd.controlling.JDLogger;
import jd.controlling.LinkGrabberController;
import jd.controlling.ProgressController;
import jd.gui.UserIO;
import jd.gui.swing.jdgui.menu.MenuAction;
import jd.http.Browser;
import jd.plugins.AddonPanel;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;
import jd.plugins.OptionalPlugin;
import jd.utils.JDUtilities;

import org.appwork.utils.Regex;
import org.jdownloader.extensions.AbstractExtension;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.extensions.StartException;
import org.jdownloader.extensions.StopException;

@OptionalPlugin(rev = "$Revision$", id = "lecturnity", interfaceversion = 7)
public class LecturnityDownloaderExtension extends AbstractExtension implements ActionListener {

    public final static String PROPERTY_DOWNLOADDIR = "PROPERTY_DOWNLOADDIR";

    private final String       downloadDir          = JDUtilities.getDefaultDownloadDirectory() + "/lecturnity/";

    private MenuAction         inputAction;

    private HostPluginWrapper  hpw                  = null;

    private Browser            br;

    public ExtensionConfigPanel<LecturnityDownloaderExtension> getConfigPanel() {
        return null;
    }

    public boolean hasConfigPanel() {
        return false;
    }

    public LecturnityDownloaderExtension() throws StartException {
        super("Lecurnity Loader");
        br = new Browser();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == inputAction) {
            String url = UserIO.getInstance().requestInputDialog("Type in URL of the Lecturnity-Webplayer! (Could take a few seconds!)");
            if (url == null || url.trim().length() == 0) return;
            getLinks(url.trim());
        }
    }

    private void getLinks(final String url) {
        new Thread(new Runnable() {

            public void run() {
                ProgressController progress = new ProgressController("Lecturnity: Parsing " + url, null);
                progress.setInitials("LE");
                progress.setIndeterminate(true);

                String message;
                try {
                    String name = url.substring(url.substring(0, url.length() - 1).lastIndexOf('/') + 1, url.length() - 1);

                    ArrayList<DownloadLink> links = listFiles(name, name + "/", url);
                    LinkGrabberController.getInstance().addLinks(links, true, false);

                    message = "Successfully added " + links.size() + " links to downloadlist!";
                } catch (Exception e1) {
                    JDLogger.exception(e1);

                    message = "An error occured while parsing the site " + url + "! See Log for more informations!";
                    progress.setColor(Color.RED);
                }

                progress.setStatusText("Lecturnity: " + message);
                progress.doFinalize(5 * 1000l);

                UserIO.getInstance().requestMessageDialog(message);
            }

        }).start();
    }

    private ArrayList<DownloadLink> listFiles(String name, String subDir, String source) throws Exception {
        String page = br.getPage(source);

        String[] links = br.getRegex("<a href=\"(.*?)\">").getColumn(0);

        String downloadDir = this.downloadDir + subDir;

        /*
         * Create a new FilePackage for each folder.
         */
        FilePackage fp = FilePackage.getInstance();
        fp.setName(name);
        fp.setDownloadDirectory(downloadDir);

        ArrayList<DownloadLink> result = new ArrayList<DownloadLink>();
        DownloadLink dLink;
        for (String link : links) {
            if (link.startsWith("?") || link.startsWith("/")) continue;

            if (link.endsWith("/")) {
                /*
                 * Recursively call this method again to analyze the subfolders.
                 */
                result.addAll(listFiles(name + "-" + link.substring(0, link.length() - 1), subDir + link, source + link));
            } else {
                /*
                 * Create new DownloadLink with all information, we know at this
                 * time! We know the filename and the filesize and so it is
                 * currently available.
                 */
                dLink = new DownloadLink(hpw.getPlugin(), link, hpw.getHost(), source + link, true);
                dLink.setFinalFileName(link);
                dLink.setDownloadSize(getSize(new Regex(page, link + "</a>[ ]+.*?[ ].*?[ ]+(\\d+\\.?\\d?[K|M|G]?)").getMatch(0)));
                dLink.setBrowserUrl(source);
                dLink.setAvailable(true);
                dLink.setProperty(PROPERTY_DOWNLOADDIR, downloadDir);

                fp.add(dLink);

                result.add(dLink);
            }
        }

        return result;
    }

    private static final long getSize(String string) {
        if (string == null) return 0;
        try {
            double res = Double.parseDouble(string.replaceAll("[K|M|G]", ""));

            if (string.contains("K")) {
                res *= 1024;
            } else if (string.contains("M")) {
                res *= 1024 * 1024;
            } else if (string.contains("G")) {
                res *= 1024 * 1024 * 1024;
            }

            return Math.round(res);
        } catch (Exception e) {
            return 0;
        }
    }

    @Override
    protected void stop() throws StopException {
        HostPluginWrapper.writeLock.lock();
        HostPluginWrapper.getHostWrapper().remove(hpw);
        HostPluginWrapper.writeLock.unlock();
        hpw = null;
        logger.finest("Lecturnity: Unloaded Host-Plugin!");
    }

    @Override
    protected void start() throws StartException {

        hpw = new HostPluginWrapper("lecturnity-loader", "jd.plugins.optional.lecturnity.", "LecturnityLoader", "HIDE_ME", 0, "$Revision$");
        logger.finest("Lecturnity: Loaded Host-Plugin!");

        inputAction = new MenuAction("lecturnity", 0);
        inputAction.setActionListener(this);
        inputAction.setIcon(this.getIconKey());

        logger.info("Lecturnity: OK!");

    }

    @Override
    protected void initSettings(ConfigContainer config) {
    }

    @Override
    public String getConfigID() {
        return null;
    }

    @Override
    public String getAuthor() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public AddonPanel getGUI() {
        return null;
    }

    @Override
    public ArrayList<MenuAction> getMenuAction() {
        ArrayList<MenuAction> menu = new ArrayList<MenuAction>();

        menu.add(inputAction);

        return menu;
    }

    @Override
    protected void initExtension() throws StartException {
    }

}