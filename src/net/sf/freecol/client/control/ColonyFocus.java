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

package net.sf.freecol.client.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitLocation.NoAddReason;
import net.sf.freecol.common.model.WorkLocation;


/**
 * Works out where the colonists of a colony should work, for a focus
 * the player picks, so that the player does not have to place them
 * one by one.
 *
 * The plan is made on a scratch copy of the colony: colonists are
 * placed one at a time where they make the most of what the focus
 * wants, while the colony keeps enough food.  Teachers stay with
 * their students.
 */
public final class ColonyFocus {

    /** What a colony should make the most of. */
    public enum Focus {
        /** Enough food to grow, a builder, a statesman, then skills. */
        BALANCED,
        /** As much food as possible, to grow. */
        FOOD,
        /** As many hammers as possible, to build. */
        BUILD,
        /** As many bells as possible, for liberty. */
        LIBERTY;

        /**
         * Get the message key for the name of this focus.
         *
         * @return The key.
         */
        public String getKey() {
            return "colonyPanel.focus." + name().toLowerCase();
        }
    }

    /** Where one colonist should work, and at what. */
    public static final class Assignment {

        /** The colonist. */
        public final Unit unit;
        /** Where it should work. */
        public final WorkLocation workLocation;
        /** What it should make. */
        public final GoodsType workType;

        Assignment(Unit unit, WorkLocation workLocation,
                   GoodsType workType) {
            this.unit = unit;
            this.workLocation = workLocation;
            this.workType = workType;
        }
    }

    /** A colonist placed in the scratch colony. */
    private static final class Placement {

        final Unit unit;
        final WorkLocation workLocation;
        final GoodsType type;
        final int amount;

        Placement(Unit unit, WorkLocation workLocation, GoodsType type,
                  int amount) {
            this.unit = unit;
            this.workLocation = workLocation;
            this.type = type;
            this.amount = amount;
        }

        void apply() {
            unit.setLocation(workLocation);
            unit.changeWorkType(type);
        }
    }

    /** How many turns of stock make a raw material plentiful enough. */
    private static final int STOCK_TURNS = 4;

    private ColonyFocus() {} // Static only

    /**
     * Plan where the colonists of a colony should work.
     *
     * @param colony The {@code Colony} to plan for.
     * @param focus The {@code Focus} to plan with.
     * @return The changes to make, colonist by colonist.  Colonists
     *     already where they should be are left out.
     */
    public static List<Assignment> plan(Colony colony, Focus focus) {
        final Specification spec = colony.getSpecification();
        final GoodsType food = spec.getPrimaryFoodType();
        final Colony scratch = colony.copyColony();
        final Tile tile = scratch.getTile();

        // Everyone but the teachers stands outside, ready to be placed
        final List<Unit> workers = new ArrayList<>();
        for (Unit u : scratch.getUnitList()) {
            if (!u.getLocation().equals(tile)
                && u.getWorkLocation() != null
                && u.getWorkLocation().canTeach()) continue;
            workers.add(u);
        }
        final List<Unit> placed = new ArrayList<>(workers);
        // Where everyone was, so that between equals nobody moves
        final Map<Unit, WorkLocation> origin = new HashMap<>();
        for (Unit u : workers) origin.put(u, u.getWorkLocation());
        for (Unit u : workers) u.setLocation(tile);

        final GoodsType hammers = find(spec, "model.goods.hammers");
        final GoodsType bells = find(spec, "model.goods.bells");
        final List<GoodsType> goals = new ArrayList<>();
        int foodTarget = 0;
        switch (focus) {
        case FOOD:
            foodTarget = Integer.MAX_VALUE;
            break;
        case BUILD:
            for (int i = 0; i < 3; i++) goals.add(hammers);
            break;
        case LIBERTY:
            for (int i = 0; i < 3; i++) goals.add(bells);
            break;
        default:
            foodTarget = 2; // Enough to grow, slowly
            goals.add(hammers);
            goals.add(bells);
            break;
        }
        goals.removeIf(g -> g == null);

        while (!workers.isEmpty()) {
            Placement p = null;
            // The goals of the focus first, then each to its skill,
            // then wherever they make the most
            while (p == null && !goals.isEmpty()) {
                p = placeFor(scratch, origin, workers, goals.get(0));
                if (p == null) {
                    goals.remove(0);
                } else if (p.type == goals.get(0)) {
                    goals.remove(0);
                } // else made the raw material first, try again later
            }
            if (p == null && foodTarget == Integer.MAX_VALUE) {
                p = bestFood(scratch, origin, workers);
                if (p == null) foodTarget = 2; // No more food to be had
            }
            if (p == null) p = bySkill(scratch, origin, workers, food);
            if (p == null) p = mostOfAnything(scratch, origin, workers, food);
            if (p == null) break; // Nowhere left to work
            p.apply();

            // Never let the colony go hungry for it
            if (!p.type.isFoodType()
                && scratch.getAdjustedNetProductionOf(food)
                    < Math.min(foodTarget, 2)) {
                p.unit.setLocation(tile);
                final Placement f = bestFood(scratch, origin, workers);
                if (f != null) {
                    if (goalOf(p, hammers, bells)) {
                        goals.add(0, p.type); // Try again later
                    }
                    p = f;
                }
                p.apply();
            }
            workers.remove(p.unit);
        }

        // What has changed?
        final List<Assignment> ret = new ArrayList<>();
        for (Unit u : placed) {
            final WorkLocation wl = u.getWorkLocation();
            if (wl == null) continue; // Left where it was
            final Unit real = colony.getCorresponding(u);
            final WorkLocation realWl = colony.getCorresponding(wl);
            if (real == null || realWl == null) continue;
            if (real.getLocation() == realWl
                && real.getWorkType() == u.getWorkType()) continue;
            ret.add(new Assignment(real, realWl, u.getWorkType()));
        }
        return ret;
    }

