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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.ProductionType;


/**
 * Lays the buildings of a colony out as a small town.
 *
 * The buildings stand in rows, each row along a street that runs
 * across the town.  An avenue runs down the middle of the town: it
 * enters under the colony name at the top, leads to the town hall in
 * the second row and continues past the town square in front of the
 * hall and out of town.  The other buildings are grouped by what they
 * do, with the church, schools and public buildings in the row of the
 * town hall and the workshops in the rows above and below.
 */
public final class TownPlan {

    /** What part of town a building belongs in. */
    public enum District {
        HALL, CHURCH, SCHOOL, OTHER, CRAFT
    }

    /** A building site to place in the town. */
    public static final class Plot {

        /** The component showing the site, if any. */
        public final Component component;

        /** The type of building on the site. */
        public final BuildingType type;

        /** The size of the site. */
        public final Dimension size;

        /** True if nothing has been built on the site yet. */
        public final boolean empty;

        /** Where the site was placed. */
        public Rectangle bounds = null;

        /** The district of the site. */
        private final District district;

        /** The position of the site in the original list. */
        private final int index;


        /**
         * Create a new building site.
         *
         * @param component The component showing the site.
         * @param type The type of building on the site.
         * @param size The size of the site.
         * @param empty True if nothing has been built on the site yet.
         * @param index The position of the site in the original list.
         */
        public Plot(Component component, BuildingType type, Dimension size,
                    boolean empty, int index) {
            this.component = component;
            this.type = type;
            this.size = size;
            this.empty = empty;
            this.district = TownPlan.getDistrict(type);
            this.index = index;
        }

        public District getDistrict() {
            return this.district;
        }
    }

    /** One row of buildings, standing on a street. */
    private static final class Row {

        /** The sites left of the middle, innermost first. */
        final List<Plot> left = new ArrayList<>();

        /** The sites right of the middle, innermost first. */
        final List<Plot> right = new ArrayList<>();

        /** The width of the middle of the row, kept free of sites. */
        int middle;

        /** The least height of the row. */
        int minHeight;

        int width(List<Plot> side, int gap) {
            int w = 0;
            for (Plot p : side) w += p.size.width + gap;
            return w;
        }

        boolean isEmpty() {
            return left.isEmpty() && right.isEmpty();
        }
    }

    /** The sites, placed. */
    private final List<Plot> plots;

    /** The streets running across the town. */
    private final List<Rectangle> streets = new ArrayList<>();

    /** The parts of the avenue running down the middle of the town. */
    private final List<Rectangle> avenue = new ArrayList<>();

    /** The town square. */
    private Rectangle square = null;

    /** The area at the top center kept free, if any. */
    private Rectangle reserved = null;

    /** The width of the streets. */
    private final int road;


    private TownPlan(List<Plot> plots, int road) {
        this.plots = plots;
        this.road = road;
    }

    public List<Plot> getPlots() {
        return this.plots;
    }

    public List<Rectangle> getStreets() {
        return this.streets;
    }

    public List<Rectangle> getAvenue() {
        return this.avenue;
    }

    public Rectangle getSquare() {
        return this.square;
    }

    public Rectangle getReserved() {
        return this.reserved;
    }

    public int getRoad() {
        return this.road;
    }

    /**
     * Decide what part of town a building type belongs in.
     *
     * @param type The {@code BuildingType} to check.
     * @return The {@code District} for the type.
     */
    public static District getDistrict(BuildingType type) {
        if (type == null) return District.OTHER;
        if (type.hasAbility(Ability.TEACH)) return District.SCHOOL;
        boolean converts = false;
        for (boolean unattended : new boolean[] { false, true }) {
            for (ProductionType pt
                     : type.getAvailableProductionTypes(unattended)) {
                for (AbstractGoods ag : pt.getOutputList()) {
                    if (ag.getType().isLibertyType()) return District.HALL;
                    if (ag.getType().isImmigrationType()) {
                        return District.CHURCH;
                    }
                }
                if (!unattended && !pt.getInputList().isEmpty()
                    && !pt.getOutputList().isEmpty()) converts = true;
            }
        }
        return (converts) ? District.CRAFT : District.OTHER;
    }

    /**
     * Lay out a town.
     *
     * @param size The size of the area to lay the town out in.
     * @param reservedTop The size of an area at the top center to keep
     *     free, or null for none.
     * @param plots The building sites to place.
     * @param minRoad The least width of a street.
     * @param maxRoad The greatest width of a street.
     * @return The town plan, or null if the sites do not fit.
     */
    public static TownPlan create(Dimension size, Dimension reservedTop,
                                  List<Plot> plots, int minRoad,
                                  int maxRoad) {
        if (plots.isEmpty() || size.width <= 0 || size.height <= 0) {
            return null;
        }
        for (int rows = 3; rows <= 5; rows++) {
            TownPlan plan = tryRows(size, reservedTop, plots, minRoad,
                                    maxRoad, rows);
            if (plan != null) return plan;
        }
        return null;
    }

