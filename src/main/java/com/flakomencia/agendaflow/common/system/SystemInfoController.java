package com.flakomencia.agendaflow.common.system;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/api/v1/system", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "System", description = "Technical bootstrap endpoints")
class SystemInfoController {

    @GetMapping("/info")
    @Operation(summary = "Get bootstrap service information")
    Map<String, String> info() {
        return Map.of(
                "service", "agendaflow-api",
                "status", "UP",
                "phase", "bootstrap");
    }
}
