/*
 * UnauthorizedClassicSector.java          ler
 *
 * Authors:
 * Eric Butler <eric@codebutler.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.codebutler.farebot.card.classic;

import com.codebutler.farebot.card.UnauthorizedException;

import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

@Root(name="sector")
public class UnauthorizedClassicSector extends ClassicSector {
    @Attribute(name="unauthorized") public static final boolean mUnauthorized = true;

    private UnauthorizedClassicSector() { /** For XML serializer **/ }

    public UnauthorizedClassicSector(int sectorIndex) {
        super(sectorIndex, null);
    }

    @Override public byte[] readBlocks(int startBlock, int blockCount) {
        throw new UnauthorizedException();
    }

    @Override public java.util.List<ClassicBlock> getBlocks() {
        throw new UnauthorizedException();
    }

    @Override public ClassicBlock getBlock(int index) {
        throw new UnauthorizedException();
    }
}
