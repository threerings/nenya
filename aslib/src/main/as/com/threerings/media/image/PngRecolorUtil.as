//
// Nenya library - tools for developing networked games
// Copyright (C) 2002-2012 Three Rings Design, Inc., All Rights Reserved
// http://code.google.com/p/nenya/
//
// This library is free software; you can redistribute it and/or modify it
// under the terms of the GNU Lesser General Public License as published
// by the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA

package com.threerings.media.image {

import flash.utils.ByteArray;

import com.threerings.display.ColorUtil;
import com.threerings.util.Log;
import com.threerings.util.StringUtil;

import com.threerings.util.F;
public class PngRecolorUtil
{
    private static const log :Log = Log.getLog(PngRecolorUtil);

    protected static function isPng (bytes :ByteArray) :Boolean
    {
        if (bytes.length < PNG_HEADER.length) {
            return false;
        }
        for (var ii :int = 0; ii < PNG_HEADER.length; ii++) {
            if (PNG_HEADER[ii] != bytes[ii]) {
                return false;
            }
        }
        return true;
    }


    /**
     * Find the required chunks in the bytes that represent a PNG image.  All of the chunks that
     * are succesfully found are pushed into the returned array in the order in which they were
     * found in the image.
     */
    protected static function findChunks (pngBytes :ByteArray, chunkTypes :Array) :Array
    {
        var chunks :Array = [];
        if (chunkTypes.length < 1) {
            return chunks;
        }

        for (var ii :int = PNG_HEADER.length; ii < pngBytes.length;) {
            pngBytes.position = ii;
            var chunk :PngChunk =
                new PngChunk(ii + 8, pngBytes.readInt(), pngBytes.readUTFBytes(4));

            var idx :int = chunkTypes.indexOf(chunk.type);
            if (idx >= 0) {
                chunks.push(chunk);
                if (chunkTypes.length == 1) {
                    return chunks;
                } else {
                    chunkTypes.splice(idx, 1);
                }
            }

            // 4 bytes each for length, chunk type and CRC
            ii += 12 + chunk.length;
        }
        return chunks;
    }

    public static function recolorPNG (pngBytes :ByteArray,
        colors :Array/* of Colorization */) :ByteArray
    {
        if (colors == null || colors.length == 0) {
            return pngBytes;
        }

        // first, lets make sure we really have a PNG
        if (!isPng(pngBytes)) {
            log.warning("recolorPNG received invalid pngBytes", new Error());
            return pngBytes;
        }

        var chunks :Array = findChunks(pngBytes, [HEADER_CHUNK, PALETTE_CHUNK]);
        if (chunks.length != 2 || (chunks[0] as PngChunk).type != HEADER_CHUNK) {
            log.warning("recolorPNG received an unexected PNG format", "requiredChunksFound",
                chunks.length, new Error());
            return pngBytes;
        }
        var header :PngChunk = chunks[0] as PngChunk;
        var palette :PngChunk = chunks[1] as PngChunk;
        // Check to make sure the header declares this to be an indexed-color PNG
        var colorType :int = pngBytes[header.idx + COLOR_TYPE_IDX];
        if (colorType != INDEXED_COLOR_TYPE) {
            log.warning(
                "Color Type in PNG header is not indexed-color", "type", colorType, new Error());
            return pngBytes;
        }

        initializeCRCTable();
        // CRC starts with chunk type
        var crc :uint = 0xFFFFFFFF;
        for (var ii :int = 0; ii < 4; ii++) {
            crc = crcCalc(crc, PALETTE_CHUNK.charCodeAt(ii));
        }
        pngBytes.position = palette.idx;
        for (ii = palette.idx; ii < palette.idx + palette.length; ii += 3) {
            var red :uint = pngBytes.readUnsignedByte();
            var green :uint = pngBytes.readUnsignedByte();
            var blue :uint = pngBytes.readUnsignedByte();

            var hsv :Array = ColorUtil.RGBtoHSB(red, green, blue);
            var fhsv :Array = Colorization.toFixedHSV(hsv);
            for each (var color :Colorization in colors) {
                if (color != null && color.matches(hsv, fhsv)) {
                    var newRgb :uint = color.recolorColor(hsv);
                    pngBytes[ii] = red = ColorUtil.getRed(newRgb);
                    pngBytes[ii + 1] = green = ColorUtil.getGreen(newRgb);
                    pngBytes[ii + 2] = blue = ColorUtil.getBlue(newRgb);
                    break;
                }
            }

            crc = F.foldl([red, green, blue], crc, crcCalc);
        }
        crc = uint(crc ^ uint(0xFFFFFFFF));
        // write out the CRC for the modified color table
        pngBytes.position = palette.idx + palette.length;
        pngBytes.writeUnsignedInt(crc);

        return pngBytes;
    }

    protected static function initializeCRCTable () :void
    {
        if (_crcTable != null) {
            return;
        }

        _crcTable = [];
        for (var n :uint = 0; n < 256; n++)
        {
            var c :uint = n;
            for (var k :uint = 0; k < 8; k++)
            {
                if (c & 1) {
                    c = uint(uint(0xedb88320) ^ uint(c >>> 1));
                } else {
                    c = uint(c >>> 1);
                }
            }
            _crcTable[n] = c;
        }
    }

    protected static function crcCalc (crc :uint, byte :uint) :uint
    {
        return uint(_crcTable[(crc ^ byte) & uint(0xFF)] ^ uint(crc >>> 8));
    }

    /** The amount to pad error messages by. */
    private static const ERROR_PADDING :int = 5;

    /** Various Png-recolor related constants. */
    protected static const PNG_HEADER :Array =
        [ 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A ];
    protected static const PALETTE_CHUNK :String = "PLTE";
    protected static const HEADER_CHUNK :String = "IHDR";
    protected static const COLOR_TYPE_IDX :int = 9;
    protected static const INDEXED_COLOR_TYPE :int = 3;

    protected static var _crcTable :Array;
}
}

import com.threerings.util.StringUtil;

/** Used for recoloring pngs. */
class PngChunk
{
    /** The index of the beginning of the data section of the chunk. */
    public var idx :int;

    /** The length of the data section of the chunk. */
    public var length :int;

    /** The type of this chunk. */
    public var type :String;

    public function PngChunk (idx :int, length :int, type :String)
    {
        this.idx = idx;
        this.length = length;
        this.type = type;
    }

    public function toString () :String
    {
        return StringUtil.simpleToString(this);
    }
}