package com.pepology.mostrador.services;

import com.pepology.mostrador.exceptions.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Recibe archivos de imagen y los deja en disco. La base solo guarda la URL.
 */
@Service
public class UploadService {

	private static final Set<String> ALLOWED_TYPES = Set.of(
			"image/jpeg",
			"image/png",
			"image/webp",
			"image/gif");

	private static final Map<String, String> EXTENSIONS = Map.of(
			"image/jpeg", ".jpg",
			"image/png", ".png",
			"image/webp", ".webp",
			"image/gif", ".gif");

	private final Path uploadDir;

	/** Crea el directorio de uploads si todavía no existe. */
	public UploadService(@Value("${mostrador.upload-dir}") String uploadDir) throws IOException {
		this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
		Files.createDirectories(this.uploadDir);
	}

	/** Ruta absoluta de la carpeta, para servir los archivos por HTTP. */
	public Path directory() {
		return uploadDir;
	}

	/**
	 * Guarda el archivo con un nombre UUID y devuelve la URL pública (/uploads/...).
	 * Solo acepta JPG, PNG, WEBP o GIF.
	 */
	public String store(MultipartFile file) throws IOException {
		if (file == null || file.isEmpty()) {
			throw new BusinessRuleException("Hay que elegir una imagen");
		}
		String contentType = normalizeType(file);
		if (!ALLOWED_TYPES.contains(contentType)) {
			throw new BusinessRuleException("Solo se aceptan imágenes JPG, PNG, WEBP o GIF");
		}
		String filename = UUID.randomUUID() + EXTENSIONS.get(contentType);
		Path target = uploadDir.resolve(filename).normalize();
		if (!target.startsWith(uploadDir)) {
			throw new BusinessRuleException("Nombre de archivo inválido");
		}
		file.transferTo(target);
		return "/uploads/" + filename;
	}

	/** Resuelve el MIME a partir del content-type o, si viene genérico, de la extensión. */
	private static String normalizeType(MultipartFile file) {
		String contentType = file.getContentType();
		if (contentType != null && ALLOWED_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
			return contentType.toLowerCase(Locale.ROOT);
		}
		String name = file.getOriginalFilename();
		if (name == null) {
			return "";
		}
		String lower = name.toLowerCase(Locale.ROOT);
		if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
			return "image/jpeg";
		}
		if (lower.endsWith(".png")) {
			return "image/png";
		}
		if (lower.endsWith(".webp")) {
			return "image/webp";
		}
		if (lower.endsWith(".gif")) {
			return "image/gif";
		}
		return contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
	}
}
