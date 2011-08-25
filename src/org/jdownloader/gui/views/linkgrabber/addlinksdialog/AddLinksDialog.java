package org.jdownloader.gui.views.linkgrabber.addlinksdialog;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.config.JsonConfig;
import org.appwork.swing.components.ExtTextArea;
import org.appwork.swing.components.searchcombo.SearchComboBox;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class AddLinksDialog extends AbstractDialog<CrawlerJob> {

    private ExtTextArea            input;
    private JTextArea              descriptionField;
    private SearchComboBox<String> destination;
    private JButton                browseButton;

    public AddLinksDialog() {
        super(0, _GUI._.AddLinksDialog_AddLinksDialog_(), null, _GUI._.AddLinksDialog_AddLinksDialog_confirm(), null);
    }

    @Override
    protected CrawlerJob createReturnValue() {
        return null;
    }

    public void pack() {
        this.getDialog().pack();
        // descriptionField.setPreferredSize(descriptionField.getSize());

        // this.getDialog().setMinimumSize(this.getDialog().getPreferredSize());

    }

    @Override
    public JComponent layoutDialogContent() {

        MigPanel p = new MigPanel("ins 0,wrap 2", "[][grow,fill]", "[shrink][fill,grow]");
        descriptionField = new JTextArea();
        descriptionField.setEditable(false);

        descriptionField.setFocusable(false);
        descriptionField.setEnabled(false);
        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isAddDialogHelpTextVisible()) {
            descriptionField.setText(_GUI._.AddLinksDialog_layoutDialogContent_description());
        }
        destination = new SearchComboBox<String>() {

            @Override
            protected Icon getIcon(String value) {
                return null;
            }

            @Override
            protected String getText(String value) {
                return value;
            }
        };

        ArrayList<String> history = JsonConfig.create(LinkgrabberSettings.class).getDownloadDestinationHistory();
        if (history != null) destination.setList(history);
        descriptionField.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(MouseEvent e) {
                JPopupMenu popup = new JPopupMenu();
                JMenuItem item = new JMenuItem(_GUI._.AddLinksDialog_mouseClicked(), NewTheme.I().getIcon("cancel", 16));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        JsonConfig.create(GraphicalUserInterfaceSettings.class).setAddDialogHelpTextVisible(!JsonConfig.create(GraphicalUserInterfaceSettings.class).isAddDialogHelpTextVisible());
                        if (JsonConfig.create(GraphicalUserInterfaceSettings.class).isAddDialogHelpTextVisible()) {
                            descriptionField.setText(_GUI._.AddLinksDialog_layoutDialogContent_description());
                        } else {
                            descriptionField.setText("");
                        }
                    }
                });
                popup.add(item);
                popup.show(descriptionField, e.getX(), e.getY());
            }

        });
        SwingUtils.setOpaque(descriptionField, false);
        input = new ExtTextArea();
        // input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setHelpText(_GUI._.AddLinksDialog_layoutDialogContent_input_help());
        browseButton = new JButton(_GUI._.literally_browse());

        p.add(descriptionField, "spanx,pushx,growx,gapleft 37,wmin 10");

        p.add(createIconLabel("linkgrabber", null), "aligny top,height 32!,width 32!");
        p.add(new JScrollPane(input), "height 30:100:n");
        p.add(createIconLabel("save", _GUI._.AddLinksDialog_layoutDialogContent_save_tt()), "aligny top,height 32!,width 32!");
        p.add(destination, "split 2,pushx,growx");
        p.add(browseButton);
        return p;
    }

    @Override
    protected boolean isIgnoreSizeLimitations() {
        return true;
    }

    @Override
    protected int getPreferredWidth() {
        return 600;
    }

    protected boolean isResizable() {
        return true;
    }

    public static void main(String[] args) {
        AddLinksDialog d = new AddLinksDialog();
        try {
            Dialog.getInstance().showDialog(d);
        } catch (DialogClosedException e) {
            e.printStackTrace();
        } catch (DialogCanceledException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    private Component createIconLabel(String iconKey, String tooltip) {
        JLabel ret = new JLabel(NewTheme.I().getIcon(iconKey, 32));
        ret.setToolTipText(tooltip);
        return ret;
    }

}
