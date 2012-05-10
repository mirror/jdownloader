package org.jdownloader.gui.views.downloads.context;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import jd.controlling.IOEQ;
import jd.controlling.downloadcontroller.DownloadController;
import jd.controlling.packagecontroller.AbstractNode;
import jd.plugins.DownloadLink;
import jd.plugins.FilePackage;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.DialogNoAnswerException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.DownloadFolderChooserDialog;
import org.jdownloader.gui.views.components.packagetable.LinkTreeUtils;
import org.jdownloader.translate._JDT;

public class SetDownloadFolderInDownloadTableAction extends AppAction {

    /**
     * 
     */
    private static final long                             serialVersionUID = -6632019767606316873L;

    private boolean                                       retOkay          = false;

    private HashSet<FilePackage>                          packages;
    private HashMap<FilePackage, ArrayList<DownloadLink>> newPackages;

    private FilePackage                                   pkg;

    private File                                          path;

    public boolean newValueSet() {
        return retOkay;
    }

    public SetDownloadFolderInDownloadTableAction(AbstractNode node, ArrayList<AbstractNode> selectio2) {
        if (selectio2 == null) {
            selectio2 = new ArrayList<AbstractNode>();
            selectio2.add(node);
        }

        packages = new HashSet<FilePackage>();
        HashSet<AbstractNode> map = new HashSet<AbstractNode>();
        for (AbstractNode n : selectio2) {
            map.add(n);
            if (n instanceof FilePackage) {
                if (pkg == null) pkg = (FilePackage) n;
                packages.add((FilePackage) n);
            } else {
                if (pkg == null) pkg = ((DownloadLink) n).getParentNode();
                packages.add(((DownloadLink) n).getParentNode());
            }

        }
        newPackages = new HashMap<FilePackage, ArrayList<DownloadLink>>();
        for (Iterator<FilePackage> it = packages.iterator(); it.hasNext();) {
            FilePackage next = it.next();
            boolean allChildren = true;
            boolean noChildren = true;
            ArrayList<DownloadLink> splitme = new ArrayList<DownloadLink>();
            for (DownloadLink l : next.getChildren()) {
                if (!map.contains(l)) {
                    allChildren = false;

                } else {
                    splitme.add(l);
                    noChildren = false;
                }
            }
            // if we do not have all links selected,
            if (allChildren || noChildren) {
                // keep package
            } else {
                // we might need to split packages
                it.remove();
                newPackages.put(next, splitme);

            }

        }
        setName(_GUI._.SetDownloadFolderAction_SetDownloadFolderAction_());
        path = LinkTreeUtils.getDownloadDirectory(node);
        setIconKey("save");
    }

    public void actionPerformed(ActionEvent e) {
        if (!isEnabled()) return;

        try {

            final File file = DownloadFolderChooserDialog.open(path, false, _GUI._.OpenDownloadFolderAction_actionPerformed_object_(pkg.getName()));
            if (file == null) return;

            retOkay = true;
            IOEQ.add(new Runnable() {

                public void run() {

                    for (FilePackage pkg : packages) {
                        pkg.setDownloadDirectory(file.getAbsolutePath());
                    }

                    for (final Entry<FilePackage, ArrayList<DownloadLink>> entry : newPackages.entrySet()) {

                        try {
                            File oldPath = LinkTreeUtils.getDownloadDirectory(entry.getKey().getDownloadDirectory());
                            File newPath = file;
                            if (oldPath.equals(newPath)) continue;
                            Dialog.getInstance().showConfirmDialog(Dialog.LOGIC_DONOTSHOW_BASED_ON_TITLE_ONLY | Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN,

                            _JDT._.SetDownloadFolderAction_actionPerformed_(entry.getKey().getName()), _JDT._.SetDownloadFolderAction_msg(entry.getKey().getName(), entry.getValue().size()), null, _JDT._.SetDownloadFolderAction_yes(), _JDT._.SetDownloadFolderAction_no());
                            entry.getKey().setDownloadDirectory(file.getAbsolutePath());
                            continue;
                        } catch (DialogClosedException e) {
                            return;
                        } catch (DialogCanceledException e) {
                            /* user clicked no */
                        }

                        final FilePackage pkg = FilePackage.getInstance();
                        pkg.setExpanded(true);
                        pkg.setCreated(System.currentTimeMillis());
                        pkg.setName(entry.getKey().getName());
                        pkg.setComment(entry.getKey().getComment());
                        pkg.setPasswordList(new ArrayList<String>(Arrays.asList(entry.getKey().getPasswordList())));
                        pkg.getProperties().putAll(entry.getKey().getProperties());
                        pkg.setDownloadDirectory(file.getAbsolutePath());
                        IOEQ.getQueue().add(new QueueAction<Object, RuntimeException>() {
                            @Override
                            protected Object run() {
                                DownloadController.getInstance().moveOrAddAt(pkg, entry.getValue(), -1);
                                return null;
                            }
                        });
                    }
                }

            });
        } catch (DialogNoAnswerException e1) {
        }

    }

    @Override
    public boolean isEnabled() {
        return newPackages.size() > 0 || packages.size() > 0;
    }

}
