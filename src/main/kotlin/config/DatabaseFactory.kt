package com.wellnessapp.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

object DatabaseFactory {
    fun init() {
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = EnvConfig.databaseUrl
            username = EnvConfig.databaseUser
            password = EnvConfig.databasePassword
            driverClassName = "org.postgresql.Driver"
            maximumPoolSize = 10
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_READ_COMMITTED"
            connectionTimeout = 10_000 // fail fast (10s) instead of hanging on network issues
            addDataSourceProperty("sslmode", "require") // Supabase pooler requires SSL
            validate()
        }
        val dataSource = HikariDataSource(hikariConfig)
        Database.connect(dataSource)
    }
}
