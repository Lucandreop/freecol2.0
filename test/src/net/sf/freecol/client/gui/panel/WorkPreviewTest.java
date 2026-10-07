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

import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.ColonyTile;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.model.UnitType;
import net.sf.freecol.server.model.ServerUnit;
import net.sf.freecol.util.test.FreeColTestCase;


public class WorkPreviewTest extends FreeColTestCase {

    private static final BuildingType carpenterType
        = spec().getBuildingType("model.building.carpenterHouse");
    private static final GoodsType lumberType
        = spec().getGoodsType("model.goods.lumber");
    private static final UnitType colonistType
        = spec().getUnitType("model.unit.freeColonist");


    public void testPreview() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(2, 5, 8);
        Unit unit = new ServerUnit(game, colony.getTile(), colony.getOwner(),
                                   colonistType);

        // A field tile: the colonist would produce something there
        ColonyTile field = null;
        for (ColonyTile ct : colony.getColonyTiles()) {
            if (!ct.isColonyCenterTile() && ct.getUnitCount() == 0) {
                field = ct;
                break;
            }
        }
        assertNotNull(field);
        ColonyPanel.WorkPreview p = ColonyPanel.getWorkPreview(field, unit);
        assertNotNull("A colonist can work a free field", p);
        assertEquals("colonyPanel.preview.produce", p.main.getId());
        assertFalse(p.kind == ColonyPanel.WorkPreview.Kind.BAD);

        // The carpenter needs lumber, which this colony lacks
        Building carpenter = colony.getBuilding(carpenterType);
        colony.removeGoods(lumberType, colony.getGoodsCount(lumberType));
        p = ColonyPanel.getWorkPreview(carpenter, unit);
        if (colony.getNetProductionOf(lumberType) <= 0) {
            assertNotNull(p);
            assertEquals(ColonyPanel.WorkPreview.Kind.WARNING, p.kind);
            assertEquals("colonyPanel.preview.missing", p.note.getId());
        }

        // With lumber in store it is a plain production
        colony.addGoods(lumberType, 50);
        p = ColonyPanel.getWorkPreview(carpenter, unit);
        assertNotNull(p);
        assertEquals("colonyPanel.preview.produce", p.main.getId());
        assertNull(p.note);

        // Nothing to say about places with no room for workers at all
        assertNull(ColonyPanel.getWorkPreview(
            colony.getColonyTile(colony.getTile()), unit));
        assertNull(ColonyPanel.getWorkPreview(colony.getBuilding(
            spec().getBuildingType("model.building.chapel")), unit));
    }
}
