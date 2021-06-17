/*
 *  Twidere X
 *
 *  Copyright (C) 2020-2021 Tlaster <tlaster@outlook.com>
 * 
 *  This file is part of Twidere X.
 * 
 *  Twidere X is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  Twidere X is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with Twidere X. If not, see <http://www.gnu.org/licenses/>.
 */
package com.twidere.twiderex.db

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.twidere.twiderex.viewmodel.compose.ComposeType
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java,
    )

    @Test
    @Throws(IOException::class)
    fun migrate1To2() {
        val id = UUID.randomUUID().toString()
        val content = "Content"
        val createdAt = 1000L
        val composeType = ComposeType.New
        helper.createDatabase(TEST_DB, 1).apply {
            execSQL(
                "INSERT INTO draft (_id, content, createdAt, composeType, media) VALUES (?, ?, ?, ?, ?)",
                arrayOf(id, content, createdAt, composeType, "")
            )
            close()
        }

        helper.runMigrationsAndValidate(TEST_DB, 2, true, AppDatabase_Migration_1_2).apply {
            query(SimpleSQLiteQuery("SELECT * FROM draft WHERE _id = ?", arrayOf(id))).apply {
                moveToFirst()
                getString(getColumnIndex("_id")).also {
                    assert(it == id)
                }
                getString(getColumnIndex("content")).also {
                    assert(it == content)
                }
                getLong(getColumnIndex("createdAt")).also {
                    assert(it == createdAt)
                }
            }
            close()
        }
    }
}
