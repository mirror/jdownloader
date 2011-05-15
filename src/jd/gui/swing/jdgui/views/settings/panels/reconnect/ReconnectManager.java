package jd.gui.swing.jdgui.views.settings.panels.reconnect;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import jd.controlling.reconnect.ReconnectPluginController;
import jd.controlling.reconnect.RouterPlugin;
import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;

import org.appwork.app.gui.MigPanel;
import org.appwork.storage.StorageEvent;
import org.appwork.storage.StorageKeyAddedEvent;
import org.appwork.storage.StorageValueChangeEvent;
import org.appwork.utils.event.DefaultEventListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.images.Theme;
import org.jdownloader.translate._JDT;

public class ReconnectManager extends MigPanel implements SettingsComponent, ActionListener, DefaultEventListener<StorageEvent<?>> {
    private JComboBox combobox;
    private JButton   autoButton;
    private MigPanel  card;

    public ReconnectManager() {
        super("ins 0,wrap 2", "[grow,fill][]", "");
        initComponents();
        layoutComponents();
        fill();
        ReconnectPluginController.getInstance().getStorage().getEventSender().addListener(this);
    }

    public void onEvent(final StorageEvent<?> event) {
        boolean b = false;
        if (event instanceof StorageValueChangeEvent<?>) {
            final StorageValueChangeEvent<?> changeEvent = (StorageValueChangeEvent<?>) event;
            if (changeEvent.getKey().equals(ReconnectPluginController.PRO_ACTIVEPLUGIN)) {
                b = true;
            }
        } else if (event instanceof StorageKeyAddedEvent<?>) {
            final StorageKeyAddedEvent<?> changeEvent = (StorageKeyAddedEvent<?>) event;
            if (changeEvent.getKey().equals(ReconnectPluginController.PRO_ACTIVEPLUGIN)) {
                b = true;
            }
        }
        if (b == false) return;
        setView(((RouterPlugin) combobox.getSelectedItem()).getGUI());
    }

    public void actionPerformed(final ActionEvent e) {

        if (e.getSource() == this.autoButton) {
            ReconnectPluginController.getInstance().autoFind();
        } else {
            ReconnectPluginController.getInstance().setActivePlugin((RouterPlugin) this.combobox.getSelectedItem());
        }
    }

    private void layoutComponents() {
        this.add(this.combobox, "growx, pushx,height 20!");
        this.add(this.autoButton, "height 20!");
        this.combobox.addActionListener(this);
        add(card, "spanx,pushy,growy");
    }

    private void fill() {
        new EDTRunner() {
            @Override
            protected void runInEDT() {
                combobox.setModel(new DefaultComboBoxModel(ReconnectPluginController.getInstance().getPlugins().toArray(new RouterPlugin[] {})));
                setView(((RouterPlugin) combobox.getSelectedItem()).getGUI());
            }
        };

    }

    protected void setView(final JComponent gui) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                card.removeAll();
                card.add(gui);
            }
        };

    }

    private void initComponents() {
        this.combobox = new JComboBox();
        final ListCellRenderer org = combobox.getRenderer();
        combobox.setRenderer(new ListCellRenderer() {

            public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel ret = (JLabel) org.getListCellRendererComponent(list, ((RouterPlugin) value).getName(), index, isSelected, cellHasFocus);
                ret.setIcon(((RouterPlugin) value).getIcon16());
                return ret;
            }
        });
        this.autoButton = new JButton(_JDT._.reconnectmanager_wizard(), Theme.getIcon("wizard", 20));
        this.autoButton.addActionListener(this);

        this.card = new MigPanel("ins 0", "[grow,fill]", "[grow,fill]");

    }

    public boolean isMultiline() {
        return true;
    }

    public String getConstraints() {
        return "wmin 10,height 60:n:n,pushy,growy";
    }

}
