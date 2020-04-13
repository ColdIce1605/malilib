package fi.dy.masa.malilib.render;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import javax.annotation.Nullable;
import org.lwjgl.opengl.GL11;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.SpriteAtlasTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.util.math.Vector3f;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapState;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import fi.dy.masa.malilib.config.HudAlignment;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.util.Color4f;
import fi.dy.masa.malilib.util.GuiUtils;
import fi.dy.masa.malilib.util.IntBoundingBox;
import fi.dy.masa.malilib.util.InventoryUtils;
import fi.dy.masa.malilib.util.PositionUtils;
import fi.dy.masa.malilib.util.PositionUtils.HitPart;

public class RenderUtils
{
    public static final Identifier TEXTURE_MAP_BACKGROUND = new Identifier("textures/map/map_background.png");

    private static final Random RAND = new Random();
    //private static final Vec3d LIGHT0_POS = (new Vec3d( 0.2D, 1.0D, -0.7D)).normalize();
    //private static final Vec3d LIGHT1_POS = (new Vec3d(-0.2D, 1.0D,  0.7D)).normalize();

    public static void setupBlend()
    {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SrcFactor.ONE, GlStateManager.DstFactor.ZERO);
    }

    public static void setupBlendSimple()
    {
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
    }

    public static void bindTexture(Identifier texture)
    {
        mc().getTextureManager().bindTexture(texture);
    }

    public static void color(float r, float g, float b, float a)
    {
        RenderSystem.color4f(r, g, b, a);
    }

    public static void disableDiffuseLighting()
    {
        // FIXME 1.15-pre4+
        DiffuseLighting.disable();
    }

    public static void enableDiffuseLightingForLevel(MatrixStack matrixStack)
    {
        DiffuseLighting.enableForLevel(matrixStack.peek().getModel());
    }

    public static void enableDiffuseLightingGui3D()
    {
        // FIXME 1.15-pre4+
        DiffuseLighting.enableGuiDepthLighting();
    }

    public static void drawOutlinedBox(int x, int y, int width, int height, int colorBg, int colorBorder)
    {
        drawOutlinedBox(x, y, width, height, colorBg, colorBorder, 0f);
    }

    public static void drawOutlinedBox(int x, int y, int width, int height, int colorBg, int colorBorder, float zLevel)
    {
        // Draw the background
        drawRect(x, y, width, height, colorBg, zLevel);

        // Draw the border
        drawOutline(x - 1, y - 1, width + 2, height + 2, colorBorder, zLevel);
    }

    public static void drawOutline(int x, int y, int width, int height, int colorBorder)
    {
        drawOutline(x, y, width, height, colorBorder, 0f);
    }

    public static void drawOutline(int x, int y, int width, int height, int colorBorder, float zLevel)
    {
        drawRect(x                    , y,      1, height, colorBorder, zLevel); // left edge
        drawRect(x + width - 1        , y,      1, height, colorBorder, zLevel); // right edge
        drawRect(x + 1,              y, width - 2,      1, colorBorder, zLevel); // top edge
        drawRect(x + 1, y + height - 1, width - 2,      1, colorBorder, zLevel); // bottom edge
    }

    public static void drawOutline(int x, int y, int width, int height, int borderWidth, int colorBorder)
    {
        drawOutline(x, y, width, height, borderWidth, colorBorder, 0f);
    }

    public static void drawOutline(int x, int y, int width, int height, int borderWidth, int colorBorder, float zLevel)
    {
        drawRect(x                      ,                        y, borderWidth            , height     , colorBorder, zLevel); // left edge
        drawRect(x + width - borderWidth,                        y, borderWidth            , height     , colorBorder, zLevel); // right edge
        drawRect(x + borderWidth        ,                        y, width - 2 * borderWidth, borderWidth, colorBorder, zLevel); // top edge
        drawRect(x + borderWidth        , y + height - borderWidth, width - 2 * borderWidth, borderWidth, colorBorder, zLevel); // bottom edge
    }

    public static void drawTexturedRect(int x, int y, int u, int v, int width, int height)
    {
        drawTexturedRect(x, y, u, v, width, height, 0);
    }

    public static void drawRect(int x, int y, int width, int height, int color)
    {
        drawRect(x, y, width, height, color, 0f);
    }

    public static void drawRect(int x, int y, int width, int height, int color, float zLevel)
    {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >>  8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        RenderSystem.disableTexture();
        setupBlend();
        color(r, g, b, a);

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION);

        buffer.vertex(x        , y         , zLevel).next();
        buffer.vertex(x        , y + height, zLevel).next();
        buffer.vertex(x + width, y + height, zLevel).next();
        buffer.vertex(x + width, y         , zLevel).next();

        tessellator.draw();

        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    public static void drawTexturedRect(int x, int y, int u, int v, int width, int height, float zLevel)
    {
        float pixelWidth = 0.00390625F;
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        setupBlend();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);

        buffer.vertex(x        , y + height, zLevel).texture( u          * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y + height, zLevel).texture((u + width) * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y         , zLevel).texture((u + width) * pixelWidth,  v           * pixelWidth).next();
        buffer.vertex(x        , y         , zLevel).texture( u          * pixelWidth,  v           * pixelWidth).next();

        tessellator.draw();
    }

    public static void drawTexturedRectBatched(int x, int y, int u, int v, int width, int height, BufferBuilder buffer)
    {
        drawTexturedRectBatched(x, y, u, v, width, height, 0, buffer);
    }

    public static void drawTexturedRectBatched(int x, int y, int u, int v, int width, int height, float zLevel, BufferBuilder buffer)
    {
        float pixelWidth = 0.00390625F;

        buffer.vertex(x        , y + height, zLevel).texture( u          * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y + height, zLevel).texture((u + width) * pixelWidth, (v + height) * pixelWidth).next();
        buffer.vertex(x + width, y         , zLevel).texture((u + width) * pixelWidth,  v           * pixelWidth).next();
        buffer.vertex(x        , y         , zLevel).texture( u          * pixelWidth,  v           * pixelWidth).next();
    }

    public static void drawHoverText(int x, int y, List<String> textLines)
    {
        MinecraftClient mc = mc();

        if (textLines.isEmpty() == false && GuiUtils.getCurrentScreen() != null)
        {
            TextRenderer font = mc.textRenderer;
            RenderSystem.disableRescaleNormal();
            disableDiffuseLighting();
            RenderSystem.disableLighting();
            RenderSystem.disableDepthTest();
            int maxLineLength = 0;
            int maxWidth = GuiUtils.getCurrentScreen().width;
            List<String> linesNew = new ArrayList<>();

            for (String lineOrig : textLines)
            {
                String[] lines = lineOrig.split("\\n");

                for (String line : lines)
                {
                    int length = font.getStringWidth(line);

                    if (length > maxLineLength)
                    {
                        maxLineLength = length;
                    }

                    linesNew.add(line);
                }
            }

            textLines = linesNew;

            final int lineHeight = font.fontHeight + 1;
            int textHeight = textLines.size() * lineHeight - 2;
            int textStartX = x + 4;
            int textStartY = Math.max(8, y - textHeight - 6);

            if (textStartX + maxLineLength + 6 > maxWidth)
            {
                textStartX = Math.max(2, maxWidth - maxLineLength - 8);
            }

            double zLevel = 300;
            int borderColor = 0xF0100010;
            drawGradientRect(textStartX - 3, textStartY - 4, textStartX + maxLineLength + 3, textStartY - 3, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX - 3, textStartY + textHeight + 3, textStartX + maxLineLength + 3, textStartY + textHeight + 4, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX - 3, textStartY - 3, textStartX + maxLineLength + 3, textStartY + textHeight + 3, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX - 4, textStartY - 3, textStartX - 3, textStartY + textHeight + 3, zLevel, borderColor, borderColor);
            drawGradientRect(textStartX + maxLineLength + 3, textStartY - 3, textStartX + maxLineLength + 4, textStartY + textHeight + 3, zLevel, borderColor, borderColor);

            int fillColor1 = 0x505000FF;
            int fillColor2 = 0x5028007F;
            drawGradientRect(textStartX - 3, textStartY - 3 + 1, textStartX - 3 + 1, textStartY + textHeight + 3 - 1, zLevel, fillColor1, fillColor2);
            drawGradientRect(textStartX + maxLineLength + 2, textStartY - 3 + 1, textStartX + maxLineLength + 3, textStartY + textHeight + 3 - 1, zLevel, fillColor1, fillColor2);
            drawGradientRect(textStartX - 3, textStartY - 3, textStartX + maxLineLength + 3, textStartY - 3 + 1, zLevel, fillColor1, fillColor1);
            drawGradientRect(textStartX - 3, textStartY + textHeight + 2, textStartX + maxLineLength + 3, textStartY + textHeight + 3, zLevel, fillColor2, fillColor2);

            for (int i = 0; i < textLines.size(); ++i)
            {
                String str = textLines.get(i);
                font.drawWithShadow(str, textStartX, textStartY, 0xFFFFFFFF);
                textStartY += lineHeight;
            }

            RenderSystem.enableLighting();
            RenderSystem.enableDepthTest();
            enableDiffuseLightingGui3D();
            RenderSystem.enableRescaleNormal();
        }
    }

    public static void drawGradientRect(int left, int top, int right, int bottom, double zLevel, int startColor, int endColor)
    {
        int sa = (startColor >> 24 & 0xFF);
        int sr = (startColor >> 16 & 0xFF);
        int sg = (startColor >>  8 & 0xFF);
        int sb = (startColor & 0xFF);

        int ea = (endColor >> 24 & 0xFF);
        int er = (endColor >> 16 & 0xFF);
        int eg = (endColor >>  8 & 0xFF);
        int eb = (endColor & 0xFF);

        RenderSystem.disableTexture();
        RenderSystem.disableAlphaTest();
        setupBlend();
        RenderSystem.shadeModel(GL11.GL_SMOOTH);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        buffer.vertex(right, top,    zLevel).color(sr, sg, sb, sa).next();
        buffer.vertex(left,  top,    zLevel).color(sr, sg, sb, sa).next();
        buffer.vertex(left,  bottom, zLevel).color(er, eg, eb, ea).next();
        buffer.vertex(right, bottom, zLevel).color(er, eg, eb, ea).next();

        tessellator.draw();

        RenderSystem.shadeModel(GL11.GL_FLAT);
        RenderSystem.disableBlend();
        RenderSystem.enableAlphaTest();
        RenderSystem.enableTexture();
    }

    public static void drawCenteredString(int x, int y, int color, String text)
    {
        TextRenderer textRenderer = mc().textRenderer;
        textRenderer.drawWithShadow(text, x - textRenderer.getStringWidth(text) / 2, y, color);
    }

    public static void drawHorizontalLine(int x, int y, int width, int color)
    {
        drawRect(x, y, width, 1, color);
    }

    public static void drawVerticalLine(int x, int y, int height, int color)
    {
        drawRect(x, y, 1, height, color);
    }

    public static void renderSprite(int x, int y, int width, int height, Identifier atlas, Identifier texture)
    {
        if (texture != null)
        {
            Sprite sprite = mc().getSpriteAtlas(atlas).apply(texture);
            RenderSystem.disableLighting();
            DrawableHelper.drawSprite(x, y, 0, width, height, sprite);//.drawTexturedRect(x, y, sprite, width, height);
        }
    }

    public static void renderText(int x, int y, int color, String text)
    {
        String[] parts = text.split("\\\\n");
        TextRenderer textRenderer = mc().textRenderer;

        for (String line : parts)
        {
            textRenderer.drawWithShadow(line, x, y, color);
            y += textRenderer.fontHeight + 1;
        }
    }

    public static void renderText(int x, int y, int color, List<String> lines)
    {
        if (lines.isEmpty() == false)
        {
            TextRenderer textRenderer = mc().textRenderer;

            for (String line : lines)
            {
                textRenderer.draw(line, x, y, color);
                y += textRenderer.fontHeight + 2;
            }
        }
    }

    public static int renderText(int xOff, int yOff, double scale, int textColor, int bgColor,
            HudAlignment alignment, boolean useBackground, boolean useShadow, List<String> lines)
    {
        TextRenderer fontRenderer = mc().textRenderer;
        final int scaledWidth = GuiUtils.getScaledWindowWidth();
        final int lineHeight = fontRenderer.fontHeight + 2;
        final int contentHeight = lines.size() * lineHeight - 2;
        final int bgMargin = 2;

        // Only Chuck Norris can divide by zero
        if (scale == 0d)
        {
            return 0;
        }

        if (scale != 1d)
        {
            if (scale != 0)
            {
                xOff = (int) (xOff * scale);
                yOff = (int) (yOff * scale);
            }

            RenderSystem.pushMatrix();
            RenderSystem.scaled(scale, scale, 0);
        }

        double posX = xOff + bgMargin;
        double posY = yOff + bgMargin;

        posY = getHudPosY((int) posY, yOff, contentHeight, scale, alignment);
        posY += getHudOffsetForPotions(alignment, scale, mc().player);

        for (String line : lines)
        {
            final int width = fontRenderer.getStringWidth(line);

            switch (alignment)
            {
                case TOP_RIGHT:
                case BOTTOM_RIGHT:
                    posX = (scaledWidth / scale) - width - xOff - bgMargin;
                    break;
                case CENTER:
                    posX = (scaledWidth / scale / 2) - (width / 2) - xOff;
                    break;
                default:
            }

            final int x = (int) posX;
            final int y = (int) posY;
            posY += lineHeight;

            if (useBackground)
            {
                drawRect(x - bgMargin, y - bgMargin, width + bgMargin, bgMargin + fontRenderer.fontHeight, bgColor);
            }

            if (useShadow)
            {
                fontRenderer.drawWithShadow(line, x, y, textColor);
            }
            else
            {
                fontRenderer.draw(line, x, y, textColor);
            }
        }

        if (scale != 1d)
        {
            RenderSystem.popMatrix();
        }

        return contentHeight + bgMargin * 2;
    }

    public static int getHudOffsetForPotions(HudAlignment alignment, double scale, PlayerEntity player)
    {
        if (alignment == HudAlignment.TOP_RIGHT)
        {
            // Only Chuck Norris can divide by zero
            if (scale == 0d)
            {
                return 0;
            }

            Collection<StatusEffectInstance> effects = player.getStatusEffects();

            if (effects.isEmpty() == false)
            {
                int y1 = 0;
                int y2 = 0;

                for (StatusEffectInstance effectInstance : effects)
                {
                    StatusEffect effect = effectInstance.getEffectType();

                    if (effectInstance.shouldShowParticles() && effectInstance.shouldShowIcon())
                    {
                        if (effect.isBeneficial())
                        {
                            y1 = 26;
                        }
                        else
                        {
                            y2 = 52;
                            break;
                        }
                    }
                }

                return (int) (Math.max(y1, y2) / scale);
            }
        }

        return 0;
    }

    public static int getHudPosY(int yOrig, int yOffset, int contentHeight, double scale, HudAlignment alignment)
    {
        int scaledHeight = GuiUtils.getScaledWindowHeight();
        int posY = yOrig;

        switch (alignment)
        {
            case BOTTOM_LEFT:
            case BOTTOM_RIGHT:
                posY = (int) ((scaledHeight / scale) - contentHeight - yOffset);
                break;
            case CENTER:
                posY = (int) ((scaledHeight / scale / 2.0d) - (contentHeight / 2.0d) + yOffset);
                break;
            default:
        }

        return posY;
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBlockBoundingBoxSidesBatchedQuads(BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        double minX = pos.getX() - expand;
        double minY = pos.getY() - expand;
        double minZ = pos.getZ() - expand;
        double maxX = pos.getX() + expand + 1;
        double maxY = pos.getY() + expand + 1;
        double maxZ = pos.getZ() + expand + 1;

        drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    /**
     * Assumes a BufferBuilder in GL_LINES mode has been initialized
     */
    public static void drawBlockBoundingBoxOutlinesBatchedLines(BlockPos pos, Color4f color, double expand, BufferBuilder buffer)
    {
        double minX = pos.getX() - expand;
        double minY = pos.getY() - expand;
        double minZ = pos.getZ() - expand;
        double maxX = pos.getX() + expand + 1;
        double maxY = pos.getY() + expand + 1;
        double maxZ = pos.getZ() + expand + 1;

        drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBoxAllSidesBatchedQuads(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            Color4f color, BufferBuilder buffer)
    {
        drawBoxHorizontalSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, buffer);
        drawBoxTopBatchedQuads(minX, minZ, maxX, maxY, maxZ, color, buffer);
        drawBoxBottomBatchedQuads(minX, minY, minZ, maxX, maxZ, color, buffer);
    }

    /**
     * Draws a box with outlines around the given corner positions.
     * Takes in buffers initialized for GL_QUADS and GL_LINES modes.
     * @param posMin
     * @param posMax
     * @param colorLines
     * @param colorSides
     * @param bufferQuads
     * @param bufferLines
     */
    public static void drawBoxWithEdgesBatched(BlockPos posMin, BlockPos posMax, Color4f colorLines, Color4f colorSides, BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        final double x1 = posMin.getX();
        final double y1 = posMin.getY();
        final double z1 = posMin.getZ();
        final double x2 = posMax.getX() + 1;
        final double y2 = posMax.getY() + 1;
        final double z2 = posMax.getZ() + 1;

        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllSidesBatchedQuads(x1, y1, z1, x2, y2, z2, colorSides, bufferQuads);
        fi.dy.masa.malilib.render.RenderUtils.drawBoxAllEdgesBatchedLines(x1, y1, z1, x2, y2, z2, colorLines, bufferLines);
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBoxHorizontalSidesBatchedQuads(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            Color4f color, BufferBuilder buffer)
    {
        // West side
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        // East side
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();

        // North side
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        // South side
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBoxTopBatchedQuads(double minX, double minZ, double maxX, double maxY, double maxZ, Color4f color, BufferBuilder buffer)
    {
        // Top side
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
    }

    /**
     * Assumes a BufferBuilder in GL_QUADS mode has been initialized
     */
    public static void drawBoxBottomBatchedQuads(double minX, double minY, double minZ, double maxX, double maxZ, Color4f color, BufferBuilder buffer)
    {
        // Bottom side
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
    }

    /**
     * Assumes a BufferBuilder in GL_LINES mode has been initialized
     */
    public static void drawBoxAllEdgesBatchedLines(double minX, double minY, double minZ, double maxX, double maxY, double maxZ,
            Color4f color, BufferBuilder buffer)
    {
        // West side
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();

        // East side
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();

        // North side (don't repeat the vertical lines that are done by the east/west sides)
        buffer.vertex(maxX, minY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, minY, minZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(minX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, maxY, minZ).color(color.r, color.g, color.b, color.a).next();

        // South side (don't repeat the vertical lines that are done by the east/west sides)
        buffer.vertex(minX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(maxX, minY, maxZ).color(color.r, color.g, color.b, color.a).next();

        buffer.vertex(maxX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
        buffer.vertex(minX, maxY, maxZ).color(color.r, color.g, color.b, color.a).next();
    }

    public static void drawBox(IntBoundingBox bb, Color4f color, BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX + 1;
        double maxY = bb.maxY + 1;
        double maxZ = bb.maxZ + 1;

        drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, bufferQuads);
        drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, bufferLines);
    }

    public static void drawBox(BlockBox bb, Color4f color, BufferBuilder bufferQuads, BufferBuilder bufferLines)
    {
        double minX = bb.minX;
        double minY = bb.minY;
        double minZ = bb.minZ;
        double maxX = bb.maxX + 1;
        double maxY = bb.maxY + 1;
        double maxZ = bb.maxZ + 1;

        drawBoxAllSidesBatchedQuads(minX, minY, minZ, maxX, maxY, maxZ, color, bufferQuads);
        drawBoxAllEdgesBatchedLines(minX, minY, minZ, maxX, maxY, maxZ, color, bufferLines);
    }

    /**
     * Renders a text plate/billboard, similar to the player name plate.<br>
     * The plate will always face towards the viewer.
     * @param text
     * @param x
     * @param y
     * @param z
     * @param scale
     * @param mc
     */
    public static void drawTextPlate(List<String> text, double x, double y, double z, float scale)
    {
        Entity entity = mc().getCameraEntity();

        if (entity != null)
        {
            drawTextPlate(text, x, y, z, entity.yaw, entity.pitch, scale, 0xFFFFFFFF, 0x40000000, true);
        }
    }

    public static void drawTextPlate(List<String> text, double x, double y, double z, float yaw, float pitch,
            float scale, int textColor, int bgColor, boolean disableDepth)
    {
        TextRenderer textRenderer = mc().textRenderer;

        RenderSystem.alphaFunc(GL11.GL_GREATER, 0.1F);
        RenderSystem.pushMatrix();
        RenderSystem.translatef((float) x, (float) y, (float) z);
        RenderSystem.normal3f(0.0F, 1.0F, 0.0F);

        RenderSystem.rotatef(-yaw, 0.0F, 1.0F, 0.0F);
        RenderSystem.rotatef(pitch, 1.0F, 0.0F, 0.0F);

        RenderSystem.scalef(-scale, -scale, scale);
        RenderSystem.disableLighting();
        RenderSystem.disableCull();

        if (disableDepth)
        {
            RenderSystem.depthMask(false);
            RenderSystem.disableDepthTest();
        }

        setupBlend();
        RenderSystem.disableTexture();

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int maxLineLen = 0;

        for (String line : text)
        {
            maxLineLen = Math.max(maxLineLen, textRenderer.getStringWidth(line));
        }

        int strLenHalf = maxLineLen / 2;
        int textHeight = textRenderer.fontHeight * text.size() - 1;
        int bga = ((bgColor >>> 24) & 0xFF);
        int bgr = ((bgColor >>> 16) & 0xFF);
        int bgg = ((bgColor >>>  8) & 0xFF);
        int bgb = (bgColor          & 0xFF);

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);
        buffer.vertex(-strLenHalf - 1,          -1, 0.0D).color(bgr, bgg, bgb, bga).next();
        buffer.vertex(-strLenHalf - 1,  textHeight, 0.0D).color(bgr, bgg, bgb, bga).next();
        buffer.vertex( strLenHalf    ,  textHeight, 0.0D).color(bgr, bgg, bgb, bga).next();
        buffer.vertex( strLenHalf    ,          -1, 0.0D).color(bgr, bgg, bgb, bga).next();
        tessellator.draw();

        RenderSystem.enableTexture();
        int textY = 0;

        // translate the text a bit infront of the background
        if (disableDepth == false)
        {
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-0.6f, -1.2f);
            //RenderSystem.translate(0, 0, -0.02);
        }

        for (String line : text)
        {
            if (disableDepth)
            {
                RenderSystem.depthMask(false);
                RenderSystem.disableDepthTest();
            }

            textRenderer.draw(line, -strLenHalf, textY, 0x20000000 | (textColor & 0xFFFFFF));

            RenderSystem.enableDepthTest();
            RenderSystem.depthMask(true);

            textRenderer.draw(line, -strLenHalf, textY, textColor);
            textY += textRenderer.fontHeight;
        }

        if (disableDepth == false)
        {
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
        }

        color(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.popMatrix();
    }

    public static void renderBlockTargetingOverlay(Entity entity, BlockPos pos, Direction side, Vec3d hitVec,
            Color4f color, MatrixStack matrixStack, MinecraftClient mc)
    {
        Direction playerFacing = entity.getHorizontalFacing();
        HitPart part = PositionUtils.getHitPart(side, playerFacing, pos, hitVec);
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        double x = pos.getX() + 0.5d - cameraPos.x;
        double y = pos.getY() + 0.5d - cameraPos.y;
        double z = pos.getZ() + 0.5d - cameraPos.z;

        RenderSystem.pushMatrix();

        MatrixStack matrixStackTmp = new MatrixStack();
        blockTargetingOverlayTranslations(x, y, z, side, playerFacing, matrixStackTmp);
        RenderSystem.multMatrix(matrixStackTmp.peek().getModel());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        int quadAlpha = (int) (0.18f * 255f);
        int hr = (int) (color.r * 255f);
        int hg = (int) (color.g * 255f);
        int hb = (int) (color.b * 255f);
        int ha = (int) (color.a * 255f);
        int c = 255;

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        // White full block background
        buffer.vertex(x - 0.5, y - 0.5, z).color(c, c, c, quadAlpha).next();
        buffer.vertex(x + 0.5, y - 0.5, z).color(c, c, c, quadAlpha).next();
        buffer.vertex(x + 0.5, y + 0.5, z).color(c, c, c, quadAlpha).next();
        buffer.vertex(x - 0.5, y + 0.5, z).color(c, c, c, quadAlpha).next();

        switch (part)
        {
            case CENTER:
                buffer.vertex(x - 0.25, y - 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.25, y - 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.25, y + 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x - 0.25, y + 0.25, z).color(hr, hg, hb, ha).next();
                break;
            case LEFT:
                buffer.vertex(x - 0.50, y - 0.50, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x - 0.25, y - 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x - 0.25, y + 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x - 0.50, y + 0.50, z).color(hr, hg, hb, ha).next();
                break;
            case RIGHT:
                buffer.vertex(x + 0.50, y - 0.50, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.25, y - 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.25, y + 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.50, y + 0.50, z).color(hr, hg, hb, ha).next();
                break;
            case TOP:
                buffer.vertex(x - 0.50, y + 0.50, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x - 0.25, y + 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.25, y + 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.50, y + 0.50, z).color(hr, hg, hb, ha).next();
                break;
            case BOTTOM:
                buffer.vertex(x - 0.50, y - 0.50, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x - 0.25, y - 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.25, y - 0.25, z).color(hr, hg, hb, ha).next();
                buffer.vertex(x + 0.50, y - 0.50, z).color(hr, hg, hb, ha).next();
                break;
            default:
        }

        tessellator.draw();

        RenderSystem.lineWidth(1.6f);

        buffer.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);

        // Middle small rectangle
        buffer.vertex(x - 0.25, y - 0.25, z).color(c, c, c, c).next();
        buffer.vertex(x + 0.25, y - 0.25, z).color(c, c, c, c).next();
        buffer.vertex(x + 0.25, y + 0.25, z).color(c, c, c, c).next();
        buffer.vertex(x - 0.25, y + 0.25, z).color(c, c, c, c).next();
        tessellator.draw();

        buffer.begin(GL11.GL_LINES, VertexFormats.POSITION_COLOR);
        // Bottom left
        buffer.vertex(x - 0.50, y - 0.50, z).color(c, c, c, c).next();
        buffer.vertex(x - 0.25, y - 0.25, z).color(c, c, c, c).next();

        // Top left
        buffer.vertex(x - 0.50, y + 0.50, z).color(c, c, c, c).next();
        buffer.vertex(x - 0.25, y + 0.25, z).color(c, c, c, c).next();

        // Bottom right
        buffer.vertex(x + 0.50, y - 0.50, z).color(c, c, c, c).next();
        buffer.vertex(x + 0.25, y - 0.25, z).color(c, c, c, c).next();

        // Top right
        buffer.vertex(x + 0.50, y + 0.50, z).color(c, c, c, c).next();
        buffer.vertex(x + 0.25, y + 0.25, z).color(c, c, c, c).next();
        tessellator.draw();

        RenderSystem.popMatrix();
    }

    public static void renderBlockTargetingOverlaySimple(Entity entity, BlockPos pos, Direction side,
            Color4f color, MatrixStack matrixStack, MinecraftClient mc)
    {
        Direction playerFacing = entity.getHorizontalFacing();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getPos();

        double x = pos.getX() + 0.5d - cameraPos.x;
        double y = pos.getY() + 0.5d - cameraPos.y;
        double z = pos.getZ() + 0.5d - cameraPos.z;

        RenderSystem.pushMatrix();

        MatrixStack matrixStackTmp = new MatrixStack();
        blockTargetingOverlayTranslations(x, y, z, side, playerFacing, matrixStackTmp);
        RenderSystem.multMatrix(matrixStackTmp.peek().getModel());

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();

        int a = (int) (color.a * 255f);
        int r = (int) (color.r * 255f);
        int g = (int) (color.g * 255f);
        int b = (int) (color.b * 255f);
        int c = 255;

        buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR);

        // Simple colored quad
        buffer.vertex(x - 0.5, y - 0.5, z).color(r, g, b, a).next();
        buffer.vertex(x + 0.5, y - 0.5, z).color(r, g, b, a).next();
        buffer.vertex(x + 0.5, y + 0.5, z).color(r, g, b, a).next();
        buffer.vertex(x - 0.5, y + 0.5, z).color(r, g, b, a).next();

        tessellator.draw();

        RenderSystem.lineWidth(1.6f);

        buffer.begin(GL11.GL_LINE_LOOP, VertexFormats.POSITION_COLOR);

        // Middle rectangle
        buffer.vertex(x - 0.375, y - 0.375, z).color(c, c, c, c).next();
        buffer.vertex(x + 0.375, y - 0.375, z).color(c, c, c, c).next();
        buffer.vertex(x + 0.375, y + 0.375, z).color(c, c, c, c).next();
        buffer.vertex(x - 0.375, y + 0.375, z).color(c, c, c, c).next();

        tessellator.draw();

        RenderSystem.popMatrix();
    }

    private static void blockTargetingOverlayTranslations(double x, double y, double z,
            Direction side, Direction playerFacing, MatrixStack matrixStack)
    {
        matrixStack.translate(x, y, z);

        switch (side)
        {
            case DOWN:
                matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180f - playerFacing.asRotation()));
                matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(90f));
                break;
            case UP:
                matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180f - playerFacing.asRotation()));
                matrixStack.multiply(Vector3f.POSITIVE_X.getDegreesQuaternion(-90f));
                break;
            case NORTH:
                matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(180f));
                break;
            case SOUTH:
                break;
            case WEST:
                matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(-90f));
                break;
            case EAST:
                matrixStack.multiply(Vector3f.POSITIVE_Y.getDegreesQuaternion(90f));
                break;
        }

        matrixStack.translate(-x, -y, -z + 0.510);
    }

    public static void renderMapPreview(ItemStack stack, int x, int y, int dimensions)
    {
        if (stack.getItem() instanceof FilledMapItem && GuiBase.isShiftDown())
        {
            RenderSystem.pushMatrix();
            RenderSystem.disableLighting();
            color(1f, 1f, 1f, 1f);

            int y1 = y - dimensions - 20;
            int y2 = y1 + dimensions;
            int x1 = x + 8;
            int x2 = x1 + dimensions;
            int z = 300;

            bindTexture(fi.dy.masa.malilib.render.RenderUtils.TEXTURE_MAP_BACKGROUND);

            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.getBuffer();
            buffer.begin(GL11.GL_QUADS, VertexFormats.POSITION_TEXTURE);
            buffer.vertex(x1, y2, z).texture(0.0f, 1.0f).next();
            buffer.vertex(x2, y2, z).texture(1.0f, 1.0f).next();
            buffer.vertex(x2, y1, z).texture(1.0f, 0.0f).next();
            buffer.vertex(x1, y1, z).texture(0.0f, 0.0f).next();
            tessellator.draw();

            MapState mapdata = FilledMapItem.getMapState(stack, mc().world);

            if (mapdata != null)
            {
                x1 += 8;
                y1 += 8;
                z = 310;
                double scale = (double) (dimensions - 16) / 128.0D;
                RenderSystem.translatef(x1, y1, z);
                RenderSystem.scaled(scale, scale, 0);
                mc().gameRenderer.getMapRenderer().draw(new MatrixStack(), MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), mapdata, false, 0xF000F0);
            }

            RenderSystem.enableLighting();
            RenderSystem.popMatrix();
        }
    }

    public static void renderShulkerBoxPreview(ItemStack stack, int x, int y, boolean useBgColors)
    {
        if (stack.hasTag())
        {
            DefaultedList<ItemStack> items = InventoryUtils.getStoredItems(stack, -1);

            if (items.size() == 0)
            {
                return;
            }

            RenderSystem.pushMatrix();
            disableDiffuseLighting();
            RenderSystem.translatef(0F, 0F, 400F);

            InventoryOverlay.InventoryRenderType type = InventoryOverlay.getInventoryType(stack);
            InventoryOverlay.InventoryProperties props = InventoryOverlay.getInventoryPropsTemp(type, items.size());

            x += 8;
            y -= (props.height + 18);

            if (stack.getItem() instanceof BlockItem && ((BlockItem) stack.getItem()).getBlock() instanceof ShulkerBoxBlock)
            {
                setShulkerboxBackgroundTintColor((ShulkerBoxBlock) ((BlockItem) stack.getItem()).getBlock(), useBgColors);
            }
            else
            {
                color(1f, 1f, 1f, 1f);
            }

            InventoryOverlay.renderInventoryBackground(type, x, y, props.slotsPerRow, items.size(), mc());

            enableDiffuseLightingGui3D();
            RenderSystem.enableDepthTest();
            RenderSystem.enableRescaleNormal();

            Inventory inv = fi.dy.masa.malilib.util.InventoryUtils.getAsInventory(items);
            InventoryOverlay.renderInventoryStacks(type, inv, x + props.slotOffsetX, y + props.slotOffsetY, props.slotsPerRow, 0, -1, mc());

            RenderSystem.disableDepthTest();
            RenderSystem.popMatrix();
        }
    }

    /**
     * Calls RenderUtils.color() with the dye color of the provided shulker box block's color
     * @param block
     * @param useBgColors
     */
    public static void setShulkerboxBackgroundTintColor(@Nullable ShulkerBoxBlock block, boolean useBgColors)
    {
        if (block != null && useBgColors)
        {
            // In 1.13+ there is the uncolored Shulker Box variant, which returns null from getColor()
            final DyeColor dye = block.getColor() != null ? block.getColor() : DyeColor.PURPLE;
            final float[] colors = dye.getColorComponents();
            color(colors[0], colors[1], colors[2], 1f);
        }
        else
        {
            color(1f, 1f, 1f, 1f);
        }
    }

    public static void renderModelInGui(int x, int y, BakedModel model, BlockState state, float zLevel)
    {
        if (state.getBlock() == Blocks.AIR)
        {
            return;
        }

        RenderSystem.pushMatrix();
        bindTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX);
        mc().getTextureManager().getTexture(SpriteAtlasTexture.BLOCK_ATLAS_TEX).setFilter(false, false);

        RenderSystem.enableRescaleNormal();
        RenderSystem.enableAlphaTest();
        RenderSystem.alphaFunc(516, 0.1F);
        RenderSystem.enableBlend();
        setupBlendSimple();
        color(1f, 1f, 1f, 1f);

        setupGuiTransform(x, y, model.hasDepth(), zLevel);
        //model.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GUI);
        RenderSystem.rotatef( 30, 1, 0, 0);
        RenderSystem.rotatef(225, 0, 1, 0);
        RenderSystem.scalef(0.625f, 0.625f, 0.625f);

        renderModel(model, state);

        RenderSystem.disableAlphaTest();
        RenderSystem.disableRescaleNormal();
        RenderSystem.disableLighting();
        RenderSystem.popMatrix();
    }

    public static void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d, float zLevel)
    {
        RenderSystem.translatef(xPosition, yPosition, 100.0F + zLevel);
        RenderSystem.translatef(8.0F, 8.0F, 0.0F);
        RenderSystem.scalef(1.0F, -1.0F, 1.0F);
        RenderSystem.scalef(16.0F, 16.0F, 16.0F);

        if (isGui3d)
        {
            RenderSystem.enableLighting();
        }
        else
        {
            RenderSystem.disableLighting();
        }
    }

    private static void renderModel(BakedModel model, BlockState state)
    {
        RenderSystem.pushMatrix();
        RenderSystem.translatef(-0.5F, -0.5F, -0.5F);
        int color = 0xFFFFFFFF;

        if (model.isBuiltin() == false)
        {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferbuilder = tessellator.getBuffer();
            bufferbuilder.begin(GL11.GL_QUADS, VertexFormats.POSITION_COLOR_TEXTURE_LIGHT_NORMAL);

            for (Direction face : Direction.values())
            {
                RAND.setSeed(0);
                renderQuads(bufferbuilder, model.getQuads(state, face, RAND), state, color);
            }

            RAND.setSeed(0);
            renderQuads(bufferbuilder, model.getQuads(state, null, RAND), state, color);
            tessellator.draw();
        }

        RenderSystem.popMatrix();
    }

    private static void renderQuads(BufferBuilder renderer, List<BakedQuad> quads, BlockState state, int color)
    {
        final int quadCount = quads.size();

        for (int i = 0; i < quadCount; ++i)
        {
            BakedQuad quad = quads.get(i);
            renderQuad(renderer, quad, state, 0xFFFFFFFF);
        }
    }

    private static void renderQuad(BufferBuilder buffer, BakedQuad quad, BlockState state, int color)
    {
        /*
        buffer.putVertexData(quad.getVertexData());
        buffer.setQuadColor(color);

        if (quad.hasColor())
        {
            BlockColors blockColors = mc().getBlockColorMap();
            int m = blockColors.getColorMultiplier(state, null, null, quad.getColorIndex());

            float r = (float) (m >>> 16 & 0xFF) / 255F;
            float g = (float) (m >>>  8 & 0xFF) / 255F;
            float b = (float) (m        & 0xFF) / 255F;
            buffer.multiplyColor(r, g, b, 4);
            buffer.multiplyColor(r, g, b, 3);
            buffer.multiplyColor(r, g, b, 2);
            buffer.multiplyColor(r, g, b, 1);
        }

        putQuadNormal(buffer, quad);
    }

    private static void putQuadNormal(BufferBuilder renderer, BakedQuad quad)
    {
        Vec3i direction = quad.getFace().getVector();
        renderer.normal(direction.getX(), direction.getY(), direction.getZ());
        */
    }

    private static MinecraftClient mc()
    {
        return MinecraftClient.getInstance();
    }

    /*
    public static void enableGUIStandardItemLighting(float scale)
    {
        RenderSystem.pushMatrix();
        RenderSystem.rotate(-30.0F, 0.0F, 1.0F, 0.0F);
        RenderSystem.rotate(165.0F, 1.0F, 0.0F, 0.0F);

        enableStandardItemLighting(scale);

        RenderSystem.popMatrix();
    }

    public static void enableStandardItemLighting(float scale)
    {
        RenderSystem.enableLighting();
        RenderSystem.enableLight(0);
        RenderSystem.enableLight(1);
        RenderSystem.enableColorMaterial();
        RenderUtils.colorMaterial(GL11.GL_FRONT_AND_BACK, GL11.GL_AMBIENT_AND_DIFFUSE);
        RenderSystem.glLight(GL11.GL_LIGHT0, GL11.GL_POSITION, RenderHelper.setColorBuffer((float) LIGHT0_POS.x, (float) LIGHT0_POS.y, (float) LIGHT0_POS.z, 0.0f));

        float lightStrength = 0.3F * scale;
        RenderSystem.glLight(GL11.GL_LIGHT0, GL11.GL_DIFFUSE, RenderHelper.setColorBuffer(lightStrength, lightStrength, lightStrength, 1.0F));
        RenderSystem.glLight(GL11.GL_LIGHT0, GL11.GL_AMBIENT, RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.glLight(GL11.GL_LIGHT0, GL11.GL_SPECULAR, RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.glLight(GL11.GL_LIGHT1, GL11.GL_POSITION, RenderHelper.setColorBuffer((float) LIGHT1_POS.x, (float) LIGHT1_POS.y, (float) LIGHT1_POS.z, 0.0f));
        RenderSystem.glLight(GL11.GL_LIGHT1, GL11.GL_DIFFUSE, RenderHelper.setColorBuffer(lightStrength, lightStrength, lightStrength, 1.0F));
        RenderSystem.glLight(GL11.GL_LIGHT1, GL11.GL_AMBIENT, RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));
        RenderSystem.glLight(GL11.GL_LIGHT1, GL11.GL_SPECULAR, RenderHelper.setColorBuffer(0.0F, 0.0F, 0.0F, 1.0F));

        RenderSystem.shadeModel(GL11.GL_FLAT);

        float ambientLightStrength = 0.4F;
        RenderSystem.glLightModel(GL11.GL_LIGHT_MODEL_AMBIENT, RenderHelper.setColorBuffer(ambientLightStrength, ambientLightStrength, ambientLightStrength, 1.0F));
    }
    */
}
