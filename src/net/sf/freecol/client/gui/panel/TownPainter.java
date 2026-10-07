/**
 *  Copyright (C) 2002-2024   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client.gui.panel;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.TexturePaint;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;
import java.util.function.Function;

import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ProductionType;


/**
 * Paints the ground of a town laid out by a {@link TownPlan}: the land
 * the colony stands on, worn by the townsfolk, the fenced yards of the
 * buildings, its dirt streets, the cobbled town square with a well in
 * the middle, and a name plate for each building.
 */
public final class TownPainter {

    /** The colour of the dirt of the streets. */
    private static final Color DIRT = new Color(150, 116, 74);

    /** The colour of the edge of the streets. */
    private static final Color DIRT_EDGE = new Color(96, 70, 40);

    /** A cached dirt texture, and the street width it was made for. */
    private static BufferedImage dirtTexture = null;
    private static int dirtTextureRoad = -1;

    /** Land images made seamless. */
    private static final Map<BufferedImage, BufferedImage> seamless
        = new WeakHashMap<>();

    /** Silhouettes of building pictures, for their shadows. */
    private static final Map<BufferedImage, BufferedImage> silhouettes
        = new WeakHashMap<>();


    /**
     * The view of the town, in perspective: lines running away from
     * the viewer meet at a vanishing point high above the town.
     */
    private static final class View {

        /** The vanishing point. */
        final float cx, vy;

        /** The bottom of the town, where things have their full size. */
        final float bottom;

        View(int width, int height) {
            this.cx = width / 2f;
            this.vy = -1.8f * height;
            this.bottom = height;
        }

        /**
         * Follow a line running away from the viewer.
         *
         * @param x The x coordinate where the line starts.
         * @param y0 The y coordinate where the line starts.
         * @param y The y coordinate to follow the line to.
         * @return The x coordinate of the line at {@code y}.
         */
        float x(float x, float y0, float y) {
            return cx + (x - cx) * (y - vy) / (y0 - vy);
        }

        /**
         * Get how large things are at a given depth.
         *
         * @param y The y coordinate.
         * @return The scale at {@code y}, one at the bottom of the town.
         */
        float scale(float y) {
            return (y - vy) / (bottom - vy);
        }

        /**
         * Get a piece of ground, square in the town, as seen in the
         * view.
         *
         * @param x0 The left of the near edge.
         * @param x1 The right of the near edge.
         * @param top The y coordinate of the far edge.
         * @param base The y coordinate of the near edge.
         * @return The piece of ground.
         */
        Path2D quad(float x0, float x1, float top, float base) {
            final Path2D p = new Path2D.Float();
            p.moveTo(x(x0, base, top), top);
            p.lineTo(x(x1, base, top), top);
            p.lineTo(x1, base);
            p.lineTo(x0, base);
            p.closePath();
            return p;
        }
    }


    private TownPainter() {} // Static only

    /**
     * Paint the ground of a town.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the town.
     * @param height The height of the town.
     * @param plan The {@code TownPlan} to paint.
     * @param land A terrain image (an isometric tile) of the land the
     *     town stands on, or null to use a plain colour.
     * @param worn A terrain image of worn, patchy grass to blend into
     *     the land, or null for none.
     * @param trees A picture of a stand of trees for the groves, or
     *     null for none.
     * @param pictures Gets the picture standing on a site, to cast its
     *     shadow, or null for no shadows.
     */
    public static void paint(Graphics2D g, int width, int height,
                             TownPlan plan, BufferedImage land,
                             BufferedImage worn, BufferedImage trees,
                             Function<TownPlan.Plot, BufferedImage> pictures) {
        final int road = plan.getRoad();
        final View view = new View(width, height);
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                                 RenderingHints.VALUE_RENDER_QUALITY);
            paintLand(g2d, width, height, land, worn);
            paintWear(g2d, width, height, road);
            paintFields(g2d, view, width, plan, road, trees);
            paintYards(g2d, view, plan, road);
            if (trees != null) paintLaneTrees(g2d, plan, road, trees);
            paintStreets(g2d, view, plan, road);
            if (plan.getSquare() != null) {
                paintSquare(g2d, view, plan.getSquare(), road);
            }
            paintShadows(g2d, plan, pictures);
            paintDepth(g2d, width, height);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Cover the town with the land it stands on.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the town.
     * @param height The height of the town.
     * @param land The terrain image, or null.
     * @param worn The worn grass image, or null.
     */
    private static void paintLand(Graphics2D g, int width, int height,
                                  BufferedImage land, BufferedImage worn) {
        g.setColor(new Color(110, 116, 60));
        g.fillRect(0, 0, width, height);
        if (land != null) layLand(g, width, height, land, 1f);
        // The grass of a town is trodden down and patchy.
        if (worn != null) layLand(g, width, height, worn, 0.55f);
        // Tone the land down a little so the buildings stand out.
        g.setColor(new Color(40, 30, 10, 50));
        g.fillRect(0, 0, width, height);
    }

