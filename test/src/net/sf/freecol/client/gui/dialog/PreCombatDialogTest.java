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

package net.sf.freecol.client.gui.dialog;

import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.CombatModel;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.Map;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Role;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.TileType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class PreCombatDialogTest extends FreeColTestCase {

    private static final Role dragoonRole
        = spec().getRole("model.role.dragoon");
    private static final TileType plains
        = spec().getTileType("model.tile.plains");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");
    private static final UnitType veteranType
        = spec().getUnitType("model.unit.veteranSoldier");


    public void testWinChanceMatchesCombatOdds() {
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.changeMap(map);
        CombatModel combatModel = game.getCombatModel();
        Player dutch = game.getPlayerByNationId("model.nation.dutch");
        Player french = game.getPlayerByNationId("model.nation.french");

        Tile tile1 = map.getTile(5, 8);
        Tile tile2 = map.getTile(4, 8);
        Unit colonist = new ServerUnit(game, tile1, dutch, colonistType);
        Unit dragoon = new ServerUnit(game, tile2, french, veteranType,
                                      dragoonRole);
        dragoon.setMovesLeft(dragoon.getInitialMovesLeft());

        // The percentage is the rounded odds the attack roll uses
        double odds = combatModel.calculateCombatOdds(dragoon, colonist).win;
        int percent = PreCombatDialog.getWinChancePercent(combatModel,
            dragoon, colonist);
        assertEquals((int)Math.round(odds * 100.0), percent);
        assertTrue("A veteran dragoon should be favoured: " + percent,
            percent > 50 && percent < 100);
    }

    public void testWinChanceAgainstSettlementIsHidden() {
        Game game = getStandardGame();
        Map map = getTestMap(plains);
        game.changeMap(map);
        CombatModel combatModel = game.getCombatModel();
        Player french = game.getPlayerByNationId("model.nation.french");

        Colony colony = createStandardColony(2, 5, 8);
        Unit dragoon = new ServerUnit(game, map.getTile(4, 8), french,
                                      veteranType, dragoonRole);
        dragoon.setMovesLeft(dragoon.getInitialMovesLeft());

        assertEquals(-1, PreCombatDialog.getWinChancePercent(combatModel,
                dragoon, colony));
    }
}
