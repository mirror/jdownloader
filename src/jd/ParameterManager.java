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

package jd;

import java.io.File;
import java.util.Vector;
import java.util.logging.Logger;

import jd.captcha.JACController;
import jd.controlling.PasswordListController;
import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.reconnect.Reconnecter;
import jd.gui.UIConstants;
import jd.gui.UserIF;

import org.jdownloader.plugins.controller.crawler.CrawlerPluginController;
import org.jdownloader.plugins.controller.host.HostPluginController;

public class ParameterManager {

    private static final Logger LOG = jd.controlling.JDLogger.getLogger();

    public static void processParameters(final String[] input) {

        boolean addLinksSwitch = false;
        boolean addContainersSwitch = false;
        boolean addPasswordsSwitch = false;

        final Vector<String> linksToAdd = new Vector<String>();
        final Vector<String> containersToAdd = new Vector<String>();

        for (final String currentArg : input) {

            if (currentArg.equals("--scan") || currentArg.equals("-scan")) {
                HostPluginController.getInstance().init(true);
                CrawlerPluginController.getInstance().init(true);
            } else if (currentArg.equals("--help") || currentArg.equals("-h")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                ParameterManager.showCmdHelp();

            } else if (currentArg.equals("--add-links") || currentArg.equals("--add-link") || currentArg.equals("-a")) {

                addLinksSwitch = true;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                ParameterManager.LOG.info(currentArg + " parameter");

            } else if (currentArg.equals("--add-containers") || currentArg.equals("--add-container") || currentArg.equals("-co")) {

                addContainersSwitch = true;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                ParameterManager.LOG.info(currentArg + " parameter");

            } else if (currentArg.equals("--add-passwords") || currentArg.equals("--add-password") || currentArg.equals("-p")) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = true;
                ParameterManager.LOG.info(currentArg + " parameter");

            } else if (currentArg.equals("--show") || currentArg.equals("-s")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                JACController.showDialog(false);

            } else if (currentArg.equals("--train") || currentArg.equals("-t")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;

                JACController.showDialog(true);

            } else if (currentArg.equals("--minimize") || currentArg.equals("-m")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                UserIF.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_MINIMIZED);

                ParameterManager.LOG.info(currentArg + " parameter");

            } else if (currentArg.equals("--focus") || currentArg.equals("-f")) {

                // final OptionalPluginWrapper addon =
                // JDUtilities.getOptionalPlugin("trayicon");
                // if (addon != null && addon.isEnabled()) {
                // addon.getPlugin().interact("refresh", null);
                // }
                // addLinksSwitch = false;
                // addContainersSwitch = false;
                // addPasswordsSwitch = false;
                // ParameterManager.LOG.info(currentArg + " parameter");
                // UserIF.getInstance().setFrameStatus(UIConstants.WINDOW_STATUS_FOREGROUND);

            } else if (currentArg.equals("--reconnect") || currentArg.equals("-r")) {

                addLinksSwitch = false;
                addContainersSwitch = false;
                addPasswordsSwitch = false;
                ParameterManager.LOG.info(currentArg + " parameter");
                Reconnecter.getInstance().forceReconnect();

            } else if (addLinksSwitch && currentArg.charAt(0) != '-') {

                linksToAdd.add(currentArg);

            } else if (addContainersSwitch && currentArg.charAt(0) != '-') {

                if (new File(currentArg).exists()) {
                    containersToAdd.add(currentArg);
                } else {
                    ParameterManager.LOG.warning("Container does not exist");
                }

            } else if (addPasswordsSwitch && !(currentArg.charAt(0) == '-')) {

                ParameterManager.LOG.info("Add password: " + currentArg);

                PasswordListController.getInstance().addPassword(currentArg, false);

            } else if (currentArg.contains("http://") && !(currentArg.charAt(0) == '-')) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                linksToAdd.add(currentArg);

            } else if (new File(currentArg).exists() && !(currentArg.charAt(0) == '-')) {

                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;
                containersToAdd.add(currentArg);

            } else {
                addContainersSwitch = false;
                addLinksSwitch = false;
                addPasswordsSwitch = false;

            }

        }

        if (linksToAdd.size() > 0) {
            ParameterManager.LOG.info("Links to add: " + linksToAdd.toString());
        }
        if (containersToAdd.size() > 0) {
            ParameterManager.LOG.info("Containers to add: " + containersToAdd.toString());
        }

        final StringBuilder adder = new StringBuilder();
        for (final String string : linksToAdd) {
            if (adder.length() > 0) adder.append("\r\n");
            adder.append(string);
        }
        for (final String string : containersToAdd) {
            if (adder.length() > 0) adder.append("\r\n");
            adder.append("file://");
            adder.append(string);
        }
        String job = adder.toString().trim();
        if (job.length() == 0) return;
        LinkCollector.getInstance().addCrawlerJob(new LinkCollectingJob(job));
    }

    public static void showCmdHelp() {
        final String[][] help = new String[][] { { "JDownloader", "JD-Team" }, { "http://jdownloader.org/\t\t", "http://board.jdownloader.org" + System.getProperty("line.separator") }, { "-h/--help\t", "Show this help message" }, { "-a/--add-link(s)", "Add links" }, { "-co/--add-container(s)", "Add containers" }, { "-m/--minimize\t", "Minimize download window" }, { "-f/--focus\t", "Get jD to foreground/focus" }, { "-s/--show\t", "Show JAC prepared captchas" }, { "-t/--train\t", "Train a JAC method" }, { "-r/--reconnect\t", "Perform a Reconnect" }, { "-C/--captcha <filepath or url> <method>", "Get code from image using JAntiCaptcha" }, { "-p/--add-password(s)", "Add passwords" }, { "-n --new-instance", "Force new instance if another jD is running" } };
        for (final String helpLine[] : help) {
            System.out.println(helpLine[0] + "\t" + helpLine[1]);
        }
    }
}
