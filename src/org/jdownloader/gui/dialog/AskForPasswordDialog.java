package org.jdownloader.gui.dialog;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Font;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.BinaryLogic;
import org.appwork.utils.StringUtils;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.InputDialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.AbstractIcon;

public class AskForPasswordDialog extends InputDialog implements AskDownloadPasswordDialogInterface {
    private DownloadLink downloadLink;

    public AskForPasswordDialog(String message, DownloadLink link) {
        super(UIOManager.LOGIC_COUNTDOWN, _GUI.T.AskForPasswordDialog_AskForPasswordDialog_title_(), message, null, new AbstractIcon(IconKey.ICON_PASSWORD, 32), _GUI.T.lit_continue(), null);
        this.downloadLink = link;
        setTimeout(10 * 60 * 1000);
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");
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
            final Font font = textField.getFont();
            textField.setContentType("text/plain");
            textField.setFont(font);
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
        String packagename = downloadLink.getParentNode().getName();
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.lit_filename())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(downloadLink.getView().getDisplayName()));
        if (downloadLink.getParentNode() != FilePackage.getDefaultFilePackage()) {
            p.add(SwingUtils.toBold(new JLabel(_GUI.T.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
            p.add(leftLabel(packagename));
        }
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.lit_hoster())), "split 2,sizegroup left,alignx left");
        DomainInfo di = downloadLink.getDomainInfo();
        JLabel ret = new JLabel(di.getTld());
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        ret.setIcon(di.getFavIcon());
        p.add(ret);
        input = getSmallInputComponent();
        // this.input.setBorder(BorderFactory.createEtchedBorder());
        input.setText(defaultMessage);
        p.add(SwingUtils.toBold(new JLabel(_GUI.T.ExtractionListenerList_layoutDialogContent_password())), "split 2,sizegroup left,alignx left");
        p.add((JComponent) input, "w 450,pushx,growx");
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
        return p;
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
        return downloadLink.getDomainInfo().getTld();
    }

    @Override
    public String getPackageName() {
        if (downloadLink.getParentNode() == FilePackage.getDefaultFilePackage()) {
            return null;
        }
        return downloadLink.getParentNode().getName();
    }
}
