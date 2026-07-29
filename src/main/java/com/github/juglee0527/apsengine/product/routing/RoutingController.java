package com.github.juglee0527.apsengine.product.routing;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoutingController {

    private final RoutingService routingService;

    public RoutingController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/api/v1/products/{productId}/routings")
    public ResponseEntity<RoutingResponse> create(
            @PathVariable long productId,
            @Valid @RequestBody RoutingCreateRequest request
    ) {
        Routing routing = routingService.create(
                productId,
                request.code(),
                request.name(),
                request.operations()
        );
        RoutingResponse response = RoutingResponse.from(routing);
        URI location = URI.create("/api/v1/routings/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/api/v1/routings/{routingId}")
    public RoutingResponse getById(@PathVariable long routingId) {
        return RoutingResponse.from(routingService.getById(routingId));
    }

    @GetMapping("/api/v1/products/{productId}/routings")
    public List<RoutingResponse> getAllByProduct(
            @PathVariable long productId
    ) {
        List<Routing> routings = routingService.getAllByProduct(productId);
        List<RoutingResponse> responses =
                new ArrayList<>(routings.size());
        for (Routing routing : routings) {
            responses.add(RoutingResponse.from(routing));
        }
        return responses;
    }
}
