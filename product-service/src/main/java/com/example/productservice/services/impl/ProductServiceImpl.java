package com.example.productservice.services.impl;

import com.example.productservice.dto.request.ProductRequest;
import com.example.productservice.dto.response.CategoryResponse;
import com.example.productservice.dto.response.ProductImageResponse;
import com.example.productservice.dto.response.ProductResponse;
import com.example.productservice.entities.Category;
import com.example.productservice.entities.Product;
import com.example.productservice.exception.CategoryNotFoundException;
import com.example.productservice.exception.CustomException;
import com.example.productservice.mapper.CategoryMapper;
import com.example.productservice.mapper.ProductMapper;
import com.example.productservice.repositories.ProductRepository;
import com.example.productservice.repositories.specification.ProductSpecification;
import com.example.productservice.repositories.specification.SpecSearchCriteria;
import com.example.productservice.services.CategoryService;
import com.example.productservice.services.ProductImageService;
import com.example.productservice.services.ProductService;
import com.example.productservice.util.GenerateUniqueCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.example.productservice.constant.CommonDefine.*;
import static com.example.productservice.repositories.specification.SearchOperation.OR_PREDICATE_FLAG;
import static com.example.productservice.util.AppConst.SEARCH_SPEC_OPERATOR;
import static com.example.productservice.util.AppConst.SORT_BY;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;
    private final CategoryService categoryService;
    private final ProductMapper productMapper;
    private final CategoryMapper categoryMapper;
    private final ProductImageService productImageService;
//    private final BaseRedisServiceImpl<String, String, Object> redisService;
    private final ObjectMapper objectMapper;

    private ProductResponse convertToProductDTO(Object object) {
        if (object instanceof LinkedHashMap) {
            return objectMapper.convertValue(object, ProductResponse.class);
        } else {
            return (ProductResponse) object;
        }
    }

    @Override
    public Long countProducts() {
        return productRepository.count();
    }

    @Override
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        String key = String.format(GET_ALL_PRODUCTS, pageable.getPageNumber(), pageable.getPageSize());

        //redis
//        if (redisService.keyExists(key)) {
////            Map<String, Object> productsMap = redisService.getField(key);
////
////            for (Map.Entry<String, Object> entry : productsMap.entrySet()) {
////                Map<String, Object> product = (Map<String, Object>) entry.getValue();
////                    productDTOS.add(convertToProductDTO(product));
////            }
//
//            List<Object> values = redisService.getList(key);
//            List<ProductDTO> productDTOS = new ArrayList<>();
//            for (Object value : values) {
//                productDTOS.add(convertToProductDTO(value));
//            }
//            return new PageImpl<>(productDTOS, pageable, productDTOS.size());
//        } else {
            Page<Product> products = productRepository.findByDeletedAtIsNull(pageable);
            return products.map(product -> {
                ProductResponse productDTO = productMapper.INSTANCE.toProductResponse(product);
//                redisService.rightPushAll(key, Collections.singletonList(productDTO));
//                redisService.hashSet(key, PRODUCT_ID + product.getProductId(), productDTO);
                return productDTO;
            });
