package quests.ui.dialogs;

import arc.audio.*;
import quests.*;
import quests.type.Quest;
import quests.ui.*;
import arc.*;
import arc.graphics.*;
import arc.graphics.g2d.*;
import arc.input.*;
import arc.math.*;
import arc.math.geom.*;
import arc.scene.*;
import arc.scene.actions.*;
import arc.scene.event.*;
import arc.scene.style.*;
import arc.scene.ui.*;
import arc.scene.ui.TextButton.*;
import arc.scene.ui.layout.*;
import arc.struct.*;
import arc.util.*;
import quests.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.content.TechTree.*;
import mindustry.core.*;
import mindustry.ctype.*;
import mindustry.game.EventType.*;
import mindustry.game.Objectives.*;
import mindustry.gen.*;
import mindustry.graphics.*;
import mindustry.input.*;
import mindustry.type.*;
import mindustry.ui.*;
import mindustry.ui.dialogs.*;
import mindustry.ui.layout.*;
import mindustry.ui.layout.TreeLayout.*;

import java.util.*;
import static mindustry.Vars.*;
import static mindustry.gen.Tex.*;

public class QuestDialog extends BaseDialog{
    public static boolean debugShowRequirements = false;

    public ObjectSet<TechTreeNode> nodes = new ObjectSet<>();
    public TechTreeNode root = new TechTreeNode(TechTree.roots.get(2), null);
    public TechNode lastNode = root.node;
    public Rect bounds = new Rect();
    public ItemsDisplay itemDisplay;
    public QuestDisplay questDisplay;
    public Table abortDisplay;
    public View view;
    public Quest current;

    //to avoid vanilla planets
    public String questPlanet;
    public int currentSectors = -1;
    /* put a sound file in the "sounds/" directory */
    public Sound researchSound = Vars.tree.loadSound("research");
    public ItemSeq items;

    private boolean showTechSelect;

