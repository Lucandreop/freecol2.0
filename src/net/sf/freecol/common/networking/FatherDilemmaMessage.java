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

package net.sf.freecol.common.networking;

import javax.xml.stream.XMLStreamException;

import net.sf.freecol.client.FreeColClient;
import net.sf.freecol.common.io.FreeColXMLReader;
import net.sf.freecol.common.model.FoundingFather;
import net.sf.freecol.common.model.Game;
import net.sf.freecol.server.FreeColServer;
import net.sf.freecol.server.model.ServerPlayer;


/**
 * The message sent to offer the bold proposal of a founding father who
 * has just joined the congress, and to answer it.
 */
public class FatherDilemmaMessage extends AttributeMessage {

    public static final String TAG = "fatherDilemma";
    private static final String FATHER_TAG = "father";
    private static final String RESULT_TAG = "result";


    /**
     * Create a new {@code FatherDilemmaMessage} offering a proposal.
     *
     * @param father The {@code FoundingFather} making the proposal.
     */
    public FatherDilemmaMessage(FoundingFather father) {
        super(TAG, FATHER_TAG, father.getId());
    }

    /**
     * Create a new {@code FatherDilemmaMessage} from a stream.
     *
     * @param game The {@code Game} this message belongs to.
     * @param xr The {@code FreeColXMLReader} to read from.
     * @exception XMLStreamException if the stream is corrupt.
     */
    public FatherDilemmaMessage(Game game, FreeColXMLReader xr)
        throws XMLStreamException {
        super(TAG, xr, FATHER_TAG, RESULT_TAG);
    }


    /**
     * Get the father making the proposal.
     *
     * @param game The {@code Game} to look the father up in.
     * @return The {@code FoundingFather}, or null if not found.
     */
    private FoundingFather getFather(Game game) {
        return game.getSpecification()
            .getFoundingFather(getStringAttribute(FATHER_TAG));
    }

    /**
     * Set the answer to the proposal.
     *
     * @param accept True if the proposal is accepted.
     * @return This message.
     */
    public FatherDilemmaMessage setResult(boolean accept) {
        setStringAttribute(RESULT_TAG, Boolean.toString(accept));
        return this;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public boolean currentPlayerMessage() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessagePriority getPriority() {
        return Message.MessagePriority.LATE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clientHandler(FreeColClient freeColClient) {
        final FoundingFather father = getFather(freeColClient.getGame());
        if (father != null) igc(freeColClient).fatherDilemmaHandler(father);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ChangeSet serverHandler(FreeColServer freeColServer,
                                   ServerPlayer serverPlayer) {
        final FoundingFather father = getFather(freeColServer.getGame());
        if (father == null) {
            return serverPlayer.clientError("Bogus father: "
                + getStringAttribute(FATHER_TAG));
        }
        return igc(freeColServer).fatherDilemma(serverPlayer, father,
            getBooleanAttribute(RESULT_TAG, Boolean.FALSE));
    }
}
