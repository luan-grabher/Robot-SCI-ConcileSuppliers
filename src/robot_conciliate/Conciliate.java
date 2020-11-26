package robot_conciliate;

import Dates.Dates;
import Entity.Executavel;
import Executor.Execution;
import Robo.AppRobo;
import SimpleDotEnv.Env;
import java.util.ArrayList;
import robot_conciliate.Control.Controller;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Conciliate {

    private static String name;
    public static String path = "\\\\zac\\Robos\\Tarefas\\Todas Empresas\\";

    public static void main(String[] args) {
        //Inicia robô
        AppRobo app = new AppRobo("Conciliação Automática");

        //Define WorkPath
        Env.setPath(path);

        //Define os parâmetros
        app.definirParametros();

        //Define variáveis
        Integer codEmpresa = app.getParametro("empresa").getInteger();
        Integer contaCTB = app.getParametro("contaCTB").getInteger();
        Integer participant = app.getParametro("participant").getInteger();
        participant = participant == 0 ? null : participant;

        List<Integer> accounts = new ArrayList<>();
        if (app.getParametro("contaCTB").getString().contains(";")) {
            String[] contas = app.getParametro("contaCTB").getString().split(";");

            for (String conta : contas) {
                conta = conta.replaceAll("[^0-9]", ""); //Remove tudo que não for número
                //Se a conta nao estiver em branco, contendo apenas numeros
                if ("".equals(conta)) {
                    //Adiciona na lista de contas
                    accounts.add(Integer.valueOf(conta));
                }
            }
        }

        List<Integer> participants = new ArrayList<>();
        if (participant != null && app.getParametro("participant").getString().contains(";")) {
            String[] participantes = app.getParametro("participant").getString().split(";");

            for (String participante : participantes) {
                participante = participante.replaceAll("[^0-9]", ""); //Remove tudo que não for número
                //Se a conta nao estiver em branco, contendo apenas numeros
                if ("".equals(participante)) {
                    //Adiciona na lista de contas
                    participants.add(Integer.valueOf(participante));
                }
            }
        }

        Calendar dataInicial = app.getParametro("dataInicial").getCalendar("ymd");
        Calendar dataFinal = app.getParametro("dataFinal").getCalendar("ymd");
        boolean zerarConciliacao = app.getParametro("zerarConciliacao").getBoolean();

        String dataInicialStr = Dates.getCalendarInThisStringFormat(dataInicial, "dd/MM/yyyy");
        String dataFinalStr = Dates.getCalendarInThisStringFormat(dataFinal, "dd/MM/yyyy");

        name = "Conciliação Automática -- #" + codEmpresa + " conta " + app.getParametro("contaCTB").getString() + " participante " + app.getParametro("participant").getString() + " " + dataInicialStr + " -> " + dataFinalStr;
        app.setNome(name);

        //Se a Lista de contas estiver vazia e só tiver uma conta
        if (accounts.isEmpty()) {
            if (participants.isEmpty()) {
                //Executa uma conta
                app.executar(
                        principal(codEmpresa, contaCTB, participant, dataInicial, dataFinal, zerarConciliacao)
                );
            } else {
                //Executa uma conta e vários participantes
                app.executar(
                        principal(codEmpresa, contaCTB, participants, dataInicial, dataFinal, zerarConciliacao)
                );
            }
        } else {
            //Executa varias contas
            app.executar(
                    principal(codEmpresa, accounts, participant, dataInicial, dataFinal, zerarConciliacao)
            );
        }

        System.exit(0);
    }

    /**
     * VÁRIOS PARTICIPANTES Método principal que gerencia a execução e o
     * controle.
     *
     */
    public static String principal(Integer enterprise, Integer account, List<Integer> participants, Calendar dateStart, Calendar dateEnd, boolean remakeConciliate) {
        StringBuilder result = new StringBuilder();
        for (Integer participant : participants) {
            result.append("\n\n\nCONTA CONTÁBIL: ").append(account).append("\n");
            result.append(principal(enterprise, account, participant, dateStart, dateEnd, remakeConciliate));
        }
        return result.toString();
    }

    /**
     * VÁRIAS CONTAS Método principal que gerencia a execução e o controle.
     *
     */
    public static String principal(Integer enterprise, List<Integer> accounts, Integer participant, Calendar dateStart, Calendar dateEnd, boolean remakeConciliate) {
        StringBuilder result = new StringBuilder();
        for (Integer account : accounts) {
            result.append("\n\n\nCONTA CONTÁBIL: ").append(account).append("\n");
            result.append(principal(enterprise, account, participant, dateStart, dateEnd, remakeConciliate));
        }
        return result.toString();
    }

    /**
     * SOMENTE UM PARTICIPANTE E UMA CONTA Método principal que gerencia a
     * execução e o controle.
     *
     */
    public static String principal(Integer enterprise, Integer account, Integer participant, Calendar dateStart, Calendar dateEnd, boolean remakeConciliate) {

        //Se empresa for maior que zero
        if (enterprise == null || enterprise <= 0) {
            return "[ERRO] Código de empresa inválido.";
        }
        //Se conta contabil for maior que zero
        if (account == null || account <= 0) {
            return "[ERRO] Conta contábil inválida.";
        }

        //Verifica se data inicial é menor que data final
        if (dateStart.compareTo(dateEnd) < 0) {
            //Define Controle
            Controller control = new Controller(enterprise, account, participant, dateStart, dateEnd);

            //Definir funções executadas
            Map<String, Executavel> executables = new LinkedHashMap<>();

            executables.put("Conectando ao banco de dados", control.new setDatabase());

            executables.put("Buscando lançamentos no banco", control.new getDatabaseEntries());
            if (remakeConciliate) {
                executables.put("Zerando conciliação", control.new remakeConciliate());
            }
            executables.put("Definir números de documento", control.new setEntriesDocuments());
            executables.put("Conciliar lançamentos", control.new conciliateEntries());
            executables.put("Atualizar lançamentos no banco de dados", control.new updateEntriesOnDatabase());

            //Execução
            Execution execution = new Execution(name);
            execution.setShowMessages(false);
            execution.setExecutionMap(executables);
            execution.runExecutables();

            execution.endExecution(false);
            return execution.getRetorno();
        } else {
            return "[ERRO] A data inicial deve ser menor que a final!";
        }
    }

}
