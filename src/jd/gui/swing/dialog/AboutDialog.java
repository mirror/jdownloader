//    jDownloader - Downloadmanager
//    Copyright (C) 2009  JD-Team support@jdownloader.org
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

package jd.gui.swing.dialog;

import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import jd.gui.swing.Factory;
import jd.gui.swing.components.linkbutton.JLink;
import jd.gui.swing.jdgui.JDGui;
import jd.nutils.io.JDIO;
import jd.utils.JDUtilities;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.TypeRef;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.uio.UIOManager;
import org.appwork.utils.Application;
import org.appwork.utils.IO;
import org.appwork.utils.formatter.SizeFormatter;
import org.appwork.utils.logging.Log;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.ConfirmDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.logging.LogController;

public class AboutDialog extends AbstractDialog<Integer> {

    public AboutDialog() {
        super(UIOManager.BUTTONS_HIDE_CANCEL | UIOManager.BUTTONS_HIDE_OK | Dialog.STYLE_HIDE_ICON, _GUI._.jd_gui_swing_components_AboutDialog_title(), null, null, null);
    }

    @Override
    protected Integer createReturnValue() {
        return null;
    }

    @Override
    protected boolean isResizable() {
        return false;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel contentpane = new JPanel();
        JLabel lbl = new JLabel("JDownloader 2");
        lbl.setFont(lbl.getFont().deriveFont(lbl.getFont().getSize() * 2.0f));

        JPanel links = new JPanel(new MigLayout("ins 0", "[]push[]push[]push[]"));
        try {
            JButton btn = Factory.createButton(_GUI._.jd_gui_swing_components_AboutDialog_license(), NewTheme.I().getIcon("premium", 16), new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    String license = JDIO.readFileToString(JDUtilities.getResourceFile("licenses/jdownloader.license"));
                    try {
                        ConfirmDialog d = new ConfirmDialog(Dialog.STYLE_LARGE | Dialog.STYLE_HIDE_ICON | UIOManager.BUTTONS_HIDE_CANCEL, _GUI._.jd_gui_swing_components_AboutDialog_license_title(), license, null, null, null) {

                            @Override
                            protected boolean isResizable() {
                                return true;
                            }

                        };
                        d.setPreferredSize(JDGui.getInstance().getMainFrame().getSize());

                        Dialog.getInstance().showDialog(d);
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }

            });
            btn.setBorder(null);

            links.add(btn);
            links.add(new JLink(_GUI._.jd_gui_swing_components_AboutDialog_homepage(), NewTheme.I().getIcon("url", 16), new URL("http://www.jdownloader.org/home?lng=en")));
            links.add(new JLink(_GUI._.jd_gui_swing_components_AboutDialog_forum(), NewTheme.I().getIcon("board", 16), new URL("http://board.jdownloader.org")));
            links.add(new JLink(_GUI._.jd_gui_swing_components_AboutDialog_contributers(), NewTheme.I().getIcon("contributer", 16), new URL("http://jdownloader.org/knowledge/wiki/contributers")));
        } catch (MalformedURLException e1) {
            e1.printStackTrace();
        }

        contentpane.setLayout(new MigLayout("ins 10, wrap 1", "[grow,fill]"));
        contentpane.add(new JLabel(NewTheme.I().getIcon("logo/jd_logo_128_128", -1)), "aligny center, spany 6");

        contentpane.add(lbl, "split 2");
        // this has been the branch label
        contentpane.add(new JLabel(""), "pushx,growx");

        MigPanel stats = new MigPanel("ins 0,wrap 2", "[][grow,align right]", "[]");

        contentpane.add(stats, "pushx,growx,spanx");
        HashMap<String, Object> map = null;
        try {
            map = JSonStorage.restoreFromString(IO.readFileToString(Application.getResource("build.json")), new TypeRef<HashMap<String, Object>>() {
            });

            // contentpane.add(btn, "aligny center, spany 3");
            stats.add(new JLabel(_GUI._.jd_gui_swing_components_AboutDialog_builddate()));
            stats.add(disable(map.get("buildDate")));
            stats.add(new JLabel(_GUI._.jd_gui_swing_components_AboutDialog_sourcerevisions()), "spanx");
            stats.add(new JLabel("   " + _GUI._.jd_gui_swing_components_AboutDialog_core()), "");
            stats.add(disable("#" + map.get("JDownloaderRevision")));

            stats.add(new JLabel("   " + _GUI._.jd_gui_swing_components_AboutDialog_launcher()), "");
            stats.add(disable("#" + map.get("JDownloaderUpdaterRevision")));

            stats.add(new JLabel("   " + _GUI._.jd_gui_swing_components_AboutDialog_appworkutilities()), "");

            stats.add(disable("#" + map.get("AppWorkUtilsRevision")));

            stats.add(new JLabel("   " + _GUI._.jd_gui_swing_components_AboutDialog_browser()), "");

            stats.add(disable("#" + map.get("JDBrowserRevision")));
            stats.add(new JLabel("   " + _GUI._.jd_gui_swing_components_AboutDialog_updater()), "");

            stats.add(disable("#" + map.get("UpdateClientV2Revision")));
        } catch (Throwable t) {
            Log.exception(t);

        }
        try {
            stats.add(new JLabel("Java:"), "");
            java.lang.management.MemoryUsage memory = java.lang.management.ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
            ExtButton comp;
            stats.add(comp = disable(System.getProperty("java.vendor") + " - " + System.getProperty("java.version") + " (" + SizeFormatter.formatBytes(memory.getUsed()) + "/" + SizeFormatter.formatBytes(memory.getCommitted()) + "/" + SizeFormatter.formatBytes(memory.getMax()) + ")"));
            comp.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {

                    CrossSystem.showInExplorer(new File(CrossSystem.getJavaBinary()));
                    try {
                        java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                        List<String> arguments = runtimeMxBean.getInputArguments();
                        StringBuilder sb = new StringBuilder();
                        for (String s : arguments) {
                            if (sb.length() > 0) sb.append(" ");
                            sb.append(s);
                        }

                        StringSelection selection = new StringSelection(sb.toString());
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        clipboard.setContents(selection, selection);
                    } catch (final Throwable e1) {

                    }
                }
            });
            try {
                java.lang.management.RuntimeMXBean runtimeMxBean = java.lang.management.ManagementFactory.getRuntimeMXBean();
                List<String> arguments = runtimeMxBean.getInputArguments();
                StringBuilder sb = new StringBuilder();
                for (String s : arguments) {
                    if (sb.length() > 0) sb.append("\r\n");
                    sb.append(s);
                }

                comp.setToolTipText(sb.toString());
            } catch (final Throwable e1) {
                LogController.GL.log(e1);
            }
        } catch (final Throwable e) {
            LogController.GL.log(e);
        }

        contentpane.add(new JLabel(_GUI._.jd_gui_swing_components_AboutDialog_synthetica("(#289416475)")), "gaptop 10, spanx");
        contentpane.add(new JLabel("© AppWork GmbH 2007-2013"), "spanx");
        contentpane.add(links, "gaptop 15, growx, pushx, spanx");
        this.registerEscape(contentpane);

        return contentpane;
    }

    private ExtButton disable(final Object object) {

        ExtButton ret = new ExtButton(new AppAction() {
            {
                setName(object + "");
            }

            @Override
            public void actionPerformed(ActionEvent e) {
            }
        });
        ret.setBorderPainted(false);
        ret.setContentAreaFilled(false);
        ret.setEnabled(true);
        ret.setPreferredSize(new Dimension(ret.getPreferredSize().width, 20));
        return ret;
    }

}