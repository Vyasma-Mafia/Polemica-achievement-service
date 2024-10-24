package com.github.mafia.vyasma.polemicaachivementservice

import liquibase.Contexts
import liquibase.LabelExpression
import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.exception.LiquibaseException
import liquibase.resource.DirectoryResourceAccessor
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.JdbcDatabaseContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import java.io.FileNotFoundException
import java.nio.file.Path
import java.sql.SQLException

@Testcontainers
object IntegrationTest {
    class MyPostgreSQLContainer(imageName: String) : PostgreSQLContainer<MyPostgreSQLContainer>(imageName)

    var POSTGRES: MyPostgreSQLContainer = MyPostgreSQLContainer("postgres:15")
        .withDatabaseName("achievement")
        .withUsername("postgres")
        .withPassword("postgres")

    init {
        POSTGRES.start()

        runMigrations(POSTGRES)
    }

    private fun runMigrations(c: JdbcDatabaseContainer<*>) {
        val migrationsPath = Path.of("../migrations")

        try {
            val connection = JdbcConnection(c.createConnection(""))
            val liquibase = Liquibase(
                "master.xml",
                DirectoryResourceAccessor(migrationsPath),
                DatabaseFactory.getInstance().findCorrectDatabaseImplementation(
                    connection
                )
            )

            liquibase.update(Contexts(), LabelExpression())
        } catch (e: LiquibaseException) {
            throw RuntimeException("Error running migrations", e)
        } catch (e: SQLException) {
            throw RuntimeException("Error running migrations", e)
        } catch (e: FileNotFoundException) {
            throw RuntimeException("Error running migrations", e)
        }
    }

    @DynamicPropertySource
    fun jdbcProperties(registry: DynamicPropertyRegistry) {
        registry.add("spring.datasource.url") { POSTGRES.jdbcUrl }
        registry.add("spring.datasource.username") { POSTGRES.username }
        registry.add("spring.datasource.password") { POSTGRES.password }
    }
}
