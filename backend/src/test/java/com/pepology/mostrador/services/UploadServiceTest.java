package com.pepology.mostrador.services;

import com.pepology.mostrador.exceptions.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Unit tests de UploadService: guarda JPEG y rechaza tipos que no son imagen. */
class UploadServiceTest {

	@TempDir
	Path tempDir;

	@Test
	void storeSavesJpegAndReturnsPublicUrl() throws Exception {
		UploadService service = new UploadService(tempDir.toString());
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"foto.jpg",
				"image/jpeg",
				new byte[] { 1, 2, 3, 4 });

		String url = service.store(file);

		assertTrue(url.startsWith("/uploads/"));
		assertTrue(url.endsWith(".jpg"));
		assertTrue(Files.exists(tempDir.resolve(url.substring("/uploads/".length()))));
	}

	@Test
	void storeRejectsEmptyFile() throws Exception {
		UploadService service = new UploadService(tempDir.toString());
		MockMultipartFile file = new MockMultipartFile("file", "foto.jpg", "image/jpeg", new byte[0]);

		assertThrows(BusinessRuleException.class, () -> service.store(file));
	}

	@Test
	void storeRejectsPdf() throws Exception {
		UploadService service = new UploadService(tempDir.toString());
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"doc.pdf",
				"application/pdf",
				new byte[] { 1, 2, 3 });

		BusinessRuleException error = assertThrows(BusinessRuleException.class, () -> service.store(file));
		assertEquals("Solo se aceptan imágenes JPG, PNG, WEBP o GIF", error.getMessage());
	}
}
