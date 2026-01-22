package pl.edu.salonmanager.salon_manager.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        fileStorageService = new FileStorageService(tempDir.toString(), "jpg,png,gif");
    }

    // ==================== Store File Tests ====================

    @Test
    void shouldStoreFileSuccessfully() {
        // Given
        byte[] content = "test image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        // When
        String filename = fileStorageService.storeFile(file, "TestTitle");

        // Then
        assertThat(filename).isNotNull();
        assertThat(filename).contains("TestTitle");
        assertThat(filename).endsWith(".jpg");

        Path storedFile = tempDir.resolve(filename);
        assertThat(Files.exists(storedFile)).isTrue();
    }

    @Test
    void shouldGenerateUniqueFilename() {
        // Given
        byte[] content = "test image content".getBytes();
        MultipartFile file1 = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
        MultipartFile file2 = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        // When
        String filename1 = fileStorageService.storeFile(file1, "TestTitle");
        String filename2 = fileStorageService.storeFile(file2, "TestTitle");

        // Then
        assertThat(filename1).isNotEqualTo(filename2);
        assertThat(filename1).contains("TestTitle");
        assertThat(filename2).contains("TestTitle");
    }

    @Test
    void shouldCleanSpecialCharactersFromTitle() {
        // Given
        byte[] content = "test image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.png", "image/png", content);

        // When
        String filename = fileStorageService.storeFile(file, "Test Title! @#$%");

        // Then
        assertThat(filename).doesNotContain("!");
        assertThat(filename).doesNotContain("@");
        assertThat(filename).doesNotContain("#");
        assertThat(filename).doesNotContain("$");
        assertThat(filename).doesNotContain("%");
        assertThat(filename).doesNotContain(" ");
        assertThat(filename).contains("Test_Title");
    }

    @Test
    void shouldThrowExceptionWhenFileIsEmpty() {
        // Given
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(file, "TestTitle"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Nie można zapisać pustego pliku");
    }

    @Test
    void shouldThrowExceptionWhenFilenameIsNull() {
        // Given - use a mock to return null for getOriginalFilename
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getOriginalFilename()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(file, "TestTitle"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Nazwa pliku jest wymagana");

        verify(file).isEmpty();
        verify(file).getOriginalFilename();
    }

    @Test
    void shouldThrowExceptionWhenExtensionNotAllowed() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.exe", "application/octet-stream", content);

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(file, "TestTitle"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Niedozwolone rozszerzenie pliku");
    }

    @Test
    void shouldAcceptAllowedExtensions() {
        // Given
        byte[] content = "test content".getBytes();

        // When & Then - jpg
        MultipartFile jpgFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
        String jpgFilename = fileStorageService.storeFile(jpgFile, "JpgTest");
        assertThat(jpgFilename).endsWith(".jpg");

        // When & Then - png
        MultipartFile pngFile = new MockMultipartFile("file", "test.png", "image/png", content);
        String pngFilename = fileStorageService.storeFile(pngFile, "PngTest");
        assertThat(pngFilename).endsWith(".png");

        // When & Then - gif
        MultipartFile gifFile = new MockMultipartFile("file", "test.gif", "image/gif", content);
        String gifFilename = fileStorageService.storeFile(gifFile, "GifTest");
        assertThat(gifFilename).endsWith(".gif");
    }

    @Test
    void shouldHandleUppercaseExtension() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.JPG", "image/jpeg", content);

        // When
        String filename = fileStorageService.storeFile(file, "TestTitle");

        // Then
        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".JPG");
    }

    @Test
    void shouldHandleFileWithoutExtension() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "testfile", "application/octet-stream", content);

        // When & Then
        assertThatThrownBy(() -> fileStorageService.storeFile(file, "TestTitle"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Niedozwolone rozszerzenie pliku");
    }

    // ==================== Load File Tests ====================

    @Test
    void shouldLoadFile() throws IOException {
        // Given
        byte[] content = "test image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
        String filename = fileStorageService.storeFile(file, "TestTitle");

        // When
        Path loadedPath = fileStorageService.loadFile(filename);

        // Then
        assertThat(loadedPath).isNotNull();
        assertThat(Files.exists(loadedPath)).isTrue();
        assertThat(Files.readAllBytes(loadedPath)).isEqualTo(content);
    }

    @Test
    void shouldReturnNormalizedPath() {
        // Given
        String filename = "test_file.jpg";

        // When
        Path path = fileStorageService.loadFile(filename);

        // Then
        assertThat(path).isNotNull();
        assertThat(path.isAbsolute()).isTrue();
        assertThat(path.toString()).doesNotContain("..");
    }

    // ==================== Delete File Tests ====================

    @Test
    void shouldDeleteFileSuccessfully() throws IOException {
        // Given
        byte[] content = "test image content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
        String filename = fileStorageService.storeFile(file, "TestTitle");

        Path filePath = tempDir.resolve(filename);
        assertThat(Files.exists(filePath)).isTrue();

        // When
        fileStorageService.deleteFile(filename);

        // Then
        assertThat(Files.exists(filePath)).isFalse();
    }

    @Test
    void shouldNotThrowExceptionWhenDeletingNonExistentFile() {
        // Given
        String nonExistentFilename = "non_existent_file.jpg";

        // When & Then - should not throw
        fileStorageService.deleteFile(nonExistentFilename);

        // Verify file doesn't exist
        Path filePath = tempDir.resolve(nonExistentFilename);
        assertThat(Files.exists(filePath)).isFalse();
    }

    // ==================== Constructor Tests ====================

    @Test
    void shouldCreateUploadDirectoryIfNotExists() {
        // Given
        Path newDir = tempDir.resolve("new_upload_dir");
        assertThat(Files.exists(newDir)).isFalse();

        // When
        FileStorageService service = new FileStorageService(newDir.toString(), "jpg,png");

        // Then
        assertThat(Files.exists(newDir)).isTrue();
        assertThat(Files.isDirectory(newDir)).isTrue();
    }

    @Test
    void shouldParseMultipleExtensions() {
        // Given
        byte[] content = "test content".getBytes();
        FileStorageService service = new FileStorageService(tempDir.toString(), "jpg,png,gif,webp");

        // When & Then
        MultipartFile webpFile = new MockMultipartFile("file", "test.webp", "image/webp", content);
        String filename = service.storeFile(webpFile, "WebpTest");
        assertThat(filename).endsWith(".webp");
    }

    // ==================== Edge Cases ====================

    @Test
    void shouldHandleEmptyTitle() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        // When
        String filename = fileStorageService.storeFile(file, "");

        // Then
        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".jpg");
        assertThat(filename).contains("_");
    }

    @Test
    void shouldHandleTitleWithOnlySpecialCharacters() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        // When
        String filename = fileStorageService.storeFile(file, "!@#$%^&*()");

        // Then
        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".jpg");
    }

    @Test
    void shouldReplaceExistingFile() throws IOException {
        // Given
        byte[] content1 = "first content".getBytes();
        byte[] content2 = "second content".getBytes();
        MultipartFile file1 = new MockMultipartFile("file", "test.jpg", "image/jpeg", content1);
        MultipartFile file2 = new MockMultipartFile("file", "test.jpg", "image/jpeg", content2);

        String filename1 = fileStorageService.storeFile(file1, "SameTitle");
        Path path1 = tempDir.resolve(filename1);
        byte[] storedContent1 = Files.readAllBytes(path1);

        String filename2 = fileStorageService.storeFile(file2, "SameTitle");
        Path path2 = tempDir.resolve(filename2);
        byte[] storedContent2 = Files.readAllBytes(path2);

        // Then - files have different names due to UUID
        assertThat(filename1).isNotEqualTo(filename2);
        assertThat(storedContent1).isEqualTo(content1);
        assertThat(storedContent2).isEqualTo(content2);
    }

    @Test
    void shouldHandleLongTitle() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);
        String longTitle = "A".repeat(200);

        // When
        String filename = fileStorageService.storeFile(file, longTitle);

        // Then
        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".jpg");
    }

    @Test
    void shouldHandlePolishCharactersInTitle() {
        // Given
        byte[] content = "test content".getBytes();
        MultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", content);

        // When
        String filename = fileStorageService.storeFile(file, "Zdjęcie ślubne żółty");

        // Then
        assertThat(filename).isNotNull();
        assertThat(filename).endsWith(".jpg");
        // Polish characters should be replaced with underscores
        assertThat(filename).doesNotContain("ę");
        assertThat(filename).doesNotContain("ś");
        assertThat(filename).doesNotContain("ó");
        assertThat(filename).doesNotContain("ż");
    }
}
