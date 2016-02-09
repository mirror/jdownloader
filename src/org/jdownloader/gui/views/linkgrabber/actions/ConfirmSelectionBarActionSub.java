package org.jdownloader.gui.views.linkgrabber.actions;

import jd.controlling.TaskQueue;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.CrawledPackage;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.swing.exttable.ExtTableEvent;
import org.appwork.swing.exttable.ExtTableListener;
import org.appwork.swing.exttable.ExtTableModelEventWrapper;
import org.appwork.swing.exttable.ExtTableModelListener;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTable;
import org.jdownloader.gui.views.linkgrabber.LinkGrabberTableModel;
import org.jdownloader.gui.views.linkgrabber.contextmenu.ConfirmLinksContextAction;
import org.jdownloader.settings.staticreferences.CFG_LINKGRABBER;

public class ConfirmSelectionBarActionSub extends ConfirmLinksContextAction implements ActionContext, ExtTableListener, ExtTableModelListener, GenericConfigEventListener<Boolean> {

    private DelayedRunnable delayer;

    /**
     * @return
     */
    protected String getTextForForcedAll() {
        return _GUI.T.ConfirmSelectionBarAction_sub_button_add_and_force_all();
    }

    /**
     * @return
     */
    protected String getTextForForcedSelectionOnly() {
        return _GUI.T.ConfirmSelectionBarAction_sub_button_add_and_force_selection();
    }

    /**
     * @return
     */
    protected String getTextForNoAutoStartAll() {
        return _GUI.T.ConfirmSelectionBarAction_sub_button_add_all();
    }

    /**
     * @return
     */
    protected String getTextForNoAutoStartSelectionOnly() {
        return _GUI.T.ConfirmSelectionBarAction_sub_button_add_selection();
    }

    /**
     * @return
     */
    protected String getTextForAutoStartAll() {
        return _GUI.T.ConfirmSelectionBarAction_sub_button_add_and_start_all();
    }

    /**
     * @return
     */
    protected String getTextForAutoStartSelectionOnly() {
        return _GUI.T.ConfirmSelectionBarAction_sub_button_add_and_start_selection();
    }

    public ConfirmSelectionBarActionSub() {
        super();
        delayer = new DelayedRunnable(TaskQueue.TIMINGQUEUE, 500, 1000) {

            @Override
            public void delayedrun() {
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        update();
                    }
                };

            }
        };

    }

    @Override
    public void requestUpdate(Object requestor) {
        super.requestUpdate(requestor);
        updateListeners();
        update();
    }

    @Override
    public void setSelectionOnly(boolean selectionOnly) {
        super.setSelectionOnly(selectionOnly);
        updateListeners();
        update();
    }

    protected void updateListeners() {
        if (isSelectionOnly()) {
            LinkGrabberTableModel.getInstance().getEventSender().removeListener(this);
            LinkGrabberTable.getInstance().getEventSender().addListener(this, true);
        } else {
            LinkGrabberTable.getInstance().getEventSender().removeListener(this);
            LinkGrabberTableModel.getInstance().getEventSender().addListener(this, true);
        }
        switch (getAutoStart()) {
        case AUTO:
            CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getEventSender().addListener(this, true);
            break;
        default:
            CFG_LINKGRABBER.LINKGRABBER_AUTO_START_ENABLED.getEventSender().removeListener(this);
        }
    }

    @Override
    public ConfirmLinksContextAction setAutoStart(AutoStartOptions autoStart) {
        ConfirmLinksContextAction ret = super.setAutoStart(autoStart);
        updateListeners();
        return ret;
    }

    @Override
    protected SelectionInfo<CrawledPackage, CrawledLink> getSelection() {
        if (!isSelectionOnly()) {
            return LinkGrabberTable.getInstance().getSelectionInfo(false, true);
        } else {
            return LinkGrabberTable.getInstance().getSelectionInfo(true, true);
        }
    }

    protected void update() {
        setEnabled(hasSelection());
    }

    @Override
    public void setVisible(boolean newValue) {
        super.setVisible(newValue);
    }

    @Override
    protected void requestTableContextUpdate() {

    }

    @Override
    public void initContextDefaults() {
        setCtrlToggle(false);
    }

    protected void initTableContext(boolean empty, boolean selection) {

    }

    @Override
    public void onExtTableEvent(ExtTableEvent<?> event) {
        if (event.getType() == ExtTableEvent.Types.SELECTION_CHANGED) {
            update();
        }
    }

    @Override
    public void onExtTableModelEvent(ExtTableModelEventWrapper listener) {
        delayer.resetAndStart();
    }

    @Override
    public void onConfigValidatorError(KeyHandler<Boolean> keyHandler, Boolean invalidValue, ValidationException validateException) {
    }

    @Override
    public void onConfigValueModified(KeyHandler<Boolean> keyHandler, Boolean newValue) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                updateLabelAndIcon();
            }
        };

    }

}
