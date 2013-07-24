package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.test;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JScrollPane;

import jd.controlling.IOEQ;
import jd.controlling.linkchecker.LinkChecker;
import jd.controlling.linkchecker.LinkCheckerHandler;
import jd.controlling.linkcrawler.CrawledLink;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.controlling.linkcrawler.LinkCrawlerHandler;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.ExceptionsRuleDialog;
import jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter.editdialog.FilterRuleDialog;

import org.appwork.scheduler.DelayedRunnable;
import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.swing.exttable.ExtTableModel;
import org.appwork.uio.UIOManager;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.jdownloader.controlling.filter.LinkFilterController;
import org.jdownloader.controlling.filter.LinkgrabberFilterRule;
import org.jdownloader.controlling.packagizer.PackagizerController;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class TestWaitDialog extends AbstractDialog<List<CrawledLink>> {

    private CircledProgressBar          progress;

    private Thread                      recThread;

    private String                      url;
    private java.util.List<CrawledLink> found;

    private JLabel                      lbl;

    private int                         filtered;

    private ExtTableModel<CrawledLink>  model;

    private DelayedRunnable             delayer;

    private LinkFilterController        linkFilterController;

    private PackagizerController        packagizer;

    public TestWaitDialog(String string, String title, LinkFilterController controller) {
        super(UIOManager.BUTTONS_HIDE_OK, title, null, null, _GUI._.literally_close());
        this.url = string;
        linkFilterController = controller;
        delayer = new DelayedRunnable(IOEQ.TIMINGQUEUE, 200, 1000) {

            @Override
            public void delayedrun() {
                update();
            }
        };

    }

    public TestWaitDialog(String string, LinkFilterController controller) {
        this(string, _GUI._.TestWaitDialog_TestWaitDialog_title_(), controller);
    }

    @Override
    protected java.util.List<CrawledLink> createReturnValue() {
        return found;
    }

    private void runTest(final LinkCrawler lc, final LinkChecker<CrawledLink> lch) {
        System.out.println("TEST");
        if (linkFilterController != null) lc.setFilter(linkFilterController);
        found = new ArrayList<CrawledLink>();
        filtered = 0;
        lch.setLinkCheckHandler(new LinkCheckerHandler<CrawledLink>() {

            public void linkCheckStopped() {
            }

            public void linkCheckStarted() {
            }

            public void linkCheckDone(CrawledLink link) {
                synchronized (found) {
                    if (linkFilterController != null && linkFilterController.dropByFileProperties(link)) {
                        filtered++;
                    }
                    if (packagizer != null) {
                        packagizer.runByFile(link);
                    }
                    found.add(link);
                }
                delayer.run();
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

            @Override
            public void handleBrokenLink(CrawledLink link) {
            }

            @Override
            public void handleUnHandledLink(CrawledLink link) {
            }
        });
        lc.crawl(url);
        lc.waitForCrawling();
        lch.waitForChecked();
    }

    protected void update() {
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                if (lbl != null) {
                    if (found.size() == 0) {
                        lbl.setText(_GUI._.TestWaitDialog_runInEDTnothing_found());
                    } else {
                        lbl.setText(_GUI._.TestWaitDialog_runInEDT_(filtered, found.size(), ((filtered * 10000) / found.size()) / 100d));
                    }
                }
                // synchronized (found) {
                model._fireTableStructureChanged(new ArrayList<CrawledLink>(found), true);
                // }

            }
        };

    }

    public java.util.List<CrawledLink> getFound() {
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
        if (linkFilterController != null) {
            p.add(label(_GUI._.TestWaitDialog_layoutDialogContent_filtered()));
            lbl = new JLabel();
            p.add(lbl);
        } else {
            p.add(Box.createGlue(), "spanx");
        }
        p.add(new JScrollPane(new ResultTable(this, model = createTableModel())), "spanx,pushx,growx,newline");
        recThread = new Thread("LinkFilterTesting") {

            private LinkCrawler              lc  = new LinkCrawler();
            private LinkChecker<CrawledLink> lch = new LinkChecker<CrawledLink>();

            @Override
            public void interrupt() {
                lch.stopChecking();
                lc.stopCrawling();
                lc.setCrawlingAllowed(false);
                super.interrupt();
            }

            public void run() {
                try {
                    runTest(lc, lch);
                } finally {
                    new EDTRunner() {

                        @Override
                        protected void runInEDT() {
                            progress.setIndeterminate(false);
                            setTitle(_GUI._.TestWaitDialog_TestWaitDialog_title_finished());
                        }
                    };
                }
            }
        };

        recThread.start();

        return p;
    }

    protected ExtTableModel<CrawledLink> createTableModel() {
        return new ResultTableModel();
    }

    protected int getPreferredWidth() {
        // TODO Auto-generated method stub
        return JDGui.getInstance().getMainFrame().getWidth();
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
            linkFilterController.update();
            // update?
        } catch (DialogClosedException e1) {
            e1.printStackTrace();
        } catch (DialogCanceledException e1) {
            e1.printStackTrace();
        }
    }

    public void setPackagizer(PackagizerController packagizer) {
        this.packagizer = packagizer;
    }

}
