UPDATE VSUC_EMPRESAS_TLAN L
SET 
/*BDDATA = ':date',*/
/*BDDEBITO = :accountDebit,*/
/*BDCREDITO = :accountCredit,*/
/*BDCODTERCEIROD = :participantDebit,*/
/*BDCODTERCEIROC = :participantCredit,*/
/*BDCODHIST = :historyCode,*/
BDCOMPL = ':complement',
BDDCTO = ':document',
/*BDVALOR = :value,*/
BDCONCC = :conciliedCredit,
BDCONCD = :conciliedDebit
WHERE 
BDCHAVE = :key
AND BDCODEMP = :enterprise