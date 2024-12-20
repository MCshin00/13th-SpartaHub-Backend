package com.sparta.Hub.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "p_hub_route")
public class HubRoute extends Audit {
  @Id
  @GeneratedValue(generator = "UUID")
  private UUID hubId;

  @Column(nullable = false, precision = 5, scale = 2) // 거리: 최대 999.99
  private BigDecimal distance;
  @Column(nullable = false, precision = 4, scale = 2) // 시간: 최대 99.99
  private BigDecimal deliveryTime;

  @Column(nullable = false)
  private String startHubName;
  @Column(nullable = false)
  private String endHubName;

  @ManyToOne
  @JoinColumn(name = "start_hub_id", nullable = false)
  private Hub startHub;

  @ManyToOne
  @JoinColumn(name = "end_hub_id", nullable = false)
  private Hub endHub;

  public HubRoute(BigDecimal deliveryTime, BigDecimal distance, Hub startHub, Hub endHub) {
    this.deliveryTime = deliveryTime;
    this.distance = distance;
    this.startHub = startHub;
    this.endHub = endHub;
  }

  public void updateEndHubRoute(Hub endHub) {
    this.endHub = endHub;
  }

  public void updateRoad(BigDecimal deliveryTime, BigDecimal distance,Hub endHub, String endHubName) {
    this.deliveryTime = deliveryTime;
    this.distance = distance;
    this.endHub = endHub;
    this.endHubName = endHubName;
  }
}
