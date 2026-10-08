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

package net.sf.freecol.client.gui.panel;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.util.test.FreeColTestCase;


public class AgendaTest extends FreeColTestCase {

    private static final BuildingType docksType
        = spec().getBuildingType("model.building.docks");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType hammerType
        = spec().getGoodsType("model.goods.hammers");


    private static Agenda.Item find(List<Agenda.Item> items, String id) {
        for (Agenda.Item item : items) {
            if (item.text.getId().equals(id)) return item;
        }
        return null;
    }

    public void testBuildAndOrder() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(1, 5, 8);
        Player player = colony.getOwner();
        colony.setCurrentlyBuilding(docksType);

        // With all the hammers in store the docks are done next turn
        colony.addGoods(hammerType, 500);
        colony.invalidateCache();
        Agenda.Item build = find(Agenda.getItems(player), "agenda.build");
        assertNotNull("The build is on the agenda", build);
        assertEquals(1, build.turns);
        assertEquals(colony, build.subject);
        assertFalse(build.problem);

        // Without hammers or anyone making them, the build has stalled
        colony.removeGoods(hammerType, colony.getGoodsCount(hammerType));
        colony.invalidateCache();
        if (colony.getAdjustedNetProductionOf(hammerType) <= 0) {
            Agenda.Item stalled = find(Agenda.getItems(player),
                                       "agenda.buildStalled");
            assertNotNull("A stalled build is a problem", stalled);
            assertTrue(stalled.problem);
            assertNotNull("It says why", stalled.detail);
        }

        // Problems come first, then the soonest events
        List<Agenda.Item> items = Agenda.getItems(player);
        for (int i = 1; i < items.size(); i++) {
            assertTrue(items.get(i - 1).compareTo(items.get(i)) <= 0);
        }
    }

    public void testStarving() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(1, 5, 8);
        Player player = colony.getOwner();

        colony.removeGoods(foodType, colony.getGoodsCount(foodType));
        colony.invalidateCache();
        if (colony.getAdjustedNetProductionOf(foodType) < 0) {
            Agenda.Item starve = find(Agenda.getItems(player),
                                      "agenda.starve");
            assertNotNull(starve);
            assertTrue(starve.problem);
            assertEquals("Problems come first", starve,
                         Agenda.getItems(player).get(0));
        }
    }

    public void testEveryTextExists() throws Exception {
        for (String file : new String[] {
                "data/strings/FreeColMessages.properties",
                "data/strings/FreeColMessages_pt_BR.properties" }) {
            Properties p = new Properties();
            try (Reader r = new InputStreamReader(
                    Files.newInputStream(Paths.get(file)),
                    StandardCharsets.UTF_8)) {
                p.load(r);
            }
            for (String key : new String[] {
                    "agenda.title", "agenda.fold", "agenda.empty",
                    "agenda.inTurns", "agenda.starve", "agenda.grow",
                    "agenda.build", "agenda.buildStalled", "agenda.father",
                    "agenda.immigrant", "agenda.sailEurope",
                    "agenda.sailAmerica", "agenda.improve", "agenda.teach",
                    "model.option.guiShowAgenda.name",
                    "model.option.guiShowAgenda.shortDescription" }) {
                assertNotNull(file + " " + key, p.getProperty(key));
            }
        }
    }
}
