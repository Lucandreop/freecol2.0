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

package net.sf.freecol.client.control;

import static net.sf.freecol.common.util.CollectionUtils.any;
import static net.sf.freecol.common.util.CollectionUtils.count;
import static net.sf.freecol.common.util.CollectionUtils.max;
import static net.sf.freecol.common.util.CollectionUtils.sum;
import static net.sf.freecol.common.util.StringUtils.getEnumKey;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;
import java.util.logging.Logger;

import net.sf.freecol.client.ClientOptions;
import net.sf.freecol.common.io.FreeColDirectories;
import net.sf.freecol.common.model.Colony;
import net.sf.freecol.common.model.Europe;
import net.sf.freecol.common.model.GoodsType;
import net.sf.freecol.common.model.Market;
import net.sf.freecol.common.model.ModelMessage;
import net.sf.freecol.common.model.ModelMessage.MessageType;
import net.sf.freecol.common.model.Player;
import net.sf.freecol.common.model.Tile;
import net.sf.freecol.common.model.Unit;


/**
 * The colonial advisor, which helps new players learn the game.
 *
 * Following how recent strategy games teach (introduce each system
 * when it first matters, rather than all at once), the advisor:
 *
 * - gives short tips, at most one a turn, the first time something
 *   becomes relevant, and never twice;
 * - tracks a list of first objectives, and congratulates the player
 *   when one is reached;
 * - warns about serious problems the game does not already warn
 *   about (an undefended colony with enemies close, too many
 *   royalists, colonists stuck on the docks), which is useful even
 *   to experienced players.
 *
 * The decisions are static and depend only on the player, so they can
 * be tested without a client.
 */
public final class Advisor {

    private static final Logger logger = Logger.getLogger(Advisor.class.getName());

    /** Turns before the same warning is repeated. */
    private static final int WARNING_INTERVAL = 5;

    /** Distance at which enemies threaten an undefended colony. */
    private static final int THREAT_RANGE = 2;


    /** The tips, in the order they are given when several apply. */
    public static enum Tip {
        WELCOME(p -> true),
        FIRST_COLONY(p -> p.getSettlementCount() > 0),
        FOOD(p -> any(p.getColonyList(), c -> c.getNetProductionOf(
                    p.getSpecification().getPrimaryFoodType()) < 0)),
        NATIVES(p -> any(p.getGame().getLiveNativePlayers(),
                         n -> n.hasContacted(p))),
        EUROPE(p -> p.getSettlementCount() > 0
            && p.getGame().getTurn().getNumber() >= 5),
        FOUNDING_FATHER(p -> p.getCurrentFather() != null),
        LIBERTY(p -> any(p.getColonyList(), c -> c.getSonsOfLiberty() > 0)),
        TRADE_ROUTE(p -> p.getSettlementCount() >= 2),
        TAX(p -> p.getTax() > 0),
        WAR(p -> any(p.getGame().getLivePlayers(p), o -> p.atWarWith(o))),
        INDEPENDENCE(p -> p.isColonial() && p.getSoL() >= 50);

        private final Predicate<Player> applies;

        Tip(Predicate<Player> applies) {
            this.applies = applies;
        }

        public boolean appliesTo(Player player) {
            return this.applies.test(player);
        }

        public String getKey() {
            return "tutorial.tip." + getEnumKey(this);
        }
    }

    /** The first objectives, in the order they are usually reached. */
    public static enum Objective {
        FOUND_COLONY(Player::getSettlementCount, 1),
        MEET_NATIVES(p -> (int)count(p.getGame().getLiveNativePlayers(),
                                     n -> n.hasContacted(p)), 1),
        SELL_IN_EUROPE(Advisor::countGoodsSold, 1),
        THREE_COLONIES(Player::getSettlementCount, 3),
        FIRST_FATHER(Player::getFatherCount, 1),
        TEN_COLONISTS(p -> sum(p.getColonyList(), Colony::getUnitCount), 10),
        HALF_REBELS(p -> max(p.getColonyList(), Colony::getSonsOfLiberty), 50),
        INDEPENDENCE(p -> (p.isColonial()) ? 0 : 1, 1);

        private final ToIntFunction<Player> progress;
        private final int target;

        Objective(ToIntFunction<Player> progress, int target) {
            this.progress = progress;
            this.target = target;
        }

        /**
         * Get the progress of a player towards this objective.
         *
         * @param player The {@code Player} to check.
         * @return The progress, capped at the target.
         */
        public int getProgress(Player player) {
            return Math.min(this.target, this.progress.applyAsInt(player));
        }

        public int getTarget() {
            return this.target;
        }

        public boolean isComplete(Player player) {
            return getProgress(player) >= this.target;
        }

        public String getKey() {
            return "tutorial.objective." + getEnumKey(this);
        }
    }


    /** The game the state below belongs to. */
    private UUID gameId = null;

    /** The objectives already complete, or null before the first check. */
    private Set<Objective> completed = null;

    /** The turn each warning was last given, by warning key. */
    private final Map<String, Integer> warned = new HashMap<>();


    /**
     * Count the kinds of goods a player has sold in Europe.
     *
     * @param player The {@code Player} to check.
     * @return The number of goods types with sales.
     */
    private static int countGoodsSold(Player player) {
        final Market market = player.getMarket();
        if (market == null) return 0;
        int n = 0;
        for (GoodsType type : player.getSpecification().getGoodsTypeList()) {
            if (market.getSales(type) > 0) n++;
        }
        return n;
    }

