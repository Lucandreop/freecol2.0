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

package net.sf.freecol.server.ai;

import java.util.Random;

import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Player;


/**
 * The personality of a European AI player, so that the AI nations do
 * not all behave alike and are harder to predict.
 *
 * Each trait runs from 0 to 1.  The traits start from a base that
 * depends on the national advantage (conquerors are aggressive,
 * traders are mercantile, and so on) and vary randomly by up to 0.2
 * either way.  The variation is derived from the game identifier and
 * the nation, so a player keeps its personality when a game is saved
 * and loaded, but not from one game to the next.
 */
public final class AIPersonality {

    /** How far a trait may vary from its base, either way. */
    private static final double VARIATION = 0.2;

    /** Willingness to go to war. */
    private final double aggression;

    /** Interest in trade. */
    private final double mercantile;

    /** Interest in exploring and settling new land. */
    private final double expansion;


    /**
     * Create a personality with the given traits.
     *
     * @param aggression Willingness to go to war.
     * @param mercantile Interest in trade.
     * @param expansion Interest in exploring and settling new land.
     */
    public AIPersonality(double aggression, double mercantile,
                         double expansion) {
        this.aggression = clamp(aggression);
        this.mercantile = clamp(mercantile);
        this.expansion = clamp(expansion);
    }

    /**
     * Create the personality of a player.
     *
     * @param player The {@code Player} to create a personality for.
     * @return The {@code AIPersonality} of the player.
     */
    public static AIPersonality create(Player player) {
        final double[] base = getBaseTraits(player.getNationType().getId());
        final Random random = new Random(31L
            * player.getGame().getUUID().hashCode()
            + player.getNationId().hashCode());
        return new AIPersonality(vary(base[0], random), vary(base[1], random),
                                 vary(base[2], random));
    }

    /**
     * Get the base traits for a nation type.
     *
     * @param nationTypeId The nation type identifier.
     * @return The base aggression, mercantile and expansion traits.
     */
    private static double[] getBaseTraits(String nationTypeId) {
        switch (nationTypeId) {
        case "model.nationType.conquest":
            return new double[] { 0.75, 0.3, 0.6 };
        case "model.nationType.trade":
            return new double[] { 0.3, 0.8, 0.5 };
        case "model.nationType.cooperation":
            return new double[] { 0.25, 0.5, 0.5 };
        case "model.nationType.immigration":
            return new double[] { 0.4, 0.4, 0.8 };
        case "model.nationType.naval":
            return new double[] { 0.55, 0.6, 0.4 };
        default:
            return new double[] { 0.45, 0.5, 0.5 };
        }
    }

    private static double vary(double value, Random random) {
        return value + (random.nextDouble() * 2.0 - 1.0) * VARIATION;
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    public double getAggression() {
        return this.aggression;
    }

    public double getMercantile() {
        return this.mercantile;
    }

    public double getExpansion() {
        return this.expansion;
    }

    /**
     * Get the factor to apply to tension before deciding the stance
     * towards another player.  Aggressive players are provoked sooner
     * and stay at war longer.  The factor stays near 1 for peaceful
     * players, as war is only declared near the maximum tension.
     *
     * @return A factor from 0.95 to 1.25.
     */
    public double getTensionFactor() {
        return 0.95 + 0.3 * this.aggression;
    }

    /**
     * Get the share of the combined land strength of both sides this
     * player needs before it starts a war.
     *
     * @return A share from 0.4 (aggressive) to 0.6 (peaceful).
     */
    public double getWarStrengthShare() {
        return 0.6 - 0.2 * this.aggression;
    }

    /**
     * Get the factor to apply to the chance that a recent peace treaty
     * holds.  Aggressive players break treaties more readily.
     *
     * @return A factor from 0.7 to 1.3.
     */
    public double getPeaceFactor() {
        return 1.3 - 0.6 * this.aggression;
    }

    /**
     * Get the preference for a type of founding father.
     *
     * @param type The {@code FoundingFatherType} to weigh.
     * @return A factor from 0.5 to 1.5.
     */
    public double getFatherBias(FoundingFatherType type) {
        switch (type) {
        case MILITARY:
            return 0.5 + this.aggression;
        case TRADE:
            return 0.5 + this.mercantile;
        case EXPLORATION:
            return 0.5 + this.expansion;
        default:
            return 1.0;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("[aggression=%.2f mercantile=%.2f expansion=%.2f]",
                             this.aggression, this.mercantile, this.expansion);
    }
}
