package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import jd.controlling.downloadcontroller.FilePackageStorable;
import jd.gui.swing.jdgui.interfaces.ContextMenuAction;
import jd.plugins.DownloadLink;
import net.miginfocom.swing.MigLayout;

import org.appwork.exceptions.WTFException;
import org.appwork.storage.JSonStorage;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.appwork.utils.swing.dialog.TextAreaDialog;
import org.jdownloader.gui.translate._GUI;

public class CreateDLCAction extends ContextMenuAction {

    private static final long             serialVersionUID = 7244681674979415222L;

    private final ArrayList<DownloadLink> links;

    public CreateDLCAction(ArrayList<DownloadLink> links) {
        this.links = links;

        init();
    }

    @Override
    protected String getIcon() {
        return "dlc";
    }

    @Override
    protected String getName() {
        return _GUI._.gui_table_contextmenu_dlc() + " (" + links.size() + ")";
    }

    public void actionPerformed(ActionEvent e) {
        // File[] files =
        // UserIO.getInstance().requestFileChooser("_LOADSAVEDLC", null, null,
        // new JDFileFilter(null,
        // ContainerPluginController.getInstance().getContainerExtensions("dlc"),
        // true), null, null, UserIO.SAVE_DIALOG);
        // if (files == null) return;
        //
        // JDUtilities.getController().saveDLC(files[0], links);
        //
        Log.exception(new WTFException("TODO"));
        FilePackageStorable storable = new FilePackageStorable(links.get(0).getFilePackage());
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