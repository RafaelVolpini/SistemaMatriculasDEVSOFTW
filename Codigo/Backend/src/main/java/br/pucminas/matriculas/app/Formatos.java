package br.pucminas.matriculas.app;

import br.pucminas.matriculas.model.enums.TipoInscricao;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class Formatos {

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter CURTO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter INPUT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm");

    private Formatos() {
    }

    public static String dataHora(LocalDateTime valor) {
        return valor == null ? "—" : DATA_HORA.format(valor);
    }

    public static String curto(LocalDateTime valor) {
        return valor == null ? "—" : CURTO.format(valor);
    }

    public static String hora(LocalDateTime valor) {
        return valor == null ? "—" : HORA.format(valor);
    }

    public static String tipo(TipoInscricao tipo) {
        return tipo == TipoInscricao.OBRIGATORIA ? "obrigatória" : "optativa";
    }

    /** Valor aceito por &lt;input type="datetime-local"&gt;. */
    public static String input(LocalDateTime valor) {
        return valor == null ? "" : INPUT.format(valor);
    }
}
