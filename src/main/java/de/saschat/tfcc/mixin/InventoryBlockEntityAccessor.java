package de.saschat.tfcc.mixin;

import net.dries007.tfc.common.blockentities.InventoryBlockEntity;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.util.INBTSerializable;
import net.minecraftforge.items.IItemHandlerModifiable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(InventoryBlockEntity.class)
public interface InventoryBlockEntityAccessor<C extends IItemHandlerModifiable & INBTSerializable<CompoundTag>> {
    @Accessor("inventory")
    C getInventory();
}
