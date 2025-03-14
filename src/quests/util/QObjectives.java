package quests.util;

import quests.type.*;
import quests.ui.dialogs.*;
import arc.*;
import mindustry.*;
import mindustry.ctype.*;
import mindustry.game.Objectives.*;

public class QObjectives {
    public static class customResearch implements Objective {
        public UnlockableContent content;

        public customResearch(UnlockableContent content){ this.content = content;};

        @Override
        public boolean complete() { return content.unlocked();}

        @Override
        public String display() { return Core.bundle.format("requirements.unlocked", content.localizedName); }
    }

    public static class sectorsCompleted implements Objective {
        public int standard;

        public sectorsCompleted(int standard){
            this.standard = standard;
        }

        @Override
        public boolean complete(){
            return QuestDialog.completed() >= this.standard;
        }

        @Override
        public String display(){
            return Core.bundle.format("requirements.sectorscompleted", standard);
        }
    }

    public static class notUnlocked implements Objective{
        public Quest opposite;

        public notUnlocked(Quest content) { this.opposite = content; }

        @Override
        public boolean complete(){
            return !opposite.unlocked();
        }

        @Override
        public String display(){
            return Core.bundle.format("requirements.notunlocked", opposite.localizedName);
        }
    }

    public static class questResearch implements Objective {
        public Quest[] prerequisite;

        public questResearch(Quest... quest) { this.prerequisite = quest; }

        @Override
        public boolean complete() {
            for (Quest f: prerequisite) {
                if (!f.unlocked()) return false;
            }
            return true;
        }

        @Override
        public String display(){
            return Core.bundle.format("focus.prerequisite") + "\n";
        }
    }

    public static class inGame implements Objective {
        boolean playing = Vars.state.isCampaign() && Vars.state.hasSector();

        @Override
        public boolean complete() {
            return playing;
        }

        @Override
        public String display(){
            return Core.bundle.format("requirements.playing") + "\n";
        }
    }
}