    private static boolean goalOf(Placement p, GoodsType hammers,
                                  GoodsType bells) {
        return p.type == hammers || p.type == bells;
    }

    private static GoodsType find(Specification spec, String id) {
        for (GoodsType g : spec.getGoodsTypeList()) {
            if (g.getId().equals(id)) return g;
        }
        return null;
    }

    /**
     * Find a place to make some goods, making the raw material for
     * it first if there is not enough of it.
     *
     * @param colony The scratch {@code Colony}.
     * @param origin Where each colonist worked before.
     * @param workers The colonists still to place.
     * @param type The {@code GoodsType} to make.
     * @return A placement, or null if the goods can not be made.
     */
    private static Placement placeFor(Colony colony,
            Map<Unit, WorkLocation> origin,
            List<Unit> workers,
                                      GoodsType type) {
        final Placement p = best(colony, origin, workers, type);
        if (p == null || hasInput(colony, p)) return p;
        // Make the raw material first
        return best(colony, origin, workers, type.getInputType());
    }

    /**
     * Is there enough raw material for a placement to work?
     *
     * @param colony The scratch {@code Colony}.
     * @param p The {@code Placement} to check.
     * @return True if the raw material is there, or none is needed.
     */
    private static boolean hasInput(Colony colony, Placement p) {
        final GoodsType raw = p.type.getInputType();
        if (raw == null) return true;
        final int made = colony.getAdjustedNetProductionOf(raw);
        return made >= p.amount
            || colony.getGoodsCount(raw) + made * STOCK_TURNS
                >= p.amount * STOCK_TURNS;
    }

    /**
     * Place a colonist at what it is an expert in, if it is wanted.
     *
     * @param colony The scratch {@code Colony}.
     * @param origin Where each colonist worked before.
     * @param workers The colonists still to place.
     * @param food The food {@code GoodsType}.
     * @return A placement, or null if no expert can work at its skill.
     */
    private static Placement bySkill(Colony colony,
            Map<Unit, WorkLocation> origin,
            List<Unit> workers,
                                     GoodsType food) {
        for (Unit u : workers) {
            final GoodsType skill = u.getType().getExpertProduction();
            if (skill == null) continue;
            final Placement p = best(colony, origin, List.of(u), skill);
            if (p != null && (p.type.isFoodType() || hasInput(colony, p))) {
                return p;
            }
        }
        return null;
    }

