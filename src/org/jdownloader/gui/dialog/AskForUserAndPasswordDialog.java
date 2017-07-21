package org.jdownloader.gui.dialog;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtPasswordField;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.locale._AWU;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;

public class AskForUserAndPasswordDialog extends InputDialog implements AskUsernameAndPasswordDialogInterface {
    private final DownloadLink downloadLink;
    private ExtPasswordField   password;
    private JCheckBox          save;
    private Color              titleColor;

    public AskForUserAndPasswordDialog(String message, DownloadLink link) {
        super(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_HIDE_ICON, _GUI.T.AskForUserAndPasswordDialog_AskForUserAndPasswordDialog_title_(), message, null, null, _GUI.T.lit_continue(), null);
        this.downloadLink = link;
        setTimeout(10 * 60 * 1000);
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");
        titleColor = Color.DARK_GRAY;
        if (!StringUtils.isEmpty(message)) {
            textField = new JTextPane() {
                private static final long serialVersionUID = 1L;

                @Override
                public boolean getScrollableTracksViewportWidth() {
                    return !BinaryLogic.containsAll(flagMask, Dialog.STYLE_LARGE);
                }

                public boolean getScrollableTracksViewportHeight() {
                    return true;
                }
            };
            textField.setContentType("text/plain");
            textField.setText(message);
            textField.setEditable(false);
            textField.setBackground(null);
            textField.setOpaque(false);
            textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
            textField.setCaretPosition(0);
            p.add(textField, "pushx, growx");
            // inout dialog can become too large(height) if we do not limit the
            // prefered textFIled size here.
            textField.setPreferredSize(textField.getPreferredSize());
        }
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.lit_filename())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(downloadLink.getView().getDisplayName()));
        final String packagename = getPackageName();
        if (StringUtils.isNotEmpty(packagename)) {
            p.add(SwingUtils.toBold(new JLabel(_GUI.T.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
            p.add(leftLabel(packagename));
        }
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.lit_hoster())), "split 2,sizegroup left,alignx left");
        final DomainInfo di = downloadLink.getDomainInfo();
        JLabel ret = new JLabel(di.getTld());
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        ret.setIcon(di.getFavIcon());
        p.add(ret);
        input = getSmallInputComponent();
        // this.input.setBorder(BorderFactory.createEtchedBorder());
        input.setText(defaultMessage);
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.lit_username())), "split 2,sizegroup left,alignx left");
        p.add((JComponent) input, "w 450,pushx,growx");
        password = new ExtPasswordField();
        password.addKeyListener(this);
        password.addMouseListener(this);
        save = new JCheckBox();
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.lit_password())), "split 2,sizegroup left,alignx left");
        p.add(password, "w 450,pushx,growx");
        p.add(addSettingName(_AWU.T.AccountNew_layoutDialogContent_save()));
        p.add(save, "sizegroup g1");
        if (StringUtils.isNotEmpty(packagename)) {
            getDialog().addWindowFocusListener(new WindowFocusListener() {
                @Override
                public void windowLostFocus(WindowEvent e) {
                }

                @Override
                public void windowGainedFocus(WindowEvent e) {
                    final ArrayList<AbstractNode> selection = new ArrayList<AbstractNode>();
                    selection.add(downloadLink);
                    DownloadsTableModel.getInstance().setSelectedObjects(selection);
                }
            });
        }
        return p;
    }

    private JLabel addSettingName(final String name) {
        final JLabel lbl = new JLabel(name);
        lbl.setForeground(titleColor);
        return lbl;
    }

    private Component leftLabel(String name) {
        JLabel ret = new JLabel(name);
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        return ret;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public long getLinkID() {
        return downloadLink.getUniqueID().getID();
    }

    @Override
    public String getLinkName() {
        return downloadLink.getView().getDisplayName();
    }

    @Override
    public String getLinkHost() {
        return downloadLink.getHost();
    }

    @Override
    public String getPackageName() {
        final FilePackage parent = downloadLink.getParentNode();
        if (FilePackage.isDefaultFilePackage(parent) || parent == null) {
            return null;
        }
        return parent.getName();
    }

    @Override
    public String getUsername() {
        return new EDTHelper<String>() {
            @Override
            public String edtRun() {
                return input.getText();
            }
        }.getReturnValue();
    }

    @Override
    public String getPassword() {
        return new EDTHelper<String>() {
            @Override
            public String edtRun() {
                return password.getText();
            }
        }.getReturnValue();
    }

    @Override
    public boolean isRememberSelected() {
        if ((getReturnmask() & (Dialog.RETURN_OK | Dialog.RETURN_TIMEOUT)) == 0) {
            return false;
        }
        // return new LoginData(accid.getText(), new String(pass.getPassword()), );
        return new EDTHelper<Boolean>() {
            @Override
            public Boolean edtRun() {
                return save.isSelected();
            }
        }.getReturnValue() == Boolean.TRUE;
    }
}
