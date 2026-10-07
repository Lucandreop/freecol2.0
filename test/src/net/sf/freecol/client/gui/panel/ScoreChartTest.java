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

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.util.List;

import net.sf.freecol.common.model.Game;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.HistoryEvent.HistoryEventType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Turn;
import net.sf.freecol.util.test.FreeColTestCase;


public class ScoreChartTest extends FreeColTestCase {

    private static void addEvent(Player player, int turn, int score) {
        HistoryEvent event = new HistoryEvent(new Turn(turn),
            HistoryEventType.FOUND_COLONY, player);
        event.setScore(score);
        player.addHistory(event);
    }

    public void testScorePoints() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        addEvent(dutch, 1, 0);
        addEvent(dutch, 10, 40);
        addEvent(dutch, 10, 55); // Last score of a turn wins
        addEvent(dutch, 30, 120);
        game.setTurn(new Turn(50));
        dutch.setScore(300);

        List<int[]> points = ScoreChart.getScorePoints(dutch);
        assertEquals(4, points.size());
        assertEquals(1, points.get(0)[0]);
        assertEquals(10, points.get(1)[0]);
        assertEquals(55, points.get(1)[1]);
        assertEquals(30, points.get(2)[0]);
        assertEquals(50, points.get(3)[0]);
        assertEquals(300, points.get(3)[1]);
    }

    public void testPaintsWithoutHistory() {
        Game game = getStandardGame();
        game.changeMap(getTestMap());
        Player dutch = game.getPlayerByNationId("model.nation.dutch");

        // Just the current turn, should still paint without failing
        ScoreChart chart = new ScoreChart(dutch, new Dimension(300, 100));
        assertEquals(1, ScoreChart.getScorePoints(dutch).size());
        chart.setSize(300, 100);
        BufferedImage image = new BufferedImage(300, 100,
                                                BufferedImage.TYPE_INT_ARGB);
        chart.paint(image.createGraphics());
    }
}
