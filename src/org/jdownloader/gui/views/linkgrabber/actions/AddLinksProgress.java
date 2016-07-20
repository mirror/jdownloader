package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog.ModalityType;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.swing.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.locator.RememberRelativeDialogLocator;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.helpdialogs.HelpDialog;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class AddLinksProgress extends AbstractDialog<Object> {

    private CircledProgressBar                 progress;
    private JLabel                             duration;
    private JLabel                             old;

    private JLabel                             header;

    private Timer                              updateTimer;
    private long                               startTime;

    private final LinkCollectingJob            job;
    private final AtomicReference<LinkCrawler> lcReference = new AtomicReference<LinkCrawler>();
    private JLabel                             filtered;

    public AddLinksProgress(LinkCollectingJob crawljob) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.AddLinksProgress_AddLinksProgress_(), null, _GUI.T.literally_hide(), _GUI.T.literally_abort());
        setLocator(new RememberRelativeDialogLocator("AddLinksProgress", JDGui.getInstance().getMainFrame()));
        this.job = crawljob;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public ModalityType getModalityType() {
        return ModalityType.MODELESS;
    }

    @Override
    public Window getOwner() {
        return super.getOwner();
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 9", "[][][][][][][][][]", "[grow,fill]");
        progress = new CircledProgressBar();
        progress.setIndeterminate(true);
        progress.setValueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_LINKGRABBER, 26), 1.0f));
        ((ImagePainter) progress.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getValueClipPainter()).setForeground(Color.GREEN);

        progress.setNonvalueClipPainter(new ImagePainter(new AbstractIcon(IconKey.ICON_LINKGRABBER, 26), 0.5f));
        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.GREEN);
        p.add(progress, "height 40!,width 40!,spany 2");
        p.add(header = new JLabel(), "spanx");
        SwingUtils.toBold(header);
        String stz = getSearchInText();
        header.setText(_GUI.T.AddLinksProgress_layoutDialogContent_header_(stz.substring(0, Math.min(45, stz.length()))));
        p.add(label(_GUI.T.AddLinksProgress_layoutDialogContent_duration()));

        p.add(duration = new JLabel(), "width 50:n:n,growx");
        p.add(new JLabel(new AbstractIcon(IconKey.ICON_GO_NEXT, 18)));
        p.add(label(_GUI.T.AddLinksProgress_found()));
        p.add(old = new JLabel());
        JLabel lbl;
        p.add(lbl = new JLabel(new AbstractIcon(IconKey.ICON_FILTER, 18)));
        lbl.setToolTipText(_GUI.T.AddLinksProgress_filter());
        p.add(filtered = new JLabel(), "alignx right,sg 1");

        startTime = System.currentTimeMillis();
        updateTimer = new Timer(500, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                final LinkCrawler lc = lcReference.get();
                old.setText("" + (lc == null ? 0 : lc.getCrawledLinksFoundCounter()));
                filtered.setText("" + (lc == null ? 0 : lc.getFilteredLinks().size()));

            }
        });
        old.setText("0");
        filtered.setText("0");
        getAddLinksDialogThread(job, lcReference).start();
        updateTimer.setRepeats(true);
        updateTimer.start();
        return p;
    }

    public Thread getAddLinksDialogThread(final LinkCollectingJob job, final AtomicReference<LinkCrawler> lcReference) {
        return new Thread("AddLinksDialogThread:" + job.getOrigin().getOrigin()) {

            public void run() {
                try {
                    final Thread thread = LinkCollector.getInstance().getAddLinksThread(job, lcReference);
                    thread.run();
                } finally {
                    if (isInitialized()) {
                        new EDTRunner() {
                            @Override
                            protected void runInEDT() {
                                dispose();
                            }
                        };
                    }
                }
            }
        };
    }

    protected String getSearchInText() {
        final String ret = job.getText();
        if (ret == null) {
            return "";
        } else {
            return ret;
        }

    }

    @Override
    protected void setReturnmask(boolean b) {
        if (b) {
            // Hide

            new Thread("SimpleTextBallon") {
                public void run() {
                    IconedProcessIndicator iconComp = JDGui.getInstance().getStatusBar().getLinkGrabberIndicator();
                    Point loc = iconComp.getLocationOnScreen();
                    if (CFG_GUI.HELP_DIALOGS_ENABLED.isEnabled()) {
                        HelpDialog.show(false, false, new Point(loc.x + iconComp.getWidth() / 2, loc.y + iconComp.getHeight() / 2), "linkcrawlerprogressdialog", Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI.T.AddLinksProgress_setReturnmask_title_(), _GUI.T.AddLinksProgress_setReturnmask_msg_(), new AbstractIcon(IconKey.ICON_LINKGRABBER, 32));
                    }

                }
            }.start();

        } else {
            // cancel
            final LinkCrawler lc = lcReference.get();
            if (lc != null) {
                LinkCollector.getInstance().abort();
                // lc.stopCrawling();
            }
        }
        super.setReturnmask(b);
    }

    @Override
    public void dispose() {
        this.setVisible(false);
        super.dispose();
        updateTimer.stop();
    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }

}
