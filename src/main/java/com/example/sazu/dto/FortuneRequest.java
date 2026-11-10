package com.example.sazu.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FortuneRequest {

    private String name;

    @NotBlank(message = "생년월일을 입력해 주세요. (예: 1998-05-19)")
    private String birthDate;

    /**
     * 출생 시각 (0~23 정수 또는 null=시간 미상)
     */
    private Integer birthHour;

    private String birthTime;

    private String gender;

    private Boolean isLunar;

    @Min(value = 1, message = "카드 번호는 1부터 12 사이여야 합니다.")
    @Max(value = 12, message = "카드 번호는 1부터 12 사이여야 합니다.")
    private int cardNumber;

    private String styleTheme;

    /**
     * 사용자가 입력한 부적 소원 문구 (선택, 예: "말하는 대로 다 이뤄지는 비비디 바비디~부")
     */
    private String wishMessage;
}
