package br.pucminas.matriculas.externo;

import java.time.LocalDateTime;

/** Aviso exibido como toast na interface e guardado no histórico. */
public record Notificacao(long id, String origem, Nivel nivel, String mensagem, LocalDateTime dataHora) {

    public static final String ORIGEM_COBRANCA = "Sistema Cobranca";
    public static final String ORIGEM_SISTEMA = "Sistema";

    public enum Nivel { INFO, SUCESSO, ALERTA, ERRO }
}
