package com.sparta.shipment.domain.dto.request;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
public class CreateShipmentRequestDto {

    @NotNull
    private UUID orderId;

    @NotNull
    private UUID startHubId;

    @NotNull
    private UUID endHubId;

    @NotNull
     private String shippingAddress;

    @NotNull
    private String receiverName;

    @NotNull
    private String receiverSlackId;

}
