/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services;

import com.github.britooo.looca.api.core.Looca;
import com.github.britooo.looca.api.group.processos.Processo;
import com.github.britooo.looca.api.group.processos.ProcessoGrupo;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import com.github.britooo.looca.api.group.sistema.Sistema;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import repositories.ProcessosRepository;

/**
 *
 * @author murilo
 */
public class KillProcessosService {

    Looca looca = new Looca();
    Sistema sistema = looca.getSistema();
    ProcessoGrupo grupoDeProcessos = looca.getGrupoDeProcessos();
    ProcessosRepository execute = new ProcessosRepository();

    public void monitorar(Integer idAtm, Integer idEmpresa) {
        // Verificar sistema operacional
        Boolean isLinux;
        if (sistema.getSistemaOperacional().equals("Windows")) {
            isLinux = false;
        } else {
            isLinux = true;
        }

        // ========= Matar processos a cada x segundos ===========
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                // Verificar processos permitidos do banco
                List<String> processosPermitidos = execute.processosPermitidos(idEmpresa);

                // Leitura dos processos atuais
                List<Processo> processosLido = grupoDeProcessos.getProcessos();

                for (Processo processoLido : processosLido) {
                    // se o processo começar com kworker/ não matar
                    if (processoLido.getNome().startsWith("kworker/")) {
                        continue;
                    }

                    // Se o processo não for econtrado, executar kill
                    if (!processosPermitidos.contains(processoLido.getNome())) {
                        String comando = isLinux
                                ? "pkill -f " + processoLido.getNome()
                                : "TASKKILL /F /IM " + processoLido.getNome() + ".exe";
                        try {
                            Runtime.getRuntime().exec(comando);
                            
                            ZonedDateTime horarioBrasilia = ZonedDateTime.now(ZoneId.of("America/Sao_Paulo"));
                            LocalDateTime dtMetrica = horarioBrasilia.toLocalDateTime();

                            System.out.println("\nProcesso não permitido encontrado e morto!");
                            System.out.println("\nNome: " + processoLido.getNome());
                            System.out.println("DataHora: " + dtMetrica);

                            execute.cadastrarProcessoKilled(idAtm, processoLido, dtMetrica);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }

                }
            }
        }, 0, 2000);
    }

}
