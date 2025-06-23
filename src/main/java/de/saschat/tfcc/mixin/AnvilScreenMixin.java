package de.saschat.tfcc.mixin;

import de.saschat.tfcc.ForgeOrder;
import de.saschat.tfcc.PreStep;
import it.unimi.dsi.fastutil.ints.IntArrayFIFOQueue;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntPriorityQueue;
import net.dries007.tfc.client.screen.AnvilScreen;
import net.dries007.tfc.client.screen.BlockEntityScreen;
import net.dries007.tfc.common.blockentities.AnvilBlockEntity;
import net.dries007.tfc.common.capabilities.forge.ForgeRule;
import net.dries007.tfc.common.capabilities.forge.ForgeStep;
import net.dries007.tfc.common.capabilities.forge.Forging;
import net.dries007.tfc.common.capabilities.heat.HeatCapability;
import net.dries007.tfc.common.capabilities.heat.IHeat;
import net.dries007.tfc.common.container.AnvilContainer;
import net.dries007.tfc.common.recipes.AnvilRecipe;
import net.dries007.tfc.network.PacketHandler;
import net.dries007.tfc.network.ScreenButtonPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.extensibility.IMixinConfig;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

@Mixin(AnvilScreen.class)
public class AnvilScreenMixin extends BlockEntityScreen<AnvilBlockEntity, AnvilContainer> {
    public AnvilScreenMixin(AnvilContainer container, Inventory playerInventory, Component name, ResourceLocation texture) {
        super(container, playerInventory, name, texture);
    }
    @Unique private static PreStep[] OPTIMAL = new PreStep[300];
    static {
        Arrays.fill(OPTIMAL, null);
        OPTIMAL[149] = new PreStep(List.of(), 0);
        Queue<PreStep> queue = new ArrayBlockingQueue<>(512);
        List<PreStep> buffer = new ArrayList<>();
        int reached = 1;
        queue.add(new PreStep(new LinkedList(), 0));

        System.out.println("Calculating");
        while(reached < 299) {
            while(!queue.isEmpty()) {
                PreStep value = queue.remove();

                for(ForgeStep step : ForgeStep.values()) {
                    int nextValue = value.value() + step.step();
                    if (nextValue > -150 && nextValue < 150 && OPTIMAL[nextValue+149] == null) {
                        OPTIMAL[nextValue+149] = value.add(step);
                        buffer.add(OPTIMAL[nextValue+149]);
                        ++reached;
                        System.out.println("Reached " + (nextValue + 149) + ", progress: " + reached + "/299");
                    }
                }
            }

            Objects.requireNonNull(queue);
            queue.addAll(buffer);
            buffer.clear();
        }
    }


    /**
     * Adapted from {@link ForgeRule}
     */
    @Unique private static ForgeStep[] getFinals(ForgeRule[] rules) {
        ForgeStep[] lastSteps = new ForgeStep[3];

        for(ForgeRule rule : rules) {
            ForgeOrder order = ForgeOrder.get(rule);
            ForgeStep step = ((ForgeRuleAccessor)(Object) rule).step();
            switch (order) {
                case THIRD_LAST:
                    lastSteps[2] = step;
                    break;
                case SECOND_LAST:
                    lastSteps[1] = step;
                    break;
                case LAST:
                    lastSteps[0] = step;
            }
        }
        for(ForgeRule rule : rules) {
            ForgeOrder order = ForgeOrder.get(rule);
            if (order == ForgeOrder.NOT_LAST || order == ForgeOrder.ANY) {
                boolean placed = false;
                ForgeStep step = ((ForgeRuleAccessor)(Object) rule).step();
                for(int i = 2; i >= 0; --i) {
                    if (lastSteps[i] != null && lastSteps[i] == step && (order == ForgeOrder.ANY || i > 0)) {
                        lastSteps[i] = step;
                        placed = true;
                        break;
                    }
                }
                if (!placed) {
                    for(int i = 2; i >= 0; --i) {
                        if (lastSteps[i] == null) {
                            lastSteps[i] = step;
                            break;
                        }
                    }
                }
            }
        }
        return lastSteps;
    }

    @Unique
    private Button button = new Button.Builder(Component.literal("Do"), this::perform)
            .pos(16, 22)
            .size(22, 20)
            .build();

    private void perform(Button button) {
        Forging mainInputForging = blockEntity.getMainInputForging();

        int source = mainInputForging.getWork();
        int target = mainInputForging.getWorkTarget() - source;

        AnvilRecipe recipe = mainInputForging.getRecipe(blockEntity.getLevel());
        ForgeStep[] finals = getFinals(recipe.getRules());
        for (ForgeStep aFinal : finals) {
            if(aFinal != null)
                target -= aFinal.step();
        }

        List<ForgeStep> toPerform = new LinkedList<>();
        toPerform.addAll(OPTIMAL[target+149].steps());
        for (int i = 2; i >= 0; i--) {
            if(finals[i] != null)
                toPerform.add(finals[i]);
        }

        for (ForgeStep forgeStep : toPerform) {
            System.out.println("TFCC: " + forgeStep.name());
            PacketHandler.send(PacketDistributor.SERVER.noArg(), new ScreenButtonPacket(forgeStep.ordinal(), null));
        }
    }

    @Inject(at = @At("HEAD"), method = "init")
    public void init(CallbackInfo ci) {
        addRenderableWidget(button);
    }

    //  PacketDistributor.sendToServer(new ScreenButtonPacket(step.ordinal()));

    @Inject(at = @At("HEAD"), method = "renderBg")
    public void gg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY, CallbackInfo ci) {
        Forging mainInputForging = blockEntity.getMainInputForging();

        AnvilBlockEntity.AnvilInventory inv = ((InventoryBlockEntityAccessor<AnvilBlockEntity.AnvilInventory>) blockEntity).getInventory();
        boolean exists = false;
        if(mainInputForging != null && !inv.getItem().is(Items.AIR)) {
            AnvilRecipe recipe = mainInputForging.getRecipe(blockEntity.getLevel());
            IHeat heat = HeatCapability.get(inv.getItem());
            if(heat != null && heat.canWork() && recipe != null) {
                exists = true;
            }
        }
        button.active = exists;
    }
}
