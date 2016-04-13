package org.jdownloader.plugins.components.youtube;

import org.appwork.storage.Storable;
import org.appwork.utils.StringUtils;
import org.jdownloader.plugins.components.youtube.variants.AbstractVariant;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.VideoVariant;
import org.jdownloader.plugins.components.youtube.variants.generics.GenericAudioInfo;
import org.jdownloader.settings.staticreferences.CFG_GUI;

public class BlackOrWhitelistEntry implements Storable {
    private String base;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    private String          group;
    private AbstractVariant variant;

    public BlackOrWhitelistEntry(/* Storable */) {
    }

    public BlackOrWhitelistEntry(AbstractVariant v) {
        base = v.getBaseVariant().name();
        group = v.getGroup().name();

    }

    @Override
    public int hashCode() {
        if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) {
            return (group + "." + base).hashCode();
        } else {
            return (group + "." + createVariant().getTypeId()).hashCode();
        }
    }

    @Override
    public String toString() {
        if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) {
            return group + "." + base;
        }
        return group + "." + createVariant().getTypeId();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof BlackOrWhitelistEntry) {
            if (CFG_GUI.EXTENDED_VARIANT_NAMES_ENABLED.isEnabled()) {
                return StringUtils.equals(base, ((BlackOrWhitelistEntry) obj).base) && StringUtils.equals(group, ((BlackOrWhitelistEntry) obj).group);
            } else {
                return StringUtils.equals(createVariant().getTypeId(), ((BlackOrWhitelistEntry) obj).createVariant().getTypeId()) && StringUtils.equals(group, ((BlackOrWhitelistEntry) obj).group);
            }

        }
        return false;
    }

    private AbstractVariant<GenericAudioInfo> createVariant() {
        if (variant != null) {
            return variant;
        }
        AbstractVariant v = AbstractVariant.get(base);
        if (v instanceof VideoVariant && StringUtils.equals(group, VariantGroup.VIDEO_3D.name())) {
            ((VideoVariant) v).getGenericInfo().setThreeD(true);
        }
        variant = v;
        return v;
    }

    @Deprecated
    public BlackOrWhitelistEntry(String oldID) {
        VariantBase baseEnum = VariantBase.COMPATIBILITY_MAP.get(oldID);
        if (baseEnum != null) {
            base = baseEnum.name();
            group = baseEnum.getGroup().name();
        } else {
            String newType = VariantBase.COMPATIBILITY_MAP_ID.get(oldID);

            for (VariantBase v : VariantBase.values()) {
                if (StringUtils.equals(v.getTypeId(), newType) || StringUtils.equals(v.name(), oldID)) {
                    base = v.name();
                    group = v.getGroup().name();
                    break;
                }
            }

        }
    }

    public boolean matches(AbstractVariant variant) {

        return StringUtils.equals(base, variant.getBaseVariant().name()) && StringUtils.equals(group, variant.getGroup().name());
    }
}