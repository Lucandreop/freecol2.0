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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import javax.swing.JComponent;

import net.sf.freecol.common.model.HistoryEvent;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Turn;


/**
 * A small line chart of a player's score over time.
 *
 * The points come from the score recorded with each history event,
 * plus the current score, so no extra data needs to be saved.
 */
public class ScoreChart extends JComponent {

    /** Space around the plot for the labels. */
    private static final int MARGIN = 6;

    /** The points to plot, as {turn, score}, sorted by turn. */
    private final List<int[]> points;

    /** The line color. */
    private final Color color;


    /**
     * Create a score chart for a player.
     *
     * @param player The {@code Player} to chart.
     * @param size The preferred size of the chart.
     */
    public ScoreChart(Player player, Dimension size) {
        this.points = getScorePoints(player);
        Color c = player.getNationColor();
        this.color = (c == null) ? Color.BLACK : c.darker();
        setPreferredSize(size);
        setOpaque(false);
    }

    /**
     * Get the score of a player at each turn something happened.
     *
     * @param player The {@code Player} to examine.
     * @return A list of {turn, score} pairs sorted by turn, ending
     *     with the current turn and score.
     */
    public static List<int[]> getScorePoints(Player player) {
        TreeMap<Integer, Integer> scores = new TreeMap<>();
        for (HistoryEvent event : player.getHistory()) {
            // Keep the last score recorded in a turn
            scores.put(event.getTurn().getNumber(), event.getScore());
        }
        scores.put(player.getGame().getTurn().getNumber(), player.getScore());
        List<int[]> result = new ArrayList<>();
        scores.forEach((turn, score) -> result.add(new int[] { turn, score }));
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D)g.create();
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                             RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setFont(getFont());
        final FontMetrics fm = g2d.getFontMetrics();
        final int firstTurn = this.points.get(0)[0];
        final int lastTurn = this.points.get(this.points.size() - 1)[0];
        int min = 0, max = 1;
        for (int[] p : this.points) {
            min = Math.min(min, p[1]);
            max = Math.max(max, p[1]);
        }

        // Plot area, leaving room for the score on the left and the
        // years below
        final String maxLabel = Integer.toString(max);
        final int left = MARGIN + fm.stringWidth(maxLabel) + MARGIN;
        final int top = MARGIN + fm.getAscent() / 2;
        final int right = getWidth() - MARGIN;
        final int bottom = getHeight() - MARGIN - fm.getHeight();
        final int w = Math.max(1, right - left);
        final int h = Math.max(1, bottom - top);
        final int turns = Math.max(1, lastTurn - firstTurn);
        final int range = max - min;

        // Axes and labels
        g2d.setColor(new Color(0, 0, 0, 140));
        g2d.drawLine(left, top, left, bottom);
        g2d.drawLine(left, bottom, right, bottom);
        g2d.drawString(maxLabel, left - MARGIN - fm.stringWidth(maxLabel),
                       top + fm.getAscent() / 2);
        final String minLabel = Integer.toString(min);
        g2d.drawString(minLabel, left - MARGIN - fm.stringWidth(minLabel),
                       bottom);
        final String fromLabel = Integer.toString(Turn.getTurnYear(firstTurn));
        final String toLabel = Integer.toString(Turn.getTurnYear(lastTurn));
        g2d.drawString(fromLabel, left, bottom + fm.getAscent() + 2);
        g2d.drawString(toLabel, right - fm.stringWidth(toLabel),
                       bottom + fm.getAscent() + 2);

        // The score line, with the area below it lightly filled
        Polygon line = new Polygon();
        for (int[] p : this.points) {
            line.addPoint(left + (p[0] - firstTurn) * w / turns,
                          bottom - (p[1] - min) * h / range);
        }
        Polygon area = new Polygon(line.xpoints, line.ypoints, line.npoints);
        area.addPoint(line.xpoints[line.npoints - 1], bottom);
        area.addPoint(line.xpoints[0], bottom);
        g2d.setColor(new Color(this.color.getRed(), this.color.getGreen(),
                               this.color.getBlue(), 50));
        g2d.fillPolygon(area);
        g2d.setColor(this.color);
        g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND,
                                      BasicStroke.JOIN_ROUND));
        g2d.drawPolyline(line.xpoints, line.ypoints, line.npoints);
        g2d.dispose();
    }
}
