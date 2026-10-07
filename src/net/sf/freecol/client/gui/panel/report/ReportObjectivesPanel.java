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

package net.sf.freecol.client.gui.panel.report;

import java.awt.Color;
import java.awt.Font;

import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.control.Advisor.Objective;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;


/**
 * This panel lists the first objectives of the game, with the
 * player's progress and a hint on how to reach each one.
 */
public final class ReportObjectivesPanel extends ReportPanel {

    /**
     * Create the objectives panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportObjectivesPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportObjectivesAction");

        final Player player = getMyPlayer();
        final Font bold = FontLibrary.getScaledFont("normal-bold-smaller");
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 2, fillx", "[]20[fill]",
                                            ""));
        reportPanel.add(Utility.localizedTextArea("tutorial.objectives.intro",
                                                  60), "span, wrap 15");
        for (Objective o : Objective.values()) {
            final boolean done = o.isComplete(player);
            JLabel status = Utility.localizedLabel((done)
                ? StringTemplate.key("tutorial.objective.complete")
                : StringTemplate.template("tutorial.objective.progress")
                    .addAmount("%progress%", o.getProgress(player))
                    .addAmount("%target%", o.getTarget()));
            status.setForeground((done) ? new Color(0, 110, 0)
                : Color.DARK_GRAY);
            reportPanel.add(status, "top");
            JLabel name = Utility.localizedLabel(o.getKey() + ".name");
            name.setFont(bold);
            reportPanel.add(name);
            reportPanel.add(Utility.localizedTextArea(o.getKey() + ".hint",
                                                      50), "skip, wrap 10");
        }
    }
}
