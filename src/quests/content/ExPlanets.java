package quests.content;

import arc.graphics.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.graphics.*;
import mindustry.graphics.g3d.*;
import mindustry.maps.planet.*;
import mindustry.type.*;

public class ExPlanets {
    public static Planet exam;

    public static void load(){
        exam = new Planet("exam", Planets.sun, 1.0F, 3) {
            {
                this.generator = new SerpuloPlanetGenerator();
                this.meshLoader = () -> {
                    return new HexMesh(this, 6);
                };
                this.cloudMeshLoader = () -> {
                    return new MultiMesh(new GenericMesh[]{new HexSkyMesh(this, 11, 0.15F, 0.13F, 5, (new Color()).set(Pal.spore).mul(0.9F).a(0.75F), 2, 0.45F, 0.9F, 0.38F), new HexSkyMesh(this, 1, 0.6F, 0.16F, 5, Color.white.cpy().lerp(Pal.spore, 0.55F).a(0.75F), 2, 0.45F, 1.0F, 0.41F)});
                };
                this.launchCapacityMultiplier = 0.5F;
                this.sectorSeed = 2;
                this.allowWaves = true;
                this.allowWaveSimulation = true;
                this.allowSectorInvasion = true;
                this.allowLaunchSchematics = true;
                this.enemyCoreSpawnReplace = true;
                this.allowLaunchLoadout = true;
                this.prebuildBase = false;
                this.ruleSetter = (r) -> {
                    r.waveTeam = Team.crux;
                    r.placeRangeCheck = false;
                    r.showSpawns = false;
                };
                this.iconColor = Color.valueOf("7d4dff");
                this.atmosphereColor = Color.valueOf("3c1b8f");
                this.atmosphereRadIn = 0.02F;
                this.atmosphereRadOut = 0.3F;
                this.startSector = 15;
                this.alwaysUnlocked = true;
                this.landCloudColor = Pal.spore.cpy().a(0.5F);
                this.hiddenItems.addAll(Items.erekirItems).removeAll(Items.serpuloItems);
            }
        };
    }
}
