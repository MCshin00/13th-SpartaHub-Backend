package com.sparta.Hub.domain.service;

import com.sparta.Hub.domain.dto.request.CreateHubRouteReq;
import com.sparta.Hub.domain.dto.request.UpdateHubRouteReq;
import com.sparta.Hub.domain.dto.response.CreateHubRouteRes;
import com.sparta.Hub.domain.dto.response.DeleteHubRouteRes;
import com.sparta.Hub.domain.dto.response.GetHubRouteInfoRes;
import com.sparta.Hub.domain.dto.response.KakaoApiRes;
import com.sparta.Hub.domain.dto.response.UpdateHubRouteRes;
import com.sparta.Hub.exception.HubExceptionMessage;
import com.sparta.Hub.exception.HubRouteExceptionMessage;
import com.sparta.Hub.model.entity.Hub;
import com.sparta.Hub.model.entity.HubRoute;
import com.sparta.Hub.model.repository.HubRepository;
import com.sparta.Hub.model.repository.HubRouteRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
public class HubRouteService {

  private static final String CENTER_GYEONGGI_SOUTH = "경기 남부 센터";
  private static final String CENTER_DAEGU = "대구광역시 센터";
  private static final String CENTER_DAEJEON = "대전광역시 센터";

  @Value("${kakao.api.key}")
  private String apiKey;

  private final HubRouteRepository hubRouteRepository;
  private final HubRepository hubRepository;

  public CreateHubRouteRes createHubRoute(
      CreateHubRouteReq createHubRouteReq,
      String requestUsername,
      String requestRole
  ) {
    //validateRole(requestRole);
    validateEquealHub(createHubRouteReq);

    Hub startHub = validateHub(createHubRouteReq.getStratHubId());
    Hub endHub = validateHub(createHubRouteReq.getEndHubId());

    Map<String, Integer> startRouteInfo = moveCenterHub(startHub);
    Map<String, Integer> endRouteInfo = moveCenterHub(endHub);

    KakaoApiRes kakaoApiRes = finalMoveRoute(startHub, endHub, startRouteInfo, endRouteInfo);

    HubRoute hubRoute = HubRoute.builder()
        .startHub(startHub)
        .endHub(endHub)
        .startHubName(startHub.getHubname())
        .endHubName(endHub.getHubname())
        .deliveryTime(kakaoApiRes.getDeliveryTime())
        .distance(kakaoApiRes.getDistance())
        .build();

    hubRouteRepository.save(hubRoute);

    return CreateHubRouteRes.builder()
        .hubRouteId(hubRoute.getHubId())
        .startHubName(hubRoute.getStartHubName())
        .endHubName(hubRoute.getEndHubName())
        .deliveryTime(hubRoute.getDeliveryTime())
        .distance(hubRoute.getDistance())
        .build();
  }


  @Cacheable(cacheNames = "hubroutecache", key = "args[0]")
  public GetHubRouteInfoRes getHubRoute(UUID hubRouteId) {
    HubRoute hubRoute = validateExistHubRoute(hubRouteId);
    return GetHubRouteInfoRes.builder()
        .hubRouteId(hubRoute.getHubId())
        .startHubName(hubRoute.getStartHubName())
        .endHubName(hubRoute.getEndHubName())
        .distance(hubRoute.getDistance())
        .deliveryTime(hubRoute.getDeliveryTime())
        .build();

  }

  @Cacheable(cacheNames = "hubrouteAllcache", key = "getMethodName()")
  public Page<GetHubRouteInfoRes> getAllHubRoutes(String keyword, Pageable pageable) {
    return hubRouteRepository.searchHubRoutes(keyword, pageable);
  }