    /**
     * Get the next tip to give.
     *
     * @param player The {@code Player} to advise.
     * @param seen The keys of the tips already given.
     * @return The first {@code Tip} that applies and was not given
     *     yet, or null if none.
     */
    public static Tip nextTip(Player player, Set<String> seen) {
        for (Tip tip : Tip.values()) {
            if (!seen.contains(tip.getKey()) && tip.appliesTo(player)) {
                return tip;
            }
        }
        return null;
    }

    /**
     * Find the problems worth warning a player about.
     *
     * @param player The {@code Player} to advise.
     * @return A map from a key identifying each problem (so it is not
     *     repeated every turn) to the message describing it.
     */
    public static Map<String, ModelMessage> findWarnings(Player player) {
        Map<String, ModelMessage> result = new HashMap<>();
        for (Colony colony : player.getColonyList()) {
            final Tile tile = colony.getTile();
            if (!any(tile.getUnitList(), Unit::isDefensiveUnit)) {
                for (Tile t : tile.getSurroundingTiles(1, THREAT_RANGE)) {
                    if (!player.canSee(t)) continue;
                    if (any(t.getUnitList(), u -> !player.owns(u)
                            && u.isOffensiveUnit() && !u.isNaval()
                            && player.atWarWith(u.getOwner()))) {
                        result.put("undefended." + colony.getId(),
                            new ModelMessage(MessageType.ADVISOR,
                                "advisor.warning.undefended", colony)
                                .addName("%colony%", colony.getName()));
                        break;
                    }
                }
            }
            if (colony.getProductionBonus() < 0) {
                result.put("tories." + colony.getId(),
                    new ModelMessage(MessageType.ADVISOR,
                        "advisor.warning.tories", colony)
                        .addName("%colony%", colony.getName())
                        .addAmount("%bonus%", colony.getProductionBonus()));
            }
        }

        final Europe europe = player.getEurope();
        if (europe != null && player.getSettlementCount() > 0) {
            List<Unit> units = europe.getUnitList();
            int waiting = (int)count(units, u -> !u.isNaval());
            if (waiting >= 2 && !any(units, Unit::isNaval)) {
                result.put("docks", new ModelMessage(MessageType.ADVISOR,
                        "advisor.warning.docks", player)
                    .addAmount("%number%", waiting));
            }
        }

        if (player.isColonial() && player.getSoL() >= 50) {
            result.put("independence", new ModelMessage(MessageType.ADVISOR,
                    "advisor.warning.independence", player)
                .addAmount("%number%", player.getSoL()));
        }
        return result;
    }

    /**
     * Get the keys of the tips a player has already been given.
     *
     * @param options The {@code ClientOptions} remembering them.
     * @return The set of tip keys.
     */
    public static Set<String> getSeenTips(ClientOptions options) {
        String text = options.getText(ClientOptions.TUTORIAL_TIPS_SEEN);
        Set<String> result = new LinkedHashSet<>();
        if (text != null && !text.isEmpty()) {
            result.addAll(Arrays.asList(text.split(",")));
        }
        return result;
    }


    /**
     * Advise the player at the start of a turn.  Any advice is added to
     * the player's messages, to be shown with the turn report.
     *
     * @param options The {@code ClientOptions} to consult.
     * @param player The {@code Player} to advise.
     */
    public void startTurn(ClientOptions options, Player player) {
        final UUID id = player.getGame().getUUID();
        if (!id.equals(this.gameId)) {
            this.gameId = id;
            this.completed = null;
            this.warned.clear();
        }
        final int turn = player.getGame().getTurn().getNumber();

        if (options.getBoolean(ClientOptions.GUI_SHOW_TUTORIAL)) {
            Set<String> seen = getSeenTips(options);
            Tip tip = nextTip(player, seen);
            if (tip != null) {
                player.addModelMessage(new ModelMessage(MessageType.TUTORIAL,
                        tip.getKey(), player));
                seen.add(tip.getKey());
                options.setText(ClientOptions.TUTORIAL_TIPS_SEEN,
                                String.join(",", seen));
                if (!options.save(FreeColDirectories.getClientOptionsFile())) {
                    logger.warning("Could not save the tips seen");
                }
            }

            Set<Objective> done = EnumSet.noneOf(Objective.class);
            for (Objective o : Objective.values()) {
                if (o.isComplete(player)) done.add(o);
            }
            if (this.completed != null) {
                for (Objective o : done) {
                    if (this.completed.contains(o)) continue;
                    player.addModelMessage(new ModelMessage(
                            MessageType.TUTORIAL,
                            "tutorial.objective.done", player)
                        .add("%objective%", o.getKey() + ".name"));
                }
            }
            this.completed = done;
        }

        if (options.getBoolean(ClientOptions.GUI_SHOW_ADVISOR)) {
            for (Map.Entry<String, ModelMessage> e
                     : findWarnings(player).entrySet()) {
                Integer last = this.warned.get(e.getKey());
                if (last != null && turn - last < WARNING_INTERVAL) continue;
                this.warned.put(e.getKey(), turn);
                player.addModelMessage(e.getValue());
            }
        }
    }
}
