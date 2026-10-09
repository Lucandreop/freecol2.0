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

import java.awt.event.ActionEvent;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.panel.GuidePanel;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Unit;


/**
 * An action to send the active ship to Europe, the quickest way.
 */
public class SailToEuropeAction extends UnitAction {

    public static final String id = "sailToEuropeAction";


    /**
     * Creates this action.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public SailToEuropeAction(FreeColClient freeColClient) {
        super(freeColClient, id);

        addImageIcons("europe");
    }


    // Override FreeColAction

    /**
     * {@inheritDoc}
     */
    @Override
    protected boolean shouldBeEnabled() {
        if (!super.shouldBeEnabled()) return false;
        final Unit unit = getGUI().getActiveUnit();
        final Player player = unit.getOwner();
        return unit.isNaval() && unit.hasTile()
            && unit.getType().canMoveToHighSeas()
            && player.getEurope() != null && player.canMoveToEurope();
    }


    // Interface ActionListener

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent ae) {
        GuidePanel.mark("sail");
        igc().sailToEurope(getGUI().getActiveUnit());
    }
}
