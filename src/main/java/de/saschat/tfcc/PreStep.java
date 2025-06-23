package de.saschat.tfcc;

import net.dries007.tfc.common.capabilities.forge.ForgeStep;

import java.util.ArrayList;
import java.util.List;

public record PreStep(List<ForgeStep> steps, int value) {
    public PreStep add(ForgeStep step) {
        ArrayList<ForgeStep> newSteps = new ArrayList<>(steps);
        newSteps.add(step);
        return new PreStep(newSteps, value + step.step());
    }

}
