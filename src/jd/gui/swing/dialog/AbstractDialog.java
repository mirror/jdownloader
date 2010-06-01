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

package jd.gui.swing.dialog;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRootPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import jd.config.SubConfiguration;
import jd.gui.UserIO;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.interfaces.JDMouseAdapter;
import jd.gui.userio.DummyFrame;
import jd.nutils.JDFlags;
import jd.nutils.Screen;
import jd.nutils.encoding.Encoding;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractDialog extends JCountdownDialog implements ActionListener {

    private static final long serialVersionUID = -169149552591067268L;

    public static final String DIALOGS_CONFIG = "DIALOGS";

    protected JButton btnCancel;

    protected JButton btnOK;

    protected JComponent contentpane;
    protected static Color BACKGROUND_COLOR = new Color(0xeae9d7);

    private static Dimension DEFAULT_DIMENSION;
    protected int flag;

    private int returnValue = 0;

    private ImageIcon icon;

    private String okOption;

    private String cancelOption;

    private JCheckBox dont;

    private JPanel buttonBar;

    public AbstractDialog(int flag, String title, ImageIcon icon, String okOption, String cancelOption) {
        super(DummyFrame.getDialogParent());

        this.flag = flag;
        setTitle(title);

        this.icon = JDFlags.hasAllFlags(flag, UserIO.NO_ICON) ? null : icon;
        this.okOption = (okOption == null) ? JDL.L("gui.btn_ok", "OK") : okOption;
        this.cancelOption = (cancelOption == null) ? JDL.L("gui.btn_cancel", "Cancel") : cancelOption;
    }

    public void init() {
        dont: if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN)) {
            SubConfiguration cfg = SubConfiguration.getConfig(DIALOGS_CONFIG);
            Object value;
            if ((value = cfg.getProperty(getDontShowAgainKey())) != null && value instanceof Integer) {
                int i = ((Integer) value).intValue();
                int ret = (i & (UserIO.RETURN_OK | UserIO.RETURN_CANCEL)) | UserIO.RETURN_DONT_SHOW_AGAIN | UserIO.RETURN_SKIPPED_BY_DONT_SHOW;

                // return if the stored values are excluded
                if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL) && JDFlags.hasAllFlags(ret, UserIO.RETURN_CANCEL)) {
                    break dont;
                }
                if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_OK) && JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)) {
                    break dont;
                }

                this.returnValue = ret;
                return;
            }
        }

        this.setModal(true);
        this.setLayout(new MigLayout("ins 5", "[fill,grow]", "[fill,grow][]"));
        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        btnOK = new JButton(this.okOption);
        btnOK.addActionListener(this);

        JButton focus = btnOK;

        btnCancel = new JButton(this.cancelOption);
        btnCancel.addActionListener(this);
        if (icon != null) {
            add(new JLabel(this.icon), "split 2,alignx left,aligny center,shrinkx,gapright 10");
        }
        contentpane = contentInit();
        add(contentpane, "pushx,growx,pushy,growy,spanx,aligny center,wrap");

        add(countDownLabel, "split 3,growx,hidemode 2");
        if ((flag & UserIO.DONT_SHOW_AGAIN) > 0) {
            dont = new JCheckBox(JDL.L("gui.dialogs.dontshowthisagain", "Don't show this again"));
            dont.setHorizontalAlignment(JCheckBox.TRAILING);
            dont.setHorizontalTextPosition(JCheckBox.LEADING);

            add(dont, "alignx right");
        } else {
            add(Box.createHorizontalGlue(), "growx,pushx");
        }

        buttonBar = new JPanel(new MigLayout("ins 0", "[fill,grow]", "[fill,grow]"));
        if ((flag & UserIO.NO_OK_OPTION) == 0) {
            getRootPane().setDefaultButton(btnOK);

            btnOK.addHierarchyListener(new HierarchyListener() {
                public void hierarchyChanged(HierarchyEvent e) {
                    if ((e.getChangeFlags() & HierarchyEvent.PARENT_CHANGED) != 0) {
                        JButton defaultButton = (JButton) e.getComponent();
                        JRootPane root = SwingUtilities.getRootPane(defaultButton);
                        if (root != null) {
                            root.setDefaultButton(defaultButton);
                        }
                    }
                }
            });
            focus = btnOK;
            buttonBar.add(btnOK, "alignx right,tag ok,sizegroup confirms");
        }
        if ((flag & UserIO.NO_CANCEL_OPTION) == 0) {
            buttonBar.add(btnCancel, "alignx right,tag cancel,sizegroup confirms");
            if ((flag & UserIO.NO_OK_OPTION) != 0) {
                this.getRootPane().setDefaultButton(btnCancel);
                btnCancel.requestFocusInWindow();
                focus = btnCancel;
            }
        }
        addButtons(buttonBar);
        add(buttonBar, "alignx right");

        if (JDFlags.hasNoFlags(flag, UserIO.NO_COUNTDOWN)) {
            this.countdown(UserIO.getCountdownTime());
        } else {
            countDownLabel.setVisible(false);
        }

        if (dont != null) {
            if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_OK)) {
                btnOK.addMouseListener(new JDMouseAdapter() {

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        dont.setEnabled(false);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        dont.setEnabled(true);
                    }

                });
            }

            if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL)) {
                btnCancel.addMouseListener(new JDMouseAdapter() {

                    @Override
                    public void mouseEntered(MouseEvent e) {
                        dont.setEnabled(false);
                    }

                    @Override
                    public void mouseExited(MouseEvent e) {
                        dont.setEnabled(true);
                    }

                });
            }
        }
        this.invalidate();
        this.pack();
        this.setResizable(true);

        this.toFront();
        this.setMinimumSize(this.getPreferredSize());
        if (DEFAULT_DIMENSION != null) this.setSize(DEFAULT_DIMENSION);
        if (SwingGui.getInstance() == null) {
            this.setLocation(Screen.getCenterOfComponent(null, this));
        } else if (SwingGui.getInstance().getMainFrame().getExtendedState() == JFrame.ICONIFIED || !SwingGui.getInstance().getMainFrame().isVisible()) {
            this.setLocation(Screen.getDockBottomRight(this));
        } else {
            this.setLocation(Screen.getCenterOfComponent(SwingGui.getInstance().getMainFrame(), this));
        }

        KeyStroke ks = KeyStroke.getKeyStroke("ESCAPE");
        focus.getInputMap().put(ks, "ESCAPE");
        focus.getInputMap(JButton.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).put(ks, "ESCAPE");
        focus.getInputMap(JButton.WHEN_IN_FOCUSED_WINDOW).put(ks, "ESCAPE");
        focus.getActionMap().put("ESCAPE", new AbstractAction() {
            private static final long serialVersionUID = -4143073679291503041L;

            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });

        focus.requestFocus();
        this.packed();
        this.setVisible(true);

        // fixes always on top bug in windows
        /*
         * Bugdesc: found in svn
         */
        DummyFrame.getDialogParent().setAlwaysOnTop(true);
        DummyFrame.getDialogParent().setAlwaysOnTop(false);

    }

    protected void addButtons(JPanel buttonBar) {
    }

    protected String getDontShowAgainKey() {
        return "DONT_SHOW_AGAIN_" + this.toString();
    }

    /**
     * may be overwritten to set focus to special components etc.
     */
    protected void packed() {
    }

    /**
     * Could result in issues when no title is specified and default title is
     * used! So all dialogs with default title will use the same DONT_SHOW_AGAIN
     * result.
     */
    @Override
    public String toString() {
        return Encoding.filterString("dialog-" + this.getTitle()).replaceAll("\\d", "");
    }

    abstract public JComponent contentInit();

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnOK) {
            setReturnValue(true);
        } else if (e.getSource() == btnCancel) {
            setReturnValue(false);
        }
        dispose();
    }

    @Override
    protected void onCountdown() {
        setReturnValue(false);
        returnValue |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
        this.dispose();
    }

    protected void setReturnValue(boolean b) {
        returnValue = b ? UserIO.RETURN_OK : UserIO.RETURN_CANCEL;
        if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN)) {
            if (dont.isSelected() && dont.isEnabled()) {
                returnValue |= UserIO.RETURN_DONT_SHOW_AGAIN;
                SubConfiguration cfg = SubConfiguration.getConfig(DIALOGS_CONFIG);
                cfg.setProperty(getDontShowAgainKey(), returnValue);
                cfg.save();
            }
        }
    }

    public int getReturnValue() {
        return returnValue;
    }

    public static void setDefaultDimension(Dimension dimension) {
        DEFAULT_DIMENSION = dimension;
    }

    public static Dimension getDefaultDimension() {
        return DEFAULT_DIMENSION;
    }

    public static void resetDialogInformations() {
        SubConfiguration.getConfig(DIALOGS_CONFIG).getProperties().clear();
        SubConfiguration.getConfig(DIALOGS_CONFIG).save();
    }

}
