package com.beem.TastyMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

import java.util.Arrays;
import java.util.Locale;

@Configuration
public class I18nConfig {

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();

        // Varsayılan dili Türkçe olarak ayarla
        resolver.setDefaultLocale(Locale.forLanguageTag("tr"));

        // Desteklenen diller
        resolver.setSupportedLocales(Arrays.asList(
                Locale.forLanguageTag("tr"),
                Locale.forLanguageTag("en")
        ));

        return resolver;
    }

    @Bean
    public ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages"); // setBasenames yerine setBasename tercih edilebilir
        source.setDefaultEncoding("UTF-8");
        source.setUseCodeAsDefaultMessage(true);
        // Varsayılan dil bulunamadığında varsayılan locale dosyasına düşmesini sağla
        source.setFallbackToSystemLocale(false);
        return source;
    }
}