package com.kotomichi.db

import android.content.Context
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

object DbFactory {
    fun create(context: Context): KotomichiDatabase {
        val driver = AndroidSqliteDriver(
            schema = KotomichiDatabase.Schema,
            context = context,
            name = "kotomichi.db"
        )
        return KotomichiDatabase(driver)
    }
}