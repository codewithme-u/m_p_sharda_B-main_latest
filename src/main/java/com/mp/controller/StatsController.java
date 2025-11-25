package com.mp.controller;

import com.mp.entity.Institution;
import com.mp.service.InstitutionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final InstitutionService institutionService;

    public StatsController(InstitutionService institutionService) {
        this.institutionService = institutionService;
    }

    @GetMapping("/institutions")
    public List<InstitutionStatDTO> getInstitutionStats() {
        List<Institution> list = institutionService.getAll();
        
        // Map the institution list to the format the frontend expects
        return list.stream().map(inst -> new InstitutionStatDTO(
                inst.getId(),
                inst.getInstituteName(),
                0 // Placeholder for student count (logic to be added later)
        )).collect(Collectors.toList());
    }

    // DTO record
    public record InstitutionStatDTO(Long institutionId, String name, int studentCount) {}
}