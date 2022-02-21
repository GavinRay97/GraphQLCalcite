import org.junit.jupiter.api.Test
import org.teiid.adminapi.impl.ModelMetaData
import org.teiid.runtime.EmbeddedConfiguration
import org.teiid.runtime.EmbeddedServer
import org.teiid.translator.jdbc.h2.H2ExecutionFactory
import java.util.Properties

class TeiidTest {

    @Test
    fun test() {
        val props = System.getProperties()
        // Need to set this property or else the following error happens:
        //
        // WARN: TEIID50036 VDB test.1 model "my-schema" metadata failed to load.
        // Reason:TEIID11029 More than one schema was imported from.
        // It is a best practice to import from only one schema - make sure the schemaPattern property is set or the schemaPattern import property is unique.
        props.setProperty("org.teiid.translator.jdbc.useFullSchemaNameDefault", "true")

        val es = EmbeddedServer()
        val ec = EmbeddedConfiguration()

        ec.setUseDisk(false)
        es.start(ec)

        val ef = H2ExecutionFactory()
        ef.setSupportsDirectQueryProcedure(true)
        ef.start()
        es.addTranslator("translator-h2", ef)

        val ds = org.h2.jdbcx.JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1")
            user = "sa"
            password = ""
        }
        es.addConnectionFactory("java:/accounts-ds", ds)

        ds.connection.createStatement().execute(
            """
            CREATE TABLE "my_table" (
                "id" INTEGER NOT NULL,
                "name" VARCHAR(255) NOT NULL,
                PRIMARY KEY ("id")
            );
        """
        )
        ds.connection.createStatement().execute(
            """
            INSERT INTO "my_table" VALUES (1, 'John');
        """
        )

        es.addConnectionFactory("java:/accounts-ds", ds)

        val mmd = ModelMetaData()
        mmd.name = "my-schema"
        mmd.addSourceMapping("my-schema", "translator-h2", "java:/accounts-ds")

        es.deployVDB("test", mmd)
        es.driver.connect("jdbc:teiid:test", Properties()).use {
            it.createStatement().use { stmt ->
                val rs = stmt.executeQuery("""SELECT * FROM "my_table";""")
                rs.use {
                    while (it.next()) {
                        val md = it.metaData
                        for (i in 1..md.columnCount) {
                            println(md.getColumnName(i) + ": " + it.getObject(i))
                        }
                        println()
                    }
                }
            }
        }
    }
}