    /**
     * Cover the town with a land texture.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the town.
     * @param height The height of the town.
     * @param land The terrain image.
     * @param alpha How opaque the land is.
     */
    private static void layLand(Graphics2D g, int width, int height,
                                BufferedImage land, float alpha) {
        final BufferedImage tile = getSeamless(land);
        final int tw = tile.getWidth(), th = tile.getHeight();
        final Composite oldComposite = g.getComposite();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  alpha));
        g.setPaint(new TexturePaint(tile, new Rectangle(0, 0, tw, th)));
        g.fillRect(0, 0, width, height);
        // Lay the land again, shifted and faded, to hide the repeats.
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
                                                  alpha * 0.45f));
        g.setPaint(new TexturePaint(tile, new Rectangle(tw / 3, th * 2 / 5,
                                                        tw, th)));
        g.fillRect(0, 0, width, height);
        g.setComposite(oldComposite);
    }

    /**
     * Get a seamless texture made from a land image, reusing one made
     * before if possible.
     *
     * @param land The terrain image.
     * @return A seamless texture.
     */
    private static synchronized BufferedImage getSeamless(BufferedImage land) {
        return seamless.computeIfAbsent(land, TownPainter::makeSeamless);
    }

    /**
     * Scatter patches of bare earth over the town, where people walk.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the town.
     * @param height The height of the town.
     * @param road The width of a street.
     */
    private static void paintWear(Graphics2D g, int width, int height,
                                  int road) {
        final Random random = new Random(1607);
        final int count = width * height / (road * road * 10);
        final Color earth = new Color(120, 92, 56, 90);
        final Color clear = new Color(120, 92, 56, 0);
        for (int i = 0; i < count; i++) {
            final float r = road * (0.8f + random.nextFloat() * 1.8f);
            final float cx = random.nextFloat() * width;
            final float cy = random.nextFloat() * height;
            final Graphics2D g2d = (Graphics2D)g.create();
            try {
                // Flattened, as the land is seen from an angle
                g2d.translate(cx, cy);
                g2d.scale(1.0, 0.5);
                g2d.setPaint(new RadialGradientPaint(0f, 0f, r,
                        new float[] { 0f, 1f }, new Color[] { earth, clear }));
                g2d.fill(new Ellipse2D.Float(-r, -r, 2 * r, 2 * r));
            } finally {
                g2d.dispose();
            }
        }
    }

    /**
     * Paint the yards of the buildings: packed earth behind a wooden
     * fence for each building, and stakes marking out the empty sites.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param view The {@code View} of the town.
     * @param plan The {@code TownPlan} with the buildings.
     * @param road The width of a street.
     */
    private static void paintYards(Graphics2D g, View view, TownPlan plan,
                                   int road) {
        final BufferedImage dirt = getDirtTexture(road);
        for (TownPlan.Plot p : plan.getPlots()) {
            if (p.bounds == null) continue;
            final float base = p.bounds.y + p.bounds.height;
            final float h = p.bounds.height - road * 0.2f;
            final float x = p.bounds.x - road * 0.25f;
            final float w = p.bounds.width + road * 0.5f;
            final float y = base - h;
            if (p.empty) {
                paintStakes(g, view, x + road * 0.3f, x + w - road * 0.3f,
                            y + road * 0.3f, base - road * 0.1f, road);
                continue;
            }
            final Shape yard = view.quad(x, x + w, y, base + road * 0.3f);
            g.setStroke(new BasicStroke(road * 0.3f, BasicStroke.CAP_ROUND,
                                        BasicStroke.JOIN_ROUND));
            g.setColor(new Color(100, 76, 44, 45));
            g.draw(yard);
            // Trodden earth, barest in front by the street
            final Composite oldComposite = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.45f));
            g.setPaint(new TexturePaint(dirt, new Rectangle(0, 0,
                        dirt.getWidth(), dirt.getHeight())));
            g.fill(yard);
            g.setComposite(AlphaComposite.getInstance(
                    AlphaComposite.SRC_OVER, 0.5f));
            g.fill(view.quad(x, x + w, base - road * 1.6f,
                             base + road * 0.3f));
            g.setComposite(oldComposite);
            // Fence along the back and the sides
            final float inset = road * 0.2f;
            final float fy = y + road * 0.35f;
            final float fb = base - road * 0.2f;
            final float l = x + inset, r = x + w - inset;
            paintFence(g, view.x(l, fb, fy), fy, view.x(r, fb, fy), fy,
                       road);
            paintFence(g, view.x(l, fb, fy), fy, l, fb, road);
            paintFence(g, view.x(r, fb, fy), fy, r, fb, road);
        }
    }

    /**
     * Fill the open ground in the rows of the town with gardens and
     * small fields, as on the edge of a village.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param view The {@code View} of the town.
     * @param width The width of the town.
     * @param plan The {@code TownPlan} with the buildings.
     * @param road The width of a street.
     * @param trees A picture of a stand of trees, or null.
     */
    private static void paintFields(Graphics2D g, View view, int width,
                                    TownPlan plan, int road,
                                    BufferedImage trees) {
        final List<Rectangle> obstacles = new ArrayList<>();
        for (TownPlan.Plot p : plan.getPlots()) {
            if (p.bounds != null) obstacles.add(p.bounds);
        }
        obstacles.addAll(plan.getAvenue());
        if (plan.getSquare() != null) obstacles.add(plan.getSquare());
        if (plan.getReserved() != null) obstacles.add(plan.getReserved());

        final int margin = road / 2;
        int top = 0, kind = 0;
        for (Rectangle street : plan.getStreets()) {
            final int bottom = street.y;
            final int fieldTop = top + road / 2;
            final int fieldBottom = bottom - road / 3;
            if (fieldBottom - fieldTop >= 2 * road) {
                // Find the stretches of the row free of anything else
                final List<int[]> taken = new ArrayList<>();
                for (Rectangle o : obstacles) {
                    if (o.y < fieldBottom && o.y + o.height > fieldTop) {
                        taken.add(new int[] { o.x - margin,
                                              o.x + o.width + margin });
                    }
                }
                taken.sort((a, b) -> Integer.compare(a[0], b[0]));
                int x = margin;
                for (int[] t : taken) {
                    if (t[0] - x >= 3 * road) {
                        paintField(g, view, new Rectangle(x, fieldTop,
                            t[0] - x, fieldBottom - fieldTop), road,
                            kind++, trees);
                    }
                    x = Math.max(x, t[1]);
                }
                if (width - margin - x >= 3 * road) {
                    paintField(g, view, new Rectangle(x, fieldTop,
                        width - margin - x, fieldBottom - fieldTop),
                        road, kind++, trees);
                }
            }
            top = street.y + street.height;
        }
    }

    /**
     * Paint a small fenced field or garden, or a grove of trees.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param view The {@code View} of the town.
     * @param r Where the field lies.
     * @param road The width of a street, to size the furrows.
     * @param kind Which kind of field to paint.
     * @param trees A picture of a stand of trees, or null.
     */
    private static void paintField(Graphics2D g, View view, Rectangle r,
                                   int road, int kind,
                                   BufferedImage trees) {
        if (trees != null && kind % 3 == 1) {
            paintGrove(g, r, trees);
            return;
        }
        final Color[] crops = {
            new Color(86, 132, 44),   // vegetables
            new Color(196, 170, 70),  // wheat
            new Color(52, 104, 40),   // tobacco
        };
        final Color crop = crops[(kind / 2) % crops.length];
        final Random random = new Random(r.x * 31 + r.y);
        final float base = r.y + r.height;
        final Shape field = view.quad(r.x, r.x + r.width, r.y, base);
        // Tilled earth
        g.setColor(new Color(112, 82, 50));
        g.fill(field);
        final Shape oldClip = g.getClip();
        g.clip(field);
        // Furrows across the field, with the crop growing along them
        final float row = Math.max(4f, road * 0.3f);
        final float dot = Math.max(2f, road * 0.16f);
        for (float y = r.y + row / 2; y < r.y + r.height; y += row) {
            g.setColor(new Color(80, 56, 32));
            g.fill(new Rectangle.Float(r.x, y + row * 0.25f, r.width,
                                       row * 0.25f));
            g.setColor(new Color(150, 116, 76));
            g.fill(new Rectangle.Float(r.x, y + row * 0.05f, r.width,
                                       Math.max(1f, row * 0.12f)));
            for (float x = r.x + random.nextFloat() * dot; x < r.x + r.width;
                 x += dot * (1.1f + random.nextFloat() * 0.6f)) {
                final int shade = random.nextInt(30) - 15;
                g.setColor(new Color(clamp(crop.getRed() + shade),
                                     clamp(crop.getGreen() + shade),
                                     clamp(crop.getBlue() + shade / 2)));
                g.fill(new Ellipse2D.Float(x, y - dot * 0.55f, dot,
                                           dot * 0.9f));
            }
        }
        // Lit from above, shaded towards the front
        g.setPaint(new GradientPaint(0, r.y, new Color(255, 240, 200, 30),
                0, r.y + r.height, new Color(30, 20, 10, 50)));
        g.fill(field);
        g.setClip(oldClip);
        // A low fence around it
        final float inset = road * 0.1f;
        final float l = r.x + inset, rr = r.x + r.width - inset;
        final float ft = r.y + inset, fb = base - inset;
        paintFence(g, view.x(l, fb, ft), ft, view.x(rr, fb, ft), ft, road);
        paintFence(g, view.x(l, fb, ft), ft, l, fb, road);
        paintFence(g, view.x(rr, fb, ft), ft, rr, fb, road);
        paintFence(g, l, fb, rr, fb, road);
    }

    /**
     * Plant a few trees in the lanes between the yards.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param plan The {@code TownPlan} with the buildings.
     * @param road The width of a street.
     * @param trees A picture of a stand of trees.
     */
    private static void paintLaneTrees(Graphics2D g, TownPlan plan,
                                       int road, BufferedImage trees) {
        final List<TownPlan.Plot> placed = new ArrayList<>();
        for (TownPlan.Plot p : plan.getPlots()) {
            if (p.bounds != null) placed.add(p);
        }
        placed.sort((a, b) -> Integer.compare(a.bounds.x, b.bounds.x));
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            for (TownPlan.Plot p : placed) {
                final int base = p.bounds.y + p.bounds.height;
                // The next site to the right in the same row
                TownPlan.Plot next = null;
                for (TownPlan.Plot o : placed) {
                    if (o.bounds.x > p.bounds.x
                        && o.bounds.y + o.bounds.height == base) {
                        next = o;
                        break;
                    }
                }
                if (next == null) continue;
                final int left = p.bounds.x + p.bounds.width + road / 4;
                final int gap = next.bounds.x - road / 4 - left;
                if (gap < road * 0.6f) continue;
                final int tw = Math.min(Math.round(gap * 1.5f), road * 3);
                final int th = tw * trees.getHeight() / trees.getWidth();
                final int depth = Math.min(p.bounds.height,
                                           next.bounds.height);
                final int y = base - Math.round(depth * 0.55f) - th / 2;
                g2d.drawImage(trees, left + gap / 2 - tw / 2, y, tw, th,
                              null);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint a straight wooden fence, either across or along the view.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param x0 The x coordinate of the start of the fence.
     * @param y0 The y coordinate of the start of the fence (at the
     *     foot of the posts).
     * @param x1 The x coordinate of the end of the fence.
     * @param y1 The y coordinate of the end of the fence.
     * @param road The width of a street, to size the fence.
     */
    private static void paintFence(Graphics2D g, float x0, float y0,
                                   float x1, float y1, int road) {
        final float postH = road * 0.42f;
        final float postW = Math.max(1.5f, road * 0.08f);
        final float rail = Math.max(1f, road * 0.05f);
        final boolean across = Math.abs(x1 - x0) >= Math.abs(y1 - y0);
        g.setStroke(new BasicStroke(rail, BasicStroke.CAP_BUTT,
                                    BasicStroke.JOIN_MITER));
        g.setColor(new Color(150, 110, 64));
        for (float f : (across) ? new float[] { 0.75f, 0.35f }
                                : new float[] { 0.75f }) {
            g.draw(new Line2D.Float(x0, y0 - postH * f, x1, y1 - postH * f));
        }
        final float len = (float)Math.hypot(x1 - x0, y1 - y0);
        final float step = (across) ? road * 0.55f : road * 0.4f;
        final int posts = Math.max(1, Math.round(len / step));
        for (int i = 0; i <= posts; i++) {
            final float px = x0 + (x1 - x0) * i / posts;
            final float py = y0 + (y1 - y0) * i / posts;
            g.setColor(new Color(104, 72, 40));
            g.fill(new Rectangle.Float(px - postW / 2, py - postH,
                                       postW, postH));
            g.setColor(new Color(170, 130, 80));
            g.fill(new Rectangle.Float(px - postW / 2, py - postH,
                                       Math.max(1f, postW * 0.4f), postH));
            g.setColor(new Color(60, 40, 20, 90));
            g.fill(new Ellipse2D.Float(px - postW, py - postW * 0.4f,
                                       postW * 2, postW * 0.8f));
        }
    }

    /**
     * Mark out an empty building site with stakes and a rope.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param view The {@code View} of the town.
     * @param x0 The left of the near edge of the site.
     * @param x1 The right of the near edge of the site.
     * @param top The y coordinate of the far edge of the site.
     * @param base The y coordinate of the near edge of the site.
     * @param road The width of a street, to size the stakes.
     */
    private static void paintStakes(Graphics2D g, View view, float x0,
                                    float x1, float top, float base,
                                    int road) {
        final float stakeH = road * 0.35f;
        final float stakeW = Math.max(1.5f, road * 0.07f);
        final float dash = Math.max(2f, road * 0.18f);
        g.setStroke(new BasicStroke(Math.max(1f, road * 0.04f),
                BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
                new float[] { dash, dash * 0.6f }, 0f));
        g.setColor(new Color(225, 205, 160, 200));
        final Path2D rope = view.quad(x0, x1, top, base);
        rope.transform(AffineTransform.getTranslateInstance(0,
                                                            -stakeH * 0.6f));
        g.draw(rope);
        g.setColor(new Color(150, 110, 64));
        final float tl = view.x(x0, base, top), tr = view.x(x1, base, top);
        for (float[] c : new float[][] { { tl, top }, { tr, top },
                                         { x0, base }, { x1, base } }) {
            g.fill(new Rectangle.Float(c[0] - stakeW / 2, c[1] - stakeH,
                                       stakeW, stakeH));
        }
    }

    /**
     * Turn an isometric tile image into a rectangular image that
     * tiles seamlessly.
     *
     * @param diamond The isometric tile image.
     * @return A seamless texture of the same size.
     */
    static BufferedImage makeSeamless(BufferedImage diamond) {
        final int w = diamond.getWidth(), h = diamond.getHeight();
        final BufferedImage tile
            = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = tile.createGraphics();
        try {
            g.drawImage(diamond, 0, 0, null);
            g.drawImage(diamond, -w / 2, -h / 2, null);
            g.drawImage(diamond, w / 2, -h / 2, null);
            g.drawImage(diamond, -w / 2, h / 2, null);
            g.drawImage(diamond, w / 2, h / 2, null);
        } finally {
            g.dispose();
        }
        return tile;
    }

    /**
     * Paint the dirt streets and the avenue.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param view The {@code View} of the town.
     * @param plan The {@code TownPlan} with the streets.
     * @param road The width of a street.
     */
    private static void paintStreets(Graphics2D g, View view, TownPlan plan,
                                     int road) {
        final Area streets = new Area();
        final float arc = road * 0.8f;
        for (Rectangle r : plan.getStreets()) {
            streets.add(new Area(new RoundRectangle2D.Float(
                        r.x - road, r.y, r.width + 2 * road, r.height,
                        arc, arc)));
        }
        // The avenue narrows as it runs away from the viewer.
        for (Rectangle r : plan.getAvenue()) {
            streets.add(new Area(avenue(view, r, road, 0f, 1f)));
        }

        paintDirt(g, streets, road);

        // Cart ruts along each street.
        final Shape oldClip = g.getClip();
        g.clip(streets);
        g.setStroke(new BasicStroke(Math.max(1f, road / 10f),
                BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(new Color(80, 58, 32, 70));
        final Random random = new Random(road);
        for (Rectangle r : plan.getStreets()) {
            for (float f : new float[] { 0.32f, 0.68f }) {
                g.draw(wobblyLine(r.x - road, r.y + r.height * f,
                                  r.x + r.width + road, r.y + r.height * f,
                                  road * 0.08f, road * 2, random));
            }
        }
        for (Rectangle r : plan.getAvenue()) {
            for (float f : new float[] { 0.32f, 0.68f }) {
                g.draw(avenue(view, r, road, f, f));
            }
        }
        // The streets are worn down below the land: shade their far
        // edge, and catch the light on their near edge.
        final float edge = road * 0.4f;
        for (Rectangle r : plan.getStreets()) {
            g.setPaint(new GradientPaint(0, r.y, new Color(40, 25, 10, 110),
                    0, r.y + edge, new Color(40, 25, 10, 0)));
            g.fill(new Rectangle.Float(r.x - road, r.y, r.width + 2 * road,
                                       edge));
            g.setColor(new Color(255, 235, 190, 50));
            g.fill(new Rectangle.Float(r.x - road, r.y + r.height - 2,
                                       r.width + 2 * road, 2));
        }
        for (Rectangle r : plan.getAvenue()) {
            g.setPaint(new GradientPaint(r.x, 0, new Color(40, 25, 10, 90),
                    r.x + edge, 0, new Color(40, 25, 10, 0)));
            g.fill(avenue(view, r, road, 0f, 0.4f));
        }
        g.setClip(oldClip);
    }

    /**
     * Get a part of the avenue, or a strip along it, narrowing as it
     * runs away from the viewer.
     *
     * @param view The {@code View} of the town.
     * @param r The part of the avenue.
     * @param road The width of a street.
     * @param from Where the strip starts across the avenue, from zero
     *     at its left edge to one at its right.
     * @param to Where the strip ends across the avenue.
     * @return The strip, or a line if {@code from} equals {@code to}.
     */
    private static Path2D avenue(View view, Rectangle r, int road,
                                 float from, float to) {
        final float top = r.y - road, bottom = r.y + r.height + road;
        final float cx = r.x + r.width / 2f;
        final float wt = r.width * view.scale(top);
        final float wb = r.width * view.scale(bottom);
        final Path2D p = new Path2D.Float();
        p.moveTo(cx + wt * (from - 0.5f), top);
        if (from == to) {
            p.lineTo(cx + wb * (from - 0.5f), bottom);
            return p;
        }
        p.lineTo(cx + wt * (to - 0.5f), top);
        p.lineTo(cx + wb * (to - 0.5f), bottom);
        p.lineTo(cx + wb * (from - 0.5f), bottom);
        p.closePath();
        return p;
    }

    /**
     * Fill an area with dirt, with a soft, worn edge where the grass
     * meets the dirt.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param area The area to fill.
     * @param road The width of a street.
     */
    private static void paintDirt(Graphics2D g, Shape area, int road) {
        for (int i = 3; i >= 1; i--) {
            g.setStroke(new BasicStroke(road * 0.18f * i,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(DIRT_EDGE.getRed(), DIRT_EDGE.getGreen(),
                                 DIRT_EDGE.getBlue(), 40));
            g.draw(area);
        }
        g.setPaint(new TexturePaint(getDirtTexture(road),
                new Rectangle(0, 0, 4 * road, 4 * road)));
        g.fill(area);
    }

    /**
     * Paint the land just outside the town, where the colonists who
     * are not working wait: the same land as the town, with the avenue
     * running on down from the town to a road along the bottom.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the land.
     * @param height The height of the land.
     * @param land The terrain image, or null.
     * @param worn The worn grass image, or null.
     * @param road The width of a street.
     * @param avenueX The x coordinate of the middle of the avenue.
     */
    public static void paintOutskirts(Graphics2D g, int width, int height,
                                      BufferedImage land, BufferedImage worn,
                                      int road, int avenueX) {
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            paintLand(g2d, width, height, land, worn);
            paintWear(g2d, width, height, road);
            // The colonists stand on the road, so it runs along the
            // bottom, where their feet are.
            final float roadY = height - road * 1.6f;
            final float arc = road * 0.8f;
            final Area roads = new Area(new RoundRectangle2D.Float(-road,
                    roadY, width + 2 * road, road, arc, arc));
            roads.add(new Area(new RoundRectangle2D.Float(avenueX - road / 2f,
                    -road, road, roadY + 2 * road, arc, arc)));
            paintDirt(g2d, roads, road);
            // Worn below the land: shade the far edge
            g2d.setPaint(new GradientPaint(0, roadY,
                    new Color(40, 25, 10, 110), 0, roadY + road * 0.4f,
                    new Color(40, 25, 10, 0)));
            g2d.fill(new Rectangle.Float(0, roadY, width, road * 0.4f));
            // A little shade from the town above, and darker edges
            g2d.setPaint(new GradientPaint(0, 0, new Color(20, 12, 0, 70),
                    0, road * 0.8f, new Color(20, 12, 0, 0)));
            g2d.fillRect(0, 0, width, Math.round(road * 0.8f));
            paintVignette(g2d, width, height);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Make a slightly wobbly horizontal line.
     *
     * @return The line.
     */
    private static Shape wobblyLine(float x0, float y0, float x1, float y1,
                                    float wobble, float step, Random random) {
        final Path2D p = new Path2D.Float();
        p.moveTo(x0, y0);
        for (float x = x0 + step; x < x1; x += step) {
            final float t = (x - x0) / (x1 - x0);
            p.lineTo(x, y0 + (y1 - y0) * t
                + (random.nextFloat() - 0.5f) * 2 * wobble);
        }
        p.lineTo(x1, y1);
        return p;
    }

    /**
     * Get a dirt texture to fill the streets with.
     *
     * @param road The width of a street.
     * @return The texture.
     */
    private static synchronized BufferedImage getDirtTexture(int road) {
        if (dirtTexture != null && dirtTextureRoad == road) {
            return dirtTexture;
        }
        final int size = Math.max(16, 4 * road);
        final BufferedImage img
            = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                               RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(DIRT);
            g.fillRect(0, 0, size, size);
            final Random random = new Random(1492);
            final int specks = size * size / 6;
            for (int i = 0; i < specks; i++) {
                final int shade = random.nextInt(50) - 25;
                final int alpha = 40 + random.nextInt(80);
                g.setColor(new Color(clamp(DIRT.getRed() + shade),
                                     clamp(DIRT.getGreen() + shade),
                                     clamp(DIRT.getBlue() + shade * 3 / 4),
                                     alpha));
                final float s = 1f + random.nextFloat() * road / 8f;
                final float x = random.nextFloat() * size;
                final float y = random.nextFloat() * size;
                // Draw wrapped round so the texture tiles seamlessly.
                for (int dx = -size; dx <= size; dx += size) {
                    for (int dy = -size; dy <= size; dy += size) {
                        g.fill(new Ellipse2D.Float(x + dx, y + dy,
                                                   s, s * 0.7f));
                    }
                }
            }
            // A few pebbles.
            for (int i = 0; i < size / 3; i++) {
                final float x = random.nextFloat() * size;
                final float y = random.nextFloat() * size;
                final float s = 1.5f + random.nextFloat() * road / 7f;
                g.setColor(new Color(190, 170, 140, 160));
                g.fill(new Ellipse2D.Float(x, y, s, s * 0.7f));
                g.setColor(new Color(70, 50, 30, 120));
                g.fill(new Ellipse2D.Float(x + s * 0.2f, y + s * 0.5f,
                                           s, s * 0.4f));
            }
        } finally {
            g.dispose();
        }
        dirtTexture = img;
        dirtTextureRoad = road;
        return img;
    }

    private static int clamp(int c) {
        return Math.max(0, Math.min(255, c));
    }

    /**
     * Paint the cobbled town square, with a well in the middle.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param view The {@code View} of the town.
     * @param square The square.
     * @param road The width of a street.
     */
    private static void paintSquare(Graphics2D g, View view,
                                    Rectangle square, int road) {
        final float arc = road * 0.8f;
        final float x0 = square.x, x1 = square.x + square.width;
        final float bottom = square.y + square.height;
        // The square is paved a step above the streets: its shadow and
        // its stone front come first.
        final float step = Math.max(3f, road * 0.28f);
        g.setColor(new Color(0, 0, 0, 70));
        g.fill(view.quad(x0 + step * 0.8f, x1 + step * 0.8f,
                         square.y + step * 0.8f, bottom + step * 0.8f));
        g.setColor(new Color(92, 76, 58));
        g.fill(view.quad(x0, x1, square.y, bottom));
        g.setColor(new Color(60, 48, 36, 140));
        g.setStroke(new BasicStroke(1f));
        for (float x = square.x + step * 2; x < square.x + square.width;
             x += step * 2.2f) {
            g.draw(new Line2D.Float(x, square.y + square.height - step,
                                    x, square.y + square.height));
        }
        final Shape shape = view.quad(x0, x1, square.y - step,
                                      bottom - step);
        g.setColor(new Color(120, 102, 78));
        g.fill(shape);

        // The cobbles.
        final Shape oldClip = g.getClip();
        g.clip(shape);
        final Random random = new Random(square.width * 31 + square.height);
        final int stone = Math.max(6, road / 3);
        final int rowH = stone * 3 / 4;
        for (int y = square.y - Math.round(step), row = 0;
             y < square.y + square.height;
             y += rowH, row++) {
            final int offset = (row % 2 == 0) ? 0 : -stone / 2;
            for (int x = square.x + offset; x < square.x + square.width;
                 x += stone) {
                final int shade = 170 + random.nextInt(40);
                g.setColor(new Color(shade, shade - 14, shade - 40));
                final float jx = random.nextFloat() * 1.5f;
                final float jy = random.nextFloat() * 1.5f;
                g.fill(new RoundRectangle2D.Float(x + 1 + jx, y + 1 + jy,
                        stone - 2.5f, rowH - 2.5f, stone / 2f, rowH / 2f));
            }
        }
        // A little shade towards the bottom, the square is lit from above.
        g.setPaint(new GradientPaint(0, square.y - step,
                new Color(255, 245, 220, 40),
                0, square.y + square.height, new Color(40, 25, 10, 60)));
        g.fill(shape);
        g.setClip(oldClip);

        // A curb of larger stones around the edge, catching the light
        // along the top.
        g.setStroke(new BasicStroke(Math.max(2f, road / 7f)));
        g.setColor(new Color(100, 80, 56));
        g.draw(shape);
        g.setStroke(new BasicStroke(Math.max(1f, road / 16f)));
        g.setColor(new Color(255, 240, 210, 90));
        final float ty = square.y - step + 1;
        g.draw(new Line2D.Float(view.x(x0, bottom, ty) + arc / 2, ty,
            view.x(x1, bottom, ty) - arc / 2, ty));

        paintWell(g, square.x + square.width / 2f,
                  square.y - step + square.height / 2f, road);
    }

    /**
     * Paint a stone well with a little roof.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param cx The x coordinate of the center of the well.
     * @param cy The y coordinate of the center of the well.
     * @param road The width of a street, to size the well.
     */
    private static void paintWell(Graphics2D g, float cx, float cy,
                                  int road) {
        final float w = road * 1.6f, h = w * 0.5f;
        // Shadow
        g.setColor(new Color(0, 0, 0, 60));
        g.fill(new Ellipse2D.Float(cx - w * 0.55f, cy - h * 0.1f,
                                   w * 1.25f, h * 1.1f));
        // The stone wall of the well
        final float wallH = h * 0.7f;
        g.setPaint(new GradientPaint(cx - w / 2, 0, new Color(150, 140, 125),
                                     cx + w / 2, 0, new Color(95, 88, 78)));
        g.fill(new Rectangle.Float(cx - w / 2, cy - wallH, w, wallH));
        g.fill(new Ellipse2D.Float(cx - w / 2, cy - h / 2, w, h));
        // Courses of stone
        g.setStroke(new BasicStroke(Math.max(1f, road / 20f)));
        g.setColor(new Color(70, 64, 56, 150));
        for (int i = 1; i <= 2; i++) {
            final float y = cy - wallH + wallH * i / 3f;
            g.draw(new java.awt.geom.Arc2D.Float(cx - w / 2, y - h / 2,
                    w, h, 180, 180, java.awt.geom.Arc2D.OPEN));
        }
        // The top of the wall and the water
        final float topY = cy - wallH;
        g.setColor(new Color(175, 165, 150));
        g.fill(new Ellipse2D.Float(cx - w / 2, topY - h / 2, w, h));
        g.setColor(new Color(30, 50, 70));
        g.fill(new Ellipse2D.Float(cx - w * 0.36f, topY - h * 0.34f,
                                   w * 0.72f, h * 0.68f));
        // Posts and roof
        final float postH = h * 1.9f;
        final float post = Math.max(2f, road / 9f);
        g.setColor(new Color(92, 62, 34));
        g.fill(new Rectangle.Float(cx - w * 0.42f, topY - postH, post, postH));
        g.fill(new Rectangle.Float(cx + w * 0.42f - post, topY - postH,
                                   post, postH));
        g.setStroke(new BasicStroke(Math.max(1f, road / 16f)));
        g.drawLine(Math.round(cx - w * 0.42f), Math.round(topY - postH * 0.6f),
                   Math.round(cx + w * 0.42f), Math.round(topY - postH * 0.6f));
        final Path2D roof = new Path2D.Float();
        roof.moveTo(cx - w * 0.62f, topY - postH + h * 0.15f);
        roof.lineTo(cx, topY - postH - h * 0.55f);
        roof.lineTo(cx + w * 0.62f, topY - postH + h * 0.15f);
        roof.closePath();
        g.setPaint(new GradientPaint(cx, topY - postH - h * 0.55f,
                new Color(150, 70, 45), cx, topY - postH + h * 0.15f,
                new Color(105, 45, 30)));
        g.fill(roof);
        g.setColor(new Color(60, 30, 20));
        g.draw(roof);
        // The bucket hanging over the water
        g.setColor(new Color(120, 85, 50));
        g.fill(new Rectangle.Float(cx - post, topY - postH * 0.45f,
                                   post * 2, post * 1.6f));
    }

    /**
     * Get the goods a type of building works with.
     *
     * @param type The {@code BuildingType} to check.
     * @return The goods used (null if none) and the goods made, or null
     *     if the building makes nothing.
     */
    static GoodsType[] getWorkedGoods(BuildingType type) {
        for (boolean unattended : new boolean[] { false, true }) {
            for (ProductionType pt
                     : type.getAvailableProductionTypes(unattended)) {
                final List<AbstractGoods> outputs = pt.getOutputList();
                if (outputs.isEmpty()) continue;
                final List<AbstractGoods> inputs = pt.getInputList();
                return new GoodsType[] {
                    (inputs.isEmpty()) ? null : inputs.get(0).getType(),
                    outputs.get(0).getType() };
            }
        }
        return null;
    }

    /**
     * Paint the name plates of the buildings of a town, each on the
     * street in front of its building.  The plates of empty sites are
     * faded.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param plan The {@code TownPlan} with the buildings.
     * @param lib The {@code ImageLibrary} for the goods pictures.
     * @param font The font to use.
     * @param width The width of the town.
     */
    public static void paintSigns(Graphics2D g, TownPlan plan,
                                  ImageLibrary lib, Font font, int width) {
        final int road = plan.getRoad();
        final int inset = Math.max(1, road / 10);
        for (TownPlan.Plot p : plan.getPlots()) {
            if (p.bounds == null) continue;
            // A plate may reach half way to the next building in its row.
            final int base = p.bounds.y + p.bounds.height;
            int left = 0, right = width;
            for (TownPlan.Plot o : plan.getPlots()) {
                if (o == p || o.bounds == null
                    || o.bounds.y + o.bounds.height != base) continue;
                if (o.bounds.x < p.bounds.x) {
                    left = Math.max(left,
                        (o.bounds.x + o.bounds.width + p.bounds.x) / 2);
                } else {
                    right = Math.min(right,
                        (p.bounds.x + p.bounds.width + o.bounds.x) / 2);
                }
            }
            final int cx = p.bounds.x + p.bounds.width / 2;
            final int half = Math.min(cx - left, right - cx);
            final GoodsType[] goods = getWorkedGoods(p.type);
            final BufferedImage input = (goods == null || goods[0] == null)
                ? null : lib.getSmallGoodsTypeImage(goods[0]);
            final BufferedImage output = (goods == null) ? null
                : lib.getSmallGoodsTypeImage(goods[1]);
            final Graphics2D g2d = (Graphics2D)g.create();
            try {
                if (p.empty) {
                    g2d.setComposite(AlphaComposite.getInstance(
                            AlphaComposite.SRC_OVER, 0.6f));
                }
                paintSign(g2d, font, cx, base + inset, 2 * half - inset,
                          road - 2 * inset, Messages.getName(p.type),
                          input, output);
            } finally {
                g2d.dispose();
            }
        }
    }

    /**
     * Paint a name plate for a building on the street in front of it.
     *
     * The plate shows the name of the building and, if given, small
     * pictures of the goods it uses and makes.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param font The font to use, shrunk if need be to fit.
     * @param cx The x coordinate of the center of the plate.
     * @param top The y coordinate of the top of the plate.
     * @param maxWidth The greatest width of the plate.
     * @param maxHeight The greatest height of the plate.
     * @param name The name of the building.
     * @param input A picture of the goods used, or null.
     * @param output A picture of the goods made, or null.
     */
    public static void paintSign(Graphics2D g, Font font, int cx, int top,
                                 int maxWidth, int maxHeight, String name,
                                 BufferedImage input, BufferedImage output) {
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // Fit the text into the height of the plate.
            final int pad = Math.max(2, maxHeight / 8);
            Font f = font;
            FontMetrics fm = g2d.getFontMetrics(f);
            while (fm.getAscent() + fm.getDescent() > maxHeight - 2 * pad
                && f.getSize2D() > 8f) {
                f = f.deriveFont(f.getSize2D() - 1f);
                fm = g2d.getFontMetrics(f);
            }
            // Then shrink it a little more, if need be, to fit the width.
            final float least = Math.max(8f, f.getSize2D() * 0.8f);
            while (signWidth(fm, name, input, output, pad) > maxWidth
                && f.getSize2D() > least) {
                f = f.deriveFont(f.getSize2D() - 0.5f);
                fm = g2d.getFontMetrics(f);
            }
            final int textH = fm.getAscent() + fm.getDescent();
            if (textH > maxHeight - pad) return; // No room at all
            final int icon = textH;
            final int arrow = (input != null && output != null)
                ? fm.stringWidth(" \u2192 ") : 0;
            final int iconsW = signWidth(fm, "", input, output, pad)
                - 4 * pad;
            String text = name;
            while (fm.stringWidth(text) + iconsW + 4 * pad > maxWidth
                && text.length() > 3) {
                text = text.substring(0, text.length() - 2) + "\u2026";
            }
            final int w = Math.min(maxWidth,
                                   fm.stringWidth(text) + iconsW + 4 * pad);
            final int h = textH + 2 * pad;
            final int x = cx - w / 2;
            final float arc = h * 0.6f;
            // A small shadow, then a wooden board with a parchment face.
            g2d.setColor(new Color(0, 0, 0, 70));
            g2d.fill(new RoundRectangle2D.Float(x + 2, top + 2, w, h,
                                                arc, arc));
            g2d.setColor(new Color(110, 75, 40));
            g2d.fill(new RoundRectangle2D.Float(x, top, w, h, arc, arc));
            g2d.setColor(new Color(248, 236, 205));
            g2d.fill(new RoundRectangle2D.Float(x + 1.5f, top + 1.5f,
                    w - 3, h - 3, arc, arc));
            int px = x + 2 * pad;
            final int iy = top + pad;
            if (input != null) {
                g2d.drawImage(input, px, iy, icon, icon, null);
                px += icon;
            }
            if (input != null && output != null) {
                g2d.setFont(f);
                g2d.setColor(new Color(90, 60, 30));
                g2d.drawString(" \u2192 ", px, iy + fm.getAscent());
                px += arrow;
            }
            if (output != null) {
                g2d.drawImage(output, px, iy, icon, icon, null);
                px += icon;
            }
            if (iconsW > 0) px += pad;
            g2d.setFont(f);
            g2d.setColor(new Color(50, 35, 20));
            g2d.drawString(text, px, iy + fm.getAscent());
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Get the width of a name plate.
     *
     * @param fm The {@code FontMetrics} of the font used.
     * @param text The text on the plate.
     * @param input A picture of the goods used, or null.
     * @param output A picture of the goods made, or null.
     * @param pad The padding inside the plate.
     * @return The width of the plate.
     */
    private static int signWidth(FontMetrics fm, String text,
                                 BufferedImage input, BufferedImage output,
                                 int pad) {
        final int icon = fm.getAscent() + fm.getDescent();
        int iconsW = 0;
        if (input != null) iconsW += icon;
        if (output != null) iconsW += icon;
        if (input != null && output != null) {
            iconsW += fm.stringWidth(" \u00e2\u2020\u2019 ");
        }
        if (iconsW > 0) iconsW += pad;
        return fm.stringWidth(text) + iconsW + 4 * pad;
    }

    /**
     * Paint the shadows the buildings cast on the ground, falling away
     * to the right as the light comes from the upper left.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param plan The {@code TownPlan} with the buildings.
     * @param pictures Gets the picture standing on a site, or null.
     */
    private static void paintShadows(Graphics2D g, TownPlan plan,
            Function<TownPlan.Plot, BufferedImage> pictures) {
        if (pictures == null) return;
        for (TownPlan.Plot p : plan.getPlots()) {
            if (p.bounds == null) continue;
            final BufferedImage image = pictures.apply(p);
            if (image == null) continue;
            final BufferedImage shadow = getSilhouette(image);
            final int base = p.bounds.y + p.bounds.height;
            final int x = p.bounds.x + (p.bounds.width - image.getWidth()) / 2;
            final Graphics2D g2d = (Graphics2D)g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2d.translate(x, base);
                // Lay the picture down on the ground behind the building
                g2d.transform(new AffineTransform(1, 0, -0.75, 0.38, 0, 0));
                g2d.setComposite(AlphaComposite.getInstance(
                        AlphaComposite.SRC_OVER, 0.22f));
                // Twice, slightly apart, for a softer edge
                g2d.drawImage(shadow, 0, -image.getHeight(), null);
                g2d.drawImage(shadow, 2, -image.getHeight() + 2, null);
            } finally {
                g2d.dispose();
            }
        }
    }

    /**
     * Get the silhouette of a picture, reusing one made before if
     * possible.
     *
     * @param image The picture.
     * @return A black image with the outline of the picture.
     */
    private static synchronized BufferedImage getSilhouette(
        BufferedImage image) {
        return silhouettes.computeIfAbsent(image, img -> {
                final BufferedImage s = new BufferedImage(img.getWidth(),
                    img.getHeight(), BufferedImage.TYPE_INT_ARGB);
                final Graphics2D g = s.createGraphics();
                try {
                    g.drawImage(img, 0, 0, null);
                    g.setComposite(AlphaComposite.SrcIn);
                    g.setColor(new Color(20, 14, 6));
                    g.fillRect(0, 0, img.getWidth(), img.getHeight());
                } finally {
                    g.dispose();
                }
                return s;
            });
    }

    /**
     * Give the scene some depth: a light haze over the far side of the
     * town, a little shade on the near side and darker edges.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the town.
     * @param height The height of the town.
     */
    private static void paintDepth(Graphics2D g, int width, int height) {
        g.setPaint(new GradientPaint(0, 0, new Color(214, 220, 228, 60),
                0, height * 0.45f, new Color(214, 220, 228, 0)));
        g.fillRect(0, 0, width, Math.round(height * 0.45f));
        g.setPaint(new GradientPaint(0, height * 0.7f, new Color(0, 0, 0, 0),
                0, height, new Color(20, 12, 0, 45)));
        g.fillRect(0, Math.round(height * 0.7f), width,
                   height - Math.round(height * 0.7f));
        paintVignette(g, width, height);
    }

    /**
     * Paint a grove of trees.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param r Where the grove stands.
     * @param trees A picture of a stand of trees.
     */
    private static void paintGrove(Graphics2D g, Rectangle r,
                                   BufferedImage trees) {
        final float scale = Math.min(1f,
            r.height * 1.25f / trees.getHeight());
        final int tw = Math.round(trees.getWidth() * scale);
        final int th = Math.round(trees.getHeight() * scale);
        if (tw <= 0 || th <= 0) return;
        final int count = Math.max(1, Math.round(r.width / (tw * 0.6f)));
        final float step = (count == 1) ? 0 : (r.width - tw) / (count - 1f);
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            // A soft shadow on the ground under the trees
            final Color dark = new Color(20, 30, 10, 55);
            final Color none = new Color(20, 30, 10, 0);
            final float cy = r.y + r.height - th * 0.25f;
            final Graphics2D sg = (Graphics2D)g2d.create();
            sg.translate(r.x + r.width / 2f, cy);
            sg.scale(r.width / (float)th, 0.5);
            sg.setPaint(new RadialGradientPaint(0f, 0f, th * 0.6f,
                new float[] { 0.5f, 1f }, new Color[] { dark, none }));
            sg.fill(new Ellipse2D.Float(-th * 0.6f, -th * 0.6f,
                                        th * 1.2f, th * 1.2f));
            sg.dispose();
            for (int i = 0; i < count; i++) {
                final float x = (count == 1) ? r.x + (r.width - tw) / 2f
                    : r.x + i * step;
                g2d.drawImage(trees, Math.round(x),
                    r.y + r.height - th, tw, th, null);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Darken the edges of the town a little, to frame it.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the town.
     * @param height The height of the town.
     */
    private static void paintVignette(Graphics2D g, int width, int height) {
        final float radius = (float)Math.hypot(width, height) / 2f;
        if (radius <= 0) return;
        g.setPaint(new RadialGradientPaint(width / 2f, height / 2f, radius,
                new float[] { 0.6f, 1f },
                new Color[] { new Color(0, 0, 0, 0),
                              new Color(20, 12, 0, 90) }));
        g.fillRect(0, 0, width, height);
    }
}
