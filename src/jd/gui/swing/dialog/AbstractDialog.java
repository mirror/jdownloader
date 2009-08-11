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
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
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
import jd.nutils.JDHash;
import jd.nutils.Screen;
import jd.nutils.encoding.Encoding;
import jd.utils.locale.JDL;
import net.miginfocom.swing.MigLayout;

public abstract class AbstractDialog extends JCountdownDialog implements ActionListener {

    private static final long serialVersionUID = -169149552591067268L;

    private static final String DIALOGS_CONFIG = "DIALOGS";

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

    private JLabel dontlabel;

    public AbstractDialog(int flag, String title, ImageIcon icon, String okOption, String cancelOption) {
        super(DummyFrame.getDialogParent());

        this.flag = flag;
        setTitle(title);

        this.icon = (JDFlags.hasAllFlags(flag, UserIO.NO_ICON)) ? null : icon;
        this.okOption = (okOption == null) ? JDL.L("gui.btn_ok", null) : okOption;
        this.cancelOption = (cancelOption == null) ? JDL.L("gui.btn_cancel", null) : cancelOption;

    }

    public void init() {
        dont:if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN)) {
            SubConfiguration cfg = SubConfiguration.getConfig(DIALOGS_CONFIG);
            // System.out.println(cfg+toString()+"This restore" +
            // "DONT_SHOW_AGAIN_" + JDHash.getMD5(this.toString()) + " " +
            // cfg.getProperty("DONT_SHOW_AGAIN_" +
            // JDHash.getMD5(this.toString())));
            Object value;
            if ((value = cfg.getProperty("DONT_SHOW_AGAIN_" + this.toString())) != null) {
                if (value instanceof Integer) {
                    int i = ((Integer) value).intValue();
                    int ret = (i & (UserIO.RETURN_OK | UserIO.RETURN_CANCEL)) | UserIO.RETURN_DONT_SHOW_AGAIN | UserIO.RETURN_SKIPPED_BY_DONT_SHOW;
                    
                    //return if the stored values are excluded
                    if(JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL)&&JDFlags.hasAllFlags(flag, UserIO.RETURN_CANCEL)){
                      break dont;  
                    }
                    if(JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_OK)&&JDFlags.hasAllFlags(ret, UserIO.RETURN_OK)){
                        break dont;
                    }
                    
                    this.returnValue = ret;
                
                }
                return;

            }

        }

        this.setModal(true);

        this.setLayout(new MigLayout("ins 5", "[fill,grow]", "[fill,grow][]"));

        this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        btnOK = new JButton(this.okOption);
        JButton focus = btnOK;

        btnOK.addActionListener(this);
        btnCancel = new JButton(this.cancelOption);
        btnCancel.addActionListener(this);
        if (icon != null) {
            add(new JLabel(this.icon), "split 2,alignx left,aligny center,shrinkx,gapright 10");
        }
        contentpane = contentInit();
        add(contentpane, "pushx,growx,pushy,growy,spanx,aligny center,wrap");

        add(this.countDownLabel, "split 4,growx");

        if ((flag & UserIO.DONT_SHOW_AGAIN) > 0) {
            dont = new JCheckBox();

            dont.setHorizontalAlignment(JCheckBox.TRAILING);

            add(dontlabel=new JLabel(JDL.L("gui.dialogs.dontshowthisagain", "Don't show this again")));
            add(dont, "alignx right");
        }

        if ((flag & UserIO.NO_OK_OPTION) == 0) {

            // this.getRootPane().setDefaultButton(btnOK);
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
            add(btnOK, "alignx right");
        }
        if ((flag & UserIO.NO_CANCEL_OPTION) == 0) {

            add(btnCancel, "alignx right");
            if ((flag & UserIO.NO_OK_OPTION) != 0) {
                this.getRootPane().setDefaultButton(btnCancel);
                btnCancel.requestFocusInWindow();
                focus = btnCancel;
            }
        }
        this.setMinimumSize(new Dimension(300, -1));

        if (JDFlags.hasNoFlags(flag, UserIO.NO_COUNTDOWN)) {
            this.countdown(UserIO.getCountdownTime());
        } else {
            countDownLabel.setVisible(false);
        }
        if(dont!=null){
        btnOK.addMouseListener(new JDMouseAdapter(){        

            public void mouseEntered(MouseEvent e) {
                if(JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_OK)){
                   dont.setEnabled(false);
                   dontlabel.setEnabled(false);
                }
                
            }
            public void mouseExited(MouseEvent e) {
                dont.setEnabled(true);
                dontlabel.setEnabled(true);
            }
          
            
        });
        
        btnCancel.addMouseListener(new JDMouseAdapter(){        

            public void mouseEntered(MouseEvent e) {
                if(JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN_IGNORES_CANCEL)){
                    dont.setEnabled(false);
                    dontlabel.setEnabled(false);
                }
                
            }
            public void mouseExited(MouseEvent e) {
                dont.setEnabled(true);
                dontlabel.setEnabled(true);
            }
          
            
        });
        }
        this.setAlwaysOnTop(true);
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

    }

    protected void processKeyEvent(KeyEvent e) {
        super.processKeyEvent(e);

    }

    /**
     * may be overwritten to set focus to special components etc.
     */
    protected void packed() {
    }

    public String toString() {
        return Encoding.filterString("dialog-" + this.getTitle());
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

    // @Override
    protected void onCountdown() {

        setReturnValue(false);
        returnValue |= UserIO.RETURN_COUNTDOWN_TIMEOUT;
        this.dispose();
    }

    protected void setReturnValue(boolean b) {
        returnValue = b ? UserIO.RETURN_OK : UserIO.RETURN_CANCEL;
        if (JDFlags.hasAllFlags(flag, UserIO.DONT_SHOW_AGAIN)) {

            if (dont.isSelected()&&dont.isEnabled()) {
             
                returnValue |= UserIO.RETURN_DONT_SHOW_AGAIN;
                SubConfiguration cfg = SubConfiguration.getConfig(DIALOGS_CONFIG);
                cfg.setProperty("DONT_SHOW_AGAIN_" + (this.toString()), returnValue);
                cfg.save();
           
                // System.out.println(cfg+toString()+" This save" +
                // "DONT_SHOW_AGAIN_" + JDHash.getMD5(this.toString()) + " " +
                // returnValue);

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

}
