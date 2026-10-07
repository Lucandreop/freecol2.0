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

import static net.sf.freecol.common.util.CollectionUtils.sum;

import java.awt.Dimension;
import java.awt.Image;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.client.gui.panel.MigPanel;
import net.sf.freecol.client.gui.panel.ScoreChart;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Turn;


/**
 * This dialog is displayed to a player who has won the game.
 *
 * Besides the victory picture it sums up the game: a few statistics,
 * a chart of the score over time and the milestones of the player's
 * history.
 */
public final class VictoryDialog extends FreeColConfirmDialog {

    /**
     * Create a Victory dialog.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param frame The owner frame.
     */
    public VictoryDialog(FreeColClient freeColClient, JFrame frame) {
        super(freeColClient, frame);

        final ImageLibrary lib = freeColClient.getGUI().getFixedImageLibrary();
        final float scale = lib.getScaleFactor();
        final Player player = freeColClient.getMyPlayer();

        JPanel panel = new MigPanel(new MigLayout("wrap 2", "[center]20[left]",
                                                  ""));
        panel.add(Utility.localizedHeader(Messages.message("victory.text"),
                                          Utility.FONTSPEC_TITLE),
                  "span 2, align center, wrap 10");

        // The picture, shrunk to leave room for the summary
        Image image = lib.getScaledImage("image.flavor.Victory");
        int height = (int)(200 * scale);
        if (image.getHeight(null) > height) {
            int width = image.getWidth(null) * height / image.getHeight(null);
            image = image.getScaledInstance(width, height, Image.SCALE_SMOOTH);
        }
        panel.add(new JLabel(new ImageIcon(image)));
        panel.add(createStatisticsPanel(player), "top");

        // Score over time and the milestones, side by side to keep the
        // dialog short enough for small screens
        final Dimension boxSize = new Dimension((int)(330 * scale),
                                                (int)(150 * scale));
        panel.add(Utility.localizedHeaderLabel("victory.scoreChart",
                      JLabel.LEADING, Utility.FONTSPEC_SUBTITLE),
                  "align left, gaptop 10");
        panel.add(Utility.localizedHeaderLabel("victory.timeline",
                      JLabel.LEADING, Utility.FONTSPEC_SUBTITLE),
                  "align left, gaptop 10");
        ScoreChart chart = new ScoreChart(player, boxSize);
        chart.setFont(FontLibrary.getScaledFont("normal-plain-tiny"));
        panel.add(chart, "top");

        JPanel timeline = new MigPanel(new MigLayout("wrap 2, insets 0",
                                                     "[]10[fill]", ""));
        timeline.setOpaque(false);
        for (HistoryEvent event : player.getHistory()) {
            timeline.add(Utility.localizedLabel(event.getTurn().getLabel()),
                         "top");
            timeline.add(Utility.localizedTextArea(event, 24));
        }
        JScrollPane scroll = new JScrollPane(timeline,
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setPreferredSize(boxSize);
        panel.add(scroll, "top");

        initializeConfirmDialog(frame, false, panel, null,
                                "victory.yes", "victory.continue");
    }

    /**
     * Create a panel summing up how the player fared.
     *
     * @param player The {@code Player} who won.
     * @return The statistics panel.
     */
    private static JPanel createStatisticsPanel(Player player) {
        final List<Colony> colonies = player.getColonyList();
        final int firstTurn = 1;
        final int lastTurn = player.getGame().getTurn().getNumber();
        JPanel stats = new MigPanel(new MigLayout("wrap 1, insets 0", "", ""));
        stats.setOpaque(false);
        stats.add(Utility.localizedLabel(StringTemplate
                .template("victory.years")
                .addAmount("%from%", Turn.getTurnYear(firstTurn))
                .addAmount("%to%", Turn.getTurnYear(lastTurn))));
        stats.add(Utility.localizedLabel(StringTemplate
                .template("victory.colonies")
                .addAmount("%amount%", colonies.size())));
        stats.add(Utility.localizedLabel(StringTemplate
                .template("victory.colonists")
                .addAmount("%amount%", sum(colonies, Colony::getUnitCount))));
        stats.add(Utility.localizedLabel(StringTemplate
                .template("victory.fathers")
                .addAmount("%amount%", player.getFatherCount())));
        stats.add(Utility.localizedLabel(StringTemplate
                .template("victory.gold")
                .addAmount("%amount%", player.getGold())));
        JLabel score = Utility.localizedLabel(StringTemplate
                .template("victory.score")
                .addAmount("%amount%", player.getScore()));
        score.setFont(FontLibrary.getScaledFont("normal-bold-smaller"));
        stats.add(score, "gaptop 6");
        return stats;
    }
}
