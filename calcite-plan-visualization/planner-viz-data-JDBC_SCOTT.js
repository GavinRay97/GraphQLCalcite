var data = {
  "steps":[ {
    "id":"INITIAL",
    "updates":{
      "set-0":{
        "label":"set-0",
        "kind":"set"
      },
      "4":{
        "label":"subset#4-set#0-\nJDBC.JDBC_SCOTT.[]",
        "explanation":"{subset=rel#4:RelSubset#0.JDBC.JDBC_SCOTT.[]}",
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
      "6":{
        "label":"subset#6-set#1-\nNONE.[]",
        "explanation":"{subset=rel#6:RelSubset#1.NONE.[]}",
        "set":"set-1",
        "kind":"subset",
        "inputs":[ "5" ]
      },
      "5":{
        "label":"#5-LogicalFilter",
        "explanation":"{condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "inputs":[ "4" ]
      },
      "set-2":{
        "label":"set-2",
        "kind":"set"
      },
      "8":{
        "label":"subset#8-set#2-\nNONE.[]",
        "explanation":"{subset=rel#8:RelSubset#2.NONE.[]}",
        "set":"set-2",
        "kind":"subset",
        "inputs":[ "7" ]
      },
      "7":{
        "label":"#7-LogicalSort",
        "explanation":"{offset=1, fetch=2}",
        "set":"set-2",
        "inputs":[ "6" ]
      },
      "9":{
        "label":"subset#9-set#2-\nENUMERABLE.[]",
        "explanation":"{subset=rel#9:RelSubset#2.ENUMERABLE.[], offset=1, fetch=2}",
        "set":"set-2",
        "kind":"subset",
        "inputs":[ "10" ]
      },
      "10":{
        "label":"#10-AbstractConverter",
        "explanation":"{convention=ENUMERABLE, sort=[]}",
        "set":"set-2",
        "inputs":[ "8" ]
      }
    },
    "matchedRels":[ ]
  }, {
    "id":"17-JdbcToEnumerableConverterRule(in:JDBC.JDBC_SCOTT,out:ENUMERABLE)",
    "updates":{
      "13":{
        "label":"subset#13-set#0-\nENUMERABLE.[]",
        "explanation":"{subset=rel#13:RelSubset#0.ENUMERABLE.[], table=[JDBC_SCOTT, EMP]}",
        "set":"set-0",
        "kind":"subset",
        "inputs":[ "12" ]
      },
      "12":{
        "label":"#12-JdbcToEnumerableConverter",
        "explanation":"{}",
        "set":"set-0",
        "inputs":[ "4" ]
      }
    },
    "matchedRels":[ "0" ]
  }, {
    "id":"43-EnumerableFilterRule(in:NONE,out:ENUMERABLE)",
    "updates":{
      "15":{
        "label":"subset#15-set#1-\nENUMERABLE.[]",
        "explanation":"{subset=rel#15:RelSubset#1.ENUMERABLE.[], condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "kind":"subset",
        "inputs":[ "14" ]
      },
      "14":{
        "label":"#14-EnumerableFilter",
        "explanation":"{condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "inputs":[ "13" ]
      }
    },
    "matchedRels":[ "5" ]
  }, {
    "id":"48-JdbcFilterRule(in:NONE,out:JDBC.JDBC_SCOTT)",
    "updates":{
      "17":{
        "label":"subset#17-set#1-\nJDBC.JDBC_SCOTT.[]",
        "explanation":"{subset=rel#17:RelSubset#1.JDBC.JDBC_SCOTT.[], condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "kind":"subset",
        "inputs":[ "16" ]
      },
      "16":{
        "label":"#16-JdbcFilter",
        "explanation":"{condition=AND(SEARCH($7, Sarg[20, 30]), >=($5, 1500), SEARCH($2, Sarg['MANAGER':CHAR(8), 'SALESMAN']:CHAR(8)))}",
        "set":"set-1",
        "inputs":[ "4" ]
      }
    },
    "matchedRels":[ "5" ]
  }, {
    "id":"76-EnumerableLimitRule",
    "updates":{
      "9":{
        "inputs":[ "10", "18" ]
      },
      "18":{
        "label":"#18-EnumerableLimit",
        "explanation":"{offset=1, fetch=2}",
        "set":"set-2",
        "inputs":[ "15" ]
      }
    },
    "matchedRels":[ "7" ]
  }, {
    "id":"80-JdbcSortRule(in:NONE,out:JDBC.JDBC_SCOTT)",
    "updates":{
      "20":{
        "label":"subset#20-set#2-\nJDBC.JDBC_SCOTT.[]",
        "explanation":"{subset=rel#20:RelSubset#2.JDBC.JDBC_SCOTT.[], offset=1, fetch=2}",
        "set":"set-2",
        "kind":"subset",
        "inputs":[ "19" ]
      },
      "19":{
        "label":"#19-JdbcSort",
        "explanation":"{offset=1, fetch=2}",
        "set":"set-2",
        "inputs":[ "17" ]
      }
    },
    "matchedRels":[ "7" ]
  }, {
    "id":"123-JdbcToEnumerableConverterRule(in:JDBC.JDBC_SCOTT,out:ENUMERABLE)",
    "updates":{
      "15":{
        "inputs":[ "14", "22" ]
      },
      "22":{
        "label":"#22-JdbcToEnumerableConverter",
        "explanation":"{}",
        "set":"set-1",
        "inputs":[ "17" ]
      }
    },
    "matchedRels":[ "16" ]
  }, {
    "id":"148-EnumerableLimitRule",
    "updates":{ },
    "matchedRels":[ "19" ]
  }, {
    "id":"151-JdbcToEnumerableConverterRule(in:JDBC.JDBC_SCOTT,out:ENUMERABLE)",
    "updates":{
      "9":{
        "inputs":[ "10", "18", "25" ]
      },
      "25":{
        "label":"#25-JdbcToEnumerableConverter",
        "explanation":"{}",
        "set":"set-2",
        "inputs":[ "20" ]
      }
    },
    "matchedRels":[ "19" ]
  }, {
    "id":"FINAL",
    "updates":{
      "4":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 1E2\nrows: 1E2\ncpu:  1.01E2\nio:   0E0"
      },
      "0":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 1E2\nrows: 1E2\ncpu:  1.01E2\nio:   0E0"
      },
      "6":{
        "cost":"{inf}"
      },
      "5":{
        "cost":"{inf}"
      },
      "8":{
        "cost":"{inf}"
      },
      "7":{
        "cost":"{inf}"
      },
      "9":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 2E0\nrows: 1.05E2\ncpu:  2.03E2\nio:   0E0"
      },
      "10":{
        "cost":"{inf}"
      },
      "13":{
        "cost":"\nrowCount: 1E2\nrows: 1.1E2\ncpu:  1.11E2\nio:   0E0"
      },
      "12":{
        "cost":"\nrowCount: 1E2\nrows: 1.1E2\ncpu:  1.11E2\nio:   0E0"
      },
      "15":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "14":{
        "cost":"\nrowCount: 3E0\nrows: 1.13E2\ncpu:  2.11E2\nio:   0E0"
      },
      "17":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "16":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "18":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 2E0\nrows: 1.05E2\ncpu:  2.03E2\nio:   0E0"
      },
      "20":{
        "cost":"\nrowCount: 2E0\nrows: 1.06E2\ncpu:  3.2E2\nio:   0E0"
      },
      "19":{
        "cost":"\nrowCount: 2E0\nrows: 1.06E2\ncpu:  3.2E2\nio:   0E0"
      },
      "22":{
        "inFinalPlan":true,
        "cost":"\nrowCount: 3E0\nrows: 1.03E2\ncpu:  2.01E2\nio:   0E0"
      },
      "25":{
        "cost":"\nrowCount: 2E0\nrows: 1.06E2\ncpu:  3.2E2\nio:   0E0"
      }
    },
    "matchedRels":[ ]
  }, {
    "id":"FINAL",
    "updates":{
      "9":{
        "inputs":[ "10", "18", "25", "39" ]
      },
      "39":{
        "label":"#39-AbstractConverter",
        "explanation":"{convention=ENUMERABLE, sort=[]}",
        "set":"set-2",
        "cost":"{inf}",
        "inputs":[ "20" ]
      }
    },
    "matchedRels":[ ]
  } ]
};
