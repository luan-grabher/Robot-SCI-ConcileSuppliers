SELECT
LIST(BDCHAVE)
FROM VSUC_EMPRESAS_TLAN
WHERE
BDCODEMP = :enterprise
AND BDDATA < ':date'
AND (BDDEBITO = :account OR BDCREDITO = :account)
AND (:conciled is NULL or (:conciled is not NULL AND(BDCONCC = :conciled or BDCONCD = :conciled)))
AND (:participant is NULL OR (:participant is not NULL AND (BDCODTERCEIROC = :participant OR BDCODTERCEIROD = :participant)))
