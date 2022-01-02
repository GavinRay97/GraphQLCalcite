var data = {
  "steps":[ {
    "id":"INITIAL",
    "updates":{
      "set-0":{
        "label":"set-0",
        "kind":"set"
      },
      "3":{
        "label":"subset#3-set#0-\nJDBC.JDBC_SCOTT.[]",
        "explanation":"{subset=rel#3:RelSubset#0.JDBC.JDBC_SCOTT.[]}",
        "set":"set-0",
        "kind":"subset",
        "inputs":[ "0" ]
      },
      "0":{
        "label":"#0-JdbcTableScan",
        "explanation":"{table=[JDBC_SCOTT, EMP]}",
        "set":"set-0"
      },
      "set-1":{
        "label":"set-1",
        "kind":"set"
      },
      "5":{
        "label":"subset#5-set#1-\nNONE.[]",
        "explanation":"{subset=rel#5:RelSubset#1.NONE.[]}",
        "set":"set-1",
        "kind":"subset",
        "inputs":[ "4" ]
      },
      "4":{
        "label":"#4-LogicalFilter",
        "explanation":"{condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "inputs":[ "3" ]
      },
      "6":{
        "label":"subset#6-set#1-\nENUMERABLE.[]",
        "explanation":"{subset=rel#6:RelSubset#1.ENUMERABLE.[], condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "kind":"subset",
        "inputs":[ "7" ]
      },
      "7":{
        "label":"#7-AbstractConverter",
        "explanation":"{convention=ENUMERABLE, sort=[]}",
        "set":"set-1",
        "inputs":[ "5" ]
      }
    },
    "matchedRels":[ ]
  }, {
    "id":"17-JdbcToEnumerableConverterRule(in:JDBC.JDBC_SCOTT,out:ENUMERABLE)",
    "updates":{
      "10":{
        "label":"subset#10-set#0-\nENUMERABLE.[]",
        "explanation":"{subset=rel#10:RelSubset#0.ENUMERABLE.[], table=[JDBC_SCOTT, EMP]}",
        "set":"set-0",
        "kind":"subset",
        "inputs":[ "9" ]
      },
      "9":{
        "label":"#9-JdbcToEnumerableConverter",
        "explanation":"{}",
        "set":"set-0",
        "inputs":[ "3" ]
      }
    },
    "matchedRels":[ "0" ]
  }, {
    "id":"43-EnumerableFilterRule(in:NONE,out:ENUMERABLE)",
    "updates":{
      "6":{
        "inputs":[ "7", "11" ]
      },
      "11":{
        "label":"#11-EnumerableFilter",
        "explanation":"{condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "inputs":[ "10" ]
      }
    },
    "matchedRels":[ "4" ]
  }, {
    "id":"48-JdbcFilterRule(in:NONE,out:JDBC.JDBC_SCOTT)",
    "updates":{
      "13":{
        "label":"subset#13-set#1-\nJDBC.JDBC_SCOTT.[]",
        "explanation":"{subset=rel#13:RelSubset#1.JDBC.JDBC_SCOTT.[], condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "kind":"subset",
        "inputs":[ "12" ]
      },
      "12":{
        "label":"#12-JdbcFilter",
        "explanation":"{condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "inputs":[ "3" ]
      }
    },
    "matchedRels":[ "4" ]
  }, {
    "id":"92-JdbcToEnumerableConverterRule(in:JDBC.JDBC_SCOTT,out:ENUMERABLE)",
    "updates":{
      "6":{
        "inputs":[ "7", "11", "15" ]
      },
      "15":{
        "label":"#15-JdbcToEnumerableConverter",
        "explanation":"{}",
        "set":"set-1",
        "inputs":[ "13" ]
      }
    },
    "matchedRels":[ "12" ]
  }, {
    "id":"FINAL",
    "updates":{
      "3":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 1E2\nrows: 1E2\ncpu:  1.01E2\nio:   0E0"
      },
      "0":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 1E2\nrows: 1E2\ncpu:  1.01E2\nio:   0E0"
      },
      "5":{
        "cost":"{inf}"
      },
      "4":{
        "cost":"{inf}"
      },
      "6":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "7":{
        "cost":"{inf}"
      },
      "10":{
        "cost":"\nrowCount: 1E2\nrows: 1.1E2\ncpu:  1.11E2\nio:   0E0"
      },
      "9":{
        "cost":"\nrowCount: 1E2\nrows: 1.1E2\ncpu:  1.11E2\nio:   0E0"
      },
      "11":{
        "cost":"\nrowCount: 3E0\nrows: 1.13E2\ncpu:  2.11E2\nio:   0E0"
      },
      "13":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "12":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "15":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      }
    },
    "matchedRels":[ ]
  }, {
    "id":"FINAL",
    "updates":{
      "6":{
        "inputs":[ "7", "11", "15", "25" ]
      },
      "25":{
        "label":"#25-AbstractConverter",
        "explanation":"{convention=ENUMERABLE, sort=[]}",
        "set":"set-1",
        "cost":"{inf}",
        "inputs":[ "13" ]
      }
    },
    "matchedRels":[ ]
  } ]
};
