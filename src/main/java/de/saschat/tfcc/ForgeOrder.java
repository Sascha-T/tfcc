package de.saschat.tfcc;

import net.dries007.tfc.common.capabilities.forge.ForgeRule;

/**
 * Must match {@link net.dries007.tfc.common.capabilities.forge.ForgeRule.Order}
 */
public enum ForgeOrder {
    ANY(88),
    LAST(0),
    NOT_LAST(66),
    SECOND_LAST(22),
    THIRD_LAST(44);

    int a;
    ForgeOrder(int a) {
        this.a = a;
    }

    public static ForgeOrder get(ForgeRule rule) {
        for (ForgeOrder value : values()) {
            if(value.a == rule.overlayY())
                return value;
        }
        return null;
    }

}
