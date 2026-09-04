package com.pepology.mostrador.config;

import com.pepology.mostrador.services.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Expone la carpeta de fotos en GET /uploads/** para que el navegador las pueda mostrar.
 */
@Configuration
@RequiredArgsConstructor
public class UploadWebConfig implements WebMvcConfigurer {

	private final UploadService uploadService;

	/** Asocia la URL pública /uploads/... con el directorio en disco. */
	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		String location = uploadService.directory().toUri().toString();
		if (!location.endsWith("/")) {
			location += "/";
		}
		registry.addResourceHandler("/uploads/**").addResourceLocations(location);
	}
}
