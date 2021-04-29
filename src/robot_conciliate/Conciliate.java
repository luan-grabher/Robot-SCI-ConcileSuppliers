package robot_conciliate;

import Dates.Dates;
import Entity.Executavel;
import Executor.Execution;
import Robo.AppRobo;
import java.util.ArrayList;
import robot_conciliate.Control.Controller;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Conciliate {

    private static String name;

    public static void main(String[] args) {
        try {
            //Inicia robô
            AppRobo app = new AppRobo("Conciliação Automática");

            //Define os parâmetros
            app.definirParametros();
            
            

            //Define variáveis
            Integer codEmpresa = Integer.valueOf("0" + app.getParametro("empresa"));
            Integer contaCTB = Integer.valueOf("0" + app.getParametro("contaCTB"));
            Integer participant = Integer.valueOf("0" + app.getParametro("participant"));
            participant = participant == 0 ? null : participant;

            List<Integer> accounts = new ArrayList<>();
            if (app.getParametro("contaCTB").contains(";")) {
                String[] contas = app.getParametro("contaCTB").split(";");

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
            if (participant != null && app.getParametro("participant").contains(";")) {
                String[] participantes = app.getParametro("participant").split(";");

                for (String participante : participantes) {
                    participante = participante.replaceAll("[^0-9]", ""); //Remove tudo que não for número
                    //Se a conta nao estiver em branco, contendo apenas numeros
                    if ("".equals(participante)) {
                        //Adiciona na lista de contas
                        participants.add(Integer.valueOf(participante));
                    }
                }
            }

            Calendar dataInicial = Dates.getCalendarFromFormat(app.getParametro("dataInicial"), "y-M-d");
            Calendar dataFinal = Dates.getCalendarFromFormat(app.getParametro("dataFinal"), "y-M-d");
            boolean zerarConciliacao = Boolean.valueOf(app.getParametro("zerarConciliacao"));

            String dataInicialStr = Dates.getCalendarInThisStringFormat(dataInicial, "dd/MM/yyyy");
            String dataFinalStr = Dates.getCalendarInThisStringFormat(dataFinal, "dd/MM/yyyy");

            name = "Conciliação Automática -- #" + codEmpresa + " conta '" + app.getParametro("contaCTB") + "' participante '" + app.getParametro("participant") + "' - " + dataInicialStr + " -> " + dataFinalStr + " | zerar concilação: " + zerarConciliacao;
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
        } catch (Exception e) {
        }

        System.exit(0);
    }

    /**
     * VÁRIOS PARTICIPANTES Método principal que gerencia a execução e o
     * controle.
     *
     * @param enterprise Empresa
     * @param account Conta
     * @param participants Lista de Participantes
     * @param dateStart inicio
     * @param dateEnd fim
     * @param remakeConciliate Refazer conciliação
     * @return Retorno das execuções de cada particiapante
     */
    public static String principal(Integer enterprise, Integer account, List<Integer> participants, Calendar dateStart, Calendar dateEnd, boolean remakeConciliate) {
        StringBuilder result = new StringBuilder();
        participants.forEach(participant -> {
            result.append("\n\n\nCONTA CONTÁBIL: ").append(account).append("\n");
            result.append(principal(enterprise, account, participant, dateStart, dateEnd, remakeConciliate));
        });
        return result.toString();
    }

    /**
     * VÁRIAS CONTAS Método principal que gerencia a execução e o controle.
     *
     * @param enterprise Empresa
     * @param accounts Lista de Contas
     * @param participant Particpante
     * @param dateStart inicio
     * @param dateEnd fim
     * @param remakeConciliate Refazer conciliação
     * @return Retorno das execuções de cada conta
     */
    public static String principal(Integer enterprise, List<Integer> accounts, Integer participant, Calendar dateStart, Calendar dateEnd, boolean remakeConciliate) {
        StringBuilder result = new StringBuilder();
        accounts.forEach(account -> {
            result.append("\n\n\nCONTA CONTÁBIL: ").append(account).append("\n");
            result.append(principal(enterprise, account, participant, dateStart, dateEnd, remakeConciliate));
        });
        return result.toString();
    }

    /**
     * SOMENTE UM PARTICIPANTE E UMA CONTA Método principal que gerencia a
     * execução e o controle.
     *
     * @param enterprise Empresa
     * @param account Conta
     * @param participant Particpante
     * @param dateStart inicio
     * @param dateEnd fim
     * @param remakeConciliate Refazer conciliação
     * @return Retorno das execução da conta e participante
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
