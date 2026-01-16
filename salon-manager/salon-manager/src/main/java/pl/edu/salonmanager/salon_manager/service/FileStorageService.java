package pl.edu.salonmanager.salon_manager.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {

    private final Path uploadPath;
    private final List<String> allowedExtensions;

    public FileStorageService(
            @Value("${app.upload.dir}") String uploadDir,
            @Value("${app.upload.allowed-extensions}") String extensions) {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.allowedExtensions = Arrays.asList(extensions.split(","));

        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException ex) {
            throw new RuntimeException("Nie można utworzyć katalogu dla plików", ex);
        }
    }

    public String storeFile(MultipartFile file, String bookTitle) {
        // Walidacja czy plik nie jest pusty
        if (file.isEmpty()) {
            throw new BadRequestException("Nie można zapisać pustego pliku");
        }

        // Pobranie oryginalnej nazwy i rozszerzenia
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BadRequestException("Nazwa pliku jest wymagana");
        }

        // Sprawdzenie rozszerzenia
        String extension = getFileExtension(originalFilename);
        if (!allowedExtensions.contains(extension.toLowerCase())) {
            throw new BadRequestException(
                    "Niedozwolone rozszerzenie pliku. Dozwolone: " + allowedExtensions);
        }

        // Generowanie unikalnej nazwy pliku
        String filename = generateUniqueFilename(bookTitle, extension);
        Path targetLocation = this.uploadPath.resolve(filename);

        try {
            // Zapis pliku na dysk
            Files.copy(file.getInputStream(), targetLocation,
                    StandardCopyOption.REPLACE_EXISTING);
            return filename;
        } catch (IOException ex) {
            throw new RuntimeException("Błąd podczas zapisu pliku", ex);
        }
    }

    public Path loadFile(String filename) {
        return uploadPath.resolve(filename).normalize();
    }

    public void deleteFile(String filename) {
        try {
            Path filePath = uploadPath.resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            throw new RuntimeException("Błąd podczas usuwania pliku", ex);
        }
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot == -1) {
            return "";
        }
        return filename.substring(lastDot + 1);
    }

    private String generateUniqueFilename(String bookTitle, String extension) {
        // Czyszczenie tytułu z niedozwolonych znaków
        String cleanTitle = bookTitle.replaceAll("[^a-zA-Z0-9]", "_");
        // Dodanie UUID dla unikalności
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        return cleanTitle + "_" + uniqueId + "." + extension;
    }
}