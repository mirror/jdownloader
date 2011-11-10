package org.jdownloader.gui.views.linkgrabber.contextmenu;

import java.awt.event.ActionEvent;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.controlling.downloadcontroller.DownloadLinkStorable;
import jd.controlling.linkcrawler.CrawledLink;
import net.miginfocom.swing.MigLayout;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.TextAreaDialog;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;

public class OpenUrlAction extends AppAction {

    private CrawledLink link;

    public OpenUrlAction(CrawledLink link) {
        setName(_GUI._.OpenUrlAction_OpenUrlAction_());
        setIconKey("browse");
        toContextMenuAction();
        this.link = link;
    }

    public void actionPerformed(ActionEvent e) {
        DownloadLinkStorable storable = new DownloadLinkStorable(link.getDownloadLink());
        long jj = System.currentTimeMillis();
        String string = null;
        for (int i = 1; i < 10000; i++) {
            string = JSonStorage.toString(storable);
        }
        System.out.println("save:" + (System.currentTimeMillis() - jj));
        try {
            TextAreaDialog dialog = new TextAreaDialog("Output", "Outout", string) {

                @Override
                protected boolean isResizable() {
                    return true;
                }

                @Override
                public JComponent layoutDialogContent() {
                    final JPanel panel = new JPanel(new MigLayout("ins 0, wrap 1", "[grow, fill]", "[]5[]"));
                    panel.add(new JLabel(this.message));
                    this.txtArea = new JTextArea(this.def);
                    JScrollPane scrollingArea = new JScrollPane(txtArea);
                    panel.add(scrollingArea);
                    return panel;
                }

            };
            Dialog.getInstance().showDialog(dialog);
        } catch (DialogNoAnswerException e1) {
        }
    }
}
