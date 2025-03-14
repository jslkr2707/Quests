package quests.content;

import arc.struct.Seq;
import mindustry.content.*;

import static mindustry.content.TechTree.*;
import static quests.util.QObjectives.*;
import static quests.content.ExFocus.*;


public class ExTechTree {
    static TechTree.TechNode context = null;

    public static void load() {
        ExPlanets.exam.techTree = nodeRoot("asdf", example, () -> {
            node(intermediate, () -> {});
            node(advanced, Seq.with(new sectorsCompleted(1), new questResearch(intermediate)), () -> {});
        });
    }
}


