package jd.plugins.optional.extraction;

import java.util.TimerTask;

import jd.plugins.optional.jdunrar.JDUnrarConstants;

/**
 * Is a {@link TimerTask} for updating the unpacking process.
 * Will be executed every second.
 * 
 * @author botzi
 *
 */
class UpdateDisplay extends TimerTask {
    private ExtractionController con;
    
    UpdateDisplay(ExtractionController con) {
        this.con = con;
    }

    @Override
    public void run() {
        con.fireEvent(JDUnrarConstants.WRAPPER_ON_PROGRESS);
    }
}