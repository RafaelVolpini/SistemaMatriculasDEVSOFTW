package br.pucminas.matriculas.externo;

import br.pucminas.matriculas.model.Aluno;
import br.pucminas.matriculas.model.Disciplina;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * &lt;&lt;external&gt;&gt; Simulação do Sistema de Cobrança, que fica fora do sistema de matrículas.
 *
 * <p>Singleton porque {@code Disciplina.matricular(aluno, tipo)} não recebe a cobrança como parâmetro no
 * diagrama. Simplificação consciente: é estado global, então os testes chamam {@link #limpar()} antes de cada caso.
 */
public final class SistemaCobranca {

    private static final SistemaCobranca INSTANCE = new SistemaCobranca();

    /** Última lista cobrada de cada aluno — permite ao aviso dizer o que entrou/saiu da cobrança. */
    private final Map<String, List<String>> ultimaCobranca = new HashMap<>();

    private SistemaCobranca() {
    }

    public static SistemaCobranca getInstance() {
        return INSTANCE;
    }

    public synchronized void notificarCobranca(Aluno aluno, List<Disciplina> disciplinas) {
        List<String> codigos = disciplinas.stream().map(Disciplina::getCodigo).toList();
        List<String> anteriores = ultimaCobranca.getOrDefault(aluno.getId(), List.of());

        List<String> incluidas = new ArrayList<>(codigos);
        incluidas.removeAll(anteriores);
        List<String> removidas = new ArrayList<>(anteriores);
        removidas.removeAll(codigos);
        ultimaCobranca.put(aluno.getId(), codigos);

        StringBuilder mensagem = new StringBuilder("Cobrança de ")
                .append(aluno.getNome()).append(" (RA ").append(aluno.getRegistroAcademico()).append("): ");
        if (codigos.isEmpty()) {
            mensagem.append("nenhuma disciplina a cobrar neste semestre");
        } else {
            mensagem.append(String.join(", ", codigos))
                    .append(" — ").append(codigos.size()).append(codigos.size() == 1 ? " disciplina" : " disciplinas");
        }
        if (!incluidas.isEmpty()) {
            mensagem.append(". Incluída: ").append(String.join(", ", incluidas));
        }
        if (!removidas.isEmpty()) {
            mensagem.append(". Removida da cobrança: ").append(String.join(", ", removidas));
        }

        Notificacao.Nivel nivel = removidas.isEmpty() ? Notificacao.Nivel.INFO : Notificacao.Nivel.ALERTA;
        MuralNotificacoes.getInstance().publicar(Notificacao.ORIGEM_COBRANCA, nivel, mensagem.toString());
    }

    /** Recalcula a "última cobrança" de cada aluno ao carregar os dados salvos. */
    public synchronized void sincronizar(List<Aluno> alunos) {
        ultimaCobranca.clear();
        ultimaCobranca.putAll(alunos.stream().collect(Collectors.toMap(
                Aluno::getId, a -> a.disciplinasAtivas().stream().map(Disciplina::getCodigo).toList())));
    }

    public synchronized void limpar() {
        ultimaCobranca.clear();
    }
}
