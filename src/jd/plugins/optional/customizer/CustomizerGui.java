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

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.GuiRunnable;
import jd.gui.swing.components.table.JDRowHighlighter;
import jd.gui.swing.jdgui.actions.ThreadedAction;
import jd.gui.swing.jdgui.interfaces.DroppedPanel;
import jd.gui.swing.jdgui.interfaces.SwitchPanel;
import jd.gui.swing.jdgui.views.ViewToolbar;
import jd.nutils.JDFlags;
import jd.utils.JDTheme;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public class CustomizerGui extends SwitchPanel {

    private static final long serialVersionUID = 7508784076121700378L;
    private static final String JDL_PREFIX = "jd.plugins.optional.customizer.CustomizerGui.";

    private final CustomizerTable table;
    private SubConfiguration config;

    public CustomizerInfoPanel getInfoPanel() {
        return new CustomizerInfoPanel();
    }

    public CustomizerGui(SubConfiguration config) {
        table = new CustomizerTable();
        this.config = config;
        initActions();
        initGUI();
    }

    private void initGUI() {
        this.setLayout(new MigLayout("ins 0, wrap 1", "[grow,fill]", "[][grow,fill]"));
        this.add(new ViewToolbar("action.customize.addsetting", "action.customize.removesetting"));
        this.add(new JScrollPane(table), "grow");
    }

    private void initActions() {
        new ThreadedAction("action.customize.addsetting", "gui.images.add") {
            private static final long serialVersionUID = 2902582906883565245L;

            @Override
            public void initDefaults() {
            }

            @Override
            public void threadedActionPerformed(final ActionEvent e) {
                table.editingStopped(null);
                new GuiRunnable<Object>() {

                    @Override
                    public Object runSave() {
                        String result = UserIO.getInstance().requestInputDialog(JDL.L("action.customize.addsetting.ask", "Please insert the name for the new Setting:"));
                        if (result != null) {
                            CustomizeSetting.getSettings().add(new CustomizeSetting(result));
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
            }

            @Override
            public void threadedActionPerformed(ActionEvent e) {
                int[] rows = table.getSelectedRows();
                table.editingStopped(null);
                if (rows.length == 0) return;
                if (JDFlags.hasSomeFlags(UserIO.getInstance().requestConfirmDialog(0, JDL.LF("action.customize.removesetting.ask", "Remove selected Setting(s)? (%s Account(s))", rows.length)), UserIO.RETURN_OK, UserIO.RETURN_DONT_SHOW_AGAIN)) {
                    ArrayList<CustomizeSetting> settings = CustomizeSetting.getSettings();
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
        config.setProperty(JDPackageCustomizer.PROPERTY_SETTINGS, CustomizeSetting.getSettings());
        config.save();
    }

    @Override
    protected void onShow() {
        table.getModel().refreshModel();
        table.getModel().fireTableDataChanged();
    }

    public class CustomizerInfoPanel extends DroppedPanel implements KeyListener, ActionListener {

        private static final long serialVersionUID = 1313970313241445270L;

        private final Color valueColor;
        private final Color titleColor;

        private JLabel iconContainer;
        private JTextField tester;
        private JButton reset;

        private CustomizerInfoPanel() {
            this.setLayout(new MigLayout("ins 5, wrap 2", "[]20[grow,fill]", "[][]"));
            valueColor = getBackground().darker().darker().darker().darker().darker();
            titleColor = getBackground().darker().darker();
            this.iconContainer = new JLabel(JDTheme.II("gui.images.newpackage", 32, 32));
            add(iconContainer, "spany, gapleft 1");

            JLabel title;
            add(title = new JLabel(JDL.L(JDL_PREFIX + "tester", "Insert examplelinks here to highlight the matched setting:")));
            add(tester = new JTextField(), "growx, split 2");
            add(reset = new JButton(JDTheme.II("gui.images.undo", 16, 16)));

            title.setForeground(titleColor);
            tester.setForeground(valueColor);
            reset.setForeground(valueColor);

            tester.addKeyListener(this);
            reset.addActionListener(this);

            table.addJDRowHighlighter(new JDRowHighlighter(new Color(204, 255, 170)) {

                @Override
                public boolean doHighlight(Object obj) {
                    return tester.getText().length() > 0 && (CustomizeSetting) obj == CustomizeSetting.getFirstMatch(tester.getText());
                }

            });
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

        @Override
        protected void onHide() {
        }

        @Override
        protected void onShow() {
        }

    }

}
