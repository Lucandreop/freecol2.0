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

package net.sf.freecol.server.model;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Objective;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;


public class ObjectiveRewardTest extends FreeColTestCase {

    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testProgressNeverNegative() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        // With no colonies there is no rebel percentage to report
        assertEquals(0, Objective.HALF_REBELS.getProgress(dutch));
        assertEquals(Objective.FOUND_COLONY, Objective.getCurrent(dutch));
    }

    public void testRewards() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");

        // Nothing reached, nothing given
        int gold = dutch.getGold();
        dutch.csRewardObjectives(new ChangeSet());
        assertEquals(gold, dutch.getGold());

        // The first colony earns its gold once only
        createStandardColony(1, 5, 8);
        dutch.csRewardObjectives(new ChangeSet());
        assertTrue(dutch.hasObjectiveReward(Objective.FOUND_COLONY));
        assertEquals(gold + Objective.FOUND_COLONY.getGold(),
                     dutch.getGold());
        dutch.csRewardObjectives(new ChangeSet());
        assertEquals(gold + Objective.FOUND_COLONY.getGold(),
                     dutch.getGold());

        // Three colonies earn a colonist on the docks in Europe
        createStandardColony(1, 10, 8);
        createStandardColony(1, 15, 8);
        final int docks = dutch.getEurope().getUnitCount();
        dutch.csRewardObjectives(new ChangeSet());
        assertTrue(dutch.hasObjectiveReward(Objective.THREE_COLONIES));
        assertEquals(docks + 1, dutch.getEurope().getUnitCount());
    }
}
