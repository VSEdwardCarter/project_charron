
package com.charron.api.loading.web;

import com.charron.api.loading.application.LoadingPreviewService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoadingPreviewControllerTest {

    @Test
    void delegatesPreviewRequestToService() {
        LoadingPreviewService service =
                mock(LoadingPreviewService.class);

        LoadingPreviewController controller =
                new LoadingPreviewController(service);

        UUID cargoSpaceId = UUID.randomUUID();

        LoadingPreviewRequest request =
                new LoadingPreviewRequest(
                        cargoSpaceId,
                        List.of(
                                new LoadingPreviewRequest.CargoItem(
                                        UUID.randomUUID(),
                                        "Generator",
                                        1,
                                        1200,
                                        800,
                                        1000,
                                        150000L
                                )
                        )
                );

        LoadingPreviewResponse expected =
                new LoadingPreviewResponse(
                        cargoSpaceId,
                        "VALID",
                        "Loading plan passed validation",
                        List.of()
                );

        when(service.preview(request)).thenReturn(expected);

        LoadingPreviewResponse actual =
                controller.preview(request);

        assertSame(expected, actual);
        verify(service).preview(request);
        verifyNoMoreInteractions(service);
    }
}
