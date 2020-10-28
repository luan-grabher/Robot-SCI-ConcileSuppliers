UPDATE VSUC_EMPRESAS_TLAN L
SET 
BDCONCC = :concilited
BDCONCD = :concilited
WHERE 
:keysInList
AND BDCODEMP = :enterprise