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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.JComponent;
import javax.swing.JLabel;

import net.miginfocom.swing.MigLayout;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.panel.Utility;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Objective;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Specification;
import net.sf.freecol.common.model.StringTemplate;


/**
 * This panel shows the first objectives of the game as a path to
 * follow: those reached, with their rewards, the next one, with a hint
 * on how to reach it, and those still to come.
 */
public final class ReportObjectivesPanel extends ReportPanel {

    /** How an objective stands. */
    private enum State { DONE, CURRENT, LATER }

    private static final Color DONE_COLOUR = new Color(40, 110, 40);
    private static final Color CURRENT_COLOUR = new Color(176, 124, 20);
    private static final Color LATER_COLOUR = new Color(130, 120, 105);


    /**
     * Create the objectives panel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public ReportObjectivesPanel(FreeColClient freeColClient) {
        super(freeColClient, "reportObjectivesAction");

        final Player player = getMyPlayer();
        final Specification spec = getSpecification();
        final Objective current = Objective.getCurrent(player);
        final Font bold = FontLibrary.getScaledFont("normal-bold-smaller");
        final Font plain = FontLibrary.getScaledFont("normal-plain-smaller");
        reportPanel.removeAll();
        reportPanel.setLayout(new MigLayout("wrap 3, fillx",
                                            "[]15[grow, fill]15[right]", ""));
        reportPanel.add(Utility.localizedTextArea("tutorial.objectives.intro",
                                                  60), "span, wrap 10");
        int done = 0;
        for (Objective o : Objective.values()) {
            if (o.isComplete(player)) done++;
        }
        final JLabel header = Utility.localizedLabel(StringTemplate
            .template("reportObjectivesPanel.header")
            .addAmount("%done%", done)
            .addAmount("%total%", Objective.values().length));
        header.setFont(bold);
        reportPanel.add(header, "span, wrap 15");

        int step = 1;
        for (Objective o : Objective.values()) {
            final State state = (o.isComplete(player)) ? State.DONE
                : (o == current) ? State.CURRENT : State.LATER;
            final Color colour = (state == State.DONE) ? DONE_COLOUR
                : (state == State.CURRENT) ? CURRENT_COLOUR : LATER_COLOUR;
            reportPanel.add(new Badge(state, step++, colour, bold), "top");

            final JLabel name = Utility.localizedLabel(o.getKey() + ".name");
            name.setFont(bold);
            name.setForeground((state == State.LATER) ? LATER_COLOUR
                               : Color.BLACK);
            reportPanel.add(name);

            final JLabel progress = new JLabel((state == State.DONE)
                ? Messages.message("tutorial.objective.complete")
                : o.getProgress(player) + " / " + o.getTarget());
            progress.setFont(plain);
            progress.setForeground(colour);
            reportPanel.add(progress, "top");

            // Only the next step needs the hint on how to get there
            if (state == State.CURRENT) {
                reportPanel.add(Utility.localizedTextArea(o.getKey()
                        + ".hint", 50), "skip, span 2");
            }
            final JLabel reward = Utility.localizedLabel(StringTemplate
                .template((state == State.DONE)
                    ? "reportObjectivesPanel.rewarded"
                    : "reportObjectivesPanel.reward")
                .addStringTemplate("%reward%", o.getReward(spec)));
            reward.setFont(plain);
            reward.setForeground(colour);
            reportPanel.add(reward, "skip, span 2, wrap 12");
        }
    }

    /**
     * A round badge with the number of a step, a tick once it is done.
     */
    private static final class Badge extends JComponent {

        private final State state;
        private final int step;
        private final Color colour;
        private final Font font;

        Badge(State state, int step, Color colour, Font font) {
            this.state = state;
            this.step = step;
            this.colour = colour;
            this.font = font;
            final int size = Math.round(font.getSize2D() * 1.9f);
            setPreferredSize(new Dimension(size, size));
        }

        @Override
        protected void paintComponent(Graphics g) {
            final Graphics2D g2d = (Graphics2D)g.create();
            try {
                g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                     RenderingHints.VALUE_ANTIALIAS_ON);
                g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                     RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                final int d = Math.min(getWidth(), getHeight()) - 2;
                g2d.setColor(colour);
                if (state == State.LATER) {
                    g2d.drawOval(1, 1, d, d);
                } else {
                    g2d.fillOval(1, 1, d, d);
                }
                final String text = (state == State.DONE) ? "\u2713"
                    : String.valueOf(step);
                g2d.setFont(font);
                final FontMetrics fm = g2d.getFontMetrics();
                g2d.setColor((state == State.LATER) ? colour : Color.WHITE);
                g2d.drawString(text, 1 + (d - fm.stringWidth(text)) / 2,
                               1 + (d + fm.getAscent() - fm.getDescent()) / 2);
            } finally {
                g2d.dispose();
            }
        }
    }
}
