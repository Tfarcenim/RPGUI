package tfar.rpgui;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.common.NeoForge;
import org.apache.commons.lang3.tuple.Pair;

@Mod(RPGUI.MOD_ID)
public class RPGUIForge {

    public static RPGUIForge CONFIG;
    public static ModConfigSpec CLIENT_SPEC;

    static ModConfigSpec.IntValue height;
    static ModConfigSpec.IntValue width;

    static ModConfigSpec.IntValue xp_xPos;
    static ModConfigSpec.IntValue xp_yPos;

    public RPGUIForge(IEventBus bus,ModContainer container) {
            final Pair<RPGUIForge, ModConfigSpec> specPair = new ModConfigSpec.Builder().configure(builder -> {
                width = builder.defineInRange("xPos",() ->-182,-1000000,1000000);
                height = builder.defineInRange("yPos",() ->-80,-1000000,1000000);

                xp_xPos = builder.defineInRange("xp_xPos",() ->-182+55,-1000000,1000000);
                xp_yPos = builder.defineInRange("xp_yPos",() ->-80,-1000000,1000000);

                return this;
            });
            CLIENT_SPEC = specPair.getRight();
            CONFIG = specPair.getLeft();

        container.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
        bus.addListener(this::registerOverlay);
        NeoForge.EVENT_BUS.addListener(this::disableOthers);
        // Use Forge to bootstrap the Common mod.
        RPGUI.init();
    }


    void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.PLAYER_HEALTH,RPGUI.id("overlay"),RPGUIForge::renderOverlay);
        event.registerAbove(VanillaGuiLayers.EXPERIENCE_BAR,RPGUI.id("xp_overlay"),RPGUIForge::renderXPOverlay);
    }

    static int background_width = 75;
    static int background_height = 66;
    static int diamond_height = 30;
    static int health_offset = background_width + diamond_height;

    static final ResourceLocation overlay = ResourceLocation.fromNamespaceAndPath(RPGUI.MOD_ID,"textures/gui/layout.png");

    static void renderXPOverlay(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        if (Minecraft.getInstance().player.jumpableVehicle() == null && !Minecraft.getInstance().options.hideGui) {
            if (Minecraft.getInstance().gameMode.hasExperience()) {
                Minecraft minecraft = Minecraft.getInstance();
                Font font = minecraft.font;
                minecraft.getProfiler().push("expLevel");
                String s = "" + minecraft.player.totalExperience;
                int xPos = guiGraphics.guiWidth() / 2 + xp_xPos.get();
                int yPos = guiGraphics.guiHeight() + xp_yPos.get();

                guiGraphics.drawString(font, s, xPos + 1, yPos, 0, false);
                guiGraphics.drawString(font, s, xPos - 1, yPos, 0, false);
                guiGraphics.drawString(font, s, xPos, yPos + 1, 0, false);
                guiGraphics.drawString(font, s, xPos, yPos - 1, 0, false);
                guiGraphics.drawString(font, s, xPos, yPos, 0x80ff20, false);
                minecraft.getProfiler().pop();
            }
        }
    }


    static void renderOverlay(GuiGraphics guiGraphics, DeltaTracker partialTick) {
        if (!Minecraft.getInstance().options.hideGui) {
            if (Minecraft.getInstance().gameMode.getPlayerMode() == GameType.SPECTATOR) {
                Minecraft.getInstance().gui.getSpectatorGui().renderHotbar(guiGraphics);
            } else {
                Entity entity = Minecraft.getInstance().cameraEntity;
                if (entity instanceof Player player) {

                    int xPos = guiGraphics.guiWidth() / 2 + width.get();
                    int yPos = guiGraphics.guiHeight() + height.get();
                    guiGraphics.blit(overlay, xPos, yPos, 0, 0, background_width, background_height);

                    double healthFill = player.getHealth() / player.getMaxHealth();


                    int fill = (int) (30 * healthFill);

                    //75,18
                    guiGraphics.blit(overlay,xPos+41,yPos+17 + (31 - fill),background_width,18 + (30 - fill),30, fill);

                    double absorptionFill = Math.min(player.getAbsorptionAmount()/player.getMaxHealth(),1);
                    fill = (int) (30 * absorptionFill);
                    //75+30,18
                    guiGraphics.blit(overlay,xPos+41,yPos+17 + (31 - fill),background_width+diamond_height,18 + (30 - fill),30, fill);

                    ItemStack current = player.getInventory().getSelected();
                    if (!current.isEmpty()) {
                        renderSlot(guiGraphics,xPos+9,yPos+26,partialTick,player,current,1);
                    }

                    int su = (player.getInventory().selected-1) %9;
                    if (su < 0)su+=9;

                    ItemStack up = player.getInventory().getItem(su);
                    if (!up.isEmpty()) {
                        renderSlot(guiGraphics,xPos+28,yPos+6,partialTick,player,up,1);
                    }

                    int sn = (player.getInventory().selected+1) %9;

                    ItemStack down = player.getInventory().getItem(sn);
                    if (!down.isEmpty()) {
                        renderSlot(guiGraphics,xPos+28,yPos+46,partialTick,player,down,1);
                    }
                }
            }
        }
    }

    /**
     * Renders a slot with the specified item stack at the given position on the screen.
     *
     * @param guiGraphics the graphics object used for rendering.
     * @param x           the x-coordinate of the slot.
     * @param y           the y-coordinate of the slot.
     * @param partialTick the partial tick value for smooth animation.
     * @param player      the player associated with the slot.
     * @param stack       the item stack to render in the slot.
     * @param seed        the seed value used for random rendering variations.
     */
    private static void renderSlot(GuiGraphics guiGraphics, int x, int y, DeltaTracker partialTick, Player player, ItemStack stack, int seed) {
        if (!stack.isEmpty()) {
            float f = (float)stack.getPopTime() - partialTick.getGameTimeDeltaPartialTick(false);
            if (f > 0.0F) {
                float f1 = 1.0F + f / 5.0F;
                guiGraphics.pose().pushPose();
                guiGraphics.pose().translate((float)(x + 8), (float)(y + 12), 0.0F);
                guiGraphics.pose().scale(1.0F / f1, (f1 + 1.0F) / 2.0F, 1.0F);
                guiGraphics.pose().translate((float)(-(x + 8)), (float)(-(y + 12)), 0.0F);
            }

            guiGraphics.renderItem(player, stack, x, y, seed);
            if (f > 0.0F) {
                guiGraphics.pose().popPose();
            }

            guiGraphics.renderItemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }

    void disableOthers(RenderGuiLayerEvent.Pre event) {
        ResourceLocation overlay = event.getName();
        if (overlay.equals(VanillaGuiLayers.PLAYER_HEALTH) || overlay.equals(VanillaGuiLayers.HOTBAR) || overlay.equals(VanillaGuiLayers.EXPERIENCE_BAR) || overlay.equals(VanillaGuiLayers.EXPERIENCE_LEVEL)) {
            event.setCanceled(true);
        }
    }
}