package jd.http.ext;

import jd.http.ext.events.ExtBrowserEvent;

public class FrameStatusEvent extends ExtBrowserEvent {

    public FrameStatusEvent(ExtBrowser extBrowser, FrameController htmlFrameController2, Types evalEnd) {
        super(extBrowser, htmlFrameController2, evalEnd.ordinal(), null);
    }

    public Types getType() {
        return Types.values()[this.getEventID()];
    }

    public static enum Types {
        /**
         * Frame has been loaded and evaluated completly. Only Async calls my
         * cange it now
         */
        EVAL_END,
        /**
         * JS EValuation of the frame contents start now
         */
        EVAL_START,
        /**
         * Download of the fram's main content starts now
         */
        LOAD_START,

        /**
         * Download of the frames main content is finished LOAD_END
         */
        LOAD_END
    }

}
