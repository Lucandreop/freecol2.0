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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import net.sf.freecol.client.gui.panel.TownPlan.District;
import net.sf.freecol.client.gui.panel.TownPlan.Plot;
import net.sf.freecol.common.model.Ability;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.util.test.FreeColTestCase;


public class TownPlanTest extends FreeColTestCase {

    private static BuildingType type(String id) {
        return spec().getBuildingType("model.building." + id);
    }

    /**
     * Make a site for every kind of building the colony panel shows,
     * at about the size they have at scale 1.5.
     */
    private static List<Plot> makePlots() {
        final List<Plot> plots = new ArrayList<>();
        int index = 0;
        for (BuildingType bt : spec().getBuildingTypeList()) {
            if (bt.getUpgradesFrom() != null || bt.isDefenceType()
                || bt.hasAbility(Ability.PRODUCE_IN_WATER)) continue;
            final Dimension size = (bt == type("townHall"))
                ? new Dimension(275, 192)
                : new Dimension(150 + 10 * (index % 5), 135);
            plots.add(new Plot(null, bt, size, false, index++));
        }
        return plots;
    }

    public void testDistricts() {
        assertEquals(District.HALL, TownPlan.getDistrict(type("townHall")));
        assertEquals(District.CHURCH, TownPlan.getDistrict(type("chapel")));
        assertEquals(District.SCHOOL,
                     TownPlan.getDistrict(type("schoolhouse")));
        assertEquals(District.CRAFT,
                     TownPlan.getDistrict(type("carpenterHouse")));
        assertEquals(District.OTHER, TownPlan.getDistrict(type("depot")));
    }

    public void testWorkedGoods() {
        final GoodsType[] goods
            = TownPainter.getWorkedGoods(type("carpenterHouse"));
        assertNotNull(goods);
        assertEquals(spec().getGoodsType("model.goods.lumber"), goods[0]);
        assertEquals(spec().getGoodsType("model.goods.hammers"), goods[1]);
        assertNull(TownPainter.getWorkedGoods(type("depot")));
    }

    public void testLayout() {
        final Dimension size = new Dimension(1200, 730);
        final Dimension banner = new Dimension(314, 114);
        final List<Plot> plots = makePlots();
        final TownPlan plan = TownPlan.create(size, banner, plots, 18, 33);
        assertNotNull("The town fits", plan);

        final Rectangle area = new Rectangle(size);
        final Rectangle reserved = new Rectangle(
            (size.width - banner.width) / 2, 0, banner.width, banner.height);
        Plot hall = null;
        for (Plot p : plan.getPlots()) {
            assertNotNull(p.type.getSuffix() + " placed", p.bounds);
            assertTrue(p.type.getSuffix() + " inside",
                       area.contains(p.bounds));
            assertFalse(p.type.getSuffix() + " clear of the banner",
                        p.bounds.intersects(reserved));
            assertFalse(p.type.getSuffix() + " clear of the square",
                        p.bounds.intersects(plan.getSquare()));
            for (Plot o : plan.getPlots()) {
                if (o != p) {
                    assertFalse(p.type.getSuffix() + " overlaps "
                        + o.type.getSuffix(), p.bounds.intersects(o.bounds));
                }
            }
            // Every building stands on a street
            boolean onStreet = false;
            for (Rectangle s : plan.getStreets()) {
                if (s.y == p.bounds.y + p.bounds.height) onStreet = true;
            }
            assertTrue(p.type.getSuffix() + " on a street", onStreet);
            if (p.getDistrict() == District.HALL) hall = p;
        }

        // The town hall heads the square, in the middle of the town
        assertNotNull(hall);
        assertEquals(size.width / 2,
                     hall.bounds.x + hall.bounds.width / 2, 1);
        assertTrue(plan.getSquare().y >= hall.bounds.y + hall.bounds.height);
        assertTrue(plan.getRoad() >= 18 && plan.getRoad() <= 33);
    }

    public void testNoRoom() {
        assertNull("A small area is left to the fallback layout",
                   TownPlan.create(new Dimension(600, 400), null,
                                   makePlots(), 18, 33));
    }
}
