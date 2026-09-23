package br.pucminas.matriculas.externo;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Histórico de avisos (do Sistema de Cobrança e do próprio sistema) que a interface consome como toasts.
 * Singleton pelo mesmo motivo do SistemaCobranca: o domínio o alcança sem mudar as assinaturas modeladas.
 */
public final class MuralNotificacoes {

    private static final MuralNotificacoes INSTANCE = new MuralNotificacoes();

    private final List<Notificacao> notificacoes = new ArrayList<>();
    private long proximoId = 1;
    private int silenciadas = -1;

    private MuralNotificacoes() {
    }

    public static MuralNotificacoes getInstance() {
        return INSTANCE;
    }

    public synchronized Notificacao publicar(String origem, Notificacao.Nivel nivel, String mensagem) {
        Notificacao notificacao = new Notificacao(proximoId++, origem, nivel, mensagem, LocalDateTime.now());
        System.out.println("[" + origem + "] " + mensagem);
        if (silenciadas >= 0) {
            silenciadas++;
            return notificacao;
        }
        notificacoes.add(notificacao);
        return notificacao;
    }

    /** Executa a ação sem gerar toasts (usado ao criar dezenas de alunos de teste); devolve quantos avisos foram omitidos. */
    public synchronized <T> Silenciado<T> silenciar(Supplier<T> acao) {
        silenciadas = 0;
        try {
            T resultado = acao.get();
            return new Silenciado<>(resultado, silenciadas);
        } finally {
            silenciadas = -1;
        }
    }

    public synchronized List<Notificacao> desde(long idExclusivo) {
        return notificacoes.stream().filter(n -> n.id() > idExclusivo).toList();
    }

    public synchronized List<Notificacao> ultimas(int quantidade) {
        int inicio = Math.max(0, notificacoes.size() - quantidade);
        return List.copyOf(notificacoes.subList(inicio, notificacoes.size()));
    }

    public synchronized List<Notificacao> todas() {
        return List.copyOf(notificacoes);
    }

    public synchronized long ultimoId() {
        return proximoId - 1;
    }

    public synchronized void restaurar(List<Notificacao> salvas) {
        notificacoes.clear();
        notificacoes.addAll(salvas);
        proximoId = salvas.stream().mapToLong(Notificacao::id).max().orElse(0) + 1;
    }

    public synchronized void limpar() {
        notificacoes.clear();
        proximoId = 1;
        silenciadas = -1;
    }

    public record Silenciado<T>(T resultado, int avisosOmitidos) {
    }
}
