package kr.co._29cm.homework.config;

import jakarta.annotation.PostConstruct;
import kr.co._29cm.homework.domain.Product;
import kr.co._29cm.homework.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class CsvDataLoader {

    private final ProductRepository productRepository;

    @PostConstruct
    public void loadProducts() {
        try {
            ClassPathResource resource = new ClassPathResource("products.csv");
            if (!resource.exists()) {
                return;
            }
            try (BufferedReader br = new BufferedReader(new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                boolean headerSkipped = false;
                while ((line = br.readLine()) != null) {
                    if (!headerSkipped) { // skip header
                        headerSkipped = true;
                        continue;
                    }
                    // CSV: 상품번호,상품명,판매가격,재고수량
                    String[] parts = splitCsv(line);
                    if (parts.length < 4) continue;
                    Long productNumber = Long.valueOf(parts[0]);
                    String name = parts[1];
                    BigDecimal price = new BigDecimal(parts[2]);
                    Integer stock = Integer.valueOf(parts[3]);

                    productRepository.findByProductNumber(productNumber)
                            .orElseGet(() -> productRepository.save(new Product(productNumber, name, price, stock)));
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String[] splitCsv(String line) {
        // 간단한 CSV 파서: 따옴표 포함 가능성 처리
        // 정교한 파싱이 필요하면 OpenCSV 등을 사용할 수 있음
        boolean inQuote = false;
        StringBuilder token = new StringBuilder();
        java.util.List<String> tokens = new java.util.ArrayList<>();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuote = !inQuote;
            } else if (c == ',' && !inQuote) {
                tokens.add(token.toString());
                token.setLength(0);
            } else {
                token.append(c);
            }
        }
        tokens.add(token.toString());
        return tokens.toArray(new String[0]);
    }
}


