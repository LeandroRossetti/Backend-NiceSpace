package com.cowork.backend.location;

import com.cowork.backend.amenity.Amenity;
import com.cowork.backend.amenity.AmenityRepository;
import com.cowork.backend.location.dto.LocationDto;
import com.cowork.backend.location.dto.LocationSearchFilter;
import com.cowork.backend.reservation.ReservationRepository;
import com.cowork.backend.reservation.ReservationStatus;
import com.cowork.backend.security.AuthHelper;
import com.cowork.backend.user.AccountType;
import com.cowork.backend.user.User;
import com.cowork.backend.workspace.Floor;
import com.cowork.backend.workspace.FloorRepository;
import com.cowork.backend.workspace.Workspace;
import com.cowork.backend.workspace.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceFilterTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private FloorRepository floorRepository;

    @Mock
    private AmenityRepository amenityRepository;

    @Mock
    private WorkspaceRepository workspaceRepository;

    @Mock
    private ReservationRepository reservationRepository;

    @Mock
    private AuthHelper authHelper;

    private LocationService locationService;

    private static final Instant START = Instant.parse("2026-11-02T12:00:00Z");
    private static final Instant END = Instant.parse("2026-11-02T14:00:00Z");

    @BeforeEach
    void setUp() {
        locationService = new LocationService(
                locationRepository,
                floorRepository,
                amenityRepository,
                workspaceRepository,
                reservationRepository,
                authHelper,
                new ObjectMapper());
    }

    @Test
    void searchReturnsAllActiveLocationsWhenFilterIsEmpty() {
        Location palermo = location(1L, "Palermo", "Buenos Aires", amenities(1L));
        Location belgrano = location(2L, "Belgrano", "Buenos Aires", amenities(2L));
        when(locationRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(palermo, belgrano));

        List<LocationDto> result = locationService.search(
                new LocationSearchFilter(null, null, List.of(), null, null));

        assertThat(result).extracting(LocationDto::id).containsExactly(1L, 2L);
    }

    @Test
    void searchFiltersByCityIgnoringCaseAndPartialMatch() {
        Location palermo = location(1L, "Palermo", "Buenos Aires", Set.of());
        Location belgrano = location(2L, "Belgrano", "Buenos Aires", Set.of());
        when(locationRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(palermo, belgrano));

        List<LocationDto> result = locationService.search(
                new LocationSearchFilter("pal", null, List.of(), null, null));

        assertThat(result).extracting(LocationDto::id).containsExactly(1L);
    }

    @Test
    void searchFiltersByProvinceIgnoringCase() {
        Location caba = location(1L, "Palermo", "CABA", Set.of());
        Location cordoba = location(2L, "Centro", "Córdoba", Set.of());
        when(locationRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(caba, cordoba));

        List<LocationDto> result = locationService.search(
                new LocationSearchFilter(null, "córd", List.of(), null, null));

        assertThat(result).extracting(LocationDto::id).containsExactly(2L);
    }

    @Test
    void searchRequiresAllRequestedAmenities() {
        Location full = location(1L, "Palermo", null, amenities(1L, 2L));
        Location partial = location(2L, "Belgrano", null, amenities(1L));
        when(locationRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(full, partial));

        List<LocationDto> result = locationService.search(
                new LocationSearchFilter(null, null, List.of(1L, 2L), null, null));

        assertThat(result).extracting(LocationDto::id).containsExactly(1L);
    }

    @Test
    void searchByAvailabilityKeepsOnlyLocationsWithBookableFreeWorkspace() {
        Location libre = location(1L, "Palermo", null, Set.of());
        Location ocupada = location(2L, "Belgrano", null, Set.of());

        Floor floor1 = floor(10L, libre, mapWith(101L));
        Floor floor2 = floor(20L, ocupada, mapWith(202L));

        Workspace wsLibre = workspace(101L, libre, floor1, true);
        Workspace wsOcupado = workspace(202L, ocupada, floor2, true);

        when(locationRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(libre, ocupada));
        when(workspaceRepository.findByLocationIdIn(anyCollection())).thenReturn(List.of(wsLibre, wsOcupado));
        when(floorRepository.findByLocationIdInOrderByLevelOrderAsc(anyCollection())).thenReturn(List.of(floor1, floor2));
        when(reservationRepository.findOccupiedWorkspaceIdsInLocations(
                anyCollection(), eq(ReservationStatus.CANCELADA), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(202L));

        List<LocationDto> result = locationService.search(
                new LocationSearchFilter(null, null, List.of(), START, END));

        assertThat(result).extracting(LocationDto::id).containsExactly(1L);
    }

    @Test
    void searchByAvailabilityExcludesWorkspaceNotPositionedInMap() {
        Location sinPlano = location(1L, "Palermo", null, Set.of());
        Floor floor = floor(10L, sinPlano, mapWith(999L)); // el mapa no referencia al workspace 101
        Workspace ws = workspace(101L, sinPlano, floor, true);

        when(locationRepository.findByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(List.of(sinPlano));
        when(workspaceRepository.findByLocationIdIn(anyCollection())).thenReturn(List.of(ws));
        when(floorRepository.findByLocationIdInOrderByLevelOrderAsc(anyCollection())).thenReturn(List.of(floor));
        when(reservationRepository.findOccupiedWorkspaceIdsInLocations(
                anyCollection(), eq(ReservationStatus.CANCELADA), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        List<LocationDto> result = locationService.search(
                new LocationSearchFilter(null, null, List.of(), START, END));

        assertThat(result).isEmpty();
    }

    private Location location(Long id, String city, String province, Set<Amenity> amenities) {
        return Location.builder()
                .id(id)
                .nombre("Sede " + id)
                .owner(owner())
                .city(city)
                .province(province)
                .isActive(true)
                .amenities(new HashSet<>(amenities))
                .build();
    }

    private User owner() {
        return User.builder()
                .id(100L)
                .nombre("Dueño")
                .email("owner@openspot.app")
                .accountType(AccountType.OWNER)
                .build();
    }

    private Set<Amenity> amenities(Long... ids) {
        Set<Amenity> result = new HashSet<>();
        for (Long id : ids) {
            result.add(Amenity.builder().id(id).nombre("Amenity " + id).build());
        }
        return result;
    }

    private Floor floor(Long id, Location location, String mapStructure) {
        return Floor.builder()
                .id(id)
                .location(location)
                .nombre("Piso " + id)
                .levelOrder(1)
                .mapStructure(mapStructure)
                .build();
    }

    private Workspace workspace(Long id, Location location, Floor floor, boolean active) {
        return Workspace.builder()
                .id(id)
                .location(location)
                .floor(floor)
                .nombre("Espacio " + id)
                .isActive(active)
                .build();
    }

    private String mapWith(long... workspaceIds) {
        StringBuilder elements = new StringBuilder();
        for (int i = 0; i < workspaceIds.length; i++) {
            if (i > 0) {
                elements.append(',');
            }
            elements.append("{\"workspaceId\":").append(workspaceIds[i])
                    .append(",\"x\":0,\"y\":0,\"w\":100,\"h\":50,\"rotation\":0}");
        }
        return "{\"elements\":[" + elements + "],\"objects\":[]}";
    }
}
