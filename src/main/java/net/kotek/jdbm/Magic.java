/*******************************************************************************
 * Copyright 2010 Cees De Groot, Alex Boisvert, Jan Kotek
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package net.kotek.jdbm;

/**
 * This interface contains magic cookies.
 */
interface Magic {
    /**
     * Magic cookie at start of file
     */
    short FILE_HEADER = 0x1350;

    /**
     * Magic for blocks. They're offset by the block type magic codes.
     */
    short BLOCK = 0x1351;

    /**
     * Magics for blocks in certain lists. Offset by baseBlockMagic
     */
    short FREE_PAGE = 0;
    short USED_PAGE = 1;
    short TRANSLATION_PAGE = 2;
    short FREELOGIDS_PAGE = 3;
    short FREEPHYSIDS_PAGE = 4;

    /**
     * Number of lists in a file
     */
    short NLISTS = 5;

    /**
     * Magic for transaction file
     */
    short LOGFILE_HEADER = 0x1360;

    /**
     * Size of an externalized byte
     */
    short SZ_BYTE = 1;
    /**
     * Size of an externalized short
     */
    short SZ_SHORT = 2;

    /**
     * Size of an externalized int
     */
    short SZ_INT = 4;
    /**
     * Size of an externalized long
     */
    short SZ_LONG = 8;

    /**
     * size of three byte integer
     */
    short SZ_SIX_BYTE_LONG = 6;


    /**offsets in file header (zero page in file)*/
    short FILE_HEADER_O_MAGIC = 0; // short magic
    short FILE_HEADER_O_LISTS = Magic.SZ_SHORT; // long[2*NLISTS]
    int FILE_HEADER_O_ROOTS = FILE_HEADER_O_LISTS + (Magic.NLISTS * 2 * Magic.SZ_LONG);
    /**
     * The number of "root" rowids available in the file.
     */
    int FILE_HEADER_NROOTS = (Storage.BLOCK_SIZE - FILE_HEADER_O_ROOTS) / Magic.SZ_LONG;


    short PAGE_HEADER_O_MAGIC = 0; // short magic
    short PAGE_HEADER_O_NEXT = Magic.SZ_SHORT;
    short PAGE_HEADER_O_PREV = PAGE_HEADER_O_NEXT + Magic.SZ_SIX_BYTE_LONG;
    short PAGE_HEADER_SIZE = PAGE_HEADER_O_PREV + Magic.SZ_SIX_BYTE_LONG;

    short PhysicalRowId_O_LOCATION = 0; // long block
//    short PhysicalRowId_O_OFFSET = Magic.SZ_SIX_BYTE_LONG; // short offset
    int PhysicalRowId_SIZE = Magic.SZ_SIX_BYTE_LONG;
    
    short DATA_PAGE_O_FIRST = PAGE_HEADER_SIZE; // short firstrowid
    short DATA_PAGE_O_DATA = (short) (DATA_PAGE_O_FIRST + Magic.SZ_SHORT);
    short DATA_PER_PAGE = (short) (Storage.BLOCK_SIZE - DATA_PAGE_O_DATA);


    short FreePhysicalRowId_O_SIZE = Magic.PhysicalRowId_SIZE; // int size
    short FreePhysicalRowId_SIZE = FreePhysicalRowId_O_SIZE + Magic.SZ_INT;

    // offsets
    short FreePhysicalRowId_O_COUNT = Magic.PAGE_HEADER_SIZE; // short count
    short FreePhysicalRowId_O_FREE = FreePhysicalRowId_O_COUNT + Magic.SZ_SHORT;
    short FreePhysicalRowId_ELEMS_PER_PAGE = (short) ((Storage.BLOCK_SIZE - FreePhysicalRowId_O_FREE) / FreePhysicalRowId_SIZE);


    // offsets
    short FreeLogicalRowId_O_COUNT = Magic.PAGE_HEADER_SIZE; // short count
    /** keeps track of the most recent found free slot so we can locate it again quickly*/
    short FreeLogicalRowId_O_LAST_FREE = (short) (FreeLogicalRowId_O_COUNT + Magic.SZ_SHORT);
    /**keeps track of the most recent found allocated slot so we can locate it again quickly*/
    short FreeLogicalRowId_O_LAST_ALOC = (short) (FreeLogicalRowId_O_LAST_FREE + Magic.SZ_SHORT);
    short FreeLogicalRowId_O_FREE = (short) (FreeLogicalRowId_O_LAST_ALOC + Magic.SZ_SHORT);
    short FreeLogicalRowId_ELEMS_PER_PAGE = (short) ((Storage.BLOCK_SIZE - FreeLogicalRowId_O_FREE) / Magic.PhysicalRowId_SIZE);




}
