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

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.TileType;


/**
 * Optional pictures for the town in the colony panel: the sky and the
 * land beyond the town, ground textures, walls, fences, trees and so
 * on, kept in {@code resources/images/town} of the default rules.
 *
 * Every picture is optional.  When one is missing, {@link TownPainter}
 * draws that part of the town itself, so the pictures can be added one
 * at a time.  See PROMPTS.md in that directory for the list.
 */
public final class TownArt {

    private static final Logger logger
        = Logger.getLogger(TownArt.class.getName());

    /** The file name extensions tried, in order. */
    private static final String[] EXTENSIONS = { ".png", ".jpg", ".jpeg" };

    /** The pictures loaded, or empty if missing. */
    private static final Map<String, Optional<BufferedImage>> cache
        = new HashMap<>();

    /** The kinds of country around a town. */
    public static final String TEMPERATE = "temperate", ARID = "arid",
        COLD = "cold", TROPICAL = "tropical";


    private TownArt() {} // Static only

    /**
     * Get the directory holding the pictures.
     *
     * @return The directory.
     */
    private static File getDirectory() {
        final String dir = System.getProperty("freecol.townArt");
        return (dir != null) ? new File(dir)
            : new File(FreeColDirectories.getDataDirectory(),
                       "default/resources/images/town");
    }

    /**
     * Get a picture by name.
     *
     * Pictures on a flat magenta background, for tools that can not
     * make transparent ones, have the magenta removed.
     *
     * @param name The name of the picture, without extension.
     * @return The picture, or null if there is none.
     */
    public static synchronized BufferedImage get(String name) {
        return cache.computeIfAbsent(name, n -> Optional.ofNullable(load(n)))
            .orElse(null);
    }

    /**
     * Forget the pictures loaded, so that new ones are seen.
     */
    public static synchronized void clear() {
        cache.clear();
    }

    /**
     * Load a picture.
     *
     * @param name The name of the picture, without extension.
     * @return The picture, or null if there is none or it can not be read.
     */
    private static BufferedImage load(String name) {
        for (String ext : EXTENSIONS) {
            final File file = new File(getDirectory(), name + ext);
            if (!file.isFile()) continue;
            try {
                final BufferedImage raw = ImageIO.read(file);
                if (raw == null) continue;
                return removeMagenta(toArgb(raw));
            } catch (IOException ioe) {
                logger.log(Level.WARNING, "Can not read " + file, ioe);
            }
        }
        return null;
    }

    /**
     * Copy a picture into the ARGB format.
     *
     * @param raw The picture.
     * @return An ARGB copy.
     */
    private static BufferedImage toArgb(BufferedImage raw) {
        if (raw.getType() == BufferedImage.TYPE_INT_ARGB) return raw;
        final BufferedImage img = new BufferedImage(raw.getWidth(),
            raw.getHeight(), BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g = img.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(raw, 0, 0, null);
        } finally {
            g.dispose();
        }
        return img;
    }

    /**
     * Make a flat magenta background transparent, if the picture has
     * one (judged by its corners).
     *
     * @param img The ARGB picture, changed in place.
     * @return The picture.
     */
    static BufferedImage removeMagenta(BufferedImage img) {
        final int w = img.getWidth(), h = img.getHeight();
        if (!isMagenta(img.getRGB(0, 0)) || !isMagenta(img.getRGB(w - 1, 0))
            || !isMagenta(img.getRGB(0, h - 1))
            || !isMagenta(img.getRGB(w - 1, h - 1))) return img;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                final int argb = img.getRGB(x, y);
                final int r = (argb >> 16) & 0xff, gr = (argb >> 8) & 0xff,
                    b = argb & 0xff;
                // How far from magenta: 0 is pure magenta
                final int green = gr, pink = Math.min(r, b);
                if (green < 90 && pink > 160) {
                    img.setRGB(x, y, 0);
                } else if (green < 140 && pink > 140) {
                    // Soften the fringe left by anti-aliasing
                    final int alpha = Math.min(255, (green - 90) * 5 + 40);
                    img.setRGB(x, y, (alpha << 24) | (argb & 0xffffff));
                }
            }
        }
        return img;
    }

    private static boolean isMagenta(int argb) {
        final int r = (argb >> 16) & 0xff, g = (argb >> 8) & 0xff,
            b = argb & 0xff;
        return r > 180 && b > 180 && g < 90;
    }

    /**
     * Decide what kind of country a type of land is, to pick the
     * pictures of the sky and ground that suit it.
     *
     * @param type The {@code TileType} the colony stands on.
     * @return One of TEMPERATE, ARID, COLD or TROPICAL.
     */
    public static String getBiome(TileType type) {
        final String id = (type == null) ? "" : type.getSuffix();
        switch (id) {
        case "desert": case "scrubForest":
            return ARID;
        case "tundra": case "borealForest": case "arctic":
            return COLD;
        case "savannah": case "tropicalForest": case "swamp":
        case "rainForest": case "marsh": case "wetlandForest":
            return TROPICAL;
        default:
            return TEMPERATE;
        }
    }

    /**
     * Get the picture of the ground for a kind of land.
     *
     * @param type The {@code TileType} the colony stands on.
     * @return The ground picture, or null if there is none.
     */
    public static BufferedImage getGround(TileType type) {
        final String id = (type == null) ? "" : type.getSuffix();
        switch (id) {
        case "desert": case "scrubForest":
            return get("ground_sand");
        case "savannah": case "prairie":
            return get("ground_grass_dry");
        case "tundra": case "borealForest": case "arctic":
            return get("ground_snow");
        default:
            return get("ground_grass");
        }
    }
}
