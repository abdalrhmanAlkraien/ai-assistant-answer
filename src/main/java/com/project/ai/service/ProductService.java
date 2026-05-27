package com.project.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ai.dto.BulkUploadResponse;
import com.project.ai.dto.IndexResponse;
import com.project.ai.dto.ProductRequest;
import com.project.ai.dto.ProductUpdateRequest;
import com.project.ai.model.Product;
import com.project.ai.strategy.ecommerce.executor.EcommerceContextBuilder;
import com.project.ai.repository.ProductRepository;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.filter.MetadataFilterBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: Abd-alrhman Alkraien.
 * @Date: 23/05/2026
 * @Time: 10:56 PM
 */
@Service
@RequiredArgsConstructor
@Log4j2
public class ProductService {

    private final ProductRepository productRepository;
    private final EcommerceContextBuilder ecommerceContextBuilder;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingStore<TextSegment> embeddingStore;

    @Transactional
    public Product create(ProductRequest request) {
        if (productRepository.existsById(request.productId())) {
            throw new IllegalArgumentException("Product '%s' already exists".formatted(request.productId()));
        }

        Product product = Product.builder()
                .productId(request.productId())
                .title(request.title())
                .category(request.category())
                .brand(request.brand())
                .price(request.price())
                .currency(request.currency() != null ? request.currency() : "USD")
                .active(true)
                .imageUrl(request.imageUrl())
                .description(request.description())
                .build();

        Product saved = productRepository.save(product);
        ecommerceContextBuilder.invalidate();

        log.info("[ProductService] Created product id={} title={}", saved.getProductId(), saved.getTitle());
        return saved;
    }

    @Transactional
    public List<Product> createBatch(List<ProductRequest> requests) {
        List<Product> products = requests.stream()
                .filter(r -> !productRepository.existsById(r.productId()))
                .map(r -> Product.builder()
                        .productId(r.productId())
                        .title(r.title())
                        .category(r.category())
                        .brand(r.brand())
                        .price(r.price())
                        .currency(r.currency() != null ? r.currency() : "USD")
                        .active(true)
                        .build())
                .toList();

        List<Product> saved = productRepository.saveAll(products);
        ecommerceContextBuilder.invalidate();

        log.info("[ProductService] Created {} products", saved.size());
        return saved;
    }

    @Transactional
    public Product update(String productId, ProductRequest request) {
        Product product = findById(productId);

        product.setTitle(request.title());
        product.setCategory(request.category());
        product.setBrand(request.brand());
        product.setPrice(request.price());
        product.setCurrency(request.currency() != null ? request.currency() : "USD");

        Product saved = productRepository.save(product);
        ecommerceContextBuilder.invalidate();

        log.info("[ProductService] Updated product id={}", productId);
        return saved;
    }

    @Transactional
    public void delete(String productId) {
        Product product = findById(productId);
        removeEmbedding(productId);
        productRepository.delete(product);
        ecommerceContextBuilder.invalidate();
        log.info("[ProductService] Deleted product id={}", productId);
    }

    @Transactional
    public void deleteProducts(List<String> productIds) {
        log.info("[ProductService] Deleting {} products", productIds.size());

        List<String> existing = productRepository.findAllById(productIds)
                .stream()
                .map(Product::getProductId)
                .toList();

        List<String> notFound = productIds.stream()
                .filter(id -> !existing.contains(id))
                .toList();

        if (!notFound.isEmpty()) {
            log.warn("[ProductService] Products not found — skipping: {}", notFound);
        }

        if (existing.isEmpty()) {
            throw new IllegalArgumentException("None of the provided product IDs exist: " + productIds);
        }

        existing.forEach(this::removeEmbedding);

        productRepository.deleteAllByProductIds(existing);
        log.info("[ProductService] Deleted {} products from DB", existing.size());
    }


