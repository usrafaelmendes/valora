package br.com.squadcore.comparaprecos.dto;

/** Informa somente se a instalação já tem algum usuário; nenhum dado de usuário é exposto. */
public record ConfiguracaoInicialStatusResponse(boolean configurado) {
}
