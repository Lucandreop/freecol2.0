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

package net.sf.freecol.client.gui.action;

import static net.sf.freecol.common.util.CollectionUtils.any;

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.model.Unit;


/**
 * An action to set the active pioneer to improve the colony lands on
 * its own.
 */
public class AutoImproveAction extends UnitAction {

    public static final String id = "autoImproveAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public AutoImproveAction(FreeColClient freeColClient) {
        super(freeColClient, id);

        addImageIcons("automate");
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) return false;
        final Unit unit = getGUI().getActiveUnit();
        return unit.hasTile() && !unit.isOnCarrier()
            && any(unit.getSpecification().getTileImprovementTypeList(),
                   it -> !it.isNatural() && it.isWorkerAllowed(unit));
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        igc().autoImprove(getGUI().getActiveUnit());
    }
}
