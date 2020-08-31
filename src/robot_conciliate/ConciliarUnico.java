package robot_conciliate;

import Auxiliar.Valor;
import Entity.Function;
import Executor.Execution;
import Robo.AppRobo;
import robot_conciliate.Control.Controller;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import lctocontabil.Entity.ComandosSqlUnico;

public class ConciliarUnico {

    private static String nomeRobo;

    public static void main(String[] args) {
        //Inicia robô
        AppRobo app = new AppRobo("Conciliação Automática");

        //Define os parâmetros
        app.definirParametros();

        //Define variáveis
        Integer codEmpresa = app.getParametro("empresa").getInteger();
        Integer contaCTB = app.getParametro("contaCTB").getInteger();
        Calendar dataInicial = app.getParametro("dataInicial").getCalendar("ymd");
        Calendar dataFinal = app.getParametro("dataFinal").getCalendar("ymd");
        boolean zerarConciliacao = app.getParametro("zerarConciliacao").getBoolean();
        String tipoContaContabil = app.getParametro("tipoContaContabil").getString();

        String dataInicialStr =  new Valor(dataInicial).getSQLDate();
        String dataFinalStr =  new Valor(dataFinal).getSQLDate();
        
        nomeRobo = "Conciliação Automática -- #" + codEmpresa + " conta " + contaCTB + " " + dataInicialStr + " -> " + dataFinalStr;
        app.setNome(nomeRobo);

        app.executar(principal(codEmpresa, contaCTB, tipoContaContabil, dataInicial, dataFinal, zerarConciliacao));

        System.exit(0);
    }
    
    public static String principal(Integer codEmpresa, Integer contaCTB, String tipoContaContabil, Calendar dataInicial, Calendar dataFinal, boolean zerarConciliacao) {

        return principal(codEmpresa, contaCTB, tipoContaContabil, dataInicial, dataFinal, zerarConciliacao, 0);
    }

    public static String principal(Integer codEmpresa, Integer contaCTB, String tipoContaContabil, Calendar dataInicial, Calendar dataFinal, boolean zerarConciliacao, int nroExecucao) {
        nroExecucao++;
        String r = "";
        //Se empresa for maior que zero
        if (codEmpresa <= 0) {
            return "[ERRO] Código de empresa inválido.";
        }
        //Se conta contabil for maior que zero
        if (contaCTB <= 0) {
            return "[ERRO] Conta contábil inválida.";
        }
        
        if (dataInicial.compareTo(dataFinal) < 0) {
            //Define Controle
            Controller controle = new Controller(new ComandosSqlUnico(), codEmpresa, contaCTB, tipoContaContabil, dataInicial, dataFinal);

            //Definir funções executadas
            List<Function> runs = new ArrayList<>();
            if (zerarConciliacao) {
                runs.add(new Function(
                        "Zerando Conciliação...", controle.new zerarConciliacaoNoBanco()
                ));
            }
            runs.add(new Function(
                    "Definir número de documento nos lançamentos",
                    controle.new definirNroDocumentoNosLancamentos()
            ));

            runs.add(new Function(
                    "Conciliar lançamentos",
                    controle.new conciliarLancamentos()
            ));

            runs.add(new Function(
                    "Atualizar banco de dados",
                    controle.new atualizarBancoDeDados()
            ));

            //Execução
            Execution execução = new Execution(nomeRobo, runs);
            execução.setMostrarMensagens(false);
            execução.rodarRunnables();
            execução.finalizar();

            //caso for a primeira execução, chama mais uma
            if (nroExecucao == 1) {
                r = principal(codEmpresa, contaCTB, tipoContaContabil, dataInicial, dataFinal, false, nroExecucao);
            }else{
                r = Controller.getResultado();
            }
        } else {
            r = "[ERRO] A data inicial deve ser menor que a final!";
        }
                
        return r;
    }

}
