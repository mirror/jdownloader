package jd.gui.swing.jdgui.views.settings.panels.advanced;

import java.awt.AlphaComposite;
import java.awt.Composite;
import java.awt.Graphics;
import java.awt.Graphics2D;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

import org.appwork.utils.swing.HelpNotifier;
import org.appwork.utils.swing.HelpNotifierCallbackListener;
import org.appwork.utils.swing.dialog.Dialog;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;
import org.jdownloader.translate._JDT;

public class AdvancedSettings extends AbstractConfigPanel implements DocumentListener {

    private static final long serialVersionUID = 1L;
    private JTextField        filterText;
    private String            filterHelp;
    private AdvancedTable     table;

    public String getTitle() {
        return _GUI._.gui_settings_advanced_title();
    }

    public AdvancedSettings() {
        super();
        this.addHeader(getTitle(), NewTheme.I().getIcon("advancedConfig", 32));
        this.addDescription(_JDT._.gui_settings_advanced_description());

        filterText = new JTextField() {

            private static final long serialVersionUID = 1L;

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);

                Graphics2D g2 = (Graphics2D) g;
                Composite comp = g2.getComposite();

                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.6f));
                g2.drawImage(NewTheme.I().getIcon("search", 16).getImage(), 3, 3, null);
                g2.setComposite(comp);
            }

        };

        HelpNotifier.register(filterText, new HelpNotifierCallbackListener() {

            public void onHelpNotifyShown(JComponent c) {
            }

            public void onHelpNotifyHidden(JComponent c) {
            }
        }, filterHelp = "Filter Settings");

        // filterText.setOpaque(false);
        // filterText.putClientProperty("Synthetica.opaque", Boolean.FALSE);
        // filterText.setBorder(null);
        filterText.setBorder(BorderFactory.createCompoundBorder(filterText.getBorder(), BorderFactory.createEmptyBorder(0, 20, 0, 0)));
        add(filterText, "gapleft 37,spanx,growx,pushx");
        filterText.getDocument().addDocumentListener(this);
        add(new JScrollPane(table = new AdvancedTable()));
    }

    @Override
    public ImageIcon getIcon() {
        return NewTheme.I().getIcon("advancedConfig", 20);
    }

    @Override
    public void save() {

    }

    @Override
    public void updateContents() {
        Dialog.getInstance().showMessageDialog(_JDT._.gui_settings_advanced_description());
    }

    public void insertUpdate(DocumentEvent e) {
        filter();
    }

    private void filter() {
        if (!this.filterText.getText().equals(filterHelp)) {
            table.filter(filterText.getText());
        } else {
            table.filter(null);
        }
    }

    public void removeUpdate(DocumentEvent e) {
        filter();
    }

    public void changedUpdate(DocumentEvent e) {
        filter();
    }
}