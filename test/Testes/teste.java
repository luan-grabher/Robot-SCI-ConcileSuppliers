package Testes;

import Auxiliar.Valor;
import robot_conciliate.ConciliarUnico;
import java.util.Calendar;

public class teste {

    public static void main(String[] args) {
        testeComPesquisa();
    }

    private static void testeComPesquisa() {
        Integer codEmpresa = 702;
        Integer contaCTB = 150;
        Calendar dataInicial = new Valor("01/01/2017").getCalendar();
        Calendar dataFinal = new Valor("01/01/2020").getCalendar();
        boolean zerarConciliacao = false;
        String tipoContaContabil = "conta ctb";

        
        System.out.println(
                ConciliarUnico.principal(
                        codEmpresa, contaCTB, tipoContaContabil, dataInicial, dataFinal, zerarConciliacao
                ).replaceAll("<br>", "\n")
        );
    }

}
