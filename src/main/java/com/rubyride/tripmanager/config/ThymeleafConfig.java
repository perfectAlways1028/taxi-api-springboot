package com.rubyride.tripmanager.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.spring5.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.nio.charset.StandardCharsets;

@Configuration
public class ThymeleafConfig {
  @Primary
  @Bean
  public ITemplateResolver thymeleafTemplateResolver() {
    final var templateResolver = new ClassLoaderTemplateResolver();
    templateResolver.setPrefix("mail-templates/");
    templateResolver.setSuffix(".html");
    templateResolver.setTemplateMode(TemplateMode.HTML);
    templateResolver.setCharacterEncoding(StandardCharsets.UTF_8.name());

    return templateResolver;
  }

  @Bean
  public SpringTemplateEngine thymeleafTemplateEngine(final ITemplateResolver templateResolver) {
    final var templateEngine = new SpringTemplateEngine();
    templateEngine.setTemplateResolver(templateResolver);
    templateEngine.setTemplateEngineMessageSource(emailMessageSource());
    return templateEngine;
  }

  private ResourceBundleMessageSource emailMessageSource() {
    final var source = new ResourceBundleMessageSource();
    source.setBasenames("messages/mailMessages");
    source.setDefaultEncoding(StandardCharsets.UTF_8.name());

    return source;
  }
}
