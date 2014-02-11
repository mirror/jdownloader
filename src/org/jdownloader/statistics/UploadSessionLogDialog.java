package org.jdownloader.statistics;

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
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.DomainInfo;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.SelectionInfo.PackageView;
import org.jdownloader.gui.views.downloads.table.DownloadsTableModel;
import org.jdownloader.images.AbstractIcon;

public class UploadSessionLogDialog extends AbstractDialog<Object> implements UploadSessionLogDialogInterface {

    private DownloadLink downloadLink;

    public UploadSessionLogDialog(DownloadLink downloadLink) {
        super(UIOManager.LOGIC_COUNTDOWN, _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_object_title(), new AbstractIcon(IconKey.ICON_ERROR, 32), _GUI._.lit_yes(), _GUI._.lit_no());
        setTimeout(5 * 60 * 1000);
        this.downloadLink = downloadLink;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredWidth() {
        return 550;
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");

        JTextPane textField = new JTextPane() {
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

        textField.setText(_GUI._.UploadSessionLogDialog_UploadSessionLogDialog_object_msg());
        textField.setEditable(false);
        textField.setBackground(null);
        textField.setOpaque(false);
        textField.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        textField.setCaretPosition(0);

        // inout dialog can become too large(height) if we do not limit the
        // prefered textFIled size here.
        textField.setPreferredSize(textField.getPreferredSize());

        String packagename = downloadLink.getParentNode().getName();
        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_filename())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(downloadLink.getName()));
        p.add(SwingUtils.toBold(new JLabel(_GUI._.IfFileExistsDialog_layoutDialogContent_package())), "split 2,sizegroup left,alignx left");
        p.add(leftLabel(packagename));
        p.add(SwingUtils.toBold(new JLabel(_GUI._.lit_hoster())), "split 2,sizegroup left,alignx left");
        DomainInfo di = downloadLink.getDomainInfo();
        JLabel ret = new JLabel(di.getTld());
        ret.setHorizontalAlignment(SwingConstants.LEFT);
        ret.setIcon(di.getFavIcon());
        p.add(ret);
        p.add(textField, "pushx, growx");
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

    @Override
    protected void setReturnmask(boolean b) {
        super.setReturnmask(b);

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
    public String getLinkName() {
        return null;
    }

    @Override
    public long getLinkID() {
        return downloadLink.getUniqueID().getID();
    }

    @Override
    public String getPackageName() {
        return downloadLink.getParentNode().getName();
    }

    @Override
    public String getHost() {
        return downloadLink.getDomainInfo().getTld();
    }

}
