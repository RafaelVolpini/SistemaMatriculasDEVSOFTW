package br.pucminas.matriculas.model;

import br.pucminas.matriculas.model.enums.StatusPeriodo;

import java.util.ArrayList;
import java.util.List;

public class Semestre {

    private String identificador;
    private List<Disciplina> disciplinasOfertadas = new ArrayList<>();

    // composição "Possui" (Semestre 1 ◆— 1 PeriodoMatriculas)
    private PeriodoMatriculas periodo;

    public Semestre(String identificador) {
        this.identificador = identificador;
    }

    public void gerarCurriculo(List<Disciplina> disciplinas) {
        if (periodoEncerrado()) {
            throw new RegraNegocioException("O período de " + identificador + " já foi encerrado; o currículo não pode mudar.");
        }
        for (Disciplina disciplina : disciplinas) {
            if (!disciplinasOfertadas.contains(disciplina)) {
                disciplinasOfertadas.add(disciplina);
            }
        }
    }

    public void encerrarPeriodo() {
        if (periodo == null || periodo.getStatus() != StatusPeriodo.ABERTO) {
            throw new RegraNegocioException("O semestre " + identificador + " não possui período de matrículas aberto.");
        }
        periodo.fechar();
        for (Disciplina disciplina : disciplinasOfertadas) {
            disciplina.avaliarPermanenciaAoFimDoPeriodo();
        }
    }

    public boolean periodoEncerrado() {
        return periodo != null && periodo.getStatus() == StatusPeriodo.ENCERRADO;
    }

    public boolean oferece(Disciplina disciplina) {
        return disciplinasOfertadas.contains(disciplina);
    }

    public String getIdentificador() {
        return identificador;
    }

    public List<Disciplina> getDisciplinasOfertadas() {
        return List.copyOf(disciplinasOfertadas);
    }

    public PeriodoMatriculas getPeriodo() {
        return periodo;
    }

    public void setPeriodo(PeriodoMatriculas periodo) {
        this.periodo = periodo;
    }
}
