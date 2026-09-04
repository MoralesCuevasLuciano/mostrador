package com.pepology.mostrador.controllers;

import com.pepology.mostrador.dto.upload.UploadResponse;
import com.pepology.mostrador.services.UploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Subida de fotos de variantes. Multipart a /api/uploads; responde con la URL pública.
 */
@RestController
@RequestMapping("/api/uploads")
@RequiredArgsConstructor
public class UploadController {

	private final UploadService uploadService;

	/** POST /api/uploads — guarda el archivo y devuelve { url }. */
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UploadResponse upload(@RequestParam("file") MultipartFile file) throws IOException {
		return new UploadResponse(uploadService.store(file));
	}
}
