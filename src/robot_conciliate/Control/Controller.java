package robot_conciliate.Control;

import Auxiliar.Valor;
import Dates.Dates;
import Entity.Executavel;
import Entity.Warning;
import SimpleDotEnv.Env;
import fileManager.FileManager;
import robot_conciliate.Model.ChangeEntries;
import robot_conciliate.Model.ConciliateContabilityEntries;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import lctocontabil.Entity.ContabilityEntry;
import lctocontabil.Model.ContabilityEntries_Model;
import sql.Database;

public class Controller {

    //---------------------------------------------------------------------------------------------------------------------
    private final Integer enterprise;
    private final Integer account;
    private final Integer participant;
    private final Calendar startDate;
    private final Calendar endDate;
    private final String databaseCfgFilePath = Env.get("databaseCfgFilePath");

    private final Map<Integer, ContabilityEntry> entries = new HashMap<>();

    public Controller(Integer enterprise, Integer account, Integer participant, Calendar startDate, Calendar finalDate) {
        this.enterprise = enterprise;
        this.account = account;
        this.participant = participant;
        this.startDate = startDate;
        this.endDate = finalDate;
    }

    /**
     * Define o banco de dados estático, se houver problema ao conectar aplica
     * um Error
     */
    public class setDatabase extends Executavel {

        @Override
        public void run() {
            Database.setStaticObject(new Database(FileManager.getFile(databaseCfgFilePath)));

            if (!Database.getDatabase().testConnection()) {
                throw new Error("Erro ao conectar ao banco de dados!");
            }
        }
    }

    /**
     * Pega lançamentos do banco de dados com a empresa, conta, participante
     * entre as datas informadas
     */
    public class getDatabaseEntries extends Executavel {

        @Override
        public void run() {
            entries.clear();

            String sql = FileManager.getText(FileManager.getFile("sql\\selectContabilityEntries.sql"));

            Map<String, String> swaps = new HashMap<>();
            swaps.put("enterprise", enterprise.toString());
            swaps.put("account", account.toString());
            swaps.put("participant", participant == null ? "NULL" : participant.toString());
            swaps.put("dateStart", Dates.getCalendarInThisStringFormat(startDate, "YYYY-MM-dd"));
            swaps.put("dateEnd", Dates.getCalendarInThisStringFormat(endDate, "YYYY-MM-dd"));

            entries.putAll(ContabilityEntries_Model.getEntries(sql, swaps));
            
            if(entries.isEmpty()){
                throw new Error("Nenhum lançamento encontrado no banco!");
            }
        }
    }

    /**
     * Define os números de documentos de todos os lançamentos
     */
    public class setEntriesDocuments extends Executavel {

        @Override
        public void run() {
            ChangeEntries.setDocument(entries);
        }
    }

    /**
     * Atualiza os lançamentos no banco de dados
     */
    public class updateEntriesOnDatabase extends Executavel {

        @Override
        public void run() {
            ContabilityEntries_Model.updateContabilityEntriesOnDatabase(entries);
        }
    }

    /**
     * Zera a conciliação dos lançamentos
     */
    public class remakeConciliate extends Executavel {

        @Override
        public void run() {
            ContabilityEntries_Model.defineConciliatedsTo(entries, Boolean.FALSE);
        }
    }

    
    /** 
    * Concilia os lançamentos
    */
    public class conciliateEntries extends Executavel {

        @Override
        public void run() {
            ConciliateContabilityEntries model = new ConciliateContabilityEntries(entries, enterprise, account, participant, startDate, endDate);
            System.out.println("Definindo predicatos padrões");
            model.setDefaultPredicates();
            System.out.println("Criando lista de participantes e documentos");
            model.createParticipantAndDcoumentList();
            System.out.println("Conciliando participantes");
            model.conciliateParticipants();
            throw new Warning(model.getInfos());
        }
    }
}