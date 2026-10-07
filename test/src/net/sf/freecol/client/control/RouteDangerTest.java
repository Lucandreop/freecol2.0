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
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class RouteDangerTest extends FreeColTestCase {

    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final UnitType frigateType
        = spec().getUnitType("model.unit.frigate");
    private static final UnitType merchantmanType
        = spec().getUnitType("model.unit.merchantman");
    private static final UnitType privateerType
        = spec().getUnitType("model.unit.privateer");
    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");


    public void testThreats() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plains, true);
        game.changeMap(map);
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        // Land is x < 10, ocean beyond
        Tile shipTile = map.getTile(10, 7);
        Unit merchantman = new ServerUnit(game, shipTile, dutch,
                                          merchantmanType);
        dutch.invalidateCanSeeTiles();
        assertNull("Nobody around",
            InGameController.findThreatNear(merchantman, shipTile, 2));

        // A land unit at war can not attack a ship
        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);
        Unit dragoon = new ServerUnit(game, map.getTile(9, 7), french,
                                      veteranType, dragoonRole);
        dutch.invalidateCanSeeTiles();
        assertNull("Dragoon is no threat to a ship",
            InGameController.findThreatNear(merchantman, shipTile, 2));

        // A warship is a threat at war, but not in peace
        Unit frigate = new ServerUnit(game, map.getTile(11, 7), french,
                                      frigateType);
        dutch.invalidateCanSeeTiles();
        assertEquals("Frigate at war", frigate,
            InGameController.findThreatNear(merchantman, shipTile, 2));
        dutch.setStance(french, Stance.PEACE);
        french.setStance(dutch, Stance.PEACE);
        assertNull("Frigate at peace",
            InGameController.findThreatNear(merchantman, shipTile, 2));

        // Privateers attack anyone, even in peace
        Unit privateer = new ServerUnit(game, map.getTile(11, 8), french,
                                        privateerType);
        dutch.invalidateCanSeeTiles();
        assertEquals("Privateer", privateer,
            InGameController.findThreatNear(merchantman, shipTile, 2));

        // ...but only when close enough
        assertNull("Out of range",
            InGameController.findThreatNear(merchantman, map.getTile(15, 2), 2));

        // Our own warships are never a threat
        frigate.changeOwner(dutch);
        privateer.changeOwner(dutch);
        dragoon.changeOwner(dutch);
        dutch.invalidateCanSeeTiles();
        assertNull("Own units",
            InGameController.findThreatNear(merchantman, shipTile, 2));
    }
}
