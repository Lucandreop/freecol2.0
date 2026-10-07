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

import static net.sf.freecol.common.util.CollectionUtils.sum;

import java.util.Random;

import net.sf.freecol.common.model.AbstractUnit;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.FoundingFather.Dilemma;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.networking.ChangeSet;
import net.sf.freecol.server.ServerTestHelper;
import net.sf.freecol.util.test.FreeColTestCase;


public class FatherDilemmaTest extends FreeColTestCase {

    private static final FoundingFather adamSmith
        = spec().getFoundingFather("model.foundingFather.adamSmith");
    private static final FoundingFather francisDrake
        = spec().getFoundingFather("model.foundingFather.francisDrake");
    private static final FoundingFather henryHudson
        = spec().getFoundingFather("model.foundingFather.henryHudson");
    private static final FoundingFather magellan
        = spec().getFoundingFather("model.foundingFather.ferdinandMagellan");
    private static final FoundingFather peterMinuit
        = spec().getFoundingFather("model.foundingFather.peterMinuit");
    private static final FoundingFather thomasJefferson
        = spec().getFoundingFather("model.foundingFather.thomasJefferson");


    private static int refSize(Player player) {
        return sum(player.getMonarch().getExpeditionaryForce().getUnitList(),
                   AbstractUnit::getNumber);
    }

    public void testSpecificationDilemmas() {
        Dilemma dilemma = henryHudson.getDilemma();
        assertNotNull(dilemma);
        assertEquals(Dilemma.Cost.NATIVE_TENSION, dilemma.getCost());
        assertEquals(300, dilemma.getAmount());
        assertEquals(1, dilemma.getModifiers().size());
        // The extra features are attributed to the father
        assertEquals(henryHudson, dilemma.getModifiers().get(0).getSource());

        assertNull("Not every father has a proposal",
                   peterMinuit.getDilemma());
    }

    public void testCosts() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        ServerPlayer french
            = (ServerPlayer)game.getPlayerByNationId("model.nation.french");
        Random random = new Random(1);
        ChangeSet cs = new ChangeSet();

        // Gold: only affordable with enough gold, and only once
        dutch.setGold(100);
        assertFalse(dutch.canAffordDilemma(magellan));
        dutch.setGold(1000);
        assertTrue(dutch.canAffordDilemma(magellan));
        dutch.csAcceptDilemma(magellan, random, cs);
        assertEquals(500, dutch.getGold());
        assertTrue(dutch.hasAcceptedDilemma(magellan));
        assertFalse(dutch.canAffordDilemma(magellan));

        // Tax
        int tax = dutch.getTax();
        dutch.csAcceptDilemma(adamSmith, random, cs);
        assertEquals(tax + 4, dutch.getTax());

        // Royal force
        int ref = refSize(dutch);
        dutch.csAcceptDilemma(thomasJefferson, random, cs);
        assertTrue("REF should grow", refSize(dutch) > ref);

        // European tension
        int tension = french.getTension(dutch).getValue();
        dutch.csAcceptDilemma(francisDrake, random, cs);
        assertEquals(tension + 300, french.getTension(dutch).getValue());
    }

    public void testOffer() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        dutch.setAI(false);

        // A human player is asked, and nothing is paid until answering
        dutch.setGold(0);
        dutch.csAddFoundingFather(henryHudson, new Random(1), new ChangeSet());
        assertEquals(henryHudson, dutch.getPendingDilemma());
        assertFalse(dutch.hasAcceptedDilemma(henryHudson));

        // No offer when the cost can not be paid
        dutch.setPendingDilemma(null);
        dutch.csAddFoundingFather(magellan, new Random(1), new ChangeSet());
        assertNull(dutch.getPendingDilemma());
    }

    public void testFeaturesApply() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        final String furs = "model.goods.furs";

        dutch.addFather(henryHudson);
        assertEquals(1, count(dutch, furs));
        dutch.csAcceptDilemma(henryHudson, new Random(1), new ChangeSet());
        assertEquals(2, count(dutch, furs));
    }

    private static int count(Player player, String id) {
        return (int)player.getModifiers(id)
            .filter(m -> m.getSource() == henryHudson).count();
    }

    public void testSerialization() {
        Game game = ServerTestHelper.startServerGame(getTestMap());
        ServerPlayer dutch
            = (ServerPlayer)game.getPlayerByNationId("model.nation.dutch");
        dutch.addFather(henryHudson);
        dutch.csAcceptDilemma(henryHudson, new Random(1), new ChangeSet());

        // The accepted proposal, and its features, survive a copy
        Player copy = dutch.copy(game, Player.class);
        assertTrue(copy.hasAcceptedDilemma(henryHudson));
        assertEquals(2, count(copy, "model.goods.furs"));
    }
}
