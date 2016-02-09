package org.jdownloader.iconsetter.gui;

import org.appwork.utils.swing.dialog.ProgressDialog.ProgressGetter;
import org.jdownloader.iconsetter.IconResource;

public class Icon8Chooser {

    private IconResource res;
    private String       searchTags;

    public Icon8Chooser(IconResource res) {
        this.res = res;
    }

    public void choose() {
        ProgressGetter pg = new ProgressGetter() {

            @Override
            public int getProgress() {
                return -1;
            }

            @Override
            public String getString() {
                return null;
            }

            @Override
            public void run() throws Exception {
                query();
            }

            @Override
            public String getLabelString() {
                return null;
            }
        };
        // InputDialog d = new InputDialog(0, "Search Term", "", res.getTags(), null, null, null);
        // UIOManager.I().show(null, d);
        // searchTags = d.getText();
        // ProgressDialog pd = new ProgressDialog(pg, 0, "Contact Icon8", "Please wait...", null, null, null);
        // UIOManager.I().show(null, pd);

    }

    protected void query() {

    }

}
