package com.sparta.shipment.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ShipmentCommonExceptionMessage {
    NOT_ALLOWED_API("접근 권한이 없습니다."),
    NOT_ALLOWED_NULL("은(는) 필수 입력 값입니다."),
    NOT_ALLOWED_STATUS("유효한 상태 값이 아닙니다. 유효한 값 : ["),
    NOT_ALLOWED_SORT("정렬은 createdAt, lastModifiedAt으로만 가능합니다.");


    private final String message;
}
