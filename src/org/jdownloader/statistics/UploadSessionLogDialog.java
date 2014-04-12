package org.jdownloader.statistics;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
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
    private String       errorID;
    private int          desiredWidh = 0;
    private String       dK;

    public UploadSessionLogDialog(String errorID, DownloadLink downloadLink) {
        super(UIOManager.LOGIC_COUNTDOWN | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN | UIOManager.LOGIC_DONT_SHOW_AGAIN_IGNORES_CANCEL, _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_object_title2(), new AbstractIcon(IconKey.ICON_BOTTY_STOP, -1), _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_yes(), _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_no());
        setTimeout(10 * 60 * 1000);
        this.downloadLink = downloadLink;
        this.errorID = errorID;
        dK = "UploadSessionLogDialog+" + System.currentTimeMillis();

    }

    @Override
    public String getDontShowAgainKey() {
        return dK;
    }

    @Override
    protected String getDontShowAgainLabelText() {
        return _GUI._.UploadSessionLogDialog_getDontShowAgainLabelText_always();
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredWidth() {
        return Math.max(800, desiredWidh);
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");

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

        p.add(SwingUtils.toBold(new JLabel(_GUI._.UploadSessionLogDialog_layoutDialogContent_errorid())), "split 2,sizegroup left,alignx left");
        JTextField txt;
        p.add(txt = new JTextField(errorID));
        txt.setBorder(null);
        txt.setOpaque(false);
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);

        JLabel lbl;
        desiredWidh = getDialog().getRawPreferredSize().width;
        p.add(lbl = new JLabel("<html>" + _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_object_msg2() + "<b><u>" + _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_object_more() + "</b></u>" + "</html>"), "pushx, growx,gapbottom 5");
        lbl.addMouseListener(new MouseListener() {

            @Override
            public void mouseReleased(MouseEvent e) {
            }

            @Override
            public void mousePressed(MouseEvent e) {
            }

            @Override
            public void mouseExited(MouseEvent e) {
            }

            @Override
            public void mouseEntered(MouseEvent e) {
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                CrossSystem.openURLOrShowMessage("http://board.jdownloader.org/showthread.php?p=287444");
            }
        });
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

        JTextField txt;
        txt = new JTextField(name);
        txt.setBorder(null);
        txt.setOpaque(false);
        SwingUtils.setOpaque(txt, false);
        txt.setEditable(false);

        txt.setHorizontalAlignment(SwingConstants.LEFT);
        return txt;
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
