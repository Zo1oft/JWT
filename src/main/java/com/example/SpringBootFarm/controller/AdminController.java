package com.example.SpringBootFarm.controller;

import com.example.SpringBootFarm.model.Animal;
import com.example.SpringBootFarm.service.AnimalService;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AnimalService animalService;

    public AdminController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping
    public String adminPage(Model model) {
        model.addAttribute("animals", animalService.getAnimals());
        return "admin";
    }

    @PostMapping("/add-sound")
    public ResponseEntity<String> addSound(@RequestParam String sound) {
        animalService.addSound(sound);
        return ResponseEntity.ok("Sound added: " + sound);
    }
}
