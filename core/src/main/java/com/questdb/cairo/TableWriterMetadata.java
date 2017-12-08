/*******************************************************************************
 *    ___                  _   ____  ____
 *   / _ \ _   _  ___  ___| |_|  _ \| __ )
 *  | | | | | | |/ _ \/ __| __| | | |  _ \
 *  | |_| | |_| |  __/\__ \ |_| |_| | |_) |
 *   \__\_\\__,_|\___||___/\__|____/|____/
 *
 * Copyright (C) 2014-2017 Appsicle
 *
 * This program is free software: you can redistribute it and/or  modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 ******************************************************************************/

package com.questdb.cairo;

import com.questdb.common.AbstractRecordMetadata;
import com.questdb.common.RecordColumnMetadata;
import com.questdb.std.CharSequenceIntHashMap;
import com.questdb.std.FilesFacade;
import com.questdb.std.ObjList;

public class TableWriterMetadata extends AbstractRecordMetadata {
    private final ObjList<TableColumnMetadata> columnMetadata;
    private final CharSequenceIntHashMap columnNameIndexMap = new CharSequenceIntHashMap();
    private int columnCount;
    private final int timestampIndex;

    public TableWriterMetadata(FilesFacade ff, ReadOnlyMemory metaMem) {
        TableUtils.validate(ff, metaMem, columnNameIndexMap);
        this.columnCount = metaMem.getInt(TableUtils.META_OFFSET_COUNT);
        this.timestampIndex = metaMem.getInt(TableUtils.META_OFFSET_TIMESTAMP_INDEX);
        this.columnMetadata = new ObjList<>(this.columnCount);

        long offset = TableUtils.getColumnNameOffset(columnCount);

        // don't create strings in this loop, we already have them in columnNameIndexMap
        for (int i = 0; i < columnCount; i++) {
            CharSequence name = metaMem.getStr(offset);
            int index = columnNameIndexMap.keyIndex(name);
            columnMetadata.add(new TableColumnMetadata(columnNameIndexMap.keyAt(index).toString(), TableUtils.getColumnType(metaMem, i)));
            offset += ReadOnlyMemory.getStorageLength(name);
        }
    }

    @Override
    public int getColumnCount() {
        return columnCount;
    }

    @Override
    public int getColumnIndexQuiet(CharSequence name) {
        return columnNameIndexMap.get(name);
    }

    @Override
    public RecordColumnMetadata getColumnQuick(int index) {
        return columnMetadata.getQuick(index);
    }

    @Override
    public int getTimestampIndex() {
        return timestampIndex;
    }

    void addColumn(CharSequence name, int type) {
        String str = name.toString();
        columnNameIndexMap.put(str, columnMetadata.size());
        columnMetadata.add(new TableColumnMetadata(str, type));
        columnCount++;
    }

    void removeColumn(CharSequence name) {
        int index = columnNameIndexMap.keyIndex(name);
        int columnIndex = columnNameIndexMap.valueAt(index);
        columnMetadata.remove(columnIndex);
        columnNameIndexMap.removeAt(index);
        columnCount--;

        // enumerate columns that would have moved up after column deletion
        for (int i = columnIndex; i < columnCount; i++) {
            columnNameIndexMap.put(columnMetadata.getQuick(i).getName(), i);
        }
    }
}