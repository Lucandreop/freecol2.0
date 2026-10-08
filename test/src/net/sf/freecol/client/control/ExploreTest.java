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

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.LostCityRumour;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Tests where an exploring unit heads for.
 */
public class ExploreTest extends FreeColTestCase {

    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType merchantmanType
        = spec().getUnitType("model.unit.merchantman");


    public void testLandExplorer() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plains, true);
        game.changeMap(map);
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        // Land is x < 10, ocean beyond
        Unit colonist = new ServerUnit(game, map.getTile(6, 8), dutch,
                                       colonistType);
        assertNull("Nothing to explore",
            InGameController.findExploreTarget(colonist));

        final Tile unknown = map.getTile(2, 8);
        unknown.setExplored(dutch, false);
        Tile target = InGameController.findExploreTarget(colonist);
        assertNotNull("Goes to see the unknown tile", target);
        assertTrue("Close enough to see it",
                   target.getDistanceTo(unknown) <= colonist.getLineOfSight());

        // Rumours are left alone
        target.addLostCityRumour(new LostCityRumour(game, target));
        final Tile rumour = target;
        assertTrue("Rumour placed", rumour.hasLostCityRumour());
        target = InGameController.findExploreTarget(colonist);
        assertNotNull("Another way to see the unknown tile", target);
        assertNotSame("Not onto the rumour", rumour, target);
        assertTrue("Still close enough to see it",
                   target.getDistanceTo(unknown) <= colonist.getLineOfSight());
    }

    public void testNavalExplorer() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plains, true);
        game.changeMap(map);
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        Unit ship = new ServerUnit(game, map.getTile(12, 8), dutch,
                                   merchantmanType);
        final Tile unknown = map.getTile(16, 3);
        unknown.setExplored(dutch, false);
        final Tile target = InGameController.findExploreTarget(ship);
        assertNotNull("Sails to see the unknown sea", target);
        assertFalse("Stays on the water", target.isLand());
        assertTrue("Close enough to see it",
                   target.getDistanceTo(unknown) <= ship.getLineOfSight());
    }
}
