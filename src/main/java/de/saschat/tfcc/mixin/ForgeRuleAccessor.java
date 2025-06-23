package de.saschat.tfcc.mixin;

import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeStep;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ForgeRule.class)
public interface ForgeRuleAccessor {
    @Accessor("type")
    ForgeStep step();
}
