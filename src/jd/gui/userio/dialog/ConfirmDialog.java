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

package jd.gui.userio.dialog;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.WindowConstants;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.nutils.Screen;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public class ConfirmDialog extends JCountdownDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -169149552591067268L;

    private JButton btnCancel;

    private JButton btnOK;

    private JTextPane textField;

    private int flag;

    private Boolean returnValue = false;

    private String message;

    private ImageIcon icon;

    private String okOption;

    private String cancelOption;

    public ConfirmDialog(int flag, String title, String message, ImageIcon icon, String okOption, String cancelOption) {
        super(SimpleGUI.CURRENTGUI);
        this.flag = flag;
        setTitle(title);
        this.message = message;
        this.icon = icon;
        this.okOption = (okOption == null) ? JDLocale.L("gui.btn_ok", null) : okOption;
        this.cancelOption = (okOption == null) ? JDLocale.L("gui.btn_cancel", null) : cancelOption;
        init();
    }

    public void init() {

        this.setModal(true);

        this.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        textField = new JTextPane();
        textField.setBorder(null);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.setText(this.message);
        textField.setEditable(false);

        btnOK = new JButton(this.okOption);
        btnOK.addActionListener(this);

        btnCancel = new JButton(this.cancelOption);
        btnCancel.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (icon != null) {
            add(new JLabel(this.icon), "split 2,alignx left,aligny center,shrinkx");
        }
        add(textField, "pushx,growx,pushy,growy,spanx,aligny center,wrap");
        add(this.countDownLabel, "split 3,growx");
        if ((flag & UserIO.NO_OK_OPTION) == 0) add(btnOK, "alignx right");
        if ((flag & UserIO.NO_CANCEL_OPTION) == 0) add(btnCancel, "alignx right");
        this.setMinimumSize(new Dimension(300, -1));
        this.pack();
        this.setResizable(false);
        if (SimpleGUI.CURRENTGUI == null || SimpleGUI.CURRENTGUI.getExtendedState() == JFrame.ICONIFIED || !SimpleGUI.CURRENTGUI.isVisible() || !SimpleGUI.CURRENTGUI.isActive()) {
            this.setLocation(Screen.getDockBottomRight(this));
        } else {
            this.setLocation(Screen.getCenterOfComponent(SimpleGUI.CURRENTGUI, this));
        }
        this.toFront();
        this.setAlwaysOnTop(true);
        SubConfiguration cfg = SubConfiguration.getConfig(SimpleGuiConstants.GUICONFIGNAME);
        if ((flag & UserIO.NO_COUNTDOWN) == 0) this.countdown(Math.max(2, cfg.getIntegerProperty(SimpleGuiConstants.PARAM_INPUTTIMEOUT, 20)));

        this.setVisible(true);
        this.toFront();

    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            this.returnValue = true;

        } else if (e.getSource() == btnCancel) {
            this.returnValue = false;

        }
        dispose();

    }

    @Override
    protected void onCountdown() {
        this.returnValue = false;
        this.dispose();
    }

    public Boolean getReturnValue() {
        // TODO Auto-generated method stub
        return returnValue;
    }
}
