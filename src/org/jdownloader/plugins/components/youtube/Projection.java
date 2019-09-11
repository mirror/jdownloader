package org.jdownloader.plugins.components.youtube;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.translate._JDT;

public enum Projection implements LabelInterface, TooltipInterface {
    ANAGLYPH_3D {
        @Override
        public String getLabel() {
            return _JDT.T.Projection_3D();
        }

        @Override
        public String getTooltip() {
            return _JDT.T.Projection_3D_tt();
        }
    },
    NORMAL {
        @Override
        public String getLabel() {
            return _JDT.T.Projection_Normal();
        }

        @Override
        public String getTooltip() {
            return _JDT.T.Projection_Normal_tt();
        }
    },
    SPHERICAL {
        @Override
        public String getLabel() {
            return _JDT.T.Projection_Spherical();
        }

        @Override
        public String getTooltip() {
            return _JDT.T.Projection_Spherical_tt();
        }
    },
    SPHERICAL_3D {
        @Override
        public String getLabel() {
            return _JDT.T.Projection_Spherical3D();
        }

        @Override
        public String getTooltip() {
            return _JDT.T.Projection_Spherical3D_tt();
        }
    };
}
