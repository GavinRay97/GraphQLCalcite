import org.apache.calcite.jdbc.JavaTypeFactoryImpl
import org.intellij.lang.annotations.Language

// Create and export an instance of JavaTypeFactoryImpl here to avoid instantiating it every use
val JAVA_TYPE_FACTORY_IMPL = JavaTypeFactoryImpl()

@Suppress("SqlNoDataSourceInspection")
@Language("sql")
const val TEST_SQL_QUERY = """
        SELECT
            empid, name, salary, deptno, commission
        FROM
            emps
        WHERE
            deptno = 20
            AND
                (salary > 8000 AND salary < 10000)
            AND
                (name = 'Eric' OR commission = 10)
    """

@Language("graphql")
const val TEST_GRAPHQL_QUERY = """
        query {
            emps(
                where: {
                    _and: [
                        { deptno: { _eq: 20 } }
                        {
                            _and: [
                                { salary: { _gte: 8000 } },
                                { salary: { _lte: 10000 } }
                            ]
                        }
                    ],
                    _or: [
                        { name: { _eq: "Eric" } },
                        { commission: { _eq: 10 } }
                    ]
                }
            ) {
                   empid
                   deptno
                   name
                   salary
                   commission
            }
        }
        """

@Language("graphql")
const val TEST_GRAPHQL_QUERY_ORINOCO = """
        query {
            ORDERS(
                where: {
                    _and: [
                        { ID: { _lte: 3 } }
                    ]
                }
            ) {
                PRODUCT
                ID
                ROWTIME
                UNITS
           }
        }
        """

@Language("graphql")
const val TEST_GRAPHQL_QUERY_SCOTT = """
        query {
          EMP(
            where: {
              _or: [
                { DEPTNO: { _eq: 20 } },
                { DEPTNO: { _eq: 30 } }
              ]
              _and: [
                { SAL: { _gte: 1500 } }
                {
                    _or: [
                        { JOB: { _eq: "CLERK" } },
                        { JOB: { _eq: "MANAGER" } }
                    ]
                }
              ]
            }
          ) {
            EMPNO
            ENAME
            JOB
            MGR
            HIREDATE
            SAL
            COMM
            DEPTNO
          }
        }
        """

fun Any.prettyPrint(): String {
    var indentLevel = 0
    val indentWidth = 4

    fun padding() = "".padStart(indentLevel * indentWidth)

    val toString = toString()

    val stringBuilder = StringBuilder(toString.length)

    var i = 0
    while (i < toString.length) {
        when (val char = toString[i]) {
            '(', '[', '{' -> {
                indentLevel++
                stringBuilder.appendLine(char).append(padding())
            }
            ')', ']', '}' -> {
                indentLevel--
                stringBuilder.appendLine().append(padding()).append(char)
            }
            ',' -> {
                stringBuilder.appendLine(char).append(padding())
                // ignore space after comma as we have added a newline
                val nextChar = toString.getOrElse(i + 1) { char }
                if (nextChar == ' ') i++
            }
            else -> {
                stringBuilder.append(char)
            }
        }
        i++
    }

    return stringBuilder.toString()
}
