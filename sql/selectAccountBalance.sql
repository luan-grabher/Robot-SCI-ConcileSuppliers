    select
        SUM(L.BDVALOR) as SALDO
    from VSUC_EMPRESAS_TLAN L
    WHERE
    L.BDCODEMP = :enterprise AND
    L.BDDATA <= ':date' AND
    L.BDDEBITO = :account AND
    (:participant is NULL OR (:participant is not NULL AND (BDCODTERCEIROD = :participant))) AND
    CAST(L.BDCONCC AS VARCHAR(10)) <> 'TRUE' AND CAST(L.BDCONCD AS VARCHAR(10)) <> 'TRUE'

union

    select
        SUM(L.BDVALOR) as SALDO
    from VSUC_EMPRESAS_TLAN L 
    where 
    l.BDCODEMP = :enterprise AND
    l.BDDATA <= ':date' AND
    l.BDCREDITO = :account AND
    (:participant is NULL OR (:participant is not NULL AND (BDCODTERCEIROC = :participant))) AND
    CAST(L.BDCONCC AS VARCHAR(10)) <> 'TRUE' AND CAST(L.BDCONCD AS VARCHAR(10)) <> 'TRUE'