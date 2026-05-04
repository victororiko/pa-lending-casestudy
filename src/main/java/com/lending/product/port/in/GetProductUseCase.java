package com.lending.product.port.in;

import com.lending.product.adapter.in.web.CreateProductRequest;
import com.lending.product.domain.model.LoanProduct;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface GetProductUseCase {
    LoanProduct getProduct(UUID id);
    Page<LoanProduct> listProducts(Pageable pageable);
}
