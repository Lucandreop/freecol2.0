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

import static net.sf.freecol.common.util.CollectionUtils.none;
import static net.sf.freecol.common.util.StringUtils.getBreakingPoint;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.FontLibrary;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.i18n.Messages;
import net.sf.freecol.common.model.AbstractGoods;
import net.sf.freecol.common.model.BuildableType;
import net.sf.freecol.common.model.Building;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.ProductionType;
import net.sf.freecol.common.model.StringTemplate;


/**
 * This panel shows the progress of constructing a building or
 * unit in a colony.
 */
public class ConstructionPanel extends MigPanel
    implements PropertyChangeListener {

    public static final String EVENT
        = Colony.ColonyChangeEvent.BUILD_QUEUE_CHANGE.toString();

    /** The enclosing client. */
    private final FreeColClient freeColClient;

    /** Should a mouse click open the build queue? */
    private final boolean openBuildQueue;

    /** The colony performing the construction. */
    private Colony colony;

    /** The text to display if buildable == null. */
    private StringTemplate defaultLabel
        = StringTemplate.key("constructionPanel.clickToBuild");


    /**
     * Creates a ConstructionPanel.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     * @param colony The {@code Colony} whose construction is to be
     *     modified.
     * @param openBuildQueue True if the build queue should be immediately
     *     shown.
     */
    public ConstructionPanel(FreeColClient freeColClient,
                             Colony colony, boolean openBuildQueue) {
        super("ConstructionPanelUI",
            new MigLayout("fill, ins 0 0 0 0, gapy 0, wrap 2", "push[]10[center]push", "[center]"));

        this.freeColClient = freeColClient;
        this.colony = colony;
        this.openBuildQueue = openBuildQueue;

        setOpaque(false);
    }


    /**
     * Add a listener for button presses on this panel to show the BuildQueuePanel
     */
    public void initialize() {
        if (colony != null) {
            // we are interested in changes to the build queue, as well as
            // changes to the warehouse and the colony's production bonus
            colony.addPropertyChangeListener(EVENT, this);
                
            if (openBuildQueue) {
                addMouseListener(new MouseAdapter() {
                        @Override
                        public void mousePressed(MouseEvent e) {
                            freeColClient.getGUI().showBuildQueuePanel(colony);
                        }
                    });
            }
        }
        update();
    }

    /**
     * Removes PropertyChangeListeners and MouseListeners
     */
    public void cleanup() {
        if (colony != null) {
            colony.removePropertyChangeListener(EVENT, this);
        }
        for (MouseListener listener : getMouseListeners()) {
            removeMouseListener(listener);
        }
    }

    /**
     * This method updates the Construction Panel.
     *
     * With zero arguments, the update() method can only be
     * run on what a given colony is currently building.
     */
    public void update() {
        update((colony == null) ? null : colony.getCurrentlyBuilding());
    }

    /**
     * This method updates the Construction Panel.
     *
     * With one argument, the update() method can be called
     * to update the panel based on a called {@code BuildableType}
     * This method is used when a change to to the
     * {@code BuildQueuePanel} are made.
     *
     * @param buildable The BuildableType object to update.
     *
     * @see BuildQueuePanel for the only use of the one-argument method.
     */
    public void update(BuildableType buildable) {
        removeAll();
        final ImageLibrary lib = this.freeColClient.getGUI()
            .getFixedImageLibrary();
        
        Font font = FontLibrary.getScaledFont("normal-plain-smaller", null);
        Font fontTitle = FontLibrary.getScaledFont("normal-plain-smaller", null);
        final int maxFontSize = lib.scaleInt(17);
        if (font.getSize() > maxFontSize) {
            font = font.deriveFont((float) maxFontSize);
            fontTitle = fontTitle.deriveFont((float) maxFontSize);
        }

        if (buildable == null) {
            String clickToBuild = Messages.message(getDefaultLabel());
            int breakingPoint = getBreakingPoint(clickToBuild);
            if (breakingPoint > 0) {
                JLabel label0 = new JLabel(
                    clickToBuild.substring(0, breakingPoint));
                label0.setFont(font);
                label0.setForeground(getForeground());
                add(label0, "span, align center bottom");
                JLabel label1 = new JLabel(
                    clickToBuild.substring(breakingPoint + 1));
                label1.setFont(font);
                label1.setForeground(getForeground());
                add(label1, "span, align center top");
            } else {
                JLabel label = new JLabel(clickToBuild);
                label.setFont(font);
                label.setForeground(getForeground());
                add(label, "span, align center");
            }
        } else {
            final JPanel infoPanel = new JPanel(new MigLayout("wrap 1", "[center]"));
            infoPanel.setOpaque(false);
            
            final Image image = lib.getSmallBuildableTypeImageWithWithSize(buildable, colony.getOwner(), new Dimension(lib.scaleInt(100), lib.scaleInt(96)));
            add(new JLabel(new ImageIcon(image)), "spany");
            
            final JLabel label0 = Utility.localizedLabel(buildable.getCurrentlyBuildingLabel());
            label0.setFont(fontTitle);
            label0.setForeground(getForeground());
            infoPanel.add(label0);

            for (AbstractGoods ag : buildable.getRequiredGoodsList()) {
                int amountNeeded = ag.getAmount();
                int amountAvailable = colony.getGoodsCount(ag.getType());
                int amountProduced = colony.getAdjustedNetProductionOf(ag.getType());
                infoPanel.add(new FreeColProgressBar(this.freeColClient, ag.getType(), 0,
                                           amountNeeded, amountAvailable, amountProduced),
                    "height 20:");
            }
            // When will it be done, or why is it not getting done?
            final Font statusFont = font.deriveFont(font.getSize2D() * 0.85f);
            final JLabel status = new JLabel(wrap(
                Messages.message(getBuildStatus(colony, buildable)),
                getFontMetrics(statusFont), lib.scaleInt(190)));
            status.setFont(statusFont);
            status.setForeground(getForeground());
            infoPanel.add(status);
            add(infoPanel);
        }

        revalidate();
        repaint();
    }


    /**
     * Break a text into centered lines no wider than a given width.
     *
     * @param text The text.
     * @param fm The {@code FontMetrics} of the font it is shown in.
     * @param width The greatest width of a line.
     * @return The text as HTML for a label.
     */
    private static String wrap(String text, FontMetrics fm, int width) {
        final StringBuilder sb = new StringBuilder("<html><center>");
        String line = "";
        for (String word : text.split(" ")) {
            final String longer = (line.isEmpty()) ? word : line + " " + word;
            if (!line.isEmpty() && fm.stringWidth(longer) > width) {
                sb.append(escape(line)).append("<br>");
                line = word;
            } else {
                line = longer;
            }
        }
        return sb.append(escape(line)).append("</center></html>").toString();
    }

    private static String escape(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;")
            .replace(">", "&gt;");
    }

    /**
     * Say when a build will be done, or why it is not getting done.
     *
     * Package-visible for the test suite.
     *
     * @param colony The {@code Colony} building.
     * @param buildable The {@code BuildableType} being built.
     * @return A template describing the state of the build.
     */
    static StringTemplate getBuildStatus(Colony colony,
                                         BuildableType buildable) {
        final AbstractGoods needed = new AbstractGoods();
        final int turns = colony.getTurnsToComplete(buildable, needed);
        if (turns >= 0) {
            return (turns <= 1)
                ? StringTemplate.key("constructionPanel.status.nextTurn")
                : StringTemplate.template("constructionPanel.status.turns")
                    .addAmount("%number%", turns);
        }
        final GoodsType type = needed.getType();
        if (type == null) {
            return StringTemplate.key("constructionPanel.status.stalled");
        }
        // Is there a building that could make the missing goods?
        for (Building building : colony.getBuildings()) {
            for (ProductionType pt : building.getType()
                     .getAvailableProductionTypes(false)) {
                if (none(pt.getOutputs(), AbstractGoods.matches(type))) {
                    continue;
                }
                if (building.getUnitCount() == 0) {
                    return StringTemplate
                        .template("constructionPanel.status.nobody")
                        .addNamed("%goods%", type)
                        .addNamed("%building%", building);
                }
                for (AbstractGoods input : pt.getInputList()) {
                    final GoodsType in = input.getType();
                    if (colony.getGoodsCount(in) <= 0
                        && colony.getNetProductionOf(in) <= 0) {
                        return StringTemplate
                            .template("constructionPanel.status.noInput")
                            .addNamed("%goods%", in)
                            .addNamed("%building%", building);
                    }
                }
            }
        }
        return StringTemplate.template("constructionPanel.status.missing")
            .addAmount("%amount%", needed.getAmount())
            .addNamed("%goods%", type);
    }

    /**
     * @return A {@code StringTemplate} of the ConstructionPanel's Label
     */
    private final StringTemplate getDefaultLabel() {
        return defaultLabel;
    }

    /**
     * Set the ConstructionPanel's Label as a {@code StringTemplate}
     *
     * @param newDefaultLabel The default StringTemplate label of the panel.
     */
    public final void setDefaultLabel(final StringTemplate newDefaultLabel) {
        this.defaultLabel = newDefaultLabel;
    }


    // Interface PropertyChangeListener

    /**
     * {@inheritDoc}
     *
     * Upon a change to a bound parameter, call the {@link #update()} method.
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        update();
    }
    
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        
        final Color oldColor = g.getColor();
        g.setColor(new Color(0, 0, 0, 128));
        final Dimension size = getSize();
        g.fillRect(0, 0, size.width, size.height);
        g.setColor(oldColor);
    }
}
