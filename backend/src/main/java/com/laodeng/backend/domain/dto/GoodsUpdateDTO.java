package com.laodeng.backend.domain.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * @author laodeng
 * @version v1.0
 * @date 2026/11/08
 * @description 商品更新请求参数（仅更新非空字段）
 */

@Data
@Builder
@Accessors(chain = true)
@NoArgsConstructor
@AllArgsConstructor
public class GoodsUpdateDTO {

    @NotNull
    private Long id;

    @Size(max = 255)
    private String name;

    @DecimalMin(value = "0.00", inclusive = false)
    @Digits(integer = 8, fraction = 2)
    private BigDecimal price;

    @Size(max = 255)
    private String image;

    @Size(max = 1000)
    private String description;

}
