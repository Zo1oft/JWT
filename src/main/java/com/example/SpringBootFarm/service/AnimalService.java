package com.example.SpringBootFarm.service;

import com.example.SpringBootFarm.model.Animal;
import com.example.SpringBootFarm.model.Cow;
import com.example.SpringBootFarm.model.NewAnimal;
import com.example.SpringBootFarm.model.Pig;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AnimalService {
    private final List<Animal> animals = new ArrayList<>();

    public AnimalService() {
        animals.add(new Cow());
        animals.add(new Pig());
    }

    public void addSound(String sound) {
        animals.add(new NewAnimal(sound));
    }

    public List<Animal> getAnimals() {
        return animals;
    }
}
