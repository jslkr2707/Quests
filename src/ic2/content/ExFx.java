package ic2.content;

import arc.graphics.Color;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import mindustry.ctype.ContentList;
import mindustry.entities.Effect;
import mindustry.graphics.Pal;

import static arc.graphics.g2d.Draw.color;
import static arc.graphics.g2d.Lines.stroke;
import static arc.math.Angles.randLenVectors;

public class ExFx {
    public static final Effect

    carbondust = new Effect(420, e -> randLenVectors(e.id, 8, 6f + e.fin() * 5f, (x, y) -> {
        color(Color.black);
        Fill.circle(e.x + x, e.y + y, 2f);
    })),

    turretOverheat = new Effect(60, e -> {
        color(Pal.lightOrange);

    });

}