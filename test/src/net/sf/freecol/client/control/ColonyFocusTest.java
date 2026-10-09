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
import java.util.Iterator;
import java.util.List;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Tests the arrangement of colonists for a focus.
 */
public class ColonyFocusTest extends FreeColTestCase {

    private static final GoodsType bellsType
        = spec().getGoodsType("model.goods.bells");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType hammersType
        = spec().getGoodsType("model.goods.hammers");
    private static final TileType mixedForest
        = spec().getTileType("model.tile.mixedForest");


    /**
     * Make the plan happen as the client does: a colonist moves when
     * there is room for it.
     */
    private static void apply(Colony colony,
                              List<ColonyFocus.Assignment> plan) {
        final List<ColonyFocus.Assignment> todo = new ArrayList<>(plan);
        boolean progress = true;
        while (progress && !todo.isEmpty()) {
            progress = false;
            for (Iterator<ColonyFocus.Assignment> it = todo.iterator();
                 it.hasNext();) {
                final ColonyFocus.Assignment a = it.next();
                if (a.unit.getLocation() != a.workLocation) {
                    if (a.workLocation.isFull()) continue;
                    a.unit.setLocation(a.workLocation);
                }
                a.unit.changeWorkType(a.workType);
                it.remove();
                progress = true;
            }
        }
        assertTrue("Everyone placed", todo.isEmpty());
        colony.invalidateCache();
    }

    /**
     * A colony of plains with some forest, for lumber.
     */
    private Colony makeColony(int size) {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(size);
        int forests = 0;
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (!ct.isColonyCenterTile() && forests < 2) {
                ct.getWorkTile().setType(mixedForest);
                forests++;
            }
        }
        colony.invalidateCache();
        return colony;
    }

    public void testFood() {
        Colony colony = makeColony(4);
        apply(colony, ColonyFocus.plan(colony, ColonyFocus.Focus.FOOD));
        assertEquals("Nobody leaves", 4, colony.getUnitCount());
        assertTrue("Food to grow",
                   colony.getAdjustedNetProductionOf(foodType) > 0);
    }

    public void testBuild() {
        Colony colony = makeColony(4);
        apply(colony, ColonyFocus.plan(colony, ColonyFocus.Focus.BUILD));
        assertEquals("Nobody leaves", 4, colony.getUnitCount());
        assertTrue("Hammers",
                   colony.getAdjustedNetProductionOf(hammersType) > 0);
        assertTrue("Nobody starves",
                   colony.getAdjustedNetProductionOf(foodType) >= 0);
    }

    public void testLiberty() {
        Colony colony = makeColony(4);
        apply(colony, ColonyFocus.plan(colony, ColonyFocus.Focus.LIBERTY));
        assertEquals("Nobody leaves", 4, colony.getUnitCount());
        assertTrue("Bells",
                   colony.getAdjustedNetProductionOf(bellsType) > 0);
        assertTrue("Nobody starves",
                   colony.getAdjustedNetProductionOf(foodType) >= 0);
    }

    public void testBalanced() {
        Colony colony = makeColony(5);
        apply(colony, ColonyFocus.plan(colony, ColonyFocus.Focus.BALANCED));
        assertEquals("Nobody leaves", 5, colony.getUnitCount());
        assertTrue("Some food to spare",
                   colony.getAdjustedNetProductionOf(foodType) >= 0);
        assertTrue("Hammers",
                   colony.getAdjustedNetProductionOf(hammersType) > 0);
        assertTrue("Bells",
                   colony.getAdjustedNetProductionOf(bellsType) > 0);

        // Asking again changes nothing
        assertTrue("Settled", ColonyFocus.plan(colony,
                ColonyFocus.Focus.BALANCED).isEmpty());
    }
}
