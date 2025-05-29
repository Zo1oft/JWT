package com.example.SpringBootFarm.controller;


import com.example.SpringBootFarm.model.Animal;
import com.example.SpringBootFarm.model.Chef;
import com.example.SpringBootFarm.service.AnimalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

@Controller
public class FarmController {

    private final AnimalService animalService;

    public FarmController(AnimalService animalService) {
        this.animalService = animalService;
    }

    @GetMapping("/farm")
    public String getSound(Model model) {
        List<String> sound = new ArrayList<>();
        for (var animal : animalService.getAnimals()){
            sound.add(animal.speak());
        }
        String randomSound = sound.get(new Random().nextInt(sound.size()));
        model.addAttribute("sound", randomSound);
        return "farm";
    }
}