package quests.ui;

import arc.*;
import arc.scene.ui.layout.*;
import arc.util.*;
import quests.type.*;
import mindustry.gen.*;

public class QuestDisplay extends Table {

    public QuestDisplay(){
        rebuild(null);
    }

    public void rebuild(Quest f){
        clear();
        bottom().left();
        margin(0);

        add(Core.bundle.get("quests.researching"));
        row();
        table(Tex.button, t -> {
            t.margin(20f);
            t.center();
            t.image(f == null ? Icon.none.getRegion() : f.fullIcon).padBottom(10f);
            t.row();
            t.add(f == null ? Core.bundle.get("quests.none") : f.localizedName).scaling(Scaling.bounded);
        }).center().padTop(10f).size(200f, 150f);
    }
}
