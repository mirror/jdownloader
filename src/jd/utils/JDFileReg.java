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

package jd.utils;

import java.awt.Dimension;
import java.awt.Font;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;

import jd.config.SubConfiguration;
import jd.controlling.JDLogger;
import jd.gui.UserIO;
import jd.gui.skins.simple.GuiRunnable;
import jd.gui.userio.dialog.AbstractDialog;
import jd.gui.userio.dialog.ContainerDialog;
import jd.nutils.JDImage;
import jd.nutils.OSDetector;
import jd.nutils.io.JDIO;
import net.miginfocom.swing.MigLayout;

public class JDFileReg {

    public static String createSetKey(String key, String valueName, String value) {
        StringBuilder sb = new StringBuilder();

        sb.append("\r\n[HKEY_CLASSES_ROOT\\" + key + "]");

        if (valueName != null && valueName.trim().length() > 0) {
            sb.append("\r\n\"" + valueName + "\"=\"" + value + "\"");
        } else {
            sb.append("\r\n@=\"" + value + "\"");
        }

        return sb.toString();
    }

    public static void unregisterFileExts() {
        JDUtilities.runCommand("cmd", new String[] { "/c", "regedit", "/S", JDUtilities.getResourceFile("tools/windows/uninstall.reg").getAbsolutePath() }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
    }

    public static void registerFileExts() {
        if (!OSDetector.isWindows()) return;
        if (!SubConfiguration.getConfig("CNL2").getBooleanProperty("INSTALLED", false)) {
            StringBuilder sb = new StringBuilder();
            sb.append(createRegisterWinFileExt("jd"));
            sb.append(createRegisterWinFileExt("dlc"));
            sb.append(createRegisterWinFileExt("ccf"));
            sb.append(createRegisterWinFileExt("rsdf"));
            sb.append(createRegisterWinProtocol("jd"));
            sb.append(createRegisterWinProtocol("jdlist"));
            sb.append(createRegisterWinProtocol("dlc"));
            sb.append(createRegisterWinProtocol("ccf"));
            sb.append(createRegisterWinProtocol("rsdf"));
            AbstractDialog.setDefaultDimension(new Dimension(550, 400));
            JDIO.writeLocalFile(JDUtilities.getResourceFile("tmp/installcnl.reg"), "Windows Registry Editor Version 5.00\r\n\r\n\r\n\r\n" + sb.toString());
            int answer = showQuestion();
            if ((answer & UserIO.RETURN_OK) > 0) {
                JDUtilities.runCommand("cmd", new String[] { "/c", "regedit", JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath() }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
                JDUtilities.runCommand("cmd", new String[] { "/c", "regedit", "/e", JDUtilities.getResourceFile("tmp/test.reg").getAbsolutePath(), "HKEY_CLASSES_ROOT\\.dlc" }, JDUtilities.getResourceFile("tmp").getAbsolutePath(), 600);
                if (JDUtilities.getResourceFile("tmp/test.reg").exists()) {

                    JDLogger.getLogger().info("Installed Click'n'Load and associated .*dlc,.*ccf,.*rsdf and .*jd with JDownloader. Uninstall with " + JDUtilities.getResourceFile("tools/windows/uninstall.reg"));
                } else {

                    UserIO.getInstance().requestConfirmDialog(UserIO.NO_CANCEL_OPTION, JDLocale.L("gui.cnl.install.error.title", "Click'n'Load Installation"), JDLocale.LF("gui.cnl.install.error.message", "Installation of CLick'n'Load failed. Try these alternatives:\r\n * Start JDownloader as Admin.\r\n * Try to execute %s manually.\r\n * Open Configuration->General->Click'n'load-> [Install].\r\nFor details, visit http://jdownloader.org/click-n-load.", JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath()), JDTheme.II("gui.clicknload", 48, 48), null, null);

                    JDLogger.getLogger().severe("Installation of CLick'n'Load failed. Please try to start JDownloader as Admin. For details, visit http://jdownloader.org/click-n-load. Try to execute " + JDUtilities.getResourceFile("tmp/installcnl.reg").getAbsolutePath() + " manually");
                }

            }
            SubConfiguration.getConfig("CNL2").setProperty("INSTALLED", true);
            SubConfiguration.getConfig("CNL2").save();
        }
        JDUtilities.getResourceFile("tmp/test.reg").delete();

    }

    private static int showQuestion() {
        return (Integer) new GuiRunnable<Object>() {

            private ContainerDialog dialog;

            @Override
            public Object runSave() {
                JPanel c = new JPanel(new MigLayout("ins 10,wrap 1", "[grow,fill]", "[][][grow,fill]"));

                JLabel lbl = new JLabel(JDLocale.L("installer.gui.message", "After Installation, JDownloader will update to the latest version."));

                if (OSDetector.isWindows()) {
                    JDUtilities.getResourceFile("downloads");

                }
                c.add(lbl, "pushx,growx,split 2");

                Font f = lbl.getFont();
                f = f.deriveFont(f.getStyle() ^ Font.BOLD);

                lbl.setFont(f);
                c.add(new JLabel(JDTheme.II("gui.clicknload", 48, 48)), "alignx right");
                c.add(new JSeparator(), "pushx,growx,gapbottom 5");

                JTextArea txt;
                c.add(txt = new JTextArea(), "growy,pushy");
                txt.setText(JDLocale.L("gui.cnl.install.text", "Click'n'load is a very comfortable way to add links to JDownloader. \r\nTo install Click'n'Load, JDownloader has to set some registry entries. \r\nYou might have to confirm some Windows messages to continue."));
                txt.setLineWrap(true);
                txt.setBorder(null);
                txt.setOpaque(false);

                new ContainerDialog(UserIO.NO_COUNTDOWN, JDLocale.L("gui.cnl.install.title", "Click'n'Load Installation"), c, null, null) {
                    protected void packed() {
                        dialog = this;
                        this.setIconImage(JDImage.getImage("logo/jd_logo_54_54"));
                        this.setSize(550, 400);
                    }

                    protected void setReturnValue(boolean b) {
                        super.setReturnValue(b);

                    }
                };

                return dialog.getReturnValue();
            }

        }.getReturnValue();

    }

    private static String createRegisterWinFileExt(String ext) {

        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\") + " \\\"%1\\\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\r\n;Register fileextension ." + ext);
        sb.append(createSetKey("." + ext, "", "JDownloader " + ext + " file"));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\shell", "", "open"));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\")));
        sb.append(createSetKey("JDownloader " + ext + " file" + "\\shell\\open\\command", "", command));
        return sb.toString();
    }

    private static String createRegisterWinProtocol(String p) {
        String command = JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\") + " --add-link \\\"%1\\\"";
        StringBuilder sb = new StringBuilder();
        sb.append("\r\n\r\n;Register Protocol " + p + "://jdownloader.org/sample." + p);
        sb.append(createSetKey(p, "", "JDownloader " + p));
        sb.append(createSetKey(p + "\\DefaultIcon", "", JDUtilities.getResourceFile("JDownloader.exe").getAbsolutePath().replace("\\", "\\\\")));
        sb.append(createSetKey(p + "\\shell", "", "open"));
        sb.append(createSetKey(p, "Url Protocol", ""));
        sb.append(createSetKey(p + "\\shell\\open\\command", "", command));
        return sb.toString();
    }
}
