package org.jdownloader.gui.views.downloads.action;

import java.awt.event.ActionEvent;

import jd.controlling.TaskQueue;
import jd.plugins.DownloadLink;

import org.appwork.utils.event.queue.QueueAction;
import org.appwork.utils.swing.EDTRunner;
import org.jdownloader.controlling.contextmenu.ActionContext;
import org.jdownloader.controlling.contextmenu.CustomizableSelectionAppAction;
import org.jdownloader.controlling.contextmenu.Customizer;
import org.jdownloader.gui.views.SelectionInfo;
import org.jdownloader.translate._JDT;

public class GenericChunksAction extends CustomizableSelectionAppAction implements ActionContext {

    /**
     * 
     */
    private static final long  serialVersionUID = 1L;
    public final static String CHUNKS           = "CHUNKS";

    private int                chunks           = 0;

    public GenericChunksAction() {
        super();
        setName(Integer.toString(Math.max(0, chunks)));
    }

    public void actionPerformed(ActionEvent e) {
        TaskQueue.getQueue().add(new QueueAction<Void, RuntimeException>() {

            @Override
            protected Void run() throws RuntimeException {
                SelectionInfo selectionInfo = getSelection();
                if (selectionInfo != null) {
                    for (final Object dl : selectionInfo.getChildren()) {
                        if (dl instanceof DownloadLink) {
                            ((DownloadLink) dl).setChunks(chunks);
                        }
                    }
                }
                return null;
            }
        });
    }

    public void setChunks(final int chunks) {
        this.chunks = Math.max(0, chunks);
    }

    public static String getTranslationChunks() {
        return _JDT._.GenericChunksAction_getTranslationChunks();
    }

    @Customizer(link = "#getTranslationChunks")
    public int getChunks() {
        return chunks;
    }

    @Override
    public void loadContextSetups() {
        super.loadContextSetups();
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                setName(Integer.toString(Math.max(0, chunks)));
            }
        }.start();
    }

}