  @Transactional
  @CachePut(cacheNames = "hubroutecache", key = "args[0]")
  @CacheEvict(cacheNames = "hubrouteAllcache", allEntries = true)
  public UpdateHubRouteRes updateHubRoute(
      UUID hubRouteId,
      UpdateHubRouteReq updateHubRouteReq,
      String requestUsername,
      String requestRole
  ) {
    //validateRole(requestRole);
    HubRoute hubRoute = validateExistHubRoute(hubRouteId);
    hubRoute.updateCreatedByAndLastModifiedBy(requestUsername);

    hubRouteRepository.save(checkUpdate(hubRoute, updateHubRouteReq));

    return UpdateHubRouteRes.builder()
        .hubRouteId(hubRoute.getHubId())
        .deliveryTime(hubRoute.getDeliveryTime())
        .distance(hubRoute.getDistance())
        .build();

  }

  @Caching(evict = {
      @CacheEvict(cacheNames = "hubroutecache", key = "args[0]"),
      @CacheEvict(cacheNames = "hubrouteAllcache", allEntries = true)
  })
  public DeleteHubRouteRes deleteHubRoute(UUID hubRouteId, String requestUsername,
      String requestRole) {
    validateRole(requestRole);
    HubRoute hubRoute = validateExistHubRoute(hubRouteId);
    hubRoute.updateDeleted(requestUsername);
    hubRouteRepository.save(hubRoute);
    return new DeleteHubRouteRes(hubRouteId);
  }


  private void validateRole(String requestRole) {
    if (!requestRole.equals("MASTER")) {
      throw new IllegalArgumentException(HubExceptionMessage.NOT_ALLOWED_API.getMessage());
    }
  }

  private HubRoute validateExistHubRoute(UUID hubRouteId) {
    return hubRouteRepository.findById(hubRouteId).orElseThrow(() ->
        new IllegalArgumentException(HubRouteExceptionMessage.HUB_ROUTE_NOT_EXIST.getMessage()));
  }


  private void validateEquealHub(CreateHubRouteReq createHubRouteReq) {
    if (createHubRouteReq.getStratHubId().equals(createHubRouteReq.getEndHubId())) {
      throw new IllegalArgumentException(HubRouteExceptionMessage.HUB_ROUTE_EQUEAL.getMessage());
    }
  }


  private Hub validateHub(UUID hubId) {
    return hubRepository.findById(hubId).orElseThrow(() -> new IllegalArgumentException(
        HubExceptionMessage.HUB_NOT_EXIST.getMessage()));
  }

  private Map<String, Integer> moveCenterHub(Hub hub) {

    if (hub.isCenterHub()) {
      Map<String, Integer> routeInfo = new HashMap<>();
      routeInfo.put("distance", 0);
      routeInfo.put("deliveryTime", 0);
      return routeInfo;
    }else{
      return kakaoMapApi(hub,hub.getCenterHub());
    }

  }

