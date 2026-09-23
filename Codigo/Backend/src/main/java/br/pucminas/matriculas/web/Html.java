package br.pucminas.matriculas.web;

import br.pucminas.matriculas.app.Formatos;
import br.pucminas.matriculas.externo.Notificacao;

import java.util.Collection;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Pequenos utilitários para montar os fragmentos HTML devolvidos ao HTMX. */
final class Html {

    private Html() {
    }

    static String esc(Object valor) {
        if (valor == null) {
            return "";
        }
        return valor.toString()
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    static <T> String each(Collection<T> itens, Function<T, String> render) {
        return itens.stream().map(render).collect(Collectors.joining());
    }

    /** Etiqueta colorida; {@code tom}: azul, verde, ambar, vermelho, cinza, roxo. */
    static String chip(String texto, String tom, String dica) {
        return "<span class=\"chip chip-" + tom + "\"" + (dica == null ? "" : " title=\"" + esc(dica) + "\"") + ">"
                + esc(texto) + "</span>";
    }

    static String chip(String texto, String tom) {
        return chip(texto, tom, null);
    }

    static String option(String valor, String rotulo, boolean selecionado) {
        return "<option value=\"" + esc(valor) + "\"" + (selecionado ? " selected" : "") + ">" + esc(rotulo) + "</option>";
    }

    /** hx-vals com JSON; os valores são escapados para caber no atributo. */
    static String vals(String... paresChaveValor) {
        StringBuilder json = new StringBuilder("{");
        for (int i = 0; i < paresChaveValor.length; i += 2) {
            if (i > 0) {
                json.append(',');
            }
            json.append('"').append(paresChaveValor[i]).append("\":\"")
                    .append(paresChaveValor[i + 1].replace("\\", "\\\\").replace("\"", "\\\"")).append('"');
        }
        return " hx-vals=\"" + esc(json.append('}')) + "\"";
    }

    static String iniciais(String nome) {
        String[] partes = nome.trim().split("\\s+");
        String primeira = partes[0].substring(0, 1);
        String ultima = partes.length > 1 ? partes[partes.length - 1].substring(0, 1) : "";
        return (primeira + ultima).toUpperCase();
    }

    static String toast(Notificacao n) {
        String origem = Notificacao.ORIGEM_COBRANCA.equals(n.origem()) ? "cobranca" : "sistema";
        return "<div class=\"toast toast-" + n.nivel().name().toLowerCase() + " origem-" + origem + "\" role=\"status\">"
                + "<div class=\"toast-titulo\"><span>[" + esc(n.origem()) + "]</span><small>" + Formatos.hora(n.dataHora())
                + "</small></div><div class=\"toast-texto\">" + esc(n.mensagem()) + "</div></div>";
    }

    static String toastErro(String mensagem) {
        return "<div hx-swap-oob=\"beforeend:#toasts\"><div class=\"toast toast-erro origem-sistema\" role=\"alert\">"
                + "<div class=\"toast-titulo\"><span>Não foi possível concluir</span></div>"
                + "<div class=\"toast-texto\">" + esc(mensagem) + "</div></div></div>";
    }
}
