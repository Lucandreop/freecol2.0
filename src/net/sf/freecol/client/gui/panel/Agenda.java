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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FreeColGameObject;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;


/**
 * The things about to happen to a player: colonists about to be born,
 * buildings about to be finished, founding fathers about to join, ships
 * about to arrive... and the problems in the way, such as a colony
 * about to starve or a build that has stalled.
 *
 * Seeing what is only a turn or two away is much of what makes a turn
 * based game hard to put down, so the agenda is shown on the map.
 */
public final class Agenda {

    /** Things further away than this many turns are left out. */
    public static final int HORIZON = 30;

    /** One thing on the agenda. */
    public static final class Item implements Comparable<Item> {

        /** The number of turns until it happens. */
        public final int turns;

        /** True if this is a problem to fix rather than an event. */
        public final boolean problem;

        /** What happens. */
        public final StringTemplate text;

        /** More about it, or null. */
        public final StringTemplate detail;

        /** The colony or unit it is about, or null. */
        public final FreeColGameObject subject;


        public Item(int turns, boolean problem, StringTemplate text,
                    StringTemplate detail, FreeColGameObject subject) {
            this.turns = turns;
            this.problem = problem;
            this.text = text;
            this.detail = detail;
            this.subject = subject;
        }

        /**
         * Problems come first, then the soonest events.
         *
         * @param other The other {@code Item}.
         * @return The comparison result.
         */
        @Override
        public int compareTo(Item other) {
            if (this.problem != other.problem) return (this.problem) ? -1 : 1;
            return Integer.compare(this.turns, other.turns);
        }
    }


    private Agenda() {} // Static only

    /**
     * Get the agenda of a player.
     *
     * @param player The {@code Player} to get the agenda for.
     * @return The things on the agenda, problems first, then the
     *     soonest events.
     */
    public static List<Item> getItems(Player player) {
        final List<Item> items = new ArrayList<>();
        for (Colony colony : player.getColonyList()) {
            addColonyItems(colony, items);
        }
        addFatherItem(player, items);
        addImmigrationItem(player, items);
        player.getUnits().forEach(u -> addUnitItems(u, items));
        Collections.sort(items);
        return items;
    }

    /**
     * Add an event if it is soon enough.
     *
     * @param items The items to add to.
     * @param turns The number of turns until it happens.
     * @param text What happens.
     * @param subject The colony or unit it is about, or null.
     */
    private static void addEvent(List<Item> items, int turns,
                                 StringTemplate text,
                                 FreeColGameObject subject) {
        if (turns > HORIZON) return;
        items.add(new Item(Math.max(1, turns), false, text, null, subject));
    }

    /**
     * Add what is about to happen in a colony.
     *
     * @param colony The {@code Colony} to check.
     * @param items The items to add to.
     */
    private static void addColonyItems(Colony colony, List<Item> items) {
        final String name = colony.getName();
        // Food: about to starve, or about to grow
        final int starve = colony.getStarvationTurns();
        if (starve >= 0) {
            items.add(new Item(starve, true,
                    StringTemplate.template("agenda.starve")
                        .addName("%colony%", name),
                    null, colony));
        } else {
            final int grow = colony.getNewColonistTurns();
            if (grow > 0) {
                addEvent(items, grow, StringTemplate.template("agenda.grow")
                    .addName("%colony%", name), colony);
            }
        }
        // The build: about to be done, or stalled
        final BuildableType build = colony.getCurrentlyBuilding();
        if (build != null) {
            final int turns = colony.getTurnsToComplete(build);
            if (turns >= 0) {
                addEvent(items, turns, StringTemplate.template("agenda.build")
                    .addNamed("%building%", build)
                    .addName("%colony%", name), colony);
            } else {
                items.add(new Item(0, true,
                        StringTemplate.template("agenda.buildStalled")
                            .addNamed("%building%", build)
                            .addName("%colony%", name),
                        ConstructionPanel.getBuildStatus(colony, build),
                        colony));
            }
        }
    }

    /**
     * Add when the next founding father will join the congress.
     *
     * @param player The {@code Player} to check.
     * @param items The items to add to.
     */
    private static void addFatherItem(Player player, List<Item> items) {
        final FoundingFather father = player.getCurrentFather();
        final int perTurn = player.getLibertyProductionNextTurn();
        if (father == null || perTurn <= 0) return;
        final int remaining = player.getRemainingFoundingFatherCost();
        addEvent(items, (remaining + perTurn - 1) / perTurn,
                 StringTemplate.template("agenda.father")
                     .addNamed("%father%", father), null);
    }

    /**
     * Add when the next immigrant will turn up on the docks in Europe.
     *
     * @param player The {@code Player} to check.
     * @param items The items to add to.
     */
    private static void addImmigrationItem(Player player, List<Item> items) {
        if (!player.isColonial() || player.getEurope() == null) return;
        final int perTurn = player.getTotalImmigrationProduction();
        if (perTurn <= 0) return;
        final int needed = player.getImmigrationRequired()
            - player.getImmigration();
        addEvent(items, (needed + perTurn - 1) / perTurn,
                 StringTemplate.key("agenda.immigrant"), null);
    }

    /**
     * Add what a unit is about to finish: a voyage, an improvement to
     * the land, or teaching a student.
     *
     * @param unit The {@code Unit} to check.
     * @param items The items to add to.
     */
    private static void addUnitItems(Unit unit, List<Item> items) {
        if (unit.isNaval() && unit.isAtSea() && unit.getWorkLeft() > 0) {
            addEvent(items, unit.getWorkLeft(), StringTemplate
                .template((unit.getDestination() instanceof Europe)
                    ? "agenda.sailEurope" : "agenda.sailAmerica")
                .addStringTemplate("%unit%", unit.getLabel()), unit);
        }
        if (unit.getState() == Unit.UnitState.IMPROVING
            && unit.getWorkImprovement() != null) {
            addEvent(items, unit.getWorkTurnsLeft(),
                StringTemplate.template("agenda.improve")
                    .addNamed("%improvement%",
                              unit.getWorkImprovement().getType()), unit);
        }
        final Unit student = unit.getStudent();
        if (student != null && unit.getColony() != null) {
            addEvent(items, unit.getNeededTurnsOfTraining()
                     - unit.getTurnsOfTraining(),
                StringTemplate.template("agenda.teach")
                    .addName("%colony%", unit.getColony().getName()),
                unit.getColony());
        }
    }
}