  private KakaoApiRes finalMoveRoute(
      Hub startHub,
      Hub endHub,
      Map<String, Integer> startRouteInfo, //중앙허브 이동값
      Map<String, Integer> endRouteInfo    //중앙허브 이동값
  ) {
    Map<String, Integer> routeInfo = new HashMap<>();
    //중앙허브가 시작,끝 허브인경우
    if (startHub.isCenterHub() && endHub.isCenterHub()) {
      routeInfo = kakaoMapApi(startHub, endHub);
      return changeRouteInfo(routeInfo);

    } else if (startHub.isCenterHub()&&!endHub.isCenterHub()) {
      if (startHub.equals(endHub.getCenterHub())) {

        return sameCenterHub(startRouteInfo, endRouteInfo, routeInfo);

      } else if (startHub.getHubname().equals(CENTER_GYEONGGI_SOUTH)
          || startHub.getHubname().equals(CENTER_DAEGU)) {
        if (endHub.getCenterHub().getHubname().equals(CENTER_DAEJEON)) {

          Map<String, Integer> finalRoute = kakaoMapApi(startHub, endHub.getCenterHub());

          return oneMoveCenterHub(finalRoute, startRouteInfo, endRouteInfo, routeInfo);

        } else {
          Hub centerHub = hubRepository.findByHubname(CENTER_DAEJEON);
          routeInfo = kakaoMapApi(startHub, centerHub);
          Map<String, Integer> finalRoute = kakaoMapApi(centerHub, endHub.getCenterHub());

          return twoMoveCenterHub(startRouteInfo, endRouteInfo, routeInfo, finalRoute);

        }
      } else {
        Map<String, Integer> finalRoute = kakaoMapApi(startHub, endHub.getCenterHub());
        return oneMoveCenterHub(finalRoute, startRouteInfo, endRouteInfo, routeInfo);
      }
    } else if (endHub.isCenterHub()&& !startHub.isCenterHub()) {
      if (endHub.equals(startHub.getCenterHub())) {
        return sameCenterHub(startRouteInfo, endRouteInfo, routeInfo);
      } else if (endHub.getHubname().equals(CENTER_GYEONGGI_SOUTH) || endHub.getHubname().equals(CENTER_DAEGU)
      ) {
        if (startHub.getCenterHub().getHubname().equals(CENTER_DAEJEON)) {

          Map<String, Integer> finalRoute = kakaoMapApi(startHub.getCenterHub(), endHub);

          return oneMoveCenterHub(finalRoute, startRouteInfo, endRouteInfo, routeInfo);
        } else {
          Hub centerHub = hubRepository.findByHubname(CENTER_DAEJEON);
          routeInfo = kakaoMapApi(startHub.getCenterHub(), centerHub);
          Map<String, Integer> finalRoute = kakaoMapApi(centerHub, endHub);

          return twoMoveCenterHub(startRouteInfo, endRouteInfo, routeInfo, finalRoute);
        }
      } else {
        Map<String, Integer> finalRoute = kakaoMapApi(startHub.getCenterHub(), endHub);

        return oneMoveCenterHub(finalRoute, startRouteInfo, endRouteInfo, routeInfo);
      }

    } else {
      if (startHub.getCenterHub().equals(endHub.getCenterHub())) {
        return sameCenterHub(startRouteInfo, endRouteInfo, routeInfo);

      } else if (startHub.getCenterHub().getHubname().equals(CENTER_GYEONGGI_SOUTH)
          || startHub.getCenterHub().getHubname().equals(CENTER_DAEGU)) {

        if (endHub.getCenterHub().getHubname().equals(CENTER_DAEJEON)){

          Map<String, Integer> finalRoute = kakaoMapApi(startHub.getCenterHub(),
              endHub.getCenterHub());
          return oneMoveCenterHub(finalRoute, startRouteInfo, endRouteInfo, routeInfo);

        } else {
          Hub centerHub = hubRepository.findByHubname(CENTER_DAEJEON);
          routeInfo = kakaoMapApi(startHub.getCenterHub(), centerHub);
          Map<String, Integer> finalRoute = kakaoMapApi(centerHub, endHub.getCenterHub());

          return twoMoveCenterHub(startRouteInfo, endRouteInfo, routeInfo, finalRoute);
        }

      } else {
        Map<String, Integer> finalRoute = kakaoMapApi(startHub.getCenterHub(),
            endHub.getCenterHub());
        return oneMoveCenterHub(finalRoute, startRouteInfo, endRouteInfo, routeInfo);

      }
    }
  }

  private KakaoApiRes twoMoveCenterHub(
      Map<String, Integer> startRouteInfo,
      Map<String, Integer> endRouteInfo,
      Map<String, Integer> routeInfo,
      Map<String, Integer> finalRoute) {

    Integer distance = finalRoute.get("distance")
        + startRouteInfo.get("distance")
        + endRouteInfo.get("distance")
        + routeInfo.get("distance");
    Integer deliveryTime = finalRoute.get("deliveryTime")
        + endRouteInfo.get("deliveryTime")
        + startRouteInfo.get("deliveryTime")
        + routeInfo.get("deliveryTime");
    routeInfo.put("distance", distance);
    routeInfo.put("deliveryTime", deliveryTime);
    return changeRouteInfo(routeInfo);

  }

