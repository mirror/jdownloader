package org.jdownloader.extensions.extraction.gui.bubble;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.WeakHashMap;

import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.ExtractionListener;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;

public class ExtractionBubbleSupport extends AbstractBubbleSupport implements ExtractionListener {
    
    private ArrayList<Element> elements;
    
    public ExtractionBubbleSupport(String bubbletype, BooleanKeyHandler bubbleEnabledIfArchiveExtractionIsInProgress) {
        super(bubbletype, bubbleEnabledIfArchiveExtractionIsInProgress);
        elements = new ArrayList<Element>();
        ExtractionBubbleContent.fillElements(elements);
    }
    
    private class ExtractionBubbleWrapper implements AbstractNotifyWindowFactory {
        
        private final WeakReference<ExtractionController> caller;
        private volatile ExtractionBubble                 bubble = null;
        
        private ExtractionBubble getBubble() {
            return bubble;
        }
        
        private ExtractionBubbleWrapper(ExtractionController caller) {
            this.caller = new WeakReference<ExtractionController>(caller);
        }
        
        @Override
        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
            ExtractionController controller = caller.get();
            if (controller == null || controller.isFinished()) return null;
            bubble = new ExtractionBubble(ExtractionBubbleSupport.this, controller);
            return bubble;
        }
        
    }
    
    @Override
    public void onExtractionEvent(ExtractionEvent event) {
        if (isEnabled()) {
            ExtractionBubbleWrapper wrapper = null;
            switch (event.getType()) {
                case START:
                    wrapper = getExtractionBubbleWrapper(event.getCaller(), true);
                    if (wrapper != null) {
                        show(wrapper);
                    }
                    // BubbleNotify.getInstance().show(getBubble(event.getCaller()));
                    // getBubble(event.getCaller()).refresh(event);
                    break;
                default:
                    wrapper = getExtractionBubbleWrapper(event.getCaller(), true);
                    if (wrapper != null) {
                        ExtractionBubble bubble = wrapper.getBubble();
                        if (bubble != null) {
                            bubble.refresh(event);
                            if (ExtractionEvent.Type.CLEANUP.equals(event.getType())) {
                                remove(event.getCaller());
                                bubble.stop();
                            }
                        }
                    }
                    break;
            }
        }
    }
    
    private final WeakHashMap<ExtractionController, ExtractionBubbleWrapper> map = new WeakHashMap<ExtractionController, ExtractionBubbleWrapper>();
    
    private ExtractionBubbleWrapper remove(ExtractionController caller) {
        synchronized (map) {
            return map.remove(caller);
        }
    }
    
    private ExtractionBubbleWrapper getExtractionBubbleWrapper(ExtractionController caller, boolean allowNull) {
        synchronized (map) {
            ExtractionBubbleWrapper ret = map.get(caller);
            if (ret == null && allowNull == false) {
                ret = new ExtractionBubbleWrapper(caller);
                ret = map.put(caller, ret);
            }
            return ret;
        }
    }
    
    @Override
    public List<Element> getElements() {
        return elements;
    }
    
}
