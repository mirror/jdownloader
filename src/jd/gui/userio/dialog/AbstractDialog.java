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
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.skins.simple.SimpleGUI;
import jd.gui.skins.simple.SimpleGuiConstants;
import jd.nutils.JDHash;
import jd.nutils.Screen;
import jd.utils.JDLocale;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractDialog extends JCountdownDialog implements ActionListener {

    /**
     * 
     */
    private static final long serialVersionUID = -169149552591067268L;

    private static final String DIALOGS_CONFIG = "DIALOGS";

    private JButton btnCancel;

    private JButton btnOK;

    protected JPanel contentpane;

    private int flag;

    private Object returnValue = null;

    private ImageIcon icon;

    private String okOption;

    private String cancelOption;

    private JCheckBox dont;

    public AbstractDialog(int flag, String title, ImageIcon icon, String okOption, String cancelOption) {
        super(SimpleGUI.CURRENTGUI);
        contentpane = new JPanel(new MigLayout("ins 0,wrap 1", "[fill,grow]", "[fill,grow]"));
        this.flag = flag;
        setTitle(title);

        this.icon = icon;
        this.okOption = (okOption == null) ? JDLocale.L("gui.btn_ok", null) : okOption;
        this.cancelOption = (okOption == null) ? JDLocale.L("gui.btn_cancel", null) : cancelOption;

    }

    public void init() {
        if ((flag & UserIO.DONT_SHOW_AGAIN) > 0) {
            SubConfiguration cfg = SubConfiguration.getConfig(DIALOGS_CONFIG);
            // System.out.println(cfg+toString()+"This restore" +
            // "DONT_SHOW_AGAIN_" + JDHash.getMD5(this.toString()) + " " +
            // cfg.getProperty("DONT_SHOW_AGAIN_" +
            // JDHash.getMD5(this.toString())));
            Object value;
            if ((value = cfg.getProperty("DONT_SHOW_AGAIN_" + JDHash.getMD5(this.toString()))) != null) {
                if (value instanceof Integer) {
                    int i = ((Integer) value).intValue();
                    this.returnValue = (i & (UserIO.RETURN_OK | UserIO.RETURN_CANCEL)) | UserIO.RETURN_DONT_SHOW_AGAIN | UserIO.RETURN_SKIPPED_BY_DONT_SHOW;
                }
                return;

            }

        }
        this.setModal(true);

        this.setLayout(new MigLayout("ins 5,wrap 1", "[fill,grow]"));

        btnOK = new JButton(this.okOption);
        btnOK.addActionListener(this);

        btnCancel = new JButton(this.cancelOption);
        btnCancel.addActionListener(this);

        this.getRootPane().setDefaultButton(btnOK);
        this.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        if (icon != null) {
            add(new JLabel(this.icon), "split 2,alignx left,aligny center,shrinkx,gapright 10");
        }
        contentInit(contentpane);
        add(contentpane, "pushx,growx,pushy,growy,spanx,aligny center,wrap");
        add(this.countDownLabel, "split 5,growx");

        if ((flag & UserIO.DONT_SHOW_AGAIN) > 0) {
            dont = new JCheckBox();

            dont.setHorizontalAlignment(JCheckBox.TRAILING);

            add(new JLabel(JDLocale.L("gui.dialogs.dontshowthisagain", "Don't show this again")));
            add(dont, "alignx right");
        }
        if ((flag & UserIO.NO_OK_OPTION) == 0) {
            add(btnOK, "alignx right");
        }
        if ((flag & UserIO.NO_CANCEL_OPTION) == 0) {
            add(btnCancel, "alignx right");
        }
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

    public String toString() {
        return "dialog-" + this.getTitle();
    }

    abstract public void contentInit(JPanel contentpane);

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            setReturnValue(true);

        } else if (e.getSource() == btnCancel) {

            setReturnValue(false);
        }
        dispose();

    }

    // @Override
    protected void onCountdown() {

        setReturnValue(false);
        this.dispose();
    }

    private void setReturnValue(boolean b) {
        returnValue = b ? UserIO.RETURN_OK : UserIO.RETURN_CANCEL;
        if ((flag & UserIO.DONT_SHOW_AGAIN) > 0) {

            if (dont.isSelected()) {
                returnValue = b ? UserIO.RETURN_OK | UserIO.RETURN_DONT_SHOW_AGAIN : UserIO.RETURN_CANCEL | UserIO.RETURN_DONT_SHOW_AGAIN;
                SubConfiguration cfg = SubConfiguration.getConfig(DIALOGS_CONFIG);
                cfg.setProperty("DONT_SHOW_AGAIN_" + JDHash.getMD5(this.toString()), (Integer) returnValue);
                cfg.save();
                // System.out.println(cfg+toString()+" This save" +
                // "DONT_SHOW_AGAIN_" + JDHash.getMD5(this.toString()) + " " +
                // returnValue);

            }
        }

    }

    public Object getReturnValue() {
        // TODO Auto-generated method stub

        return returnValue;
    }
}
