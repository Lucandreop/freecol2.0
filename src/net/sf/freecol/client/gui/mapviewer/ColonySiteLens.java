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

package net.sf.freecol.client.gui.mapviewer;

import static net.sf.freecol.common.util.CollectionUtils.transform;

import java.awt.Color;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;


/**
 * Rates the explored land tiles as colony sites for a player, so the
 * map can be tinted from red (poor) to green (excellent).
 *
 * The rating is {@link Player#getColonyValue(Tile)}, the same value
 * the AI uses to choose where to build colonies.  It is expensive to
 * compute, so values are cached until the turn changes or the player
 * founds or loses a settlement.
 */
class ColonySiteLens {

    /** Alpha of the tint drawn over each tile. */
    private static final int ALPHA = 110;

    /** Tint for land where a colony can not be built. */
    private static final Color UNUSABLE = new Color(0, 0, 0, 70);

    /** The cached values, by tile. */
    private final HashMap<Tile, Integer> values = new HashMap<>();

    /**
     * The value shown as fully green.  The 90th percentile of the
     * usable sites, so a few exceptional tiles do not turn all the
     * others red.
     */
    private int reference = 0;

    /** The state the cache was computed for. */
    private Player player = null;
    private int turn = -1;
    private int settlementCount = -1;


    /**
     * Get the tint for a tile.
     *
     * @param player The {@code Player} looking for a colony site.
     * @param tile The {@code Tile} to rate.
     * @return The {@code Color} to draw over the tile, or null to
     *     leave it untouched.
     */
    public Color getColor(Player player, Tile tile) {
        if (!tile.isExplored() || !tile.isLand()) return null;
        validate(player);
        Integer value = this.values.get(tile);
        if (value == null) { // Newly explored this turn
            value = player.getColonyValue(tile);
            this.values.put(tile, value);
        }
        if (value <= 0) return UNUSABLE;
        float ratio = (this.reference <= 0) ? 1.0f
            : Math.min(1.0f, (float)value / this.reference);
        // Hue 0 (red) through 1/6 (yellow) to 1/3 (green)
        Color c = Color.getHSBColor(ratio / 3.0f, 0.9f, 0.9f);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), ALPHA);
    }

    /**
     * Rebuild the cache if the player, turn or settlement count changed.
     *
     * The values of all explored land tiles are computed at once so
     * the colors are scaled consistently across the whole map.
     *
     * @param player The {@code Player} looking for a colony site.
     */
    private void validate(Player player) {
        final int turn = player.getGame().getTurn().getNumber();
        final int settlementCount = player.getSettlementCount();
        if (player == this.player && turn == this.turn
            && settlementCount == this.settlementCount) return;

        this.player = player;
        this.turn = turn;
        this.settlementCount = settlementCount;
        this.values.clear();
        player.getGame().getMap().forEachTile(
            t -> t.isExplored() && t.isLand(),
            t -> this.values.put(t, player.getColonyValue(t)));
        List<Integer> usable = transform(this.values.values(), v -> v > 0);
        Collections.sort(usable);
        this.reference = (usable.isEmpty()) ? 0
            : usable.get((usable.size() * 9) / 10);
    }
}
