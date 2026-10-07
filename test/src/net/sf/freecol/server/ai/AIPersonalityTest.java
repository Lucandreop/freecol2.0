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

package net.sf.freecol.server.ai;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.FoundingFatherType;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.server.model.ServerPlayer;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class AIPersonalityTest extends FreeColTestCase {

    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final TileType plainsType
        = spec().getTileType("model.tile.plains");
    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");


    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testPersonality() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plainsType));
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player spanish = game.getPlayerByNationId("model.nation.spanish");

        // Stable for a player in a game
        assertEquals(AIPersonality.create(dutch).toString(),
                     AIPersonality.create(dutch).toString());

        // Conquerors are more aggressive than traders
        AIPersonality dutchP = AIPersonality.create(dutch);
        AIPersonality spanishP = AIPersonality.create(spanish);
        assertTrue(spanishP + " vs " + dutchP,
            spanishP.getAggression() > dutchP.getAggression());
        assertTrue(dutchP.getMercantile() > spanishP.getMercantile());

        // Traits stay in range, and the father bias follows them
        for (AIPersonality p : List.of(dutchP, spanishP)) {
            assertTrue(p.getAggression() >= 0.0 && p.getAggression() <= 1.0);
            assertEquals(0.5 + p.getAggression(),
                p.getFatherBias(FoundingFatherType.MILITARY), 1e-9);
        }
    }

    public void testWarNeedsStrength() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plainsType));
        Map map = game.getMap();
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer french = getServerPlayer(game, "model.nation.french");
        EuropeanAIPlayer dutchAI = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        Player.makeContact(dutch, french);
        assertEquals(Stance.PEACE, dutch.getStance(french));
        dutch.setTension(french, new Tension(Tension.TENSION_MAX));

        // Hated, but far stronger: no war
        for (int i = 0; i < 4; i++) {
            new ServerUnit(game, map.getTile(10, 2 + i), french,
                           veteranType, dragoonRole);
        }
        assertEquals(Stance.PEACE, dutchAI.determinePersonalStance(french));

        // Hated and weaker: war
        for (int i = 0; i < 12; i++) {
            new ServerUnit(game, map.getTile(4, 2 + i), dutch,
                           veteranType, dragoonRole);
        }
        assertEquals(Stance.WAR, dutchAI.determinePersonalStance(french));

        // Little tension: peace whatever the strength
        dutch.setTension(french, new Tension(Tension.TENSION_MIN));
        assertEquals(Stance.PEACE, dutchAI.determinePersonalStance(french));
    }

    public void testFatherChoiceVaries() {
        Game game = ServerTestHelper.startServerGame(getTestMap(plainsType));
        AIMain aiMain = ServerTestHelper.getServer().getAIMain();
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        EuropeanAIPlayer dutchAI = (EuropeanAIPlayer)aiMain.getAIPlayer(dutch);

        // No custom house father offered, so the choice is random
        List<FoundingFather> offered = List.of(
            spec().getFoundingFather("model.foundingFather.thomasJefferson"),
            spec().getFoundingFather("model.foundingFather.williamPenn"),
            spec().getFoundingFather("model.foundingFather.hernandoDeSoto"),
            spec().getFoundingFather("model.foundingFather.paulRevere"));
        Set<FoundingFather> chosen = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            FoundingFather ff = dutchAI.selectFoundingFather(offered);
            assertTrue(offered.contains(ff));
            chosen.add(ff);
        }
        assertTrue("The choice should vary: " + chosen, chosen.size() > 1);
    }
}
