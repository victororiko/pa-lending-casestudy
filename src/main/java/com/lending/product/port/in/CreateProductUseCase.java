package com.lending.product.port.in;

import com.lending.product.adapter.in.web.CreateProductRequest;
import com.lending.product.domain.model.LoanProduct;

public interface CreateProductUseCase {
    LoanProduct createProduct(CreateProductRequest request);
}
