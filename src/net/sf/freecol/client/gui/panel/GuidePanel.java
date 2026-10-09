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
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * A card on the map that takes a new player through the first steps
 * of the game, one at a time, moving on as each is done.  It can be
 * skipped at any time, and is then not shown again.
 */
public final class GuidePanel extends JPanel {

    private static final Logger logger = Logger.getLogger(GuidePanel.class.getName());

    /** The steps of the guide, in order. */
    enum Step {
        WELCOME, MOVE, LAND, FOUND, COLONY, TURN, EXPLORE, EUROPE, OBJECTIVES;

        String getKey() {
            return "guide." + name().toLowerCase(Locale.US);
        }

        /** Is this step only something to read? */
        boolean isReading() {
            return this == WELCOME || this == OBJECTIVES;
        }
    }

    /** Things the player has done that the game does not record. */
    private static final Set<String> events = ConcurrentHashMap.newKeySet();

    /** The colour of the text. */
    private static final Color INK = new Color(50, 35, 20);

    /** The client to get the player and options from. */
    private final FreeColClient freeColClient;

    /** The image library to scale with. */
    private final ImageLibrary lib;

    /** Checks now and then whether the step is done. */
    private final Timer timer;

    /** The step being shown. */
    private Step step = Step.WELCOME;

    /** The step last laid out. */
    private Step shown = null;

    /** Where the units stood when the player was asked to move. */
    private final Map<Unit, Tile> startTiles = new HashMap<>();

    /** The turn when the player was asked to end it. */
    private int startTurn = -1;


    /**
     * Create the guide card.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public GuidePanel(FreeColClient freeColClient) {
        this.freeColClient = freeColClient;
        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        final int pad = lib.scaleInt(8);
        setLayout(new MigLayout("wrap 1, ins " + pad + " " + (2 * pad)
                + " " + pad + " " + (2 * pad) + ", gap 0 " + lib.scaleInt(4),
                "[left]"));
        setOpaque(false);
        this.timer = new Timer(1000, ae -> refresh());
    }

    /**
     * Note something the player has done, for the steps that are
     * about opening a panel or giving an order.
     *
     * @param event The name of what was done.
     */
    public static void mark(String event) {
        events.add(event);
    }

    /**
     * Is the guide wanted?
     *
     * @return True if there is a game and the player has not skipped
     *     or finished the guide.
     */
    public boolean isWanted() {
        return freeColClient.getMyPlayer() != null
            && freeColClient.getClientOptions()
                .getBoolean(ClientOptions.GUI_SHOW_GUIDE);
    }

    /**
     * Move on past the steps already done, and show the current one.
     */
    public void refresh() {
        final Player player = freeColClient.getMyPlayer();
        if (player == null || !isWanted()) return;
        // A game already under way needs no welcome
        if (step == Step.WELCOME && player.getSettlementCount() > 0) {
            next(player);
        }
        while (step != null && !step.isReading() && isDone(step, player)) {
            next(player);
        }
        if (step == null) {
            stop();
            return;
        }
        if (step == shown) return;
        shown = step;
        layoutStep();
    }

    /**
     * Go on to the next step, noting what it needs to know.
     *
     * @param player The {@code Player} being guided.
     */
    private void next(Player player) {
        final Step[] steps = Step.values();
        step = (step.ordinal() + 1 < steps.length)
            ? steps[step.ordinal() + 1] : null;
        if (step == Step.MOVE) {
            startTiles.clear();
            for (Unit u : player.getUnitSet()) {
                if (u.hasTile()) startTiles.put(u, u.getTile());
            }
        } else if (step == Step.TURN) {
            startTurn = freeColClient.getGame().getTurn().getNumber();
        }
    }

    /**
     * Has a step been done?
     *
     * @param s The {@code Step} to check.
     * @param player The {@code Player} being guided.
     * @return True if the player has done what the step asks.
     */
    private boolean isDone(Step s, Player player) {
        final boolean colony = player.getSettlementCount() > 0;
        switch (s) {
        case MOVE:
            if (colony || isLanded(player)) return true;
            for (Unit u : player.getUnitSet()) {
                final Tile start = startTiles.get(u);
                if (start != null && u.getTile() != start) return true;
            }
            return false;
        case LAND:
            return colony || isLanded(player);
        case FOUND:
            return colony;
        case COLONY:
            return events.contains("colony");
        case TURN:
            return startTurn >= 0
                && freeColClient.getGame().getTurn().getNumber() > startTurn;
        case EXPLORE:
            if (events.contains("explore") || events.contains("sail")) {
                return true;
            }
            for (Unit u : player.getUnitSet()) {
                if (u.isNaval() && (u.isInEurope() || u.isAtSea())) return true;
            }
            return false;
        case EUROPE:
            return events.contains("europe");
        default:
            return false;
        }
    }

