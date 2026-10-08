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
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.Random;


/**
 * Paints the European port as a merchant's desk: a wooden desk, with
 * the parts of the port laid on it as sheets of parchment, the ships
 * in port on a painting of the sea, the docks on an old map, and the
 * name of the port on a scroll.
 *
 * Like the town (see {@link TownArt}), every part can be replaced by a
 * picture, and is drawn here when there is none.
 */
public final class PortPainter {

    /** The colour of the ink on the parchment. */
    public static final Color INK = new Color(58, 38, 20);

    private PortPainter() {} // Static only

    /**
     * Paint the wooden desk the port is laid out on.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param width The width of the desk.
     * @param height The height of the desk.
     */
    public static void paintDesk(Graphics2D g, int width, int height) {
        final BufferedImage art = TownArt.get("europe_desk");
        if (art != null) {
            g.drawImage(art, 0, 0, width, height, null);
            return;
        }
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            final Random random = new Random(1588);
            final int plank = Math.max(40, height / 9);
            for (int y = 0; y < height; y += plank) {
                final int shade = random.nextInt(18);
                g2d.setColor(new Color(92 + shade, 58 + shade / 2,
                                       33 + shade / 3));
                g2d.fillRect(0, y, width, plank);
                // The grain runs along the plank
                for (int i = 0; i < plank / 2; i++) {
                    final float gy = y + random.nextFloat() * plank;
                    final int a = 25 + random.nextInt(45);
                    g2d.setColor((random.nextBoolean())
                        ? new Color(60, 36, 18, a) : new Color(140, 96, 58, a));
                    final Path2D grain = new Path2D.Float();
                    grain.moveTo(0, gy);
                    final float wave = 1f + random.nextFloat() * 3f;
                    for (int x = 0; x <= width; x += 40) {
                        grain.lineTo(x, gy + (float)Math.sin(x / (60f
                            + random.nextFloat() * 40f)) * wave);
                    }
                    g2d.setStroke(new BasicStroke(0.6f
                        + random.nextFloat() * 1.2f));
                    g2d.draw(grain);
                }
                // A knot or two
                if (random.nextInt(3) == 0) {
                    final float kx = random.nextFloat() * width;
                    final float ky = y + plank * (0.3f + random.nextFloat() * 0.4f);
                    final float kr = plank * 0.18f;
                    g2d.setColor(new Color(58, 34, 16, 150));
                    g2d.fill(new Ellipse2D.Float(kx - kr * 1.6f, ky - kr * 0.6f,
                                                 kr * 3.2f, kr * 1.2f));
                }
                // The seam between planks
                g2d.setColor(new Color(36, 20, 10, 200));
                g2d.fillRect(0, y + plank - 2, width, 2);
                g2d.setColor(new Color(160, 116, 70, 60));
                g2d.fillRect(0, y + plank, width, 1);
            }
            // Darker towards the edges of the desk
            g2d.setPaint(new RadialGradientPaint(width / 2f, height / 2f,
                (float)Math.hypot(width, height) / 2f, new float[] { 0.55f, 1f },
                new Color[] { new Color(0, 0, 0, 0), new Color(20, 10, 0, 140) }));
            g2d.fillRect(0, 0, width, height);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint a sheet of parchment.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param r Where the sheet lies.
     * @param seed A seed, so that each sheet has its own stains.
     */
    public static void paintParchment(Graphics2D g, Rectangle r, long seed) {
        if (r.width <= 0 || r.height <= 0) return;
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            final float arc = Math.min(12f, r.height / 6f);
            final Shape sheet = new RoundRectangle2D.Float(r.x, r.y,
                r.width, r.height, arc, arc);
            // A shadow on the desk
            g2d.setColor(new Color(0, 0, 0, 90));
            g2d.fill(new RoundRectangle2D.Float(r.x + 4, r.y + 5,
                r.width, r.height, arc, arc));
            final BufferedImage art = TownArt.get("parchment");
            if (art != null) {
                g2d.setClip(sheet);
                g2d.drawImage(art, r.x, r.y, r.width, r.height, null);
            } else {
                g2d.setColor(new Color(234, 216, 178));
                g2d.fill(sheet);
                g2d.setClip(sheet);
                final Random random = new Random(seed);
                for (int i = 0; i < 7; i++) {
                    final float cx = r.x + random.nextFloat() * r.width;
                    final float cy = r.y + random.nextFloat() * r.height;
                    final float rr = Math.max(10f, Math.min(r.width, r.height)
                        * (0.2f + random.nextFloat() * 0.5f));
                    g2d.setPaint(new RadialGradientPaint(cx, cy, rr,
                        new float[] { 0f, 1f },
                        new Color[] { new Color(196, 168, 120, 55),
                                      new Color(196, 168, 120, 0) }));
                    g2d.fill(new Ellipse2D.Float(cx - rr, cy - rr, 2 * rr,
                                                 2 * rr));
                }
            }
            // Edges darkened with age
            final float burn = Math.min(18f, Math.min(r.width, r.height) / 5f);
            for (int i = 0; i < 6; i++) {
                final float inset = burn * i / 6f;
                g2d.setStroke(new BasicStroke(burn / 5f));
                g2d.setColor(new Color(120, 82, 40, 50 - i * 7));
                g2d.draw(new RoundRectangle2D.Float(r.x + inset, r.y + inset,
                    r.width - 2 * inset, r.height - 2 * inset, arc, arc));
            }
            g2d.setClip(null);
            g2d.setStroke(new BasicStroke(1f));
            g2d.setColor(new Color(110, 76, 40, 160));
            g2d.draw(sheet);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint a painting of the sea, for the ships in port.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param r Where the painting hangs.
     */
    public static void paintHarbour(Graphics2D g, Rectangle r) {
        if (r.width <= 0 || r.height <= 0) return;
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(0, 0, 0, 90));
            g2d.fillRect(r.x + 4, r.y + 5, r.width, r.height);
            final BufferedImage art = TownArt.get("europe_harbour");
            if (art != null) {
                g2d.drawImage(art, r.x, r.y, r.width, r.height, null);
            } else {
                g2d.clip(r);
                final float horizon = r.y + r.height * 0.55f;
                g2d.setPaint(new GradientPaint(0, r.y, new Color(150, 186, 222),
                    0, horizon, new Color(228, 232, 228)));
                g2d.fill(new Rectangle.Float(r.x, r.y, r.width,
                                             horizon - r.y));
                // A few clouds
                final Random random = new Random(r.width);
                for (int i = 0; i < 4; i++) {
                    final float cx = r.x + random.nextFloat() * r.width;
                    final float cy = r.y + (horizon - r.y) * (0.2f
                        + random.nextFloat() * 0.4f);
                    final float cr = r.height * (0.12f + random.nextFloat() * 0.1f);
                    g2d.setColor(new Color(255, 255, 255, 120));
                    g2d.fill(new Ellipse2D.Float(cx - cr * 1.8f, cy - cr * 0.4f,
                                                 cr * 3.6f, cr * 0.8f));
                }
                // The far shore
                g2d.setColor(new Color(132, 150, 150));
                final Path2D shore = new Path2D.Float();
                shore.moveTo(r.x, horizon);
                for (int x = 0; x <= r.width; x += 8) {
                    shore.lineTo(r.x + x, horizon - (float)(Math.sin(x / 37.0)
                        + 1.2) * r.height * 0.03f);
                }
                shore.lineTo(r.x + r.width, horizon);
                shore.closePath();
                g2d.fill(shore);
                // The sea, with a little swell
                g2d.setPaint(new GradientPaint(0, horizon, new Color(84, 126, 150),
                    0, r.y + r.height, new Color(36, 72, 98)));
                g2d.fill(new Rectangle.Float(r.x, horizon, r.width,
                                             r.y + r.height - horizon));
                g2d.setStroke(new BasicStroke(1.2f));
                for (int i = 0; i < r.width * r.height / 900; i++) {
                    final float wx = r.x + random.nextFloat() * r.width;
                    final float wy = horizon + 3 + random.nextFloat()
                        * (r.y + r.height - horizon - 4);
                    final float wl = 4f + (wy - horizon) * 0.25f;
                    g2d.setColor(new Color(220, 236, 240, 70));
                    g2d.drawLine(Math.round(wx), Math.round(wy),
                                 Math.round(wx + wl), Math.round(wy));
                }
            }
            // A plain wooden frame
            g2d.setClip(null);
            g2d.setStroke(new BasicStroke(4f));
            g2d.setColor(new Color(96, 62, 32));
            g2d.drawRect(r.x, r.y, r.width, r.height);
            g2d.setStroke(new BasicStroke(1f));
            g2d.setColor(new Color(170, 126, 76));
            g2d.drawRect(r.x + 2, r.y + 2, r.width - 4, r.height - 4);
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint an old map, for the docks.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param r Where the map lies.
     */
    public static void paintMap(Graphics2D g, Rectangle r) {
        if (r.width <= 0 || r.height <= 0) return;
        final BufferedImage art = TownArt.get("europe_map");
        if (art != null) {
            final Graphics2D g2d = (Graphics2D)g.create();
            try {
                g2d.setColor(new Color(0, 0, 0, 90));
                g2d.fillRect(r.x + 4, r.y + 5, r.width, r.height);
                g2d.drawImage(art, r.x, r.y, r.width, r.height, null);
            } finally {
                g2d.dispose();
            }
            return;
        }
        paintParchment(g, r, 1492);
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.clip(r);
            // Lines of latitude and longitude
            g2d.setStroke(new BasicStroke(1f));
            g2d.setColor(new Color(120, 90, 50, 50));
            final int step = Math.max(30, r.width / 8);
            for (int x = r.x + step; x < r.x + r.width; x += step) {
                g2d.drawLine(x, r.y, x, r.y + r.height);
            }
            for (int y = r.y + step; y < r.y + r.height; y += step) {
                g2d.drawLine(r.x, y, r.x + r.width, y);
            }
            // The coasts of two continents across the ocean
            final Random random = new Random(1497);
            g2d.setStroke(new BasicStroke(1.6f));
            g2d.setColor(new Color(110, 76, 40, 120));
            for (float side : new float[] { 0.12f, 0.88f }) {
                final Path2D coast = new Path2D.Float();
                float x = r.x + r.width * side;
                coast.moveTo(x, r.y);
                for (float y = r.y; y <= r.y + r.height; y += 10) {
                    x += (random.nextFloat() - 0.5f) * 14f;
                    coast.lineTo(x, y);
                }
                g2d.draw(coast);
            }
            // A compass rose
            final float cx = r.x + r.width * 0.78f, cy = r.y + r.height * 0.7f;
            final float rr = Math.min(r.width, r.height) * 0.18f;
            for (int i = 0; i < 8; i++) {
                final double a = i * Math.PI / 4;
                final float len = (i % 2 == 0) ? rr : rr * 0.55f;
                final Path2D point = new Path2D.Float();
                point.moveTo(cx, cy);
                point.lineTo(cx + Math.cos(a - 0.18) * len * 0.25,
                             cy + Math.sin(a - 0.18) * len * 0.25);
                point.lineTo(cx + Math.cos(a) * len, cy + Math.sin(a) * len);
                point.lineTo(cx + Math.cos(a + 0.18) * len * 0.25,
                             cy + Math.sin(a + 0.18) * len * 0.25);
                point.closePath();
                g2d.setColor(new Color(110, 76, 40, (i % 2 == 0) ? 120 : 80));
                g2d.fill(point);
            }
            g2d.setColor(new Color(110, 76, 40, 100));
            g2d.draw(new Ellipse2D.Float(cx - rr * 0.7f, cy - rr * 0.7f,
                                         rr * 1.4f, rr * 1.4f));
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint a scroll for the name of the port.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param r Where the scroll lies.
     */
    public static void paintScroll(Graphics2D g, Rectangle r) {
        if (r.width <= 0 || r.height <= 0) return;
        final BufferedImage art = TownArt.get("banner_scroll");
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            if (art != null) {
                g2d.drawImage(art, r.x, r.y, r.width, r.height, null);
                return;
            }
            final float roll = r.height * 0.32f;
            final Rectangle band = new Rectangle(r.x + Math.round(roll),
                r.y + r.height / 8, r.width - Math.round(2 * roll),
                r.height * 3 / 4);
            paintParchment(g2d, band, 1600);
            // The rolled ends
            for (float x : new float[] { r.x, r.x + r.width - 2 * roll }) {
                final Shape end = new RoundRectangle2D.Float(x + roll * 0.3f,
                    r.y, roll * 1.4f, r.height, roll, roll);
                g2d.setPaint(new GradientPaint(x, 0, new Color(170, 140, 96),
                    x + roll * 1.7f, 0, new Color(236, 214, 170)));
                g2d.fill(end);
                g2d.setColor(new Color(110, 76, 40, 170));
                g2d.draw(end);
            }
        } finally {
            g2d.dispose();
        }
    }

    /**
     * Paint a wooden tray, with a slot for each kind of goods.
     *
     * @param g The {@code Graphics2D} to paint with.
     * @param tray Where the tray lies.
     * @param slots Where the slots lie.
     */
    public static void paintMarket(Graphics2D g, Rectangle tray,
                                   Iterable<Rectangle> slots) {
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setColor(new Color(0, 0, 0, 100));
            g2d.fillRect(tray.x + 4, tray.y + 5, tray.width, tray.height);
            g2d.setPaint(new GradientPaint(0, tray.y, new Color(112, 72, 38),
                0, tray.y + tray.height, new Color(74, 46, 24)));
            g2d.fillRect(tray.x, tray.y, tray.width, tray.height);
            g2d.setColor(new Color(176, 132, 80));
            g2d.drawRect(tray.x + 1, tray.y + 1, tray.width - 3,
                         tray.height - 3);
            for (Rectangle s : slots) {
                final Shape slot = new RoundRectangle2D.Float(s.x + 2, s.y + 2,
                    s.width - 4, s.height - 4, 8, 8);
                g2d.setColor(new Color(40, 24, 12));
                g2d.fill(new RoundRectangle2D.Float(s.x + 1, s.y + 1,
                    s.width - 2, s.height - 2, 9, 9));
                g2d.setColor(new Color(236, 220, 186));
                g2d.fill(slot);
                g2d.setPaint(new GradientPaint(0, s.y, new Color(0, 0, 0, 40),
                    0, s.y + s.height * 0.4f, new Color(0, 0, 0, 0)));
                g2d.fill(slot);
            }
        } finally {
            g2d.dispose();
        }
    }
}
