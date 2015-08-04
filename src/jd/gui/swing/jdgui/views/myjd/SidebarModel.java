package jd.gui.swing.jdgui.views.myjd;

import javax.swing.DefaultListModel;
import javax.swing.JList;

import jd.SecondLevelLaunch;
import jd.gui.swing.jdgui.views.settings.panels.MyJDownloaderSettingsPanel;

import org.appwork.controlling.SingleReachableState;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class SidebarModel extends DefaultListModel implements GenericConfigEventListener<Object> {

    private static final long            serialVersionUID = -204494527404304349L;

    private Object                       lock             = new Object();

    private SingleReachableState         TREE_COMPLETE    = new SingleReachableState("TREE_COMPLETE");
    private final JList                  list;

    protected MyJDownloaderSettingsPanel myJDownloader;

    public SidebarModel(JList list) {
        super();
        this.list = list;
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
        SecondLevelLaunch.EXTENSIONS_LOADED.executeWhenReached(new Runnable() {

            @Override
            public void run() {
                fill();
            }
        });
    }

    private void edtAllElement(final Object element) {
        if (element == null) {
            return;
        }
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                addElement(element);
            }
        };
    }

    private MyJDownloaderSettingsPanel getMyJDownloaderPanel() {
        if (myJDownloader != null) {
            return myJDownloader;
        }

        return new EDTHelper<MyJDownloaderSettingsPanel>() {

            public MyJDownloaderSettingsPanel edtRun() {
                if (myJDownloader != null) {
                    return myJDownloader;
                }
                myJDownloader = new MyJDownloaderSettingsPanel();
                return myJDownloader;
            }
        }.getReturnValue();
    }

    public void fill() {
        new Thread("FillMyJDownloaderSideBarModel") {
            @Override
            public void run() {
                try {
                    synchronized (lock) {

                        new EDTRunner() {

                            @Override
                            protected void runInEDT() {
                                removeAllElements();
                            }
                        };

                        edtAllElement(getMyJDownloaderPanel());

                    }
                } finally {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            if (list != null) {
                                list.repaint();
                            }
                        }
                    };
                    TREE_COMPLETE.setReached();
                }
            }
        }.start();
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

    /**
     * @return the tREE_COMPLETE
     */
    public SingleReachableState getTreeCompleteState() {
        return TREE_COMPLETE;
    }

}
