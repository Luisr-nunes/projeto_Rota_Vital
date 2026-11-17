package com.hemorede.arquitetura;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regra de dependência das três camadas: a apresentação (controllers) só usa a
 * camada de negócio (services) e nunca chama a camada de dados (repositories) direto.
 */
class ControllerSemRepositorioTest {

    @Test
    void nenhumControllerDependeDeRepository() throws ClassNotFoundException {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<BeanDefinition> controllers = scanner.findCandidateComponents("com.hemorede.controller");
        assertFalse(controllers.isEmpty(), "nenhum controller encontrado: o pacote mudou?");

        List<String> violacoes = new ArrayList<>();
        for (BeanDefinition bd : controllers) {
            Class<?> controller = Class.forName(bd.getBeanClassName());
            for (Field campo : controller.getDeclaredFields()) {
                if (campo.getType().getPackageName().startsWith("com.hemorede.repository")) {
                    violacoes.add(controller.getSimpleName() + "." + campo.getName());
                }
            }
        }
        assertTrue(violacoes.isEmpty(), "controllers acessando repository direto: " + violacoes);
    }
}
