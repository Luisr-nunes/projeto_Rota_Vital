package com.hemorede.domain.enums;

/** Perfis de acesso do sistema. */
public enum Perfil {
    /** Doador: mantém os próprios dados básicos e acompanha a elegibilidade. */
    DOADOR,
    /** Hospital: solicita bolsas e acompanha as próprias entregas. */
    HOSPITAL,
    /** Hemocentro (o sistema): controla e visualiza tudo. */
    HEMOCENTRO
}
