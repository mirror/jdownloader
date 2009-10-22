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

package jd.plugins.optional.customizer;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.toolbar.ViewToolbar;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class CustomizerGui extends SwitchPanel implements KeyListener, ActionListener {

    private static final long serialVersionUID = 7508784076121700378L;
    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.CustomizerGui.";

    private final SubConfiguration config;

    private CustomizerTable table;

    private JTextField tester;

    private JButton reset;

    public CustomizerGui(SubConfiguration config) {
        this.config = config;

        initActions();
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("ins 5, wrap 1", "[grow,fill]", "[][grow,fill][]"));
        ViewToolbar vt = new ViewToolbar() {
            private static final long serialVersionUID = -2194834048392779383L;

            @Override
            public void setDefaults(int i, AbstractButton ab) {
                ab.setForeground(new JLabel().getForeground());
            }
        };
        vt.setList(new String[] { "action.customize.addsetting", "action.customize.removesetting" });

        this.add(vt, "dock north,gapleft 3");
        this.add(new JScrollPane(table = new CustomizerTable(config.getGenericProperty(JDPackageCustomizer.PROPERTY_SETTINGS, new ArrayList<CustomizeSetting>()))), "growx,growy");
        this.add(new JLabel(JDL.L(JDL_PREFIX + "tester", "Insert examplelinks here to highlight the matched setting:")), "split 3, h pref!");
        this.add(tester = new JTextField(), "growx, h pref!");
        this.add(reset = new JButton(JDTheme.II("gui.images.undo", 16, 16)), "h pref!");

        table.addJDRowHighlighter(new JDRowHighlighter(new Color(204, 255, 170)) {

            @Override
            public boolean doHighlight(Object obj) {
                return ((CustomizeSetting) obj).matches(tester.getText());
            }

        });
        tester.addKeyListener(this);
        reset.addActionListener(this);
    }

    private void initActions() {
        new ThreadedAction("action.customize.addsetting", "gui.images.add") {
            private static final long serialVersionUID = 2902582906883565245L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.customize.addsetting.tooltip", "Add a new Setting"));
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                table.editingStopped(null);
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        String result = UserIO.getInstance().requestInputDialog(JDL.L("action.customize.addsetting.ask", "Please insert the name for the new Setting:"));
                        if (result != null) {
                            table.getModel().getSettings().add(new CustomizeSetting(result));
                            table.getModel().refreshModel();
                            table.getModel().fireTableDataChanged();
                        }
                        return null;
                    }
                }.start();

            }
        };

        new ThreadedAction("action.customize.removesetting", "gui.images.delete") {
            private static final long serialVersionUID = -961227177718839351L;

            @Override
            public void initDefaults() {
                this.setToolTipText(JDL.L("action.customize.removesetting.tooltip", "Remove selected Setting(s)"));
            }

            @Override
            public void init() {
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                table.editingStopped(null);
                if (rows.length == 0) return;
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.LF("action.customize.removesetting.ask", "Remove selected Setting(s)? (%s Account(s))", rows.length)), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    ArrayList<CustomizeSetting> settings = table.getModel().getSettings();
                    for (int i = rows.length - 1; i >= 0; --i) {
                        settings.remove(rows[i]);
                    }
                }
                table.getModel().refreshModel();
                table.getModel().fireTableDataChanged();
            }
        };
    }

    @Override
    protected void onHide() {
        config.setProperty(JDPackageCustomizer.PROPERTY_SETTINGS, table.getModel().getSettings());
        config.save();
    }

    @Override
    protected void onShow() {
        table.getModel().setSettings(config.getGenericProperty(JDPackageCustomizer.PROPERTY_SETTINGS, new ArrayList<CustomizeSetting>()));
        table.getModel().refreshModel();
    }

    public void keyPressed(KeyEvent e) {
        table.getModel().fireTableDataChanged();
    }

    public void keyReleased(KeyEvent e) {
    }

    public void keyTyped(KeyEvent e) {
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == reset) {
            tester.setText("");
            table.getModel().fireTableDataChanged();
        }
    }

}
