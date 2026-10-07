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

package net.sf.freecol.common.model;

import static net.sf.freecol.common.util.StringUtils.getEnumKey;


/**
 * How far a native nation trusts a European player.
 *
 * Trust runs from 0 to {@link #MAXIMUM}.  It grows with trade, gifts,
 * missions and lasting peace, and falls with stolen land, anger and
 * war.  Each level unlocks the benefits of the ones below it:
 *
 * - PARTNER: better trade prices and land at half price.
 * - ALLY: no more tribute demands; on reaching it the settlements
 *   teach again and the tribe reveals its lands.
 * - INTEGRATION: settlements near the player's colonies send
 *   volunteers, and small ones may join a colony altogether, as free
 *   colonists.
 */
public enum TrustLevel {
    NONE(0),
    PARTNER(25),
    ALLY(60),
    INTEGRATION(90);

    /** The highest trust value. */
    public static final int MAXIMUM = 100;

    /** The trust needed to reach this level. */
    private final int threshold;


    TrustLevel(int threshold) {
        this.threshold = threshold;
    }

    /**
     * Get the trust needed to reach this level.
     *
     * @return The threshold.
     */
    public int getThreshold() {
        return this.threshold;
    }

    /**
     * Get the level for a trust value.
     *
     * @param value The trust value.
     * @return The highest {@code TrustLevel} reached.
     */
    public static TrustLevel fromValue(int value) {
        TrustLevel result = NONE;
        for (TrustLevel level : values()) {
            if (value >= level.threshold) result = level;
        }
        return result;
    }

    /**
     * Is this level at least as high as another?
     *
     * @param other The other {@code TrustLevel}.
     * @return True if this level is the same or higher.
     */
    public boolean isAtLeast(TrustLevel other) {
        return ordinal() >= other.ordinal();
    }

    /**
     * Get the message key for this level.
     *
     * @return A message key.
     */
    public String getKey() {
        return "model.trustLevel." + getEnumKey(this);
    }
}
