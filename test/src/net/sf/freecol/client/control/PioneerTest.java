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

import java.util.HashSet;
import java.util.Set;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


/**
 * Tests which improvement a pioneer working on its own goes for.
 */
public class PioneerTest extends FreeColTestCase {

    private static final Role pioneerRole
        = spec().getRole("model.role.pioneer");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");


    public void testFindImprovement() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(2);
        final Tile home = colony.getTile();
        Unit pioneer = new ServerUnit(game, home, colony.getOwner(),
                                      colonistType, pioneerRole);

        final Set<Tile> taken = new HashSet<>();
        InGameController.PioneerJob job
            = InGameController.findImprovement(pioneer, taken);
        assertNotNull("Something to improve", job);
        assertEquals("On the colony lands", colony,
                     job.tile.getOwningSettlement());
        assertTrue("Allowed there", job.tile.isImprovementTypeAllowed(job.type));
        assertNull("Lands are not changed into another type",
                   job.type.getChange(job.tile.getType()));

        // Another pioneer already there: somewhere else
        taken.add(job.tile);
        InGameController.PioneerJob other
            = InGameController.findImprovement(pioneer, taken);
        if (other != null) {
            assertNotSame("Not where another pioneer works",
                          job.tile, other.tile);
        }

        // Without tools, nothing can be done
        pioneer.changeRole(spec().getDefaultRole(), 0);
        assertNull("No tools, no work",
                   InGameController.findImprovement(pioneer, new HashSet<>()));
    }
}
