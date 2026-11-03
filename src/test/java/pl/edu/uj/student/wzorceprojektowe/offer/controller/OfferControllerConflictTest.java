package pl.edu.uj.student.wzorceprojektowe.offer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import pl.edu.uj.student.wzorceprojektowe.offer.model.UpdateOfferRequest;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferChangeService;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferCreateService;
import pl.edu.uj.student.wzorceprojektowe.offer.service.OfferSearchService;

class OfferControllerConflictTest {

  @Test
  void optimisticLockingFailureReturnsHttp409() throws Exception {
    OfferChangeService changeService = mock(OfferChangeService.class);

    when(changeService.updateOffer(any(UpdateOfferRequest.class)))
        .thenThrow(new OptimisticLockingFailureException(
            "Offer changed or was deleted"));

    OfferController controller = new OfferController(
        mock(OfferSearchService.class),
        mock(OfferCreateService.class),
        changeService);

    MockMvc mockMvc = MockMvcBuilders
        .standaloneSetup(controller)
        .build();

    mockMvc.perform(
            post("/offer/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "id": "00000000-0000-0000-0000-000000000001",
                      "quantity": 3
                    }
                    """))
        .andExpect(status().isConflict());

    verify(changeService).updateOffer(any(UpdateOfferRequest.class));
  }
}
