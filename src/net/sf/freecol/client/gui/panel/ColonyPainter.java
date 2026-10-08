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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * Paints the right hand side of the colony panel: the carved board
 * with a sheet of parchment pinned to it, under the colony tiles and
 * the numbers in the octagons, the card of what is being built, and
 * the sea of the port below.
 *
 * Like the town (see {@link TownArt}), the parchment and the sea can
 * be replaced by pictures, and are drawn here when there are none.
 */
public final class ColonyPainter {

    /** The width of the board picture, unscaled. */
    public static final int BOARD_WIDTH = 474;

    /** Where the parchment lies on the plain wood of the board, unscaled. */
    private static final Rectangle SHEET = new Rectangle(60, 6, 386, 372);

    /** The centres of the octagons on the board, unscaled. */
    private static final int[][] OCTAGONS
        = { { 100, 40 }, { 405, 40 }, { 100, 186 }, { 405, 186 } };

    /** Half the height of an octagon, with its rim, unscaled. */
    private static final float OCTAGON_SIZE = 36f;

    /** The colour of the ink numbers are engraved in. */
    private static final Color ENGRAVED = new Color(40, 26, 12);

    /** The light catching the lower edge of an engraving. */
    private static final Color ENGRAVED_LIGHT = new Color(255, 244, 220, 170);

    /** How far down the board its carving ends, unscaled. */
    private static final int BOARD_BOTTOM = 432;

    private ColonyPainter() {} // Static only

    /**
     * Get how far down a board its carving ends.
     *
     * @param board The picture of the board, scaled.
     * @return The distance from the top of the board.
     */
    public static int getBoardBottom(BufferedImage board) {
        return Math.round(BOARD_BOTTOM * board.getWidth() / (float)BOARD_WIDTH);
    }

