package org.jdownloader.gui.notify.captcha;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.appwork.utils.swing.EDTHelper;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.captcha.v2.solver.CESChallengeSolver;
import org.jdownloader.captcha.v2.solver.CESSolverJob;
import org.jdownloader.gui.notify.AbstractBubbleSupport;
import org.jdownloader.gui.notify.BubbleNotify.AbstractNotifyWindowFactory;
import org.jdownloader.gui.notify.Element;
import org.jdownloader.gui.notify.gui.AbstractNotifyWindow;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.settings.staticreferences.CFG_CAPTCHA;

public class CESBubbleSupport extends AbstractBubbleSupport {

    private static final CESBubbleSupport INSTANCE = new CESBubbleSupport();

    /**
     * get the only existing instance of CESBubbleSupport. This is a singleton
     * 
     * @return
     */
    public static CESBubbleSupport getInstance() {
        return CESBubbleSupport.INSTANCE;
    }

    private CESBubbleSupport() {
        super(_GUI._.CESBubbleSupport_CESBubbleSupport(), CFG_CAPTCHA.REMOTE_CAPTCHA_BUBBLE_ENABLED);
    }

    @Override
    public List<Element> getElements() {
        return null;
    }

    public CESBubble show(final CESChallengeSolver<?> solver, final CESSolverJob<?> cesSolverJob, final int timeoutms) throws InterruptedException {
        if (isEnabled() && timeoutms > 0) {
            final CESBubble ret = new EDTHelper<CESBubble>() {

                @Override
                public org.jdownloader.gui.notify.captcha.CESBubble edtRun() {
                    final AtomicReference<CESBubble> ret = new AtomicReference<CESBubble>(null);
                    show(new AbstractNotifyWindowFactory() {

                        @Override
                        public AbstractNotifyWindow<?> buildAbstractNotifyWindow() {
                            CESBubble bubble = new CESBubble(solver, cesSolverJob, timeoutms);
                            ret.set(bubble);
                            return bubble;
                        }
                    });
                    return ret.get();
                }
            }.getReturnValue();
            try {
                if (ret != null) {
                    final long waitUntil = System.currentTimeMillis() + timeoutms;
                    while (!cesSolverJob.getJob().isSolved()) {
                        final long rest = waitUntil - System.currentTimeMillis();
                        Thread.sleep(1000);
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                ret.update(rest);
                            }
                        }.waitForEDT();
                        if (rest <= 0) {
                            return ret;
                        }
                    }
                }
            } catch (InterruptedException e) {
                hide(ret);
                throw e;
            }
            return ret;
        }
        return null;
    }

    public void hide(CESBubble bubble) {
        if (bubble != null) {
            bubble.hideBubble(5000);
        }
    }
}
