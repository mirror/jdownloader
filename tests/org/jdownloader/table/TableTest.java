package org.jdownloader.table;

import java.util.logging.Level;

import javax.swing.JScrollPane;

import org.appwork.app.gui.BasicGui;
import org.appwork.swing.exttable.test.ExtTextTable;
import org.appwork.utils.logging.Log;
import org.appwork.utils.swing.EDTRunner;

public class TableTest {

    public static void main(final String[] args) {
        Log.L.setLevel(Level.ALL);
        new EDTRunner() {

            @Override
            protected void runInEDT() {
                // LookAndFeelController.getInstance().setUIManager();
                new BasicGui("testTable") {

                    @Override
                    protected void layoutPanel() {
                        this.getFrame().add(new JScrollPane(new ExtTextTable()));
                    }

                    @Override
                    protected void requestExit() {
                        System.exit(1);
                    }

                };
            }
        };

    }
}