    /**
     * Is any colonist of a player standing on land?
     *
     * @param player The {@code Player} to check.
     * @return True if a land unit is on land and off its ship.
     */
    private static boolean isLanded(Player player) {
        for (Unit u : player.getUnitSet()) {
            if (!u.isNaval() && !u.isOnCarrier() && u.hasTile()
                && u.getTile().isLand()) return true;
        }
        return false;
    }

    /**
     * Lay out the current step.
     */
    private void layoutStep() {
        removeAll();
        final Font small = FontLibrary.getScaledFont("simple-bold-tiny");
        final Font bold = FontLibrary.getScaledFont("simple-bold-small");
        final Font plain = FontLibrary.getScaledFont("simple-plain-small");

        final JLabel progress = new JLabel(Messages.message(StringTemplate
                .template("guide.progress")
                .addAmount("%number%", step.ordinal() + 1)
                .addAmount("%total%", Step.values().length)));
        progress.setFont(small);
        progress.setForeground(new Color(150, 100, 20));
        add(progress);

        final JLabel title = new JLabel(Messages.message(step.getKey() + ".title"));
        title.setFont(bold);
        title.setForeground(INK);
        add(title, "gaptop " + lib.scaleInt(2));

        final JTextArea text = new JTextArea(Messages.message(step.getKey() + ".text"));
        text.setFont(plain);
        text.setForeground(INK);
        text.setOpaque(false);
        text.setEditable(false);
        text.setFocusable(false);
        text.setLineWrap(true);
        text.setWrapStyleWord(true);
        add(text, "width " + lib.scaleInt(380) + "!");

        final JPanel buttons = new JPanel(new MigLayout("ins 0, gap "
                + lib.scaleInt(12), "[left]push[right]"));
        buttons.setOpaque(false);
        if (step == Step.WELCOME) {
            buttons.add(link(Messages.message("guide.start"), bold,
                new Color(30, 90, 30), () -> {
                    next(freeColClient.getMyPlayer());
                    refresh();
                }));
        } else if (step == Step.OBJECTIVES) {
            buttons.add(link(Messages.message("guide.finish"), bold,
                new Color(30, 90, 30), this::finish));
        } else {
            buttons.add(link(Messages.message("guide.skipStep"), plain,
                new Color(110, 80, 50), () -> {
                    next(freeColClient.getMyPlayer());
                    refresh();
                }));
        }
        if (step != Step.OBJECTIVES) {
            final JLabel skip = link(Messages.message("guide.skip"), plain,
                new Color(110, 80, 50), this::finish);
            skip.setToolTipText(Messages.message("guide.skip.tip"));
            buttons.add(skip);
        }
        add(buttons, "growx, gaptop " + lib.scaleInt(4));

        setSize(getPreferredSize());
        final Container parent = getParent();
        if (parent != null) {
            setLocation((parent.getWidth() - getWidth()) / 2, getY());
        }
        revalidate();
        repaint();
    }

    /**
     * Make a label that acts on a click.
     *
     * @param text The text.
     * @param font The font.
     * @param color The colour.
     * @param action What to do on a click.
     * @return The label.
     */
    private JLabel link(String text, Font font, Color color, Runnable action) {
        final JLabel label = new JLabel(text);
        label.setFont(font);
        label.setForeground(color);
        label.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        label.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    action.run();
                }
            });
        return label;
    }

    /**
     * The guide is done with, finished or skipped: do not show it
     * again.
     */
    private void finish() {
        final ClientOptions options = freeColClient.getClientOptions();
        options.setBoolean(ClientOptions.GUI_SHOW_GUIDE, false);
        if (!options.save(FreeColDirectories.getClientOptionsFile())) {
            logger.warning("Could not save that the guide is done");
        }
        stop();
    }

    /**
     * Take the card off the map.
     */
    private void stop() {
        timer.stop();
        setSize(0, 0);
        final Container parent = getParent();
        if (parent != null) {
            parent.remove(this);
            parent.repaint();
        }
    }


    // Override JComponent

    /**
     * {@inheritDoc}
     */
    @Override
    public void addNotify() {
        super.addNotify();
        timer.start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
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
            g2d.setColor(new Color(150, 100, 20, 240));
            g2d.fillRoundRect(0, 0, getWidth() - 2, getHeight() - 2, arc, arc);
            g2d.setColor(new Color(250, 240, 212, 245));
            g2d.fillRoundRect(3, 3, getWidth() - 8, getHeight() - 8, arc, arc);
        } finally {
            g2d.dispose();
        }
        super.paintComponent(g);
    }
}
