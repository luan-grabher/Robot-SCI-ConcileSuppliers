package robot_conciliate.Model;

import Auxiliar.Valor;
import robot_conciliate.Control.Controller;
import java.time.Duration;
import java.util.Calendar;
import java.util.List;
import lctocontabil.Entity.ComandosSqlUnico;
import lctocontabil.Model.LctoContabil_Model;
import sql.Banco;

public class SaldoConta {

    private static final ComandosSqlUnico comandos = new ComandosSqlUnico();
    private static final String pathBancoSql = Controller.pathBancoSql;
    private static final Banco banco = new Banco(pathBancoSql);
    private static List<String[]> rs;

    private static final int TIPO_PARTICIPANTE = 1;
    private static final int TIPO_CONTACTB = 2;

    private static String colunaDebito;
    private static String colunaCredito;

    private static Integer codigoEmpresa;
    private static int conta;
    private static int tipoConta;

    private static Integer dataInicial;
    private static Integer dataFinal;

    public static final int SALDO_DIFERENTE = 0;
    public static final int SALDO_ZERADO = 1;
    public static final int SALDO_NULL = 2;

    public static void set(Integer codigoEmpresa, Integer conta, Integer tipoConta, Integer dataInicial, Integer dataFinal) {
        SaldoConta.codigoEmpresa = codigoEmpresa;
        SaldoConta.conta = conta;
        SaldoConta.tipoConta = tipoConta;
        SaldoConta.dataInicial = dataInicial;
        SaldoConta.dataFinal = dataFinal;

        definirFiltrosConta();
    }

    private static String retornaUltimaDataSaldoZerado() {
        //Percorrer os dias
        //--Encontra diferença entre os dias
        Calendar dataInicialCal = new Valor(new Valor(dataInicial).getIntegerToSql()).getCalendar("ymd");
        Calendar dataFinalCal = new Valor(new Valor(dataFinal).getIntegerToSql()).getCalendar("ymd");

        int diferencaDias = new Valor(Duration.between(dataInicialCal.toInstant(), dataFinalCal.toInstant()).toDays()).getInteger();

        //Define onde irá começar
        Calendar actualDate = Calendar.getInstance();
        actualDate.set(dataFinalCal.get(Calendar.YEAR), dataFinalCal.get(Calendar.MONTH), dataFinalCal.get(Calendar.DATE), 0, 0, 0);

        //Percorre todos os dias
        for (int i = diferencaDias; i >= 0; i--) {
            actualDate.add(Calendar.DATE, -1);

            //pega data sql
            String sqlDate = new Valor(actualDate).getSQLDate();

            int saldoNessaData = saldoNaData(sqlDate);
            if(saldoNessaData == SALDO_ZERADO) {
                return sqlDate;
            }else if(saldoNessaData == SALDO_NULL){
                return "";
            }
        }

        return "";
    }

    public static int saldoNaData(String sqlDate) {
        String sql = comandos.selectSaldoConta(codigoEmpresa, conta, sqlDate, colunaDebito, colunaCredito);

        rs = banco.select(sql);

        //Se tiver retornado só um resultado, os numeros são iguais, então está conciliado ou nulo 
        if (rs.size() == 1) {
            //Se diferente de null,está conciliado, se não não está conciliado
            if (!"null".equals(rs.get(0)[0])) {
                //Saldo zerado
                
                //Conciliar no banco os lctos anteriores à data inicial da pesquisa
                conciliarLctosAnterioresADataInicial(sqlDate);
                return SALDO_ZERADO;
            } else {
                return SALDO_NULL;
            }
        } else {
            return SALDO_DIFERENTE;
        }
    }
    
    private static void conciliarLctosAnterioresADataInicial(String sqlDate){
        //Pesquisa Lctos
        String selectChavesLctosAnteriores = comandos.selectChavesAnterioresAData(codigoEmpresa, conta, sqlDate, tipoConta);
        rs = banco.select(selectChavesLctosAnteriores);
        if(rs.size() == 1){
            //Pega chaves
            String chavesLctosAnteriores = rs.get(0)[0];
            
            //Inicia modelo
            LctoContabil_Model modeloLctoContabil = new LctoContabil_Model(comandos, pathBancoSql);
            modeloLctoContabil.conciliarLctosDaLista(codigoEmpresa, chavesLctosAnteriores, "TRUE");
        }
    }

    private static void definirFiltrosConta() {
        colunaCredito = tipoConta == TIPO_PARTICIPANTE ? "BDCODTERCEIROC" : "BDCREDITO";
        colunaDebito = tipoConta == TIPO_PARTICIPANTE ? "BDCODTERCEIROD" : "BDDEBITO";
    }
}
