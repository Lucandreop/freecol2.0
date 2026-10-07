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

import net.sf.freecol.common.model.BuildingType;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.util.test.FreeColTestCase;


public class ColonyStatusTest extends FreeColTestCase {

    private static final BuildingType docksType
        = spec().getBuildingType("model.building.docks");
    private static final GoodsType foodType
        = spec().getPrimaryFoodType();
    private static final GoodsType hammerType
        = spec().getGoodsType("model.goods.hammers");


    public void testFoodStatus() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(1, 5, 8);

        colony.invalidateCache();
        final int net = colony.getAdjustedNetProductionOf(foodType);
        final StringTemplate t = ColonyPanel.getFoodStatus(colony);
        if (net > 0) {
            assertEquals("colonyPanel.food.grow", t.getId());
        } else if (net == 0) {
            assertEquals("colonyPanel.food.still", t.getId());
        }

        // With no food in store and more mouths than food, it starves now
        colony.removeGoods(foodType, colony.getGoodsCount(foodType));
        if (colony.getAdjustedNetProductionOf(foodType) < 0) {
            assertEquals("colonyPanel.food.starving",
                         ColonyPanel.getFoodStatus(colony).getId());
        }
    }

    public void testBuildStatus() {
        Game game = getStandardGame();
        game.changeMap(getTestMap(true));
        Colony colony = createStandardColony(1, 5, 8);

        // Nobody makes hammers in a fresh one-man colony
        colony.removeGoods(hammerType, colony.getGoodsCount(hammerType));
        colony.invalidateCache();
        StringTemplate t = ConstructionPanel.getBuildStatus(colony, docksType);
        if (colony.getAdjustedNetProductionOf(hammerType) <= 0) {
            assertTrue("Says why the docks are not getting built: " + t.getId(),
                t.getId().equals("constructionPanel.status.nobody")
                || t.getId().equals("constructionPanel.status.noInput"));
        }

        // With all the hammers in store, it is done next turn
        colony.addGoods(hammerType, 500);
        colony.invalidateCache();
        t = ConstructionPanel.getBuildStatus(colony, docksType);
        assertEquals("constructionPanel.status.nextTurn", t.getId());
    }
}
