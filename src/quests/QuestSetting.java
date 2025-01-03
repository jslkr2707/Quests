package quests;

import arc.scene.ui.layout.*;
import mindustry.*;
import mindustry.gen.*;
import mindustry.type.*;
import quests.type.*;
import quests.ui.dialogs.QuestDialog;
import arc.*;
import arc.scene.*;
import arc.scene.style.*;
import mindustry.content.TechTree;
import mindustry.graphics.*;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.*;

import static mindustry.Vars.*;

public class QuestSetting {
    public static boolean showTechSelect;
    public String dialogTitleKey;
    
    public static TechTree.TechNode findRoot(){
        TechTree.TechNode root = null;
        for (TechTree.TechNode node: TechTree.roots) {
            if (node.content instanceof Quest) continue;
            
            root = node;
        }
        
        return root;
    }

    public static Planet getFirstPlanet(){
        Planet quest = null;

        for (Planet pl : content.planets()) {
            if (pl.techTree == null) continue;

            if (pl.techTree.content instanceof Quest) {
                quest = pl;
                break;
            }
        }

        return quest;
    }

    public static Planet getVanillaPlanet(){
        Planet va = null;

        for (Planet pl : content.planets()) {
            if (pl.techTree == null || pl.techTree.content instanceof Quest) continue;

            va = pl;
            break;
        }

        return va;
    }

    public static void removeQuestPlanets(Table t) {
        t.button(b -> {
            b.imageDraw(() -> ui.research.root.node.icon()).padRight(8).size(iconMed);
            b.add().growX();
            b.label(() -> ui.research.root.node.localizedName()).color(Pal.accent);
            b.add().growX();
            b.add().size(iconMed);
        }, () -> {
            new BaseDialog("@techtree.select") {{
                cont.pane(t -> t.table(Tex.button, in -> {
                    in.defaults().width(300f).height(60f);
                    for (TechTree.TechNode node : TechTree.roots) {
                        if (node.content instanceof Quest) continue;
                        if (node.requiresUnlock && !node.content.unlocked() && node != ui.research.getPrefRoot()) continue;

                        in.button(node.localizedName(), node.icon(), Styles.flatTogglet, iconMed, () -> {
                            if (node == ui.research.lastNode) {
                                return;
                            }

                            ui.research.rebuildTree(node);
                            hide();
                        }).marginLeft(12f).checked(node == ui.research.lastNode).row();
                    }
                }));
                addCloseButton();
            }}.show();
        }).visible(showTechSelect = TechTree.roots.count(node -> !(node.requiresUnlock && !node.content.unlocked())) > 1).minWidth(300f);
    }

    public static void init(String name) {
        QuestDialog dialog = new QuestDialog(Core.bundle.format(name));

        ui.planet.shown(() -> {
            ui.planet.buttons.button("@quests", Icon.book, dialog::show).size(200.0f, 54.0f).pad(2.0f).bottom().right();
        });

        ui.research.shown(() -> {
            if (ui.research.lastNode.content instanceof Quest){
                ui.research.switchTree(getVanillaPlanet().techTree);
                ui.research.rebuildItems();
                ui.research.rebuildTree(getVanillaPlanet().techTree);
            }

            var group = (Group)ui.research.getChildren().first();

            ui.research.titleTable.remove();
            group.fill(c -> removeQuestPlanets(c.center().top()));
        });
    }
}