//        }
    }

    @Override
    public ProductResponse getProductByName(String name) {
        Product product = productRepository.findByNameAndDeletedAtIsNull(name);
        if (product == null) {
            throw new CustomException("Product not found with name: " + name, HttpStatus.BAD_REQUEST);
        }
        return productMapper.INSTANCE.toProductResponse(product);
    }

    @Override
    public Page<ProductResponse> getProductByNameLike(String name, Pageable pageable) {
        return productRepository.findByNameLikeAndDeletedAtIsNull(name, pageable).map(productMapper.INSTANCE::toProductResponse);
    }

    @Override
    public ProductResponse getProductById(Long id) {
//        if (redisService.keyExists(PRODUCT_ID + id)) {
//            Object object =  redisService.getField(PRODUCT_ID + id);
//            return convertToProductDTO(object);
//        }
        Product product = findProductById(id);
        var productResponse = productMapper.INSTANCE.toProductResponse(product);
        //redis
//        redisService.hashSetAll(PRODUCT_ID + id, productResponse);
        return productResponse;
    }

    @Override
    public List<ProductResponse> getProductsByIds(Set<Long> productIds) {
        List<Product> products = productRepository.findByProductIdInAndDeletedAtIsNull(productIds);
        products.forEach(product -> {
            if (product.getProductId() == null) {
                throw new CustomException("Product is not found", HttpStatus.NOT_FOUND);
            }
        });

        return productMapper.INSTANCE.productListToProductDTOList(products);
    }

    @Override
    public ProductResponse getProductByCode(String code) {
        return productMapper.toProductResponse(productRepository.findByCodeProductAndDeletedAtIsNull(code));
    }

    @Override
    public void addProduct(ProductRequest request, List<MultipartFile> imageFiles) {
        if (productRepository.existsByNameAndDeletedAtIsNull(request.getName())) {
            throw new CustomException("Product already exists with name: " + request.getName(), HttpStatus.CONFLICT);
        }

        CategoryResponse categoryDTO = categoryService.getCategoryById(request.getCategoryId());
        if (categoryDTO == null) {
            throw new CustomException("Can not find category with id " + request.getCategoryId(), HttpStatus.NOT_FOUND);
        }

        Product product = productMapper.INSTANCE.toProduct(request);

        product.setCategory(categoryMapper.INSTANCE.categoryResponsetoCategory(categoryDTO));

        do {
            product.setCodeProduct(GenerateUniqueCode.generateProductCode());
        } while (productRepository.existsByCodeProductAndDeletedAtIsNull(product.getCodeProduct()));

        productRepository.save(product);

        productImageService.saveProductImage(product.getProductId(), imageFiles);

        //redis
//        redisService.getKeyPrefixes("get_products" + "*").forEach(redisService::delete);
    }

    @Override
    public Page<ProductResponse> findByCategory(Pageable pageable, Long categoryId) {
        CategoryResponse category = categoryService.getCategoryById(categoryId);

        String key = String.format(GET_PRODUCTS_BY_CATEGORY, category.getCategoryId(), pageable.getPageNumber(), pageable.getPageSize());

//        if (redisService.keyExists(key)) {
//            List<Object> values = redisService.getList(key);
//            List<ProductDTO> productDTOS = new ArrayList<>();
//            for (Object value : values) {
//                productDTOS.add(convertToProductDTO(value));
//            }
//            return new PageImpl<>(productDTOS, pageable, productDTOS.size());
//        }else {
            if (category == null) {
                throw new CustomException("Can not find category with id " + categoryId, HttpStatus.NOT_FOUND);
            }
            return productRepository.findByCategoryAndDeletedAtIsNull(pageable, categoryMapper.INSTANCE.categoryResponsetoCategory(category))
                    .map(product -> {
                        ProductResponse productDTO = productMapper.INSTANCE.toProductResponse(product);
                        //redis
//                        redisService.rightPushAll(key, Collections.singletonList(productDTO));
                        return productDTO;
                    });
//        }
    }

    @Override
    public void updateProduct(long id, ProductRequest request, List<MultipartFile> imageFiles) {
        Product existingProduct = findProductById(id);
        if (request.getPrice() != null) {
            existingProduct.setPrice(request.getPrice());
        }
        if (request.getName() != null) {
            if (productRepository.existsByNameAndDeletedAtIsNull(request.getName()) && !request.getName().equals(existingProduct.getName())) {
                throw new CustomException("Product already exists with name: " + request.getName(), HttpStatus.BAD_REQUEST);
            }
            existingProduct.setName(request.getName());
        }
        if (request.getDescription() != null) {
            existingProduct.setDescription(request.getDescription());
        }

//        if (updatedProductDTO.getImages() != null) {
//            Set<ProductImageDTO> images = updatedProductDTO.getImages();
//            existingProduct.get().setImages(productMapper.INSTANCE.productImageDTOSetToProductImageSet(images));
//        }

//   not do this     if (updatedProductDTO.getStockQuantity() != null) {
//            existingProduct.get().setStockQuantity(updatedProductDTO.getStockQuantity());
//        }

        if (request.getManufacturer() != null) {
            existingProduct.setManufacturer(request.getManufacturer());
        }

        if (request.getSize() != null) {
            existingProduct.setSize(request.getSize());
        }

        if (request.getWeight() != null) {
            existingProduct.setWeight(request.getWeight());
        }

        if (request.getCategoryId() != null) {
            CategoryResponse categoryDTO = categoryService.getCategoryById(request.getCategoryId());
            if (categoryDTO == null) {
                throw new CategoryNotFoundException("Can not find category with id " + request.getCategoryId());
            }

            Category category = categoryMapper.INSTANCE.categoryResponsetoCategory(categoryDTO);
            existingProduct.setCategory(category);
        }

        existingProduct.setStockQuantity(existingProduct.getStockQuantity());
        productRepository.save(existingProduct);

        List<Long> productImageIds = new ArrayList<>();
        List<ProductImageResponse> productImageDTOs = productImageService.getProductImages(existingProduct.getProductId());
        for (ProductImageResponse productImageDTO : productImageDTOs) {
            if (!request.getImages().contains(productImageDTO)) {
                productImageIds.add(productImageDTO.getImageId());
            }
        }
        productImageService.updateProductImage(existingProduct.getProductId(), productImageIds , imageFiles);

        //redis
//        if (redisService.keyExists(PRODUCT_ID + id)) {
//            redisService.delete(PRODUCT_ID + id);
//        }
//        redisService.getKeyPrefixes("get_products" + "*").forEach(redisService::delete);
    }

    @Override
    public void updateStockQuantity(long id, Integer stockQuantity) {
        Product product = findProductById(id);
//        redisService.delete(PRODUCT_ID + id);

        product.setStockQuantity(stockQuantity);
        productRepository.save(product);

        //redis
//        redisService.hashSetAll(PRODUCT_ID + id, productMapper.INSTANCE.productToProductDTO(product));
    }

    @Override
    public void deleteProduct(long id) {
        findProductById(id);
        productRepository.deleteById(id);
        //redis
//        redisService.delete(PRODUCT_ID + id);
//        redisService.getKeyPrefixes("get_products" + "*").forEach(redisService::delete);
    }

    @Override
    public void moveToTrash(Long id) {
        Product product = findProductById(id);
        LocalDateTime now = LocalDateTime.now();
        product.setDeletedAt(now);
        productRepository.save(product);
    }

    @Override
    public Page<ProductResponse> getInTrash(Pageable pageable) {
        Page<Product> products = productRepository.findByDeletedAtIsNotNull(pageable);
        return products.map(productMapper.INSTANCE::toProductResponse);
    }

    private Product findProductById(long id) {
        //có thể sử dụng Distributed Lock
        return productRepository.findById(id).orElseThrow(() -> new CustomException("Product not found with id: " + id, HttpStatus.BAD_REQUEST));
    }

    @Override
    public void restoreProduct(Long id) {
        Product product = findProductById(id);
        product.setDeletedAt(null);
        productRepository.save(product);
    }

    @Override
    public Page<ProductResponse> searchBySpecification(Pageable pageable, String sort, String[] product, String category) {
        log.info("getProductsBySpecifications");

        Pageable pageableSorted = pageable;
        if (StringUtils.hasText(sort)){
            Pattern patternSort = Pattern.compile(SORT_BY);
            Matcher matcher = patternSort.matcher(sort);
            if (matcher.find()) {
                String columnName = matcher.group(1);
                pageableSorted = matcher.group(3).equalsIgnoreCase("desc")
                        ? PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(columnName).descending())
                        : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(columnName).ascending());
            }
        }

        List<SpecSearchCriteria> params = new ArrayList<>();
        Pattern pattern = Pattern.compile(SEARCH_SPEC_OPERATOR);
        if (product != null) {
            params.addAll(parseProductCriteria(product, pattern));
        }
        if (category != null) {
            params.addAll(parseCategoryCriteria(category, pattern));
        }

        if (params.isEmpty()) {
            return productRepository.findAll(pageableSorted).map(productMapper.INSTANCE::toProductResponse);
        }

        Specification<Product> result = new ProductSpecification(params.get(0));
        for (int i = 1; i < params.size(); i++) {
            result = params.get(i).getOrPredicate()
                    ? Specification.where(result).or(new ProductSpecification(params.get(i)))
                    : Specification.where(result).and(new ProductSpecification(params.get(i)));
        }

        Page<Product> products = productRepository.findAll(Objects.requireNonNull(result), pageableSorted);
        return products.map(productMapper.INSTANCE::toProductResponse);
    }

    private List<SpecSearchCriteria> parseProductCriteria(String[] product, Pattern pattern) {
        List<SpecSearchCriteria> params = new ArrayList<>();
        for (String p : product) {
            Matcher matcher = pattern.matcher(p);
            if (matcher.find()) {
                SpecSearchCriteria searchCriteria = new SpecSearchCriteria(null, matcher.group(2), matcher.group(4), matcher.group(6), matcher.group(1), matcher.group(3), matcher.group(5));
                if (p.startsWith(OR_PREDICATE_FLAG)) {
                    searchCriteria.setOrPredicate(true);
                }
                params.add(searchCriteria);
            }
        }
        return params;
    }

    private List<SpecSearchCriteria> parseCategoryCriteria(String category, Pattern pattern) {
        List<SpecSearchCriteria> params = new ArrayList<>();
        Matcher matcher = pattern.matcher(category);
        if (matcher.find()) {
            SpecSearchCriteria searchCriteria = new SpecSearchCriteria(null, matcher.group(2), matcher.group(4), matcher.group(6), matcher.group(1), matcher.group(3), matcher.group(5));
            if (category.startsWith(OR_PREDICATE_FLAG)){
                searchCriteria.setOrPredicate(true);
            }
            params.add(searchCriteria);
        }
        return params;
    }
}