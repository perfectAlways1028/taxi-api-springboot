package com.rubyride.tripmanager;

import com.fasterxml.classmate.GenericType;
import com.rubyride.api.*;
import com.rubyride.model.*;
import com.rubyride.tripmanager.repository.mongo.UserRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@ContextConfiguration
@SpringBootTest(classes = {TestRedisConfiguration.class, TestMongoConfiguration.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@WithMockUser(authorities = "ADMIN")
public class TripManagerApplicationTests {
  private final TripApi tripApi;
  private final PlaceApi placeApi;
  private final ZoneApi zoneApi;
  private final UserApi userApi;
  private final DriverApi driverApi;
  private final VehicleApi vehicleApi;
  private final ShiftApi shiftApi;
  private final UserRepository userRepository;

  private UUID zoneId;
  private UUID fromPlaceId;
  private UUID toPlaceId;
  private UUID riderId;
  private UUID driverUserId;
  private UUID driverVehicleId;
  private UUID driverId;
  private UUID shiftId;
  private UUID tripRequestId;
  private UUID unassignedShiftId;
  private UUID unassignedTripRequestId;

  public TripManagerApplicationTests(@Autowired final TripApi tripApi, @Autowired final ZoneApi zoneApi, @Autowired final PlaceApi placeApi, @Autowired final UserApi userApi, @Autowired final DriverApi driverApi, @Autowired final VehicleApi vehicleApi, @Autowired final ShiftApi shiftApi, @Autowired final UserRepository userRepository) {
    this.tripApi = tripApi;
    this.placeApi = placeApi;
    this.zoneApi = zoneApi;
    this.userApi = userApi;
    this.driverApi = driverApi;
    this.vehicleApi = vehicleApi;
    this.shiftApi = shiftApi;
    this.userRepository = userRepository;
  }

  @BeforeAll
  public void setup() {
    final var zone = zoneApi.addZone(new Zone()
        .timeZone(0)
        .name("Test Zone"))
        .getBody();
    zoneId = zone.getId();

    final var  rider = userApi.addUser(new User()
              .firstName("Test")
              .lastName("User")
              .active(true)
              .email("testuser@foo.com")
              .userName("testuser")
              .password("xxx")
              .roles(List.of(Role.RIDER))
              .zones(Set.of(zoneId)))
              .getBody();
    riderId = rider.getId();

    final var fromPlace = placeApi.addPlace(new Place()
        .zoneId(zoneId)
        .address(new Address()
            .name("Home")
            .street("123 Home St")
            .city("City")
            .state("State")
            .zipcode("12345"))
        .location(new LatitudeLongitude()
            .latitude(-40D)
            .longitude(80D))
        .userId(riderId)
        .isPrivate(true))
        .getBody();
    fromPlaceId = fromPlace.getId();

    final var toPlace = placeApi.addPlace(new Place()
        .zoneId(zoneId)
        .address(new Address()
            .name("Work")
            .street("456 Work St")
            .city("City")
            .state("State")
            .zipcode("12345"))
        .location(new LatitudeLongitude()
            .latitude(-41D)
            .longitude(81D))
        .userId(riderId)
        .isPrivate(false))
        .getBody();
    toPlaceId = toPlace.getId();

    final var  driverUser = userApi.addUser(new User()
              .firstName("Driver")
              .lastName("Driver")
              .active(true)
              .email("driver@foo.com")
              .userName("testDriver")
              .password("xxx")
              .roles(List.of(Role.DRIVER))
              .zones(Set.of(zoneId)))
              .getBody();
    driverUserId = driverUser.getId();

    final var driverVehicle = vehicleApi.addVehicle(new Vehicle()
        .make("Make")
        .model("Model")
        .year(2020)
        .color("Blue")
        .plate("ABCDEF")
        .plateState("State"))
        .getBody();
    driverVehicleId = driverVehicle.getId();

    final var driver = driverApi.addDriver(new Driver()
        .userId(driverUserId)
        .vehicle(driverVehicleId)
        .assignedZone(zoneId)
        .homeZone(zoneId)
        .onDuty(true))
        .getBody();
    driverId = driver.getId();

  }

  @AfterAll
  public void cleanup() {
    driverApi.deleteDriver(driverId);
    vehicleApi.deleteVehicle(driverVehicleId);
    userRepository.deleteById(driverUserId);
    placeApi.deletePlace(toPlaceId);
    placeApi.deletePlace(fromPlaceId);
    userRepository.deleteById(riderId);
    zoneApi.deleteZone(zoneId);
  }

  @Test
  @Order(1)
  public void setupUnassigntripRequest() {
    final var shift = shiftApi.createShift(new Shift()
            .active(true)
            .driverId(driverUserId)
            .startTime(OffsetDateTime.now())
            .endTime(OffsetDateTime.now().plusHours(8L)))
            .getBody();
    unassignedShiftId = shift.getId();

    Assertions
            .assertThat(shiftApi.getShiftById(unassignedShiftId)
                    .getBody()
                    .getEvents())
            .hasSize(2);

    final var tripRequest = tripApi.requestTrip(new TripRequest()
            .fromLocationId(fromPlaceId)
            .toLocationId(toPlaceId)
            .tripRequestType(TripRequestType.PASSENGER)
            .passengerCount(1)
            .riderId(riderId)
            .primaryTimeConstraint(new TimeConstraint()
                    .constraintType(TimeConstraint.ConstraintTypeEnum.PICKUP_AT)
                    .time(OffsetDateTime.now().plusMinutes(5L)))
            .status(TripRequestStatus.NEW))
            .getBody();
    unassignedTripRequestId = tripRequest.getId();

    tripApi.assignTripToShift(unassignedTripRequestId, unassignedShiftId, null);

    final var shiftEvents = shiftApi.getShiftById(unassignedShiftId)
            .getBody()
            .getEvents();

    Assertions
            .assertThat(shiftEvents)
            .hasSize(4);

    Assertions
            .assertThat(shiftEvents)
            .filteredOn(event -> unassignedTripRequestId.equals(event.getTripRequestId()))
            .hasSize(2);
  }

  @Test
  @Order(2)
  public void setTripUnassign() {
    var tripRequest = tripApi.setNeedsAssigned(unassignedTripRequestId).getBody();

    Assertions
            .assertThat(tripRequest)
            .isNotNull();

    Assertions
            .assertThat(tripRequest.getStatus())
            .isEqualTo(TripRequestStatus.NEEDS_ASSIGNMENT);
    Assertions
            .assertThat(tripRequest.getShiftId())
            .isNull();
  }

  @Test
  @Order(3)
  public void tripRequest() {
    final var shift = shiftApi.createShift(new Shift()
        .active(true)
        .driverId(driverUserId)
        .startTime(OffsetDateTime.now())
        .endTime(OffsetDateTime.now().plusHours(8L)))
        .getBody();
    shiftId = shift.getId();

    Assertions
        .assertThat(shiftApi.getShiftById(shiftId)
            .getBody()
            .getEvents())
        .hasSize(2);

    final var tripRequest = tripApi.requestTrip(new TripRequest()
        .fromLocationId(fromPlaceId)
        .toLocationId(toPlaceId)
        .tripRequestType(TripRequestType.PASSENGER)
        .passengerCount(1)
        .riderId(riderId)
        .primaryTimeConstraint(new TimeConstraint()
            .constraintType(TimeConstraint.ConstraintTypeEnum.PICKUP_AT)
            .time(OffsetDateTime.now().plusMinutes(5L)))
        .status(TripRequestStatus.NEW))
        .getBody();
    tripRequestId = tripRequest.getId();

    tripApi.assignTripToShift(tripRequestId, shiftId, null);

    final var shiftEvents = shiftApi.getShiftById(shiftId)
        .getBody()
        .getEvents();

    Assertions
        .assertThat(shiftEvents)
        .hasSize(4);

    Assertions
        .assertThat(shiftEvents)
        .filteredOn(event -> tripRequestId.equals(event.getTripRequestId()))
        .hasSize(2);
  }

  @Test
  @Order(4)
  public void setTripEnroute() {
    tripApi.tripEnroute(tripRequestId, new LatitudeLongitude()
        .latitude(-40D)
        .longitude(80D));

    final var shiftEvents = shiftApi.getShiftById(shiftId)
        .getBody()
        .getEvents();

    Assertions
        .assertThat(shiftEvents)
        .hasSize(5);

    Assertions
        .assertThat(shiftEvents)
        .filteredOn(event -> riderId.equals(event.getRiderId()) &&
            tripRequestId.equals(event.getTripRequestId()) &&
            EventAction.DRIVER_EN_ROUTE.equals(event.getAction()))
        .isNotEmpty()
        .extracting(Event::getComplete)
        .contains(true);
  }

  @Test
  @Order(5)
  public void setTripPickupArrived() {
    tripApi.tripPickupArrived(tripRequestId, new LatitudeLongitude()
        .latitude(-40D)
        .longitude(80D));

    final var shiftEvents = shiftApi.getShiftById(shiftId)
        .getBody()
        .getEvents();

    Assertions
        .assertThat(shiftEvents)
        .hasSize(6);

    Assertions
        .assertThat(shiftEvents)
        .filteredOn(event -> riderId.equals(event.getRiderId()) &&
            tripRequestId.equals(event.getTripRequestId()) &&
            EventAction.PICKUP_ARRIVAL.equals(event.getAction()))
        .isNotEmpty()
        .extracting(Event::getComplete)
        .contains(true);
  }

  @Test
  @Order(6)
  public void setTripPickupComplete() {
    tripApi.tripPickUpComplete(tripRequestId, new LatitudeLongitude()
        .latitude(-40D)
        .longitude(80D));

    final var shiftEvents = shiftApi.getShiftById(shiftId)
        .getBody()
        .getEvents();

    Assertions
        .assertThat(shiftEvents)
        .hasSize(6);

    Assertions
        .assertThat(shiftEvents)
        .filteredOn(event -> riderId.equals(event.getRiderId()) &&
            tripRequestId.equals(event.getTripRequestId()) &&
            EventAction.PICKUP.equals(event.getAction()))
        .isNotEmpty()
        .extracting(Event::getComplete)
        .contains(true);
  }

  @Test
  @Order(7)
  public void setTripDropoffArrived() {
    tripApi.tripDropffArrived(tripRequestId, new LatitudeLongitude()
        .latitude(-41D)
        .longitude(81D));

    final var shiftEvents = shiftApi.getShiftById(shiftId)
        .getBody()
        .getEvents();

    Assertions
        .assertThat(shiftEvents)
        .hasSize(7);

    Assertions
        .assertThat(shiftEvents)
        .filteredOn(event -> riderId.equals(event.getRiderId()) &&
            tripRequestId.equals(event.getTripRequestId()) &&
            EventAction.DROPOFF_ARRIVAL.equals(event.getAction()))
        .isNotEmpty()
        .extracting(Event::getComplete)
        .contains(true);
  }

  @Test
  @Order(8)
  public void setTripDropoffComplete() {
    tripApi.tripDropOffComplete(tripRequestId, new LatitudeLongitude()
        .latitude(-41D)
        .longitude(81D));

    final var shiftEvents = shiftApi.getShiftById(shiftId)
        .getBody()
        .getEvents();

    Assertions
        .assertThat(shiftEvents)
        .hasSize(7);

    Assertions
        .assertThat(shiftEvents)
        .filteredOn(event -> riderId.equals(event.getRiderId()) &&
            tripRequestId.equals(event.getTripRequestId()) &&
            EventAction.DROPOFF.equals(event.getAction()))
        .isNotEmpty()
        .extracting(Event::getComplete)
        .contains(true);
  }
}
