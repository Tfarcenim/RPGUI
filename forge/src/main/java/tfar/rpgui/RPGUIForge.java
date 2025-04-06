package tfar.rpgui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.NamedGuiOverlay;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.commons.lang3.tuple.Pair;

@Mod(RPGUI.MOD_ID)
public class RPGUIForge {

    public static RPGUIForge CONFIG;
    public static ForgeConfigSpec CLIENT_SPEC;

    static ForgeConfigSpec.IntValue height;
    static ForgeConfigSpec.IntValue width;

    static ForgeConfigSpec.IntValue xp_xPos;
    static ForgeConfigSpec.IntValue xp_yPos;

    public RPGUIForge() {
            final Pair<RPGUIForge, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(builder -> {
                width = builder.defineInRange("xPos",() ->-182,-1000000,1000000);
                height = builder.defineInRange("yPos",() ->-80,-1000000,1000000);

                xp_xPos = builder.defineInRange("xp_xPos",() ->-182+55,-1000000,1000000);
                xp_yPos = builder.defineInRange("xp_yPos",() ->-80,-1000000,1000000);

                return this;
            });
            CLIENT_SPEC = specPair.getRight();
            CONFIG = specPair.getLeft();

        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        // This method is invoked by the Forge mod loader when it is ready
        // to load your mod. You can access Forge and Common code in this
        // project.
        bus.addListener(this::registerOverlay);
        MinecraftForge.EVENT_BUS.addListener(this::disableOthers);
        // Use Forge to bootstrap the Common mod.
        RPGUI.init();
    }


    void registerOverlay(RegisterGuiOverlaysEvent event) {
        event.registerAbove(VanillaGuiOverlay.PLAYER_HEALTH.id(),"overlay",RPGUIForge::renderOverlay);
        event.registerAbove(VanillaGuiOverlay.EXPERIENCE_BAR.id(),"xp_overlay",RPGUIForge::renderXPOverlay);
    }

    static int background_width = 75;
    static int background_height = 66;
    static int diamond_height = 30;
    static int health_offset = background_width + diamond_height;

    static final ResourceLocation overlay = new ResourceLocation(RPGUI.MOD_ID,"textures/gui/layout.png");

    static void renderXPOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (gui.getMinecraft().player.jumpableVehicle() == null && !gui.getMinecraft().options.hideGui) {
            gui.setupOverlayRenderState(true, false);
            if (gui.getMinecraft().gameMode.hasExperience()) {
                Minecraft minecraft = Minecraft.getInstance();
                Font font = minecraft.font;
                minecraft.getProfiler().push("expLevel");
                String s = "" + minecraft.player.totalExperience;
                int xPos = screenWidth / 2 + xp_xPos.get();
                int yPos = screenHeight + xp_yPos.get();

                guiGraphics.drawString(font, s, xPos + 1, yPos, 0, false);
                guiGraphics.drawString(font, s, xPos - 1, yPos, 0, false);
                guiGraphics.drawString(font, s, xPos, yPos + 1, 0, false);
                guiGraphics.drawString(font, s, xPos, yPos - 1, 0, false);
                guiGraphics.drawString(font, s, xPos, yPos, 0x80ff20, false);
                minecraft.getProfiler().pop();
            }
        }
    }


    static void renderOverlay(ForgeGui gui, GuiGraphics guiGraphics, float partialTick, int screenWidth, int screenHeight) {
        if (!gui.getMinecraft().options.hideGui) {
            gui.setupOverlayRenderState(true, false);
            if (gui.getMinecraft().gameMode.getPlayerMode() == GameType.SPECTATOR) {
                gui.getSpectatorGui().renderHotbar(guiGraphics);
            } else {
                Entity entity = Minecraft.getInstance().cameraEntity;
                if (entity instanceof Player player) {

                    int xPos = screenWidth / 2 + width.get();
                    int yPos = screenHeight + height.get();
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
    private static void renderSlot(GuiGraphics guiGraphics, int x, int y, float partialTick, Player player, ItemStack stack, int seed) {
        if (!stack.isEmpty()) {
            float f = (float)stack.getPopTime() - partialTick;
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

    void disableOthers(RenderGuiOverlayEvent.Pre event) {
        NamedGuiOverlay overlay = event.getOverlay();
        if (overlay == VanillaGuiOverlay.PLAYER_HEALTH.type() ||overlay == VanillaGuiOverlay.HOTBAR.type() || overlay == VanillaGuiOverlay.EXPERIENCE_BAR.type()) {
            event.setCanceled(true);
        }
    }
}