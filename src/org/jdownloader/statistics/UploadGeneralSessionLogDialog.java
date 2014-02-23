package org.jdownloader.statistics;

import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import org.appwork.swing.MigPanel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.os.CrossSystem;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class UploadGeneralSessionLogDialog extends AbstractDialog<Object> implements UploadGeneralSessionLogDialogInterface {

    private int desiredWidh = 0;

    public UploadGeneralSessionLogDialog() {
        super(UIOManager.LOGIC_COUNTDOWN, _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_object_title2(), new AbstractIcon(IconKey.ICON_BOTTY_STOP, -1), _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_yes(), _GUI._.UploadSessionLogDialog_UploadSessionLogDialog_no());
        setTimeout(5 * 60 * 1000);

    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    protected int getPreferredWidth() {
        return Math.max(600, desiredWidh);
    }

    @Override
    public JComponent layoutDialogContent() {
        final JPanel p = new MigPanel("ins 0,wrap 1", "[]", "[][]");

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

}
