package jd.gui.swing.jdgui.views.myjd;

import java.util.ArrayList;

import javax.swing.DefaultListModel;

import jd.gui.swing.jdgui.views.myjd.panels.MyJDownloaderAccount;
import jd.gui.swing.jdgui.views.myjd.panels.MyJDownloaderSettingsPanelForTab;
import jd.gui.swing.jdgui.views.myjd.panels.RemoteControlPanel;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.gui.settings.AbstractConfigPanel;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SidebarModel extends DefaultListModel implements GenericConfigEventListener<Object> {

    private static final long  serialVersionUID = -204494527404304349L;

    private final Object       LOCK             = new Object();

    private MyJDownloaderPanel owner;

    public SidebarModel(MyJDownloaderPanel owner) {

        super();
        this.owner = owner;
        // this.list = list;
        GenericConfigEventListener<Boolean> listener = new GenericConfigEventListener<Boolean>() {

            @Override
            public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        fireContentsChanged(this, 0, size() - 1);
                    }
                };
            }

            @Override
            public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
            }
        };
        org.jdownloader.settings.staticreferences.CFG_LINKFILTER.LINK_FILTER_ENABLED.getEventSender().addListener(listener);
        org.jdownloader.settings.staticreferences.CFG_PACKAGIZER.PACKAGIZER_ENABLED.getEventSender().addListener(listener);

    }

    public void fill() {
        new Thread("FillMyJDownloaderSideBarModel") {
            @Override
            public void run() {
                synchronized (LOCK) {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            removeAllElements();
                        }
                    };
                    final ArrayList<AbstractConfigPanel> lst = new ArrayList<AbstractConfigPanel>();
                    lst.add(new MyJDownloaderSettingsPanelForTab());
                    lst.add(new MyJDownloaderAccount());
                    lst.add(new RemoteControlPanel());
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            owner.onBeforeModelUpdate();
                            for (AbstractConfigPanel c : lst) {
                                addElement(c);
                            }
                            owner.onAfterModelUpdate();
                        }
                    };
                }
            }
        }.start();
    }

    protected void onAfterUpdate() {
    }

    protected void onBeforeUpdate() {
    }

    public void onConfigValidatorError(KeyHandler<Object> keyHandler, Object invalidValue, ValidationException validateException) {
    }

    public void onUpdated() {
        if (!JsonConfig.create(GraphicalUserInterfaceSettings.class).isMyJDownloaderViewVisible()) {
            return;
        }
        fill();
    }

    @Override
    public void onConfigValueModified(KeyHandler<Object> keyHandler, Object newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                fireContentsChanged(this, 0, size() - 1);
            }
        };
    }

    // /**
    // * @return the tREE_COMPLETE
    // */
    // public SingleReachableState getTreeCompleteState() {
    // return TREE_COMPLETE;
    // }

}
