package com.tollfinder.application;

import com.tollfinder.cache.PlazaCandidateCache;
import com.tollfinder.config.FinderProperties;
import com.tollfinder.domain.*;
import com.tollfinder.routing.RouteProvider;
import com.tollfinder.spatial.*;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FindTollPlazasServiceTest {
    @Test
    void overlappingRoutesReuseCandidatesButAlwaysRecomputeMatches() {
        var routes = mock(RouteProvider.class);
        var repository = mock(TollPlazaRepository.class);
        var transaction = new TransactionTemplate() {
            @Override public <T> T execute(TransactionCallback<T> action) {
                return action.doInTransaction(new SimpleTransactionStatus());
            }
        };
        var properties = new FinderProperties(50, 32, Duration.ofMinutes(15), null);
        var cache = new PlazaCandidateCache(32, Duration.ofMinutes(15), Clock.systemUTC());
        var service = new FindTollPlazasService(routes, repository, cache, properties, transaction);
        var a = new Pincode("110001");
        var b = new Pincode("560001");
        var forward = new DrivingRoute(List.of(new GeoPoint(28.2, 77.2), new GeoPoint(28.3, 77.3)), 1000);
        var reverse = new DrivingRoute(forward.points().reversed(), 1000);
        when(routes.findRoute(a, b)).thenReturn(forward);
        when(routes.findRoute(b, a)).thenReturn(reverse);
        when(repository.datasetVersion()).thenReturn(1L);
        when(repository.findCandidates(any())).thenReturn(List.of(new TollPlaza(1, "Plaza", "Delhi", new GeoPoint(28.25, 77.25))));
        when(repository.findMatches(any(), anyCollection(), eq(50.0))).thenReturn(List.of());
        service.find(a, b);
        service.find(b, a);
        verify(repository, times(1)).findCandidates(new GridCell(77, 28));
        verify(repository).findMatches(eq(forward), anyCollection(), eq(50.0));
        verify(repository).findMatches(eq(reverse), anyCollection(), eq(50.0));
        verify(routes).findRoute(a, b);
        verify(routes).findRoute(b, a);
        assertThatThrownBy(() -> service.find(a, a)).isInstanceOf(IllegalArgumentException.class);
    }
}
