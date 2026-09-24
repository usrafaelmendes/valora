package br.com.squadcore.comparaprecos.entity;

public enum StatusCalculo {
    /** Custo efetivo obtido, sem pendência bloqueante. */
    CALCULADO,
    /** Há pendência bloqueante: o custo efetivo não foi obtido (nada foi presumido). */
    INCOMPLETO
}
