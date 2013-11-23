package org.jdownloader.extensions.extraction.gui.bubble;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.Timer;

import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.extensions.extraction.ExtractionController;
import org.jdownloader.extensions.extraction.ExtractionEvent;
import org.jdownloader.extensions.extraction.translate.T;
import org.jdownloader.gui.notify.BubbleNotify;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;

public class ExtractionBubble extends AbstractNotifyWindow<ExtractionBubbleContent> {

    @Override
    protected void onMouseClicked(MouseEvent m) {
        super.onMouseClicked(m);
        // JDGui.getInstance().requestPanel(JDGui.Panels.LINKGRABBER);
        // JDGui.getInstance().setFrameState(FrameState.TO_FRONT_FOCUSED);

    }

    public void hideBubble(int timeout) {
        super.hideBubble(timeout);
        updateTimer.stop();

    }

    protected long               createdTime;
    private Timer                updateTimer;
    private ExtractionController caller;

    public ExtractionBubble(ExtractionBubbleSupport extractionBubbleSupport, final ExtractionController caller) {
        super(extractionBubbleSupport, T._.bubble_text(), new ExtractionBubbleContent());
        this.caller = caller;
        // , _GUI._.balloon_reconnect_start_msg(), NewTheme.I().getIcon("reconnect", 32)

        updateTimer = new Timer(1000, new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                getContentComponent().update(caller, null);
            }
        });
        updateTimer.setRepeats(true);
        updateTimer.start();

    }

    @Override
    protected int getTimeout() {
        return 0;
    }

    @Override
    public void dispose() {
        super.dispose();
        updateTimer.stop();
    }

    public void stop() {
        System.out.println("Stop");
        startTimeout(super.getTimeout());
        getContentComponent().stop();
        updateTimer.stop();
    }

    public void refresh(final ExtractionEvent event) {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                getContentComponent().update(caller, event);
                pack();
                BubbleNotify.getInstance().relayout();
            }
        };

    }

}
