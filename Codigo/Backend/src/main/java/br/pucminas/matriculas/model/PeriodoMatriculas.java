package br.pucminas.matriculas.model;

import br.pucminas.matriculas.model.enums.StatusPeriodo;

import java.time.LocalDateTime;

public class PeriodoMatriculas {

    private LocalDateTime dataInicio;
    private LocalDateTime dataFim;
    private StatusPeriodo status;

    public PeriodoMatriculas(LocalDateTime dataInicio, LocalDateTime dataFim) {
        this(dataInicio, dataFim, StatusPeriodo.ENCERRADO);
    }

    public PeriodoMatriculas(LocalDateTime dataInicio, LocalDateTime dataFim, StatusPeriodo status) {
        if (!dataInicio.isBefore(dataFim)) {
            throw new RegraNegocioException("A data de início deve ser anterior à data de fim.");
        }
        this.dataInicio = dataInicio;
        this.dataFim = dataFim;
        this.status = status;
    }

    public void abrir() {
        status = StatusPeriodo.ABERTO;
    }

    public void fechar() {
        status = StatusPeriodo.ENCERRADO;
    }

    /** Aberto = status ABERTO e o instante atual dentro da janela [dataInicio, dataFim]. */
    public boolean estaAberto() {
        LocalDateTime agora = LocalDateTime.now();
        return status == StatusPeriodo.ABERTO && !agora.isBefore(dataInicio) && !agora.isAfter(dataFim);
    }

    /** Período aberto cuja data de fim já passou — deve ser encerrado automaticamente. */
    public boolean venceu() {
        return status == StatusPeriodo.ABERTO && LocalDateTime.now().isAfter(dataFim);
    }

    public LocalDateTime getDataInicio() {
        return dataInicio;
    }

    public LocalDateTime getDataFim() {
        return dataFim;
    }

    public StatusPeriodo getStatus() {
        return status;
    }
}
