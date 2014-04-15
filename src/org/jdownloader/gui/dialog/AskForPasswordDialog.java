package org.jdownloader.gui.dialog;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
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
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.AbstractIcon;

public class AskForPasswordDialog extends InputDialog implements AskDownloadPasswordDialogInterface {
    private DownloadLink downloadLink;

    public AskForPasswordDialog(String message, DownloadLink link) {
        super(UIOManager.LOGIC_COUNTDOWN, _GUI._.AskForPasswordDialog_AskForPasswordDialog_title_(), message, null, new AbstractIcon(IconKey.ICON_PASSWORD, 32), _GUI._.lit_continue(), null);
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

        String packagename = downloadLink.getParentNode().getName();
        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_filename())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(downloadLink.getView().getDisplayName()));
        p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(packagename));
        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_hoster())), "split 2,sizegroup left,alignx left");
        DomainInfo di = downloadLink.getDomainInfo();
        JLabel ret = new JLabel(di.getTld());
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        ret.setIcon(di.getFavIcon());
        p.add(ret);

        input = getSmallInputComponent();
        // this.input.setBorder(BorderFactory.createEtchedBorder());
        input.setText(defaultMessage);
        p.add(SwingUtils.toBold(new JLabel(_GUI._.ExtractionListenerList_layoutDialogContent_password())), "split 2,sizegroup left,alignx left");
        p.add((JComponent) input, "w 450,pushx,growx");
        getDialog().addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowLostFocus(WindowEvent e) {
            }

            @Override
            public void windowGainedFocus(WindowEvent e) {
                ArrayList<AbstractNode> links = new ArrayList<AbstractNode>();

                links.add(downloadLink);
                SelectionInfo<FilePackage, DownloadLink> si = new SelectionInfo<FilePackage, DownloadLink>(null, links, true);

                ArrayList<AbstractNode> corrected = new ArrayList<AbstractNode>();
                for (PackageView<FilePackage, DownloadLink> pv : si.getPackageViews()) {
                    if (pv.isFull()) {
                        corrected.add(pv.getPackage());
                    }
                    corrected.addAll(pv.getChildren());
                }

                DownloadsTableModel.getInstance().setSelectedObjects(corrected);
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
        return downloadLink.getHost();
    }

    @Override
    public String getPackageName() {
        return downloadLink.getParentNode().getName();
    }
}
