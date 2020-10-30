SELECT
COUNT(BDCHAVE)
FROM VSUC_EMPRESAS_TLAN
WHERE
BDCODEMP = :enterprise
AND BDDATA < ':date'
AND (BDDEBITO = :account OR BDCREDITO = :account)
AND (:participant is NULL OR (:participant is not NULL AND (BDCODTERCEIROC = :participant OR BDCODTERCEIROD = :participant)))
