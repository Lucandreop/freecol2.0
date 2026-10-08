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

package net.sf.freecol.common.model;

import static net.sf.freecol.common.util.CollectionUtils.count;
import static net.sf.freecol.common.util.CollectionUtils.max;
import static net.sf.freecol.common.util.CollectionUtils.sum;
import static net.sf.freecol.common.util.StringUtils.getEnumKey;

import java.util.function.ToIntFunction;


/**
 * The first objectives of the game, a path from a few colonists to an
 * independent nation.  They are meant to be reached in this order, and
 * each one earns a reward, given by the server at the start of the
 * turn after it is reached.
 */
public enum Objective {
    FOUND_COLONY(Player::getSettlementCount, 1, 100, null),
    MEET_NATIVES(p -> (int)count(p.getGame().getLiveNativePlayers(),
                                 n -> n.hasContacted(p)), 1, 100, null),
    SELL_IN_EUROPE(Objective::countGoodsSold, 1, 200, null),
    THREE_COLONIES(Player::getSettlementCount, 3, 0,
                   "model.unit.freeColonist"),
    FIRST_FATHER(Player::getFatherCount, 1, 300, null),
    TEN_COLONISTS(p -> sum(p.getColonyList(), Colony::getUnitCount), 10, 0,
                  "model.unit.elderStatesman"),
    HALF_REBELS(p -> (p.getColonyList().isEmpty()) ? 0
                : max(p.getColonyList(), Colony::getSonsOfLiberty), 50, 0,
                "model.unit.veteranSoldier"),
    INDEPENDENCE(p -> (p.isColonial()) ? 0 : 1, 1, 0, null);

    /** How to measure the progress of a player. */
    private final ToIntFunction<Player> progress;

    /** The progress needed. */
    private final int target;

    /** The gold given as a reward. */
    private final int gold;

    /** The identifier of the unit type given as a reward, or null. */
    private final String unitTypeId;


    Objective(ToIntFunction<Player> progress, int target, int gold,
              String unitTypeId) {
        this.progress = progress;
        this.target = target;
        this.gold = gold;
        this.unitTypeId = unitTypeId;
    }

    /**
     * Count the kinds of goods a player has sold in Europe.
     *
     * @param player The {@code Player} to check.
     * @return The number of goods types with sales.
     */
    private static int countGoodsSold(Player player) {
        final Market market = player.getMarket();
        if (market == null) return 0;
        int n = 0;
        for (GoodsType type : player.getSpecification().getGoodsTypeList()) {
            if (market.getSales(type) > 0) n++;
        }
        return n;
    }

    /**
     * Get the progress of a player towards this objective.
     *
     * @param player The {@code Player} to check.
     * @return The progress, from zero up to the target.
     */
    public int getProgress(Player player) {
        return Math.max(0, Math.min(this.target,
                                    this.progress.applyAsInt(player)));
    }

    public int getTarget() {
        return this.target;
    }

    public boolean isComplete(Player player) {
        return getProgress(player) >= this.target;
    }

    public int getGold() {
        return this.gold;
    }

    /**
     * Get the type of unit given as a reward.
     *
     * @param spec The {@code Specification} to look the type up in.
     * @return The {@code UnitType}, or null if no unit is given.
     */
    public UnitType getUnitType(Specification spec) {
        return (this.unitTypeId == null) ? null
            : spec.getUnitType(this.unitTypeId);
    }

    /**
     * Describe the reward for this objective.
     *
     * @param spec The {@code Specification} to look unit types up in.
     * @return A template describing the reward.
     */
    public StringTemplate getReward(Specification spec) {
        final UnitType unitType = getUnitType(spec);
        return (this.gold > 0)
            ? StringTemplate.template("objective.reward.gold")
                .addAmount("%amount%", this.gold)
            : (unitType != null)
            ? StringTemplate.template("objective.reward.unit")
                .addNamed("%unit%", unitType)
            : StringTemplate.key("objective.reward.glory");
    }

    public String getKey() {
        return "tutorial.objective." + getEnumKey(this);
    }

    /**
     * Get the next objective for a player to reach.
     *
     * @param player The {@code Player} to check.
     * @return The first objective not yet complete, or null if all are.
     */
    public static Objective getCurrent(Player player) {
        for (Objective o : values()) {
            if (!o.isComplete(player)) return o;
        }
        return null;
    }
}
