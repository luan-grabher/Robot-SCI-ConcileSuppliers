SELECT
*
FROM VSUC_EMPRESAS_TLAN L
WHERE
BDCODEMP = :enterprise
AND BDDATA >= ':dateStart' AND BDDATA <= ':dateEnd'
AND (l.BDCREDITO = :account OR L.BDDEBITO = :account)
AND (:participant is NULL OR (:participant is not NULL AND (BDCODTERCEIROC = :participant OR BDCODTERCEIROD = :participant)))
