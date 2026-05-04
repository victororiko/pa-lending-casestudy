package com.lending.product.port.in;

import com.lending.product.adapter.in.web.CreateProductRequest;
import com.lending.product.domain.model.LoanProduct;

import java.util.UUID;

public interface UpdateProductUseCase {
    LoanProduct updateProduct(UUID id, CreateProductRequest request);
    void deleteProduct(UUID id);
}
