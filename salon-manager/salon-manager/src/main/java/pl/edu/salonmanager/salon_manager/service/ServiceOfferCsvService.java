package pl.edu.salonmanager.salon_manager.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import pl.edu.salonmanager.salon_manager.exception.BadRequestException;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.request.CreateServiceRequest;
import pl.edu.salonmanager.salon_manager.model.dto.serviceOffer.response.ServiceOfferDto;
import pl.edu.salonmanager.salon_manager.model.entity.ServiceOffer;
import pl.edu.salonmanager.salon_manager.repository.ServiceOfferRepository;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ServiceOfferCsvService {

    private final ServiceOfferRepository serviceOfferRepository;
    private final ServiceOfferService serviceOfferService;

    private static final String CSV_HEADER = "name,price,durationMinutes";
    private static final String CSV_DELIMITER = ",";


    public String exportToCsv() {
        log.debug("Exporting service offers to CSV");

        List<ServiceOfferDto> services = serviceOfferService.getAllServices();

        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER).append("\n");

        for (ServiceOfferDto service : services) {
            csv.append(escapeCsvValue(service.getName())).append(CSV_DELIMITER)
               .append(service.getPrice()).append(CSV_DELIMITER)
               .append(service.getDurationMinutes()).append("\n");
        }

        log.info("Exported {} service offers to CSV", services.size());
        return csv.toString();
    }


    public List<ServiceOfferDto> importFromCsv(MultipartFile file) {
        log.debug("Importing service offers from CSV file: {}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }

        if (!file.getOriginalFilename().endsWith(".csv")) {
            throw new BadRequestException("File must be a CSV file");
        }

        List<ServiceOfferDto> importedServices = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = reader.readLine();
            if (headerLine == null || !headerLine.equals(CSV_HEADER)) {
                throw new BadRequestException("Invalid CSV format. Expected header: " + CSV_HEADER);
            }

            String line;
            int lineNumber = 1;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                if (line.trim().isEmpty()) {
                    continue;
                }

                try {
                    ServiceOfferDto imported = parseCsvLine(line, lineNumber);
                    importedServices.add(imported);
                } catch (Exception e) {
                    log.warn("Error parsing line {}: {}", lineNumber, e.getMessage());
                    throw new BadRequestException("Error on line " + lineNumber + ": " + e.getMessage());
                }
            }

            log.info("Successfully imported {} service offers from CSV", importedServices.size());
            return importedServices;

        } catch (IOException e) {
            log.error("Error reading CSV file", e);
            throw new BadRequestException("Error reading CSV file: " + e.getMessage());
        }
    }

    private ServiceOfferDto parseCsvLine(String line, int lineNumber) {
        String[] parts = line.split(CSV_DELIMITER, -1);

        if (parts.length != 3) {
            throw new BadRequestException("Expected 3 columns, found " + parts.length);
        }

        String name = parts[0].trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Name cannot be empty");
        }
        if (name.length() > 100) {
            throw new BadRequestException("Name too long (max 100 characters)");
        }

        BigDecimal price;
        try {
            price = new BigDecimal(parts[1].trim());
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Price must be positive");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid price format: " + parts[1]);
        }

        Integer durationMinutes;
        try {
            durationMinutes = Integer.parseInt(parts[2].trim());
            if (durationMinutes <= 0) {
                throw new BadRequestException("Duration must be positive");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Invalid duration format: " + parts[2]);
        }

        CreateServiceRequest request = new CreateServiceRequest();
        request.setName(name);
        request.setPrice(price);
        request.setDurationMinutes(durationMinutes);

        return serviceOfferService.createService(request);
    }

    private String escapeCsvValue(String value) {
        if (value == null) {
            return "";
        }

        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }

        return value;
    }
}