    /**
     * Paint the board: the carved wood, a sheet of parchment over its
     * plain middle, and the octagons over the parchment.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param board The picture of the board, scaled.
     * @param x The x coordinate of the left of the board.
     * @param y The y coordinate of the top of the board.
     */
    public static void paintBoard(Graphics2D g, BufferedImage board,
                                  int x, int y) {
        final float scale = board.getWidth() / (float)BOARD_WIDTH;
        g.drawImage(board, x, y, null);
        final Rectangle sheet = new Rectangle(
            x + Math.round(SHEET.x * scale), y + Math.round(SHEET.y * scale),
            Math.round(SHEET.width * scale), Math.round(SHEET.height * scale));
        PortPainter.paintParchment(g, sheet, 7);

        final Path2D octagons = new Path2D.Float();
        for (int[] c : OCTAGONS) {
            octagons.append(octagon(x + c[0] * scale, y + c[1] * scale,
                                    OCTAGON_SIZE * scale), false);
        }
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            // The octagons stand a little off the parchment
            g2d.setColor(new Color(0, 0, 0, 80));
            g2d.fill(AffineTransform.getTranslateInstance(3 * scale,
                    4 * scale).createTransformedShape(octagons));
            g2d.clip(octagons);
            g2d.drawImage(board, x, y, null);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Make an octagon with flat sides at the top, bottom, left and
     * right.
     *
     * @param cx The x coordinate of the centre.
     * @param cy The y coordinate of the centre.
     * @param size The distance from the centre to a side.
     * @return The octagon.
     */
    private static Shape octagon(float cx, float cy, float size) {
        final double r = size / Math.cos(Math.PI / 8);
        final Path2D path = new Path2D.Float();
        for (int i = 0; i < 8; i++) {
            final double a = Math.PI / 8 + i * Math.PI / 4;
            final double px = cx + r * Math.cos(a), py = cy + r * Math.sin(a);
            if (i == 0) path.moveTo(px, py); else path.lineTo(px, py);
        }
        path.closePath();
        return path;
    }

    /**
     * Paint a number as if engraved in wood, centred on a point.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param text The number.
     * @param font The font to use.
     * @param cx The x coordinate of the centre.
     * @param cy The y coordinate of the centre.
     */
    public static void paintEngraved(Graphics2D g, String text, Font font,
                                     int cx, int cy) {
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                 RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setFont(font);
            final FontMetrics fm = g2d.getFontMetrics();
            final int x = cx - fm.stringWidth(text) / 2;
            final int y = cy + (fm.getAscent() - fm.getDescent()) / 2;
            final int d = Math.max(1, font.getSize() / 24);
            g2d.setColor(ENGRAVED_LIGHT);
            g2d.drawString(text, x + d, y + d);
            g2d.setColor(ENGRAVED);
            g2d.drawString(text, x, y);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint a card of parchment in a wooden frame, for what the
     * colony is building.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the card.
     * @param height The height of the card.
     */
    public static void paintCard(Graphics2D g, int width, int height) {
        if (width <= 8 || height <= 8) return;
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            final float frame = Math.max(3f, Math.min(width, height) / 30f);
            final float arc = frame * 3;
            g2d.setColor(new Color(0, 0, 0, 70));
            g2d.fill(new RoundRectangle2D.Float(2, 3, width - 2, height - 3,
                                                arc, arc));
            // The frame
            g2d.setPaint(new GradientPaint(0, 0, new Color(122, 82, 44),
                0, height, new Color(74, 46, 22)));
            g2d.fill(new RoundRectangle2D.Float(0, 0, width - 2, height - 3,
                                                arc, arc));
            // The card in it, a little darker than the sheet beneath
            final RoundRectangle2D card = new RoundRectangle2D.Float(frame,
                frame, width - 2 - 2 * frame, height - 3 - 2 * frame,
                arc / 2, arc / 2);
            g2d.setPaint(new GradientPaint(0, frame, new Color(238, 222, 186),
                0, height - frame, new Color(220, 198, 154)));
            g2d.fill(card);
            g2d.setStroke(new BasicStroke(1f));
            g2d.setColor(new Color(60, 36, 16, 160));
            g2d.draw(card);
            g2d.setColor(new Color(196, 150, 96, 120));
            g2d.draw(new RoundRectangle2D.Float(0.5f, 0.5f, width - 3,
                                                height - 4, arc, arc));
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint the sea of the port, with the coast across the water.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param r Where the sea and sky are.
     * @param horizon The y coordinate of the horizon.
     */
    public static void paintSea(Graphics2D g, Rectangle r, int horizon) {
        if (r.width <= 0 || r.height <= 0) return;
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.clip(r);
            final int bottom = r.y + r.height;
            final BufferedImage art = TownArt.get("colony_sea");
            if (art != null) {
                // The picture is all sea, below the horizon
                g2d.setPaint(new GradientPaint(0, r.y, new Color(150, 186, 222),
                    0, horizon, new Color(214, 226, 226)));
                g2d.fill(new Rectangle(r.x, r.y, r.width, horizon - r.y));
                g2d.drawImage(art, r.x, horizon, r.width, bottom - horizon,
                              null);
                return;
            }
            g2d.setPaint(new GradientPaint(0, r.y, new Color(150, 186, 222),
                0, horizon, new Color(214, 226, 226)));
            g2d.fill(new Rectangle(r.x, r.y, r.width, horizon - r.y));
            final int depth = bottom - horizon;
            final Random random = new Random(r.width);
            // The wooded coast across the water
            final float coast = Math.max(4f, depth * 0.08f);
            final Path2D land = new Path2D.Float();
            land.moveTo(r.x, horizon + 1);
            for (int x = 0; x <= r.width; x += 6) {
                land.lineTo(r.x + x, horizon - coast
                    * (0.6f + 0.4f * (float)Math.sin(x / 53.0))
                    - random.nextFloat() * coast * 0.5f);
            }
            land.lineTo(r.x + r.width, horizon + 1);
            land.closePath();
            g2d.setColor(new Color(92, 118, 100));
            g2d.fill(land);
            // The sea, darker nearer
            g2d.setPaint(new GradientPaint(0, horizon, new Color(84, 134, 152),
                0, bottom, new Color(28, 64, 90)));
            g2d.fill(new Rectangle(r.x, horizon, r.width, depth));
            g2d.setColor(new Color(236, 244, 240, 120));
            g2d.drawLine(r.x, horizon, r.x + r.width, horizon);
            // The swell, longer and further apart nearer
            for (int i = 0; i < r.width * depth / 260; i++) {
                final float t = random.nextFloat();
                final float wy = horizon + 2 + t * t * (depth - 3);
                final float near = (wy - horizon) / Math.max(1, depth);
                final float wx = r.x + random.nextFloat() * r.width;
                final float wl = 3f + near * r.width * 0.05f;
                final float wh = 0.6f + near * 2.2f;
                final Path2D crest = new Path2D.Float();
                crest.moveTo(wx, wy);
                crest.quadTo(wx + wl / 2, wy - wh, wx + wl, wy);
                g2d.setStroke(new BasicStroke(0.6f + near * 1.4f,
                    BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2d.setColor(new Color(10, 30, 48, 60));
                g2d.draw(AffineTransform.getTranslateInstance(0, wh + 1)
                    .createTransformedShape(crest));
                g2d.setColor(new Color(214, 234, 236, 40 + (int)(near * 80)));
                g2d.draw(crest);
            }
        } finally {
            g2d.dispose();
        }
    }
}
