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

package net.sf.freecol.client.gui.mapviewer;

import java.awt.Color;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonySiteLensTest extends FreeColTestCase {

    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");


    private static boolean isGreenish(Color c) {
        return c.getGreen() > c.getRed();
    }

    public void testColors() {
        Game game = getStandardGame();
        Map map = getCoastTestMap(plainsType, true);
        game.changeMap(map);
        map.resetHighSeasCount();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        ColonySiteLens lens = new ColonySiteLens();

        // Water is never tinted
        assertNull("Ocean", lens.getColor(dutch, map.getTile(15, 7)));

        // The coast is a good site, inland is worse
        Color coast = lens.getColor(dutch, map.getTile(9, 7));
        Color inland = lens.getColor(dutch, map.getTile(1, 7));
        assertNotNull("Coast", coast);
        assertNotNull("Inland", inland);
        assertTrue("Coast should be green: " + coast, isGreenish(coast));
        assertTrue("Coast should beat inland: " + coast + " vs " + inland,
            coast.getGreen() - coast.getRed()
            > inland.getGreen() - inland.getRed());

        // Next to a new colony nothing can be built, which must be
        // noticed although the turn did not change
        Colony colony = createStandardColony(1, 5, 8);
        assertEquals(dutch, colony.getOwner());
        Color adjacent = lens.getColor(dutch, map.getTile(5, 7));
        assertNotNull("Adjacent", adjacent);
        assertFalse("Next to a colony should not be green: " + adjacent,
            isGreenish(adjacent));
    }
}