    /**
     * Try to lay out a town with a given number of rows.
     *
     * @param size The size of the area to lay the town out in.
     * @param reservedTop The size of an area at the top center to keep
     *     free, or null for none.
     * @param plots The building sites to place.
     * @param minRoad The least width of a street.
     * @param maxRoad The greatest width of a street.
     * @param n The number of rows, at least three.
     * @return The town plan, or null if the sites do not fit.
     */
    private static TownPlan tryRows(Dimension size, Dimension reservedTop,
                                    List<Plot> plots, int minRoad,
                                    int maxRoad, int n) {
        final int width = size.width, height = size.height;
        final int gap = minRoad / 2;

        Plot hall = null;
        final List<Plot> others = new ArrayList<>(plots.size());
        for (Plot p : plots) {
            if (hall == null && p.getDistrict() == District.HALL) {
                hall = p;
            } else {
                others.add(p);
            }
        }
        Collections.sort(others, Comparator
            .comparing(Plot::getDistrict)
            .thenComparingInt(p -> p.index));

        // The middle of the rows: the avenue entering under the
        // colony name, the town hall, the square, and the avenue
        // leaving town.
        final int hallWidth = (hall == null) ? 6 * minRoad
            : hall.size.width;
        final int squareWidth = Math.max(4 * maxRoad, hallWidth * 3 / 4);
        final Row[] rows = new Row[n];
        for (int r = 0; r < n; r++) rows[r] = new Row();
        rows[0].middle = maxRoad + 2 * gap;
        if (reservedTop != null) {
            rows[0].middle = Math.max(rows[0].middle, reservedTop.width);
            rows[0].minHeight = reservedTop.height;
        }
        rows[1].middle = hallWidth + 2 * gap;
        rows[1].minHeight = (hall == null) ? 0 : hall.size.height;
        rows[2].middle = squareWidth + 2 * gap;
        rows[2].minHeight = 2 * maxRoad;
        for (int r = 3; r < n; r++) rows[r].middle = maxRoad + 2 * gap;

        // Fill the rows, starting with the row of the town hall so
        // the church and public buildings stand next to it, and
        // ending with the top row, beside the colony name.  Keep both
        // sides of each row about as wide as each other.
        final int[] order = new int[n];
        for (int r = 0; r < n - 1; r++) order[r] = r + 1;
        order[n - 1] = 0;
        int k = 0;
        for (Plot p : others) {
            boolean placed = false;
            while (k < n && !placed) {
                final Row row = rows[order[k]];
                final int room = (width - row.middle) / 2;
                final int lw = row.width(row.left, gap);
                final int rw = row.width(row.right, gap);
                final int need = p.size.width + gap;
                final boolean leftFits = lw + need <= room;
                final boolean rightFits = rw + need <= room;
                if (rightFits && (rw <= lw || !leftFits)) {
                    row.right.add(p);
                    placed = true;
                } else if (leftFits) {
                    row.left.add(p);
                    placed = true;
                } else {
                    k++;
                }
            }
            if (!placed) return null;
        }

        // The height of each row is that of its tallest building.
        int used = n;
        while (used > 3 && rows[used-1].isEmpty()) used--;
        final int[] rowHeight = new int[used];
        int total = 0;
        for (int r = 0; r < used; r++) {
            int h = rows[r].minHeight;
            for (Plot p : rows[r].left) h = Math.max(h, p.size.height);
            for (Plot p : rows[r].right) h = Math.max(h, p.size.height);
            rowHeight[r] = h;
            total += h;
        }
        if (total + used * minRoad > height) return null;

        // Make the streets as wide as there is room for, share out
        // the rest of the height between the rows, and lay the
        // streets under them.
        final int road = Math.min(maxRoad, (height - total) / used);
        final int extra = (height - total - used * road) / (used + 1);
        final TownPlan plan = new TownPlan(plots, road);
        final int cx = width / 2;
        final int[] base = new int[used];
        int y = extra;
        for (int r = 0; r < used; r++) {
            base[r] = y + rowHeight[r];
            plan.streets.add(new Rectangle(0, base[r], width, road));
            y = base[r] + road + extra;
        }

        for (int r = 0; r < used; r++) {
            final Row row = rows[r];
            final int sideWidth = cx - row.middle / 2;
            placeSide(row.left, 0, sideWidth, base[r], true);
            placeSide(row.right, cx + row.middle / 2, sideWidth, base[r],
                      false);
        }
        if (hall != null) {
            hall.bounds = new Rectangle(cx - hall.size.width / 2,
                                        base[1] - hall.size.height,
                                        hall.size.width, hall.size.height);
        }

        // The avenue enters town under the colony name, and leaves it
        // past the square, which lies in front of the town hall.
        plan.avenue.add(new Rectangle(cx - road / 2, 0, road, base[0]));
        if (reservedTop != null) {
            plan.reserved = new Rectangle(cx - reservedTop.width / 2, 0,
                reservedTop.width, reservedTop.height);
        }
        plan.square = new Rectangle(cx - squareWidth / 2, base[1] + road,
                                    squareWidth, base[2] - base[1] - road);
        plan.avenue.add(new Rectangle(cx - road / 2, base[2],
                                      road, height - base[2]));
        return plan;
    }

    /**
     * Place the sites on one side of a row, spread out evenly.
     *
     * @param side The sites, innermost first.
     * @param x The left edge of the side.
     * @param w The width of the side.
     * @param base The y coordinate the buildings stand on.
     * @param left True if this is the left side of the row.
     */
    private static void placeSide(List<Plot> side, int x, int w, int base,
                                  boolean left) {
        if (side.isEmpty()) return;
        int used = 0;
        for (Plot p : side) used += p.size.width;
        final int space = (w - used) / (side.size() + 1);
        final List<Plot> ordered = new ArrayList<>(side);
        if (left) Collections.reverse(ordered); // Outermost first
        int px = x + space;
        for (Plot p : ordered) {
            p.bounds = new Rectangle(px, base - p.size.height,
                                     p.size.width, p.size.height);
            px += p.size.width + space;
        }
    }
}
