package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.controlling.linkcrawler.OnlineStatusUncheckedException;
import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ExceptionsRuleDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.app.gui.MigPanel;
import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class TestWaitDialog extends AbstractDialog<ArrayList<CrawledLink>> {

    private CircledProgressBar     progress;

    private Thread                 recThread;

    private String                 url;
    private ArrayList<CrawledLink> found;

    private JLabel                 lbl;

    private int                    filtered;

    private ResultTableModel       model;

    private DelayedRunnable        delayer;

    public TestWaitDialog(String string) {
        super(Dialog.BUTTONS_HIDE_OK, _GUI._.TestWaitDialog_TestWaitDialog_title_(), null, null, _GUI._.literally_close());
        this.url = string;
        delayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, 200, 1000) {

            @Override
            public void delayedrun() {
                update();
            }
        };
    }

    @Override
    protected ArrayList<CrawledLink> createReturnValue() {
        return found;
    }

    private void runTest() {
        System.out.println("TEST");
        LinkCrawler lc = new LinkCrawler();
        lc.setFilter(LinkFilterController.getInstance());
        found = new ArrayList<CrawledLink>();
        filtered = 0;

        final LinkChecker<CrawledLink> lch = new LinkChecker<CrawledLink>();
        lch.setLinkCheckHandler(new LinkCheckerHandler<CrawledLink>() {

            public void linkCheckStopped() {
            }

            public void linkCheckStarted() {
            }

            public void linkCheckDone(CrawledLink link) {
                try {
                    if (link.isAvailable()) {
                        synchronized (found) {
                            if (LinkFilterController.getInstance().dropByFileProperties(link)) {
                                filtered++;
                            }

                            found.add(link);

                        }
                        delayer.run();
                    }
                } catch (OnlineStatusUncheckedException e) {
                    e.printStackTrace();
                }
            }
        });
        lc.setHandler(new LinkCrawlerHandler() {

            public void linkCrawlerStopped() {
            }

            public void linkCrawlerStarted() {
            }

            public void handleFinalLink(CrawledLink link) {
                lch.check(link);
            }

            public void handleFilteredLink(CrawledLink link) {
                synchronized (found) {
                    filtered++;
                    found.add(link);
                }
                delayer.run();
            }
        });
        lc.crawlNormal(url);
        lc.waitForCrawling();
        lch.waitForChecked();

    }

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (found.size() == 0) {
                    lbl.setText(_GUI._.TestWaitDialog_runInEDTnothing_found());
                } else {
                    lbl.setText(_GUI._.TestWaitDialog_runInEDT_(filtered, found.size(), ((filtered * 10000) / found.size()) / 100d));
                }

            }
        };
        synchronized (found) {
            model._fireTableStructureChanged(new ArrayList<CrawledLink>(found), true);
        }
    }

    public ArrayList<CrawledLink> getFound() {
        return found;
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 3", "[][][grow,fill]", "[][][grow,fill]");
        progress = new CircledProgressBar();
        progress.setIndeterminate(true);
        progress.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("filter", 26), 1.0f));
        ((ImagePainter) progress.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getValueClipPainter()).setForeground(Color.GREEN);

        progress.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("filter", 26), 0.5f));
        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.GREEN);
        p.add(progress, "height 40!,width 40!,spany 2");
        p.add(label(_GUI._.TestWaitDialog_layoutDialogContent_testlink_()), "");
        p.add(new JLabel(url), "");
        p.add(label(_GUI._.TestWaitDialog_layoutDialogContent_filtered()));
        lbl = new JLabel();
        p.add(lbl);
        p.add(new JScrollPane(new ResultTable(this, model = new ResultTableModel())), "spanx,pushx,growx");
        recThread = new Thread("ReconnectTest") {

            public void run() {

                runTest();

                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        progress.setIndeterminate(false);
                        setTitle(_GUI._.TestWaitDialog_TestWaitDialog_title_finished());
                    }
                };

            }
        };

        recThread.start();

        return p;
    }

    protected int getPreferredWidth() {
        // TODO Auto-generated method stub
        return SwingGui.getInstance().getMainFrame().getWidth();
    }

    protected boolean isResizable() {

        return true;
    }

    @Override
    public void dispose() {
        super.dispose();
        recThread.interrupt();

    }

    public void edit(LinkgrabberFilterRule rule) {

        try {
            if (rule.isAccept()) {
                Dialog.getInstance().showDialog(new ExceptionsRuleDialog(rule));
            } else {
                Dialog.getInstance().showDialog(new FilterRuleDialog(rule));
            }
            LinkFilterController.getInstance().update();
            // update?
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

}