    @Transactional(readOnly = true)
    public IndexResponse indexAllProducts() {
        log.info("[ProductService] Starting full product indexing to ChromaDB");
        long start = System.currentTimeMillis();

        List<Product> products = productRepository.findAllActive();

        if (products.isEmpty()) {
            return IndexResponse.builder()
                    .indexed(0)
                    .failed(0)
                    .skipped(0)
                    .status("EMPTY")
                    .durationMs(0L)
                    .build();
        }

        int indexed = 0;
        int failed = 0;
        int skipped = 0;

        for (Product product : products) {
            try {
                // check if already indexed
                if (isAlreadyIndexed(product.getProductId())) {
                    log.info("[ProductService] Already indexed — skipping productId={}",
                            product.getProductId());
                    skipped++;
                    continue;
                }

                String content = buildProductContent(product);
                TextSegment segment = TextSegment.from(content,
                        Metadata.from("productId", product.getProductId()));

                Embedding embedding = embeddingModel.embed(segment).content();
                embeddingStore.add(embedding, segment);
                indexed++;

            } catch (Exception e) {
                log.warn("[ProductService] Failed to index product={}: {}",
                        product.getProductId(), e.getMessage());
                failed++;
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("[ProductService] Indexing complete — indexed={} skipped={} failed={} duration={}ms",
                indexed, skipped, failed, duration);

        return IndexResponse.builder()
                .indexed(indexed)
                .failed(failed)
                .skipped(skipped)
                .status(failed == 0 ? "SUCCESS" : "PARTIAL")
                .durationMs(duration)
                .build();
    }

    public BulkUploadResponse uploadProductsJson(MultipartFile file) throws IOException {
        log.info("[ProductService] Uploading products from JSON file={}", file.getOriginalFilename());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (!file.getOriginalFilename().endsWith(".json")) {
            throw new IllegalArgumentException("Only JSON files are supported");
        }

        ObjectMapper mapper = new ObjectMapper();
        List<ProductRequest> requests = mapper.readValue(
                file.getInputStream(),
                mapper.getTypeFactory().constructCollectionType(List.class, ProductRequest.class));

        int total = requests.size();
        int created = 0;
        int skipped = 0;
        int failed = 0;
        List<String> failedIds = new ArrayList<>();

        for (ProductRequest req : requests) {
            try {
                if (productRepository.existsById(req.productId())) {
                    log.info("[ProductService] Product already exists — skipping id={}", req.productId());
                    skipped++;
                    continue;
                }
                create(req);
                created++;
            } catch (Exception e) {
                log.warn("[ProductService] Failed to create product id={}: {}", req.productId(), e.getMessage());
                failedIds.add(req.productId());
                failed++;
            }
        }

        log.info("[ProductService] Upload complete — total={} created={} skipped={} failed={}",
                total, created, skipped, failed);

        return BulkUploadResponse.builder()
                .total(total)
                .created(created)
                .skipped(skipped)
                .failed(failed)
                .status(failed == 0 ? "SUCCESS" : "PARTIAL")
                .failedIds(failedIds)
                .build();
    }

    @Transactional
    public Product toggleStatus(String productId, boolean active) {
        Product product = findById(productId);
        product.setActive(active);
        productRepository.save(product);
        log.info("[ProductService] Product id={} status set to active={}", productId, active);
        return product;
    }

    private boolean isAlreadyIndexed(String productId) {
        try {
            Filter filter = MetadataFilterBuilder.metadataKey("productId").isEqualTo(productId);
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(embeddingModel.embed(productId).content())
                            .filter(filter)
                            .maxResults(1)
                            .minScore(0.0)
                            .build()
            ).matches();
            return !matches.isEmpty();
        } catch (Exception e) {
            log.warn("[ProductService] Could not check index status for productId={}: {}",
                    productId, e.getMessage());
            return false;
        }
    }

    private void removeEmbedding(String productId) {
        try {
            Filter filter = MetadataFilterBuilder.metadataKey("productId").isEqualTo(productId);
            List<EmbeddingMatch<TextSegment>> matches = embeddingStore.search(
                    EmbeddingSearchRequest.builder()
                            .queryEmbedding(embeddingModel.embed(productId).content())
                            .filter(filter)
                            .maxResults(10)
                            .minScore(0.0)
                            .build()
            ).matches();

            if (!matches.isEmpty()) {
                List<String> embeddingIds = matches.stream()
                        .map(EmbeddingMatch::embeddingId)
                        .toList();
                embeddingStore.removeAll(embeddingIds);
                log.info("[ProductService] Removed {} embeddings for productId={}",
                        embeddingIds.size(), productId);
            } else {
                log.info("[ProductService] No embeddings found for productId={} — skipping",
                        productId);
            }
        } catch (Exception e) {
            log.warn("[ProductService] Failed to remove embedding for productId={}: {}",
                    productId, e.getMessage());
        }
    }


    private String buildProductContent(Product product) {
        return String.format("%s %s %s price:%s %s",
                product.getTitle(),
                product.getCategory() != null ? product.getCategory() : "",
                product.getBrand() != null ? product.getBrand() : "",
                product.getPrice(),
                product.getDescription() != null ? product.getDescription() : "");
    }

    // ── Query ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Product findById(String productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
    }

    @Transactional(readOnly = true)
    public Page<Product> findAll(Pageable pageable) {
        return productRepository.findAll(pageable);
    }

    @Transactional
    public List<Product> updateProducts(List<ProductUpdateRequest> requests) {
        return requests.stream()
                .map(req -> {
                    Product product = productRepository.findById(req.getProductId())
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Product not found: " + req.getProductId()));

                    if (req.getTitle() != null)       product.setTitle(req.getTitle());
                    if (req.getCategory() != null)    product.setCategory(req.getCategory());
                    if (req.getBrand() != null)        product.setBrand(req.getBrand());
                    if (req.getPrice() != null)        product.setPrice(req.getPrice());
                    if (req.getCurrency() != null)     product.setCurrency(req.getCurrency());
                    if (req.getDescription() != null)  product.setDescription(req.getDescription());
                    if (req.getImageUrl() != null)     product.setImageUrl(req.getImageUrl());
                    if (req.getActive() != null)       product.setActive(req.getActive());

                    return productRepository.save(product);
                })
                .toList();
    }
}