  private KakaoApiRes oneMoveCenterHub(
      Map<String, Integer> finalRoute,
      Map<String, Integer> startRouteInfo,
      Map<String, Integer> endRouteInfo,
      Map<String, Integer> routeInfo) {
    Integer distnace = finalRoute.get("distance")
        + endRouteInfo.get("distance")
        + startRouteInfo.get("distance");
    Integer deliveryTime = finalRoute.get("deliveryTime")
        + endRouteInfo.get("deliveryTime")
        + startRouteInfo.get("deliveryTime");
    routeInfo.put("distance", distnace);
    routeInfo.put("deliveryTime", deliveryTime);
    return changeRouteInfo(routeInfo);
  }

  private KakaoApiRes sameCenterHub(
      Map<String, Integer> startRouteInfo,
      Map<String, Integer> endRouteInfo,
      Map<String, Integer> routeInfo) {
    routeInfo.put("distance", startRouteInfo.get("distance") + endRouteInfo.get("distance"));
    routeInfo.put("deliveryTime",
        startRouteInfo.get("deliveryTime") + endRouteInfo.get("deliveryTime"));

    return changeRouteInfo(routeInfo);
  }

  private Map<String, Integer> kakaoMapApi(Hub startHub, Hub endHub) {
    HttpHeaders headers = new HttpHeaders();
    headers.setAccept(List.of(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("Authorization", "KakaoAK " + apiKey);

    HttpEntity<String> entity = new HttpEntity<>("", headers);

    String url = "https://apis-navi.kakaomobility.com/v1/directions?" +
        "origin=" + startHub.getLongti() + "," + startHub.getLati() +
        "&destination=" + endHub.getLongti() + "," + endHub.getLati();

    RestTemplate restTemplate = new RestTemplate();
    // ResponseEntity<Map>에서 JSON 파싱
    ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);

    List<Map<String, Object>> routes = (List<Map<String, Object>>) response.getBody().get("routes");
    Map<String, Object> route = routes.get(0);  // 첫 번째 경로

    Map<String, Object> summary = (Map<String, Object>) route.get("summary");

    Object distance = summary.get("distance");
    Object duration = summary.get("duration");
    Integer meter = (Integer) distance;
    Integer second = (Integer) duration;
    Map<String, Integer> routeInfo = new HashMap<>();
    routeInfo.put("distance", meter);
    routeInfo.put("deliveryTime", second);

    return routeInfo;

  }

  private KakaoApiRes changeRouteInfo(Map<String, Integer> routeInfo) {
    LocalDateTime deliveryTime = LocalDateTime.now()
        .plus(Duration.ofSeconds(routeInfo.get("deliveryTime")));

    BigDecimal meterBD = new BigDecimal(routeInfo.get("distance"));
    BigDecimal kilometerBD = meterBD.divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP);

    // 응답 본문에서 필요한 필드만 추출
    return KakaoApiRes.builder()
        .deliveryTime(deliveryTime)
        .distance(kilometerBD)
        .build();
  }


  private HubRoute checkUpdate(HubRoute hubRoute, UpdateHubRouteReq updateHubRouteReq) {

    if (updateHubRouteReq.getEndHubId() != null) {
      Hub endHub = validateHub(updateHubRouteReq.getEndHubId());
      hubRoute.updateEndHubRoute(endHub);

      Map<String, Integer> startRouteInfo = moveCenterHub(hubRoute.getStartHub());
      Map<String, Integer> endRouteInfo = moveCenterHub(endHub);

      KakaoApiRes kakaoApiRes = finalMoveRoute(hubRoute.getStartHub(), endHub, startRouteInfo,
          endRouteInfo);

      hubRoute.updateRoad(kakaoApiRes.getDeliveryTime(), kakaoApiRes.getDistance());
      return hubRoute;
    }
    return hubRoute;

  }


}