package com.novoda.downloadmanager;

import android.database.Cursor;

import com.novoda.notils.logger.simple.Log;

import java.util.ArrayList;
import java.util.List;

class MigrationExtractor {

    private static final String BATCHES_QUERY = "SELECT batches._id, batches.batch_title FROM batches INNER JOIN DownloadsByBatch ON " +
            "DownloadsByBatch.batch_id = batches._id WHERE DownloadsByBatch.batch_total_bytes = DownloadsByBatch.batch_current_bytes";
    private static final String URIS_QUERY = "SELECT uri, _data, total_bytes FROM Downloads WHERE batch_id = ?";
    private final SqlDatabaseWrapper database;

    MigrationExtractor(SqlDatabaseWrapper database) {
        this.database = database;
    }

    List<Migration> extractMigrations() {
        Cursor batchesCursor = database.rawQuery(BATCHES_QUERY);

        List<Migration> migrations = new ArrayList<>();
        while (batchesCursor.moveToNext()) {

            String batchId = batchesCursor.getString(0);
            String batchTitle = batchesCursor.getString(1);

            Cursor uriCursor = database.rawQuery(URIS_QUERY, batchId);
            Batch.Builder newBatchBuilder = new Batch.Builder(DownloadBatchIdCreator.createFrom(batchId), batchTitle);

            List<String> originalFileLocations = new ArrayList<>();
            List<FileSize> fileSizes = new ArrayList<>();

            while (uriCursor.moveToNext()) {
                String uri = uriCursor.getString(0);
                String originalFileName = uriCursor.getString(1);
                Log.d("MainActivity", batchId + " : " + batchTitle + " : " + uri);

                newBatchBuilder.addFile(uri);

                originalFileLocations.add(originalFileName);

                long rawFileSize = uriCursor.getLong(2);
                FileSize fileSize = new LiteFileSize(rawFileSize, rawFileSize);
                fileSizes.add(fileSize);
            }

            uriCursor.close();

            Batch batch = newBatchBuilder.build();
            migrations.add(new Migration(batch, originalFileLocations, fileSizes));
        }
        batchesCursor.close();
        return migrations;
    }

}