    /**
     * Place a colonist wherever it makes the most of anything, so long
     * as there is room to store it and raw material to make it from.
     *
     * @param colony The scratch {@code Colony}.
     * @param origin Where each colonist worked before.
     * @param workers The colonists still to place.
     * @param food The food {@code GoodsType}.
     * @return A placement, or null if there is nowhere left to work.
     */
    private static Placement mostOfAnything(Colony colony,
            Map<Unit, WorkLocation> origin,
            List<Unit> workers,
                                            GoodsType food) {
        final int room = colony.getWarehouseCapacity();
        Placement best = null;
        for (WorkLocation wl : workLocations(colony)) {
            for (ProductionType pt : wl.getAvailableProductionTypes(false)) {
                for (AbstractGoods out : pt.getOutputList()) {
                    final GoodsType type = out.getType();
                    if (type.isStorable() && !type.isFoodType()
                        && colony.getGoodsCount(type) >= room) continue;
                    for (Unit u : workers) {
                        if (wl.getNoAddReason(u) != NoAddReason.NONE) continue;
                        final int amount
                            = wl.getPotentialProduction(type, u.getType());
                        if (amount <= 0) continue;
                        final Placement p = new Placement(u, wl, type, amount);
                        if (!hasInput(colony, p)) continue;
                        if (best == null || amount > best.amount
                            || (amount == best.amount && origin.get(u) == wl
                                && origin.get(best.unit) != best.workLocation)) {
                            best = p;
                        }
                    }
                }
            }
        }
        return best;
    }

    /**
     * Find the best colonist and place to make food of any kind.
     *
     * @param colony The scratch {@code Colony}.
     * @param origin Where each colonist worked before.
     * @param workers The colonists to choose from.
     * @return The best placement, or null if no food can be made.
     */
    private static Placement bestFood(Colony colony,
            Map<Unit, WorkLocation> origin,
            List<Unit> workers) {
        Placement best = null;
        for (GoodsType type : colony.getSpecification()
                 .getFoodGoodsTypeList()) {
            final Placement p = best(colony, origin, workers, type);
            if (p != null && (best == null || p.amount > best.amount)) {
                best = p;
            }
        }
        return best;
    }

    /**
     * Find the best colonist and place to make some goods.
     *
     * More is better.  Between equals, an expert at the goods comes
     * first, then a colonist with no skill to spare the other experts
     * for their own work.
     *
     * @param colony The scratch {@code Colony}.
     * @param origin Where each colonist worked before.
     * @param workers The colonists to choose from.
     * @param type The {@code GoodsType} to make.
     * @return The best placement, or null if the goods can not be made.
     */
    private static Placement best(Colony colony,
            Map<Unit, WorkLocation> origin,
            List<Unit> workers,
                                  GoodsType type) {
        if (type == null) return null;
        Placement best = null;
        for (WorkLocation wl : workLocations(colony)) {
            for (Unit u : workers) {
                if (wl.getNoAddReason(u) != NoAddReason.NONE) continue;
                final int amount = wl.getPotentialProduction(type, u.getType());
                if (amount <= 0) continue;
                if (best == null || amount > best.amount
                    || (amount == best.amount
                        && (rank(u, type) > rank(best.unit, type)
                            || (rank(u, type) == rank(best.unit, type)
                                && origin.get(u) == wl
                                && origin.get(best.unit) != best.workLocation)))) {
                    best = new Placement(u, wl, type, amount);
                }
            }
        }
        return best;
    }

    /**
     * How well a colonist suits some work, between equal producers.
     *
     * @param u The {@code Unit}.
     * @param type The {@code GoodsType} to make.
     * @return Higher for better suited.
     */
    private static int rank(Unit u, GoodsType type) {
        final GoodsType skill = u.getType().getExpertProduction();
        return (skill == type) ? 2 : (skill == null) ? 1 : 0;
    }

    /**
     * Get the places in a colony that can be worked.
     *
     * @param colony The scratch {@code Colony}.
     * @return The work locations that are the colony's and can be worked.
     */
    private static List<WorkLocation> workLocations(Colony colony) {
        final List<WorkLocation> ret = new ArrayList<>();
        for (WorkLocation wl : colony.getCurrentWorkLocationsList()) {
            if (wl.canBeWorked() && !wl.canTeach()) ret.add(wl);
        }
        return ret;
    }
}