    public QuestDialog(String name){
        super("name");


        current = all(Core.settings.getString("current", null));
        questPlanet = Core.settings.getString("planet", null);
        currentSectors = Core.settings.getInt("sectors", -1);

        titleTable.remove();
        titleTable.clear();
        titleTable.top();

        titleTable.button((b) -> {
            b.label(() -> {
                return Core.bundle.get("quests.stop");
            });
        }, () -> {
            new BaseDialog(""){{
                cont.table( t -> {
                    t.table(button, a -> {
                        a.margin(20f);
                        a.add("@quests.stop.ask");
                    }).padBottom(10f).center().top();

                    t.row();

                    t.table(Styles.none, b -> {
                        b.button("@quests.stop.yes", () -> {
                            current = null;
                            Core.settings.remove("current");
                            questDisplay.rebuild(null);
                            hide();
                        }).bottom();

                        b.button("@quests.stop.no", this::hide).bottom().padLeft(20f);
                    }).center();

                    t.row();
                }).center();
            }}.show();
        }).right().maxWidth(300f).touchable(() -> current == null ? Touchable.disabled : Touchable.enabled);

        titleTable.button((b) -> {
            b.imageDraw(() -> {
                return this.root.node.icon();
            }).padRight(8.0F).size(32.0F);
            b.add().growX();
            b.label(() -> {
                return this.root.node.localizedName();
            }).color(Pal.accent);
            b.add().growX();
            b.add().size(32.0F);
            b.center();
        }, () -> {
            new BaseDialog("@quest.select"){{
                cont.pane(t -> {
                    t.table(Tex.button, in -> {
                        in.defaults().width(300f).height(60f);
                        for(TechNode node : TechTree.roots){
                            if (node.requiresUnlock  && node != ui.research.getPrefRoot()) continue;
                            if (!(node.content instanceof Quest)) continue;

                            //TODO toggle
                            in.button(node.localizedName(), node.icon(), Styles.flatTogglet, iconMed, () -> {
                                if(node == lastNode){
                                    return;
                                }

                                rebuildTree(node);
                                hide();
                            }).marginLeft(12f).checked(node == lastNode).row();
                        }
                    });
                });

                addCloseButton();
            }}.show();
            center();
        }).visible(() -> {
            return this.showTechSelect = TechTree.roots.count((node) -> {
                return !node.requiresUnlock || node.content.unlocked();
            }) > 1;
        }).minWidth(300.0F).maxWidth(300.0f).center();

        showTechSelect = true;

        margin(0f).marginBottom(8);
        cont.stack(titleTable, view = new View(), itemDisplay = new ItemsDisplay(), questDisplay = new QuestDisplay()).grow();

        titleTable.toFront();

        shouldPause = true;

        Runnable checkMargin = () -> {
            if(Core.graphics.isPortrait() && showTechSelect){
                itemDisplay.marginTop(60f);
            }else{
                itemDisplay.marginTop(0f);
            }
            itemDisplay.invalidate();
            itemDisplay.layout();
        };

        onResize(checkMargin);

        shown(() -> {
            checkMargin.run();
            Core.app.post(checkMargin);

            Planet currPlanet = questPlanet != null ? content.planet(questPlanet) : QuestSetting.getFirstPlanet();

            if(currPlanet != null && currPlanet.techTree != null){
                this.switchTree(currPlanet.techTree);
                questPlanet = currPlanet.name;
                Core.settings.put("planet", questPlanet);
            }

            items = items();

            checkNodes(root);
            treeLayout();

            view.hoverNode = null;
            view.infoTable.remove();
            view.infoTable.clear();
        });

        addCloseButton();

        keyDown(key -> {
            if(key == Core.keybinds.get(Binding.research).key){
                Core.app.post(this::hide);
            }
        });

        //scaling/drag input
        addListener(new InputListener(){
            @Override
            public boolean scrolled(InputEvent event, float x, float y, float amountX, float amountY){
                view.setScale(Mathf.clamp(view.scaleX - amountY / 10f * view.scaleX, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
                return true;
            }

            @Override
            public boolean mouseMoved(InputEvent event, float x, float y){
                view.requestScroll();
                return super.mouseMoved(event, x, y);
            }
        });

        touchable = Touchable.enabled;

        addCaptureListener(new ElementGestureListener(){
            @Override
            public void zoom(InputEvent event, float initialDistance, float distance){
                if(view.lastZoom < 0){
                    view.lastZoom = view.scaleX;
                }

                view.setScale(Mathf.clamp(distance / initialDistance * view.lastZoom, 0.25f, 1f));
                view.setOrigin(Align.center);
                view.setTransform(true);
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button){
                view.lastZoom = view.scaleX;
            }

            @Override
            public void pan(InputEvent event, float x, float y, float deltaX, float deltaY){
                view.panX += deltaX / view.scaleX;
                view.panY += deltaY / view.scaleY;
                view.moved = true;
                view.clamp();
            }
        });
    }

    public void switchTree(TechNode node){
        if(lastNode == node || node == null) return;
        nodes.clear();
        root = new TechTreeNode(node, null);
        lastNode = node;
        view.rebuildAll();
    }

    public void rebuildTree(TechNode node){
        switchTree(node);
        view.panX = 0f;
        view.panY = -200f;
        view.setScale(1f);

        view.hoverNode = null;
        view.infoTable.remove();
        view.infoTable.clear();

        checkNodes(root);
        treeLayout();
    }

    void treeLayout(){
        float spacing = 100f;
        LayoutNode node = new LayoutNode(root, null);
        LayoutNode[] children = node.children;
        LayoutNode[] leftHalf = Arrays.copyOfRange(node.children, 0, Mathf.ceil(node.children.length/2f));
        LayoutNode[] rightHalf = Arrays.copyOfRange(node.children, Mathf.ceil(node.children.length/2f), node.children.length);

        node.children = leftHalf;
        new BranchTreeLayout(){{
            gapBetweenLevels = gapBetweenNodes = spacing;
            rootLocation = TreeLocation.top;
        }}.layout(node);

        float lastY = node.y;

        if(rightHalf.length > 0){

            node.children = rightHalf;
            new BranchTreeLayout(){{
                gapBetweenLevels = gapBetweenNodes = spacing;
                rootLocation = TreeLocation.bottom;
            }}.layout(node);

            shift(leftHalf, node.y - lastY);
        }

        node.children = children;

        float minx = 0f, miny = 0f, maxx = 0f, maxy = 0f;
        copyInfo(node);

        for(TechTreeNode n : nodes){
            if(!n.visible) continue;
            minx = Math.min(n.x - n.width/2f, minx);
            maxx = Math.max(n.x + n.width/2f, maxx);
            miny = Math.min(n.y - n.height/2f, miny);
            maxy = Math.max(n.y + n.height/2f, maxy);
        }
        bounds = new Rect(minx, miny, maxx - minx, maxy - miny);
    }

    void shift(LayoutNode[] children, float amount){
        for(LayoutNode node : children){
            node.y += amount;
            if(node.children != null && node.children.length > 0) shift(node.children, amount);
        }
    }

    void copyInfo(LayoutNode node){
        node.node.x = node.x;
        node.node.y = node.y;
        if(node.children != null){
            for(LayoutNode child : node.children){
                copyInfo(child);
            }
        }
    }

    void checkNodes(TechTreeNode node){
        boolean locked = locked(node.node);
        node.visible = true;
        node.selectable = selectable(node.node);
        for(TechTreeNode l : node.children){
            l.visible = true;
            checkNodes(l);
        }

        itemDisplay.rebuild(items);
        questDisplay.rebuild(current);
    }

    boolean selectable(TechNode node){
        return current == node.content ? view.reqComplete(node) : current == null && !node.objectives.contains(i -> !i.complete()) && (node.parent == null || node.parent.content.unlocked()) && !((Quest)node.content).exclusives.contains((UnlockableContent::unlocked));
    }

    boolean locked(TechNode node){
        return node.content.locked();
    }

    ItemSeq items(){
        return new ItemSeq() {
            //store sector item amounts for modifications
            ObjectMap<Sector, ItemSeq> cache = new ObjectMap<>();

            {
                for (Item item : content.items()) {
                    values[item.id] = 0;
                }
                total = 0;

                //add global counts of each sector
                for (Planet planet : content.planets()) {
                    for (Sector sector : planet.sectors) {
                        if (sector.hasBase()) {
                            ItemSeq cached = sector.items();
                            cache.put(sector, cached);
                            cached.each((item, amount) -> {
                                values[item.id] += Math.max(amount, 0);
                                total += Math.max(amount, 0);
                            });
                        }
                    }
                }
            }

            {
                for (Quest quest : view.completedFocus()) {
                    if (quest != null) {
                        for (ItemStack stack : quest.rewards) {
                            values[stack.item.id] += stack.amount;
                            total += stack.amount;
                        }

                        for (ItemStack stack : quest.requirements) {
                            values[stack.item.id] -= stack.amount;
                            total -= stack.amount;
                        }
                    }
                }
            }
        };
    }

    public Quest all(String name){
        if (name == null) return null;
        for (Content c: Vars.content.getBy(ContentType.typeid_UNUSED)){
            if (((Quest) c).localizedName.equals(name)) return (Quest) c;
        }
        return null;
    }

    public static int completed(){
        int a = 0;
        for(Planet planet: content.planets()){
            for (Sector sector: planet.sectors){
                if (sector.isCaptured()) a += 1;
            }
        }
        return a;
    }

    class LayoutNode extends TreeNode<LayoutNode>{
        final TechTreeNode node;

        LayoutNode(TechTreeNode node, LayoutNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = ui.research.nodeSize;
            if(node.children != null){
                children = Seq.with(node.children).map(t -> new LayoutNode(t, this)).toArray(LayoutNode.class);
            }
        }
    }

    public class TechTreeNode extends TreeNode<TechTreeNode>{
        public final TechNode node;
        public boolean visible = true, selectable = true;

        public TechTreeNode(TechNode node, TechTreeNode parent){
            this.node = node;
            this.parent = parent;
            this.width = this.height = ui.research.nodeSize;
            nodes.add(this);
            if(node.children != null){
                children = new TechTreeNode[node.children.size];
                for(int i = 0; i < children.length; i++){
                    children[i] = new TechTreeNode(node.children.get(i), this);
                }
            }
        }
    }

    public class View extends Group{
        public float panX = 0, panY = -200, lastZoom = -1;
        public boolean moved = false;
        public ImageButton hoverNode;
        public Table infoTable = new Table();

        {
            rebuildAll();
        }

        public void putCurrent(){
            Core.settings.put("current", current.localizedName);
            Core.settings.put("sectors", currentSectors);
        }

        public void delCurrent(){
            Core.settings.remove("current");
            Core.settings.remove("sectors");
        }

        public Quest[] completedFocus(){
            Seq<Content> focusList = content.getBy(ContentType.typeid_UNUSED).select(c -> c instanceof Quest);
            Quest[] arr = new Quest[focusList.size];
            for (int i = 0;i < focusList.size; i++){
                if (((Quest)focusList.get(i)).unlocked()) arr[i] = (Quest)focusList.get(i);
            }
            return arr;
        }

        public void editSettings(Item item, int amount){
            if (Core.settings.has(item.localizedName)) Core.settings.put(item.localizedName, amount);
        }

        public void rebuildAll(){
            clear();
            hoverNode = null;
            infoTable.clear();
            infoTable.touchable = Touchable.enabled;

            current = all(Core.settings.getString("current", null));
            currentSectors = Core.settings.getInt("sectors", -1);

            for(TechTreeNode node : nodes){
                ImageButton button = new ImageButton(node.node.content.fullIcon.found() ? node.node.content.fullIcon : Core.atlas.find("unknown"), Styles.nodei);
                button.getImageCell().marginTop(3f).marginBottom(3f);
                button.resizeImage(96f);
                button.visible(() -> node.visible);

                button.clicked(() -> {
                    if(moved) return;

                    if(mobile){
                        hoverNode = button;
                        rebuild();
                        float right = infoTable.getRight();
                        if(right > Core.graphics.getWidth()){
                            float moveBy = right - Core.graphics.getWidth();
                            addAction(new RelativeTemporalAction(){
                                {
                                    setDuration(0.1f);
                                    setInterpolation(Interp.fade);
                                }

                                @Override
                                protected void updateRelative(float percentDelta){
                                    panX -= moveBy * percentDelta;
                                }
                            });
                        }
                    }else if(canSpend(node.node) && locked(node.node)){
                        spend(node.node);
                    }
                });
                button.hovered(() -> {
                    if(!mobile && hoverNode != button && node.visible){
                        hoverNode = button;
                        rebuild();
                    }
                });
                button.exited(() -> {
                    if(!mobile && hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                        hoverNode = null;
                        rebuild();
                    }
                });
                button.touchable(() -> !node.visible ? Touchable.disabled : Touchable.enabled);
                button.userObject = node.node;
                button.setSize(Scl.scl(120f));
                button.update(() -> {
                    float offset = (Core.graphics.getHeight() % 2) / 2f;
                    button.setPosition(node.x + panX + width / 2f, node.y + panY + height / 2f + offset, Align.center);
                    button.getStyle().up = !locked(node.node) ? Tex.buttonOver : !selectable(node.node) || !canSpend(node.node) ? Tex.buttonRed
                            : current == null ? Tex.button : node.node.content == current ? FocusUILoader.buttonGreen : Tex.button;
                    ((TextureRegionDrawable)button.getStyle().imageUp).setRegion(node.node.content.fullIcon);
                    button.getImage().setColor(!locked(node.node) ? Color.white : node.selectable ? Color.gray : Pal.gray);
                    button.getImage().setScaling(Scaling.bounded);
                });
                addChild(button);
            }

            if(mobile){
                tapped(() -> {
                    Element e = Core.scene.hit(Core.input.mouseX(), Core.input.mouseY(), true);
                    if(e == this){
                        hoverNode = null;
                        rebuild();
                    }
                });
            }

            setOrigin(Align.center);
            setTransform(true);
            released(() -> moved = false);
        }

        void clamp(){
            float pad = Scl.scl(128f);

            float ox = width/2f, oy = height/2f;
            float rx = bounds.x + panX + ox, ry = panY + oy + bounds.y;
            float rw = bounds.width, rh = bounds.height;
            rx = Mathf.clamp(rx, -rw + pad, Core.graphics.getWidth() - pad);
            ry = Mathf.clamp(ry, -rh + pad, Core.graphics.getHeight() - pad);
            panX = rx - bounds.x - ox;
            panY = ry - bounds.y - oy;
        }

        boolean canSpend(TechNode node){
            if(!selectable(node)) return false;

            if(node.requirements.length == 0) return true;

            //can spend when there's at least 1 item that can be spent (non complete)
            for(int i = 0; i < node.requirements.length; i++){
                if(node.finishedRequirements[i].amount < node.requirements[i].amount && items.has(node.requirements[i].item)){
                    return true;
                }
            }

            //can always spend when locked
            return node.content.locked();
        }

        void spend(TechNode node){
            boolean complete = true;

            boolean[] shine = new boolean[node.requirements.length];
            boolean[] usedShine = new boolean[content.items().size];

            if (current == null) {
                for (int i = 0; i < node.requirements.length; i++) {
                    ItemStack req = node.requirements[i];
                    ItemStack completed = node.finishedRequirements[i];

                    //amount actually taken from inventory
                    int used = Math.max(Math.min(req.amount - completed.amount, items.get(req.item)), 0);
                    items.remove(req.item, used);
                    completed.amount += used;

                    if (used > 0) {
                        shine[i] = true;
                        usedShine[req.item.id] = true;
                    }

                    //disable completion if the completed amount has not reached requirements
                    if (completed.amount < req.amount) {
                        complete = false;
                    }
                }

                if(complete){
                    current = (Quest)node.content;
                    currentSectors = completed();
                    putCurrent();
                    researchSound.play();
                }
            } else if (reqComplete(node)){
                unlock(node);
                items = items();
                current = null;
                delCurrent();
                currentSectors = -1;
            }

            node.save();

            //??????
            Core.scene.act();
            rebuild(shine);
            itemDisplay.rebuild(items, usedShine);
            questDisplay.rebuild(current);
        }

        void unlock(TechNode node){
            node.content.unlock();
            Log.info(node.localizedName() + "unlock");

            //unlock parent nodes in multiplayer.
            TechNode parent = node.parent;
            while(parent != null){
                parent.content.unlock();
                parent = parent.parent;
            }

            checkNodes(root);
            hoverNode = null;
            treeLayout();
            rebuild();
            Core.scene.act();
            Sounds.unlock.play();
            Events.fire(new ResearchEvent(node.content));
            ui.research.updateVisibility();
        }

        boolean reqComplete(TechNode node){
            return completed() >= currentSectors + ((Quest)node.content).addSectors;
        }

        void rebuild(){
            rebuild(null);
        }

        //pass an array of stack indexes that should shine here
        void rebuild(@Nullable boolean[] shine){
            ImageButton button = hoverNode;

            infoTable.remove();
            infoTable.clear();
            infoTable.update(null);

            if(button == null) return;

            TechNode node = (TechNode)button.userObject;

            infoTable.exited(() -> {
                if(hoverNode == button && !infoTable.hasMouse() && !hoverNode.hasMouse()){
                    hoverNode = null;
                    rebuild();
                }
            });

            infoTable.update(() -> infoTable.setPosition(button.x + button.getWidth(), button.y + button.getHeight(), Align.topLeft));

            infoTable.left();
            infoTable.background(Tex.button).margin(8f);

            boolean selectable = selectable(node);

            infoTable.row();

            infoTable.table(b -> {
                b.margin(0).left().defaults().left();

                if(node.content.description != null || node.content.stats.toMap().size > 0){
                    b.button(Icon.info, Styles.flati, () -> ui.content.show(node.content)).growY().width(50f);
                }

                b.add().grow();
                b.table(desc -> {
                    desc.left().defaults().left();
                    desc.add(node.content.localizedName).color(Pal.accent);
                    desc.row();

                    if (node.content == current){
                        desc.add(Core.bundle.format("quests.researching")).color(Color.red);
                        desc.row();
                    }

                    if (node.content.description != null && node.content.inlineDescription) {
                        desc.table(u -> u.margin(0).left().add('"' + node.content.description + '"').color(Color.sky).growX()).fillX();
                        desc.row();
                    }

                    if(locked(node) || debugShowRequirements){
                        desc.table(t -> {
                            t.left();

                            if(selectable){
                                if (current != node.content) {
                                    //check if there is any progress, add research progress text
                                    if (Structs.contains(node.finishedRequirements, s -> s.amount > 0)) {
                                        float sum = 0f, used = 0f;
                                        boolean shiny = false;

                                        for (int i = 0; i < node.requirements.length; i++) {
                                            sum += node.requirements[i].item.cost * node.requirements[i].amount;
                                            used += node.finishedRequirements[i].item.cost * node.finishedRequirements[i].amount;
                                            if (shine != null) shiny |= shine[i];
                                        }

                                        Label label = t.add(Core.bundle.format("research.progress", Math.min((int) (used / sum * 100), 99))).left().get();

                                        if (shiny) {
                                            label.setColor(Pal.accent);
                                            label.actions(Actions.color(Color.lightGray, 0.75f, Interp.fade));
                                        } else {
                                            label.setColor(Color.lightGray);
                                        }

                                        t.row();
                                    }

                                    for (int i = 0; i < node.requirements.length; i++) {
                                        ItemStack req = node.requirements[i];
                                        ItemStack completed = node.finishedRequirements[i];

                                        //skip finished stacks
                                        if (req.amount <= completed.amount && !debugShowRequirements) continue;
                                        boolean shiny = shine != null && shine[i];

                                        t.table(list -> {
                                            int reqAmount = debugShowRequirements ? req.amount : req.amount - completed.amount;

                                            list.left();
                                            list.image(req.item.uiIcon).size(8 * 3).padRight(3);
                                            list.add(req.item.localizedName).color(Color.lightGray);
                                            Label label = list.label(() -> " " +
                                                    UI.formatAmount(Math.min(items.get(req.item), reqAmount)) + " / "
                                                    + UI.formatAmount(reqAmount)).get();

                                            Color targetColor = items.has(req.item) ? Color.lightGray : Color.scarlet;

                                            if (shiny) {
                                                label.setColor(Pal.accent);
                                                label.actions(Actions.color(targetColor, 0.75f, Interp.fade));
                                            } else {
                                                label.setColor(targetColor);
                                            }

                                        }).fillX().left();
                                        t.row();
                                    }
                                }
                            }else if(current != node.content){
                                t.table(r -> {
                                    r.left();
                                    r.add("@complete").color(Color.white).left();
                                    r.row();
                                    for(Objective o : node.objectives){
                                        r.table(ob -> {
                                            ob.marginLeft(9f).left();
                                            ob.add(o.display()).color(o.complete() ? Color.lime : Color.red).left();
                                            ob.image(o.complete() ? Icon.ok : Icon.cancel, o.complete() ? Color.lime : Color.scarlet).padLeft(6).left();
                                        }).left();
                                        r.row();
                                    }
                                }).left();
                                t.row();
                            }
                        }).left();
                    }else{
                        desc.add("@completed");
                    }
                }).pad(9);

                if(mobile && locked(node)){
                    b.row();
                    b.button("@research", Icon.ok, new TextButtonStyle(){{
                                disabled = Tex.button;
                                font = Fonts.def;
                                fontColor = Color.white;
                                disabledFontColor = Color.gray;
                                up = buttonOver;
                                over = buttonDown;
                            }}, () -> spend(node))
                            .disabled(i -> !canSpend(node)).growX().height(44f).colspan(3);
                }

            });
            infoTable.row();

            if (current != node.content) {
                if (!node.content.unlocked()) {
                    infoTable.table(t -> {
                        t.marginLeft(9f).left();

                        t.table(b -> {
                            b.left();
                            if (node.parent != null) {
                                b.add(Core.bundle.format("quests.prerequisite")).color(Color.orange).left();
                                b.row();
                                b.table(p -> {
                                    p.marginLeft(9f).left();
                                    p.add(node.parent.content.localizedName).color(node.parent.content.unlocked() ? Color.white : Color.scarlet).left();
                                    p.image(node.parent.content.unlocked() ? Icon.ok : Icon.cancel, node.parent.content.unlocked() ? Color.lime : Color.scarlet).padLeft(6f).left();
                                }).left();
                                b.row();

                                Objective pre = node.objectives.find(o -> o instanceof QObjectives.focusResearch);
                                if (pre != null) {
                                    Quest[] foc = ((QObjectives.focusResearch) pre).prerequisite;

                                    for (Quest f : foc) {
                                        b.table(fo -> {
                                            fo.left().marginLeft(9f);
                                            fo.add(f.localizedName).color(f.unlocked() ? Color.white : Color.red).left();
                                            fo.image(f.unlocked() ? Icon.ok : Icon.cancel, f.unlocked() ? Color.lime : Color.scarlet).left().padLeft(6f);
                                        }).left();
                                        b.row();
                                    }
                                }
                            } else {
                                b.add(Core.bundle.format("quests.nonerequired")).color(Color.red).left();
                                b.row();
                            }
                        }).left();
                    }).left();

                    infoTable.row();
                    if (((Quest) node.content).rewards.length > 0) {
                        infoTable.table(b -> {
                            b.margin(3f).left().defaults().left();
                            ItemStack[] rew = ((Quest) node.content).rewards;

                            b.add(Core.bundle.format("quests.reward")).color(Color.lime);
                            for (ItemStack i : rew) {
                                b.row();
                                b.table(re -> {
                                    re.left().margin(4f).marginLeft(9f);
                                    re.image(i.item.uiIcon).size(20f).left().padRight(6f);
                                    re.add(Core.bundle.format("quests.rewards", i.item.localizedName, i.amount)).color(Color.white);
                                }).left();
                            }
                        }).margin(9f).left();
                    }

                    infoTable.row();

                    if (((Quest) node.content).unlockContents.size > 0) {
                        infoTable.table(r -> {
                            r.margin(3f).left().defaults().left();

                            r.add(Core.bundle.format("quests.additional")).color(Pal.accent);
                            r.row();
                            for (UnlockableContent c : ((Quest) node.content).unlockContents) {
                                r.table(u -> {
                                    u.left().margin(3f).marginLeft(9f);
                                    u.image(c.uiIcon).size(20f).left().padRight(6f);
                                    u.add(c.localizedName).color(Color.white).left();
                                }).left().pad(3f);
                                r.row();
                            }
                        }).margin(9f).left();
                    }
                }
            } else {
                infoTable.table(tbl -> {
                    tbl.margin(3f).left().defaults().left();
                    tbl.add(Core.bundle.format("quests.untilcomplete"));
                    tbl.row();
                    tbl.table(ad -> {
                        ad.margin(3f).left().marginLeft(9f);
                        ad.add(Core.bundle.format("quests.moresectors", completed() - currentSectors, ((Quest)node.content).addSectors, reqComplete(node) ? "green" : "red")).color(Color.white);
                    }).marginLeft(9f).left();
                }).margin(9f).left();
            }

            addChild(infoTable);
            infoTable.pack();
            infoTable.act(Core.graphics.getDeltaTime());
        }

        @Override
        public void drawChildren(){
            clamp();
            float offsetX = panX + width / 2f, offsetY = panY + height / 2f;
            Draw.sort(true);

            for(TechTreeNode node : nodes){
                if(!node.visible) continue;
                for(TechTreeNode child : node.children){
                    if(!child.visible) continue;
                    boolean lock = locked(node.node) || locked(child.node);
                    Draw.z(lock ? 1f : 2f);

                    Lines.stroke(Scl.scl(8f), lock ? Pal.gray : Pal.accent);
                    Draw.alpha(parentAlpha);
                    if(Mathf.equal(Math.abs(node.y - child.y), Math.abs(node.x - child.x), 1f) && Mathf.dstm(node.x, node.y, child.x, child.y) <= node.width*3){
                        Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                    }else{
                        Lines.line(node.x + offsetX, node.y + offsetY, child.x + offsetX, node.y + offsetY);
                        Lines.line(child.x + offsetX, node.y + offsetY, child.x + offsetX, child.y + offsetY);
                    }
                }
            }

            Draw.sort(false);
            Draw.reset();
            super.drawChildren();
        }
    }
}
