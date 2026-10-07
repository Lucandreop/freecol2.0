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

import java.util.List;
import java.util.Random;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.IndianSettlement;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.Tension;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TrustLevel;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;
import net.sf.freecol.util.test.MockPseudoRandom;


public class NativeTrustTest extends FreeColTestCase {

    @Override
    public void tearDown() throws Exception {
        ServerTestHelper.stopServerGame();
        super.tearDown();
    }


    public void testLevels() {
        assertEquals(TrustLevel.NONE, TrustLevel.fromValue(0));
        assertEquals(TrustLevel.NONE, TrustLevel.fromValue(24));
        assertEquals(TrustLevel.PARTNER, TrustLevel.fromValue(25));
        assertEquals(TrustLevel.ALLY, TrustLevel.fromValue(60));
        assertEquals(TrustLevel.INTEGRATION, TrustLevel.fromValue(100));
        assertTrue(TrustLevel.ALLY.isAtLeast(TrustLevel.PARTNER));
        assertFalse(TrustLevel.PARTNER.isAtLeast(TrustLevel.ALLY));
    }

    public void testTrustAndBenefits() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));
        Map map = game.getMap();
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");
        IndianSettlement is = new FreeColTestCase.IndianSettlementBuilder(game)
            .player(tupi).settlementTile(map.getTile(5, 5))
            .skillToTeach(null).build();
        Player.makeContact(tupi, dutch);
        Random random = new Random(1);

        // Clamped to 0..100
        dutch.csModifyNativeTrust(tupi, -10, random, new ChangeSet());
        assertEquals(0, dutch.getNativeTrust(tupi));
        dutch.csModifyNativeTrust(tupi, 500, random, new ChangeSet());
        assertEquals(TrustLevel.MAXIMUM, dutch.getNativeTrust(tupi));
        dutch.setNativeTrust(tupi, 0);

        // Partners pay half for land
        Tile land = map.getTile(5, 6);
        assertEquals(tupi, land.getOwner());
        int price = dutch.getLandPrice(land);
        dutch.csModifyNativeTrust(tupi, TrustLevel.PARTNER.getThreshold(),
                                  random, new ChangeSet());
        assertEquals(TrustLevel.PARTNER, dutch.getNativeTrustLevel(tupi));
        assertTrue(dutch.getLandPrice(land) < price);

        // Allies teach again (the clamping test above already made them
        // allies once, so forget the skill they learned then)
        is.setLearnableSkill(null);
        dutch.csModifyNativeTrust(tupi, TrustLevel.ALLY.getThreshold(),
                                  random, new ChangeSet());
        assertEquals(TrustLevel.ALLY, dutch.getNativeTrustLevel(tupi));
        assertNotNull("Allied settlement should teach again",
                      is.getLearnableSkill());

        // Trust survives copying the player
        Player copy = dutch.copy(game, Player.class);
        assertEquals(dutch.getNativeTrust(tupi), copy.getNativeTrust(tupi));
    }

    public void testTurnUpdate() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");
        Player.makeContact(tupi, dutch);

        // Happy natives trust a little more every turn
        tupi.setTension(dutch, new Tension(Tension.TENSION_MIN));
        dutch.csUpdateNativeTrust(new Random(1), new ChangeSet());
        assertEquals(ServerPlayer.TRUST_PEACEFUL_TURN,
                     dutch.getNativeTrust(tupi));

        // Angry ones less, and war destroys it
        dutch.setNativeTrust(tupi, 50);
        tupi.setTension(dutch, new Tension(Tension.Level.ANGRY.getLimit()));
        dutch.csUpdateNativeTrust(new Random(1), new ChangeSet());
        assertTrue(dutch.getNativeTrust(tupi) < 50);
        tupi.setStance(dutch, Stance.WAR);
        dutch.setStance(tupi, Stance.WAR);
        dutch.csUpdateNativeTrust(new Random(1), new ChangeSet());
        assertEquals(0, dutch.getNativeTrust(tupi));
    }

    public void testIntegration() {
        Game game = ServerTestHelper.startServerGame(getTestMap(true));
        Map map = game.getMap();
        ServerPlayer dutch = getServerPlayer(game, "model.nation.dutch");
        ServerPlayer tupi = getServerPlayer(game, "model.nation.tupi");
        Colony colony = createStandardColony(1, 5, 8);
        IndianSettlement big = new FreeColTestCase.IndianSettlementBuilder(game)
            .player(tupi).settlementTile(map.getTile(5, 5))
            .initialBravesInCamp(4).capital(true).build();
        IndianSettlement small = new FreeColTestCase.IndianSettlementBuilder(game)
            .player(tupi).settlementTile(map.getTile(8, 8))
            .initialBravesInCamp(2).build();
        Player.makeContact(tupi, dutch);
        tupi.setTension(dutch, new Tension(Tension.TENSION_MIN));
        dutch.setNativeTrust(tupi, TrustLevel.MAXIMUM);
        Tile tile = colony.getTile();
        int units = tile.getUnitCount();

        // With the dice always rolling 0, a volunteer arrives and the
        // small settlement joins (the capital never does)
        int bigUnits = big.getUnitCount() + small.getUnitCount();
        dutch.csUpdateNativeTrust(new MockPseudoRandom(List.of(0), true),
                                  new ChangeSet());
        assertTrue("Small settlement should have joined", small.isDisposed());
        assertFalse(big.isDisposed());
        assertTrue("Colonists should arrive: " + tile.getUnitCount(),
                   tile.getUnitCount() >= units + 2);
        assertTrue(bigUnits > big.getUnitCount());
        for (int i = 0; i < tile.getUnitCount(); i++) {
            assertEquals(dutch, tile.getUnitList().get(i).getOwner());
        }
    }
}
