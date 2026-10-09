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

import java.util.List;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Tests what a route to Europe takes from a colony to sell.
 */
public class EuropeRouteTest extends FreeColTestCase {

    private static final GoodsType furType
        = spec().getGoodsType("model.goods.furs");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final TileType mixedForest
        = spec().getTileType("model.tile.mixedForest");


    public void testExports() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(3);

        // One colonist traps furs, another cuts lumber
        ColonyTile furs = null, lumber = null;
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (ct.isColonyCenterTile()) continue;
            if (furs == null) {
                ct.getWorkTile().setType(mixedForest);
                furs = ct;
            } else if (lumber == null) {
                ct.getWorkTile().setType(mixedForest);
                lumber = ct;
            }
        }
        final List<Unit> units = colony.getUnitList();
        units.get(0).setLocation(furs);
        units.get(0).changeWorkType(furType);
        units.get(1).setLocation(lumber);
        units.get(1).changeWorkType(lumberType);
        colony.invalidateCache();

        final List<GoodsType> exports = InGameController.getExports(colony);
        assertTrue("Furs to sell", exports.contains(furType));
        assertFalse("Lumber is for building", exports.contains(lumberType));
        for (GoodsType type : exports) {
            assertFalse("No food", type.isFoodType());
            assertTrue("Made to spare", colony.getNetProductionOf(type) > 0);
        }
    }
}
