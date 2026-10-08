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

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import net.sf.freecol.common.model.Objective;
import net.sf.freecol.client.control.Advisor.Tip;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Stance;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class AdvisorTest extends FreeColTestCase {

    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");


    public void testTips() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Set<String> seen = new HashSet<>();

        // Welcome first, then nothing until something happens
        assertEquals(Tip.WELCOME, Advisor.nextTip(dutch, seen));
        seen.add(Tip.WELCOME.getKey());
        assertNull(Advisor.nextTip(dutch, seen));

        // A colony brings its tip, which is not repeated
        createStandardColony(1, 5, 8);
        assertEquals(Tip.FIRST_COLONY, Advisor.nextTip(dutch, seen));
        seen.add(Tip.FIRST_COLONY.getKey());
        assertNotSame(Tip.FIRST_COLONY, Advisor.nextTip(dutch, seen));
    }

    public void testObjectives() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        assertFalse(Objective.FOUND_COLONY.isComplete(dutch));
        createStandardColony(2, 5, 8);
        assertTrue(Objective.FOUND_COLONY.isComplete(dutch));
        assertEquals(1, Objective.THREE_COLONIES.getProgress(dutch));
        assertFalse(Objective.THREE_COLONIES.isComplete(dutch));
        assertEquals(2, Objective.TEN_COLONISTS.getProgress(dutch));
        assertFalse(Objective.INDEPENDENCE.isComplete(dutch));
    }

    public void testWarnings() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Map map = game.getMap();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");
        Colony colony = createStandardColony(1, 5, 8);
        assertTrue(Advisor.findWarnings(dutch).isEmpty());

        // Enemy troops next to an undefended colony
        Player.makeContact(dutch, french);
        dutch.setStance(french, Stance.WAR);
        french.setStance(dutch, Stance.WAR);
        new ServerUnit(game, map.getTile(6, 8), french, veteranType,
                       dragoonRole);
        dutch.invalidateCanSeeTiles();
        assertTrue(Advisor.findWarnings(dutch)
            .containsKey("undefended." + colony.getId()));

        // Colonists stuck on the docks
        new ServerUnit(game, dutch.getEurope(), dutch, colonistType);
        new ServerUnit(game, dutch.getEurope(), dutch, colonistType);
        assertTrue(Advisor.findWarnings(dutch).containsKey("docks"));
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
            for (Tip tip : Tip.values()) {
                assertNotNull(file + " " + tip, p.getProperty(tip.getKey()));
            }
            for (Objective o : Objective.values()) {
                assertNotNull(file + " " + o,
                              p.getProperty(o.getKey() + ".name"));
                assertNotNull(file + " " + o,
                              p.getProperty(o.getKey() + ".hint"));
            }
            for (String key : new String[] {
                    "advisor.warning.undefended", "advisor.warning.tories",
                    "advisor.warning.docks", "advisor.warning.independence",
                    "tutorial.objective.done", "rebelToolTip.explanation" }) {
                assertNotNull(file + " " + key, p.getProperty(key));
            }
        }
    }
}
