package jd.gui.swing.jdgui.views.settings.panels.extensionmanager;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Icon;
import javax.swing.JScrollPane;

import jd.SecondLevelLaunch;

import org.appwork.scheduler.DelayedRunnable;
import org.jdownloader.extensions.ExtensionController;
import org.jdownloader.extensions.ExtensionControllerListener;
import org.jdownloader.extensions.Header;
import org.jdownloader.extensions.OptionalExtension;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.translate._JDT;

public class OptionalExtensionSettings extends AbstractConfigPanel implements ExtensionControllerListener {
    private static final long             serialVersionUID = 2L;
    private final OptionalExtensionsTable table;
    private final AtomicBoolean           showFlag         = new AtomicBoolean(true);

    public String getTitle() {
        return _GUI.T.extensionManager_title();
    }

    public OptionalExtensionSettings() {
        super();
        add(new Header(getTitle(), new AbstractIcon(IconKey.ICON_EXTENSIONMANAGER, 32)), "spanx,growx,pushx");
        table = new OptionalExtensionsTable();
        final JScrollPane sp = new JScrollPane(table);
        this.add(sp, "gapleft 37,growx, pushx,spanx,pushy,growy");
    }

    @Override
    public Icon getIcon() {
        return new AbstractIcon(IconKey.ICON_EXTENSION, 32);
    }

    @Override
    public void save() {
    }

    @Override
    public void updateContents() {
    }

    public String getName() {
        return getTitle();
    }

    public String getDescription() {
        return _JDT.T.gui_settings_linkgrabber_packagizer_description();
    }

    @Override
    protected void onShow() {
        super.onShow();
        showFlag.set(true);
        SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(new Runnable() {
            @Override
            public void run() {
                if (showFlag.get()) {
                    ExtensionController.getInstance().getEventSender().addListener(OptionalExtensionSettings.this);
                    onUpdated();
                }
            }
        });
    }

    @Override
    protected void onHide() {
        super.onHide();
        if (showFlag.compareAndSet(true, false)) {
            SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(new Runnable() {
                @Override
                public void run() {
                    ExtensionController.getInstance().getEventSender().removeListener(OptionalExtensionSettings.this);
                }
            });
        }
    }

    @Override
    public void onUpdated() {
        new DelayedRunnable(50, 150) {
            @Override
            public void delayedrun() {
                table.getModel()._fireTableStructureChanged(new ArrayList<OptionalExtension>(ExtensionController.getInstance().getOptionalExtensions()), true);
            }
        }.resetAndStart();
    }
}
