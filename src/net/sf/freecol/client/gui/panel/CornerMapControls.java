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
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;
import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.client.gui.GUI;
import net.sf.freecol.client.gui.ImageLibrary;
import net.sf.freecol.common.model.Direction;
import net.sf.freecol.common.model.StringTemplate;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;
import net.sf.freecol.common.resources.PropertyList;
import net.sf.freecol.common.resources.ResourceManager;
import java.awt.Container;
import java.awt.FlowLayout;
import javax.swing.Action;
import net.sf.freecol.client.gui.action.FreeColAction;
import net.sf.freecol.common.i18n.Messages;


/**
 * A collection of panels and buttons that are used to provide the
 * user with a more detailed view of certain elements on the map and
 * also to provide a means of input in case the user can't use the
 * keyboard.
 */
public final class CornerMapControls extends MapControls {

    private class MiniMapPanelSkin extends JPanel {
        
        MiniMapPanelSkin() {
            setOpaque(false);
        }
        
        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            if (miniMapSkin != null) {
                graphics.drawImage(miniMapSkin, 0, 0, null);
            }
        }
    }

    /** The image library. */
    private final ImageLibrary lib;
    
    /** The compass rose graphic. */
    private final JLabel compassRose;

    /** The mini map has its own panel. */
    private final JPanel miniMapPanel;

    /** The agenda of what is coming up in the next few turns. */
    private final AgendaPanel agendaPanel;

    /** The guide through the first steps, for new players. */
    private final GuidePanel guidePanel;

    /** The units on the tile of the active unit, to pick one from. */
    private final JPanel stackPanel;

    /** A skin for the mini map. */
    private Image miniMapSkin;

    private MiniMapPanelSkin miniMapPanelSkin;
    
    private boolean oldUseSkin = false;
    
    private boolean forceUpdate = false;
    
    
    /**
     * The basic constructor.
     *
     * @param freeColClient The {@code FreeColClient} for the game.
     */
    public CornerMapControls(final FreeColClient freeColClient) {
        super(freeColClient);

        this.lib = freeColClient.getGUI().getFixedImageLibrary();
        this.compassRose = this.lib.getCompassRose();
        this.compassRose.setFocusable(false);
        this.compassRose.setSize(this.compassRose.getPreferredSize());
        this.compassRose.addMouseListener(new MouseAdapter() {
                /**
                 * {@inheritDoc}
                 */
                @Override
                public void mouseClicked(MouseEvent e) {
                    Unit unit = getGUI().getActiveUnit();
                    if (unit == null) return;
                    int x = e.getX() - compassRose.getWidth()/2;
                    int y = e.getY() - compassRose.getHeight()/2;
                    double theta = Math.atan2(y, x) + Math.PI/2 + Math.PI/8;
                    if (theta < 0) {
                        theta += 2*Math.PI;
                    }
                    igc().moveUnit(unit, Direction.angleToDirection(theta));
                }
            });
    
        this.agendaPanel = new AgendaPanel(freeColClient);
        this.guidePanel = new GuidePanel(freeColClient);
        this.stackPanel = new JPanel(new FlowLayout(FlowLayout.CENTER,
                                                    lib.scaleInt(3), 0));
        this.stackPanel.setOpaque(false);
        this.stackPanel.setVisible(false);
        this.miniMapPanel = new MiniMapFreeColPanel(freeColClient);
        this.miniMapPanelSkin = new MiniMapPanelSkin();
        
        // Add buttons:
        this.miniMapPanel.add(this.miniMapToggleBorders);
        this.miniMapPanel.add(this.miniMapToggleFogOfWarButton);
        this.miniMapPanel.add(this.miniMapZoomInButton);
        this.miniMapPanel.add(this.miniMapZoomOutButton);
        this.miniMapPanel.add(this.miniMapPanelSkin);
        this.miniMapPanel.add(this.miniMap);
        
        updateLayoutIfNeeded();
    }

            
    // Implement MapControls
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void updateLayoutIfNeeded() {
        super.updateLayoutIfNeeded();
        
        BufferedImage newMinimapSkin = this.lib.getMiniMapSkin();
        if (!forceUpdate && oldUseSkin == isUseSkin() && (!oldUseSkin || this.miniMapSkin == newMinimapSkin)) {
            // No update necessary.
            return;
        } else if (!isUseSkin()) {
            newMinimapSkin = null;
            final MigLayout layout = new MigLayout("ins 0 0 0 0, gap 0 0");
            this.miniMapPanel.setLayout(layout);
            layout.addLayoutComponent(this.miniMap, "newline, grow, shrink, span, w 100%, h 100%");
        } else {
            this.miniMapPanel.setLayout(null);
        }

        this.oldUseSkin = isUseSkin();
        this.miniMapSkin = newMinimapSkin;
        this.forceUpdate = false;
        
        
        int width = this.lib.scaleInt(MINI_MAP_WIDTH);
        int height = this.lib.scaleInt(MINI_MAP_HEIGHT);
        this.miniMap.setSize(new Dimension(width, height));
        if (this.miniMapSkin != null) {
            width = this.miniMapSkin.getWidth(null);
            height = this.miniMapSkin.getHeight(null);
            //this.miniMapPanel.setBorder(null);
            this.miniMapPanel.setSize(width, height);
            this.miniMapPanelSkin.setLocation(0, 0);
            this.miniMapPanelSkin.setSize(width, height);
            this.miniMapPanel.setOpaque(false);
        } else {
            this.miniMapPanel.setOpaque(true);
            //this.miniMap.setBorder(new BevelBorder(BevelBorder.RAISED));
        }

        if (isUseSkin()) {
            final PropertyList pl = ResourceManager.getPropertyList("image.skin.MiniMap.properties");
            this.miniMap.setLocation(
                    this.lib.scaleInt(pl.getInt("minimap.x")),
                    this.lib.scaleInt(pl.getInt("minimap.y")));
            this.miniMap.setSize(
                    this.lib.scaleInt(pl.getInt("minimap.width")),
                    this.lib.scaleInt(pl.getInt("minimap.height")));
            
            centerComponentOnCoordinate(miniMapToggleBorders, pl, "politicalButton");
            centerComponentOnCoordinate(miniMapToggleFogOfWarButton, pl, "fogOfWarButton");
            centerComponentOnCoordinate(miniMapZoomInButton, pl, "zoomInButton");
            centerComponentOnCoordinate(miniMapZoomOutButton, pl, "zoomOutButton");
        } else {
            this.miniMapPanel.setPreferredSize(new Dimension(width, height));
            getGUI().restoreSavedSize(this.miniMapPanel, new Dimension(width, height));
        }

        miniMapPanel.revalidate();
        miniMapPanel.repaint();
    }
    
    private void centerComponentOnCoordinate(JComponent component, PropertyList pl, String key) {
        final int x = this.lib.scaleInt(pl.getInt(key + ".x"));
        final int y = this.lib.scaleInt(pl.getInt(key + ".y"));
        component.setLocation(x - component.getWidth() / 2, y - component.getHeight() / 2);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JComponent> getComponentsToAdd(Dimension newSize) {
        List<JComponent> ret = new ArrayList<>();
        if (getGame() == null) return ret;
        
        final int cw = newSize.width;
        final int ch = newSize.height;
        
        if (!this.infoPanel.isShowing() && !this.miniMapPanel.isShowing()) {
            forceUpdate = true;
            updateLayoutIfNeeded();
            
            if (!this.getFreeColClient().isMapEditor()) {
                this.infoPanel.setLocation(cw - this.infoPanel.getWidth(), ch - this.infoPanel.getHeight());
                this.miniMapPanel.setLocation(0, ch - this.miniMapPanel.getHeight());
            }
            this.infoPanel.refresh();
            ret.add(this.infoPanel);
            ret.add(this.miniMapPanel);
        }
        
        if (!this.getFreeColClient().isMapEditor()) {
            final boolean rose = getClientOptions()
                .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE);
            if (rose && !this.compassRose.isShowing()) {
                this.compassRose.setLocation(cw - this.compassRose.getWidth() - 20, 20);
                ret.add(this.compassRose);
            }
            
            // Never hand back a card already on the canvas, even if
            // it is hidden for now
            if (showAgenda() && this.agendaPanel.getParent() == null) {
                this.agendaPanel.refresh();
                this.agendaPanel.setLocation(lib.scaleInt(8), lib.scaleInt(8));
                this.agendaPanel.setVisible(!this.guidePanel.hidesAgenda());
                ret.add(this.agendaPanel);
            }
            if (this.stackPanel.getParent() == null) {
                ret.add(this.stackPanel);
            }
            if (this.guidePanel.isWanted()
                && this.guidePanel.getParent() == null) {
                this.guidePanel.refresh();
                this.guidePanel.setLocation(
                    (cw - this.guidePanel.getWidth()) / 2, lib.scaleInt(8));
                ret.add(this.guidePanel);
            }

            ret.addAll(this.unitButtons.stream().filter(b -> !b.isShowing()).collect(Collectors.toList()));
    
            if (!this.unitButtons.isEmpty()) {
                final int UNSCALED_SPACE_BETWEEN_BUTTONS = 5;
                final int spaceBetweenButtons = lib.scaleInt(UNSCALED_SPACE_BETWEEN_BUTTONS);
                final Dimension buttonsDimension = calculateTotalDimension(unitButtons, spaceBetweenButtons);
                
                final int totalWidth = buttonsDimension.width + this.miniMapPanel.getWidth() + this.infoPanel.getWidth();
                if (totalWidth < newSize.width) {
                    final Point firstButtonPoint = calculateFirstPosition(newSize, unitButtons, spaceBetweenButtons, buttonsDimension);
                    layoutUnitButtons(unitButtons, buttonsDimension, firstButtonPoint, spaceBetweenButtons);
                } else {
                    final int numberInTopRow = this.unitButtons.size() / 2;
                    
                    final List<UnitButton> bottomRowButtons = unitButtons.subList(numberInTopRow, unitButtons.size());
                    final Dimension buttonsBottomRowDimension = calculateTotalDimension(bottomRowButtons, spaceBetweenButtons);
                    final Point firstButtonBottomRowPoint = calculateFirstPosition(newSize, bottomRowButtons, spaceBetweenButtons, buttonsBottomRowDimension);
                    layoutUnitButtons(bottomRowButtons,buttonsDimension, firstButtonBottomRowPoint, spaceBetweenButtons);
    
                    final List<UnitButton> topRowButtons = unitButtons.subList(0, numberInTopRow);
                    final Dimension buttonsTopRowDimension = calculateTotalDimension(topRowButtons, spaceBetweenButtons);
                    final Point firstButtonTopRowPoint = calculateFirstPosition(newSize, bottomRowButtons, spaceBetweenButtons, buttonsTopRowDimension);
                    layoutUnitButtons(topRowButtons, buttonsDimension, new Point(firstButtonTopRowPoint.x, firstButtonBottomRowPoint.y - buttonsTopRowDimension.height - spaceBetweenButtons), spaceBetweenButtons);
                }
            }
        }
        return ret;
    }
    
    private static Dimension calculateTotalDimension(List<UnitButton> unitButtons, int spaceBetweenButtons) {
        int width = -spaceBetweenButtons, height = 0;
        for (UnitButton ub : unitButtons) {
            if (ub.isShowing()) continue;
            height = Math.max(height, ub.getHeight());
            width += spaceBetweenButtons + ub.getWidth();
        }
        return new Dimension(width, height);
    }
    
    private Point calculateFirstPosition(Dimension newSize, List<UnitButton> unitButtons, int spaceBetweenButtons, Dimension buttonsDimension) {
        final int x = this.miniMapPanel.getWidth() + 1
                + (this.infoPanel.getX() - this.miniMapPanel.getWidth() - buttonsDimension.width) / 2;
        final int y = newSize.height - buttonsDimension.height - spaceBetweenButtons;
        return new Point(x, y);
    }

    private void layoutUnitButtons(List<UnitButton> unitButtons, final Dimension buttonsDimension, Point firstButtonPoint, final int spaceBetweenButtons) {
        int x = firstButtonPoint.x;
        final int y = firstButtonPoint.y;
        
        for (UnitButton ub : unitButtons) {
            if (ub.isShowing()) continue;
            ub.setLocation(x, y);
            x += spaceBetweenButtons + ub.getWidth();
            ub.refreshAction();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<JComponent> getComponentsPresent() {
        List<JComponent> ret = new ArrayList<>();
        
        if (isShowingOrIconified(this.infoPanel)) {
            ret.add(this.infoPanel);
        }
        if (isShowingOrIconified(this.miniMapPanel)) {
            ret.add(this.miniMapPanel);
        }
        final boolean rose = getClientOptions()
            .getBoolean(ClientOptions.DISPLAY_COMPASS_ROSE);
        if (rose && this.compassRose.isShowing()) ret.add(this.compassRose);
        if (this.agendaPanel.getParent() != null) ret.add(this.agendaPanel);
        if (this.guidePanel.getParent() != null) ret.add(this.guidePanel);
        if (this.stackPanel.getParent() != null) ret.add(this.stackPanel);
        for (UnitButton ub : this.unitButtons) {
            if (ub.isShowing()) ret.add(ub);
        }
        return ret;
    }
        
    /**
     * Should the agenda be shown?
     *
     * @return True if the agenda is wanted and there is a game to show
     *     it for.
     */
    private boolean showAgenda() {
        return getMyPlayer() != null
            && getClientOptions().getBoolean(ClientOptions.GUI_SHOW_AGENDA);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void update(GUI.ViewMode viewMode, Unit active, Tile tile) {
        super.update(viewMode, active, tile);
        // Things move on as units move and turns end
        if (this.guidePanel.getParent() != null) this.guidePanel.refresh();
        if (this.agendaPanel.getParent() != null) {
            // Hidden while the guide teaches the first steps
            final boolean wanted = showAgenda()
                && !this.guidePanel.hidesAgenda();
            this.agendaPanel.setVisible(wanted);
            if (wanted) this.agendaPanel.refresh();
            this.guidePanel.place();
        }

        // Only the orders the unit can carry out, side by side
        if (active != null && !this.unitButtons.isEmpty()) {
            for (UnitButton ub : this.unitButtons) {
                final Action a = ub.getAction();
                if (a instanceof FreeColAction) ((FreeColAction)a).update();
                ub.setVisible(a != null && a.isEnabled());
            }
            layoutVisibleUnitButtons();
        }
        updateStack(active);
    }

    /**
     * Lay out the unit buttons that are visible in one row, between
     * the mini map and the info panel.
     */
    private void layoutVisibleUnitButtons() {
        final Container parent = this.unitButtons.get(0).getParent();
        if (parent == null) return;
        final int gap = lib.scaleInt(5);
        final List<UnitButton> shown = new ArrayList<>();
        int width = -gap, height = 0;
        for (UnitButton ub : this.unitButtons) {
            if (!ub.isVisible()) continue;
            shown.add(ub);
            width += gap + ub.getWidth();
            height = Math.max(height, ub.getHeight());
        }
        final int left = this.miniMapPanel.getWidth();
        final int right = this.infoPanel.getX();
        if (shown.isEmpty() || width > right - left) return;
        int x = left + (right - left - width) / 2;
        final int y = parent.getHeight() - height - gap;
        for (UnitButton ub : shown) {
            ub.setLocation(x, y);
            x += gap + ub.getWidth();
        }
    }

    /**
     * Show the units standing with the active unit, when there is more
     * than one, above the order buttons: a click picks one.
     *
     * @param active The active {@code Unit}, if any.
     */
    private void updateStack(Unit active) {
        this.stackPanel.removeAll();
        final Container parent = this.stackPanel.getParent();
        final List<Unit> units = (active == null || !active.hasTile()
            || active.isOnCarrier()) ? List.of()
            : active.getTile().getUnitList();
        final List<Unit> own = new ArrayList<>();
        for (Unit u : units) {
            if (getMyPlayer() != null && getMyPlayer().owns(u)) own.add(u);
        }
        if (parent == null || own.size() < 2) {
            this.stackPanel.setVisible(false);
            return;
        }
        for (Unit u : own) {
            final java.awt.image.BufferedImage image
                = lib.getSmallerUnitImage(u);
            final JLabel label = new JLabel(new javax.swing.ImageIcon(image));
            final boolean moves = u.getMovesLeft() > 0;
            label.setEnabled(moves);
            label.setToolTipText(Messages.message(StringTemplate
                .template("mapControls.stackUnit")
                .addStringTemplate("%unit%",
                    u.getLabel(Unit.UnitLabelType.NATIONAL))
                .addName("%moves%", u.getMovesAsString())));
            label.setBorder((u == active)
                ? javax.swing.BorderFactory.createLineBorder(
                    new java.awt.Color(240, 200, 90), 2)
                : javax.swing.BorderFactory.createEmptyBorder(2, 2, 2, 2));
            label.setCursor(java.awt.Cursor.getPredefinedCursor(
                    java.awt.Cursor.HAND_CURSOR));
            label.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override
                    public void mouseClicked(java.awt.event.MouseEvent e) {
                        getGUI().changeView(u, true);
                    }
                });
            this.stackPanel.add(label);
        }
        this.stackPanel.setSize(this.stackPanel.getPreferredSize());
        int top = parent.getHeight();
        for (UnitButton ub : this.unitButtons) {
            if (ub.isVisible()) top = Math.min(top, ub.getY());
        }
        final int left = this.miniMapPanel.getWidth();
        final int right = this.infoPanel.getX();
        this.stackPanel.setLocation(
            left + (right - left - this.stackPanel.getWidth()) / 2,
            top - this.stackPanel.getHeight() - lib.scaleInt(4));
        this.stackPanel.setVisible(true);
        this.stackPanel.revalidate();
        this.stackPanel.repaint();
    }

    private boolean isShowingOrIconified(JComponent panel) {
        final JInternalFrame f = (JInternalFrame) SwingUtilities.getAncestorOfClass(JInternalFrame.class, panel);
        if (f != null && f.isIcon()) {
            return true;
        }
        return panel.isShowing();
    }
}
