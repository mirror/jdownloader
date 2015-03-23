package org.jdownloader.extensions.eventscripter;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.gui.swing.jdgui.views.settings.panels.proxy.ProxyDeleteAction;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.ExtButton;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.actions.AppAction;
import org.jdownloader.controlling.contextmenu.gui.ExtPopupMenu;
import org.jdownloader.extensions.ExtensionConfigPanel;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.components.AbstractAddAction;
import org.jdownloader.gui.views.components.AbstractRemoveAction;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.updatev2.gui.LAFOptions;

public class EventScripterConfigPanel extends ExtensionConfigPanel<EventScripterExtension> {

    /**
     *
     */
    private static final long       serialVersionUID = 1L;
    private EventScripterTableModel model;
    private ExtButton               btnAdd;
    private EventScripterTable      table;
    private AbstractRemoveAction    deleteSelectionAction;

    public EventScripterConfigPanel(EventScripterExtension extension) {
        super(extension);

        JLabel lbl;

        add(new JScrollPane(table = new EventScripterTable(model = new EventScripterTableModel(extension)) {
            @Override
            protected boolean onShortcutDelete(List<ScriptEntry> selectedObjects, KeyEvent evt, boolean direct) {
                deleteSelectionAction.actionPerformed(null);
                return true;
            }
        }));
        MigPanel toolbar = new MigPanel("ins 0", "0[][][][grow,fill]", "[]");
        toolbar.setOpaque(false);
        btnAdd = new ExtButton(new AbstractAddAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                ScriptEntry newScript = new ScriptEntry();
                getExtension().addScriptEntry(newScript);
            }

        });

        ProxyDeleteAction dl;
        ExtButton btnRemove = new ExtButton(deleteSelectionAction = new AbstractRemoveAction() {

            @Override
            public void actionPerformed(ActionEvent e) {
                List<ScriptEntry> entries = model.getSelectedObjects();
                if (entries == null || entries.size() == 0) {
                    return;
                }
                try {
                    Dialog.getInstance().showConfirmDialog(0, _GUI._.lit_are_you_sure(), org.jdownloader.extensions.eventscripter.T._.sure_delete_entries(entries.size()), new AbstractIcon(IconKey.ICON_QUESTION, 32), null, null);
                    getExtension().removeScriptEntries(entries);
                } catch (DialogClosedException e1) {
                    e1.printStackTrace();
                } catch (DialogCanceledException e1) {
                    e1.printStackTrace();
                }

            }

        });
        final ExtButton btnDefault = new ExtButton(new AppAction() {
            {
                setIconKey(IconKey.ICON_WIZARD);
                setName(T._.btnExampleScripts());
            }

            @Override
            public void actionPerformed(ActionEvent e) {
                ExtPopupMenu p = new ExtPopupMenu();
                ArrayList<ExampleMenuEntry> lst = new ArrayList<ExampleMenuEntry>();
                lst.add(new ExampleMenuEntry(getExtension(), "infofile.js", EventTrigger.ON_PACKAGE_FINISHED, T._.example_infoFile()));
                lst.add(new ExampleMenuEntry(getExtension(), "playsound.js", EventTrigger.ON_DOWNLOAD_CONTROLLER_STOPPED, T._.example_play_sound()));
                java.util.Collections.sort(lst, new Comparator<ExampleMenuEntry>() {

                    @Override
                    public int compare(ExampleMenuEntry o1, ExampleMenuEntry o2) {
                        int ret = o1.getName().compareTo(o2.getName());
                        return ret;
                    }
                });
                for (ExampleMenuEntry ee : lst) {
                    p.add(ee);
                }

                if (e.getSource() instanceof Component) {
                    Component button = (Component) e.getSource();
                    Dimension prefSize = p.getPreferredSize();
                    int[] insets = LAFOptions.getInstance().getPopupBorderInsets();
                    p.show(button, -insets[1], -insets[0] - prefSize.height);

                }
            }
        });

        toolbar.add(btnAdd, "sg 1,height 26!");
        toolbar.add(btnRemove, "sg 1,height 26!");
        toolbar.add(btnDefault, "sg 1,height 26!");

        toolbar.add(Box.createHorizontalGlue(), "pushx,growx");
        //
        // toolbar.add(btImport, "sg 2,height 26!");
        // toolbar.add(impPopup, "height 26!,width 12!,aligny top");
        //
        // toolbar.add(btExport, "sg 2,height 26!");
        // toolbar.add(expPopup, "height 26!,width 12!,aligny top");

        add(toolbar, "gapleft 32,growx,spanx");

    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {

    }
}
