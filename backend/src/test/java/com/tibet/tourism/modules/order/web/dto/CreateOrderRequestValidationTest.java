package com.tibet.tourism.modules.order.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.LocalDate;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateOrderRequestValidationTest {

    private ValidatorFactory validatorFactory;
    private Validator validator;

    @BeforeEach
    void setUp() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterEach
    void tearDown() {
        validatorFactory.close();
    }

    @Test
    void rejectsMoreThanMaximumItems() {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(IntStream.range(0, CreateOrderRequest.MAX_ITEMS + 1)
                .mapToObj(index -> validItem())
                .toList());

        var violations = validator.validate(request);

        assertThat(violations)
                .anySatisfy(violation -> {
                    assertThat(violation.getPropertyPath().toString()).isEqualTo("items");
                    assertThat(violation.getMessage()).contains(String.valueOf(CreateOrderRequest.MAX_ITEMS));
                });
    }

    private CreateOrderItemRequest validItem() {
        CreateOrderItemRequest item = new CreateOrderItemRequest();
        item.setProductType("SCENIC_SPOT");
        item.setProductId(10L);
        item.setServiceStartDate(LocalDate.now().plusDays(1));
        item.setQuantity(1);
        return item;
    }
}
