package org.jdownloader.gui.views.linkgrabber.actions;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.Timer;

import jd.controlling.linkcollector.LinkCollectingJob;
import jd.controlling.linkcollector.LinkCollector;
import jd.controlling.linkcrawler.LinkCrawler;
import jd.gui.swing.jdgui.JDGui;
import jd.gui.swing.jdgui.components.IconedProcessIndicator;

import org.appwork.app.gui.MigPanel;
import org.appwork.swing.components.circlebar.CircledProgressBar;
import org.appwork.swing.components.circlebar.ImagePainter;
import org.appwork.utils.formatter.TimeFormatter;
import org.appwork.utils.swing.EDTRunner;
import org.appwork.utils.swing.SwingUtils;
import org.appwork.utils.swing.dialog.AbstractDialog;
import org.appwork.utils.swing.dialog.Dialog;
import org.appwork.utils.swing.dialog.DialogCanceledException;
import org.appwork.utils.swing.dialog.DialogClosedException;
import org.appwork.utils.swing.dialog.OffScreenException;
import org.appwork.utils.swing.dialog.SimpleTextBallon;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.NewTheme;

public class AddLinksProgress extends AbstractDialog<Object> {

    private CircledProgressBar progress;
    private JLabel             duration;
    private JLabel             old;

    private JLabel             header;

    private Timer              updateTimer;
    private long               startTime;

    private LinkCollectingJob  job;
    private Thread             thread;
    protected LinkCrawler      lc;
    private JLabel             filtered;

    public AddLinksProgress(LinkCollectingJob crawljob) {
        super(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.AddLinksProgress_AddLinksProgress_(), null, _GUI._.literally_hide(), _GUI._.literally_abort());
        this.job = crawljob;
    }

    @Override
    protected Object createReturnValue() {
        return null;
    }

    @Override
    public JComponent layoutDialogContent() {
        MigPanel p = new MigPanel("ins 0,wrap 9", "[][][][][][][][][]", "[grow,fill]");
        progress = new CircledProgressBar();
        progress.setIndeterminate(true);
        progress.setValueClipPainter(new ImagePainter(NewTheme.I().getImage("linkgrabber", 26), 1.0f));
        ((ImagePainter) progress.getValueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getValueClipPainter()).setForeground(Color.GREEN);

        progress.setNonvalueClipPainter(new ImagePainter(NewTheme.I().getImage("linkgrabber", 26), 0.5f));
        ((ImagePainter) progress.getNonvalueClipPainter()).setBackground(Color.WHITE);
        ((ImagePainter) progress.getNonvalueClipPainter()).setForeground(Color.GREEN);
        p.add(progress, "height 40!,width 40!,spany 2");
        p.add(header = new JLabel(), "spanx");
        SwingUtils.toBold(header);

        header.setText(_GUI._.AddLinksProgress_layoutDialogContent_header_(job.getText().substring(0, Math.min(45, job.getText().length()))));
        p.add(label(_GUI._.AddLinksProgress_layoutDialogContent_duration()));

        p.add(duration = new JLabel(), "width 50!");
        p.add(new JLabel(NewTheme.I().getIcon("go-next", 18)));
        p.add(label(_GUI._.AddLinksProgress_found()));
        p.add(old = new JLabel());
        JLabel lbl;
        p.add(lbl = new JLabel(NewTheme.I().getIcon("filter", 18)));
        lbl.setToolTipText(_GUI._.AddLinksProgress_filter());
        p.add(filtered = new JLabel(), "alignx right,sg 1");

        startTime = System.currentTimeMillis();
        updateTimer = new Timer(500, new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                duration.setText(TimeFormatter.formatMilliSeconds(System.currentTimeMillis() - startTime, 0));
                old.setText("" + (lc == null ? 0 : lc.crawledLinksFound()));
                filtered.setText("" + (lc == null ? 0 : lc.getFilteredLinks().size()));

            }
        });
        old.setText("0");
        filtered.setText("0");
        thread = new Thread("AddLinksDialog") {
            public void run() {
                lc = LinkCollector.getInstance().addCrawlerJob(job);

                lc.waitForCrawling();
                System.out.println("JOB DONE: " + lc.crawledLinksFound());
                new EDTRunner() {

                    @Override
                    protected void runInEDT() {
                        dispose();
                    }
                };

            }
        };

        thread.start();
        updateTimer.setRepeats(true);
        updateTimer.start();

        return p;
    }

    @Override
    protected void setReturnmask(boolean b) {
        if (b) {
            // Hide

            new Thread("SimpleTextBallon") {
                public void run() {
                    try {
                        SimpleTextBallon d = new SimpleTextBallon(Dialog.STYLE_SHOW_DO_NOT_DISPLAY_AGAIN, _GUI._.AddLinksProgress_setReturnmask_title_(), _GUI._.AddLinksProgress_setReturnmask_msg_(), NewTheme.I().getIcon("linkgrabber", 32)) {
                            {
                                IconedProcessIndicator iconComp = JDGui.getInstance().getStatusBar().getLinkGrabberIndicator();
                                Point loc = iconComp.getLocationOnScreen();
                                setDesiredLocation(new Point(loc.x + iconComp.getWidth() / 2, loc.y + iconComp.getHeight() / 2));
                            }

                            @Override
                            protected String getDontShowAgainKey() {
                                return "linkcrawlerprogressdialog";
                            }

                        };

                        Dialog.getInstance().showDialog(d);
                    } catch (OffScreenException e1) {
                        e1.printStackTrace();
                    } catch (DialogClosedException e1) {
                        e1.printStackTrace();
                    } catch (DialogCanceledException e1) {
                        e1.printStackTrace();
                    }
                }
            }.start();

        } else {
            // cancel
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
        thread.interrupt();
        updateTimer.stop();

    }

    private Component label(String lbl) {
        JLabel ret = new JLabel(lbl);
        ret.setEnabled(false);
        return ret;
    }

}
