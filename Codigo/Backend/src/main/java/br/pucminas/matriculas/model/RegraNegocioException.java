package br.pucminas.matriculas.model;

/** Violação de uma regra de negócio (limite de vagas, período fechado, etc.). */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
