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

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.Objective;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.client.ClientOptions;


/**
 * A small card on the map listing what is coming up in the next few
 * turns (see {@link Agenda}).  Clicking an entry goes to the colony or
 * unit it is about, and clicking the title shows more, or folds it up.
 */
public final class AgendaPanel extends JPanel {

    /** How much of the agenda to show. */
    private enum Fold {
        SOME(5), ALL(15), NONE(0);

        final int count;

        Fold(int count) {
            this.count = count;
        }

        Fold next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    /** The colour of the text. */
    private static final Color INK = new Color(50, 35, 20);

    /** The client to get the player and GUI from. */
    private final FreeColClient freeColClient;

    /** The image library to scale with. */
    private final ImageLibrary lib;

    /** How much of the agenda is shown. */
    private Fold fold = Fold.SOME;


    /**
     * Create the agenda card.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public AgendaPanel(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        final int pad = lib.scaleInt(6);
        setLayout(new MigLayout("wrap 2, ins " + pad + " " + (2 * pad) + " "
                + pad + " " + (2 * pad) + ", gap " + pad + " "
                + lib.scaleInt(2), "[center][left]"));
        setOpaque(false);
    }

    /**
     * Bring the card up to date with the agenda of the player.
     */
    public void refresh() {
        removeAll();
        final Player player = freeColClient.getMyPlayer();
        final List<Agenda.Item> items = (player == null) ? List.of()
            : Agenda.getItems(player);
        final int shown = Math.min(fold.count, items.size());

        final Font bold = FontLibrary.getScaledFont("simple-bold-small");
        final Font plain = FontLibrary.getScaledFont("simple-plain-small");
        String title = Messages.message("agenda.title");
        if (items.size() > shown) title += "  (+" + (items.size() - shown) + ")";
        title += (fold == Fold.NONE) ? "  \u25b8" : "  \u25be";
        final JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(bold);
        titleLabel.setForeground(INK);
        titleLabel.setToolTipText(Messages.message("agenda.fold"));
        titleLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        titleLabel.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    fold = fold.next();
                    refresh();
                }
            });
        add(titleLabel, "span, align left");

        // The next objective, for those still learning the game
        final Objective objective = (player == null || !freeColClient
            .getClientOptions().getBoolean(ClientOptions.GUI_SHOW_TUTORIAL))
            ? null : Objective.getCurrent(player);
        if (objective != null && fold != Fold.NONE) {
            final JLabel star = new JLabel("\u2605");
            star.setFont(bold);
            star.setForeground(new Color(176, 124, 20));
            add(star, "align center");
            final JLabel goal = new JLabel(Messages.message(StringTemplate
                .template("agenda.objective")
                .addStringTemplate("%objective%",
                    StringTemplate.key(objective.getKey() + ".name"))
                .addAmount("%progress%", objective.getProgress(player))
                .addAmount("%target%", objective.getTarget())));
            goal.setFont(bold);
            goal.setForeground(INK);
            goal.setToolTipText(Messages.message(StringTemplate
                .template("reportObjectivesPanel.reward")
                .addStringTemplate("%reward%",
                    objective.getReward(player.getSpecification()))));
            goal.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            goal.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        freeColClient.getGUI().showReportObjectivesPanel();
                    }
                });
            add(goal);
        }

        if (items.isEmpty() && fold != Fold.NONE) {
            final JLabel empty = new JLabel(Messages.message("agenda.empty"));
            empty.setFont(plain);
            empty.setForeground(INK);
            add(empty, "span, align left");
        }
        for (int i = 0; i < shown; i++) {
            final Agenda.Item item = items.get(i);
            final String tip = Messages.message((item.detail != null)
                ? item.detail
                : StringTemplate.template("agenda.inTurns")
                    .addAmount("%number%", item.turns));
            final Badge badge = new Badge(item, bold);
            badge.setToolTipText(tip);
            add(badge);
            final JLabel text = new JLabel(Messages.message(item.text));
            text.setFont(plain);
            text.setForeground((item.problem) ? new Color(140, 25, 15) : INK);
            text.setToolTipText(tip);
            if (item.subject != null) {
                text.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                text.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            goTo(item);
                        }
                    });
            }
            add(text);
        }
        setSize(getPreferredSize());
        revalidate();
        repaint();
    }

    /**
     * Go to what an entry is about: open its colony, or show its unit.
     *
     * @param item The {@code Agenda.Item} to go to.
     */
    private void goTo(Agenda.Item item) {
        final GUI gui = freeColClient.getGUI();
        if (item.subject instanceof Colony) {
            gui.showColonyPanel((Colony)item.subject, null);
        } else if (item.subject instanceof Europe) {
            gui.showEuropePanel();
        } else if (item.subject instanceof Player) {
            gui.showReportContinentalCongressPanel();
        } else if (item.subject instanceof Unit) {
            final Unit unit = (Unit)item.subject;
            if (unit.getTile() != null) {
                gui.setFocus(unit.getTile());
            } else if (unit.isInEurope()) {
                gui.showEuropePanel();
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D g2d = (Graphics2D)g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                 RenderingHints.VALUE_ANTIALIAS_ON);
            final int arc = lib.scaleInt(14);
            g2d.setColor(new Color(0, 0, 0, 60));
            g2d.fillRoundRect(2, 2, getWidth() - 2, getHeight() - 2, arc, arc);
            g2d.setColor(new Color(110, 75, 40, 235));
            g2d.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, arc, arc);
            g2d.setColor(new Color(248, 236, 205, 235));
            g2d.fillRoundRect(2, 2, getWidth() - 6, getHeight() - 6, arc, arc);
        } finally {
            g2d.dispose();
        }
        super.paintComponent(g);
    }

    /**
     * A round badge with the number of turns until something happens,
     * red for a problem and gold for the very next turn.
     */
    private final class Badge extends JComponent {

        private final Agenda.Item item;
        private final Font font;

        Badge(Agenda.Item item, Font font) {
            this.item = item;
            this.font = font;
            final int size = lib.scaleInt(20);
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
                final int d = Math.min(getWidth(), getHeight()) - 1;
                g2d.setColor((item.problem) ? new Color(170, 35, 25)
                    : (item.turns <= 1) ? new Color(200, 150, 30)
                    : new Color(90, 110, 50));
                g2d.fillOval(0, 0, d, d);
                g2d.setColor(new Color(60, 40, 20));
                g2d.drawOval(0, 0, d, d);
                final String text = (item.problem && item.turns <= 0) ? "!"
                    : String.valueOf(item.turns);
                Font f = font;
                FontMetrics fm = g2d.getFontMetrics(f);
                while (fm.stringWidth(text) > d - 4 && f.getSize2D() > 8f) {
                    f = f.deriveFont(f.getSize2D() - 1f);
                    fm = g2d.getFontMetrics(f);
                }
                g2d.setFont(f);
                g2d.setColor(Color.WHITE);
                g2d.drawString(text, (d - fm.stringWidth(text)) / 2 + 1,
                               (d + fm.getAscent() - fm.getDescent()) / 2 + 1);
            } finally {
                g2d.dispose();
            }
        }
    }
}
